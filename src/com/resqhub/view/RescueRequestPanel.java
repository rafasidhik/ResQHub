package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.DisasterController;
import com.resqhub.controller.RescueRequestController;
import com.resqhub.controller.RescueTeamController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RoleType;
import com.resqhub.service.SessionManager;
import com.resqhub.model.RescueTeam;

/**
 * Rescue request operations screen.
 *
 * operational=true : officers/admins - assignment workflow enabled
 * operational=false: citizens - submission form only
 */
public class RescueRequestPanel extends JPanel {

    private final RescueRequestController controller = new RescueRequestController();
    private final DisasterController disasterController = new DisasterController();
    private final RescueTeamController teamController = new RescueTeamController();
    private final boolean operational;

    private final JTextField requesterField = new JTextField(16);
    private final JTextField contactField = new JTextField(12);
    private final JTextField locationField = new JTextField(16);
    private final JTextField peopleField = new JTextField(4);
    private final JTextField childrenField = new JTextField(4);
    private final JTextField elderlyField = new JTextField(4);
    private final JCheckBox lifeBox = new JCheckBox("Life-threatening");
    private final JCheckBox medicalBox = new JCheckBox("Medical emergency");
    private final JCheckBox trappedBox = new JCheckBox("Trapped under debris");
    private final JTextArea assistanceArea = new JTextArea(3, 18);
    private final JComboBox<DisasterOption> disasterCombo = new JComboBox<>();

    private final javax.swing.table.DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(RescueRequestController.tableHeaders());
    private final JTable table = new JTable(tableModel);

