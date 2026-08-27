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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.RescueTeamController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.RescueAssignment;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RoleType;
import com.resqhub.model.TeamEquipment;
import com.resqhub.model.TeamMember;
import com.resqhub.model.TeamOperationalStatus;
import com.resqhub.model.TeamSkill;
import com.resqhub.model.TeamType;
import com.resqhub.service.SessionManager;

/**
 * Rescue team management screen: registration, search/filter,
 * team profile dialog with members, skills, equipment tabs.
 */
public class RescueTeamPanel extends JPanel implements Refreshable {

    private final RescueTeamController controller =
            new RescueTeamController();

    // ── registration form ────────────────────────────────────────────
    private final JTextField nameField = new JTextField(16);
    private final JComboBox<TeamType> typeCombo =
            new JComboBox<>(TeamType.values());
    private final JTextField leaderField = new JTextField(16);
    private final JTextField contactField = new JTextField(12);
    private final JTextField memberCountField = new JTextField(5);
    private final JTextField skillsField = new JTextField(16);
    private final JTextField equipmentField = new JTextField(16);
    private final JTextField baseField = new JTextField(16);

    // ── table ────────────────────────────────────────────────────────
    private final DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(RescueTeamController.tableHeaders());
    private final JTable table = new JTable(tableModel);

    // ── controls ─────────────────────────────────────────────────────
    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> filterCombo = new JComboBox<>(
            new String[]{"All", "Available", "Deployed", "Unavailable",
                    "Off Duty"});
    private final JComboBox<String> opFilterCombo = new JComboBox<>(
            new String[]{"All", "Standby", "Assigned", "En Route",
                    "On Mission", "Returning", "Completed", "Inactive"});
    private Long editingId = null;
    private final JButton saveChangesButton =
            new JButton("Save changes");

    // ── stat tiles ───────────────────────────────────────────────────
    private final JLabel totalTile = new JLabel("0");
    private final JLabel availableTile = new JLabel("0");
    private final JLabel deployedTile = new JLabel("0");
    private final JLabel offDutyTile = new JLabel("0");

