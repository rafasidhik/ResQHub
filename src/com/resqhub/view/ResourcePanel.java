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
import javax.swing.DefaultComboBoxModel;
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
import com.resqhub.controller.ResourceController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Disaster;
import com.resqhub.model.DistributionDestination;
import com.resqhub.model.Resource;
import com.resqhub.model.ResourceCategory;
import com.resqhub.model.ResourceStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.Shelter;
import com.resqhub.model.Victim;
import com.resqhub.service.ResourceService;
import com.resqhub.service.SessionManager;

/**
 * Resource &amp; Inventory screen: register resources, track current
 * stock, record stock-in / stock-out / distribution transactions,
 * monitor availability status and raise low-stock alerts.
 */
public class ResourcePanel extends JPanel implements Refreshable {

    private final ResourceController controller = new ResourceController();
    private final ResourceService service = new ResourceService();
    private final boolean write;

    // stat tiles
    private final JLabel totalTile = new JLabel("0");
    private final JLabel unitsTile = new JLabel("0");
    private final JLabel lowTile = new JLabel("0");
    private final JLabel distTile = new JLabel("0");

    // inventory form
    private final JTextField nameField = new JTextField(18);
    private final JTextField codeField = new JTextField(8);
    private final JComboBox<ResourceCategory> categoryCombo =
            new JComboBox<>(ResourceCategory.values());
    private final JTextField availableField = new JTextField(6);
    private final JTextField minLevelField = new JTextField(6);
    private final JTextField unitField = new JTextField(8);
    private final JTextField descField = new JTextField(24);
    private long editingResourceId = -1;

    private final JComboBox<String> searchField = new JComboBox<>();
    private final JComboBox<ResourceCategory> filterCategory =
            new JComboBox<>();
    private final JComboBox<ResourceStatus> filterStatus = new JComboBox<>();

    private final DefaultTableModel model =
            ViewUtil.readOnlyModel(ResourceController.resourceHeaders());
    private final JTable table = new JTable(model);

    // stock tab
    private final JComboBox<Resource> stockResource = new JComboBox<>();
    private final JTextField stockQty = new JTextField(6);
    private final JComboBox<String> sourceCombo = new JComboBox<>(
            new String[]{"Donation", "Government Supplies",
                    "Partner Organization", "Emergency Procurement",
                    "Other Relief Source"});
    private final JTextField stockReason = new JTextField(20);
    private final JComboBox<String> disasterIn = new JComboBox<>();

    private final JComboBox<Resource> outResource = new JComboBox<>();
    private final JTextField outQty = new JTextField(6);
    private final JTextField outDest = new JTextField(18);
    private final JTextField outReason = new JTextField(20);
    private final JComboBox<String> disasterOut = new JComboBox<>();

    // distribution tab
    private final JComboBox<Resource> distResource = new JComboBox<>();
    private final JTextField distQty = new JTextField(6);
    private final JComboBox<DistributionDestination> distDestCombo =
            new JComboBox<>(DistributionDestination.values());
    private final JTextField distTo = new JTextField(20);
    private final JComboBox<String> distDisaster = new JComboBox<>();
    private final JComboBox<Shelter> distShelter = new JComboBox<>();
    private final JComboBox<Victim> distVictim = new JComboBox<>();
    private final JTextField distReason = new JTextField(20);

    private final DefaultTableModel distModel =
            ViewUtil.readOnlyModel(ResourceController.distributionHeaders());
    private final JTable distTable = new JTable(distModel);

    // history tab
    private final DefaultTableModel histModel =
            ViewUtil.readOnlyModel(ResourceController.movementHeaders());
    private final JTable histTable = new JTable(histModel);

