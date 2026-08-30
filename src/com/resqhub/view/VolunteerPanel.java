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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.VolunteerController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.EmergencyRole;
import com.resqhub.model.RoleType;
import com.resqhub.model.Volunteer;
import com.resqhub.model.VolunteerActivity;
import com.resqhub.model.VolunteerAssignment;
import com.resqhub.model.VolunteerAvailability;
import com.resqhub.model.VolunteerSkill;
import com.resqhub.model.VolunteerTaskStatus;
import com.resqhub.service.SessionManager;
import com.resqhub.service.VolunteerService;

/**
 * Volunteer management screen: registration, search/filter, profile dialog
 * with skills, tasks and activity tabs, and smart assignment.
 */
public class VolunteerPanel extends JPanel implements Refreshable {

    private final VolunteerController controller =
            new VolunteerController();

    // ── registration form ────────────────────────────────────────────
    private final JTextField nameField = new JTextField(16);
    private final JTextField contactField = new JTextField(12);
    private final JTextField emailField = new JTextField(16);
    private final JTextField locationField = new JTextField(16);
    private final JTextField skillsField = new JTextField(16);
    private final JComboBox<VolunteerAvailability> availCombo =
            new JComboBox<>(VolunteerAvailability.values());
    private final JComboBox<EmergencyRole> roleCombo =
            new JComboBox<>(EmergencyRole.values());
    private final JTextField maxWorkloadField = new JTextField(4);

    // ── table ────────────────────────────────────────────────────────
    private final DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(VolunteerController.tableHeaders());
    private final JTable table = new JTable(tableModel);

    // ── controls ─────────────────────────────────────────────────────
    private final JTextField searchField = new JTextField(14);
    private final JComboBox<String> availFilterCombo = new JComboBox<>(
            new String[]{"All", "Available", "Busy", "Unavailable"});
    private final JComboBox<String> roleFilterCombo = new JComboBox<>(
            new String[]{"All", "Medical support", "Shelter support",
                    "Food distribution", "Rescue support",
                    "Communication", "Resource handling", "General"});
    private Long editingId = null;
    private final JButton saveChangesButton =
            new JButton("Save changes");

    // ── stat tiles ───────────────────────────────────────────────────
    private final JLabel totalTile = new JLabel("0");
    private final JLabel availableTile = new JLabel("0");
    private final JLabel busyTile = new JLabel("0");
    private final JLabel completedTile = new JLabel("0");