    public RescueTeamPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.WEST);
        add(buildTableArea(), BorderLayout.CENTER);
        refreshTable();
    }

    // ── header with stat tiles ───────────────────────────────────────

    private JPanel buildHeader() {
        JLabel title = new JLabel("RESCUE TEAMS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("TOTAL", totalTile, new Color(60, 60, 60)));
        tiles.add(statTile("AVAILABLE", availableTile,
                new Color(40, 110, 40)));
        tiles.add(statTile("DEPLOYED", deployedTile,
                new Color(40, 100, 160)));
        tiles.add(statTile("OFF DUTY", offDutyTile,
                new Color(120, 120, 120)));

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

    // ── registration form ────────────────────────────────────────────

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Register rescue team"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addRow(form, gbc, row, "Team name:", nameField);
        row = addRow(form, gbc, row, "Type:", typeCombo);
        row = addRow(form, gbc, row, "Leader:", leaderField);
        row = addRow(form, gbc, row, "Contact (10 digits):",
                contactField);
        row = addRow(form, gbc, row, "Members:", memberCountField);
        row = addRow(form, gbc, row, "Skills:", skillsField);
        row = addRow(form, gbc, row, "Equipment:", equipmentField);
        row = addRow(form, gbc, row, "Base location:", baseField);

        JButton registerButton = new JButton("Register team");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(registerButton, gbc);
        registerButton.addActionListener(event -> registerTeam());
        return form;
    }

    private int addRow(JPanel form, GridBagConstraints gbc, int row,
                       String label,
                       javax.swing.JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        form.add(field, gbc);
        return row + 1;
    }

    // ── table area ───────────────────────────────────────────────────

    private JPanel buildTableArea() {
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        controls.add(new JLabel("Search:"));
        controls.add(searchField);
        JButton searchBtn = new JButton("Search");
        controls.add(searchBtn);
        JButton showAllBtn = new JButton("Show All");
        controls.add(showAllBtn);

        controls.add(Box.createHorizontalStrut(10));
        controls.add(new JLabel("Availability:"));
        controls.add(filterCombo);

        controls.add(new JLabel("Operational:"));
        controls.add(opFilterCombo);

        controls.add(Box.createHorizontalStrut(10));

        JButton viewProfileBtn = new JButton("View Profile");
        controls.add(viewProfileBtn);

        JButton refreshBtn = new JButton("Refresh");
        controls.add(refreshBtn);

        JButton deleteBtn = new JButton("Delete selected");
        deleteBtn.setEnabled(
                SessionManager.getInstance().hasRole(RoleType.ADMIN));
        controls.add(deleteBtn);

        JButton editBtn = new JButton("Edit selected");
        saveChangesButton.setEnabled(false);
        controls.add(editBtn);
        controls.add(saveChangesButton);

        JButton exportBtn = new JButton("Export CSV");
        controls.add(exportBtn);

        area.add(controls, BorderLayout.NORTH);

        searchBtn.addActionListener(event -> refreshTable());
        showAllBtn.addActionListener(event -> {
            searchField.setText("");
            filterCombo.setSelectedIndex(0);
            opFilterCombo.setSelectedIndex(0);
            refreshTable();
        });
        filterCombo.addActionListener(event -> refreshTable());
        opFilterCombo.addActionListener(event -> refreshTable());
        viewProfileBtn.addActionListener(event -> viewProfile());
        refreshBtn.addActionListener(event -> refreshTable());
        deleteBtn.addActionListener(event -> deleteSelected());
        editBtn.addActionListener(event -> editSelected());
        saveChangesButton.addActionListener(event -> saveChanges());
        exportBtn.addActionListener(event ->
                ViewUtil.exportTableToCsv(this, table,
                        "rescue_teams"));

        return area;
    }

    // ── table refresh ────────────────────────────────────────────────

    private void refreshTable() {
        tableModel.setRowCount(0);
        int total = 0, available = 0, deployed = 0, offDuty = 0;
        try {
            String needle = searchField.getText() == null
                    ? "" : searchField.getText().trim();
            String availFilter = String.valueOf(
                    filterCombo.getSelectedItem());
            String opFilter = String.valueOf(
                    opFilterCombo.getSelectedItem());

            List<RescueTeam> teams;
            if (!needle.isEmpty()) {
                teams = controller.searchTeams(needle);
            } else {
                teams = controller.getAllTeams();
            }

            for (RescueTeam team : teams) {
                if (!"All".equals(availFilter)
                        && !team.getAvailabilityStatus().getLabel()
                                .equals(availFilter)) {
                    continue;
                }
                if (!"All".equals(opFilter)
                        && (team.getOperationalStatus() == null
                            || !team.getOperationalStatus().getLabel()
                                    .equals(opFilter))) {
                    continue;
                }
                total++;
                switch (team.getAvailabilityStatus()) {
                    case AVAILABLE -> available++;
                    case DEPLOYED -> deployed++;
                    case OFF_DUTY, UNAVAILABLE -> offDuty++;
                    default -> { }
                }
                tableModel.addRow(
                        RescueTeamController.toRow(team));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        totalTile.setText(String.valueOf(total));
        availableTile.setText(String.valueOf(available));
        deployedTile.setText(String.valueOf(deployed));
        offDutyTile.setText(String.valueOf(offDuty));
    }

    // ── team profile dialog ──────────────────────────────────────────

    private void viewProfile() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this,
                    "Select a team in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            RescueTeam team = controller.getAllTeams().stream()
                    .filter(t -> t.getId().equals(id))
                    .findFirst().orElse(null);
            if (team == null) {
                ViewUtil.error(this,
                        "Team not found: " + id);
                return;
            }
            showProfileDialog(team);
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void showProfileDialog(RescueTeam team) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Team Profile - " + team.getTeamName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(760, 580);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(
                10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder(
                "Team Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int r = 0;
        r = addInfoRow(infoPanel, gbc, r, "Team:",
                team.getTeamName());
        r = addInfoRow(infoPanel, gbc, r, "Type:",
                team.getTeamType().getLabel());
        r = addInfoRow(infoPanel, gbc, r, "Leader:",
                team.getLeaderName());
        r = addInfoRow(infoPanel, gbc, r, "Contact:",
                team.getContactNumber());
        r = addInfoRow(infoPanel, gbc, r, "Base:",
                team.getBaseLocation() == null
                        ? "-" : team.getBaseLocation());
        r = addInfoRow(infoPanel, gbc, r, "Members:",
                String.valueOf(team.getMemberCount()));
        r = addInfoRow(infoPanel, gbc, r, "Availability:",
                team.getAvailabilityStatus().getLabel());
        r = addInfoRow(infoPanel, gbc, r, "Operational:",
                team.getOperationalStatus() == null
                        ? "-"
                        : team.getOperationalStatus().getLabel());

        content.add(infoPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Members", buildMembersTab(team.getId()));
        tabs.add("Skills", buildSkillsTab(team.getId()));
        tabs.add("Equipment", buildEquipmentTab(team.getId()));
        tabs.add("Assignments", buildAssignmentsTab(team.getId()));
        content.add(tabs, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(event -> dialog.dispose());
        JButton editBtn = new JButton("Edit Team Information");
        editBtn.addActionListener(event -> {
            editTeamDialog(team);
            refreshTable();
        });
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.add(editBtn);
        foot.add(closeBtn);
        content.add(foot, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    /** Editable dialog for changing a team's core information. */
    private void editTeamDialog(RescueTeam team) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Edit Team - " + team.getTeamName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 360);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(
                10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameIn = new JTextField(team.getTeamName(), 18);
        JComboBox<TeamType> typeIn =
                new JComboBox<>(TeamType.values());
        typeIn.setSelectedItem(team.getTeamType());
        JTextField leaderIn =
                new JTextField(team.getLeaderName(), 18);
        JTextField contactIn =
                new JTextField(team.getContactNumber(), 12);
        JTextField membersIn = new JTextField(
                String.valueOf(team.getMemberCount()), 5);
        JTextField skillsIn = new JTextField(
                team.getSkills(), 18);
        JTextField equipIn = new JTextField(
                team.getEquipment(), 18);
        JTextField baseIn = new JTextField(
                team.getBaseLocation(), 18);

        int row = 0;
        row = editRow(form, gbc, row, "Team name:", nameIn);
        row = editRow(form, gbc, row, "Type:", typeIn);
        row = editRow(form, gbc, row, "Leader:", leaderIn);
        row = editRow(form, gbc, row, "Contact (10 digits):",
                contactIn);
        row = editRow(form, gbc, row, "Members:", membersIn);
        row = editRow(form, gbc, row, "Skills:", skillsIn);
        row = editRow(form, gbc, row, "Equipment:", equipIn);
        row = editRow(form, gbc, row, "Base location:", baseIn);

        JButton saveBtn = new JButton("Save Changes");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(saveBtn, gbc);

        saveBtn.addActionListener(event -> {
            ActionResult r = controller.updateTeam(team.getId(),
                    nameIn.getText(),
                    (TeamType) typeIn.getSelectedItem(),
                    leaderIn.getText(),
                    contactIn.getText(),
                    membersIn.getText(),
                    skillsIn.getText(),
                    equipIn.getText(),
                    baseIn.getText());
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

    private int editRow(JPanel panel, GridBagConstraints gbc,
                        int row, String label,
                        javax.swing.JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
        return row + 1;
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

    // ── members tab ──────────────────────────────────────────────────

    private JPanel buildMembersTab(long teamId) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(
                6, 6, 6, 6));

        DefaultTableModel model = ViewUtil.readOnlyModel(
                RescueTeamController.memberTableHeaders());
        JTable tbl = new JTable(model);

        try {
            for (TeamMember m : controller.getMembers(teamId)) {
                model.addRow(
                        RescueTeamController.toMemberRow(m));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(panel, e.getMessage());
        }

        panel.add(new JScrollPane(tbl), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints fgc = new GridBagConstraints();
        fgc.insets = new Insets(3, 3, 3, 3);
        fgc.anchor = GridBagConstraints.WEST;

        JTextField nameIn = new JTextField(12);
        JTextField roleIn = new JTextField(10);
        JTextField contactIn = new JTextField(10);
        JTextField skillsIn = new JTextField(10);

        fgc.gridx = 0; fgc.gridy = 0;
        form.add(new JLabel("Name:"), fgc);
        fgc.gridx = 1;
        form.add(nameIn, fgc);
        fgc.gridx = 2;
        form.add(new JLabel("Role:"), fgc);
        fgc.gridx = 3;
        form.add(roleIn, fgc);
        fgc.gridy = 1; fgc.gridx = 0;
        form.add(new JLabel("Contact:"), fgc);
        fgc.gridx = 1;
        form.add(contactIn, fgc);
        fgc.gridx = 2;
        form.add(new JLabel("Skills:"), fgc);
        fgc.gridx = 3;
        form.add(skillsIn, fgc);

        JButton addBtn = new JButton("Add member");
        JButton delBtn = new JButton("Delete selected");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(addBtn);
        btns.add(delBtn);

        JPanel footer = new JPanel(new BorderLayout(6, 4));
        footer.add(form, BorderLayout.NORTH);
        footer.add(btns, BorderLayout.SOUTH);
        panel.add(scrollableFooter(footer), BorderLayout.SOUTH);

        addBtn.addActionListener(event -> {
            ActionResult r = controller.addMember(teamId,
                    nameIn.getText(), roleIn.getText(),
                    contactIn.getText(), skillsIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(panel, r.getMessage());
                nameIn.setText("");
                roleIn.setText("");
                contactIn.setText("");
                skillsIn.setText("");
                model.setRowCount(0);
                try {
                    for (TeamMember m :
                            controller.getMembers(teamId)) {
                        model.addRow(
                                RescueTeamController.toMemberRow(
                                        m));
                    }
                } catch (DataAccessException ex) {
                    ViewUtil.error(panel, ex.getMessage());
                }
            } else {
                ViewUtil.error(panel, r.getMessage());
            }
        });

        delBtn.addActionListener(event -> {
            int row = tbl.getSelectedRow();
            if (row < 0) {
                ViewUtil.error(panel,
                        "Select a member to delete");
                return;
            }
            Long mId = (Long) model.getValueAt(row, 0);
            int choice = JOptionPane.showConfirmDialog(panel,
                    "Delete this member?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            ActionResult r = controller.deleteMember(mId);
            if (r.isSuccess()) {
                ViewUtil.info(panel, r.getMessage());
                model.removeRow(row);
            } else {
                ViewUtil.error(panel, r.getMessage());
            }
        });

        return panel;
    }

    /** Wraps footer content so it never gets clipped - scrolls if needed. */
    private JScrollPane scrollableFooter(JPanel inner) {
        JScrollPane sp = new JScrollPane(inner,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        sp.setAlignmentX(javax.swing.SwingConstants.LEFT);
        return sp;
    }

    // ── skills tab ───────────────────────────────────────────────────

    private JPanel buildSkillsTab(long teamId) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(
                6, 6, 6, 6));

        DefaultTableModel model = ViewUtil.readOnlyModel(
                RescueTeamController.skillTableHeaders());
        JTable tbl = new JTable(model);

        try {
            for (TeamSkill s : controller.getSkills(teamId)) {
                model.addRow(
                        RescueTeamController.toSkillRow(s));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(panel, e.getMessage());
        }

        panel.add(new JScrollPane(tbl), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints fgc = new GridBagConstraints();
        fgc.insets = new Insets(3, 3, 3, 3);
        fgc.anchor = GridBagConstraints.WEST;
        JTextField skillIn = new JTextField(14);
        JTextField descIn = new JTextField(14);
        fgc.gridx = 0; fgc.gridy = 0;
        form.add(new JLabel("Skill:"), fgc);
        fgc.gridx = 1;
        form.add(skillIn, fgc);
        fgc.gridx = 2;
        form.add(new JLabel("Description:"), fgc);
        fgc.gridx = 3;
        form.add(descIn, fgc);

        JButton addBtn = new JButton("Add skill");
        JButton delBtn = new JButton("Delete selected");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(addBtn);
        btns.add(delBtn);

        JPanel footer = new JPanel(new BorderLayout(6, 4));
        footer.add(form, BorderLayout.NORTH);
        footer.add(btns, BorderLayout.SOUTH);
        panel.add(scrollableFooter(footer), BorderLayout.SOUTH);

        addBtn.addActionListener(event -> {
            ActionResult r = controller.addSkill(teamId,
                    skillIn.getText(), descIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(panel, r.getMessage());
                skillIn.setText("");
                descIn.setText("");
                model.setRowCount(0);
                try {
                    for (TeamSkill s :
                            controller.getSkills(teamId)) {
                        model.addRow(
                                RescueTeamController.toSkillRow(
                                        s));
                    }
                } catch (DataAccessException ex) {
                    ViewUtil.error(panel, ex.getMessage());
                }
            } else {
                ViewUtil.error(panel, r.getMessage());
            }
        });

        delBtn.addActionListener(event -> {
            int row = tbl.getSelectedRow();
            if (row < 0) {
                ViewUtil.error(panel,
                        "Select a skill to delete");
                return;
            }
            Long sId = (Long) model.getValueAt(row, 0);
            int choice = JOptionPane.showConfirmDialog(panel,
                    "Delete this skill?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            ActionResult r = controller.deleteSkill(sId);
            if (r.isSuccess()) {
                ViewUtil.info(panel, r.getMessage());
                model.removeRow(row);
            } else {
                ViewUtil.error(panel, r.getMessage());
            }
        });

        return panel;
    }

    // ── equipment tab ────────────────────────────────────────────────

    private JPanel buildEquipmentTab(long teamId) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(
                6, 6, 6, 6));

        DefaultTableModel model = ViewUtil.readOnlyModel(
                RescueTeamController.equipmentTableHeaders());
        JTable tbl = new JTable(model);

        try {
            for (TeamEquipment e :
                    controller.getEquipment(teamId)) {
                model.addRow(
                        RescueTeamController.toEquipmentRow(e));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(panel, e.getMessage());
        }

        panel.add(new JScrollPane(tbl), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints fgc = new GridBagConstraints();
        fgc.insets = new Insets(3, 3, 3, 3);
        fgc.anchor = GridBagConstraints.WEST;
        JTextField eqIn = new JTextField(12);
        JTextField qtyIn = new JTextField(4);
        JTextField descIn = new JTextField(12);
        fgc.gridx = 0; fgc.gridy = 0;
        form.add(new JLabel("Equipment:"), fgc);
        fgc.gridx = 1;
        form.add(eqIn, fgc);
        fgc.gridx = 2;
        form.add(new JLabel("Qty:"), fgc);
        fgc.gridx = 3;
        form.add(qtyIn, fgc);
        fgc.gridx = 4;
        form.add(new JLabel("Description:"), fgc);
        fgc.gridx = 5;
        form.add(descIn, fgc);

        JButton addBtn = new JButton("Add equipment");
        JButton delBtn = new JButton("Delete selected");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(addBtn);
        btns.add(delBtn);

        JPanel footer = new JPanel(new BorderLayout(6, 4));
        footer.add(form, BorderLayout.NORTH);
        footer.add(btns, BorderLayout.SOUTH);
        panel.add(scrollableFooter(footer), BorderLayout.SOUTH);

        addBtn.addActionListener(event -> {
            ActionResult r = controller.addEquipment(teamId,
                    eqIn.getText(), qtyIn.getText(),
                    descIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(panel, r.getMessage());
                eqIn.setText("");
                qtyIn.setText("");
                descIn.setText("");
                model.setRowCount(0);
                try {
                    for (TeamEquipment e :
                            controller.getEquipment(teamId)) {
                        model.addRow(
                                RescueTeamController
                                        .toEquipmentRow(e));
                    }
                } catch (DataAccessException ex) {
                    ViewUtil.error(panel, ex.getMessage());
                }
            } else {
                ViewUtil.error(panel, r.getMessage());
            }
        });

        delBtn.addActionListener(event -> {
            int row = tbl.getSelectedRow();
            if (row < 0) {
                ViewUtil.error(panel,
                        "Select equipment to delete");
                return;
            }
            Long eId = (Long) model.getValueAt(row, 0);
            int choice = JOptionPane.showConfirmDialog(panel,
                    "Delete this equipment?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            ActionResult r = controller.deleteEquipment(eId);
            if (r.isSuccess()) {
                ViewUtil.info(panel, r.getMessage());
                model.removeRow(row);
            } else {
                ViewUtil.error(panel, r.getMessage());
            }
        });

        return panel;
    }

    // ── assignments tab ──────────────────────────────────────────────

    private JPanel buildAssignmentsTab(long teamId) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(
                6, 6, 6, 6));

        String[] cols = {"ID", "Request ID", "Status",
                "Notes", "Completed At"};
        DefaultTableModel model = ViewUtil.readOnlyModel(cols);
        JTable tbl = new JTable(model);

        try {
            List<RescueAssignment> assignments =
                    new com.resqhub.dao.RescueAssignmentDAO()
                            .findByTeam(teamId);
            for (RescueAssignment a : assignments) {
                model.addRow(new Object[]{
                        a.getId(),
                        a.getRescueRequestId(),
                        a.getAssignmentStatus().getLabel(),
                        a.getNotes() == null
                                ? "-" : a.getNotes(),
                        a.getCompletedAt() == null
                                ? "-" : a.getCompletedAt()
                });
            }
        } catch (DataAccessException e) {
            ViewUtil.error(panel, e.getMessage());
        }

        panel.add(new JScrollPane(tbl), BorderLayout.CENTER);
        return panel;
    }

    // ── edit / save / register / delete ──────────────────────────────

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this,
                    "Select a team in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            for (RescueTeam c : controller.getAllTeams()) {
                if (c.getId().equals(id)) {
                    nameField.setText(c.getTeamName());
                    typeCombo.setSelectedItem(c.getTeamType());
                    leaderField.setText(c.getLeaderName());
                    contactField.setText(c.getContactNumber());
                    memberCountField.setText(
                            String.valueOf(c.getMemberCount()));
                    skillsField.setText(c.getSkills());
                    equipmentField.setText(c.getEquipment());
                    baseField.setText(c.getBaseLocation());
                }
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return;
        }
        editingId = id;
        saveChangesButton.setEnabled(true);
        ViewUtil.info(this, "Editing team #" + id
                + " - change the form and press Save changes");
    }

    private void saveChanges() {
        if (editingId == null) {
            return;
        }
        ActionResult result = controller.updateTeam(editingId,
                nameField.getText(),
                (TeamType) typeCombo.getSelectedItem(),
                leaderField.getText(),
                contactField.getText(),
                memberCountField.getText(),
                skillsField.getText(),
                equipmentField.getText(),
                baseField.getText());
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            editingId = null;
            saveChangesButton.setEnabled(false);
            nameField.setText("");
            leaderField.setText("");
            contactField.setText("");
            memberCountField.setText("");
            skillsField.setText("");
            equipmentField.setText("");
            baseField.setText("");
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    private void registerTeam() {
        ActionResult result = controller.registerTeam(
                nameField.getText(),
                (TeamType) typeCombo.getSelectedItem(),
                leaderField.getText(),
                contactField.getText(),
                memberCountField.getText(),
                skillsField.getText(),
                equipmentField.getText(),
                baseField.getText());
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            nameField.setText("");
            leaderField.setText("");
            contactField.setText("");
            memberCountField.setText("");
            skillsField.setText("");
            equipmentField.setText("");
            baseField.setText("");
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this,
                    "Select a team in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Permanently delete team #" + id + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult result = controller.deleteTeam(id);
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    @Override
    public void refreshData() {
        refreshTable();
    }
}
