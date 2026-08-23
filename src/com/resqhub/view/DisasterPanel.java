package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

/** Disaster management screen: register, search, close. */
public class DisasterPanel extends JPanel implements Refreshable {

    private final DisasterController controller = new DisasterController();

    private final JTextField titleField = new JTextField(20);
    private final JComboBox<DisasterType> typeCombo =
            new JComboBox<>(DisasterType.values());
    private final JComboBox<DisasterSeverity> severityCombo =
            new JComboBox<>(DisasterSeverity.values());
    private final JTextField locationField = new JTextField(20);
    private final JTextField populationField = new JTextField(8);
    private final JTextField startField = new JTextField(12);
    private final JTextField endField = new JTextField(12);
    private final JTextArea descriptionArea = new JTextArea(3, 20);
    private final JTextField searchField = new JTextField(16);

    private final javax.swing.table.DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(DisasterController.tableHeaders());
    private final JTable table = new JTable(tableModel);

    private Long editingId = null;
    private final JButton saveChangesButton = new JButton("Save changes");

    public DisasterPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildForm(), BorderLayout.WEST);
        add(buildTableArea(), BorderLayout.CENTER);
        refreshTable();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; form.add(titleField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1; form.add(typeCombo, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Severity:"), gbc);
        gbc.gridx = 1; form.add(severityCombo, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1; form.add(locationField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Affected population:"), gbc);
        gbc.gridx = 1; form.add(populationField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Start (yyyy-MM-dd HH:mm):"), gbc);
        gbc.gridx = 1; form.add(startField, gbc);
        startField.setText(java.time.LocalDateTime.now()
                .format(com.resqhub.util.InputParser.DATE_TIME_FORMAT));

        row++; gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("End (optional):"), gbc);
        gbc.gridx = 1; form.add(endField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; form.add(new JScrollPane(descriptionArea), gbc);

        JButton createButton = new JButton("Register disaster");
        row++; gbc.gridx = 1; gbc.gridy = row; form.add(createButton, gbc);
        createButton.addActionListener(event -> createDisaster());

        return form;
    }

    private JPanel buildTableArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Search:"));
        controls.add(searchField);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show all");
        JButton editButton = new JButton("Edit selected");
        saveChangesButton.setEnabled(false);
        JButton closeDisasterButton = new JButton("Close selected disaster");
        JButton exportButton = new JButton("Export CSV");
        JButton deleteButton = new JButton("Delete selected");
        deleteButton.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));

        controls.add(new JLabel("Set status:"));
        JComboBox<DisasterStatus> statusCombo =
                new JComboBox<>(new DisasterStatus[] {
                        DisasterStatus.ACTIVE, DisasterStatus.CONTAINED,
                        DisasterStatus.RESOLVED});
        JButton applyStatusButton = new JButton("Apply");

        controls.add(searchButton);
        controls.add(showAllButton);
        controls.add(statusCombo);
        controls.add(applyStatusButton);
        controls.add(editButton);
        controls.add(saveChangesButton);
        controls.add(closeDisasterButton);
        controls.add(exportButton);
        controls.add(deleteButton);
        area.add(controls, BorderLayout.NORTH);

        searchButton.addActionListener(event -> refreshTable());
        showAllButton.addActionListener(event -> {
            searchField.setText("");
            refreshTable();
        });
        closeDisasterButton.addActionListener(event -> closeSelected());
        deleteButton.addActionListener(event -> deleteSelected());
        applyStatusButton.addActionListener(event -> applyStatus(
                (DisasterStatus) statusCombo.getSelectedItem()));
        editButton.addActionListener(event -> editSelected());
        saveChangesButton.addActionListener(event -> saveChanges());
        exportButton.addActionListener(event ->
                ViewUtil.exportTableToCsv(this, table, "disasters"));

        // MouseAdapter demonstrates the adapter-class idiom: we only need one click event
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                fillFormFromSelection();
            }
        });
        return area;
    }

    /** Fills the form with the clicked row so it can be edited and saved. */
    private void fillFormFromSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            for (Disaster disaster : controller.getAllDisasters()) {
                if (disaster.getId().equals(id)) {
                    titleField.setText(disaster.getTitle());
                    typeCombo.setSelectedItem(disaster.getDisasterType());
                    severityCombo.setSelectedItem(disaster.getSeverity());
                    locationField.setText(disaster.getLocation());
                    populationField.setText(
                            String.valueOf(disaster.getAffectedPopulation()));
                    startField.setText(disaster.getStartDateTime() == null ? ""
                            : disaster.getStartDateTime()
                                    .format(com.resqhub.util.InputParser.DATE_TIME_FORMAT));
                    endField.setText(disaster.getEndDateTime() == null ? ""
                            : disaster.getEndDateTime()
                                    .format(com.resqhub.util.InputParser.DATE_TIME_FORMAT));
                    descriptionArea.setText(disaster.getDescription());
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void createDisaster() {
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
            refreshTable();
        } else {
            ViewUtil.error(this, result.getMessage());
        }
    }

    private void closeSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a disaster in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        ActionResult result = controller.closeDisaster(id);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    @Override
    public void refreshData() {
        refreshTable();
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a disaster in the table first");
            return;
        }
        fillFormFromSelection();
        editingId = (Long) tableModel.getValueAt(viewRow, 0);
        saveChangesButton.setEnabled(true);
        ViewUtil.info(this, "Editing disaster #" + editingId
                + " - change the form and press Save changes");
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
            clearEditMode();
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    private void clearEditMode() {
        editingId = null;
        saveChangesButton.setEnabled(false);
    }

    private void applyStatus(DisasterStatus newStatus) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a disaster in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        ActionResult result = controller.updateStatus(id, newStatus);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
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
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<Disaster> disasters = controller.search(searchField.getText());
            for (Disaster disaster : disasters) {
                tableModel.addRow(DisasterController.toRow(disaster));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    /** Lets other panels trigger a reload when data may have changed. */
    public void onExternalDataChanged() {
        refreshTable();
    }
}
