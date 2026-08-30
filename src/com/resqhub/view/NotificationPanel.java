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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.NotificationController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationPriority;
import com.resqhub.model.NotificationStatus;
import com.resqhub.model.NotificationType;
import com.resqhub.model.RoleType;
import com.resqhub.service.SessionManager;

/**
 * Notification Center: a single place for a user to see every alert
 * relevant to them. Auto-detected alerts (critical rescue, low stock)
 * plus assignment and broadcast alerts all land here. Supports
 * priority / type / status filtering, mark-read / archive, alert
 * details, automatic generation and (for staff) role-targeted
 * broadcasts.
 */
public class NotificationPanel extends JPanel implements Refreshable {

    private final NotificationController controller =
            new NotificationController();

    private final DefaultTableModel tableModel =
            ViewUtil.readOnlyModel(NotificationController.tableHeaders());
    private final JTable table = new JTable(tableModel);

    private final JComboBox<String> priorityCombo = new JComboBox<>(
            new String[]{"All Priorities", "Critical", "Warning",
                    "Information"});
    private final JComboBox<String> typeCombo = new JComboBox<>(
            new String[]{"All Types", "Critical Rescue", "Low Stock",
                    "Assignment", "System"});
    private final JComboBox<String> statusCombo = new JComboBox<>(
            new String[]{"All Statuses", "Unread", "Read", "Archived"});

    private final JLabel unreadTile = new JLabel("0");
    private final JLabel criticalTile = new JLabel("0");
    private final JLabel warningTile = new JLabel("0");
    private final JLabel totalTile = new JLabel("0");

    public NotificationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        refreshData();
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("NOTIFICATION CENTER");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tiles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        tiles.add(statTile("UNREAD", unreadTile, new Color(180, 60, 40)));
        tiles.add(statTile("CRITICAL", criticalTile,
                new Color(150, 30, 30)));
        tiles.add(statTile("WARNING", warningTile,
                new Color(200, 130, 20)));
        tiles.add(statTile("TOTAL", totalTile, new Color(60, 90, 150)));

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