    private record DisasterOption(Long id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public RescueRequestPanel(boolean operational) {
        this.operational = operational;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildForm(), BorderLayout.WEST);
        add(buildQueueArea(), BorderLayout.CENTER);
        refreshDisasters();
        refreshQueue();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Report emergency"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Your name:"), gbc);
        gbc.gridx = 1; form.add(requesterField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Contact (10 digits):"), gbc);
        gbc.gridx = 1; form.add(contactField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Exact location:"), gbc);
        gbc.gridx = 1; form.add(locationField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("People:"), gbc);
        gbc.gridx = 1; form.add(peopleField, gbc);

        JPanel counts = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        counts.add(new JLabel("Children:"));
        counts.add(childrenField);
        counts.add(new JLabel("Elderly:"));
        counts.add(elderlyField);
        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Vulnerable:"), gbc);
        gbc.gridx = 1; form.add(counts, gbc);

        row++; gbc.gridx = 1; gbc.gridy = row; form.add(lifeBox, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; form.add(medicalBox, gbc);
        row++; gbc.gridx = 1; gbc.gridy = row; form.add(trappedBox, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Assistance needed:"), gbc);
        gbc.gridx = 1; form.add(new JScrollPane(assistanceArea), gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Disaster:"), gbc);
        gbc.gridx = 1; form.add(disasterCombo, gbc);

        JButton submitButton = new JButton("Submit rescue request");
        row++; gbc.gridx = 1; gbc.gridy = row; form.add(submitButton, gbc);
        submitButton.addActionListener(event -> submitRequest());

        return form;
    }

    private JPanel buildQueueArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.setBorder(BorderFactory.createTitledBorder(
                "Operations queue (sorted by priority)"));
        area.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton assignButton = new JButton("Assign team...");
        JButton progressButton = new JButton("Progress assignment...");
        JButton completeButton = new JButton("Complete assignment");
        JButton cancelButton = new JButton("Cancel request");
        JButton explainButton = new JButton("Why this priority?");
        JButton refreshButton = new JButton("Refresh");

        controls.add(assignButton);
        controls.add(progressButton);
        controls.add(completeButton);
        controls.add(cancelButton);
        controls.add(explainButton);
        controls.add(refreshButton);

        assignButton.setEnabled(operational);
        progressButton.setEnabled(operational);
        completeButton.setEnabled(operational);
        cancelButton.setEnabled(operational);

        JButton deleteButton = new JButton("Delete selected");
        deleteButton.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));
        controls.add(deleteButton);
        area.add(controls, BorderLayout.NORTH);

        assignButton.addActionListener(event -> showAssignDialog());
        progressButton.addActionListener(event -> showProgressDialog());
        completeButton.addActionListener(event -> completeSelected());
        cancelButton.addActionListener(event -> cancelSelected());
        explainButton.addActionListener(event -> explainPriority());
        refreshButton.addActionListener(event -> refreshQueue());
        deleteButton.addActionListener(event -> deleteSelected());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    explainPriority();
                }
            }
        });
        return area;
    }

    private void refreshDisasters() {
        try {
            disasterCombo.removeAllItems();
            for (Disaster disaster : disasterController.getActiveDisasters()) {
                disasterCombo.addItem(new DisasterOption(disaster.getId(),
                        "#" + disaster.getId() + " " + disaster.getTitle()));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private Long selectedRequestId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a request in the queue first");
            return null;
        }
        return (Long) tableModel.getValueAt(viewRow, 0);
    }

    private void submitRequest() {
        DisasterOption selected = (DisasterOption) disasterCombo.getSelectedItem();
        ActionResult result = controller.submitRequest(
                selected == null ? null : selected.id(),
                null,
                requesterField.getText(),
                contactField.getText(),
                locationField.getText(),
                peopleField.getText(),
                childrenField.getText(),
                elderlyField.getText(),
                lifeBox.isSelected(),
                medicalBox.isSelected(),
                trappedBox.isSelected(),
                assistanceArea.getText());

        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            requesterField.setText("");
            contactField.setText("");
            locationField.setText("");
            peopleField.setText("");
            childrenField.setText("");
            elderlyField.setText("");
            lifeBox.setSelected(false);
            medicalBox.setSelected(false);
            trappedBox.setSelected(false);
            assistanceArea.setText("");
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshQueue();
    }

    /** MODAL DIALOG choosing one of the AVAILABLE teams for the selected request. */
    private void showAssignDialog() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        List<RescueTeam> available;
        try {
            available = teamController.getAvailableTeams();
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return;
        }
        if (available.isEmpty()) {
            ViewUtil.info(this, "No teams are currently AVAILABLE");
            return;
        }

        final JDialog dialog = new JDialog(
                (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                "Assign team to request #" + requestId, true);
        JComboBox<String> teamCombo = new JComboBox<>();
        final List<Long> teamIds = new java.util.ArrayList<>();
        for (RescueTeam team : available) {
            teamIds.add(team.getId());
            teamCombo.addItem(RescueTeamController.toOption(team));
        }
        JButton goButton = new JButton("Assign");

        dialog.getContentPane().setLayout(new FlowLayout());
        dialog.getContentPane().add(teamCombo);
        dialog.getContentPane().add(goButton);

        goButton.addActionListener(event -> {
            long chosenTeamId = teamIds.get(teamCombo.getSelectedIndex());
            ActionResult result = controller.assignTeam(requestId, chosenTeamId);
            if (result.isSuccess()) {
                ViewUtil.info(dialog, result.getMessage());
                dialog.dispose();
                refreshQueue();
            } else {
                ViewUtil.error(dialog, result.getMessage());
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /** Modal EN_ROUTE / ON_SITE progression for the selected request's assignment. */
    private void showProgressDialog() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        long assignmentId = controller.getLatestAssignmentId(requestId);
        if (assignmentId < 0) {
            ViewUtil.info(this,
                    "Request #" + requestId + " has no assignment yet");
            return;
        }

        final JDialog dialog = new JDialog(
                (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                "Assignment #" + assignmentId, true);
        JComboBox<String> targetCombo = new JComboBox<>(new String[] {
                AssignmentStatus.EN_ROUTE.getLabel(),
                AssignmentStatus.ON_SITE.getLabel()});
        JTextField notesField = new JTextField(14);
        JButton goButton = new JButton("Update");

        dialog.getContentPane().setLayout(new FlowLayout());
        dialog.getContentPane().add(targetCombo);
        dialog.getContentPane().add(notesField);
        dialog.getContentPane().add(goButton);

        goButton.addActionListener(event -> {
            AssignmentStatus target = targetCombo.getSelectedIndex() == 0
                    ? AssignmentStatus.EN_ROUTE : AssignmentStatus.ON_SITE;
            ActionResult result = controller.progressAssignment(assignmentId,
                    target, notesField.getText());
            if (result.isSuccess()) {
                ViewUtil.info(dialog, result.getMessage());
                dialog.dispose();
            } else {
                ViewUtil.error(dialog, result.getMessage());
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void completeSelected() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        long assignmentId = controller.getLatestAssignmentId(requestId);
        if (assignmentId < 0) {
            ViewUtil.error(this, "No assignment found for request #" + requestId);
            return;
        }
        ActionResult result = controller.completeAssignment(assignmentId);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshQueue();
    }

    private void cancelSelected() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        ActionResult result = controller.cancelRequest(requestId);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshQueue();
    }

    private void explainPriority() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        ActionResult result = controller.explainPriority(requestId);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getData());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
    }

    private void deleteSelected() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Permanently delete request #" + requestId + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult result = controller.deleteRequest(requestId);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshQueue();
    }

    private void refreshQueue() {
        tableModel.setRowCount(0);
        if (!operational) {
            // Citizens submit emergencies; the operations queue is staff-only.
            return;
        }
        try {
            List<RescueRequest> pending = controller.getPendingQueue();
            for (RescueRequest request : pending) {
                tableModel.addRow(RescueRequestController.toRow(request));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }
}
