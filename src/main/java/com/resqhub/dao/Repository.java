package com.resqhub.dao;

import java.util.List;

import com.resqhub.exception.DataAccessException;

/**
 * GENERIC INTERFACE defining the CRUD contract every persistent
 * entity must satisfy. Services depend on this abstraction, not on
 * concrete DAO classes (Dependency Inversion Principle).
 *
 * Note: RoleDAO intentionally does NOT implement this interface -
 * roles are a fixed lookup table without full lifecycle operations
 * (Interface Segregation Principle - no empty/misleading methods).
 */
public interface Repository<T> {

    /** Inserts when the entity has no id yet, otherwise updates. Returns the persisted state. */
    T save(T entity) throws DataAccessException;

    T findById(long id) throws DataAccessException;

    List<T> findAll() throws DataAccessException;

    boolean deleteById(long id) throws DataAccessException;
}
