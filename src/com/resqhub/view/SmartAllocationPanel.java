package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.SmartAllocationController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RoleType;
import com.resqhub.model.ShelterAllocationStatus;
import com.resqhub.model.SmartAllocationResult;
import com.resqhub.model.Victim;
import com.resqhub.service.SessionManager;

/**
 * Smart Shelter Allocation screen. Combines the scoring engine with the
 * allocation lifecycle:
 *
 *  - "Smart Allocation" tab: capture victim/family requirements, preview
 *    the ranked suitable shelters, then allocate the best match.
 *  - "Allocations" tab: manage every allocation (confirm pending,
 *    check-in, complete, cancel, release) with status filtering.
 *  - "Waiting List" tab: victims who still need accommodation.
 */
public class SmartAllocationPanel extends JPanel implements Refreshable {

    private final SmartAllocationController controller =
            new SmartAllocationController();
    private final boolean write;
    private final JTabbedPane tabs = new JTabbedPane();

    private final JComboBox<String> victimCombo = new JComboBox<>();
    private final JTextField familyField = new JTextField(18);
    private final JTextField peopleField = new JTextField("1", 5);
    private final JComboBox<Object> priorityCombo =
            new JComboBox<>(new DefaultComboBoxModel<>(new Object[]{
                "Auto (from victim)", PriorityLevel.CRITICAL, PriorityLevel.HIGH,
                PriorityLevel.MEDIUM, PriorityLevel.LOW}));
    private final JTextField locationField = new JTextField(18);
    private final List<JCheckBox> facilityChecks = new ArrayList<>();
    private final JPanel facilitiesPanel =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    private final JCheckBox wcNeed = new JCheckBox("Wheelchair");
    private final JCheckBox elNeed = new JCheckBox("Elderly");
    private final JCheckBox mdNeed = new JCheckBox("Medical");
    private final JCheckBox spNeed = new JCheckBox("Special");
    private final JCheckBox reserveCheck = new JCheckBox(
            "Reserve (PENDING) - do not occupy yet");

    private final DefaultTableModel rankedModel =
            ViewUtil.readOnlyModel(SmartAllocationController.rankedHeaders());
    private final JTable rankedTable = new JTable(rankedModel);
    private final JLabel bestLabel = new JLabel(" ");

