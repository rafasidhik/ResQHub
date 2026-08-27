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
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RequestStatus;
import com.resqhub.model.RescueAssignment;
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
public class RescueRequestPanel extends JPanel implements Refreshable {

    private final RescueRequestController controller = new RescueRequestController();
    private final DisasterController disasterController = new DisasterController();
    private final RescueTeamController teamController = new RescueTeamController();
    private final boolean operational;

    private final JTextField requesterField = new JTextField(16);
    private final JTextField contactField = new JTextField(12);
    private final JTextField locationField = new JTextField(16);
    private final JTextField searchField = new JTextField(14);
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

    private final JComboBox<String> filterCombo = new JComboBox<>(
            new String[] {"Pending", "Under Review", "Assigned",
                    "In Progress", "Rescued", "Cancelled", "All"});
    private Long editingId = null;
    private Long editingVictimId = null;
    private final JButton saveChangesButton = new JButton("Save changes");

    private final JLabel totalTile = new JLabel("0");
    private final JLabel pendingTile = new JLabel("0");
    private final JLabel criticalTile = new JLabel("0");
    private final JLabel assignedTile = new JLabel("0");
    private final JLabel rescuedTile = new JLabel("0");

    private record DisasterOption(Long id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public RescueRequestPanel(boolean operational) {
        this.operational = operational;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.WEST);
        add(buildQueueArea(), BorderLayout.CENTER);
        refreshDisasters();
        refreshQueue();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("RESCUE REQUESTS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("TOTAL", totalTile, new Color(60, 60, 60)));
        tiles.add(statTile("PENDING", pendingTile, new Color(170, 130, 20)));
        tiles.add(statTile("CRITICAL", criticalTile, new Color(170, 40, 40)));
        tiles.add(statTile("ASSIGNED", assignedTile, new Color(40, 100, 160)));
        tiles.add(statTile("RESCUED", rescuedTile, new Color(40, 110, 40)));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(title, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        return north;
    }

    private JPanel statTile(String caption, JLabel valueLabel, Color color) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 24f));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        captionLabel.setFont(captionLabel.getFont().deriveFont(11f));
        captionLabel.setForeground(new Color(90, 90, 90));

        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setPreferredSize(new Dimension(110, 80));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(captionLabel);
        tile.add(Box.createVerticalGlue());
        return tile;
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
        JButton reviewButton = new JButton("Start Review");
        JButton unreviewButton = new JButton("Back to Pending");
        JButton explainButton = new JButton("Why this priority?");
        JButton refreshButton = new JButton("Refresh");

        controls.add(assignButton);
        controls.add(progressButton);
        controls.add(completeButton);
        controls.add(cancelButton);
        controls.add(reviewButton);
        controls.add(unreviewButton);
        controls.add(explainButton);
        controls.add(refreshButton);

        assignButton.setEnabled(operational);
        progressButton.setEnabled(operational);
        completeButton.setEnabled(operational);
        cancelButton.setEnabled(operational);
        reviewButton.setEnabled(operational);
        unreviewButton.setEnabled(operational);

