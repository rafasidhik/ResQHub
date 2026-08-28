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
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import com.resqhub.controller.FoodDistributionController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.BeneficiaryType;
import com.resqhub.model.Disaster;
import com.resqhub.model.FoodDistributionRequest;
import com.resqhub.model.FoodRequestStatus;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.Resource;
import com.resqhub.model.RoleType;
import com.resqhub.model.Shelter;
import com.resqhub.model.Volunteer;
import com.resqhub.service.SessionManager;

/**
 * Food Distribution screen: create requests, calculate requirements,
 * approve / allocate food, assign volunteers, record distributions and
 * integrate with shelter occupancy.
 */
public class FoodDistributionPanel extends JPanel implements Refreshable {

    private final FoodDistributionController controller =
            new FoodDistributionController();
    private final boolean write;

    private final JLabel requestTile = new JLabel("0");
    private final JLabel pendingTile = new JLabel("0");
    private final JLabel servedTile = new JLabel("0");
    private final JLabel remainingTile = new JLabel("0");

    // request form
    private final JTextField codeField = new JTextField(10);
    private final JComboBox<Disaster> disasterCombo = new JComboBox<>();
    private final JTextField locationField = new JTextField(18);
    private final JComboBox<BeneficiaryType> beneficiaryCombo =
            new JComboBox<>(BeneficiaryType.values());
    private final JTextField beneficiariesField = new JTextField(6);
    private final JTextField requiredField = new JTextField(6);
    private final JTextField perPersonField = new JTextField(6);
    private final JComboBox<PriorityLevel> priorityCombo =
            new JComboBox<>(PriorityLevel.values());
    private final JTextField descField = new JTextField(24);
    private long editingRequestId = -1;

    private final JComboBox<String> searchField = new JComboBox<>();
    private final JComboBox<Disaster> filterDisaster = new JComboBox<>();
    private final JTextField filterLocation = new JTextField(10);
    private final JComboBox<FoodRequestStatus> filterStatus =
            new JComboBox<>();
    private final JComboBox<PriorityLevel> filterPriority = new JComboBox<>();

    private final DefaultTableModel model =
            ViewUtil.readOnlyModel(FoodDistributionController.requestHeaders());
    private final JTable table = new JTable(model);

    // allocate & assign tab
    private final JComboBox<FoodDistributionRequest> allocRequest =
            new JComboBox<>();
    private final JComboBox<Resource> allocResource = new JComboBox<>();
    private final JTextField allocQty = new JTextField(6);
    private final JComboBox<Volunteer> allocVolunteer = new JComboBox<>();

    // distribute tab
    private final JComboBox<FoodDistributionRequest> distRequest =
            new JComboBox<>();
    private final JComboBox<Resource> distResource = new JComboBox<>();
    private final JTextField distQty = new JTextField(6);
    private final JTextField distServed = new JTextField(6);
    private final JTextField distLocation = new JTextField(16);
    private final JTextField distNote = new JTextField(20);

    private final DefaultTableModel distModel =
            ViewUtil.readOnlyModel(FoodDistributionController
                    .distributionHeaders());
    private final JTable distTable = new JTable(distModel);

    // shelter tab
    private final JComboBox<Shelter> shelterCombo = new JComboBox<>();
    private final JTextField shelterMeals = new JTextField(4);
    private final JComboBox<PriorityLevel> shelterPriority =
            new JComboBox<>(PriorityLevel.values());