    private final DefaultTableModel allocModel =
            ViewUtil.readOnlyModel(SmartAllocationController.allocationHeaders());
    private final JTable allocTable = new JTable(allocModel);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"All Statuses", "Pending", "Active", "Checked In",
                    "Completed", "Cancelled", "Released"});
    private final JTextField allocSearch = new JTextField(12);

    private final DefaultTableModel waitModel =
            ViewUtil.readOnlyModel(SmartAllocationController.waitListHeaders());
    private final JTable waitTable = new JTable(waitModel);

    private final JLabel totalTile = new JLabel("0");
    private final JLabel activeTile = new JLabel("0");
    private final JLabel pendingTile = new JLabel("0");
    private final JLabel waitingTile = new JLabel("0");

    public SmartAllocationPanel() {
        write = SessionManager.getInstance().hasRole(RoleType.ADMIN,
                RoleType.RESCUE_OFFICER, RoleType.CAMP_MANAGER);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabsHost = tabs;
        tabsHost.add("Smart Allocation", buildAllocationTab());
        tabsHost.add("Allocations", buildAllocationsTab());
        tabsHost.add("Waiting List", buildWaitingTab());
        add(tabsHost, BorderLayout.CENTER);

        loadReferences();
        refreshData();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("SMART SHELTER ALLOCATION");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("ALLOCATIONS", totalTile, new Color(60, 60, 60)));
        tiles.add(statTile("ACTIVE", activeTile, new Color(40, 110, 40)));
        tiles.add(statTile("PENDING", pendingTile, new Color(200, 130, 20)));
        tiles.add(statTile("WAITING", waitingTile, new Color(150, 30, 30)));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(title, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        return north;
    }

    private JPanel statTile(String caption, JLabel valueLabel, Color color) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 24f));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JLabel cap = new JLabel(caption);
        cap.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cap.setFont(cap.getFont().deriveFont(11f));
        cap.setForeground(new Color(90, 90, 90));
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setPreferredSize(new Dimension(110, 80));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(cap);
        tile.add(Box.createVerticalGlue());
        return tile;
    }

    // ── smart allocation tab ─────────────────────────────────────────

    private JPanel buildAllocationTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Victim / Family requirements"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addRow(form, gbc, row, "Victim:", victimCombo);
        row = addRow(form, gbc, row, "Family / person name:", familyField);
        row = addRow(form, gbc, row, "People count:", peopleField);
        row = addRow(form, gbc, row, "Priority:", priorityCombo);
        row = addRow(form, gbc, row, "Current location:", locationField);

        JPanel facilities = facilitiesPanel;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Require facilities:"), gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        form.add(facilities, gbc);
        row++;

        JPanel access = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        access.add(wcNeed);
        access.add(elNeed);
        access.add(mdNeed);
        access.add(spNeed);
        row = addRow(form, gbc, row, "Accessibility:", access);

        JButton previewBtn = new JButton("Preview Suitable Shelters");
        JButton allocateBtn = new JButton("Allocate Best Match");
        allocateBtn.setBackground(new Color(40, 110, 40));
        allocateBtn.setForeground(Color.WHITE);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        actions.add(previewBtn);
        actions.add(allocateBtn);
        row = addRow(form, gbc, row, "", reserveCheck);

        gbc.gridy = row;
        gbc.gridx = 1;
        form.add(actions, gbc);

        previewBtn.addActionListener(e -> previewRankings());
        allocateBtn.addActionListener(e -> runAllocation());

        victimCombo.addActionListener(e -> autofillFromVictim());

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.add(form, BorderLayout.NORTH);

        JLabel hint = new JLabel("<html>Ranked suitable shelters - driven by available "
                + "capacity, distance, priority, facilities and accessibility. "
                + "Higher score = better match.</html>");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(new Color(90, 90, 90));

        JPanel results = new JPanel(new BorderLayout(8, 6));
        results.add(hint, BorderLayout.NORTH);
        results.add(new JScrollPane(rankedTable), BorderLayout.CENTER);
        bestLabel.setFont(bestLabel.getFont().deriveFont(Font.BOLD, 13f));
        bestLabel.setForeground(new Color(40, 90, 40));
        results.add(bestLabel, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout());
        center.add(north, BorderLayout.NORTH);
        center.add(results, BorderLayout.CENTER);

        JPanel tab = new JPanel(new BorderLayout(6, 6));
        tab.add(center, BorderLayout.CENTER);
        return tab;
    }

    private void previewRankings() {
        rankedModel.setRowCount(0);
        bestLabel.setText(" ");
        ActionResult r = buildPreviewAction();
        if (!r.isSuccess()) {
            ViewUtil.error(this, r.getMessage());
            return;
        }
        SmartAllocationResult result = r.getData();
        result.getRanked().forEach(ranked ->
                rankedModel.addRow(SmartAllocationController.rankedRow(ranked)));
        bestLabel.setText(" BEST MATCH: " + result.getBest().getShelter().getName()
                + " (" + result.getBest().getShelter().getDistrict() + ")");
    }

    private void runAllocation() {
        ActionResult r = buildAllocateAction();
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            rankedModel.setRowCount(0);
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private ActionResult buildPreviewAction() {
        return controller.preview((String) victimCombo.getSelectedItem(),
                familyField.getText(), peopleField.getText(),
                selectedPriority(), locationField.getText(),
                selectedFacilities(), wcNeed.isSelected(), elNeed.isSelected(),
                mdNeed.isSelected(), spNeed.isSelected(), null);
    }

    private ActionResult buildAllocateAction() {
        return controller.allocate((String) victimCombo.getSelectedItem(),
                familyField.getText(), peopleField.getText(),
                selectedPriority(), locationField.getText(),
                selectedFacilities(), wcNeed.isSelected(), elNeed.isSelected(),
                mdNeed.isSelected(), spNeed.isSelected(), null,
                reserveCheck.isSelected());
    }

    private PriorityLevel selectedPriority() {
        Object sel = priorityCombo.getSelectedItem();
        return sel instanceof PriorityLevel ? (PriorityLevel) sel : null;
    }

    private List<String> selectedFacilities() {
        List<String> out = new ArrayList<>();
        for (JCheckBox box : facilityChecks) {
            if (box.isSelected()) {
                out.add(box.getText());
            }
        }
        return out;
    }

    private void autofillFromVictim() {
        String selection = (String) victimCombo.getSelectedItem();
        if (selection == null || selection.startsWith("---")) {
            return;
        }
        try {
            long id = Long.parseLong(selection.substring(0,
                    selection.indexOf(" - ")).trim());
            for (Victim v : controller.getVictims()) {
                if (v.getId().equals(id)) {
                    familyField.setText(v.getFullName());
                    if (v.getCurrentLocation() != null) {
                        locationField.setText(v.getCurrentLocation());
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
            // not a valid victim row; leave fields untouched
        }
    }

    private void loadReferences() {
        try {
            victimCombo.removeAllItems();
            victimCombo.addItem("--- family only ---");
            for (Victim v : controller.getVictims()) {
                victimCombo.addItem(v.getId() + " - " + v.getFullName()
                        + " (" + v.getEmergencyStatus().getLabel() + ")");
            }

            // rebuild facility checkboxes
            facilityChecks.clear();
            facilitiesPanel.removeAll();
            if (controller.getFacilityOptions().isEmpty()) {
                facilitiesPanel.add(new JLabel("(none available)"));
            } else {
                for (String name : controller.getFacilityOptions()) {
                    JCheckBox c = new JCheckBox(name);
                    facilityChecks.add(c);
                    facilitiesPanel.add(c);
                }
            }
            facilitiesPanel.revalidate();
            facilitiesPanel.repaint();
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    // ── allocations tab ──────────────────────────────────────────────

    private JPanel buildAllocationsTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Status:"));
        controls.add(statusFilter);
        controls.add(new JLabel("Search:"));
        controls.add(allocSearch);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshAllocations());
        controls.add(refreshBtn);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton confirmBtn = new JButton("Confirm");
        JButton checkInBtn = new JButton("Check In");
        JButton completeBtn = new JButton("Complete");
        JButton cancelBtn = new JButton("Cancel");
        JButton releaseBtn = new JButton("Release");
        if (write) {
            actions.add(confirmBtn);
            actions.add(checkInBtn);
            actions.add(completeBtn);
            actions.add(cancelBtn);
            actions.add(releaseBtn);
        }
        confirmBtn.addActionListener(e -> transition("confirm pending",
                controller::confirmPending));
        checkInBtn.addActionListener(e ->
                transition("check in", controller::checkIn));
        completeBtn.addActionListener(e ->
                transition("complete", controller::complete));
        cancelBtn.addActionListener(e ->
                transition("cancel", controller::cancel));
        releaseBtn.addActionListener(e ->
                transition("release", controller::release));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(controls);
        north.add(actions);
        tab.add(north, BorderLayout.NORTH);
        tab.add(new JScrollPane(allocTable), BorderLayout.CENTER);
        return tab;
    }

    private interface Transition {
        ActionResult run(long id);
    }

    private void transition(String verb, Transition fn) {
        int row = allocTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select an allocation first");
            return;
        }
        long id = (Long) allocModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this,
                verb + " allocation #" + id + "?",
                "Confirm", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult r = fn.run(id);
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            refreshAllocations();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void refreshAllocations() {
        allocModel.setRowCount(0);
        try {
            ShelterAllocationStatus status = null;
            String statusSel = String.valueOf(statusFilter.getSelectedItem());
            if (!"All Statuses".equals(statusSel)) {
                for (ShelterAllocationStatus s
                        : ShelterAllocationStatus.values()) {
                    if (s.getLabel().equals(statusSel)) {
                        status = s;
                        break;
                    }
                }
            }
            java.util.List<com.resqhub.model.ShelterAllocation> rows =
                    controller.filterAllocations(status, null,
                            allocSearch.getText());
            for (com.resqhub.model.ShelterAllocation a : rows) {
                allocModel.addRow(SmartAllocationController.allocationRow(a));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    // ── waiting list tab ─────────────────────────────────────────────

    private JPanel buildWaitingTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        JLabel hint = new JLabel("<html>Victims not currently sheltered - they "
                + "are the accommodation wait list. Use the Smart Allocation "
                + "tab to place them in the best available shelter.</html>");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(new Color(90, 90, 90));
        tab.add(hint, BorderLayout.NORTH);
        tab.add(new JScrollPane(waitTable), BorderLayout.CENTER);

        JButton pickBtn = new JButton("Allocate Selected Victim");
        if (write) {
            JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            foot.add(pickBtn);
            tab.add(foot, BorderLayout.SOUTH);
        }
        pickBtn.addActionListener(e -> {
            int row = waitTable.getSelectedRow();
            if (row < 0) {
                ViewUtil.error(this, "Select a victim first");
                return;
            }
            long id = (Long) waitModel.getValueAt(row, 0);
            for (int i = 0; i < victimCombo.getItemCount(); i++) {
                if (String.valueOf(victimCombo.getItemAt(i)).startsWith(id + " -")) {
                    victimCombo.setSelectedIndex(i);
                    break;
                }
            }
            tabs.setSelectedIndex(0);
        });
        return tab;
    }

    private int addRow(JPanel form, GridBagConstraints gbc, int row,
                       String label, javax.swing.JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        form.add(field, gbc);
        return row + 1;
    }

    // ── refresh ──────────────────────────────────────────────────────

    private void refreshTiles() {
        try {
            totalTile.setText(String.valueOf(controller.getAllAllocations().size()));
            activeTile.setText(String.valueOf(controller.countActive()));
            pendingTile.setText(String.valueOf(
                    controller.countByStatus(ShelterAllocationStatus.PENDING)));
            waitingTile.setText(String.valueOf(controller.countWaiting()));
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    @Override
    public void refreshData() {
        refreshTiles();
        refreshAllocations();
        try {
            waitModel.setRowCount(0);
            for (Victim v : controller.getWaitingForShelter()) {
                waitModel.addRow(SmartAllocationController.waitListRow(v));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }
}
