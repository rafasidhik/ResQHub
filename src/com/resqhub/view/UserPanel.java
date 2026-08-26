package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.resqhub.controller.AccountDeletionRequestController;
import com.resqhub.controller.ActionResult;
import com.resqhub.controller.UserController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.service.SessionManager;

/** ADMIN-only user administration: create staff accounts, unlock, status. */
public class UserPanel extends JPanel implements Refreshable {

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
    private final JTextField searchField = new JTextField(12);

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

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Search:"));
        row1.add(searchField);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        row1.add(searchButton);
        row1.add(showAllButton);

        JButton unlockButton = new JButton("Unlock selected");
        row1.add(unlockButton);

        JButton deleteButton = new JButton("Delete selected");
        deleteButton.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));
        row1.add(deleteButton);

        row1.add(new JLabel("Set status:"));
        JComboBox<AccountStatus> statusCombo =
                new JComboBox<>(new AccountStatus[] {
                        AccountStatus.ACTIVE, AccountStatus.INACTIVE});
        JButton applyButton = new JButton("Apply");
        row1.add(statusCombo);
        row1.add(applyButton);

        JButton refreshButton = new JButton("Refresh");
        row1.add(refreshButton);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton editButton = new JButton("Update selected");
        JButton resetButton = new JButton("Reset password...");
        JButton detailsButton = new JButton("View Details");
        JButton exportButton = new JButton("Export CSV");
        JButton deletionRequestsButton = new JButton("Deletion Requests");
        row2.add(editButton);
        row2.add(resetButton);
        row2.add(detailsButton);
        row2.add(exportButton);
        row2.add(deletionRequestsButton);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(row1);
        controls.add(row2);
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
        deleteButton.addActionListener(event -> deleteSelected());
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
        searchButton.addActionListener(event -> refreshTable());
        showAllButton.addActionListener(event -> {
            searchField.setText("");
            refreshTable();
        });
        editButton.addActionListener(event -> updateSelected());
        resetButton.addActionListener(event -> resetPassword());
        detailsButton.addActionListener(event -> viewDetails());
        deletionRequestsButton.addActionListener(
                event -> showDeletionRequestsDialog());
        exportButton.addActionListener(event ->
                ViewUtil.exportTableToCsv(this, table, "users"));

        return area;
    }

    /** Loads the selected user's profile and applies edits via dialogs. */
    private void updateSelected() {
        Long id = selectedUserId();
        if (id == null) {
            return;
        }
        try {
            User target = null;
            for (User candidate : controller.listUsers()) {
                if (candidate.getId().equals(id)) {
                    target = candidate;
                }
            }
            if (target == null) {
                ViewUtil.error(this, "User #" + id + " not found");
                return;
            }

            String fullName = JOptionPane.showInputDialog(this,
                    "Full name:", target.getFullName());
            if (fullName == null) {
                return;
            }
            String email = JOptionPane.showInputDialog(this,
                    "Email:", target.getEmail());
            if (email == null) {
                return;
            }
            String phone = JOptionPane.showInputDialog(this,
                    "Phone (optional):", target.getPhone());
            if (phone == null) {
                return;
            }
            Object roleChoice = JOptionPane.showInputDialog(this,
                    "Role:", "Update user #" + id,
                    JOptionPane.PLAIN_MESSAGE, null, RoleType.values(),
                    target.getRole());
            if (roleChoice == null) {
                return;
            }
            show(controller.updateUser(id, fullName, email, phone,
                    (RoleType) roleChoice));
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        refreshTable();
    }

    private void resetPassword() {
        Long id = selectedUserId();
        if (id == null) {
            return;
        }
        String newPassword = JOptionPane.showInputDialog(this,
                "New password for user #" + id + ":");
        if (newPassword == null || newPassword.isEmpty()) {
            return;
        }
        show(controller.resetPassword(id, newPassword));
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

    @Override
    public void refreshData() {
        refreshTable();
    }

    private void viewDetails() {
        Long id = selectedUserId();
        if (id == null) {
            return;
        }
        try {
            for (User candidate : controller.listUsers()) {
                if (candidate.getId().equals(id)) {
                    String text = "#" + candidate.getId() + "  "
                            + candidate.getUsername() + "\n"
                            + "Full name : " + candidate.getFullName() + "\n"
                            + "Email     : " + candidate.getEmail() + "\n"
                            + "Phone     : " + (candidate.getPhone() == null
                                    ? "-" : candidate.getPhone()) + "\n"
                            + "Role      : " + candidate.getRole().getLabel()
                            + "\n"
                            + "Status    : "
                            + candidate.getAccountStatus().getLabel() + "\n"
                            + "Last login: " + (candidate.getLastLogin() == null
                                    ? "never" : candidate.getLastLogin());
                    JOptionPane.showMessageDialog(this, text,
                            "User #" + id, JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    /** Admin dialog listing pending account-deletion requests. */
    private void showDeletionRequestsDialog() {
        AccountDeletionRequestController deletionCtrl =
                new AccountDeletionRequestController();
        List<com.resqhub.model.AccountDeletionRequest> pending =
                deletionCtrl.getPendingRequests();
        if (pending.isEmpty()) {
            ViewUtil.info(this, "No pending deletion requests");
            return;
        }

        String[] columns = {"ID", "User ID", "Requested"};
        javax.swing.table.DefaultTableModel model =
                ViewUtil.readOnlyModel(columns);
        for (com.resqhub.model.AccountDeletionRequest req : pending) {
            model.addRow(new Object[]{req.getId(), req.getUserId(),
                    req.getRequestedAt()});
        }
        JTable table = new JTable(model);
        table.setSelectionMode(
                javax.swing.ListSelectionModel.SINGLE_SELECTION);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton approveBtn = new JButton("Approve (delete user)");
        JButton denyBtn = new JButton("Deny");
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(approveBtn);
        buttons.add(denyBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(
                (java.awt.Frame) javax.swing.SwingUtilities
                        .getWindowAncestor(this),
                "Deletion Requests", true);
        dialog.setContentPane(panel);

        approveBtn.addActionListener(ev -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                ViewUtil.error(dialog, "Select a request first");
                return;
            }
            long reqId = (Long) model.getValueAt(row, 0);
            long targetUserId = (Long) model.getValueAt(row, 1);
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Approve deletion of user #" + targetUserId + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            ActionResult result = deletionCtrl.approveRequest(reqId, "");
            if (result.isSuccess()) {
                ViewUtil.info(dialog, result.getMessage());
                dialog.dispose();
                refreshTable();
            } else {
                ViewUtil.error(dialog, result.getMessage());
            }
        });

        denyBtn.addActionListener(ev -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                ViewUtil.error(dialog, "Select a request first");
                return;
            }
            long reqId = (Long) model.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Deny this deletion request?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            ActionResult result = deletionCtrl.denyRequest(reqId, "");
            if (result.isSuccess()) {
                ViewUtil.info(dialog, result.getMessage());
                dialog.dispose();
            } else {
                ViewUtil.error(dialog, result.getMessage());
            }
        });

        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void deleteSelected() {
        Long id = selectedUserId();
        if (id == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Permanently delete user #" + id + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult result = controller.deleteUser(id);
        show(result);
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String needle = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        try {
            for (User user : controller.listUsers()) {
                boolean matches = needle.isEmpty()
                        || user.getUsername().toLowerCase().contains(needle)
                        || user.getFullName().toLowerCase().contains(needle)
                        || user.getEmail().toLowerCase().contains(needle);
                if (matches) {
                    tableModel.addRow(UserController.toRow(user));
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }
}
