package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.ReportController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Disaster;
import com.resqhub.model.ReportFilters;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;

/**
 * Reports &amp; Analytics screen. Lets the user pick a report family,
 * apply optional filters (disaster, status, priority, location, blood
 * group, resource category) and generate an aggregated view backed by
 * SQL (COUNT / SUM / AVG / MIN / MAX / GROUP BY / HAVING / JOIN). The
 * top stat tiles mirror the dashboard overview; the body shows a
 * textual summary plus the result table, both exportable to CSV.
 */
public class ReportPanel extends JPanel implements Refreshable {

    private final ReportController controller = new ReportController();

    private final JComboBox<ReportType> typeCombo = new JComboBox<>(
            ReportType.values());
    private final JComboBox<String> disasterCombo = new JComboBox<>(
            new String[]{"All Disasters"});
    private final JComboBox<String> statusCombo = new JComboBox<>(
            new String[]{"All Statuses", "PENDING", "ASSIGNED",
                    "IN_PROGRESS", "RESCUED", "COMPLETED", "ACTIVE",
                    "RESOLVED", "RECEIVED", "DISTRIBUTED"});
    private final JComboBox<String> priorityCombo = new JComboBox<>(
            new String[]{"All Priorities", "CRITICAL", "HIGH", "MEDIUM",
                    "LOW"});
    private final JComboBox<String> bloodCombo = new JComboBox<>(
            new String[]{"All Blood Groups", "O+", "A+", "B+", "AB+",
                    "O-", "A-", "B-", "AB-"});
    private final JTextField locationField = new JTextField(16);
    private final JTextField resourceField = new JTextField(12);

    private JLabel activeDisasterTile = new JLabel("0");
    private JLabel pendingTile = new JLabel("0");
    private JLabel volunteersTile = new JLabel("0");
    private JLabel donorsTile = new JLabel("0");

    private JTable table;
    private final JTextArea summaryArea = new JTextArea(10, 40);
    private ReportResult currentResult;
    private ReportType currentType = ReportType.OVERVIEW;

