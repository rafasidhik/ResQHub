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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.HospitalController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Disaster;
import com.resqhub.model.Hospital;
import com.resqhub.model.HospitalCapacityLog;
import com.resqhub.model.HospitalFacility;
import com.resqhub.model.HospitalReferral;
import com.resqhub.model.HospitalReferralStatus;
import com.resqhub.model.HospitalStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.Victim;
import com.resqhub.service.SessionManager;

/**
 * Hospital Management screen: registers hospitals, tracks bed capacity
 * and emergency facilities, records emergency victim referrals (with
 * capacity + facility validation) and reviews capacity-change history.
 * Presented as three tabs: Hospitals, Referrals and Capacity Logs.
 */
public class HospitalPanel extends JPanel implements Refreshable {

    private final HospitalController controller = new HospitalController();
    private final boolean write;

    private final JTabbedPane tabs = new JTabbedPane();

    private Hospital currentHospital;

    // Hospitals tab widgets
    private final DefaultTableModel hospitalModel =
            ViewUtil.readOnlyModel(HospitalController.hospitalHeaders());
    private final JTable hospitalTable = new JTable(hospitalModel);
    private final JLabel totalTile = new JLabel("0");
    private final JLabel acceptingTile = new JLabel("0");
    private final JLabel nearTile = new JLabel("0");
    private final JLabel fullTile = new JLabel("0");
    private final JLabel bedsTile = new JLabel("0");

    // Referrals tab widgets
    private final DefaultTableModel referralModel =
            ViewUtil.readOnlyModel(HospitalController.referralHeaders());
    private final JTable referralTable = new JTable(referralModel);
    private final JComboBox<String> referralStatusFilter = new JComboBox<>(
            new String[]{"All Statuses", "Open", "Pending", "Accepted",
                    "Admitted", "Discharged", "Rejected", "Cancelled"});

    // Capacity log tab widgets
    private final DefaultTableModel logModel =
            ViewUtil.readOnlyModel(HospitalController.capacityLogHeaders());
    private final JTable logTable = new JTable(logModel);

    public HospitalPanel() {
        write = SessionManager.getInstance().hasRole(RoleType.ADMIN,
                RoleType.RESCUE_OFFICER, RoleType.CAMP_MANAGER,
                RoleType.MEDICAL_OFFICER);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);

        tabs.addTab("Hospitals", buildHospitalsTab());
        tabs.addTab("Referrals", buildReferralsTab());
        tabs.addTab("Capacity Logs", buildCapacityLogsTab());
        add(tabs, BorderLayout.CENTER);

