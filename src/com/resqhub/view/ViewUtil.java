package com.resqhub.view;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import java.awt.Component;

/** Shared Swing helpers used by every screen. */
public final class ViewUtil {

    private ViewUtil() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message,
                "ResQHub", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message,
                "ResQHub", JOptionPane.ERROR_MESSAGE);
    }

    /** Non-editable table model for read-only lists. */
    public static DefaultTableModel readOnlyModel(String[] headers) {
        return new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