    private JPanel buildCenter() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.add(new JLabel("Filter:"));
        toolbar.add(priorityCombo);
        toolbar.add(typeCombo);
        toolbar.add(statusCombo);
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> refreshTable());
        toolbar.add(applyButton);

        JButton autoButton = new JButton("Generate Automatic Alerts");
        autoButton.addActionListener(e -> generateAutomatic());
        toolbar.add(autoButton);

        JButton markAllButton = new JButton("Mark All Read");
        markAllButton.addActionListener(e -> markAllRead());
        toolbar.add(markAllButton);

        JButton exportButton = new JButton("Export CSV");
        exportButton.addActionListener(e -> ViewUtil.exportTableToCsv(
                this, table, "notifications"));
        toolbar.add(exportButton);

        boolean staff = SessionManager.getInstance()
                .hasRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (staff) {
            JButton broadcastButton = new JButton("Broadcast Alert");
            broadcastButton.addActionListener(e -> broadcastDialog());
            toolbar.add(broadcastButton);
        }

        JButton viewButton = new JButton("View Details");
        viewButton.addActionListener(e -> viewDetails());
        JButton readButton = new JButton("Mark Read");
        readButton.addActionListener(e -> markSelectedRead());
        JButton archiveButton = new JButton("Archive");
        archiveButton.addActionListener(e -> archiveSelected());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        actions.add(viewButton);
        actions.add(readButton);
        actions.add(archiveButton);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(toolbar);
        controls.add(actions);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder(
                "NOTIFICATIONS"));

        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(controls, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        return center;
    }

    private void generateAutomatic() {
        ActionResult result = controller.generateAutomaticAlerts();
        if (!result.isSuccess()) {
            ViewUtil.error(this, result.getMessage());
        } else {
            if (result.getMessage().contains("Generated")) {
                ViewUtil.info(this, result.getMessage());
            }
        }
        refreshData();
    }

    private void markAllRead() {
        ActionResult result = controller.markAllRead();
        if (!result.isSuccess()) {
            ViewUtil.error(this, result.getMessage());
        }
        refreshData();
    }

    private void markSelectedRead() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a notification first");
            return;
        }
        long id = (Long) tableModel.getValueAt(row, 0);
        ActionResult result = controller.markRead(id);
        if (!result.isSuccess()) {
            ViewUtil.error(this, result.getMessage());
        }
        refreshData();
    }

    private void archiveSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a notification first");
            return;
        }
        long id = (Long) tableModel.getValueAt(row, 0);
        ActionResult result = controller.archive(id);
        if (!result.isSuccess()) {
            ViewUtil.error(this, result.getMessage());
        }
        refreshData();
    }

    private void viewDetails() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ViewUtil.error(this, "Select a notification first");
            return;
        }
        long id = (Long) tableModel.getValueAt(row, 0);
        Notification n;
        try {
            n = controller.get(id);
        } catch (com.resqhub.exception.ResQHubException e) {
            ViewUtil.error(this, e.getMessage());
            return;
        }
        JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Notification Details",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setModal(true);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, gbc, 0, "ID", String.valueOf(n.getId()));
        addRow(form, gbc, 1, "Priority",
                n.getPriority() == null ? "-" : n.getPriority().getLabel());
        addRow(form, gbc, 2, "Type",
                n.getType() == null ? "-" : n.getType().getLabel());
        addRow(form, gbc, 3, "Module",
                n.getRelatedModule() == null ? "-" : n.getRelatedModule());
        addRow(form, gbc, 4, "Status",
                n.getStatus() == null ? "-" : n.getStatus().getLabel());

        JLabel msgLabel = new JLabel("Message:");
        gbc.gridx = 0; gbc.gridy = 5;
        form.add(msgLabel, gbc);
        JTextArea msg = new JTextArea(4, 34);
        msg.setText(n.getMessage());
        msg.setEditable(false);
        msg.setLineWrap(true);
        msg.setWrapStyleWord(true);
        gbc.gridx = 1;
        form.add(new JScrollPane(msg), gbc);

        JButton close = new JButton("Close");
        gbc.gridy = 6; gbc.gridx = 1;
        form.add(close, gbc);
        close.addActionListener(e -> dialog.dispose());

        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addRow(JPanel form, GridBagConstraints gbc,
                        int y, String label, String value) {
        gbc.gridx = 0; gbc.gridy = y;
        form.add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        form.add(new JLabel(value), gbc);
    }

    private void broadcastDialog() {
        final JDialog dialog = new JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Broadcast Alert",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setModal(true);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<NotificationPriority> priorityCombo2 = new JComboBox<>(
                NotificationPriority.values());
        JComboBox<NotificationType> typeCombo2 = new JComboBox<>(
                NotificationType.values());
        JComboBox<String> audienceCombo = new JComboBox<>(
                new String[]{"All Users", "Admins",
                        "Rescue Officers", "Camp Managers",
                        "Volunteers", "Blood Coordinators"});
        JTextField moduleField = new JTextField(20);
        JTextArea messageArea = new JTextArea(4, 34);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Priority:"), gbc);
        gbc.gridx = 1; form.add(priorityCombo2, gbc);
        gbc.gridy = 1; gbc.gridx = 0;
        form.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1; form.add(typeCombo2, gbc);
        gbc.gridy = 2; gbc.gridx = 0;
        form.add(new JLabel("Audience:"), gbc);
        gbc.gridx = 1; form.add(audienceCombo, gbc);
        gbc.gridy = 3; gbc.gridx = 0;
        form.add(new JLabel("Module:"), gbc);
        gbc.gridx = 1; form.add(moduleField, gbc);
        gbc.gridy = 4; gbc.gridx = 0;
        form.add(new JLabel("Message:"), gbc);
        gbc.gridx = 1;
        form.add(new JScrollPane(messageArea), gbc);

        JButton sendButton = new JButton("Send");
        gbc.gridy = 5; gbc.gridx = 0;
        form.add(sendButton, gbc);
        JButton cancelButton = new JButton("Cancel");
        gbc.gridx = 1;
        form.add(cancelButton, gbc);
        cancelButton.addActionListener(e -> dialog.dispose());

        sendButton.addActionListener(e -> {
            RoleType[] roles = switch (audienceCombo.getSelectedIndex()) {
                case 1 -> new RoleType[]{RoleType.ADMIN};
                case 2 -> new RoleType[]{RoleType.RESCUE_OFFICER};
                case 3 -> new RoleType[]{RoleType.CAMP_MANAGER};
                case 4 -> new RoleType[]{RoleType.VOLUNTEER};
                case 5 -> new RoleType[]{RoleType.BLOOD_COORDINATOR};
                default -> new RoleType[]{};
            };
            ActionResult result = controller.broadcast(
                    (NotificationType) typeCombo2.getSelectedItem(),
                    (NotificationPriority) priorityCombo2.getSelectedItem(),
                    messageArea.getText(),
                    moduleField.getText().trim(), roles);
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

    @Override
    public void refreshData() {
        // Evaluate business rules so current conditions surface as alerts.
        controller.generateAutomaticAlerts();
        refreshTable();
    }

    private void refreshTable() {
        NotificationPriority priority = switch (priorityCombo
                .getSelectedIndex()) {
            case 1 -> NotificationPriority.CRITICAL;
            case 2 -> NotificationPriority.WARNING;
            case 3 -> NotificationPriority.INFO;
            default -> null;
        };
        NotificationType type = switch (typeCombo.getSelectedIndex()) {
            case 1 -> NotificationType.CRITICAL_RESCUE;
            case 2 -> NotificationType.LOW_STOCK;
            case 3 -> NotificationType.ASSIGNMENT;
            case 4 -> NotificationType.SYSTEM;
            default -> null;
        };
        NotificationStatus status = switch (statusCombo.getSelectedIndex()) {
            case 1 -> NotificationStatus.UNREAD;
            case 2 -> NotificationStatus.READ;
            case 3 -> NotificationStatus.ARCHIVED;
            default -> null;
        };

        List<Notification> notifications;
        try {
            notifications = controller.filterMine(type, priority, status);
            int unread = controller.countUnread();
            unreadTile.setText(String.valueOf(unread));
        } catch (DataAccessException e) {
            ViewUtil.error(this, e.getMessage());
            return;
        }

        tableModel.setRowCount(0);
        int critical = 0;
        int warning = 0;
        for (Notification n : notifications) {
            tableModel.addRow(NotificationController.toRow(n));
            if (n.getPriority() == NotificationPriority.CRITICAL) {
                critical++;
            } else if (n.getPriority() == NotificationPriority.WARNING) {
                warning++;
            }
        }
        criticalTile.setText(String.valueOf(critical));
        warningTile.setText(String.valueOf(warning));
        totalTile.setText(String.valueOf(notifications.size()));
    }
}