        JButton deleteButton = new JButton("Delete selected");
        deleteButton.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));
        controls.add(deleteButton);

        controls.add(new javax.swing.JLabel("Show:"));
        controls.add(filterCombo);
        controls.add(new javax.swing.JLabel("Search:"));
        controls.add(searchField);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        controls.add(searchButton);
        controls.add(showAllButton);
        JButton abortButton = new JButton("Abort mission");
        JButton historyButton = new JButton("History...");
        JButton editButton = new JButton("Edit selected");
        saveChangesButton.setEnabled(false);
        JButton exportButton = new JButton("Export CSV");
        controls.add(abortButton);
        controls.add(historyButton);
        controls.add(editButton);
        controls.add(saveChangesButton);
        controls.add(exportButton);

        abortButton.setEnabled(operational);
        historyButton.setEnabled(operational);
        editButton.setEnabled(operational);
        area.add(controls, BorderLayout.NORTH);

        assignButton.addActionListener(event -> showAssignDialog());
        progressButton.addActionListener(event -> showProgressDialog());
        completeButton.addActionListener(event -> completeSelected());
        cancelButton.addActionListener(event -> cancelSelected());
        reviewButton.addActionListener(event -> startReview());
        unreviewButton.addActionListener(event -> unreview());
        explainButton.addActionListener(event -> explainPriority());
        refreshButton.addActionListener(event -> refreshQueue());
        deleteButton.addActionListener(event -> deleteSelected());
        filterCombo.addActionListener(event -> refreshQueue());
        searchButton.addActionListener(event -> refreshQueue());
        showAllButton.addActionListener(event -> {
            searchField.setText("");
            filterCombo.setSelectedItem("Pending");
            refreshQueue();
        });
        abortButton.addActionListener(event -> abortSelected());
        historyButton.addActionListener(event -> showHistory());
        editButton.addActionListener(event -> editSelected());
        saveChangesButton.addActionListener(event -> saveChanges());
        exportButton.addActionListener(event ->
                ViewUtil.exportTableToCsv(this, table, "rescue_requests"));

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

    /** After a status change: widen the filter to All and re-highlight
     *  the row so it never "vanishes" right after an action. */
    private void revealRequest(Long requestId) {
        if (!"All".equals(filterCombo.getSelectedItem())) {
            filterCombo.setSelectedItem("All");
        }
        refreshQueue();
        if (requestId == null) {
            return;
        }
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (requestId.equals(tableModel.getValueAt(i, 0))) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(
                        table.getCellRect(i, 0, true));
                break;
            }
        }
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
            Object data = result.getData();
            if (data instanceof RescueRequest req
                    && req.getPriority() == PriorityLevel.CRITICAL) {
                JOptionPane.showMessageDialog(this,
                        "\u26A0 CRITICAL RESCUE REQUEST\n"
                                + "Location: " + req.getLocation() + "\n"
                                + "People: " + req.getPeopleCount() + "\n"
                                + "Life-threatening: "
                                + (req.isLifeThreatening() ? "Yes" : "No")
                                + "\nMedical emergency: "
                                + (req.isMedicalEmergency() ? "Yes" : "No"),
                        "CRITICAL ALERT",
                        JOptionPane.WARNING_MESSAGE);
            }
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
                revealRequest(requestId);
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
        revealRequest(requestId);
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
        revealRequest(requestId);
    }

    private void startReview() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        ActionResult result = controller.startReview(requestId);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshQueue();
    }

    private void unreview() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        ActionResult result = controller.unreview(requestId);
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

    @Override
    public void refreshData() {
        refreshDisasters();
        refreshQueue();
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
        int total = 0, pending = 0, critical = 0, assigned = 0, rescued = 0;
        if (!operational) {
            return;
        }
        try {
            String needle = searchField.getText() == null
                    ? "" : searchField.getText().trim();
            String filter = String.valueOf(filterCombo.getSelectedItem());
            List<RescueRequest> requests;

            if (!needle.isEmpty()) {
                requests = controller.search(needle);
                if (!"All".equals(filter)) {
                    RequestStatus status = RequestStatus.valueOf(
                            filter.toUpperCase().replace(' ', '_'));
                    requests.removeIf(r -> r.getStatus() != status);
                }
            } else if ("All".equals(filter)) {
                requests = controller.getAllRequests();
            } else {
                RequestStatus status = RequestStatus.valueOf(
                        filter.toUpperCase().replace(' ', '_'));
                requests = controller.getByStatus(status);
            }
            for (RescueRequest request : requests) {
                total++;
                switch (request.getStatus()) {
                    case PENDING, UNDER_REVIEW -> pending++;
                    case ASSIGNED, IN_PROGRESS -> assigned++;
                    case RESCUED -> rescued++;
                    default -> { }
                }
                if (request.getPriority() == PriorityLevel.CRITICAL) {
                    critical++;
                }
                tableModel.addRow(RescueRequestController.toRow(request));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        totalTile.setText(String.valueOf(total));
        pendingTile.setText(String.valueOf(pending));
        criticalTile.setText(String.valueOf(critical));
        assignedTile.setText(String.valueOf(assigned));
        rescuedTile.setText(String.valueOf(rescued));
    }

    /** Releases the live team; the request returns to PENDING. */
    private void abortSelected() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        long assignmentId = controller.getLatestAssignmentId(requestId);
        if (assignmentId < 0) {
            ViewUtil.info(this,
                    "Request #" + requestId + " has no assignment to abort");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Abort assignment #" + assignmentId
                        + "? The team is released and the request returns"
                        + " to PENDING.",
                "Confirm abort", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        String notes = JOptionPane.showInputDialog(this,
                "Reason / notes (optional):");
        ActionResult result = controller.abortAssignment(assignmentId, notes);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        revealRequest(requestId);
    }

    /** Shows every past assignment of the selected request with notes. */
    private void showHistory() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        try {
            List<RescueAssignment> history =
                    controller.getAssignmentHistory(requestId);
            StringBuilder text = new StringBuilder(
                    "Assignment history of request #" + requestId + ":\n");
            if (history.isEmpty()) {
                text.append("  (never assigned)");
            } else {
                for (RescueAssignment a : history) {
                    text.append("  #").append(a.getId())
                            .append(" ").append(a.getAssignmentStatus())
                            .append(" | team #").append(a.getRescueTeamId())
                            .append(" | ").append(a.getNotes() == null
                                    ? "-" : a.getNotes())
                            .append("\n");
                }
            }
            JTextArea area = new JTextArea(text.toString(), 12, 40);
            area.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(area),
                    "History", JOptionPane.INFORMATION_MESSAGE);
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void editSelected() {
        Long requestId = selectedRequestId();
        if (requestId == null) {
            return;
        }
        try {
            RescueRequest target = null;
            for (RescueRequest candidate : controller.getAllRequests()) {
                if (candidate.getId().equals(requestId)) {
                    target = candidate;
                }
            }
            if (target == null) {
                ViewUtil.error(this, "Request #" + requestId + " not found");
                return;
            }
            if (target.getStatus() != RequestStatus.PENDING) {
                ViewUtil.error(this, "Only PENDING requests can be edited"
                        + " - this one is "
                        + target.getStatus().getLabel());
                return;
            }
            requesterField.setText(target.getRequesterName());
            contactField.setText(target.getContactNumber());
            locationField.setText(target.getLocation());
            peopleField.setText(String.valueOf(target.getPeopleCount()));
            childrenField.setText(String.valueOf(target.getChildrenCount()));
            elderlyField.setText(String.valueOf(target.getElderlyCount()));
            lifeBox.setSelected(target.isLifeThreatening());
            medicalBox.setSelected(target.isMedicalEmergency());
            trappedBox.setSelected(target.isTrappedUnderDebris());
            assistanceArea.setText(target.getRequiredAssistance());
            refreshDisasters();
            for (int i = 0; i < disasterCombo.getItemCount(); i++) {
                if (disasterCombo.getItemAt(i).id()
                        .equals(target.getDisasterId())) {
                    disasterCombo.setSelectedIndex(i);
                }
            }
            editingVictimId = target.getVictimId();
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return;
        }
        editingId = requestId;
        saveChangesButton.setEnabled(true);
        ViewUtil.info(this, "Editing request #" + requestId
                + " - change the form and press Save changes");
    }

    private void saveChanges() {
        if (editingId == null) {
            return;
        }
        DisasterOption selected = (DisasterOption) disasterCombo.getSelectedItem();
        ActionResult result = controller.updateRequest(editingId,
                selected == null ? null : selected.id(),
                editingVictimId,
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
        Long edited = editingId;
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            clearEditMode();
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        revealRequest(edited);
    }

    private void clearEditMode() {
        editingId = null;
        editingVictimId = null;
        saveChangesButton.setEnabled(false);
    }
}
