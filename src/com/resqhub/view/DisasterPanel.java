package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.DisasterController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.model.RoleType;
import com.resqhub.service.SessionManager;

/**
 * Disaster module: live status tiles, registration/edit form,
 * searchable records and a context action bar.
 */
public class DisasterPanel extends JPanel implements Refreshable {

    private final DisasterController controller = new DisasterController();

    private final JTextField titleField = new JTextField(18);
    private final JComboBox<DisasterType> typeCombo =
            new JComboBox<>(DisasterType.values());
    private final JComboBox<DisasterSeverity> severityCombo =
            new JComboBox<>(DisasterSeverity.values());
    private final JTextField locationField = new JTextField(18);
    private final JTextField populationField = new JTextField(8);
    private final JTextField startField = new JTextField(12);
    private final JTextField endField = new JTextField(12);
    private final JTextArea descriptionArea = new JTextArea(3, 18);

    private final javax.swing.table.DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(DisasterController.tableHeaders());
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> statusFilter =
            new JComboBox<>(new String[] {"All", "Reported", "Active",
                    "Contained", "Resolved"});

    private final JLabel activeTile = new JLabel("0");
    private final JLabel containedTile = new JLabel("0");
    private final JLabel resolvedTile = new JLabel("0");
    private final JLabel totalTile = new JLabel("0");

    private final JLabel selectedLabel = new JLabel(
            "No disaster selected - click a row");
    private final JComboBox<DisasterStatus> changeStatusCombo =
            new JComboBox<>();
    private static final String STATUS_SENTINEL = "-- choose --";

    private Long editingId = null;
    private final JButton saveChangesButton = new JButton("Save Changes");

