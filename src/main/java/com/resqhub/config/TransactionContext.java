package com.resqhub.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;

/**
 * WEB MIGRATION ADDITION - real database transactions with row locks.
 *
 * Why: the desktop service layer performed "read quantity, compute in Java,
 * write absolute value" followed by further writes on separate connections.
 * With one desktop user that is harmless; on a website two requests can
 * interleave, so stock was lost (30 parallel "distribute 10 of 100" requests
 * all succeeded and left 90 in stock) and shelters could be over-booked.
 *
 * How: a service operation opens a context with try-with-resources:
 *
 *     try (TransactionContext tx = TransactionContext.begin()) {
 *         tx.lockRow("resources", id);   // SELECT ... FOR UPDATE: others wait
 *         ... unchanged DAO calls ...    // all share ONE connection
 *         tx.commit();
 *     }                                   // closes; rolls back if not committed
 *
 * BaseDao.openConnection() hands every DAO the thread's transaction connection
 * (wrapped so their try-with-resources close() does not end the transaction),
 * so no DAO had to change. Calls made while a context is already active join
 * it instead of nesting. The lock is taken BEFORE the first plain SELECT, so
 * InnoDB's consistent-read snapshot is created after the previous writer has
 * committed - the check-then-write sequence therefore sees current data.
 */
public final class TransactionContext implements AutoCloseable {

    private static final ThreadLocal<Connection> ACTIVE = new ThreadLocal<>();

    private final Connection real;      // null when this context merely joined an outer one
    private boolean committed;

    private TransactionContext(Connection real) {
        this.real = real;
    }

    /** The shared connection (wrapped) when a transaction is active on this thread, else null. */
    public static Connection activeConnection() {
        Connection c = ACTIVE.get();
        return c == null ? null : nonClosing(c);
    }

    public static TransactionContext begin() throws DataAccessException {
        if (ACTIVE.get() != null) {
            return new TransactionContext(null);          // join the outer transaction
        }
        try {
            Connection c = DatabaseConnectionManager.getInstance().getConnection();
            c.setAutoCommit(false);
            ACTIVE.set(c);
            return new TransactionContext(c);
        } catch (ResQHubException e) {
            throw new DataAccessException("Database configuration problem", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not start a database transaction", e);
        }
    }

    /**
     * Locks one row until the transaction ends. {@code table} must be a
     * literal from the calling code (it is not user input).
     */
    public void lockRow(String table, long id) throws DataAccessException {
        Connection c = ACTIVE.get();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM " + table + " WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, id);
            ps.executeQuery().close();
        } catch (SQLException e) {
            throw new DataAccessException("Could not lock " + table + " #" + id, e);
        }
    }

    public void commit() throws DataAccessException {
        if (real == null) {
            return;                                       // outer context commits
        }
        try {
            real.commit();
            committed = true;
        } catch (SQLException e) {
            throw new DataAccessException("Could not commit the transaction", e);
        }
    }

    @Override
    public void close() {
        if (real == null) {
            return;
        }
        try {
            if (!committed) {
                real.rollback();
            }
        } catch (SQLException ignored) {
            // connection is discarded below
        } finally {
            ACTIVE.remove();
            try {
                real.setAutoCommit(true);
            } catch (SQLException ignored) {
                // closing anyway
            }
            try {
                real.close();
            } catch (SQLException ignored) {
                // nothing more to do
            }
        }
    }

    /** Proxy that ignores close()/commit()/rollback()/setAutoCommit() so DAOs cannot end our transaction. */
    private static Connection nonClosing(Connection target) {
        InvocationHandler h = (proxy, method, args) -> {
            switch (method.getName()) {
                case "close", "commit", "rollback", "setAutoCommit" -> {
                    return null;
                }
                case "isClosed" -> {
                    return Boolean.FALSE;
                }
                default -> {
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            }
        };
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, h);
    }
}