        refreshTable();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("HOSPITAL MANAGEMENT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("TOTAL HOSPITALS", totalTile,
                new Color(60, 60, 60)));
        tiles.add(statTile("ACCEPTING", acceptingTile,
                new Color(40, 110, 40)));
        tiles.add(statTile("NEAR CAPACITY", nearTile,
                new Color(200, 130, 20)));
        tiles.add(statTile("FULL", fullTile, new Color(150, 30, 30)));
        tiles.add(statTile("BEDS AVAILABLE", bedsTile,
                new Color(30, 80, 150)));

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
        tile.setPreferredSize(new Dimension(130, 80));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(captionLabel);
        tile.add(Box.createVerticalGlue());
        return tile;
    }

    // =========================== HOSPITALS TAB ========================

    private JPanel buildHospitalsTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));

        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.add(buildHospitalForm(), BorderLayout.WEST);
        body.add(buildHospitalTableArea(), BorderLayout.CENTER);
        tab.add(body, BorderLayout.CENTER);

        if (write) {
            JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            actionBar.setBorder(BorderFactory.createTitledBorder(
                    "Hospital actions"));
            JButton alertsBtn = new JButton("Generate Capacity Alerts");
            alertsBtn.addActionListener(e -> generateAlerts());
            actionBar.add(alertsBtn);
            tab.add(actionBar, BorderLayout.SOUTH);
        }
        return tab;
    }

    private JPanel buildHospitalForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Register hospital"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameIn = new JTextField(16);
        JTextField codeIn = new JTextField(10);
        JTextField districtIn = new JTextField(14);
        JTextField cityIn = new JTextField(14);
        JTextField areaIn = new JTextField(14);
        JTextField addrIn = new JTextField(18);
        JTextField phoneIn = new JTextField(14);
        JTextField emergencyIn = new JTextField(14);
        JTextField emailIn = new JTextField(18);
        JTextField totalIn = new JTextField(6);
        JTextField occIn = new JTextField(6);
        JComboBox<HospitalStatus> statusIn =
                new JComboBox<>(HospitalStatus.values());
        JComboBox<HospitalFacility> facilityIn =
                new JComboBox<>(HospitalFacility.values());
        JComboBox<String> disasterIn = buildDisasterCombo();

        Set<HospitalFacility> selected = new LinkedHashSet<>();
        JCheckBox[] facilityChecks = new JCheckBox[HospitalFacility.values().length];
        JPanel facilitiesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        HospitalFacility[] facilities = HospitalFacility.values();
        for (int i = 0; i < facilities.length; i++) {
            JCheckBox cb = new JCheckBox(facilities[i].getLabel());
            cb.setSelected(false);
            facilityChecks[i] = cb;
            facilitiesPanel.add(cb);
        }

        int row = 0;
        row = addRow(form, gbc, row, "Name:", nameIn);
        row = addRow(form, gbc, row, "Hospital code:", codeIn);
        row = addRow(form, gbc, row, "District:", districtIn);
        row = addRow(form, gbc, row, "City:", cityIn);
        row = addRow(form, gbc, row, "Area:", areaIn);
        row = addRow(form, gbc, row, "Address:", addrIn);
        row = addRow(form, gbc, row, "Phone:", phoneIn);
        row = addRow(form, gbc, row, "Emergency contact:", emergencyIn);
        row = addRow(form, gbc, row, "Email:", emailIn);
        row = addRow(form, gbc, row, "Total beds:", totalIn);
        row = addRow(form, gbc, row, "Occupied beds:", occIn);
        row = addRow(form, gbc, row, "Status:", statusIn);
        row = addRow(form, gbc, row, "Disaster (optional):", disasterIn);
        JPanel facRow = new JPanel(new BorderLayout());
        facRow.add(facilitiesPanel, BorderLayout.CENTER);
        row = addRow(form, gbc, row, "Facilities:", facRow);

        JButton registerBtn = new JButton("Register hospital");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(registerBtn, gbc);
        registerBtn.addActionListener(e -> {
            Set<HospitalFacility> set = new LinkedHashSet<>();
            for (int i = 0; i < facilityChecks.length; i++) {
                if (facilityChecks[i].isSelected()) {
                    set.add(facilities[i]);
                }
            }
            ActionResult r = controller.registerHospital(
                    nameIn.getText(), codeIn.getText(), districtIn.getText(),
                    cityIn.getText(), areaIn.getText(), addrIn.getText(),
                    phoneIn.getText(), emergencyIn.getText(), emailIn.getText(),
                    totalIn.getText(), occIn.getText(), set,
                    (HospitalStatus) statusIn.getSelectedItem(),
                    selectedDisasterId(disasterIn));
            if (r.isSuccess()) {
                ViewUtil.info(this, r.getMessage());
                nameIn.setText("");
                codeIn.setText("");
                districtIn.setText("");
                cityIn.setText("");
                areaIn.setText("");
                addrIn.setText("");
                phoneIn.setText("");
                emergencyIn.setText("");
                emailIn.setText("");
                totalIn.setText("");
                occIn.setText("");
                for (JCheckBox cb : facilityChecks) {
                    cb.setSelected(false);
                }
                refreshTable();
            } else {
                ViewUtil.error(this, r.getMessage());
            }
        });
        return form;
    }

    private JPanel buildHospitalTableArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(hospitalTable), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(14);
        controls.add(new JLabel("Search:"));
        controls.add(searchField);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> refreshTable());
        controls.add(searchBtn);
        JButton allBtn = new JButton("Show All");
        allBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable();
        });
        controls.add(allBtn);
        controls.add(Box.createHorizontalStrut(10));
        JComboBox<String> viewCombo = new JComboBox<>(
                new String[]{"All Hospitals", "Accepting Patients",
                        "Near Capacity", "Full"});
        controls.add(new JLabel("View:"));
        controls.add(viewCombo);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton profileBtn = new JButton("View Profile");
        profileBtn.addActionListener(e -> viewHospitalProfile());
        actions.add(profileBtn);
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, hospitalTable, "hospitals"));
        actions.add(exportBtn);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(controls);
        north.add(actions);
        area.add(north, BorderLayout.NORTH);
        return area;
    }

    // =========================== REFERRALS TAB ========================

    private JPanel buildReferralsTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        tab.add(new JScrollPane(referralTable), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Status:"));
        controls.add(referralStatusFilter);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshReferrals());
        controls.add(refreshBtn);
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, referralTable, "hospital_referrals"));
        controls.add(exportBtn);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        if (write) {
            JButton referBtn = new JButton("New Referral");
            referBtn.addActionListener(e -> referVictimDialog());
            bar.add(referBtn);
            JButton updateBtn = new JButton("Update Selected Status");
            updateBtn.addActionListener(e -> updateReferralStatusDialog());
            bar.add(updateBtn);
        }

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(controls);
        north.add(bar);
        tab.add(north, BorderLayout.NORTH);
        return tab;
    }

    private JPanel buildCapacityLogsTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        tab.add(new JScrollPane(logTable), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshLogs());
        bar.add(refreshBtn);
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, logTable, "capacity_logs"));
        bar.add(exportBtn);
        tab.add(bar, BorderLayout.NORTH);
        return tab;
    }

    // ===================== ACTION HANDLERS ===========================

    private void generateAlerts() {
        ActionResult r = controller.generateCapacityAlerts();
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void viewHospitalProfile() {
        int viewRow = hospitalTable.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a hospital in the table first");
            return;
        }
        Long id = (Long) hospitalModel.getValueAt(viewRow, 0);
        try {
            Hospital h = controller.getAllHospitals().stream()
                    .filter(x -> x.getId().equals(id))
                    .findFirst().orElse(null);
            if (h == null) {
                ViewUtil.error(this, "Hospital not found: " + id);
                return;
            }
            showHospitalProfileDialog(h);
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void showHospitalProfileDialog(Hospital h) {
        currentHospital = h;
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Hospital Profile - " + h.getName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(900, 640);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(BorderFactory.createTitledBorder("Hospital Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int r = 0;
        r = addInfoRow(info, gbc, r, "Name:", h.getName());
        r = addInfoRow(info, gbc, r, "Code:", h.getHospitalId());
        r = addInfoRow(info, gbc, r, "Location:",
                h.getDistrict() + (h.getCity() == null ? ""
                        : ", " + h.getCity())
                + (h.getArea() == null ? "" : ", " + h.getArea()));
        r = addInfoRow(info, gbc, r, "Address:",
                h.getAddress() == null ? "-" : h.getAddress());
        r = addInfoRow(info, gbc, r, "Beds / Occupied:",
                h.getTotalBeds() + " / " + h.getOccupiedBeds());
        r = addInfoRow(info, gbc, r, "Available:",
                String.valueOf(h.availableBeds()));
        r = addInfoRow(info, gbc, r, "Utilisation:",
                h.utilisationPercent() + "%");
        r = addInfoRow(info, gbc, r, "Facilities:",
                h.facilitiesSummary());
        r = addInfoRow(info, gbc, r, "Status:", h.getStatus().getLabel());
        r = addInfoRow(info, gbc, r, "Phone:",
                h.getPhone() == null ? "-" : h.getPhone());
        r = addInfoRow(info, gbc, r, "Emergency:",
                h.getEmergencyContact() == null ? "-"
                        : h.getEmergencyContact());
        r = addInfoRow(info, gbc, r, "Email:",
                h.getEmail() == null ? "-" : h.getEmail());
        if (h.getDisasterId() != null) {
            try {
                Map<Long, String> disasterNames = controller.disasterNameMap();
                r = addInfoRow(info, gbc, r, "Linked Disaster:",
                        disasterNames.getOrDefault(h.getDisasterId(), "?"));
            } catch (DataAccessException ignored) {
            }
        }

        content.add(info, BorderLayout.NORTH);

        JTabbedPane inner = new JTabbedPane();
        inner.add("Facilities", buildProfileFacilitiesTab(h));
        inner.add("Referrals", buildProfileReferralsTab(h.getId()));
        inner.add("Capacity History", buildProfileLogsTab(h.getId()));
        content.add(inner, BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (write) {
            JButton bedsBtn = new JButton("Update Occupancy");
            bedsBtn.addActionListener(e -> {
                updateOccupancyDialog(h);
                dialog.dispose();
                refreshTable();
            });
            foot.add(bedsBtn);
            JButton statusBtn = new JButton("Change Status");
            statusBtn.addActionListener(e -> {
                changeStatusDialog(h);
                dialog.dispose();
                refreshTable();
            });
            foot.add(statusBtn);
            JButton editBtn = new JButton("Edit Hospital");
            editBtn.addActionListener(e -> {
                editHospitalDialog(h);
                dialog.dispose();
                refreshTable();
            });
            foot.add(editBtn);
            if (SessionManager.getInstance().hasRole(RoleType.ADMIN)) {
                JButton deleteBtn = new JButton("Delete");
                deleteBtn.addActionListener(e -> {
                    deleteHospital(h);
                    dialog.dispose();
                    refreshTable();
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

    private JPanel buildProfileFacilitiesTab(Hospital h) {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        JLabel current = new JLabel("Current: " + h.facilitiesSummary());
        tab.add(current, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        if (write) {
            JComboBox<HospitalFacility> addCombo =
                    new JComboBox<>(HospitalFacility.values());
            JButton addBtn = new JButton("Add Facility");
            addBtn.addActionListener(e -> {
                ActionResult r = controller.addFacility(
                        String.valueOf(h.getId()),
                        (HospitalFacility) addCombo.getSelectedItem());
                if (r.isSuccess()) {
                    ViewUtil.info(this, r.getMessage());
                    Hospital updated = reload(h.getId());
                    current.setText("Current: "
                            + updated.facilitiesSummary());
                    refreshTable();
                } else {
                    ViewUtil.error(this, r.getMessage());
                }
            });
            JComboBox<HospitalFacility> removeCombo =
                    new JComboBox<>(HospitalFacility.values());
            JButton removeBtn = new JButton("Remove Facility");
            removeBtn.addActionListener(e -> {
                ActionResult r = controller.removeFacility(
                        String.valueOf(h.getId()),
                        (HospitalFacility) removeCombo.getSelectedItem());
                if (r.isSuccess()) {
                    ViewUtil.info(this, r.getMessage());
                    Hospital updated = reload(h.getId());
                    current.setText("Current: "
                            + updated.facilitiesSummary());
                    refreshTable();
                } else {
                    ViewUtil.error(this, r.getMessage());
                }
            });
            buttons.add(new JLabel("Add:"));
            buttons.add(addCombo);
            buttons.add(addBtn);
            buttons.add(Box.createHorizontalStrut(10));
            buttons.add(new JLabel("Remove:"));
            buttons.add(removeCombo);
            buttons.add(removeBtn);
            tab.add(buttons, BorderLayout.SOUTH);
        }
        return tab;
    }

    private JPanel buildProfileReferralsTab(long hospitalId) {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = ViewUtil.readOnlyModel(
                HospitalController.referralHeaders());
        JTable t = new JTable(model);
        try {
            Map<Long, String> hospitalNames = controller.hospitalNameMap();
            for (HospitalReferral r : controller.getReferrals(hospitalId)) {
                model.addRow(HospitalController.referralRow(r, hospitalNames));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        tab.add(new JScrollPane(t), BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildProfileLogsTab(long hospitalId) {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = ViewUtil.readOnlyModel(
                HospitalController.capacityLogHeaders());
        JTable t = new JTable(model);
        try {
            Map<Long, String> hospitalNames = controller.hospitalNameMap();
            for (HospitalCapacityLog log
                    : controller.getCapacityLogs(hospitalId)) {
                model.addRow(HospitalController.capacityLogRow(log,
                        hospitalNames));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        tab.add(new JScrollPane(t), BorderLayout.CENTER);
        return tab;
    }

    private void updateOccupancyDialog(Hospital h) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Update Occupancy - " + h.getName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel avail = new JLabel("<html>Current: <b>" + h.getOccupiedBeds()
                + "</b>/" + h.getTotalBeds() + "  |  Available: <b>"
                + h.availableBeds() + "</b></html>");
        JTextField newOcc = new JTextField(
                String.valueOf(h.getOccupiedBeds()), 6);
        JTextField reason = new JTextField(20);

        int row = 0;
        row = addRow(form, gbc, row, "", avail);
        row = addRow(form, gbc, row, "New occupied:",
                newOcc);
        row = addRow(form, gbc, row, "Reason:", reason);
        JButton saveBtn = new JButton("Update");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            ActionResult r = controller.updateOccupiedBeds(
                    String.valueOf(h.getId()), newOcc.getText(),
                    reason.getText());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void changeStatusDialog(Hospital h) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Change Status - " + h.getName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JComboBox<HospitalStatus> statusIn =
                new JComboBox<>(HospitalStatus.values());
        statusIn.setSelectedItem(h.getStatus());
        int row = addRow(form, gbc, 0, "New status:", statusIn);
        JButton saveBtn = new JButton("Change");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            ActionResult r = controller.setStatus(String.valueOf(h.getId()),
                    (HospitalStatus) statusIn.getSelectedItem());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void editHospitalDialog(Hospital h) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Edit Hospital - " + h.getName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameIn = new JTextField(h.getName(), 18);
        JTextField codeIn = new JTextField(h.getHospitalId(), 10);
        JTextField districtIn = new JTextField(h.getDistrict(), 14);
        JTextField cityIn = new JTextField(h.getCity(), 14);
        JTextField areaIn = new JTextField(h.getArea(), 14);
        JTextField addrIn = new JTextField(h.getAddress(), 18);
        JTextField phoneIn = new JTextField(h.getPhone(), 14);
        JTextField emergencyIn = new JTextField(h.getEmergencyContact(), 14);
        JTextField emailIn = new JTextField(h.getEmail(), 18);
        JTextField totalIn = new JTextField(String.valueOf(h.getTotalBeds()), 6);
        JTextField occIn = new JTextField(String.valueOf(h.getOccupiedBeds()), 6);

        Set<HospitalFacility> current = h.getFacilities();
        JCheckBox[] checks = new JCheckBox[HospitalFacility.values().length];
        JPanel facPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        HospitalFacility[] facVals = HospitalFacility.values();
        for (int i = 0; i < facVals.length; i++) {
            JCheckBox cb = new JCheckBox(facVals[i].getLabel(),
                    current.contains(facVals[i]));
            checks[i] = cb;
            facPanel.add(cb);
        }

        int row = 0;
        row = addRow(form, gbc, row, "Name:", nameIn);
        row = addRow(form, gbc, row, "Code:", codeIn);
        row = addRow(form, gbc, row, "District:", districtIn);
        row = addRow(form, gbc, row, "City:", cityIn);
        row = addRow(form, gbc, row, "Area:", areaIn);
        row = addRow(form, gbc, row, "Address:", addrIn);
        row = addRow(form, gbc, row, "Phone:", phoneIn);
        row = addRow(form, gbc, row, "Emergency:", emergencyIn);
        row = addRow(form, gbc, row, "Email:", emailIn);
        row = addRow(form, gbc, row, "Total beds:", totalIn);
        row = addRow(form, gbc, row, "Occupied beds:", occIn);
        row = addRow(form, gbc, row, "Facilities:", facPanel);

        JButton saveBtn = new JButton("Save Changes");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            Set<HospitalFacility> set = new LinkedHashSet<>();
            for (int i = 0; i < checks.length; i++) {
                if (checks[i].isSelected()) {
                    set.add(facVals[i]);
                }
            }
            ActionResult r = controller.updateHospital(
                    String.valueOf(h.getId()), nameIn.getText(),
                    codeIn.getText(), districtIn.getText(), cityIn.getText(),
                    areaIn.getText(), addrIn.getText(), phoneIn.getText(),
                    emergencyIn.getText(), emailIn.getText(),
                    totalIn.getText(), occIn.getText(), set);
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void deleteHospital(Hospital h) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete hospital '" + h.getName()
                        + "' and all its referrals/logs?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult r = controller.deleteHospital(String.valueOf(h.getId()));
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void referVictimDialog() {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Emergency Victim Referral",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(560, 620);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        List<Hospital> hospitals = safeAllHospitals();
        JComboBox<String> hospitalCombo = new JComboBox<>();
        Map<String, Hospital> byLabel = new java.util.HashMap<>();
        for (Hospital h : hospitals) {
            String label = h.getName() + " [av " + h.availableBeds() + "]";
            hospitalCombo.addItem(label);
            byLabel.put(label, h);
        }
        hospitalCombo.setSelectedItem(hospitalCombo.getItemCount() > 0
                ? hospitalCombo.getItemAt(0) : null);

        List<Victim> victims = safeVictims();
        JComboBox<String> victimCombo = new JComboBox<>();
        victimCombo.addItem("-- No victim record --");
        Map<String, Victim> victimByLabel = new java.util.HashMap<>();
        for (Victim v : victims) {
            String label = v.getId() + ": " + v.getFullName()
                    + (v.getMedicalCondition() == null ? ""
                            : " (" + v.getMedicalCondition() + ")");
            victimCombo.addItem(label);
            victimByLabel.put(label, v);
        }

        JTextField victimNameIn = new JTextField(16);
        JTextField bedsIn = new JTextField("1", 5);
        JTextField reasonIn = new JTextField(20);
        JTextArea notesIn = new JTextArea(2, 20);
        notesIn.setLineWrap(true);
        JComboBox<String> disasterCombo = buildDisasterCombo();

        Set<HospitalFacility> required = new LinkedHashSet<>();
        JCheckBox[] reqChecks = new JCheckBox[HospitalFacility.values().length];
        JPanel reqPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        HospitalFacility[] facVals = HospitalFacility.values();
        for (int i = 0; i < facVals.length; i++) {
            JCheckBox cb = new JCheckBox(facVals[i].getLabel());
            reqChecks[i] = cb;
            reqPanel.add(cb);
        }

        victimCombo.addActionListener(e -> {
            String sel = String.valueOf(victimCombo.getSelectedItem());
            Victim v = victimByLabel.get(sel);
            if (v != null) {
                victimNameIn.setText(v.getFullName());
            }
        });

        int row = 0;
        row = addRow(form, gbc, row, "Hospital:", hospitalCombo);
        row = addRow(form, gbc, row, "Victim:",
                wrap(victimCombo, victimNameIn, "or type victim name:"));
        row = addRow(form, gbc, row, "Beds required:", bedsIn);
        row = addRow(form, gbc, row, "Reason:", reasonIn);
        row = addRow(form, gbc, row, "Disaster (optional):", disasterCombo);
        row = addRow(form, gbc, row, "Required facilities:", reqPanel);
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.add(new JScrollPane(notesIn), BorderLayout.CENTER);
        row = addRow(form, gbc, row, "Notes:", notesPanel);

        JButton referBtn = new JButton("Create Referral");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(referBtn, gbc);
        referBtn.addActionListener(e -> {
            Hospital selected = hospitalCombo.getSelectedItem() == null
                    ? null : byLabel.get(String.valueOf(
                            hospitalCombo.getSelectedItem()));
            if (selected == null) {
                ViewUtil.error(dialog, "No hospital available");
                return;
            }
            Set<HospitalFacility> set = new LinkedHashSet<>();
            for (int i = 0; i < reqChecks.length; i++) {
                if (reqChecks[i].isSelected()) {
                    set.add(facVals[i]);
                }
            }
            String victimSel = String.valueOf(victimCombo.getSelectedItem());
            Victim v = victimByLabel.get(victimSel);
            String victimIdText = v == null ? ""
                    : String.valueOf(v.getId());
            ActionResult r = controller.referVictim(
                    String.valueOf(selected.getId()), victimIdText,
                    bedsIn.getText(), set, reasonIn.getText(),
                    notesIn.getText(), selectedDisasterId(disasterCombo));
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
                refreshReferrals();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });

        JScrollPane scroll = new JScrollPane(form);
        dialog.setContentPane(scroll);
        dialog.setVisible(true);
    }

    private JPanel wrap(JComboBox<String> combo, JTextField field, String label) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.add(combo, BorderLayout.NORTH);
        JPanel sub = new JPanel(new BorderLayout(4, 2));
        sub.add(new JLabel(label), BorderLayout.WEST);
        sub.add(field, BorderLayout.CENTER);
        p.add(sub, BorderLayout.CENTER);
        return p;
    }

    private void updateReferralStatusDialog() {
        int viewRow = referralTable.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a referral in the table first");
            return;
        }
        Long id = (Long) referralModel.getValueAt(viewRow, 0);

        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Update Referral Status",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JComboBox<HospitalReferralStatus> statusIn =
                new JComboBox<>(HospitalReferralStatus.values());
        int row = addRow(form, gbc, 0,
                "Set status for referral #" + id + ":", statusIn);
        JButton saveBtn = new JButton("Update");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            ActionResult r = controller.changeReferralStatus(
                    String.valueOf(id),
                    (HospitalReferralStatus) statusIn.getSelectedItem());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
                refreshReferrals();
                refreshTable();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ===================== DATA LOADERS ==============================

    private void refreshTable() {
        refreshHospitals();
        refreshReferrals();
        refreshLogs();
    }

    private void refreshHospitals() {
        hospitalModel.setRowCount(0);
        int total = 0, accepting = 0, near = 0, full = 0, beds = 0;
        try {
            List<Hospital> hospitals = controller.getAllHospitals();
            Map<Long, String> disasterNames = controller.disasterNameMap();
            for (Hospital h : hospitals) {
                total++;
                if (h.getStatus().isAccepting()) {
                    accepting++;
                }
                if (h.isNearCapacity()) {
                    near++;
                }
                if (h.getStatus() == HospitalStatus.FULL) {
                    full++;
                }
                beds += h.availableBeds();
                hospitalModel.addRow(HospitalController.hospitalRow(h,
                        disasterNames));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        totalTile.setText(String.valueOf(total));
        acceptingTile.setText(String.valueOf(accepting));
        nearTile.setText(String.valueOf(near));
        fullTile.setText(String.valueOf(full));
        bedsTile.setText(String.valueOf(beds));
    }

    private void refreshReferrals() {
        referralModel.setRowCount(0);
        try {
            String sel = String.valueOf(referralStatusFilter.getSelectedItem());
            List<HospitalReferral> referrals;
            switch (sel) {
                case "Open" -> referrals = controller.getOpenReferrals();
                case "Pending" -> referrals =
                        controller.getReferralsByStatus(
                                HospitalReferralStatus.PENDING);
                case "Accepted" -> referrals =
                        controller.getReferralsByStatus(
                                HospitalReferralStatus.ACCEPTED);
                case "Admitted" -> referrals =
                        controller.getReferralsByStatus(
                                HospitalReferralStatus.ADMITTED);
                case "Discharged" -> referrals =
                        controller.getReferralsByStatus(
                                HospitalReferralStatus.DISCHARGED);
                case "Rejected" -> referrals =
                        controller.getReferralsByStatus(
                                HospitalReferralStatus.REJECTED);
                case "Cancelled" -> referrals =
                        controller.getReferralsByStatus(
                                HospitalReferralStatus.CANCELLED);
                default -> referrals = controller.getAllReferrals();
            }
            Map<Long, String> hospitalNames = controller.hospitalNameMap();
            for (HospitalReferral r : referrals) {
                referralModel.addRow(HospitalController.referralRow(r,
                        hospitalNames));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshLogs() {
        logModel.setRowCount(0);
        try {
            List<HospitalCapacityLog> logs = controller.getAllCapacityLogs();
            Map<Long, String> hospitalNames = controller.hospitalNameMap();
            for (HospitalCapacityLog log : logs) {
                logModel.addRow(HospitalController.capacityLogRow(log,
                        hospitalNames));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private Hospital reload(long id) {
        try {
            Hospital h = controller.getHospital(id);
            if (h != null) {
                currentHospital = h;
            }
            return h == null ? currentHospital : h;
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return currentHospital;
        }
    }

    private List<Hospital> safeAllHospitals() {
        try {
            return controller.getAllHospitals();
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Victim> safeVictims() {
        try {
            return controller.getAllVictims();
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return new ArrayList<>();
        }
    }

    private JComboBox<String> buildDisasterCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem("-- No disaster --");
        try {
            for (Disaster d : controller.getDisasters()) {
                combo.addItem(d.getId() + ": " + d.getTitle());
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        return combo;
    }

    private String selectedDisasterId(JComboBox<String> combo) {
        Object sel = combo.getSelectedItem();
        if (sel == null) {
            return "";
        }
        String s = String.valueOf(sel);
        if (s.startsWith("--")) {
            return "";
        }
        int idx = s.indexOf(':');
        return idx > 0 ? s.substring(0, idx).trim() : s;
    }

    // ===================== HELPERS ===================================

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

    @Override
    public void refreshData() {
        refreshTable();
    }
}
