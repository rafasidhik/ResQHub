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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.UserController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;

/** ADMIN-only user administration: create staff accounts, unlock, status. */
public class UserPanel extends JPanel {

    private final UserController controller = new UserController();

    private final JTextField usernameField = new JTextField(16);
    private final JTextField passwordField = new JTextField(16);
    private final JTextField fullNameField = new JTextField(16);
    private final JTextField emailField = new JTextField(16);
    private final JTextField phoneField = new JTextField(16);
    private final JComboBox<RoleType> roleCombo =
            new JComboBox<>(RoleType.values());

    private final javax.swing.table.DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(UserController.tableHeaders());
    private final JTable table = new JTable(tableModel);

    public UserPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildForm(), BorderLayout.WEST);
        add(buildTableArea(), BorderLayout.CENTER);
        refreshTable();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createTitledBorder(
                "Create staff account"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addRow(form, gbc, row, "Username:", usernameField);
        row = addRow(form, gbc, row, "Password:", passwordField);
        row = addRow(form, gbc, row, "Full name:", fullNameField);
        row = addRow(form, gbc, row, "Email:", emailField);
        row = addRow(form, gbc, row, "Phone (optional):", phoneField);

        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1; form.add(roleCombo, gbc);
        row++;

        JButton createButton = new JButton("Create account");
        gbc.gridx = 1; gbc.gridy = row; form.add(createButton, gbc);
        createButton.addActionListener(event -> createUser());

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
        JButton unlockButton = new JButton("Unlock selected");
        controls.add(unlockButton);

        controls.add(new JLabel("Set status:"));
        JComboBox<AccountStatus> statusCombo =
                new JComboBox<>(new AccountStatus[] {
                        AccountStatus.ACTIVE, AccountStatus.INACTIVE});
        JButton applyButton = new JButton("Apply");
        controls.add(statusCombo);
        controls.add(applyButton);

        JButton refreshButton = new JButton("Refresh");
        controls.add(refreshButton);
        area.add(controls, BorderLayout.NORTH);

        unlockButton.addActionListener(event -> {
            Long id = selectedUserId();
            if (id == null) {
                return;
            }
            ActionResult result = controller.unlockUser(id);
            show(result);
            refreshTable();
        });
        applyButton.addActionListener(event -> {
            Long id = selectedUserId();
            if (id == null) {
                return;
            }
            ActionResult result = controller.setStatus(id,
                    (AccountStatus) statusCombo.getSelectedItem());
            show(result);
            refreshTable();
        });
        refreshButton.addActionListener(event -> refreshTable());

        return area;
    }

    private Long selectedUserId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a user in the table first");
            return null;
        }
        return (Long) tableModel.getValueAt(viewRow, 0);
    }

    private void show(ActionResult result) {
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
    }

    private void createUser() {
        ActionResult result = controller.registerUser(
                usernameField.getText(), passwordField.getText(),
                fullNameField.getText(), emailField.getText(),
                phoneField.getText(),
                (RoleType) roleCombo.getSelectedItem());
        show(result);
        if (result.isSuccess()) {
            usernameField.setText("");
            passwordField.setText("");
            fullNameField.setText("");
            emailField.setText("");
            phoneField.setText("");
        }
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<User> users = controller.listUsers();
            for (User user : users) {
                tableModel.addRow(UserController.toRow(user));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }
}
