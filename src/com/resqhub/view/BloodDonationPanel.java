package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.BloodDonorController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.BloodDonation;
import com.resqhub.model.BloodDonor;
import com.resqhub.model.BloodGroup;
import com.resqhub.model.BloodMatch;
import com.resqhub.model.BloodMatchStatus;
import com.resqhub.model.BloodRequest;
import com.resqhub.model.BloodRequestPriority;
import com.resqhub.model.BloodRequestStatus;
import com.resqhub.model.DonorAvailability;
import com.resqhub.model.DonorEligibility;
import com.resqhub.model.DonorRanking;
import com.resqhub.model.RoleType;
import com.resqhub.service.BloodDonorService.BloodGroupSummary;
import com.resqhub.service.BloodDonorService.MatchingHistoryEntry;
import com.resqhub.service.SessionManager;

/**
 * Blood Donor Management screen: donor registration & profiles, blood
 * requests by priority, donor matching (group / availability / eligibility /
 * location), donation recording & history, and blood-shortage alerts.
 */
public class BloodDonationPanel extends JPanel implements Refreshable {

    private final BloodDonorController controller = new BloodDonorController();
    private final boolean write;

    // stat tiles
    private final JLabel donorsTile = new JLabel("0");
    private final JLabel eligibleTile = new JLabel("0");
    private final JLabel requestsTile = new JLabel("0");
    private final JLabel criticalTile = new JLabel("0");
    private final JLabel shortagesTile = new JLabel("0");
    private final JLabel unitsTile = new JLabel("0");

    // donors table
    private final DefaultTableModel donorModel =
            ViewUtil.readOnlyModel(BloodDonorController.donorHeaders());
    private final JTable donorTable = new JTable(donorModel);
    private final JTextField donorSearchField = new JTextField(14);
    private final JComboBox<String> donorViewCombo = new JComboBox<>(
            new String[]{"All Donors", "Eligible & Available",
                    "Available", "Eligible"});

    // requests table
    private final DefaultTableModel requestModel =
            ViewUtil.readOnlyModel(BloodDonorController.requestHeaders());
    private final JTable requestTable = new JTable(requestModel);
    private final JComboBox<String> requestStatusFilter = new JComboBox<>(
            new String[]{"All Statuses", "Open", "Pending",
                    "Matching Donors", "Donor Found", "Blood Collected",
                    "Fulfilled", "Cancelled"});
    private final JComboBox<String> requestGroupFilter = new JComboBox<>();
    private final JComboBox<String> requestPriorityFilter = new JComboBox<>();
    private final JTextField requestKeywordField = new JTextField(10);

    // matches table
    private final DefaultTableModel matchModel =
            ViewUtil.readOnlyModel(BloodDonorController.matchHeaders());
    private final JTable matchTable = new JTable(matchModel);

    // matching history table
    private final DefaultTableModel matchingHistoryModel =
            ViewUtil.readOnlyModel(BloodDonorController.matchingHistoryHeaders());
    private final JTable matchingHistoryTable = new JTable(matchingHistoryModel);

    // donation history table
    private final DefaultTableModel donationModel =
            ViewUtil.readOnlyModel(BloodDonorController.donationHeaders());
    private final JTable donationTable = new JTable(donationModel);

    // blood supply report tab
    private final DefaultTableModel summaryModel = ViewUtil.readOnlyModel(
            new String[]{"Blood Group", "Total Donors", "Available",
                    "Eligible", "Shortage"});
    private final JTable summaryTable = new JTable(summaryModel);
    private final JTextArea shortageArea = new JTextArea(6, 40);
    private final DefaultTableModel requestStatsModel = ViewUtil.readOnlyModel(
            BloodDonorController.requestStatsHeaders());
    private final JTable requestStatsTable = new JTable(requestStatsModel);
    private final DefaultTableModel byGroupStatsModel = ViewUtil.readOnlyModel(
            new String[]{"Blood Group", "Requests"});
    private final JTable byGroupStatsTable = new JTable(byGroupStatsModel);

    private final JTabbedPane tabs = new JTabbedPane();

    public BloodDonationPanel() {
        write = SessionManager.getInstance().hasRole(RoleType.ADMIN,
                RoleType.RESCUE_OFFICER, RoleType.BLOOD_COORDINATOR,
                RoleType.MEDICAL_OFFICER);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);

        requestGroupFilter.addItem("All Groups");
        for (BloodGroup g : BloodGroup.values()) {
            requestGroupFilter.addItem(g.getLabel());
        }
        requestPriorityFilter.addItem("All Priorities");
        for (BloodRequestPriority p : BloodRequestPriority.values()) {
            requestPriorityFilter.addItem(p.getLabel());
        }

