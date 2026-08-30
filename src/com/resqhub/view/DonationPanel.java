package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.DonationController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.Donation;
import com.resqhub.model.DonationDistribution;
import com.resqhub.model.DonationStatus;
import com.resqhub.model.DonationType;
import com.resqhub.model.Donor;
import com.resqhub.model.DonorType;
import com.resqhub.model.RoleType;
import com.resqhub.service.DonationService;
import com.resqhub.service.SessionManager;

/**
 * Donation management: donor registration, cash & material donation
 * recording, donation tracking/filtering, distribution tracking with
 * quantity validation, status lifecycle, donor profiles and statistics.
 */
public class DonationPanel extends JPanel implements Refreshable {

    private final DonationController controller =
            new DonationController();

    // ── donor registration form ──────────────────────────────────────
    private final JTextField donorNameField = new JTextField(16);
    private final JTextField donorContactField = new JTextField(12);
    private final JTextField donorEmailField = new JTextField(16);
    private final JTextField donorLocationField = new JTextField(16);
    private final JComboBox<DonorType> donorTypeCombo =
            new JComboBox<>(DonorType.values());

    // ── donation recording form ──────────────────────────────────────
    private final JComboBox<String> donorSelectCombo = new JComboBox<>();
    private final JComboBox<DonationType> donationTypeCombo =
            new JComboBox<>(DonationType.values());
    private final JTextField amountField = new JTextField(10);
    private final JTextField materialNameField = new JTextField(12);
    private final JTextField quantityField = new JTextField(5);
    private final JTextField donationDescField = new JTextField(16);

    // ── tables ───────────────────────────────────────────────────────
    private final DefaultTableModel donationModel =
            ViewUtil.readOnlyModel(DonationController.donationTableHeaders());
    private final JTable donationTable = new JTable(donationModel);
    private final DefaultTableModel donorModel =
            ViewUtil.readOnlyModel(DonationController.donorTableHeaders());
    private final JTable donorTable = new JTable(donorModel);

    // ── controls ─────────────────────────────────────────────────────
    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> typeFilterCombo = new JComboBox<>(
            new String[]{"All", "Cash", "Material"});
    private final JComboBox<String> statusFilterCombo = new JComboBox<>(
            new String[]{"All", "Received", "Allocated",
                    "Partially Distributed", "Distributed"});

    // ── stat tiles ───────────────────────────────────────────────────
    private final JLabel totalDonationsTile = new JLabel("0");
    private final JLabel cashTile = new JLabel("\u20B90");
    private final JLabel donorsTile = new JLabel("0");
    private final JLabel remainingTile = new JLabel("0");

    public DonationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        refreshDonorSelect();
        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane centerTabs = new JTabbedPane();
        centerTabs.add("Donations", buildDonationsArea());
        centerTabs.add("Donors", buildDonorsArea());
        add(centerTabs, BorderLayout.CENTER);

