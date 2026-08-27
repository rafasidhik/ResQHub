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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.DisasterController;
import com.resqhub.controller.VictimController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Disaster;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.Gender;
import com.resqhub.model.RoleType;
import com.resqhub.model.ShelterStatus;
import com.resqhub.model.Victim;
import com.resqhub.service.SessionManager;

/**
 * Victim management screen with stat tiles, search/filter,
 * registration form, and full action bar.
 */
public class VictimPanel extends JPanel implements Refreshable {

    private final VictimController controller = new VictimController();
    private final DisasterController disasterController = new DisasterController();
    private final boolean operational;

    private final JTextField nameField = new JTextField(18);
    private final JTextField ageField = new JTextField(5);
    private final JRadioButton maleRadio = new JRadioButton("Male");
    private final JRadioButton femaleRadio = new JRadioButton("Female");
    private final JRadioButton otherRadio = new JRadioButton("Other");
    private final JTextField phoneField = new JTextField(12);
    private final JComboBox<EmergencyStatus> statusCombo =
            new JComboBox<>(EmergencyStatus.values());
    private final JTextArea medicalArea = new JTextArea(2, 18);
    private final JTextArea familyArea = new JTextArea(2, 18);
    private final JTextField locationField = new JTextField(18);
    private final JComboBox<DisasterOption> disasterCombo = new JComboBox<>();

    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> statusFilter =
            new JComboBox<>(new String[] {"All", "Safe",
                    "Needs Assistance", "Rescue Required",
                    "Injured", "Critical", "Missing"});
    private final JComboBox<String> shelterFilter =
            new JComboBox<>(new String[] {"All", "Not Sheltered",
                    "In Shelter", "Relocated"});

    private static final String[] HEADERS = {"ID", "Name", "Age", "Gender",
            "Status", "Shelter", "Location", "Disaster #"};
    private final javax.swing.table.DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(HEADERS);
    private final JTable table = new JTable(tableModel);

    private final JLabel totalTile = new JLabel("0");
    private final JLabel criticalTile = new JLabel("0");
    private final JLabel injuredTile = new JLabel("0");
    private final JLabel missingTile = new JLabel("0");
    private final JLabel rescueTile = new JLabel("0");

    private Long editingId = null;
    private final JButton saveChangesButton = new JButton("Save Changes");

    private record DisasterOption(Long id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public VictimPanel(boolean operational) {
        this.operational = operational;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
        add(buildActionBar(), BorderLayout.SOUTH);
        refreshDisasterCombo();
        refreshTable();
    }

    // ---------------------------------------------------------- header

    private JPanel buildHeader() {
        JLabel title = new JLabel("VICTIM MANAGEMENT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JButton refreshButton = new JButton("\u21bb Refresh");
        refreshButton.addActionListener(e -> refreshData());

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(refreshButton, BorderLayout.EAST);

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("TOTAL", totalTile, new Color(60, 60, 60)));
        tiles.add(statTile("CRITICAL", criticalTile, new Color(170, 40, 40)));
        tiles.add(statTile("RESCUE REQ", rescueTile, new Color(200, 100, 20)));
        tiles.add(statTile("INJURED", injuredTile, new Color(180, 130, 20)));
        tiles.add(statTile("MISSING", missingTile, new Color(120, 40, 120)));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(titleRow, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        return north;
    }

    private JPanel statTile(String caption, JLabel valueLabel, Color color) {
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 26f));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        captionLabel.setFont(captionLabel.getFont().deriveFont(11f));
        captionLabel.setForeground(new Color(90, 90, 90));

        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setPreferredSize(new Dimension(120, 84));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tile.add(Box.createVerticalGlue());
        tile.add(valueLabel);
        tile.add(captionLabel);
        tile.add(Box.createVerticalGlue());
        return tile;
    }

    // -------------------------------------------------- form | records split

    private javax.swing.JSplitPane buildSplit() {
        javax.swing.JSplitPane split = new javax.swing.JSplitPane(
                javax.swing.JSplitPane.HORIZONTAL_SPLIT,
                buildForm(), buildRecords());
        split.setDividerLocation(380);
        split.setResizeWeight(0.34);
        return split;
    }

