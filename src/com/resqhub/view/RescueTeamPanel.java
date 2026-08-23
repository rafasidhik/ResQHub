package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.RescueTeamController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RoleType;
import com.resqhub.model.TeamType;
import com.resqhub.service.SessionManager;

/** Rescue team screen: registration + availability control. */
public class RescueTeamPanel extends JPanel implements Refreshable {

    private final RescueTeamController controller = new RescueTeamController();

    private final JTextField nameField = new JTextField(18);
    private final JComboBox<TeamType> typeCombo = new JComboBox<>(TeamType.values());
    private final JTextField leaderField = new JTextField(18);
    private final JTextField contactField = new JTextField(12);
    private final JTextField memberCountField = new JTextField(5);
    private final JTextField skillsField = new JTextField(18);
    private final JTextField equipmentField = new JTextField(18);
    private final JTextField baseField = new JTextField(18);

    private final javax.swing.table.DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(RescueTeamController.tableHeaders());
    private final JTable table = new JTable(tableModel);

    public RescueTeamPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildForm(), BorderLayout.WEST);
        add(buildTableArea(), BorderLayout.CENTER);
        refreshTable();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createTitledBorder("Register rescue team"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addRow(form, gbc, row, "Team name:", nameField);
        row = addRow(form, gbc, row, "Type:", typeCombo);
        row = addRow(form, gbc, row, "Leader:", leaderField);
        row = addRow(form, gbc, row, "Contact (10 digits):", contactField);
        row = addRow(form, gbc, row, "Members:", memberCountField);
        row = addRow(form, gbc, row, "Skills:", skillsField);
        row = addRow(form, gbc, row, "Equipment:", equipmentField);
        row = addRow(form, gbc, row, "Base location:", baseField);

        JButton registerButton = new JButton("Register team");
        gbc.gridx = 1; gbc.gridy = row; form.add(registerButton, gbc);
        registerButton.addActionListener(event -> registerTeam());

        return form;
    }

    private int addRow(JPanel form, GridBagConstraints gbc, int row,
                       String label, javax.swing.JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel(label), gbc);
        gbc.gridx = 1; form.add(field, gbc);
        return row + 1;
    }

    private JPanel buildTableArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Set selected team availability:"));
        JComboBox<AvailabilityStatus> statusCombo =
                new JComboBox<>(AvailabilityStatus.values());
        JButton applyButton = new JButton("Apply");
        controls.add(statusCombo);
        controls.add(applyButton);

        JButton refreshButton = new JButton("Refresh");
        controls.add(refreshButton);

        JButton deleteButton = new JButton("Delete selected");
        deleteButton.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));
        controls.add(deleteButton);
        area.add(controls, BorderLayout.NORTH);

        applyButton.addActionListener(event -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                ViewUtil.error(this, "Select a team in the table first");
                return;
            }
            Long id = (Long) tableModel.getValueAt(viewRow, 0);
            ActionResult result = controller.setAvailability(id,
                    (AvailabilityStatus) statusCombo.getSelectedItem());
            if (result.isSuccess()) {
                ViewUtil.info(this, result.getMessage());
            } else {
                ViewUtil.error(this, result.getMessage());
            }
            refreshTable();
        });
        refreshButton.addActionListener(event -> refreshTable());
        deleteButton.addActionListener(event -> deleteSelected());

        return area;
    }

    @Override
    public void refreshData() {
        refreshTable();
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a team in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Permanently delete team #" + id + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult result = controller.deleteTeam(id);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    private void registerTeam() {
        ActionResult result = controller.registerTeam(
                nameField.getText(),
                (TeamType) typeCombo.getSelectedItem(),
                leaderField.getText(),
                contactField.getText(),
                memberCountField.getText(),
                skillsField.getText(),
                equipmentField.getText(),
                baseField.getText());
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            nameField.setText("");
            leaderField.setText("");
            contactField.setText("");
            memberCountField.setText("");
            skillsField.setText("");
            equipmentField.setText("");
            baseField.setText("");
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<RescueTeam> teams = controller.getAllTeams();
            for (RescueTeam team : teams) {
                tableModel.addRow(RescueTeamController.toRow(team));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }
}