    public VolunteerPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.WEST);
        add(buildTableArea(), BorderLayout.CENTER);
        refreshTable();
    }

    // ── header with stat tiles ───────────────────────────────────────

    private JPanel buildHeader() {
        JLabel title = new JLabel("VOLUNTEERS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("TOTAL", totalTile, new Color(60, 60, 60)));
        tiles.add(statTile("AVAILABLE", availableTile,
                new Color(40, 110, 40)));
        tiles.add(statTile("BUSY", busyTile, new Color(40, 100, 160)));
        tiles.add(statTile("TASKS DONE", completedTile,
                new Color(140, 110, 20)));

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
                "Register volunteer"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addRow(form, gbc, row, "Full name:", nameField);
        row = addRow(form, gbc, row, "Contact (10 digits):",
                contactField);
        row = addRow(form, gbc, row, "Email:", emailField);
        row = addRow(form, gbc, row, "Location:", locationField);
        row = addRow(form, gbc, row, "Skills:", skillsField);
        row = addRow(form, gbc, row, "Availability:", availCombo);
        row = addRow(form, gbc, row, "Emergency role:", roleCombo);
        row = addRow(form, gbc, row, "Max workload:", maxWorkloadField);

        JButton registerButton = new JButton("Register volunteer");
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(registerButton, gbc);
        registerButton.addActionListener(event -> registerVolunteer());
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
        controls.add(availFilterCombo);
        controls.add(new JLabel("Role:"));
        controls.add(roleFilterCombo);

        controls.add(Box.createHorizontalStrut(10));

        JButton viewProfileBtn = new JButton("View Profile");
        controls.add(viewProfileBtn);
        JButton assignBtn = new JButton("Assign Task");
        controls.add(assignBtn);
        JButton smartBtn = new JButton("Smart Assign");
        controls.add(smartBtn);

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
            availFilterCombo.setSelectedIndex(0);
            roleFilterCombo.setSelectedIndex(0);
            refreshTable();
        });
        availFilterCombo.addActionListener(event -> refreshTable());
        roleFilterCombo.addActionListener(event -> refreshTable());
        viewProfileBtn.addActionListener(event -> viewProfile());
        assignBtn.addActionListener(event -> openAssignDialog());
        smartBtn.addActionListener(event -> openSmartAssignDialog());
        refreshBtn.addActionListener(event -> refreshTable());
        deleteBtn.addActionListener(event -> deleteSelected());
        editBtn.addActionListener(event -> editSelected());
        saveChangesButton.addActionListener(event -> saveChanges());
        exportBtn.addActionListener(event ->
                ViewUtil.exportTableToCsv(this, table, "volunteers"));

        return area;
    }

    // ── table refresh ────────────────────────────────────────────────

    private void refreshTable() {
        tableModel.setRowCount(0);
        int total = 0, available = 0, busy = 0;
        try {
            String needle = searchField.getText() == null
                    ? "" : searchField.getText().trim();
            String availFilter = String.valueOf(
                    availFilterCombo.getSelectedItem());
            String roleFilter = String.valueOf(
                    roleFilterCombo.getSelectedItem());

            List<Volunteer> volunteers;
            if (!needle.isEmpty()) {
                volunteers = controller.searchVolunteers(needle);
            } else {
                volunteers = controller.getAllVolunteers();
            }

            for (Volunteer v : volunteers) {
                if (!"All".equals(availFilter)
                        && !v.getAvailability().getLabel()
                                .equals(availFilter)) {
                    continue;
                }
                if (!"All".equals(roleFilter)
                        && (v.getEmergencyRole() == null
                            || !v.getEmergencyRole().getLabel()
                                    .equals(roleFilter))) {
                    continue;
                }
                total++;
                switch (v.getAvailability()) {
                    case AVAILABLE -> available++;
                    case BUSY -> busy++;
                    default -> { }
                }
                tableModel.addRow(
                        VolunteerController.toRow(v));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
        }
        totalTile.setText(String.valueOf(total));
        availableTile.setText(String.valueOf(available));
        busyTile.setText(String.valueOf(busy));
        try {
            completedTile.setText(String.valueOf(
                    controller.getCompletedTaskCount()));
        } catch (Exception ignored) {
            completedTile.setText("0");
        }
    }

    // ── profile dialog ───────────────────────────────────────────────

    private void viewProfile() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this,
                    "Select a volunteer in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            Volunteer v = controller.getVolunteer(id);
            showProfileDialog(v);
        } catch (ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
        }
    }

    private void showProfileDialog(Volunteer v) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Volunteer Profile - " + v.getFullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(760, 580);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(
                10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder(
                "Profile Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int r = 0;
        r = addInfoRow(infoPanel, gbc, r, "Name:",
                v.getFullName());
        r = addInfoRow(infoPanel, gbc, r, "Contact:",
                v.getContactNumber());
        r = addInfoRow(infoPanel, gbc, r, "Email:",
                v.getEmail() == null ? "-" : v.getEmail());
        r = addInfoRow(infoPanel, gbc, r, "Location:",
                v.getLocation());
        r = addInfoRow(infoPanel, gbc, r, "Emergency role:",
                v.getEmergencyRole() == null
                        ? "-" : v.getEmergencyRole().getLabel());
        r = addInfoRow(infoPanel, gbc, r, "Availability:",
                v.getAvailability().getLabel());
        try {
            r = addInfoRow(infoPanel, gbc, r, "Active tasks:",
                    String.valueOf(controller.getWorkload(v.getId())));
        } catch (DataAccessException e) {
            r = addInfoRow(infoPanel, gbc, r, "Active tasks:",
                    "-");
        }

        content.add(infoPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Skills", buildSkillsTab(v.getId()));
        tabs.add("Tasks", buildTasksTab(v));
        tabs.add("Activity", buildActivityTab(v.getId()));
        content.add(tabs, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(event -> dialog.dispose());
        JButton editBtn = new JButton("Edit Volunteer");
        editBtn.addActionListener(event -> {
            editVolunteerDialog(v);
            refreshTable();
        });
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.add(editBtn);
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

    // ── skills tab ───────────────────────────────────────────────────

    private JPanel buildSkillsTab(long volunteerId) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(
                6, 6, 6, 6));

        DefaultTableModel model = ViewUtil.readOnlyModel(
                VolunteerController.skillTableHeaders());
        JTable tbl = new JTable(model);

        try {
            for (VolunteerSkill s :
                    controller.getSkills(volunteerId)) {
                model.addRow(
                        VolunteerController.toSkillRow(s));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(panel, e.getMessage());
        }

        panel.add(new JScrollPane(tbl), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints fgc = new GridBagConstraints();
        fgc.insets = new Insets(3, 3, 3, 3);
        fgc.anchor = GridBagConstraints.WEST;
        JTextField skillIn = new JTextField(18);
        fgc.gridx = 0; fgc.gridy = 0;
        form.add(new JLabel("Skill:"), fgc);
        fgc.gridx = 1;
        form.add(skillIn, fgc);

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
            ActionResult r = controller.addSkill(volunteerId,
                    skillIn.getText());
            if (r.isSuccess()) {
                ViewUtil.info(panel, r.getMessage());
                skillIn.setText("");
                model.setRowCount(0);
                try {
                    for (VolunteerSkill s :
                            controller.getSkills(volunteerId)) {
                        model.addRow(
                                VolunteerController.toSkillRow(s));
                    }
                } catch (DataAccessException ex) {
                    ViewUtil.error(panel, ex.getMessage());
                }
                // update the summary skills text field too
                try {
                    Volunteer updated = controller.getVolunteer(
                            volunteerId);
                    skillsField.setText(updated.getSkills());
                } catch (ResQHubException ignored) {
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

    // ── tasks tab ────────────────────────────────────────────────────

    private JPanel buildTasksTab(Volunteer v) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(
                6, 6, 6, 6));

        DefaultTableModel model = ViewUtil.readOnlyModel(
                VolunteerController.taskTableHeaders());
        JTable tbl = new JTable(model);

        try {
            for (VolunteerAssignment a :
                    controller.getTasks(v.getId())) {
                model.addRow(
                        VolunteerController.toTaskRow(a));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(panel, e.getMessage());
        }

        panel.add(new JScrollPane(tbl), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<VolunteerTaskStatus> statusCombo =
                new JComboBox<>(VolunteerTaskStatus.values());
        JButton updateBtn = new JButton("Update Status");
        JButton assignBtn = new JButton("Assign New Task");
        btns.add(new JLabel("Set status:"));
        btns.add(statusCombo);
        btns.add(updateBtn);
        btns.add(assignBtn);
        panel.add(scrollableFooter(btns), BorderLayout.SOUTH);

        updateBtn.addActionListener(event -> {
            int row = tbl.getSelectedRow();
            if (row < 0) {
                ViewUtil.error(panel,
                        "Select a task to update");
                return;
            }
            Long aId = (Long) model.getValueAt(row, 0);
            ActionResult r = controller.updateTaskStatus(aId,
                    (VolunteerTaskStatus)
                            statusCombo.getSelectedItem());
            if (r.isSuccess()) {
                ViewUtil.info(panel, r.getMessage());
                model.setRowCount(0);
                try {
                    for (VolunteerAssignment a :
                            controller.getTasks(v.getId())) {
                        model.addRow(
                                VolunteerController.toTaskRow(a));
                    }
                } catch (DataAccessException ex) {
                    ViewUtil.error(panel, ex.getMessage());
                }
                refreshTable();
            } else {
                ViewUtil.error(panel, r.getMessage());
            }
        });

        assignBtn.addActionListener(event ->
                openAssignDialogFor(v.getId()));

        return panel;
    }

    // ── activity tab ─────────────────────────────────────────────────

    private JPanel buildActivityTab(long volunteerId) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(
                6, 6, 6, 6));

        DefaultTableModel model = ViewUtil.readOnlyModel(
                VolunteerController.activityTableHeaders());
        JTable tbl = new JTable(model);

        try {
            for (VolunteerActivity a :
                    controller.getActivity(volunteerId)) {
                model.addRow(
                        VolunteerController.toActivityRow(a));
            }
        } catch (DataAccessException e) {
            ViewUtil.error(panel, e.getMessage());
        }

        panel.add(new JScrollPane(tbl), BorderLayout.CENTER);
        return panel;
    }

    /** Wraps footer content so it never gets clipped - scrolls if needed. */
    private JScrollPane scrollableFooter(JPanel inner) {
        JScrollPane sp = new JScrollPane(inner,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    // ── assignment dialogs ───────────────────────────────────────────

    private void openAssignDialog() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this,
                    "Select a volunteer in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        openAssignDialogFor(id);
    }

    private void openAssignDialogFor(Long volunteerId) {
        final Long target = volunteerId;

        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Assign Task to Volunteer #" + target,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(430, 300);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(
                10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField taskIn = new JTextField(20);
        JTextField locIn = new JTextField(16);
        JTextField prioIn = new JTextField(3);
        JTextArea descArea = new JTextArea(3, 20);

        int row = 0;
        row = addRow(form, gbc, row, "Task name:", taskIn);
        row = addRow(form, gbc, row, "Location:", locIn);
        row = addRow(form, gbc, row, "Priority (1-5):", prioIn);
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        form.add(new JScrollPane(descArea), gbc);
        row++;

        JButton assignBtn = new JButton("Assign");
        gbc.gridx = 1; gbc.gridy = row;
        form.add(assignBtn, gbc);

        assignBtn.addActionListener(event -> {
            ActionResult r = controller.assignTask(target,
                    taskIn.getText(), descArea.getText(),
                    locIn.getText(), prioIn.getText());
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

    private void openSmartAssignDialog() {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Smart Assign Best Volunteer",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(460, 340);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(
                10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField taskIn = new JTextField(20);
        JTextField skillsIn = new JTextField(20);
        JTextField locIn = new JTextField(16);
        JTextField prioIn = new JTextField(3);
        JTextArea descArea = new JTextArea(3, 20);

        int row = 0;
        row = addRow(form, gbc, row, "Task name:", taskIn);
        row = addRow(form, gbc, row, "Required skills:", skillsIn);
        row = addRow(form, gbc, row, "Task location:", locIn);
        row = addRow(form, gbc, row, "Priority (1-5):", prioIn);
        gbc.gridx = 0; gbc.gridy = row;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        form.add(new JScrollPane(descArea), gbc);
        row++;

        JButton assignBtn = new JButton("Smart Assign");
        gbc.gridx = 1; gbc.gridy = row;
        form.add(assignBtn, gbc);

        assignBtn.addActionListener(event -> {
            ActionResult r = controller.smartAssign(taskIn.getText(),
                    descArea.getText(), locIn.getText(),
                    skillsIn.getText(), prioIn.getText());
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

    // ── edit dialog ──────────────────────────────────────────────────

    private void editVolunteerDialog(Volunteer v) {
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Edit Volunteer - " + v.getFullName(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(430, 380);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(
                10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameIn = new JTextField(v.getFullName(), 18);
        JTextField contactIn =
                new JTextField(v.getContactNumber(), 12);
        JTextField emailIn =
                new JTextField(v.getEmail(), 18);
        JTextField locationIn =
                new JTextField(v.getLocation(), 18);
        JTextField skillsIn =
                new JTextField(v.getSkills(), 18);
        JComboBox<VolunteerAvailability> availIn =
                new JComboBox<>(VolunteerAvailability.values());
        availIn.setSelectedItem(v.getAvailability());
        JComboBox<EmergencyRole> roleIn =
                new JComboBox<>(EmergencyRole.values());
        roleIn.setSelectedItem(v.getEmergencyRole());
        JTextField maxIn =
                new JTextField(String.valueOf(v.getMaxWorkload()), 4);

        int row = 0;
        row = addRow(form, gbc, row, "Full name:", nameIn);
        row = addRow(form, gbc, row, "Contact (10 digits):",
                contactIn);
        row = addRow(form, gbc, row, "Email:", emailIn);
        row = addRow(form, gbc, row, "Location:", locationIn);
        row = addRow(form, gbc, row, "Skills:", skillsIn);
        row = addRow(form, gbc, row, "Availability:", availIn);
        row = addRow(form, gbc, row, "Emergency role:", roleIn);
        row = addRow(form, gbc, row, "Max workload:", maxIn);

        JButton saveBtn = new JButton("Save Changes");
        gbc.gridx = 1; gbc.gridy = row;
        form.add(saveBtn, gbc);

        saveBtn.addActionListener(event -> {
            ActionResult r = controller.updateVolunteer(v.getId(),
                    nameIn.getText(), contactIn.getText(),
                    emailIn.getText(), locationIn.getText(),
                    skillsIn.getText(),
                    (VolunteerAvailability) availIn.getSelectedItem(),
                    (EmergencyRole) roleIn.getSelectedItem(),
                    maxIn.getText());
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

    // ── edit / save / register / delete ──────────────────────────────

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this,
                    "Select a volunteer in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        try {
            Volunteer v = controller.getVolunteer(id);
            nameField.setText(v.getFullName());
            contactField.setText(v.getContactNumber());
            emailField.setText(v.getEmail());
            locationField.setText(v.getLocation());
            skillsField.setText(v.getSkills());
            availCombo.setSelectedItem(v.getAvailability());
            roleCombo.setSelectedItem(v.getEmergencyRole());
            maxWorkloadField.setText(
                    String.valueOf(v.getMaxWorkload()));
        } catch (ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
            return;
        }
        editingId = id;
        saveChangesButton.setEnabled(true);
        ViewUtil.info(this, "Editing volunteer #" + id
                + " - change the form and press Save changes");
    }

    private void saveChanges() {
        if (editingId == null) {
            return;
        }
        ActionResult result = controller.updateVolunteer(editingId,
                nameField.getText(), contactField.getText(),
                emailField.getText(), locationField.getText(),
                skillsField.getText(),
                (VolunteerAvailability) availCombo.getSelectedItem(),
                (EmergencyRole) roleCombo.getSelectedItem(),
                maxWorkloadField.getText());
        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            editingId = null;
            saveChangesButton.setEnabled(false);
            clearForm();
        } else {
            ViewUtil.error(this, result.getMessage());
        }
        refreshTable();
    }

    private void registerVolunteer() {
        ActionResult result = controller.registerVolunteer(
                nameField.getText(), contactField.getText(),
                emailField.getText(), locationField.getText(),
                skillsField.getText(),
                (VolunteerAvailability) availCombo.getSelectedItem(),
                (EmergencyRole) roleCombo.getSelectedItem(),
                maxWorkloadField.getText());
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
        contactField.setText("");
        emailField.setText("");
        locationField.setText("");
        skillsField.setText("");
        availCombo.setSelectedIndex(0);
        roleCombo.setSelectedIndex(0);
        maxWorkloadField.setText("2");
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            ViewUtil.error(this,
                    "Select a volunteer in the table first");
            return;
        }
        Long id = (Long) tableModel.getValueAt(viewRow, 0);
        int choice = JOptionPane.showConfirmDialog(this,
                "Permanently delete volunteer #" + id + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        ActionResult result = controller.deleteVolunteer(id);
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