        add(buildSidePanel(), BorderLayout.EAST);
        donationTypeCombo.addActionListener(event -> applyTypeFields());
        applyTypeFields();
        refreshData();
    }

    /** Shows only the fields relevant to the selected donation type. */
    private void applyTypeFields() {
        boolean cash = donationTypeCombo.getSelectedItem()
                == DonationType.CASH;
        amountField.setEnabled(cash);
        materialNameField.setEnabled(!cash);
        quantityField.setEnabled(!cash);
    }

    /** Scrollable action panel on the right for inputs. */
    private JScrollPane buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(320, 0));

        JLabel header = new JLabel("  Quick Actions");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        side.add(header);

        side.add(buildDonorForm());
        side.add(Box.createVerticalStrut(10));
        side.add(buildDonationForm());

        JScrollPane scroll = new JScrollPane(side,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ── header with stat tiles ───────────────────────────────────────

    private JPanel buildHeader() {
        JLabel title = new JLabel("DONATION MANAGEMENT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("TOTAL DONATIONS", totalDonationsTile,
                new Color(60, 60, 60)));
        tiles.add(statTile("TOTAL CASH", cashTile,
                new Color(20, 110, 70)));
        tiles.add(statTile("DONORS", donorsTile,
                new Color(40, 100, 160)));
        tiles.add(statTile("UNITS REMAINING", remainingTile,
                new Color(140, 110, 20)));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(title, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        return north;
    }

    private JPanel statTile(String caption, JLabel valueLabel,
                            Color color) {
        valueLabel.setFont(
                valueLabel.getFont().deriveFont(Font.BOLD, 22f));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(
                javax.swing.SwingConstants.CENTER);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setHorizontalAlignment(
                javax.swing.SwingConstants.CENTER);
        captionLabel.setFont(captionLabel.getFont().deriveFont(10f));
        captionLabel.setForeground(Color.WHITE);
        captionLabel.setOpaque(true);
        captionLabel.setBackground(color);
        captionLabel.setBorder(BorderFactory.createEmptyBorder(
                3, 4, 3, 4));

        JPanel tile = new JPanel();
        tile.setLayout(new BorderLayout());
        tile.setPreferredSize(new Dimension(120, 78));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(valueLabel, BorderLayout.CENTER);
        tile.add(captionLabel, BorderLayout.SOUTH);
        return tile;
    }

    // ── forms panel: register donor + record donation ────────────────

    private JPanel buildDonorForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(
                        new Color(150, 150, 150)),
                "Register Donor"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        setFieldWidth(donorNameField, 200);
        setFieldWidth(donorContactField, 200);
        setFieldWidth(donorEmailField, 200);
        setFieldWidth(donorLocationField, 200);

        int row = 0;
        row = addRow(form, gbc, row, "Name:", donorNameField);
        row = addRow(form, gbc, row, "Contact:", donorContactField);
        row = addRow(form, gbc, row, "Email:", donorEmailField);
        row = addRow(form, gbc, row, "Location:", donorLocationField);
        row = addRow(form, gbc, row, "Type:", donorTypeCombo);

        JButton registerBtn = new JButton("Register donor");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        form.add(registerBtn, gbc);
        registerBtn.addActionListener(event -> registerDonor());
        return form;
    }

    private JPanel buildDonationForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(
                        new Color(150, 150, 150)),
                "Record Donation"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        setFieldWidth(amountField, 120);
        setFieldWidth(materialNameField, 120);
        setFieldWidth(quantityField, 80);
        setFieldWidth(donationDescField, 200);

        int row = 0;
        row = addRow(form, gbc, row, "Donor:", donorSelectCombo);
        row = addRow(form, gbc, row, "Type:", donationTypeCombo);
        row = addRow(form, gbc, row, "Amount (\u20B9):", amountField);
        row = addRow(form, gbc, row, "Item:", materialNameField);
        row = addRow(form, gbc, row, "Quantity:", quantityField);
        row = addRow(form, gbc, row, "Description:", donationDescField);

        JButton recordBtn = new JButton("Record donation");
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        form.add(recordBtn, gbc);
        recordBtn.addActionListener(event -> recordDonation());
        return form;
    }

    private void setFieldWidth(javax.swing.JComponent field, int px) {
        Dimension d = field.getPreferredSize();
        field.setPreferredSize(new Dimension(px, d.height));
    }

    private int addRow(JPanel form, GridBagConstraints gbc, int row,
                       String label, javax.swing.JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
        return row + 1;
    }

    // ── donations area ───────────────────────────────────────────────

    private JPanel buildDonationsArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(donationTable), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT,
                6, 2));
        filterRow.add(new JLabel("Search:"));
        filterRow.add(searchField);
        JButton searchBtn = new JButton("Search");
        filterRow.add(searchBtn);
        JButton showAllBtn = new JButton("Show All");
        filterRow.add(showAllBtn);
        filterRow.add(Box.createHorizontalStrut(10));
        filterRow.add(new JLabel("Type:"));
        filterRow.add(typeFilterCombo);
        filterRow.add(new JLabel("Status:"));
        filterRow.add(statusFilterCombo);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT,
                6, 2));
        actionRow.add(new JLabel("Selected:"));
        JButton viewDonBtn = new JButton("View Donation");
        actionRow.add(viewDonBtn);
        JButton distributeBtn = new JButton("Distribute");
        actionRow.add(distributeBtn);
        JButton updateStatusBtn = new JButton("Update Status");
        actionRow.add(updateStatusBtn);
        JButton editDonBtn = new JButton("Edit");
        actionRow.add(editDonBtn);
        JButton deleteDonBtn = new JButton("Delete");
        deleteDonBtn.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));
        actionRow.add(deleteDonBtn);
        JButton exportBtn = new JButton("Export CSV");
        actionRow.add(exportBtn);

        controls.add(filterRow);
        controls.add(actionRow);
        area.add(controls, BorderLayout.NORTH);

        searchBtn.addActionListener(event -> refreshDonations());
        showAllBtn.addActionListener(event -> {
            searchField.setText("");
            typeFilterCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            refreshDonations();
        });
        typeFilterCombo.addActionListener(event -> refreshDonations());
        statusFilterCombo.addActionListener(event -> refreshDonations());
        viewDonBtn.addActionListener(event -> viewDonation());
        distributeBtn.addActionListener(event -> openDistributeDialog());
        updateStatusBtn.addActionListener(event -> openStatusDialog());
        editDonBtn.addActionListener(event -> editDonation());
        deleteDonBtn.addActionListener(event -> deleteDonation());
        exportBtn.addActionListener(event ->
                ViewUtil.exportTableToCsv(this, donationTable,
                        "donations"));
        return area;
    }

    // ── donors area ──────────────────────────────────────────────────

    private JPanel buildDonorsArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(donorTable), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JTextField donorSearchField = new JTextField(16);
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT,
                6, 2));
        filterRow.add(new JLabel("Search donors:"));
        filterRow.add(donorSearchField);
        JButton searchBtn = new JButton("Search");
        filterRow.add(searchBtn);
        JButton showAllBtn = new JButton("Show All");
        filterRow.add(showAllBtn);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT,
                6, 2));
        actionRow.add(new JLabel("Selected:"));
        JButton profileBtn = new JButton("View Profile");
        actionRow.add(profileBtn);
        JButton editDonorBtn = new JButton("Edit");
        actionRow.add(editDonorBtn);
        JButton deleteDonorBtn = new JButton("Delete");
        deleteDonorBtn.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));
        actionRow.add(deleteDonorBtn);
        JButton exportDonorBtn = new JButton("Export CSV");
        actionRow.add(exportDonorBtn);

        controls.add(filterRow);
        controls.add(actionRow);
        area.add(controls, BorderLayout.NORTH);

        searchBtn.addActionListener(event -> refreshDonors(
                donorSearchField.getText()));
        showAllBtn.addActionListener(event -> {
            donorSearchField.setText("");
            refreshDonors("");
        });
        profileBtn.addActionListener(event -> viewDonorProfile());
        editDonorBtn.addActionListener(event -> editDonor());
        deleteDonorBtn.addActionListener(event -> deleteDonor());
        exportDonorBtn.addActionListener(event ->
                ViewUtil.exportTableToCsv(this, donorTable, "donors"));
        return area;
    }

    // ── refresh logic ────────────────────────────────────────────────

    private void refreshDonations() {
        donationModel.setRowCount(0);
        try {
            String needle = searchField.getText() == null
                    ? "" : searchField.getText().trim();
            String typeFilter = String.valueOf(
                    typeFilterCombo.getSelectedItem());
            String statusFilter = String.valueOf(
                    statusFilterCombo.getSelectedItem());

            List<Donation> donations;
            if (!needle.isEmpty()) {
                donations = controller.searchDonations(needle);
            } else {
                donations = controller.getAllDonations();
            }
            for (Donation d : donations) {
                if (!"All".equals(typeFilter)
                        && d.getDonationType() != null
                        && !d.getDonationType().getLabel()
                                .equals(typeFilter)) {
                    continue;
                }
                if (!"All".equals(statusFilter)
                        && (d.getStatus() == null
                            || !d.getStatus().getLabel()
                                    .equals(statusFilter))) {
                    continue;
                }
                donationModel.addRow(
                        DonationController.toDonationRow(d));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshDonors(String keyword) {
        donorModel.setRowCount(0);
        try {
            List<Donor> donors = keyword == null || keyword.isEmpty()
                    ? controller.getAllDonors()
                    : controller.searchDonors(keyword);
            for (Donor d : donors) {
                donorModel.addRow(DonationController.toDonorRow(d));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshDonorSelect() {
        Object selected = donorSelectCombo.getSelectedItem();
        donorSelectCombo.removeAllItems();
        try {
            for (Donor d : controller.getAllDonors()) {
                donorSelectCombo.addItem(details(d));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        if (selected != null) {
            donorSelectCombo.setSelectedItem(selected);
        }
    }

    private String details(Donor d) {
        return "#" + d.getId() + " " + d.getFullName()
                + (d.getDonorType() == null
                        ? "" : " (" + d.getDonorType().getLabel() + ")");
    }

    private void refreshStats() {
        try {
            totalDonationsTile.setText(
                    String.valueOf(controller.countDonations()));
            donorsTile.setText(String.valueOf(controller.countDonors()));
            cashTile.setText("\u20B9" + controller.totalCashDonated());
            remainingTile.setText(String.valueOf(
                    controller.materialUnitsRemaining()));
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    // ── donor registration / donation recording ──────────────────────

    private void registerDonor() {
        ActionResult r = controller.registerDonor(
                donorNameField.getText(),
                donorContactField.getText(),
                donorEmailField.getText(),
                donorLocationField.getText(),
                (DonorType) donorTypeCombo.getSelectedItem());
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            donorNameField.setText("");
            donorContactField.setText("");
            donorEmailField.setText("");
            donorLocationField.setText("");
            donorTypeCombo.setSelectedIndex(0);
            refreshDonorSelect();
            refreshDonors("");
            refreshStats();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void recordDonation() {
        String donorText = String.valueOf(
                donorSelectCombo.getSelectedItem());
        if (donorText == null || donorText.startsWith("No donors")) {
            ViewUtil.error(this, "Register a donor first");
            return;
        }
        long donorId = Long.parseLong(
                donorText.substring(1, donorText.indexOf(' ')));
        ActionResult r = controller.recordDonation(donorId,
                (DonationType) donationTypeCombo.getSelectedItem(),
                amountField.getText(), materialNameField.getText(),
                quantityField.getText(), donationDescField.getText());
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            amountField.setText("");
            materialNameField.setText("");
            quantityField.setText("");
            donationDescField.setText("");
            refreshDonations();
            refreshStats();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    // ── donor profile dialog ─────────────────────────────────────────

    private void viewDonorProfile() {
        int row = donorTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a donor in the Donors tab first");
            return;
        }
        Long id = (Long) donorModel.getValueAt(row, 0);
        try {
            Donor donor = controller.getDonor(id);
            DonationService.DonorSummary summary =
                    controller.summarizeDonor(id);
            showDonorProfile(donor, summary);
        } catch (ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void showDonorProfile(Donor donor,
                                  DonationService.DonorSummary summary)
            throws DataAccessException {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Donor Profile - " + donor.getFullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(720, 560);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(BorderFactory.createTitledBorder(
                "Donor Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        int r = 0;
        r = addInfoRow(info, gbc, r, "Name:", donor.getFullName());
        r = addInfoRow(info, gbc, r, "Type:",
                donor.getDonorType() == null
                        ? "-" : donor.getDonorType().getLabel());
        r = addInfoRow(info, gbc, r, "Contact:",
                donor.getContactNumber() == null
                        ? "-" : donor.getContactNumber());
        r = addInfoRow(info, gbc, r, "Email:",
                donor.getEmail() == null ? "-" : donor.getEmail());
        r = addInfoRow(info, gbc, r, "Location:",
                donor.getLocation() == null ? "-" : donor.getLocation());
        r = addInfoRow(info, gbc, r, "Total donations:",
                String.valueOf(summary.donationCount()));
        r = addInfoRow(info, gbc, r, "Total cash:",
                "\u20B9" + summary.totalCash());
        r = addInfoRow(info, gbc, r, "Material units:",
                String.valueOf(summary.materialUnits()));
        content.add(info, BorderLayout.NORTH);

        DefaultTableModel model = ViewUtil.readOnlyModel(
                DonationController.donationTableHeaders());
        JTable tbl = new JTable(model);
        for (Donation d : controller.getDonationsForDonor(donor.getId())) {
            model.addRow(DonationController.toDonationRow(d));
        }
        content.add(new JScrollPane(tbl), BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(event -> dialog.dispose());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.add(closeBtn);
        content.add(foot, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private int addInfoRow(JPanel panel, GridBagConstraints gbc,
                           int row, String label, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        panel.add(l, gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(value), gbc);
        return row + 1;
    }

    // ── view / distribute / status / edit / delete donation ──────────

    private void viewDonation() {
        int row = donationTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a donation first");
            return;
        }
        Long id = (Long) donationModel.getValueAt(row, 0);
        try {
            Donation d = controller.getDonation(id);
            Donor donor = controller.getDonor(d.getDonorId());
            showDonationDetails(d, donor);
        } catch (ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void showDonationDetails(Donation d, Donor donor)
            throws DataAccessException {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Donation #" + d.getId(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(680, 540);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(BorderFactory.createTitledBorder(
                "Donation Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        int r = 0;
        r = addInfoRow(info, gbc, r, "Donor:",
                donor == null ? "-" : donor.getFullName());
        r = addInfoRow(info, gbc, r, "Type:",
                d.getDonationType() == null
                        ? "-" : d.getDonationType().getLabel());
        if (d.getDonationType() == DonationType.CASH) {
            r = addInfoRow(info, gbc, r, "Amount:",
                    "\u20B9" + (d.getAmount() == null
                            ? "0" : d.getAmount()));
        } else {
            r = addInfoRow(info, gbc, r, "Item:",
                    d.getMaterialName() == null ? "-" : d.getMaterialName());
            r = addInfoRow(info, gbc, r, "Quantity:",
                    String.valueOf(d.getQuantity()));
            r = addInfoRow(info, gbc, r, "Distributed so far:",
                    String.valueOf(controller.sumDistributed(d.getId())));
        }
        r = addInfoRow(info, gbc, r, "Status:",
                d.getStatus() == null ? "-" : d.getStatus().getLabel());
        r = addInfoRow(info, gbc, r, "Description:",
                d.getDescription() == null ? "-" : d.getDescription());
        content.add(info, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Distribution History", buildDistributionTab(d));
        content.add(tabs, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(event -> dialog.dispose());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (d.getDonationType() == DonationType.MATERIAL
                && d.getStatus() != DonationStatus.DISTRIBUTED) {
            JButton distBtn = new JButton("Record Distribution");
            distBtn.addActionListener(event -> {
                dialog.dispose();
                openDistributeDialogFor(d.getId(), d);
            });
            foot.add(distBtn);
        }
        foot.add(closeBtn);
        content.add(foot, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private JPanel buildDistributionTab(Donation d)
            throws DataAccessException {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        DefaultTableModel model = ViewUtil.readOnlyModel(
                DonationController.distributionTableHeaders());
        JTable tbl = new JTable(model);
        for (DonationDistribution dist :
                controller.getDistributions(d.getId())) {
            model.addRow(DonationController.toDistributionRow(dist));
        }
        panel.add(new JScrollPane(tbl), BorderLayout.CENTER);
        return panel;
    }

    private void openDistributeDialog() {
        int row = donationTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a donation first");
            return;
        }
        Long id = (Long) donationModel.getValueAt(row, 0);
        try {
            Donation d = controller.getDonation(id);
            if (d.getDonationType() != DonationType.MATERIAL) {
                ViewUtil.error(this, "Only material donations "
                        + "can be distributed");
                return;
            }
            openDistributeDialogFor(d.getId(), d);
        } catch (ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void openDistributeDialogFor(Long donationId, Donation d) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Distribute Donation #" + donationId,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 300);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int available = 0;
        try {
            int total = d.getQuantity() == null ? 0 : d.getQuantity();
            int distributed = controller.sumDistributed(d.getId());
            available = total - distributed;
        } catch (DataAccessException ignored) {
        }
        JTextField toField = new JTextField(20);
        JTextField qtyField = new JTextField(6);
        JTextField descField = new JTextField(18);

        int row = 0;
        row = addRow(form, gbc, row, "Available: " + available
                + (d.getMaterialName() == null
                        ? "" : " " + d.getMaterialName()), new JLabel(""));
        row = addRow(form, gbc, row, "Distribute to:", toField);
        row = addRow(form, gbc, row, "Quantity:", qtyField);
        row = addRow(form, gbc, row, "Description:", descField);

        JButton distBtn = new JButton("Distribute");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(distBtn, gbc);

        distBtn.addActionListener(event -> {
            ActionResult r = controller.recordDistribution(donationId,
                    toField.getText(), qtyField.getText(),
                    descField.getText());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
                refreshDonations();
                refreshStats();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });

        dialog.setContentPane(form);
        dialog.setVisible(true);
    }

    private void openStatusDialog() {
        int row = donationTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a donation first");
            return;
        }
        Long id = (Long) donationModel.getValueAt(row, 0);
        DonationStatus status = (DonationStatus) JOptionPane
                .showInputDialog(this, "Set donation status:",
                        "Update Status", JOptionPane.QUESTION_MESSAGE,
                        null, DonationStatus.values(),
                        DonationStatus.RECEIVED);
        if (status == null) {
            return;
        }
        ActionResult r = controller.updateDonationStatus(id, status);
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            refreshDonations();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void editDonation() {
        int row = donationTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a donation first");
            return;
        }
        Long id = (Long) donationModel.getValueAt(row, 0);
        try {
            Donation d = controller.getDonation(id);
            editDonationDialog(d);
        } catch (ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void editDonationDialog(final Donation d) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Edit Donation #" + d.getId(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(430, 360);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JComboBox<DonationType> typeIn =
                new JComboBox<>(DonationType.values());
        typeIn.setSelectedItem(d.getDonationType());
        JTextField amountIn = new JTextField(
                d.getAmount() == null ? "" : d.getAmount().toString(), 10);
        JTextField materialIn = new JTextField(
                d.getMaterialName(), 14);
        JTextField qtyIn = new JTextField(
                d.getQuantity() == null ? "" : String.valueOf(d.getQuantity()), 6);
        JTextField descIn = new JTextField(d.getDescription(), 18);

        int row = 0;
        row = addRow(form, gbc, row, "Type:", typeIn);
        row = addRow(form, gbc, row, "Amount (\u20B9):", amountIn);
        row = addRow(form, gbc, row, "Material / Item:", materialIn);
        row = addRow(form, gbc, row, "Quantity:", qtyIn);
        row = addRow(form, gbc, row, "Description:", descIn);

        JButton saveBtn = new JButton("Save Changes");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);

        saveBtn.addActionListener(event -> {
            ActionResult r = controller.updateDonation(d.getId(),
                    (DonationType) typeIn.getSelectedItem(),
                    amountIn.getText(), materialIn.getText(),
                    qtyIn.getText(), descIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
                refreshDonations();
                refreshStats();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.setVisible(true);
    }

    private void deleteDonation() {
        int row = donationTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a donation first");
            return;
        }
        Long id = (Long) donationModel.getValueAt(row, 0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Permanently delete donation #" + id + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult r = controller.deleteDonation(id);
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
        } else {
            ViewUtil.error(this, r.getMessage());
        }
        refreshDonations();
        refreshStats();
    }

    private void editDonor() {
        int row = donorTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a donor in the Donors tab first");
            return;
        }
        Long id = (Long) donorModel.getValueAt(row, 0);
        try {
            Donor donor = controller.getDonor(id);
            editDonorDialog(donor);
        } catch (ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void editDonorDialog(final Donor donor) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Edit Donor #" + donor.getId(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameIn = new JTextField(donor.getFullName(), 18);
        JTextField contactIn = new JTextField(donor.getContactNumber(), 12);
        JTextField emailIn = new JTextField(donor.getEmail(), 18);
        JTextField locationIn = new JTextField(donor.getLocation(), 18);
        JComboBox<DonorType> typeIn = new JComboBox<>(DonorType.values());
        typeIn.setSelectedItem(donor.getDonorType());

        int row = 0;
        row = addRow(form, gbc, row, "Full name:", nameIn);
        row = addRow(form, gbc, row, "Contact:", contactIn);
        row = addRow(form, gbc, row, "Email:", emailIn);
        row = addRow(form, gbc, row, "Location:", locationIn);
        row = addRow(form, gbc, row, "Type:", typeIn);

        JButton saveBtn = new JButton("Save Changes");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);

        saveBtn.addActionListener(event -> {
            ActionResult r = controller.updateDonor(donor.getId(),
                    nameIn.getText(), contactIn.getText(),
                    emailIn.getText(), locationIn.getText(),
                    (DonorType) typeIn.getSelectedItem());
            if (r.isSuccess()) {
                ViewUtil.info(dialog, r.getMessage());
                dialog.dispose();
                refreshDonorSelect();
                refreshDonors("");
                refreshStats();
            } else {
                ViewUtil.error(dialog, r.getMessage());
            }
        });
        dialog.setContentPane(form);
        dialog.setVisible(true);
    }

    private void deleteDonor() {
        int row = donorTable.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a donor in the Donors tab first");
            return;
        }
        Long id = (Long) donorModel.getValueAt(row, 0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete donor #" + id + "? (donors with donations "
                        + "cannot be deleted)",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult r = controller.deleteDonor(id);
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
        } else {
            ViewUtil.error(this, r.getMessage());
        }
        refreshDonorSelect();
        refreshDonors("");
        refreshStats();
    }

    @Override
    public void refreshData() {
        refreshDonations();
        refreshDonorSelect();
        refreshStats();
    }
}