    public ResourcePanel() {
        write = SessionManager.getInstance().hasRole(RoleType.ADMIN,
                RoleType.RESCUE_OFFICER, RoleType.CAMP_MANAGER);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Inventory", buildInventoryTab());
        tabs.add("Stock In / Stock Out", buildStockTab());
        tabs.add("Distribute", buildDistributeTab());
        tabs.add("History", buildHistoryTab());
        add(tabs, BorderLayout.CENTER);

        loadReferences();
        refreshData();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("RESOURCE & INVENTORY MANAGEMENT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("RESOURCES", totalTile, new Color(60, 60, 60)));
        tiles.add(statTile("TOTAL UNITS", unitsTile, new Color(40, 60, 130)));
        tiles.add(statTile("LOW STOCK", lowTile, new Color(200, 130, 20)));
        tiles.add(statTile("DISTRIBUTED", distTile, new Color(150, 30, 30)));

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
        tile.setPreferredSize(new Dimension(120, 80));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(cap);
        tile.add(Box.createVerticalGlue());
        return tile;
    }

    // ── inventory tab ────────────────────────────────────────────────

    private JPanel buildInventoryTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Register / update a resource"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        row = addRow(form, gbc, row, "Name:", nameField);
        row = addRow(form, gbc, row, "Code:", codeField);
        row = addRow(form, gbc, row, "Category:", categoryCombo);
        row = addRow(form, gbc, row, "Available qty:", availableField);
        row = addRow(form, gbc, row, "Minimum level:", minLevelField);
        row = addRow(form, gbc, row, "Unit:", unitField);
        row = addRow(form, gbc, row, "Description:", descField);

        JButton register = new JButton("Register Resource");
        JButton load = new JButton("Load Selected");
        JButton saveUpdate = new JButton("Save Update");
        JButton clear = new JButton("Clear");
        JButton delete = new JButton("Delete");
        if (write) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            actions.add(register);
            actions.add(load);
            actions.add(saveUpdate);
            actions.add(clear);
            actions.add(delete);
            gbc.gridy = row;
            gbc.gridx = 1;
            form.add(actions, gbc);
        }
        register.addActionListener(e -> registerResource());
        load.addActionListener(e -> loadSelected());
        saveUpdate.addActionListener(e -> saveUpdate());
        clear.addActionListener(e -> clearForm());
        delete.addActionListener(e -> deleteResource());