        tabs.addTab("Donors", buildDonorsTab());
        tabs.addTab("Blood Requests", buildRequestsTab());
        tabs.addTab("Donor Matching", buildMatchesTab());
        tabs.addTab("Donation History", buildDonationsTab());
        tabs.addTab("Blood Supply Report", buildReportTab());
        add(tabs, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("BLOOD DONOR MANAGEMENT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        tiles.add(statTile("DONORS", donorsTile, new Color(60, 60, 60)));
        tiles.add(statTile("ELIGIBLE & AVAILABLE", eligibleTile,
                new Color(40, 110, 40)));
        tiles.add(statTile("REQUESTS", requestsTile,
                new Color(30, 80, 150)));
        tiles.add(statTile("CRITICAL OPEN", criticalTile,
                new Color(150, 30, 30)));
        tiles.add(statTile("SHORTAGES", shortagesTile,
                new Color(200, 130, 20)));
        tiles.add(statTile("UNITS DONATED", unitsTile,
                new Color(120, 40, 120)));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(title, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        return north;
    }

    private JPanel statTile(String caption, JLabel valueLabel, Color color) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 22f));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JLabel c = new JLabel(caption);
        c.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        c.setFont(c.getFont().deriveFont(10f));
        c.setForeground(new Color(90, 90, 90));
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setPreferredSize(new Dimension(130, 76));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(c);
        tile.add(Box.createVerticalGlue());
        return tile;
    }

    // ========================= DONORS TAB ===========================

    private JPanel buildDonorsTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.add(buildDonorForm(), BorderLayout.WEST);
        body.add(buildDonorTableArea(), BorderLayout.CENTER);
        tab.add(body, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildDonorForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Register blood donor"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameIn = new JTextField(16);
        JComboBox<BloodGroup> groupIn = new JComboBox<>(BloodGroup.values());
        JTextField locationIn = new JTextField(14);
        JTextField phoneIn = new JTextField(12);
        JTextField emailIn = new JTextField(16);
        JComboBox<DonorAvailability> availIn =
                new JComboBox<>(DonorAvailability.values());
        JTextField lastDonationIn = new JTextField(10);
        lastDonationIn.setToolTipText("yyyy-MM-dd (optional)");
        JComboBox<DonorEligibility> eligIn =
                new JComboBox<>(DonorEligibility.values());
        JTextField notesIn = new JTextField(18);

        int row = 0;
        row = addRow(form, gbc, row, "Name:", nameIn);
        row = addRow(form, gbc, row, "Blood group:", groupIn);
        row = addRow(form, gbc, row, "Location:", locationIn);
        row = addRow(form, gbc, row, "Phone:", phoneIn);
        row = addRow(form, gbc, row, "Email:", emailIn);
        row = addRow(form, gbc, row, "Availability:", availIn);
        row = addRow(form, gbc, row, "Last donation:", lastDonationIn);
        row = addRow(form, gbc, row, "Eligibility:", eligIn);
        row = addRow(form, gbc, row, "Notes:", notesIn);

        JButton registerBtn = new JButton("Register donor");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(registerBtn, gbc);
        registerBtn.addActionListener(e -> {
            ActionResult r = controller.registerDonor(nameIn.getText(),
                    (BloodGroup) groupIn.getSelectedItem(),
                    locationIn.getText(), phoneIn.getText(), emailIn.getText(),
                    (DonorAvailability) availIn.getSelectedItem(),
                    lastDonationIn.getText(),
                    (DonorEligibility) eligIn.getSelectedItem(),
                    notesIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(this, r.getMessage());
                nameIn.setText("");
                locationIn.setText("");
                phoneIn.setText("");
                emailIn.setText("");
                lastDonationIn.setText("");
                notesIn.setText("");
                refreshData();
            } else {
                ViewUtil.error(this, r.getMessage());
            }
        });
        return form;
    }

    private JPanel buildDonorTableArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(donorTable), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Search:"));
        controls.add(donorSearchField);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> refreshDonors());
        controls.add(searchBtn);
        JButton allBtn = new JButton("Show All");
        allBtn.addActionListener(e -> {
            donorSearchField.setText("");
            donorViewCombo.setSelectedIndex(0);
            refreshDonors();
        });
        controls.add(allBtn);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(new JLabel("Show:"));
        controls.add(donorViewCombo);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton profileBtn = new JButton("View Profile");
        profileBtn.addActionListener(e -> viewDonorProfile());
        actions.add(profileBtn);
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, donorTable, "blood_donors"));
        actions.add(exportBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(controls);
        north.add(actions);
        area.add(north, BorderLayout.NORTH);
        return area;
    }

    private void viewDonorProfile() {
        int row = donorTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a donor in the table first");
            return;
        }
        Long id = (Long) donorModel.getValueAt(row, 0);
        try {
            BloodDonor donor = controller.getDonor(id);
            if (donor == null) {
                ViewUtil.error(this, "Donor not found: " + id);
                return;
            }
            showDonorProfileDialog(donor);
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void showDonorProfileDialog(BloodDonor donor) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Donor Profile - " + donor.getFullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(820, 620);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(BorderFactory.createTitledBorder("Donor Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        int r = 0;
        r = addInfoRow(info, gbc, r, "Name:", donor.getFullName());
        r = addInfoRow(info, gbc, r, "Blood group:",
                donor.getBloodGroup() == null ? "-"
                        : donor.getBloodGroup().getLabel());
        r = addInfoRow(info, gbc, r, "Compatible recipients:",
                donor.getBloodGroup() == null ? "-"
                        : donor.getBloodGroup().compatibleRecipients());
        r = addInfoRow(info, gbc, r, "Location:",
                donor.getLocation() == null ? "-" : donor.getLocation());
        r = addInfoRow(info, gbc, r, "Phone:",
                donor.getPhone() == null ? "-" : donor.getPhone());
        r = addInfoRow(info, gbc, r, "Email:",
                donor.getEmail() == null ? "-" : donor.getEmail());
        r = addInfoRow(info, gbc, r, "Availability:",
                donor.getAvailability() == null ? "-"
                        : donor.getAvailability().getLabel());
        r = addInfoRow(info, gbc, r, "Eligibility:",
                donor.getEligibility() == null ? "-"
                        : donor.getEligibility().getLabel());
        r = addInfoRow(info, gbc, r, "Last donation:",
                donor.getLastDonationDate() == null ? "-"
                        : donor.getLastDonationDate().toString());
        r = addInfoRow(info, gbc, r, "Notes:",
                donor.getNotes() == null ? "-" : donor.getNotes());
        content.add(info, BorderLayout.NORTH);

        JTabbedPane inner = new JTabbedPane();
        inner.add("Donation History", buildDonorHistoryTab(donor.getId()));
        inner.add("Matches", buildDonorMatchesTab(donor.getId()));
        content.add(inner, BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (write) {
            JButton editBtn = new JButton("Edit Donor");
            editBtn.addActionListener(e -> {
                editDonorDialog(donor);
                dialog.dispose();
                refreshData();
            });
            foot.add(editBtn);
            JButton recordBtn = new JButton("Record Donation");
            recordBtn.addActionListener(e -> {
                recordDonationDialog(donor);
                dialog.dispose();
                refreshData();
            });
            foot.add(recordBtn);
            if (SessionManager.getInstance().hasRole(RoleType.ADMIN)) {
                JButton deleteBtn = new JButton("Delete");
                deleteBtn.addActionListener(e -> {
                    if (JOptionPane.showConfirmDialog(dialog,
                            "Delete donor " + donor.getFullName() + "?",
                            "Confirm", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE)
                            == JOptionPane.YES_OPTION) {
                        ActionResult res = controller.deleteDonor(
                                String.valueOf(donor.getId()));
                        if (res.isSuccess()) {
                            ViewUtil.info(dialog, res.getMessage());
                            dialog.dispose();
                            refreshData();
                        } else {
                            ViewUtil.error(dialog, res.getMessage());
                        }
                    }
                });
                foot.add(deleteBtn);
            }
        }
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        foot.add(closeBtn);
        content.add(foot, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private JPanel buildDonorHistoryTab(long donorId) {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = ViewUtil.readOnlyModel(
                BloodDonorController.donationHeaders());
        JTable t = new JTable(model);
        try {
            for (BloodDonation d : controller.getDonationHistory(donorId)) {
                model.addRow(controller.donationRow(d));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        tab.add(new JScrollPane(t), BorderLayout.CENTER);

        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        try {
            BloodDonation last = controller.getLastDonation(donorId);
            List<BloodDonation> all = controller.getDonationHistory(donorId);
            int totalUnits = 0;
            for (BloodDonation d : all) {
                totalUnits += d.getUnitsDonated();
            }
            summary.add(new JLabel("Total donations: " + all.size()));
            summary.add(new JLabel("Total units: " + totalUnits));
            summary.add(new JLabel("Last donated: "
                    + (last == null || last.getDonationDate() == null ? "-"
                            : last.getDonationDate())));
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        tab.add(summary, BorderLayout.SOUTH);
        return tab;
    }

    private JPanel buildDonorMatchesTab(long donorId) {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = ViewUtil.readOnlyModel(
                BloodDonorController.matchHeaders());
        JTable t = new JTable(model);
        try {
            for (BloodMatch m : controller.getMatchesForDonor(donorId)) {
                model.addRow(controller.matchRow(m));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        tab.add(new JScrollPane(t), BorderLayout.CENTER);
        return tab;
    }

    private void editDonorDialog(BloodDonor donor) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Edit Donor - " + donor.getFullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameIn = new JTextField(donor.getFullName(), 16);
        JComboBox<BloodGroup> groupIn = new JComboBox<>(BloodGroup.values());
        if (donor.getBloodGroup() != null) {
            groupIn.setSelectedItem(donor.getBloodGroup());
        }
        JTextField locationIn = new JTextField(donor.getLocation(), 14);
        JTextField phoneIn = new JTextField(donor.getPhone(), 12);
        JTextField emailIn = new JTextField(donor.getEmail(), 16);
        JComboBox<DonorAvailability> availIn =
                new JComboBox<>(DonorAvailability.values());
        availIn.setSelectedItem(donor.getAvailability());
        JTextField lastIn = new JTextField(donor.getLastDonationDate() == null
                ? "" : donor.getLastDonationDate().toString(), 10);
        JComboBox<DonorEligibility> eligIn =
                new JComboBox<>(DonorEligibility.values());
        eligIn.setSelectedItem(donor.getEligibility());
        JTextField notesIn = new JTextField(donor.getNotes(), 18);

        int row = 0;
        row = addRow(form, gbc, row, "Name:", nameIn);
        row = addRow(form, gbc, row, "Blood group:", groupIn);
        row = addRow(form, gbc, row, "Location:", locationIn);
        row = addRow(form, gbc, row, "Phone:", phoneIn);
        row = addRow(form, gbc, row, "Email:", emailIn);
        row = addRow(form, gbc, row, "Availability:", availIn);
        row = addRow(form, gbc, row, "Last donation:", lastIn);
        row = addRow(form, gbc, row, "Eligibility:", eligIn);
        row = addRow(form, gbc, row, "Notes:", notesIn);

        JButton saveBtn = new JButton("Save Changes");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            ActionResult r = controller.updateDonor(
                    String.valueOf(donor.getId()), nameIn.getText(),
                    (BloodGroup) groupIn.getSelectedItem(),
                    locationIn.getText(), phoneIn.getText(), emailIn.getText(),
                    (DonorAvailability) availIn.getSelectedItem(),
                    lastIn.getText(),
                    (DonorEligibility) eligIn.getSelectedItem(),
                    notesIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
                refreshData();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void recordDonationDialog(BloodDonor donor) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Record Donation - " + donor.getFullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JComboBox<BloodGroup> groupIn = new JComboBox<>(BloodGroup.values());
        if (donor.getBloodGroup() != null) {
            groupIn.setSelectedItem(donor.getBloodGroup());
        }
        JTextField unitsIn = new JTextField("1", 5);
        JTextField requestIn = new JTextField(10);
        requestIn.setToolTipText("Blood request ID (optional)");
        JTextField notesIn = new JTextField(18);

        int row = 0;
        row = addRow(form, gbc, row, "Blood group:", groupIn);
        row = addRow(form, gbc, row, "Units donated:", unitsIn);
        row = addRow(form, gbc, row, "Request ID (optional):", requestIn);
        row = addRow(form, gbc, row, "Notes:", notesIn);

        JButton saveBtn = new JButton("Record Donation");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            ActionResult r = controller.recordDonation(
                    String.valueOf(donor.getId()),
                    (BloodGroup) groupIn.getSelectedItem(),
                    unitsIn.getText(), requestIn.getText(), notesIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
                refreshData();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ========================= REQUESTS TAB =========================

    private JPanel buildRequestsTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.add(buildRequestForm(), BorderLayout.WEST);
        body.add(buildRequestTableArea(), BorderLayout.CENTER);
        tab.add(body, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildRequestForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("New blood request"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField codeIn = new JTextField(12);
        JComboBox<BloodGroup> groupIn = new JComboBox<>(BloodGroup.values());
        JTextField unitsIn = new JTextField(5);
        JTextField locationIn = new JTextField(14);
        JComboBox<BloodRequestPriority> priorityIn =
                new JComboBox<>(BloodRequestPriority.values());
        JTextField emergencyIn = new JTextField(18);
        JTextField hospitalIn = new JTextField(8);
        hospitalIn.setToolTipText("Hospital ID (optional)");
        JTextField victimIn = new JTextField(8);
        victimIn.setToolTipText("Victim ID (optional)");
        JTextField requiredIn = new JTextField(12);
        requiredIn.setToolTipText("Required by (yyyy-MM-dd HH:mm) optional");

        int row = 0;
        row = addRow(form, gbc, row, "Request code:", codeIn);
        row = addRow(form, gbc, row, "Blood group required:", groupIn);
        row = addRow(form, gbc, row, "Units required:", unitsIn);
        row = addRow(form, gbc, row, "Location:", locationIn);
        row = addRow(form, gbc, row, "Priority:", priorityIn);
        row = addRow(form, gbc, row, "Emergency details:", emergencyIn);
        row = addRow(form, gbc, row, "Hospital ID (optional):", hospitalIn);
        row = addRow(form, gbc, row, "Victim ID (optional):", victimIn);
        row = addRow(form, gbc, row, "Required by:", requiredIn);

        JButton createBtn = new JButton("Create Request");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(createBtn, gbc);
        createBtn.addActionListener(e -> {
            ActionResult r = controller.createBloodRequest(codeIn.getText(),
                    (BloodGroup) groupIn.getSelectedItem(), unitsIn.getText(),
                    locationIn.getText(),
                    (BloodRequestPriority) priorityIn.getSelectedItem(),
                    emergencyIn.getText(), hospitalIn.getText(),
                    victimIn.getText(), "", requiredIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(this, r.getMessage());
                codeIn.setText("");
                unitsIn.setText("");
                locationIn.setText("");
                emergencyIn.setText("");
                hospitalIn.setText("");
                victimIn.setText("");
                requiredIn.setText("");
                refreshData();
            } else {
                ViewUtil.error(this, r.getMessage());
            }
        });
        return form;
    }

    private JPanel buildRequestTableArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(requestTable), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Group:"));
        controls.add(requestGroupFilter);
        controls.add(new JLabel("Priority:"));
        controls.add(requestPriorityFilter);
        controls.add(new JLabel("Status:"));
        controls.add(requestStatusFilter);
        controls.add(new JLabel("Keyword:"));
        controls.add(requestKeywordField);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> refreshRequests());
        controls.add(searchBtn);
        JButton refreshBtn = new JButton("Reset");
        refreshBtn.addActionListener(e -> {
            requestGroupFilter.setSelectedIndex(0);
            requestPriorityFilter.setSelectedIndex(0);
            requestStatusFilter.setSelectedIndex(0);
            requestKeywordField.setText("");
            refreshRequests();
        });
        controls.add(refreshBtn);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton matchBtn = new JButton("Find & Match Donors");
        matchBtn.addActionListener(e -> matchDonorsDialog());
        actions.add(matchBtn);
        JButton suitableBtn = new JButton("View Suitable Donors");
        suitableBtn.addActionListener(e -> viewSuitableDonors());
        actions.add(suitableBtn);
        JButton statusBtn = new JButton("Update Status");
        statusBtn.addActionListener(e -> updateRequestStatusDialog());
        actions.add(statusBtn);
        JButton historyBtn = new JButton("Request History");
        historyBtn.addActionListener(e -> viewRequestHistory());
        actions.add(historyBtn);
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, requestTable, "blood_requests"));
        actions.add(exportBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(controls);
        north.add(actions);
        area.add(north, BorderLayout.NORTH);
        return area;
    }

    private void matchDonorsDialog() {
        int row = requestTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a blood request first");
            return;
        }
        Long id = (Long) requestModel.getValueAt(row, 0);

        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Match Donors to Request #" + id,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField maxIn = new JTextField("1", 5);
        int r = addRow(form, gbc, 0, "Max donors to propose:", maxIn);
        JButton goBtn = new JButton("Match");
        gbc.gridx = 1;
        gbc.gridy = r;
        form.add(goBtn, gbc);
        goBtn.addActionListener(e -> {
            ActionResult result = controller.matchRequest(
                    String.valueOf(id), maxIn.getText());
            if (result.isSuccess()) {
                ViewUtil.info(dialog, result.getMessage());
                dialog.dispose();
                refreshData();
            } else {
                ViewUtil.error(dialog, result.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void viewSuitableDonors() {
        int row = requestTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a blood request first");
            return;
        }
        Long id = (Long) requestModel.getValueAt(row, 0);
        try {
            BloodRequest req = controller.getRequest(id);
            if (req == null) {
                ViewUtil.error(this, "Request not found");
                return;
            }
            List<DonorRanking> ranked = controller.findRankedDonors(
                    String.valueOf(id));
            JDialog dialog = new JDialog(
                    javax.swing.SwingUtilities.getWindowAncestor(this),
                    "Ranked Donors - " + req.getRequestCode(),
                    java.awt.Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setSize(860, 440);
            dialog.setLocationRelativeTo(this);
            DefaultTableModel model = ViewUtil.readOnlyModel(
                    BloodDonorController.rankingHeaders());
            JTable t = new JTable(model);
            for (DonorRanking rk : ranked) {
                model.addRow(controller.rankingRow(rk));
            }
            JPanel content = new JPanel(new BorderLayout(8, 8));
            content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            String bestInfo = ranked.isEmpty() ? "No matches"
                    : "Best: " + ranked.get(0).getDonor().getFullName()
                            + " (score " + ranked.get(0).getTotalScore()
                            + ", " + ranked.get(0).getDonor().getBloodGroup()
                                    .getLabel() + ")";
            content.add(new JLabel(ranked.size() + " ranked donor(s) for "
                    + req.getBloodGroup().getLabel() + " @ "
                    + req.getLocation() + " | " + bestInfo),
                    BorderLayout.NORTH);
            content.add(new JScrollPane(t), BorderLayout.CENTER);
            if (ranked.isEmpty()) {
                content.add(new JLabel(
                        "<html><b>No compatible available donor matches this group.</b>"
                                + " Consider a shortage alert or re-match.</html>"),
                        BorderLayout.SOUTH);
            }
            dialog.setContentPane(content);
            dialog.setVisible(true);
        } catch (ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void updateRequestStatusDialog() {
        int row = requestTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a blood request first");
            return;
        }
        Long id = (Long) requestModel.getValueAt(row, 0);
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Update Request Status",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        JComboBox<BloodRequestStatus> statusIn =
                new JComboBox<>(BloodRequestStatus.values());
        int r = addRow(form, gbc, 0,
                "Set status for request #" + id + ":", statusIn);
        JButton saveBtn = new JButton("Update");
        gbc.gridx = 1;
        gbc.gridy = r;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            ActionResult result = controller.updateRequestStatus(
                    String.valueOf(id),
                    (BloodRequestStatus) statusIn.getSelectedItem());
            if (result.isSuccess()) {
                ViewUtil.info(dialog, result.getMessage());
                dialog.dispose();
                refreshData();
            } else {
                ViewUtil.error(dialog, result.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ========================= MATCHES TAB ==========================

    private JPanel buildMatchesTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        tab.add(new JScrollPane(matchTable), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        if (write) {
            JButton statusBtn = new JButton("Update Match Status");
            statusBtn.addActionListener(e -> updateMatchStatusDialog());
            bar.add(statusBtn);

            JButton rematchBtn = new JButton("Rematch Donors");
            rematchBtn.addActionListener(e -> rematchDonorsDialog());
            bar.add(rematchBtn);
        }
        JButton historyBtn = new JButton("Matching History");
        historyBtn.addActionListener(e -> viewMatchingHistory());
        bar.add(historyBtn);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshMatches());
        bar.add(refreshBtn);
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, matchTable, "blood_matches"));
        bar.add(exportBtn);
        tab.add(bar, BorderLayout.NORTH);
        return tab;
    }

    private void updateMatchStatusDialog() {
        int row = matchTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a match first");
            return;
        }
        Long id = (Long) matchModel.getValueAt(row, 0);
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Update Match Status",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        JComboBox<BloodMatchStatus> statusIn =
                new JComboBox<>(BloodMatchStatus.values());
        int r = addRow(form, gbc, 0, "Set status for match #" + id + ":",
                statusIn);
        JButton saveBtn = new JButton("Update");
        gbc.gridx = 1;
        gbc.gridy = r;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            ActionResult result = controller.setMatchStatus(String.valueOf(id),
                    (BloodMatchStatus) statusIn.getSelectedItem());
            if (result.isSuccess()) {
                ViewUtil.info(dialog, result.getMessage());
                dialog.dispose();
                refreshData();
            } else {
                ViewUtil.error(dialog, result.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /** Re-match: excludes previously matched donors and re-runs the engine. */
    private void rematchDonorsDialog() {
        int row = matchTable.getSelectedRow();
        if (row < 0) {
            // Try selecting from the request table instead
            int reqRow = requestTable.getSelectedRow();
            if (reqRow < 0) {
                ViewUtil.error(this,
                        "Select a blood request or match first");
                return;
            }
            doRematch((Long) requestModel.getValueAt(reqRow, 0));
            return;
        }
        Long matchId = (Long) matchModel.getValueAt(row, 0);
        // Find the request ID from the selected match
        for (int r = 0; r < matchModel.getRowCount(); r++) {
            if (matchModel.getValueAt(r, 0).equals(matchId)) {
                // Get request from the match row
                // request is column 2 in match model
                String reqCode = String.valueOf(
                        matchModel.getValueAt(r, 2));
                // Look up the request by code
                try {
                    for (BloodRequest req : controller.getAllRequests()) {
                        if (req.getRequestCode().equals(reqCode)) {
                            doRematch(req.getId());
                            return;
                        }
                    }
                } catch (DataAccessException e) {
                    ViewUtil.error(this, e.getMessage());
                }
                break;
            }
        }
        ViewUtil.error(this, "Could not determine request for this match");
    }

    private void doRematch(long requestId) {
        String maxText = JOptionPane.showInputDialog(this,
                "Max donors to propose:", "5");
        if (maxText == null) return;
        try {
            ActionResult r = controller.rematchRequest(
                    String.valueOf(requestId), maxText);
            if (r.isSuccess()) {
                ViewUtil.info(this, r.getMessage());
                refreshData();
            } else {
                ViewUtil.error(this, r.getMessage());
            }
        } catch (Exception ex) {
            ViewUtil.error(this, ex.getMessage());
        }
    }

    /** Shows the full matching history timeline for a request. */
    private void viewMatchingHistory() {
        int row = requestTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a blood request first");
            return;
        }
        Long id = (Long) requestModel.getValueAt(row, 0);
        try {
            BloodRequest req = controller.getRequest(id);
            if (req == null) {
                ViewUtil.error(this, "Request not found");
                return;
            }
            List<MatchingHistoryEntry> history =
                    controller.getMatchingHistory(String.valueOf(id));
            JDialog dialog = new JDialog(
                    javax.swing.SwingUtilities.getWindowAncestor(this),
                    "Matching History - " + req.getRequestCode(),
                    java.awt.Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setSize(900, 380);
            dialog.setLocationRelativeTo(this);
            matchingHistoryModel.setRowCount(0);
            for (MatchingHistoryEntry h : history) {
                matchingHistoryModel.addRow(
                        controller.matchingHistoryRow(h));
            }
            JPanel content = new JPanel(new BorderLayout(8, 8));
            content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            String title = req.getRequestCode() + " - "
                    + req.getBloodGroup().getLabel() + " x"
                    + req.getUnitsRequired() + " @ " + req.getLocation();
            content.add(new JLabel(history.size()
                    + " match record(s) for " + title), BorderLayout.NORTH);
            content.add(new JScrollPane(matchingHistoryTable),
                    BorderLayout.CENTER);
            if (history.isEmpty()) {
                content.add(new JLabel("No matching history yet."),
                        BorderLayout.SOUTH);
            }
            dialog.setContentPane(content);
            dialog.setVisible(true);
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    // ========================= DONATION HISTORY ====================

    private JPanel buildDonationsTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        tab.add(new JScrollPane(donationTable), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        if (write) {
            JButton recordBtn = new JButton("Record Standalone Donation");
            recordBtn.addActionListener(e -> recordDonorlessDonation());
            bar.add(recordBtn);
        }
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshDonations());
        bar.add(refreshBtn);
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, donationTable,
                        "blood_donation_history"));
        bar.add(exportBtn);
        tab.add(bar, BorderLayout.NORTH);
        return tab;
    }

    private JPanel buildReportTab() {
        JPanel tab = new JPanel(new BorderLayout(10, 10));

        JPanel center = new JPanel(new BorderLayout(8, 8));

        // Request statistics (spec 32)
        JPanel statsRow = new JPanel();
        statsRow.setLayout(new BoxLayout(statsRow, BoxLayout.X_AXIS));
        JPanel reqStats = new JPanel(new BorderLayout(4, 4));
        reqStats.setBorder(BorderFactory.createTitledBorder(
                "Blood Request Statistics"));
        reqStats.add(new JScrollPane(requestStatsTable), BorderLayout.CENTER);
        JPanel byGroup = new JPanel(new BorderLayout(4, 4));
        byGroup.setBorder(BorderFactory.createTitledBorder(
                "Requests by Blood Group"));
        byGroup.add(new JScrollPane(byGroupStatsTable), BorderLayout.CENTER);
        statsRow.add(reqStats);
        statsRow.add(Box.createHorizontalStrut(12));
        statsRow.add(byGroup);
        center.add(statsRow, BorderLayout.NORTH);

        JPanel supply = new JPanel(new BorderLayout(8, 8));
        supply.setBorder(BorderFactory.createTitledBorder(
                "Blood Group Inventory Summary"));
        supply.add(new JScrollPane(summaryTable), BorderLayout.CENTER);

        JPanel shortages = new JPanel(new BorderLayout(8, 8));
        shortages.setBorder(BorderFactory.createTitledBorder(
                "Active Shortages & Alerts"));
        shortageArea.setEditable(false);
        shortageArea.setLineWrap(true);
        shortageArea.setBackground(new Color(255, 245, 220));
        shortageArea.setFont(shortageArea.getFont().deriveFont(12f));
        shortages.add(new JScrollPane(shortageArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                supply, shortages);
        split.setResizeWeight(0.55);
        center.add(split, BorderLayout.CENTER);
        tab.add(center, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton refreshBtn = new JButton("Refresh Report");
        refreshBtn.addActionListener(e -> refreshReport());
        bar.add(refreshBtn);
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, summaryTable,
                        "blood_supply_report"));
        bar.add(exportBtn);
        tab.add(bar, BorderLayout.NORTH);
        return tab;
    }

    private void recordDonorlessDonation() {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Record Blood Donation",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JComboBox<String> donorCombo = new JComboBox<>();
        donorCombo.addItem("-- Select donor --");
        java.util.Map<String, BloodDonor> byLabel = new java.util.HashMap<>();
        try {
            for (BloodDonor d : controller.getAllDonors()) {
                String label = d.getId() + ": " + d.getFullName()
                        + " [" + d.getBloodGroup().getLabel() + "]";
                donorCombo.addItem(label);
                byLabel.put(label, d);
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        JComboBox<BloodGroup> groupIn = new JComboBox<>(BloodGroup.values());
        JTextField unitsIn = new JTextField("1", 5);
        JTextField requestIn = new JTextField(10);
        JTextField notesIn = new JTextField(18);

        int r = 0;
        r = addRow(form, gbc, r, "Donor:", donorCombo);
        r = addRow(form, gbc, r, "Blood group:", groupIn);
        r = addRow(form, gbc, r, "Units:", unitsIn);
        r = addRow(form, gbc, r, "Request ID (optional):", requestIn);
        r = addRow(form, gbc, r, "Notes:", notesIn);

        JButton saveBtn = new JButton("Record");
        gbc.gridx = 1;
        gbc.gridy = r;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            Object sel = donorCombo.getSelectedItem();
            BloodDonor donor = byLabel.get(String.valueOf(sel));
            if (donor == null) {
                ViewUtil.error(dialog, "Select a blood donor");
                return;
            }
            ActionResult result = controller.recordDonation(
                    String.valueOf(donor.getId()),
                    (BloodGroup) groupIn.getSelectedItem(),
                    unitsIn.getText(), requestIn.getText(), notesIn.getText());
            if (result.isSuccess()) {
                ViewUtil.info(dialog, result.getMessage());
                dialog.dispose();
                refreshData();
            } else {
                ViewUtil.error(dialog, result.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ===================== DATA LOADING ============================

    @Override
    public void refreshData() {
        refreshDonors();
        refreshRequests();
        refreshMatches();
        refreshDonations();
        refreshReport();
        refreshStats();
    }

    private void refreshDonors() {
        donorModel.setRowCount(0);
        try {
            String needle = donorSearchField.getText() == null
                    ? "" : donorSearchField.getText().trim();
            String view = String.valueOf(donorViewCombo.getSelectedItem());
            List<BloodDonor> donors;
            if (!needle.isEmpty()) {
                donors = controller.searchDonors(needle);
            } else {
                donors = controller.getAllDonors();
            }
            boolean eligibleOnly = view.contains("Eligible & Available");
            boolean availableOnly = view.equals("Available")
                    || view.contains("Eligible & Available");
            boolean eligible = view.equals("Eligible")
                    || view.contains("Eligible & Available");
            if (eligibleOnly) {
                donors = controller.findEligibleAvailable();
            }
            for (BloodDonor d : donors) {
                if (availableOnly && d.getAvailability()
                        != DonorAvailability.AVAILABLE) {
                    continue;
                }
                if (eligible && d.getEligibility()
                        != DonorEligibility.ELIGIBLE) {
                    continue;
                }
                donorModel.addRow(BloodDonorController.donorRow(d));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshRequests() {
        requestModel.setRowCount(0);
        try {
            BloodGroup group = selectedGroup();
            BloodRequestPriority priority = selectedPriority();
            BloodRequestStatus status = selectedStatus();
            String keyword = requestKeywordField.getText();
            List<BloodRequest> requests;
            if (group == null && priority == null && status == null
                    && (keyword == null || keyword.trim().isEmpty())) {
                requests = controller.getAllRequests();
            } else {
                requests = controller.searchRequests(group, priority, status,
                        null, keyword);
            }
            for (BloodRequest r : requests) {
                requestModel.addRow(controller.requestRow(r));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private BloodGroup selectedGroup() {
        String sel = String.valueOf(requestGroupFilter.getSelectedItem());
        for (BloodGroup g : BloodGroup.values()) {
            if (g.getLabel().equals(sel)) {
                return g;
            }
        }
        return null;
    }

    private BloodRequestPriority selectedPriority() {
        String sel = String.valueOf(requestPriorityFilter.getSelectedItem());
        for (BloodRequestPriority p : BloodRequestPriority.values()) {
            if (p.getLabel().equals(sel)) {
                return p;
            }
        }
        return null;
    }

    private BloodRequestStatus selectedStatus() {
        String sel = String.valueOf(requestStatusFilter.getSelectedItem());
        switch (sel) {
            case "Pending": return BloodRequestStatus.PENDING;
            case "Matching Donors": return BloodRequestStatus.MATCHING_DONORS;
            case "Donor Found": return BloodRequestStatus.DONOR_FOUND;
            case "Blood Collected": return BloodRequestStatus.BLOOD_COLLECTED;
            case "Fulfilled": return BloodRequestStatus.FULFILLED;
            case "Cancelled": return BloodRequestStatus.CANCELLED;
            default: return null;
        }
    }

    private void viewRequestHistory() {
        int row = requestTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a blood request first");
            return;
        }
        Long id = (Long) requestModel.getValueAt(row, 0);
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Request History - Request #" + id,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(640, 420);
        dialog.setLocationRelativeTo(this);
        DefaultTableModel model = ViewUtil.readOnlyModel(
                BloodDonorController.historyHeaders());
        JTable t = new JTable(model);
        try {
            for (com.resqhub.model.RequestHistory h :
                    controller.getRequestHistory(id)) {
                model.addRow(controller.historyRow(h));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(new JLabel("Lifecycle events for this blood request"),
                BorderLayout.NORTH);
        content.add(new JScrollPane(t), BorderLayout.CENTER);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.add(closeBtn);
        content.add(foot, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void refreshMatches() {
        matchModel.setRowCount(0);
        try {
            for (BloodMatch m : controller.getAllMatches()) {
                matchModel.addRow(controller.matchRow(m));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshDonations() {
        donationModel.setRowCount(0);
        try {
            for (BloodDonation d : controller.getAllDonations()) {
                donationModel.addRow(controller.donationRow(d));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshStats() {
        try {
            donorsTile.setText(String.valueOf(controller.countDonors()));
            eligibleTile.setText(String.valueOf(
                    controller.countEligibleAvailable()));
            requestsTile.setText(String.valueOf(controller.countRequests()));
            criticalTile.setText(String.valueOf(controller.countCriticalOpen()));
            shortagesTile.setText(String.valueOf(controller.countShortages()));
            unitsTile.setText(String.valueOf(controller.countUnitsCollected()));
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshReport() {
        summaryModel.setRowCount(0);
        requestStatsModel.setRowCount(0);
        byGroupStatsModel.setRowCount(0);
        try {
            for (BloodGroupSummary s : controller.bloodGroupSummaries()) {
                summaryModel.addRow(new Object[]{
                        s.group().getLabel(), s.total(), s.available(),
                        s.eligible(), s.hasShortageFor(1) ? "SHORT" : "OK"});
            }
            com.resqhub.service.BloodDonorService.RequestStats stats =
                    controller.requestStats();
            requestStatsModel.addRow(controller.requestStatsRow(
                    "Total Requests", stats.total()));
            requestStatsModel.addRow(controller.requestStatsRow(
                    "Pending", stats.pending()));
            requestStatsModel.addRow(controller.requestStatsRow(
                    "Matching Donors", stats.matching()));
            requestStatsModel.addRow(controller.requestStatsRow(
                    "Blood Collected", stats.collected()));
            requestStatsModel.addRow(controller.requestStatsRow(
                    "Fulfilled", stats.fulfilled()));
            requestStatsModel.addRow(controller.requestStatsRow(
                    "Cancelled", stats.cancelled()));
            requestStatsModel.addRow(controller.requestStatsRow(
                    "Critical Open", stats.criticalOpen()));
            for (java.util.Map.Entry<BloodGroup, Integer> entry :
                    stats.byGroup().entrySet()) {
                byGroupStatsModel.addRow(new Object[]{
                        entry.getKey().getLabel(), entry.getValue()});
            }
            List<String> msgs = controller.findShortageMessages();
            shortageArea.setText(msgs.isEmpty()
                    ? "No active blood shortages. Supply meets demand."
                    : String.join("\n\n", msgs));
        } catch (ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    // ===================== HELPERS =================================

    private int addRow(JPanel form, GridBagConstraints gbc, int row,
                       String label, java.awt.Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        form.add(field, gbc);
        return row + 1;
    }

    private int addInfoRow(JPanel panel, GridBagConstraints gbc, int row,
                           String label, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        panel.add(l, gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(value == null ? "-" : value), gbc);
        return row + 1;
    }
}