    private JScrollPane buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("REGISTER / EDIT VICTIM"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Full name:"), gbc);
        gbc.gridx = 1; form.add(nameField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Age:"), gbc);
        gbc.gridx = 1; form.add(ageField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Gender:"), gbc);
        JPanel genderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        genderRow.add(maleRadio);
        genderRow.add(femaleRadio);
        genderRow.add(otherRadio);
        ButtonGroup genderGroup = new ButtonGroup();
        genderGroup.add(maleRadio);
        genderGroup.add(femaleRadio);
        genderGroup.add(otherRadio);
        gbc.gridx = 1; form.add(genderRow, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Phone (optional):"), gbc);
        gbc.gridx = 1; form.add(phoneField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Emergency status:"), gbc);
        gbc.gridx = 1; form.add(statusCombo, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Medical info:"), gbc);
        gbc.gridx = 1; form.add(new JScrollPane(medicalArea), gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Family info:"), gbc);
        gbc.gridx = 1; form.add(new JScrollPane(familyArea), gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Current location:"), gbc);
        gbc.gridx = 1; form.add(locationField, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Disaster:"), gbc);
        gbc.gridx = 1; form.add(disasterCombo, gbc);
        row++;

        JButton registerButton = new JButton("+ Register Victim");
        registerButton.setEnabled(operational);
        gbc.gridx = 1; gbc.gridy = row; form.add(registerButton, gbc);
        registerButton.addActionListener(e -> registerVictim());

        return new JScrollPane(form);
    }

    private JPanel buildRecords() {
        JPanel records = new JPanel(new BorderLayout(6, 6));
        records.setBorder(BorderFactory.createTitledBorder("VICTIM RECORDS"));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchField);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        searchRow.add(searchButton);
        searchRow.add(showAllButton);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterRow.add(new JLabel("Status:"));
        filterRow.add(statusFilter);
        filterRow.add(new JLabel("Shelter:"));
        filterRow.add(shelterFilter);
        JButton applyFilter = new JButton("Apply");
        filterRow.add(applyFilter);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.add(searchRow);
        northStack.add(filterRow);
        records.add(northStack, BorderLayout.NORTH);
        records.add(new JScrollPane(table), BorderLayout.CENTER);

        searchButton.addActionListener(e -> refreshTable());
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            shelterFilter.setSelectedIndex(0);
            refreshTable();
        });
        applyFilter.addActionListener(e -> refreshTable());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewDetails();
                }
            }
        });
        return records;
    }

    // ------------------------------------------------------- action bar

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("ACTIONS"),
                BorderFactory.createEmptyBorder(2, 6, 4, 6)));

        JButton editButton = new JButton("Edit Selected");
        saveChangesButton.setEnabled(false);
        JButton detailsButton = new JButton("View Details");
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));
        JButton exportButton = new JButton("Export CSV");

        bar.add(editButton);
        bar.add(saveChangesButton);
        bar.add(detailsButton);
        bar.add(deleteButton);
        bar.add(exportButton);

        editButton.addActionListener(e -> editSelected());
        saveChangesButton.addActionListener(e -> saveChanges());
        detailsButton.addActionListener(e -> viewDetails());
        deleteButton.addActionListener(e -> deleteSelected());
        exportButton.addActionListener(e ->
                ViewUtil.exportTableToCsv(this, table, "victims"));
        return bar;
    }

    // -------------------------------------------------------- handlers

    private void registerVictim() {
        DisasterOption selected =
                (DisasterOption) disasterCombo.getSelectedItem();
        Gender gender = maleRadio.isSelected() ? Gender.MALE
                : femaleRadio.isSelected() ? Gender.FEMALE
                : otherRadio.isSelected() ? Gender.OTHER : null;

        ActionResult result = controller.registerVictim(
                nameField.getText(),
                ageField.getText(),
                gender,
                phoneField.getText(),
                (EmergencyStatus) statusCombo.getSelectedItem(),
                medicalArea.getText(),
                familyArea.getText(),
                locationField.getText(),
                selected == null ? null : selected.id());
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            clearForm();
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a victim in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            for (Victim victim : controller.getAllVictims()) {
                if (victim.getId().equals(id)) {
                    loadVictimIntoForm(victim);
                    break;
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return;
        }
        editingId = id;
        saveChangesButton.setEnabled(true);
        ViewUtil.info(this, "Editing victim #" + id
                + " - modify the form and press Save Changes");
    }

    private void loadVictimIntoForm(Victim victim) {
        nameField.setText(victim.getFullName());
        ageField.setText(String.valueOf(victim.getAge()));
        Gender g = victim.getGender();
        maleRadio.setSelected(g == Gender.MALE);
        femaleRadio.setSelected(g == Gender.FEMALE);
        otherRadio.setSelected(g == Gender.OTHER);
        phoneField.setText(victim.getPhone() == null ? "" : victim.getPhone());
        statusCombo.setSelectedItem(victim.getEmergencyStatus());
        medicalArea.setText(victim.getMedicalCondition());
        familyArea.setText(victim.getFamilyInfo());
        locationField.setText(victim.getCurrentLocation());
        for (int i = 0; i < disasterCombo.getItemCount(); i++) {
            if (disasterCombo.getItemAt(i).id()
                    .equals(victim.getDisasterId())) {
                disasterCombo.setSelectedIndex(i);
            }
        }
    }

    private void saveChanges() {
        if (editingId == null) {
            return;
        }
        DisasterOption selected =
                (DisasterOption) disasterCombo.getSelectedItem();
        Gender gender = maleRadio.isSelected() ? Gender.MALE
                : femaleRadio.isSelected() ? Gender.FEMALE
                : otherRadio.isSelected() ? Gender.OTHER : null;

        ActionResult result = controller.updateVictim(editingId,
                nameField.getText(),
                ageField.getText(),
                gender,
                phoneField.getText(),
                (EmergencyStatus) statusCombo.getSelectedItem(),
                medicalArea.getText(),
                familyArea.getText(),
                locationField.getText(),
                selected == null ? null : selected.id());
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            editingId = null;
            saveChangesButton.setEnabled(false);
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    private void viewDetails() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a victim in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            for (Victim victim : controller.getAllVictims()) {
                if (victim.getId().equals(id)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("#").append(victim.getId()).append(" ")
                            .append(victim.getFullName()).append("\n")
                            .append("Age      : ").append(victim.getAge())
                            .append("\n")
                            .append("Gender   : ")
                            .append(victim.getGender() == null ? "-"
                                    : victim.getGender().getLabel())
                            .append("\n")
                            .append("Phone    : ")
                            .append(victim.getPhone() == null ? "-"
                                    : victim.getPhone())
                            .append("\n")
                            .append("Status   : ")
                            .append(victim.getEmergencyStatus().getLabel())
                            .append("\n")
                            .append("Shelter  : ")
                            .append(victim.getShelterStatus().getLabel())
                            .append("\n")
                            .append("Location : ")
                            .append(victim.getCurrentLocation())
                            .append("\n")
                            .append("Disaster #: ")
                            .append(victim.getDisasterId())
                            .append("\n\n")
                            .append("Medical  : ")
                            .append(victim.getMedicalCondition() == null
                                    || victim.getMedicalCondition().isEmpty()
                                    ? "(none)"
                                    : victim.getMedicalCondition())
                            .append("\n")
                            .append("Family   : ")
                            .append(victim.getFamilyInfo() == null
                                    || victim.getFamilyInfo().isEmpty()
                                    ? "(none)"
                                    : victim.getFamilyInfo());
                    JOptionPane.showMessageDialog(this, sb.toString(),
                            "Victim #" + id,
                            JOptionPane.INFORMATION_MESSAGE);
                    break;
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this, "Select a victim in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Permanently delete victim #" + id + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult result = controller.deleteVictim(id);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    private void clearForm() {
        nameField.setText("");
        ageField.setText("");
        phoneField.setText("");
        medicalArea.setText("");
        familyArea.setText("");
        locationField.setText("");
        maleRadio.setSelected(false);
        femaleRadio.setSelected(false);
        otherRadio.setSelected(false);
        editingId = null;
        saveChangesButton.setEnabled(false);
    }

    // --------------------------------------------------------- refresh

    @Override
    public void refreshData() {
        refreshDisasterCombo();
        refreshTable();
    }

    private void refreshDisasterCombo() {
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

    private void refreshTable() {
        tableModel.setRowCount(0);
        int total = 0, critical = 0, injured = 0, missing = 0, rescue = 0;
        String needle = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        Object statusF = statusFilter.getSelectedItem();
        Object shelterF = shelterFilter.getSelectedItem();
        try {
            for (Victim victim : controller.getAllVictims()) {
                total++;
                switch (victim.getEmergencyStatus()) {
                    case CRITICAL -> critical++;
                    case INJURED -> injured++;
                    case MISSING -> missing++;
                    case RESCUE_REQUIRED -> rescue++;
                    default -> { }
                }

                boolean matchesNeedle = needle.isEmpty()
                        || victim.getFullName().toLowerCase().contains(needle)
                        || (victim.getPhone() != null
                            && victim.getPhone().contains(needle))
                        || (victim.getCurrentLocation() != null
                            && victim.getCurrentLocation().toLowerCase()
                                .contains(needle));
                boolean matchesStatus = "All".equals(statusF)
                        || victim.getEmergencyStatus().getLabel()
                                .equalsIgnoreCase(String.valueOf(statusF));
                boolean matchesShelter = "All".equals(shelterF)
                        || victim.getShelterStatus().getLabel()
                                .equalsIgnoreCase(String.valueOf(shelterF));

                if (matchesNeedle && matchesStatus && matchesShelter) {
                    tableModel.addRow(VictimController.toRowWithShelter(
                            victim));
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        totalTile.setText(String.valueOf(total));
        criticalTile.setText(String.valueOf(critical));
        rescueTile.setText(String.valueOf(rescue));
        injuredTile.setText(String.valueOf(injured));
        missingTile.setText(String.valueOf(missing));
    }
}