        JPanel detail = new JPanel(new BorderLayout(6, 6));
        detail.add(form, BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filters.add(new JLabel("Search:"));
        searchField.setEditable(true);
        searchField.setPreferredSize(new Dimension(140, 24));
        filters.add(searchField);
        filters.add(new JLabel("Category:"));
        filters.add(filterCategory);
        filters.add(new JLabel("Status:"));
        filters.add(filterStatus);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshData());
        filters.add(refresh);
        JButton alerts = new JButton("Raise Low-Stock Alerts");
        alerts.setBackground(new Color(200, 130, 20));
        alerts.setForeground(Color.WHITE);
        alerts.addActionListener(e -> raiseAlerts());
        if (write) {
            filters.add(alerts);
        }
        JButton export = new JButton("Export CSV");
        export.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, table, "resources"));
        filters.add(export);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(detail);
        north.add(filters);
        detail.setBorder(null);

        JTabbedPane t = new JTabbedPane();
        JPanel tab = new JPanel(new BorderLayout(6, 6));
        tab.add(north, BorderLayout.NORTH);
        tab.add(new JScrollPane(table), BorderLayout.CENTER);
        return tab;
    }

    private void registerResource() {
        ActionResult r = controller.createResource(nameField.getText(),
                codeField.getText(),
                (ResourceCategory) categoryCombo.getSelectedItem(),
                availableField.getText(), minLevelField.getText(),
                unitField.getText(), descField.getText());
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
            ViewUtil.error(this, "Select a resource first");
            return;
        }
        long id = (Long) model.getValueAt(row, 0);
        try {
            Resource r = service.getResource(id);
            nameField.setText(r.getName());
            codeField.setText(r.getCode());
            categoryCombo.setSelectedItem(r.getCategory());
            availableField.setText(String.valueOf(r.getAvailableQuantity()));
            minLevelField.setText(String.valueOf(r.getMinimumLevel()));
            unitField.setText(r.getUnit() == null ? "" : r.getUnit());
            descField.setText(r.getDescription() == null ? ""
                    : r.getDescription());
            editingResourceId = id;
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void saveUpdate() {
        if (editingResourceId < 0) {
            ViewUtil.error(this, "Load a resource first");
            return;
        }
        ActionResult r = controller.updateResource(editingResourceId,
                nameField.getText(),
                (ResourceCategory) categoryCombo.getSelectedItem(),
                minLevelField.getText(), unitField.getText(),
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
        nameField.setText("");
        codeField.setText("");
        categoryCombo.setSelectedIndex(0);
        availableField.setText("");
        minLevelField.setText("");
        unitField.setText("");
        descField.setText("");
        editingResourceId = -1;
    }

    private void deleteResource() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a resource first");
            return;
        }
        long id = (Long) model.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this,
                "Delete resource #" + id + "?", "Confirm delete",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult r = controller.deleteResource(id);
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void raiseAlerts() {
        ActionResult r = controller.generateLowStockAlerts();
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    // ── stock tab ────────────────────────────────────────────────────

    private JPanel buildStockTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));

        JPanel forms = new JPanel();
        forms.setLayout(new BoxLayout(forms, BoxLayout.X_AXIS));

        JPanel inPanel = new JPanel(new GridBagLayout());
        inPanel.setBorder(BorderFactory.createTitledBorder("Stock In (receive)"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        int r = 0;
        r = addRow(inPanel, g, r, "Resource:", stockResource);
        r = addRow(inPanel, g, r, "Quantity:", stockQty);
        r = addRow(inPanel, g, r, "Source:", sourceCombo);
        r = addRow(inPanel, g, r, "Reason:", stockReason);
        r = addRow(inPanel, g, r, "Disaster (opt):", disasterIn);
        JButton inBtn = new JButton("Record Stock In");
        if (write) {
            g.gridy = r;
            g.gridx = 1;
            inPanel.add(inBtn, g);
        }
        inBtn.addActionListener(e -> doStockIn());

        JPanel outPanel = new JPanel(new GridBagLayout());
        outPanel.setBorder(BorderFactory.createTitledBorder("Stock Out (use)"));
        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(4, 4, 4, 4);
        g2.anchor = GridBagConstraints.WEST;
        int r2 = 0;
        r2 = addRow(outPanel, g2, r2, "Resource:", outResource);
        r2 = addRow(outPanel, g2, r2, "Quantity:", outQty);
        r2 = addRow(outPanel, g2, r2, "Used for:", outDest);
        r2 = addRow(outPanel, g2, r2, "Reason:", outReason);
        r2 = addRow(outPanel, g2, r2, "Disaster (opt):", disasterOut);
        JButton outBtn = new JButton("Record Stock Out");
        if (write) {
            g2.gridy = r2;
            g2.gridx = 1;
            outPanel.add(outBtn, g2);
        }
        outBtn.addActionListener(e -> doStockOut());

        forms.add(inPanel);
        forms.add(Box.createHorizontalStrut(8));
        forms.add(outPanel);
        tab.add(forms, BorderLayout.NORTH);
        return tab;
    }

    private void doStockIn() {
        Resource res = (Resource) stockResource.getSelectedItem();
        if (res == null) {
            ViewUtil.error(this, "Select a resource");
            return;
        }
        ActionResult r = controller.stockIn(res.getId(), stockQty.getText(),
                String.valueOf(sourceCombo.getSelectedItem()),
                stockReason.getText(), selectedDisasterId(disasterIn));
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            stockQty.setText("");
            stockReason.setText("");
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    private void doStockOut() {
        Resource res = (Resource) outResource.getSelectedItem();
        if (res == null) {
            ViewUtil.error(this, "Select a resource");
            return;
        }
        ActionResult r = controller.stockOut(res.getId(), outQty.getText(),
                outDest.getText(), outReason.getText(),
                selectedDisasterId(disasterOut));
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            outQty.setText("");
            outDest.setText("");
            outReason.setText("");
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    // ── distribution tab ─────────────────────────────────────────────

    private JPanel buildDistributeTab() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Distribute resources to a destination"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        row = addRow(form, gbc, row, "Resource:", distResource);
        row = addRow(form, gbc, row, "Quantity:", distQty);
        row = addRow(form, gbc, row, "Destination type:", distDestCombo);
        row = addRow(form, gbc, row, "Distributed to:", distTo);
        row = addRow(form, gbc, row, "Shelter (opt):", distShelter);
        row = addRow(form, gbc, row, "Victim (opt):", distVictim);
        row = addRow(form, gbc, row, "Disaster (opt):", distDisaster);
        row = addRow(form, gbc, row, "Reason:", distReason);
        JButton distBtn = new JButton("Approve & Distribute");
        distBtn.setBackground(new Color(40, 110, 40));
        distBtn.setForeground(Color.WHITE);
        if (write) {
            gbc.gridy = row;
            gbc.gridx = 1;
            form.add(distBtn, gbc);
        }
        distBtn.addActionListener(e -> doDistribute());
        distShelter.addActionListener(e -> shelterChanged());
        distVictim.addActionListener(e -> victimChanged());

        JPanel north = new JPanel(new BorderLayout(6, 6));
        north.add(form, BorderLayout.NORTH);
        JLabel hint = new JLabel("<html>Distribution validates available "
                + "quantity before reducing inventory - stock can never go "
                + "below zero. A distribution record and a movement history "
                + "entry are created for each approved request.</html>");
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
                ViewUtil.exportTableToCsv(this, distTable, "distributions"));
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.add(exportDist);
        distArea.add(foot, BorderLayout.SOUTH);
        tab.add(distArea, BorderLayout.CENTER);
        return tab;
    }

    private void shelterChanged() {
        Shelter sel = (Shelter) distShelter.getSelectedItem();
        if (sel != null && distTo.getText().trim().isEmpty()) {
            distTo.setText(sel.getName());
        }
    }

    private void victimChanged() {
        Victim sel = (Victim) distVictim.getSelectedItem();
        if (sel != null && distTo.getText().trim().isEmpty()) {
            distTo.setText(sel.getFullName());
        }
    }

    private void doDistribute() {
        Resource res = (Resource) distResource.getSelectedItem();
        if (res == null) {
            ViewUtil.error(this, "Select a resource");
            return;
        }
        Shelter shelter = (Shelter) distShelter.getSelectedItem();
        Victim victim = (Victim) distVictim.getSelectedItem();
        ActionResult r = controller.distribute(res.getId(), distQty.getText(),
                (DistributionDestination) distDestCombo.getSelectedItem(),
                distTo.getText(), selectedDisasterId(distDisaster),
                shelter == null ? null : String.valueOf(shelter.getId()),
                victim == null ? null : String.valueOf(victim.getId()),
                distReason.getText());
        if (r.isSuccess()) {
            ViewUtil.info(this, r.getMessage());
            distQty.setText("");
            distTo.setText("");
            distReason.setText("");
            refreshData();
        } else {
            ViewUtil.error(this, r.getMessage());
        }
    }

    // ── history tab ──────────────────────────────────────────────────

    private JPanel buildHistoryTab() {
        JPanel tab = new JPanel(new BorderLayout(6, 6));
        tab.setBorder(BorderFactory.createTitledBorder(
                "Stock movement history"));
        JLabel hint = new JLabel("<html>Every stock-in / stock-out / "
                + "distribution is recorded here with the previous and new "
                + "stock levels, source / destination and reason - giving a "
                + "complete, auditable inventory history.</html>");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(new Color(90, 90, 90));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshHistory());
        JButton export = new JButton("Export CSV");
        export.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, histTable, "stock_movements"));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        top.add(hint);
        top.add(refresh);
        top.add(export);
        tab.add(top, BorderLayout.NORTH);
        tab.add(new JScrollPane(histTable), BorderLayout.CENTER);
        return tab;
    }

    private void refreshHistory() {
        histModel.setRowCount(0);
        try {
            for (Object[] row : controller.allMovementRows()) {
                histModel.addRow(row);
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    // ── helpers / refresh ────────────────────────────────────────────

    private void refreshDistributions() {
        distModel.setRowCount(0);
        try {
            for (Object[] row : controller.allDistributionRows()) {
                distModel.addRow(row);
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshTiles() {
        try {
            totalTile.setText(String.valueOf(controller.countResources()));
            unitsTile.setText(String.valueOf(controller.totalUnits()));
            lowTile.setText(String.valueOf(controller.countLowStock()));
            distTile.setText(String.valueOf(controller.countDistributed()));
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void refreshInventory() {
        model.setRowCount(0);
        try {
            ResourceCategory cat = (ResourceCategory)
                    filterCategory.getSelectedItem();
            ResourceStatus status = (ResourceStatus)
                    filterStatus.getSelectedItem();
            String keyword = searchField.getSelectedItem() == null ? ""
                    : String.valueOf(searchField.getSelectedItem());
            List<Resource> rows = controller.filter(keyword, cat, status);
            for (Resource r : rows) {
                model.addRow(ResourceController.resourceRow(r));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void loadReferences() {
        // search history suggestions
        if (searchField.getItemCount() == 0) {
            searchField.addItem("");
        }
        filterCategory.removeAllItems();
        filterCategory.addItem(null);
        for (ResourceCategory c : ResourceCategory.values()) {
            filterCategory.addItem(c);
        }
        filterStatus.removeAllItems();
        filterStatus.addItem(null);
        for (ResourceStatus s : ResourceStatus.values()) {
            filterStatus.addItem(s);
        }

        try {
            fillResourceCombo(stockResource);
            fillResourceCombo(outResource);
            fillResourceCombo(distResource);

            disasterIn.removeAllItems();
            disasterIn.addItem("");
            disasterOut.removeAllItems();
            disasterOut.addItem("");
            distDisaster.removeAllItems();
            distDisaster.addItem("");
            for (Disaster d : controller.getDisasters()) {
                String label = d.getId() + " - " + d.getTitle();
                disasterIn.addItem(label);
                disasterOut.addItem(label);
                distDisaster.addItem(label);
            }

            distShelter.removeAllItems();
            distShelter.addItem(null);
            for (Shelter s : new com.resqhub.service.ShelterService()
                    .getAllShelters()) {
                distShelter.addItem(s);
            }

            distVictim.removeAllItems();
            distVictim.addItem(null);
            for (Victim v : new com.resqhub.service.VictimService()
                    .getAllVictims()) {
                distVictim.addItem(v);
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void fillResourceCombo(JComboBox<Resource> combo) {
        Resource selected = (Resource) combo.getSelectedItem();
        combo.removeAllItems();
        try {
            for (Resource r : controller.getAllResources()) {
                combo.addItem(r);
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return;
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
                    setText(r.getName() + " (" + r.getCode() + ") ["
                            + r.getAvailableQuantity() + " "
                            + (r.getUnit() == null ? "" : r.getUnit()) + "]");
                }
                return this;
            }
        });
        if (selected != null) {
            combo.setSelectedItem(selected);
        }
    }

    private String selectedDisasterId(JComboBox<String> combo) {
        Object sel = combo.getSelectedItem();
        if (sel == null || String.valueOf(sel).trim().isEmpty()) {
            return null;
        }
        String text = String.valueOf(sel);
        int idx = text.indexOf(" - ");
        return idx > 0 ? text.substring(0, idx).trim() : null;
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
        refreshInventory();
        refreshHistory();
        refreshDistributions();
        fillResourceCombo(stockResource);
        fillResourceCombo(outResource);
        fillResourceCombo(distResource);
    }
}
