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

    /** Saves the visible table contents as a CSV file (user picks location). */
    public static void exportTableToCsv(Component parent,
                                        javax.swing.JTable table,
                                        String suggestedName) {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new java.io.File(suggestedName + ".csv"));
        if (chooser.showSaveDialog(parent)
                != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File file = chooser.getSelectedFile();
        int rows = table.getRowCount();
        int cols = table.getColumnCount();
        try (java.io.PrintWriter out = new java.io.PrintWriter(file,
                java.nio.charset.StandardCharsets.UTF_8)) {
            for (int r = 0; r <= rows; r++) {
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < cols; c++) {
                    if (c > 0) {
                        line.append(',');
                    }
                    String value = r == 0
                            ? table.getColumnName(c)
                            : String.valueOf(table.getValueAt(r - 1, c));
                    line.append(escapeCsv(value));
                }
                out.println(line);
            }
            info(parent, "Exported " + rows + " rows to "
                    + file.getAbsolutePath());
        } catch (Exception e) {
            error(parent, "Export failed: " + e.getMessage());
        }
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