    public FoodDistributionPanel() {
        write = SessionManager.getInstance().hasRole(RoleType.ADMIN,
                RoleType.RESCUE_OFFICER, RoleType.CAMP_MANAGER);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Requests", buildRequestsTab());
        tabs.add("Allocate & Assign", buildAllocateTab());
        tabs.add("Distribute", buildDistributeTab());
        tabs.add("Shelter Integration", buildShelterTab());
        add(tabs, BorderLayout.CENTER);

        loadReferences();
        refreshData();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("FOOD DISTRIBUTION MANAGEMENT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("REQUESTS", requestTile, new Color(60, 60, 60)));
        tiles.add(statTile("PENDING", pendingTile, new Color(200, 130, 20)));
        tiles.add(statTile("PEOPLE SERVED", servedTile, new Color(40, 110, 40)));
        tiles.add(statTile("REMAINING", remainingTile, new Color(150, 30, 30)));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(title, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        return north;
    }

    private JPanel statTile(String caption, JLabel valueLabel, Color color) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 22f));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JLabel cap = new JLabel(caption);
        cap.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cap.setFont(cap.getFont().deriveFont(11f));
        cap.setForeground(new Color(90, 90, 90));
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setPreferredSize(new Dimension(130, 80));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(cap);
        tile.add(Box.createVerticalGlue());
        return tile;
    }

    // ── requests tab ──────────────────────────────────────────────────

    private JPanel buildRequestsTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Create / update a food distribution request"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        row = addRow(form, gbc, row, "Request code:", codeField);
        row = addRow(form, gbc, row, "Disaster (opt):", disasterCombo);
        row = addRow(form, gbc, row, "Location:", locationField);
        row = addRow(form, gbc, row, "Beneficiary type:", beneficiaryCombo);
        row = addRow(form, gbc, row, "No. of people:", beneficiariesField);
        row = addRow(form, gbc, row, "Required qty:", requiredField);
        row = addRow(form, gbc, row, "Per person (calc):", perPersonField);
        row = addRow(form, gbc, row, "Priority:", priorityCombo);
        row = addRow(form, gbc, row, "Description:", descField);

        JButton create = new JButton("Create Request");
        JButton createCalc = new JButton("Create from Calc");
        JButton load = new JButton("Load Selected");
        JButton update = new JButton("Save Update");
        JButton clear = new JButton("Clear");
        if (write) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            actions.add(create);
            actions.add(createCalc);
            actions.add(load);
            actions.add(update);
            actions.add(clear);
            gbc.gridy = row;
            gbc.gridx = 1;
            form.add(actions, gbc);
        }
        create.addActionListener(e -> createRequest(false));
        createCalc.addActionListener(e -> createRequest(true));
        load.addActionListener(e -> loadSelected());
        update.addActionListener(e -> updateRequest());
        clear.addActionListener(e -> clearForm());

        JPanel detail = new JPanel(new BorderLayout(6, 6));
        detail.add(form, BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filters.add(new JLabel("Search:"));
        searchField.setEditable(true);
        searchField.setPreferredSize(new Dimension(130, 24));
        filters.add(searchField);
        filters.add(new JLabel("Disaster:"));
        filters.add(filterDisaster);
        filters.add(new JLabel("Location:"));
        filters.add(filterLocation);
        filters.add(new JLabel("Status:"));
        filters.add(filterStatus);
        filters.add(new JLabel("Priority:"));
        filters.add(filterPriority);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshData());
        filters.add(refresh);
        JButton alerts = new JButton("Raise Food Shortage Alerts");
        alerts.setBackground(new Color(200, 130, 20));
        alerts.setForeground(Color.WHITE);
        alerts.addActionListener(e -> raiseShortageAlerts());
        if (write) {
            filters.add(alerts);
        }
        JButton export = new JButton("Export CSV");
        export.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, table, "food_requests"));
        filters.add(export);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(detail);
        north.add(filters);
        detail.setBorder(null);

        JPanel tab = new JPanel(new BorderLayout(6, 6));
        tab.add(north, BorderLayout.NORTH);
        tab.add(new JScrollPane(table), BorderLayout.CENTER);
        return tab;
    }

    private void createRequest(boolean useCalc) {
        Long disasterId = selectedDisasterId(disasterCombo);
        ActionResult r;
        if (useCalc) {
            r = controller.createRequestWithCalculation(codeField.getText(),
                    disasterId == null ? null : String.valueOf(disasterId),
                    locationField.getText(),
                    (BeneficiaryType) beneficiaryCombo.getSelectedItem(),
                    beneficiariesField.getText(), perPersonField.getText(),
                    (PriorityLevel) priorityCombo.getSelectedItem(),
                    descField.getText());
        } else {
            r = controller.createRequest(codeField.getText(),
                    disasterId == null ? null : String.valueOf(disasterId),
                    locationField.getText(),
                    (BeneficiaryType) beneficiaryCombo.getSelectedItem(),
                    beneficiariesField.getText(), requiredField.getText(),
                    (PriorityLevel) priorityCombo.getSelectedItem(),
                    descField.getText());
        }
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            clearForm();
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a request first");
            return;
        }
        long id = (Long) model.getValueAt(row, 0);
        try {
            FoodDistributionRequest r = controller.getRequest(id);
            codeField.setText(r.getRequestCode());
            locationField.setText(r.getLocation());
            beneficiaryCombo.setSelectedItem(r.getBeneficiaryType());
            beneficiariesField.setText(String.valueOf(r.getBeneficiaries()));
            requiredField.setText(String.valueOf(r.getRequiredQuantity()));
            priorityCombo.setSelectedItem(r.getPriority());
            descField.setText(r.getDescription() == null ? ""
                    : r.getDescription());
            editingRequestId = id;
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void updateRequest() {
        if (editingRequestId < 0) {
            ViewUtil.error(this, "Load a request first");
            return;
        }
        ActionResult r = controller.updateRequest(editingRequestId,
                locationField.getText(),
                (BeneficiaryType) beneficiaryCombo.getSelectedItem(),
                beneficiariesField.getText(), requiredField.getText(),
                (PriorityLevel) priorityCombo.getSelectedItem(),
                descField.getText());
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            clearForm();
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void clearForm() {
        codeField.setText("");
        locationField.setText("");
        beneficiaryCombo.setSelectedIndex(0);
        beneficiariesField.setText("");
        requiredField.setText("");
        perPersonField.setText("");
        descField.setText("");
        editingRequestId = -1;
    }

    private void raiseShortageAlerts() {
        ActionResult r = controller.generateFoodShortageAlerts();
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    // ── allocate & assign tab ─────────────────────────────────────────

    private JPanel buildAllocateTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Approve / allocate food & assign a volunteer"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        row = addRow(form, gbc, row, "Request:", allocRequest);
        row = addRow(form, gbc, row, "Food resource:", allocResource);
        row = addRow(form, gbc, row, "Allocate quantity:", allocQty);
        row = addRow(form, gbc, row, "Assign volunteer:", allocVolunteer);

        JButton approve = new JButton("Approve Request");
        JButton allocate = new JButton("Allocate Food");
        JButton assign = new JButton("Assign Volunteer");
        if (write) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            actions.add(approve);
            actions.add(allocate);
            actions.add(assign);
            gbc.gridy = row;
            gbc.gridx = 1;
            form.add(actions, gbc);
        }
        approve.addActionListener(e -> approveRequest());
        allocate.addActionListener(e -> allocateFood());
        assign.addActionListener(e -> assignVolunteer());

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(form, BorderLayout.NORTH);
        JLabel hint = new JLabel("<html>Allocation validates food inventory "
                + "before reserving it (insufficient stock is rejected). "
                + "The distribution itself reduces inventory. Requests can "
                + "also be modified through the table actions on the "
                + "Requests tab.</html>");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(new Color(90, 90, 90));
        north.add(hint, BorderLayout.CENTER);

        JPanel tab = new JPanel(new BorderLayout(6, 6));
        tab.add(north, BorderLayout.NORTH);
        JLabel note = new JLabel("Open requests (highest priority first):");
        note.setFont(note.getFont().deriveFont(11f));
        tab.add(note, BorderLayout.CENTER);
        return tab;
    }

    private void approveRequest() {
        FoodDistributionRequest req =
                (FoodDistributionRequest) allocRequest.getSelectedItem();
        if (req == null) {
            ViewUtil.error(this, "Select a request");
            return;
        }
        ActionResult r = controller.approveRequest(req.getId());
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void allocateFood() {
        FoodDistributionRequest req =
                (FoodDistributionRequest) allocRequest.getSelectedItem();
        Resource res = (Resource) allocResource.getSelectedItem();
        if (req == null || res == null) {
            ViewUtil.error(this, "Select a request and a food resource");
            return;
        }
        ActionResult r = controller.allocateRequest(req.getId(),
                String.valueOf(res.getId()), allocQty.getText());
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            allocQty.setText("");
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void assignVolunteer() {
        FoodDistributionRequest req =
                (FoodDistributionRequest) allocRequest.getSelectedItem();
        Volunteer v = (Volunteer) allocVolunteer.getSelectedItem();
        if (req == null || v == null) {
            ViewUtil.error(this, "Select a request and a volunteer");
            return;
        }
        ActionResult r = controller.assignVolunteer(req.getId(),
                String.valueOf(v.getId()));
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    // ── distribute tab ────────────────────────────────────────────────

    private JPanel buildDistributeTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Record an actual food distribution"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        row = addRow(form, gbc, row, "Request:", distRequest);
        row = addRow(form, gbc, row, "Food resource:", distResource);
        row = addRow(form, gbc, row, "Quantity:", distQty);
        row = addRow(form, gbc, row, "People served:", distServed);
        row = addRow(form, gbc, row, "Location:", distLocation);
        row = addRow(form, gbc, row, "Note:", distNote);
        JButton distBtn = new JButton("Record Distribution");
        distBtn.setBackground(new Color(40, 110, 40));
        distBtn.setForeground(Color.WHITE);
        if (write) {
            gbc.gridy = row;
            gbc.gridx = 1;
            form.add(distBtn, gbc);
        }
        distBtn.addActionListener(e -> doDistribute());

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(form, BorderLayout.NORTH);
        JLabel hint = new JLabel("<html>Distribution validates the quantity "
                + "against the remaining requirement and the allocation, "
                + "then reduces the food inventory. Once the full requirement "
                + "is handed out the request is Completed. History is kept "
                + "for reporting.</html>");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(new Color(90, 90, 90));
        north.add(hint, BorderLayout.CENTER);

        JPanel tab = new JPanel(new BorderLayout(6, 6));
        tab.add(north, BorderLayout.NORTH);
        JPanel distArea = new JPanel(new BorderLayout(6, 6));
        distArea.setBorder(BorderFactory.createTitledBorder(
                "Distribution history"));
        distArea.add(new JScrollPane(distTable), BorderLayout.CENTER);
        JButton exportDist = new JButton("Export CSV");
        exportDist.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, distTable,
                        "food_distributions"));
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.add(exportDist);
        distArea.add(foot, BorderLayout.SOUTH);
        tab.add(distArea, BorderLayout.CENTER);
        return tab;
    }

    private void doDistribute() {
        FoodDistributionRequest req =
                (FoodDistributionRequest) distRequest.getSelectedItem();
        Resource res = (Resource) distResource.getSelectedItem();
        if (req == null || res == null) {
            ViewUtil.error(this, "Select a request and a food resource");
            return;
        }
        ActionResult r = controller.recordDistribution(req.getId(),
                String.valueOf(res.getId()), distQty.getText(),
                distServed.getText(), distLocation.getText(),
                distNote.getText());
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            distQty.setText("");
            distServed.setText("");
            distLocation.setText("");
            distNote.setText("");
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    // ── shelter integration tab ───────────────────────────────────────

    private JPanel buildShelterTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Derive a food request from shelter occupancy"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        row = addRow(form, gbc, row, "Shelter:", shelterCombo);
        row = addRow(form, gbc, row, "Meals per person:", shelterMeals);
        row = addRow(form, gbc, row, "Priority:", shelterPriority);
        JButton create = new JButton("Create Request from Shelter");
        JButton update = new JButton("Update Open Request from Shelter");
        if (write) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            actions.add(create);
            actions.add(update);
            gbc.gridy = row;
            gbc.gridx = 1;
            form.add(actions, gbc);
        }
        create.addActionListener(e -> createFromShelter(false));
        update.addActionListener(e -> createFromShelter(true));

        JLabel calc = new JLabel("Estimated requirement: (select a shelter to "
                + "see its computed food need)");
        calc.setFont(calc.getFont().deriveFont(11f));
        calc.setForeground(new Color(90, 90, 90));

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(form, BorderLayout.NORTH);
        north.add(calc, BorderLayout.SOUTH);
        shelterCombo.addActionListener(e -> {
            Shelter s = (Shelter) shelterCombo.getSelectedItem();
            if (s != null) {
                try {
                    int meals = shelterMeals.getText().trim().isEmpty()
                            ? 1 : Integer.parseInt(shelterMeals.getText()
                                    .trim());
                    int req = controller.requirementForShelter(s.getId(),
                            meals);
                    calc.setText("Estimated requirement: " + s.getName()
                            + " (" + s.getCurrentOccupancy()
                            + " occupants) x " + meals + " = " + req);
                } catch (Exception ignored) {
                    // read preview only
                }
            }
        });

        JPanel tab = new JPanel(new BorderLayout(6, 6));
        tab.add(north, BorderLayout.NORTH);
        JLabel hint = new JLabel("<html>Shelter occupancy drives the food "
                + "requirement (occupants x meals per person). 'Create' makes "
                + "a new request; 'Update' re-syncs any still-open request for "
                + "that shelter location as occupancy grows.</html>");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(new Color(90, 90, 90));
        tab.add(hint, BorderLayout.CENTER);
        return tab;
    }

    private void createFromShelter(boolean updateMode) {
        Shelter s = (Shelter) shelterCombo.getSelectedItem();
        if (s == null) {
            ViewUtil.error(this, "Select a shelter");
            return;
        }
        if (updateMode) {
            try {
                boolean updated = new com.resqhub.service
                        .FoodDistributionService()
                        .updateRequestForShelter(s.getId(),
                        Integer.parseInt(shelterMeals.getText().trim()));
                ViewUtil.info(this, updated
                        ? "Open request for '" + s.getName()
                                + "' updated from its new occupancy"
                        : "No open request found to update for '"
                                + s.getName() + "'");
                refreshData();
            } catch (Exception e) {
                ViewUtil.error(this, e.getMessage());
            }
            return;
        }
        ActionResult r = controller.createRequestFromShelter(s.getId(),
                shelterMeals.getText(),
                (PriorityLevel) shelterPriority.getSelectedItem());
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    // ── helpers / refresh ─────────────────────────────────────────────

    private void refreshRequestsTable() {
        model.setRowCount(0);
        try {
            Map<Long, String> disasters = controller.disasterNameMap();
            Long disasterId = selectedDisasterId(filterDisaster);
            List<FoodDistributionRequest> rows = controller.filter(
                    searchField.getSelectedItem() == null ? ""
                            : String.valueOf(searchField.getSelectedItem()),
                    disasterId == null ? null : String.valueOf(disasterId),
                    filterLocation.getText(),
                    (FoodRequestStatus) filterStatus.getSelectedItem(),
                    (PriorityLevel) filterPriority.getSelectedItem());
            for (FoodDistributionRequest r : rows) {
                model.addRow(FoodDistributionController.requestRow(r,
                        disasters));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshTiles() {
        try {
            requestTile.setText(String.valueOf(controller.countRequests()));
            pendingTile.setText(String.valueOf(controller.countPending()));
            servedTile.setText(String.valueOf(
                    controller.totalBeneficiariesServed()));
            remainingTile.setText(String.valueOf(controller.totalRemaining()));
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshDistributions() {
        distModel.setRowCount(0);
        for (Object[] row : controller.allDistributionRows()) {
            distModel.addRow(row);
        }
    }

    private void loadReferences() {
        if (searchField.getItemCount() == 0) {
            searchField.addItem("");
        }
        filterStatus.removeAllItems();
        filterStatus.addItem(null);
        for (FoodRequestStatus s : FoodRequestStatus.values()) {
            filterStatus.addItem(s);
        }
        filterPriority.removeAllItems();
        filterPriority.addItem(null);
        for (PriorityLevel p : PriorityLevel.values()) {
            filterPriority.addItem(p);
        }
        try {
            fillDisasters(disasterCombo);
            fillDisasters(filterDisaster);
            fillRequests();
            fillFoodResources(allocResource);
            fillFoodResources(distResource);
            fillVolunteers();
            fillShelters();
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void fillDisasters(JComboBox<Disaster> combo)
            throws DataAccessException {
        Disaster selected = (Disaster) combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(null);
        for (Disaster d : controller.getDisasters()) {
            combo.addItem(d);
        }
        combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index,
                        selected, focus);
                if (value == null) {
                    setText("");
                } else if (value instanceof Disaster) {
                    setText(((Disaster) value).getId() + " - "
                            + ((Disaster) value).getTitle());
                }
                return this;
            }
        });
    }

    private void fillRequests() {
        try {
            List<FoodDistributionRequest> open = controller.findOpen();
            Object selAlloc = allocRequest.getSelectedItem();
            Object selDist = distRequest.getSelectedItem();
            allocRequest.removeAllItems();
            distRequest.removeAllItems();
            for (FoodDistributionRequest r : open) {
                allocRequest.addItem(r);
                distRequest.addItem(r);
            }
            allocRequest.setRenderer(requestRenderer());
            distRequest.setRenderer(requestRenderer());
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private javax.swing.ListCellRenderer<FoodDistributionRequest>
            requestRenderer() {
        return (javax.swing.ListCellRenderer<FoodDistributionRequest>)
                (javax.swing.ListCellRenderer<?>)
                new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index,
                        selected, focus);
                if (value instanceof FoodDistributionRequest) {
                    FoodDistributionRequest r =
                            (FoodDistributionRequest) value;
                    setText(r.getRequestCode() + " - " + r.getLocation()
                            + " (" + r.remainingQuantity() + "/"
                            + r.getRequiredQuantity() + " remaining)");
                }
                return this;
            }
        };
    }

    private void fillFoodResources(JComboBox<Resource> combo)
            throws DataAccessException {
        Resource selected = (Resource) combo.getSelectedItem();
        combo.removeAllItems();
        for (Resource r : controller.getFoodResources()) {
            combo.addItem(r);
        }
        combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index,
                        selected, focus);
                if (value instanceof Resource) {
                    Resource r = (Resource) value;
                    setText(r.getName() + " [" + r.getAvailableQuantity()
                            + (r.getUnit() == null ? "" : " " + r.getUnit())
                            + "]");
                }
                return this;
            }
        });
    }

    private void fillVolunteers() throws DataAccessException {
        List<Volunteer> volunteers = controller.getAllVolunteers();
        allocVolunteer.removeAllItems();
        for (Volunteer v : volunteers) {
            allocVolunteer.addItem(v);
        }
        allocVolunteer.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index,
                        selected, focus);
                if (value instanceof Volunteer) {
                    setText(((Volunteer) value).getFullName() + " @ "
                            + ((Volunteer) value).getLocation());
                }
                return this;
            }
        });
    }

    private void fillShelters() throws DataAccessException {
        List<Shelter> shelters = controller.getAllShelters();
        shelterCombo.removeAllItems();
        for (Shelter s : shelters) {
            shelterCombo.addItem(s);
        }
        shelterCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index,
                        selected, focus);
                if (value instanceof Shelter) {
                    Shelter s = (Shelter) value;
                    setText(s.getName() + " (" + s.getCurrentOccupancy()
                            + " occupants)");
                }
                return this;
            }
        });
    }

    private Long selectedDisasterId(JComboBox<Disaster> combo) {
        Object sel = combo.getSelectedItem();
        if (sel instanceof Disaster) {
            return ((Disaster) sel).getId();
        }
        return null;
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

    @Override
    public void refreshData() {
        refreshTiles();
        refreshRequestsTable();
        refreshDistributions();
        fillRequests();
    }
}
