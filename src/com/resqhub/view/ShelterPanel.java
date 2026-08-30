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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.ShelterController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterFacility;
import com.resqhub.model.ShelterOperationalStatus;
import com.resqhub.model.RoleType;
import com.resqhub.service.SessionManager;

/**
 * Shelter Management screen: registration, search / filter by status
 * and location, a shelter profile dialog with facilities and allocation
 * tabs, guided capacity monitoring (near-full / full) and victim /
 * family allocation with overcapacity protection.
 */
public class ShelterPanel extends JPanel implements Refreshable {

    private final ShelterController controller = new ShelterController();
    private final boolean write;

    private final JTextField nameField = new JTextField(16);
    private final JTextField codeField = new JTextField(10);
    private final JTextField districtField = new JTextField(14);
    private final JTextField cityField = new JTextField(14);
    private final JTextField maxCapField = new JTextField(6);
    private final JTextField occField = new JTextField(6);
    private final JComboBox<ShelterOperationalStatus> statusCombo =
            new JComboBox<>(ShelterOperationalStatus.values());

    private final DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(ShelterController.tableHeaders());
    private final JTable table = new JTable(tableModel);

    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"All Statuses", "Available", "Active",
                    "Near Capacity", "Full", "Inactive", "Closed"});
    private final JComboBox<String> viewFilter = new JComboBox<>(
            new String[]{"All Shelters", "Available (space)", "Near Capacity",
                    "Full", "Accessible", "Wheelchair Accessible"});

    private final JLabel totalTile = new JLabel("0");
    private final JLabel availableTile = new JLabel("0");
    private final JLabel nearTile = new JLabel("0");
    private final JLabel fullTile = new JLabel("0");

    public ShelterPanel() {
        write = SessionManager.getInstance().hasRole(RoleType.ADMIN,
                RoleType.RESCUE_OFFICER, RoleType.CAMP_MANAGER);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.WEST);
        add(buildTableArea(), BorderLayout.CENTER);
        refreshTable();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("SHELTER MANAGEMENT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("TOTAL", totalTile, new Color(60, 60, 60)));
        tiles.add(statTile("WITH SPACE", availableTile,
                new Color(40, 110, 40)));
        tiles.add(statTile("NEAR CAPACITY", nearTile,
                new Color(200, 130, 20)));
        tiles.add(statTile("FULL", fullTile, new Color(150, 30, 30)));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(title, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        return north;
    }

    private JPanel statTile(String caption, JLabel valueLabel,
                            Color color) {
        valueLabel.setFont(
                valueLabel.getFont().deriveFont(Font.BOLD, 24f));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(
                javax.swing.SwingConstants.CENTER);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setHorizontalAlignment(
                javax.swing.SwingConstants.CENTER);
        captionLabel.setFont(captionLabel.getFont().deriveFont(11f));
        captionLabel.setForeground(new Color(90, 90, 90));

        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setPreferredSize(new Dimension(110, 80));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(captionLabel);
        tile.add(Box.createVerticalGlue());
        return tile;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Register shelter"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addRow(form, gbc, row, "Name:", nameField);
        row = addRow(form, gbc, row, "Shelter code:", codeField);
        row = addRow(form, gbc, row, "District:", districtField);
        row = addRow(form, gbc, row, "City:", cityField);
        row = addRow(form, gbc, row, "Max capacity:", maxCapField);
        row = addRow(form, gbc, row, "Current occupancy:", occField);
        row = addRow(form, gbc, row, "Status:", statusCombo);

        JButton registerButton = new JButton("Register shelter");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(registerButton, gbc);
        registerButton.addActionListener(event -> registerShelter());
        return form;
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

    private JPanel buildTableArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Search:"));
        controls.add(searchField);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(event -> refreshTable());
        controls.add(searchBtn);
        JButton showAllBtn = new JButton("Show All");
        showAllBtn.addActionListener(event -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            viewFilter.setSelectedIndex(0);
            refreshTable();
        });
        controls.add(showAllBtn);

        controls.add(Box.createHorizontalStrut(10));
        controls.add(new JLabel("Status:"));
        controls.add(statusFilter);
        controls.add(new JLabel("View:"));
        controls.add(viewFilter);

        JButton detailBtn = new JButton("View Profile");
        detailBtn.addActionListener(event -> viewProfile());
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(event ->
                ViewUtil.exportTableToCsv(this, table, "shelters"));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        actions.add(detailBtn);
        actions.add(exportBtn);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.add(controls);
        northStack.add(actions);
        area.add(northStack, BorderLayout.NORTH);
        return area;
    }

    private void registerShelter() {
        RegistrationArgs args = registrationArgs();
        ActionResult r = controller.createShelter(
                args.name, args.code, args.district, args.city, null, null,
                args.maxCap, args.occ, null, null, null,
                false, false, false, false,
                (ShelterOperationalStatus) statusCombo.getSelectedItem());
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            nameField.setText("");
            codeField.setText("");
            districtField.setText("");
            cityField.setText("");
            maxCapField.setText("");
            occField.setText("");
            refreshTable();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private RegistrationArgs registrationArgs() {
        RegistrationArgs a = new RegistrationArgs();
        a.name = nameField.getText();
        a.code = codeField.getText();
        a.district = districtField.getText();
        a.city = cityField.getText();
        a.maxCap = maxCapField.getText();
        a.occ = occField.getText();
        return a;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        int total = 0, withSpace = 0, near = 0, full = 0;
        try {
            String needle = searchField.getText() == null
                    ? "" : searchField.getText().trim();
            String statusSel = String.valueOf(statusFilter.getSelectedItem());
            String viewSel = String.valueOf(viewFilter.getSelectedItem());

            List<Shelter> shelters;
            if (!needle.isEmpty()) {
                shelters = controller.search(needle);
            } else {
                shelters = controller.getAllShelters();
            }

            for (Shelter s : shelters) {
                if (!"All Statuses".equals(statusSel)
                        && !s.getOperationalStatus().getLabel()
                                .equals(statusSel)) {
                    continue;
                }
                if (!"All Shelters".equals(viewSel)) {
                    switch (viewSel) {
                        case "Available (space)" ->
                            { if (s.availableCapacity() <= 0) { continue; } }
                        case "Near Capacity" ->
                            { if (!s.isNearCapacity()) { continue; } }
                        case "Full" ->
                            { if (s.availableCapacity() != 0) { continue; } }
                        case "Accessible" ->
                            { if (!(s.isWheelchairAccessible()
                                    || s.isElderlyFriendly()
                                    || s.isMedicalAccessible()
                                    || s.isSpecialAssistance())) { continue; } }
                        case "Wheelchair Accessible" ->
                            { if (!s.isWheelchairAccessible()) { continue; } }
                        default -> { }
                    }
                }
                total++;
                if (s.availableCapacity() > 0) {
                    withSpace++;
                }
                if (s.getOperationalStatus()
                        == ShelterOperationalStatus.NEAR_CAPACITY) {
                    near++;
                }
                if (s.availableCapacity() == 0) {
                    full++;
                }
                tableModel.addRow(ShelterController.toRow(s));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        totalTile.setText(String.valueOf(total));
        availableTile.setText(String.valueOf(withSpace));
        nearTile.setText(String.valueOf(near));
        fullTile.setText(String.valueOf(full));
    }

    private void viewProfile() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a shelter in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            Shelter shelter = controller.getAllShelters().stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst().orElse(null);
            if (shelter == null) {
                ViewUtil.error(this, "Shelter not found: " + id);
                return;
            }
            showProfileDialog(shelter);
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void showProfileDialog(Shelter shelter) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Shelter Profile - " + shelter.getName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(820, 600);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder(
                "Shelter Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int r = 0;
        r = addInfoRow(infoPanel, gbc, r, "Name:", shelter.getName());
        r = addInfoRow(infoPanel, gbc, r, "Code:", shelter.getCode());
        r = addInfoRow(infoPanel, gbc, r, "Location:",
                shelter.getDistrict() + (shelter.getCity() == null ? ""
                        : ", " + shelter.getCity()));
        r = addInfoRow(infoPanel, gbc, r, "Address:",
                shelter.getAddress() == null ? "-" : shelter.getAddress());
        r = addInfoRow(infoPanel, gbc, r, "Capacity / Occupancy:",
                shelter.getMaxCapacity() + " / " + shelter.getCurrentOccupancy());
        r = addInfoRow(infoPanel, gbc, r, "Available:",
                String.valueOf(shelter.availableCapacity()));
        r = addInfoRow(infoPanel, gbc, r, "Status:",
                shelter.getOperationalStatus().getLabel());
        r = addInfoRow(infoPanel, gbc, r, "Contact:",
                shelter.getContactNumber() == null ? "-"
                        : shelter.getContactNumber());
        r = addInfoRow(infoPanel, gbc, r, "Manager:",
                shelter.getManagerName() == null ? "-"
                        : shelter.getManagerName());
        r = addInfoRow(infoPanel, gbc, r, "Accessibility:",
                accessibilityText(shelter));

        content.add(infoPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Facilities", buildFacilitiesTab(shelter.getId()));
        tabs.add("Allocations", buildAllocationsTab(shelter.getId()));
        content.add(tabs, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(event -> dialog.dispose());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (write) {
            JButton allocateBtn = new JButton("Allocate Victim/Family");
            allocateBtn.addActionListener(event -> {
                allocateDialog(shelter);
                refreshTable();
            });
            foot.add(allocateBtn);
            JButton editBtn = new JButton("Edit Shelter");
            editBtn.addActionListener(event -> {
                editShelterDialog(shelter);
                refreshTable();
            });
            foot.add(editBtn);
        }
        foot.add(closeBtn);
        content.add(foot, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private JPanel buildFacilitiesTab(long shelterId) {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = ViewUtil.readOnlyModel(
                ShelterController.facilityHeaders());
        JTable ft = new JTable(model);
        try {
            for (ShelterFacility f : controller.getFacilities(shelterId)) {
                model.addRow(ShelterController.facilityRow(f));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        tab.add(new JScrollPane(ft), BorderLayout.CENTER);

        if (write) {
            JTextField nameIn = new JTextField(14);
            JCheckBox availIn = new JCheckBox("Available", true);
            JButton addBtn = new JButton("Add");
            JButton removeBtn = new JButton("Remove Selected");
            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            bar.add(new JLabel("Facility:"));
            bar.add(nameIn);
            bar.add(availIn);
            bar.add(addBtn);
            bar.add(removeBtn);
            addBtn.addActionListener(e -> {
                ActionResult r = controller.addFacility(shelterId,
                        nameIn.getText(), availIn.isSelected());
                if (r.isSuccess()) {
                    ViewUtil.info(this, r.getMessage());
                    nameIn.setText("");
                    model.setRowCount(0);
                    try {
                        for (ShelterFacility f
                                : controller.getFacilities(shelterId)) {
                            model.addRow(ShelterController.facilityRow(f));
                        }
                    } catch (DataAccessException ex) {
                        ViewUtil.error(this, ex.getMessage());
                    }
                } else {
                    ViewUtil.error(this, r.getMessage());
                }
            });
            removeBtn.addActionListener(e -> {
                int row = ft.getSelectedRow();
                if (row < 0) {
                    ViewUtil.error(this, "Select a facility first");
                    return;
                }
                String name = String.valueOf(model.getValueAt(row, 1));
                ActionResult r = controller.removeFacility(shelterId, name);
                if (r.isSuccess()) {
                    model.removeRow(row);
                } else {
                    ViewUtil.error(this, r.getMessage());
                }
            });
            tab.add(bar, BorderLayout.SOUTH);
        }
        return tab;
    }

    private JPanel buildAllocationsTab(long shelterId) {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = ViewUtil.readOnlyModel(
                ShelterController.allocationHeaders());
        JTable at = new JTable(model);
        try {
            for (ShelterAllocation a : controller.getAllocations(shelterId)) {
                model.addRow(ShelterController.allocationRow(a));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        tab.add(new JScrollPane(at), BorderLayout.CENTER);

        if (write) {
            JButton releaseBtn = new JButton("Release Selected");
            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            bar.add(releaseBtn);
            releaseBtn.addActionListener(e -> {
                int row = at.getSelectedRow();
                if (row < 0) {
                    ViewUtil.error(this, "Select an allocation first");
                    return;
                }
                long id = (Long) model.getValueAt(row, 0);
                if (JOptionPane.showConfirmDialog(this,
                        "Release this allocation? Occupancy will decrease.",
                        "Confirm release", JOptionPane.YES_NO_OPTION)
                        != JOptionPane.YES_OPTION) {
                    return;
                }
                ActionResult r = controller.release(id);
                if (r.isSuccess()) {
                    ViewUtil.info(this, r.getMessage());
                    model.setRowCount(0);
                    try {
                        for (ShelterAllocation a
                                : controller.getAllocations(shelterId)) {
                            model.addRow(ShelterController.allocationRow(a));
                        }
                    } catch (DataAccessException ex) {
                        ViewUtil.error(this, ex.getMessage());
                    }
                    refreshTable();
                } else {
                    ViewUtil.error(this, r.getMessage());
                }
            });
            tab.add(bar, BorderLayout.SOUTH);
        }
        return tab;
    }

    private void allocateDialog(Shelter shelter) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Allocate to " + shelter.getName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel availLabel = new JLabel("<html>Available capacity: <b>"
                + shelter.availableCapacity() + "</b></html>");
        JTextField victimIn = new JTextField(12);
        JTextField familyIn = new JTextField(18);
        JTextField peopleIn = new JTextField(String.valueOf(1), 5);
        JTextField notesIn = new JTextField(18);

        int row = 0;
        row = addRow(form, gbc, row, "", availLabel);
        row = addRow(form, gbc, row, "Victim ID (optional):", victimIn);
        row = addRow(form, gbc, row, "Family / person name:", familyIn);
        row = addRow(form, gbc, row, "People count:", peopleIn);
        row = addRow(form, gbc, row, "Notes:", notesIn);

        JButton allocBtn = new JButton("Allocate");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(allocBtn, gbc);
        allocBtn.addActionListener(e -> {
            ActionResult r = controller.allocate(shelter.getId(),
                    victimIn.getText(), familyIn.getText(),
                    peopleIn.getText(), notesIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });

        dialog.setContentPane(form);
        dialog.setVisible(true);
    }

    private void editShelterDialog(Shelter shelter) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Edit Shelter - " + shelter.getName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(460, 520);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameIn = new JTextField(shelter.getName(), 18);
        JTextField codeIn = new JTextField(shelter.getCode(), 12);
        JTextField districtIn = new JTextField(shelter.getDistrict(), 16);
        JTextField cityIn = new JTextField(shelter.getCity(), 16);
        JTextField addrIn = new JTextField(shelter.getAddress(), 18);
        JTextField maxIn = new JTextField(
                String.valueOf(shelter.getMaxCapacity()), 6);
        JTextField occIn = new JTextField(
                String.valueOf(shelter.getCurrentOccupancy()), 6);
        JTextField contactIn = new JTextField(shelter.getContactNumber(), 12);
        JTextField managerIn = new JTextField(shelter.getManagerName(), 18);
        JComboBox<ShelterOperationalStatus> statusIn =
                new JComboBox<>(ShelterOperationalStatus.values());
        statusIn.setSelectedItem(shelter.getOperationalStatus());
        JCheckBox wcIn = new JCheckBox("Wheelchair accessible",
                shelter.isWheelchairAccessible());
        JCheckBox elIn = new JCheckBox("Elderly friendly",
                shelter.isElderlyFriendly());
        JCheckBox mdIn = new JCheckBox("Medical accessible",
                shelter.isMedicalAccessible());
        JCheckBox spIn = new JCheckBox("Special assistance",
                shelter.isSpecialAssistance());

        int row = 0;
        row = addRow(form, gbc, row, "Name:", nameIn);
        row = addRow(form, gbc, row, "Code:", codeIn);
        row = addRow(form, gbc, row, "District:", districtIn);
        row = addRow(form, gbc, row, "City:", cityIn);
        row = addRow(form, gbc, row, "Address:", addrIn);
        row = addRow(form, gbc, row, "Max capacity:", maxIn);
        row = addRow(form, gbc, row, "Current occupancy:", occIn);
        row = addRow(form, gbc, row, "Contact:", contactIn);
        row = addRow(form, gbc, row, "Manager:", managerIn);
        row = addRow(form, gbc, row, "Status:", statusIn);
        JPanel acc = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        acc.add(wcIn);
        acc.add(elIn);
        acc.add(mdIn);
        acc.add(spIn);
        row = addRow(form, gbc, row, "Accessibility:", acc);

        JButton saveBtn = new JButton("Save Changes");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);
        saveBtn.addActionListener(e -> {
            ActionResult r = controller.updateShelter(shelter.getId(),
                    nameIn.getText(), codeIn.getText(),
                    districtIn.getText(), cityIn.getText(), addrIn.getText(),
                    null, maxIn.getText(), occIn.getText(),
                    contactIn.getText(), managerIn.getText(), null,
                    wcIn.isSelected(), elIn.isSelected(),
                    mdIn.isSelected(), spIn.isSelected(),
                    (ShelterOperationalStatus) statusIn.getSelectedItem());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
                refreshTable();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });

        dialog.setContentPane(form);
        dialog.setVisible(true);
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

    private String accessibilityText(Shelter s) {
        StringBuilder sb = new StringBuilder();
        if (s.isWheelchairAccessible()) {
            sb.append("Wheelchair, ");
        }
        if (s.isElderlyFriendly()) {
            sb.append("Elderly, ");
        }
        if (s.isMedicalAccessible()) {
            sb.append("Medical, ");
        }
        if (s.isSpecialAssistance()) {
            sb.append("Special, ");
        }
        return sb.length() == 0 ? "None" : sb.substring(0, sb.length() - 2);
    }

    @Override
    public void refreshData() {
        refreshTable();
    }

    private static class RegistrationArgs {
        String name;
        String code;
        String district;
        String city;
        String maxCap;
        String occ;
    }
}