    public ReportPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        refreshData();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("REPORTS & ANALYTICS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("ACTIVE DISASTERS", activeDisasterTile,
                new Color(150, 30, 30)));
        tiles.add(statTile("PENDING RESCUES", pendingTile,
                new Color(200, 130, 20)));
        tiles.add(statTile("VOLUNTEERS AVAILABLE", volunteersTile,
                new Color(40, 120, 60)));
        tiles.add(statTile("TOTAL DONORS", donorsTile,
                new Color(60, 90, 150)));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(title, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        return north;
    }

    private JPanel statTile(String caption, JLabel valueLabel,
                            Color color) {
        valueLabel.setFont(
                valueLabel.getFont().deriveFont(Font.BOLD, 24f));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(
                javax.swing.SwingConstants.CENTER);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setHorizontalAlignment(
                javax.swing.SwingConstants.CENTER);
        captionLabel.setFont(captionLabel.getFont().deriveFont(10f));
        captionLabel.setForeground(new Color(90, 90, 90));

        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setPreferredSize(new Dimension(150, 80));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(captionLabel);
        tile.add(Box.createVerticalGlue());
        return tile;
    }

    private JPanel buildCenter() {
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controlRow.add(new JLabel("Report:"));
        controlRow.add(typeCombo);
        JButton runButton = new JButton("Generate Report");
        runButton.addActionListener(e -> runReport());
        controlRow.add(runButton);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filterRow.add(new JLabel("Disaster:"));
        filterRow.add(disasterCombo);
        filterRow.add(new JLabel("Status:"));
        filterRow.add(statusCombo);
        filterRow.add(new JLabel("Priority:"));
        filterRow.add(priorityCombo);
        filterRow.add(new JLabel("Blood:"));
        filterRow.add(bloodCombo);
        filterRow.add(new JLabel("Location:"));
        filterRow.add(locationField);
        filterRow.add(new JLabel("Category:"));
        filterRow.add(resourceField);
        JButton clearButton = new JButton("Clear Filters");
        clearButton.addActionListener(e -> clearFilters());
        filterRow.add(clearButton);

        JButton exportButton = new JButton("Export CSV");
        exportButton.addActionListener(e -> exportCsv());
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        actionRow.add(exportButton);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(controlRow);
        controls.add(filterRow);
        controls.add(actionRow);

        table = new JTable(ViewUtil.readOnlyModel(new String[]{"Result"}));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder(
                "REPORT RESULTS"));

        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(BorderFactory.createTitledBorder(
                "AGGREGATE SUMMARY"));

        JPanel body = new JPanel(new BorderLayout(6, 6));
        body.add(summaryScroll, BorderLayout.NORTH);
        body.add(tableScroll, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(controls, BorderLayout.NORTH);
        center.add(body, BorderLayout.CENTER);
        return center;
    }

    private void runReport() {
        ReportType type = (ReportType) typeCombo.getSelectedItem();
        ReportFilters filters = collectFilters();
        ActionResult result = controller.generateReport(type, filters);
        if (!result.isSuccess()) {
            ViewUtil.error(this, result.getMessage());
            return;
        }
        currentResult = result.getData();
        currentType = type;
        render(currentResult);
    }

    private ReportFilters collectFilters() {
        Long disasterId = null;
        Object sel = disasterCombo.getSelectedItem();
        if (sel != null && !"All Disasters".equals(sel)) {
            String label = sel.toString();
            int dash = label.indexOf(" #");
            if (dash > 0) {
                try {
                    disasterId = Long.parseLong(label.substring(dash + 2));
                } catch (NumberFormatException ignored) {
                    disasterId = null;
                }
            }
        }
        String status = comboValue(statusCombo, "All Statuses");
        String priority = comboValue(priorityCombo, "All Priorities");
        String blood = comboValue(bloodCombo, "All Blood Groups");
        String location = textOrNull(locationField.getText());
        String resource = textOrNull(resourceField.getText());
        return new ReportFilters(disasterId, status, priority, location,
                null, null, blood, resource);
    }

    private String comboValue(JComboBox<String> combo, String allLabel) {
        Object sel = combo.getSelectedItem();
        if (sel == null || allLabel.equals(sel)) {
            return null;
        }
        return sel.toString();
    }

    private String textOrNull(String text) {
        String t = text == null ? "" : text.trim();
        return t.isEmpty() ? null : t;
    }

    private void clearFilters() {
        disasterCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        priorityCombo.setSelectedIndex(0);
        bloodCombo.setSelectedIndex(0);
        locationField.setText("");
        resourceField.setText("");
    }

    private void render(ReportResult result) {
        DefaultTableModel model = ViewUtil.readOnlyModel(result.headers());
        for (Object[] row : result.rows()) {
            model.addRow(row);
        }
        table.setModel(model);

        StringBuilder sb = new StringBuilder();
        sb.append("Report: ").append(result.title()).append("\n\n");
        for (String line : result.summaryLines()) {
            sb.append(line).append("\n");
        }
        if (result.sqlNote() != null && !result.sqlNote().isEmpty()) {
            sb.append("\nSQL: ").append(result.sqlNote()).append("\n");
        }
        summaryArea.setText(sb.toString());
    }

    private void exportCsv() {
        if (currentResult == null || table.getRowCount() == 0) {
            ViewUtil.info(this, "Nothing to export yet - run a report first.");
            return;
        }
        ViewUtil.exportTableToCsv(this, table, controller.csvName(currentType));
    }

    private void refreshOverview() {
        ActionResult result = controller.generateReport(ReportType.OVERVIEW,
                ReportFilters.empty());
        if (!result.isSuccess()) {
            return;
        }
        ReportResult overview = result.getData();
        for (Object[] row : overview.rows()) {
            String metric = String.valueOf(row[0]);
            String value = String.valueOf(row[1]);
            switch (metric) {
                case "ACTIVE DISASTERS" -> activeDisasterTile.setText(value);
                case "PENDING RESCUES" -> pendingTile.setText(value);
                case "VOLUNTEERS AVAILABLE" -> volunteersTile.setText(value);
                case "TOTAL DONORS" -> donorsTile.setText(value);
                default -> {
                }
            }
        }
        if (currentResult == null) {
            render(overview);
        }
    }

    @Override
    public void refreshData() {
        try {
            List<Disaster> disasters = controller.getDisasters();
            disasterCombo.removeAllItems();
            disasterCombo.addItem("All Disasters");
            for (Disaster d : disasters) {
                disasterCombo.addItem(d.getTitle() + " #" + d.getId());
            }
            disasterCombo.setSelectedIndex(0);
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        refreshOverview();
    }
}
