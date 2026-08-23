package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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
import com.resqhub.model.Victim;

/** Victim management screen. JRadioButtons + ButtonGroup for gender. */
public class VictimPanel extends JPanel {

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
    private final JComboBox<DisasterOption> disasterCombo =
            new JComboBox<>();

    private final javax.swing.table.DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(VictimController.tableHeaders());
    private final JTable table = new JTable(tableModel);

    /** Simple combo item pairing a disaster id with its display text. */
    private record DisasterOption(Long id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public VictimPanel(boolean operational) {
        this.operational = operational;
        setLayout(new BorderLayout(10, 10));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildForm(), BorderLayout.WEST);
        add(buildTableArea(), BorderLayout.CENTER);
        refreshDisasterCombo();
        refreshTable();
    }

    private JPanel buildForm() {
        ButtonGroup genderGroup = new ButtonGroup();
        genderGroup.add(maleRadio);
        genderGroup.add(femaleRadio);
        genderGroup.add(otherRadio);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createTitledBorder("Register victim"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Full name:"), gbc);
        gbc.gridx = 1; form.add(nameField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Age:"), gbc);
        gbc.gridx = 1; form.add(ageField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Gender:"), gbc);
        JPanel genderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        genderRow.add(maleRadio);
        genderRow.add(femaleRadio);
        genderRow.add(otherRadio);
        gbc.gridx = 1; form.add(genderRow, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Phone (optional):"), gbc);
        gbc.gridx = 1; form.add(phoneField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Emergency status:"), gbc);
        gbc.gridx = 1; form.add(statusCombo, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Medical info:"), gbc);
        gbc.gridx = 1; form.add(new JScrollPane(medicalArea), gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Family info:"), gbc);
        gbc.gridx = 1; form.add(new JScrollPane(familyArea), gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Current location:"), gbc);
        gbc.gridx = 1; form.add(locationField, gbc);

        row++; gbc.gridx = 0; gbc.gridy = row; form.add(new JLabel("Disaster:"), gbc);
        gbc.gridx = 1; form.add(disasterCombo, gbc);

        JButton registerButton = new JButton("Register victim");
        registerButton.setEnabled(operational);
        row++; gbc.gridx = 1; gbc.gridy = row; form.add(registerButton, gbc);
        registerButton.addActionListener(event -> registerVictim());

        return form;
    }

    private JPanel buildTableArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Set selected victim's emergency status:"));
        JComboBox<EmergencyStatus> quickStatus =
                new JComboBox<>(EmergencyStatus.values());
        JButton applyButton = new JButton("Apply");
        applyButton.setEnabled(operational);
        controls.add(quickStatus);
        controls.add(applyButton);
        area.add(controls, BorderLayout.NORTH);

        applyButton.addActionListener(event -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                ViewUtil.error(this, "Select a victim in the table first");
                return;
            }
            Long id = (Long) tableModel.getValueAt(viewRow, 0);
            ActionResult result = controller.updateEmergencyStatus(id,
                    (EmergencyStatus) quickStatus.getSelectedItem());
            if (result.isSuccess()) {
                ViewUtil.info(this, result.getMessage());
            } else {
                ViewUtil.error(this, result.getMessage());
            }
            refreshTable();
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    ViewUtil.info(VictimPanel.this,
                            "Victim details:\n"
                            + selectedVictimDescription());
                }
            }
        });
        return area;
    }

    private String selectedVictimDescription() {
        int viewRow = table.getSelectedRow();
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            for (Victim victim : controller.getAllVictims()) {
                if (victim.getId().equals(id)) {
                    return victim.toString() + "\nMedical: "
                            + victim.getMedicalCondition()
                            + "\nFamily: " + victim.getFamilyInfo();
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        return "Unknown";
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

    private void registerVictim() {
        DisasterOption selected = (DisasterOption) disasterCombo.getSelectedItem();
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

    private void clearForm() {
        nameField.setText("");
        ageField.setText("");
        phoneField.setText("");
        medicalArea.setText("");
        familyArea.setText("");
        locationField.setText("");
        genderGroupClear();
    }

    private void genderGroupClear() {
        maleRadio.setSelected(false);
        femaleRadio.setSelected(false);
        otherRadio.setSelected(false);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        try {
            List<Victim> victims = controller.getAllVictims();
            for (Victim victim : victims) {
                tableModel.addRow(VictimController.toRow(victim));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }
}