    public DisasterPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);

        for (DisasterStatus status : DisasterStatus.values()) {
            changeStatusCombo.addItem(status);
        }
        changeStatusCombo.insertItemAt(null, 0);
        changeStatusCombo.setSelectedIndex(0);
        changeStatusCombo.addActionListener(e -> applyLifecycleChange());

        refreshData();
    }

    // ---------------------------------------------------------- header

    private JPanel buildHeader() {
        JLabel title = new JLabel("DISASTER MANAGEMENT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JButton refreshButton = new JButton("\u21bb Refresh");
        refreshButton.addActionListener(e -> refreshData());

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(refreshButton, BorderLayout.EAST);

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("ACTIVE", activeTile, new Color(170, 60, 30)));
        tiles.add(statTile("CONTAINED", containedTile, new Color(150, 110, 20)));
        tiles.add(statTile("RESOLVED", resolvedTile, new Color(40, 110, 40)));
        tiles.add(statTile("TOTAL", totalTile, new Color(60, 60, 60)));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(header, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        return north;
    }

    private JPanel statTile(String caption, JLabel valueLabel, Color color) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 26f));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setHorizontalAlignment(
                javax.swing.SwingConstants.CENTER);
        captionLabel.setFont(captionLabel.getFont().deriveFont(11f));
        captionLabel.setForeground(new Color(90, 90, 90));

        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setPreferredSize(new Dimension(140, 84));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(captionLabel);
        tile.add(Box.createVerticalGlue());
        return tile;
    }

    // --------------------------------------------- form | records split

    private JSplitPane buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildForm(), buildRecords());
        split.setDividerLocation(360);
        split.setResizeWeight(0.32);
        return split;
    }

    private JScrollPane buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "REGISTER / EDIT DISASTER"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; form.add(titleField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1; form.add(typeCombo, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Severity:"), gbc);
        gbc.gridx = 1; form.add(severityCombo, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1; form.add(locationField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Affected Population:"), gbc);
        gbc.gridx = 1; form.add(populationField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Start Date & Time:"), gbc);
        gbc.gridx = 1; form.add(startField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("End Date & Time:"), gbc);
        gbc.gridx = 1; form.add(endField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; form.add(new JScrollPane(descriptionArea), gbc);
        row++;

        JButton registerButton = new JButton("+ Register Disaster");
        gbc.gridx = 1; gbc.gridy = row; form.add(registerButton, gbc);
        registerButton.addActionListener(e -> registerDisaster());

        return new JScrollPane(form);
    }

    private JPanel buildRecords() {
        JPanel records = new JPanel(new BorderLayout(6, 6));
        records.setBorder(BorderFactory.createTitledBorder(
                "DISASTER RECORDS"));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchField);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        searchRow.add(searchButton);
        searchRow.add(showAllButton);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterRow.add(new JLabel("Filter Status:"));
        filterRow.add(statusFilter);
        JButton applyFilterButton = new JButton("Apply");
        filterRow.add(applyFilterButton);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.add(searchRow);
        northStack.add(filterRow);
        records.add(northStack, BorderLayout.NORTH);
        records.add(new JScrollPane(table), BorderLayout.CENTER);

        searchButton.addActionListener(e -> refreshTable());
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            refreshTable();
        });
        applyFilterButton.addActionListener(e -> refreshTable());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    editSelected(true);
                }
            }
        });
        return records;
    }

    // ------------------------------------------------------- action bar

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 4));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("ACTIONS"),
                BorderFactory.createEmptyBorder(2, 6, 4, 6)));
        bar.add(selectedLabel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton editButton = new JButton("Edit Selected");
        saveChangesButton.setEnabled(false);
        JButton closeButton = new JButton("Close / Resolve Now");
        JButton viewButton = new JButton("View Details");
        JButton exportButton = new JButton("Export CSV");
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));

        buttons.add(editButton);
        buttons.add(new JLabel("Change Status:"));
        buttons.add(changeStatusCombo);
        buttons.add(saveChangesButton);
        buttons.add(closeButton);
        buttons.add(viewButton);
        buttons.add(exportButton);
        buttons.add(deleteButton);
        bar.add(buttons, BorderLayout.CENTER);

        editButton.addActionListener(e -> editSelected(false));
        saveChangesButton.addActionListener(e -> saveChanges());
        closeButton.addActionListener(e -> closeSelected());
        viewButton.addActionListener(e -> viewDetails());
        exportButton.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, table, "disasters"));
        deleteButton.addActionListener(e -> deleteSelected());
        return bar;
    }

    // -------------------------------------------------------- handlers

    private void registerDisaster() {
        ActionResult result = controller.createDisaster(
                titleField.getText(),
                (DisasterType) typeCombo.getSelectedItem(),
                (DisasterSeverity) severityCombo.getSelectedItem(),
                locationField.getText(),
                populationField.getText(),
                startField.getText(),
                endField.getText(),
                descriptionArea.getText());
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            clearForm();
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshData();
    }

    /** Loads the selected row into the form. silent=true for double-click. */
    private void editSelected(boolean silent) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            if (!silent) {
                ViewUtil.error(this, "Select a disaster in the table first");
            }
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            for (Disaster candidate : controller.getAllDisasters()) {
                if (candidate.getId().equals(id)) {
                    titleField.setText(candidate.getTitle());
                    typeCombo.setSelectedItem(candidate.getDisasterType());
                    severityCombo.setSelectedItem(candidate.getSeverity());
                    locationField.setText(candidate.getLocation());
                    populationField.setText(
                            String.valueOf(candidate.getAffectedPopulation()));
                    startField.setText(candidate.getStartDateTime()
                            .format(java.time.format.DateTimeFormatter
                                    .ofPattern("yyyy-MM-dd HH:mm")));
                    endField.setText(candidate.getEndDateTime() == null ? ""
                            : candidate.getEndDateTime()
                                    .format(java.time.format.DateTimeFormatter
                                            .ofPattern("yyyy-MM-dd HH:mm")));
                    descriptionArea.setText(candidate.getDescription());
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return;
        }
        editingId = id;
        saveChangesButton.setEnabled(true);
        selectedLabel.setText("Editing disaster #" + id
                + " - modify the form and press Save Changes");
        if (!silent) {
            ViewUtil.info(this, "Loaded disaster #" + id + " into the form");
        }
    }

    private void saveChanges() {
        if (editingId == null) {
            return;
        }
        ActionResult result = controller.updateDisaster(editingId,
                titleField.getText(),
                (DisasterType) typeCombo.getSelectedItem(),
                (DisasterSeverity) severityCombo.getSelectedItem(),
                locationField.getText(),
                populationField.getText(),
                startField.getText(),
                endField.getText(),
                descriptionArea.getText());
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            editingId = null;
            saveChangesButton.setEnabled(false);
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshData();
    }

    private void viewDetails() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a disaster in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            for (Disaster candidate : controller.getAllDisasters()) {
                if (candidate.getId().equals(id)) {
                    StringBuilder text = new StringBuilder();
                    text.append("#").append(candidate.getId()).append(" ")
                            .append(candidate.getTitle()).append("\n")
                            .append("Type     : ")
                            .append(candidate.getDisasterType().getLabel())
                            .append("\n")
                            .append("Severity : ")
                            .append(candidate.getSeverity().getLabel())
                            .append("\n")
                            .append("Status   : ")
                            .append(candidate.getStatus().getLabel())
                            .append("\n")
                            .append("Location : ")
                            .append(candidate.getLocation()).append("\n")
                            .append("Population affected: ")
                            .append(candidate.getAffectedPopulation())
                            .append("\n")
                            .append("Started  : ")
                            .append(candidate.getStartDateTime())
                            .append("\n")
                            .append("Ended    : ")
                            .append(candidate.getEndDateTime() == null
                                    ? "-" : candidate.getEndDateTime())
                            .append("\n\n")
                            .append(candidate.getDescription() == null
                                    || candidate.getDescription().isEmpty()
                                    ? "(no description)"
                                    : candidate.getDescription());
                    JOptionPane.showMessageDialog(this, text.toString(),
                            "Disaster #" + id,
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    /** Early resolution from any non-resolved state (skips lifecycle). */
    private void closeSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a disaster in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Resolve disaster #" + id + " immediately?",
                "Confirm close", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult result = controller.closeDisaster(id);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshData();
    }

    /** Forward-only lifecycle move on the selected disaster. */
    private void applyLifecycleChange() {
        Object chosen = changeStatusCombo.getSelectedItem();
        if (chosen == null) {
            return;
        }
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a disaster in the table first");
            changeStatusCombo.setSelectedIndex(0);
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        DisasterStatus target = (DisasterStatus) chosen;
        int choice = JOptionPane.showConfirmDialog(this,
                "Move disaster #" + id + " to "
                        + target.getLabel() + "?",
                "Confirm status change", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            changeStatusCombo.setSelectedIndex(0);
            return;
        }
        ActionResult result = controller.updateStatus(id, target);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        changeStatusCombo.setSelectedIndex(0);
        refreshData();
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a disaster in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Permanently delete disaster #" + id + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult result = controller.deleteDisaster(id);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshData();
    }

    private void clearForm() {
        titleField.setText("");
        locationField.setText("");
        populationField.setText("");
        startField.setText("");
        endField.setText("");
        descriptionArea.setText("");
        editingId = null;
        saveChangesButton.setEnabled(false);
    }

    // --------------------------------------------------------- refresh

    @Override
    public void refreshData() {
        refreshTable();
    }

    /** Reloads rows honouring search + status filter and updates tiles. */
    private void refreshTable() {
        tableModel.setRowCount(0);
        int active = 0;
        int contained = 0;
        int resolved = 0;
        int total = 0;
        String needle = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        Object filter = statusFilter.getSelectedItem();
        try {
            for (Disaster disaster : controller.getAllDisasters()) {
                total++;
                switch (disaster.getStatus()) {
                    case ACTIVE -> active++;
                    case CONTAINED -> contained++;
                    case RESOLVED -> resolved++;
                    default -> { }
                }
                boolean matchesNeedle = needle.isEmpty()
                        || disaster.getTitle().toLowerCase().contains(needle)
                        || disaster.getLocation().toLowerCase()
                                .contains(needle);
                boolean matchesStatus = "All".equals(filter)
                        || disaster.getStatus().getLabel()
                                .equalsIgnoreCase(String.valueOf(filter));
                if (matchesNeedle && matchesStatus) {
                    tableModel.addRow(DisasterController.toRow(disaster));
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        activeTile.setText(String.valueOf(active));
        containedTile.setText(String.valueOf(contained));
        resolvedTile.setText(String.valueOf(resolved));
        totalTile.setText(String.valueOf(total));
        selectedLabel.setText(editingId == null
                ? "No disaster selected - click a row"
                : "Editing disaster #" + editingId);
    }
}
