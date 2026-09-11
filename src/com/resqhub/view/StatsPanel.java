package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.StatsController;
import com.resqhub.model.RoleType;
import com.resqhub.service.SessionManager;

/**
 * Landing overview for staff logins: stat cards, attention list,
 * quick actions and a system status bar (no separate menu entry).
 */
public class StatsPanel extends JPanel implements Refreshable {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final StatsController controller = new StatsController();

    private final JPanel cardGrid = new JPanel(
            new GridLayout(0, 4, 12, 12));
    private final JTextArea attentionArea = new JTextArea(6, 30);
    private final JPanel actionsPanel = new JPanel(new GridLayout(0, 1, 6, 6));
    private final JLabel updatedLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JLabel greetingLabel = new JLabel();
    private final JLabel roleLabel = new JLabel();

    public StatsPanel(java.util.function.Consumer<String> moduleOpener) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        com.resqhub.model.User me =
                SessionManager.getInstance().getCurrentUser();
        greetingLabel.setText("Welcome, " + me.getUsername());
        greetingLabel.setFont(greetingLabel.getFont().deriveFont(Font.BOLD, 19f));
        roleLabel.setText(me.getRole().getLabel()
                + "   •   ResQHub Integrated Disaster Response Coordination");
        roleLabel.setForeground(new Color(90, 90, 90));
        JPanel greetingStack = new JPanel();
        greetingStack.setLayout(new BoxLayout(greetingStack, BoxLayout.Y_AXIS));
        greetingStack.add(greetingLabel);
        greetingStack.add(roleLabel);
        greetingStack.setAlignmentY(CENTER_ALIGNMENT);
        updatedLabel.setFont(updatedLabel.getFont().deriveFont(12f));
        updatedLabel.setForeground(new Color(90, 90, 90));
        JPanel header = new JPanel(new BorderLayout());
        header.add(greetingStack, BorderLayout.WEST);
        header.add(updatedLabel, BorderLayout.EAST);

        JPanel cardArea = new JPanel(new BorderLayout());
        cardArea.add(cardGrid, BorderLayout.NORTH);

        attentionArea.setEditable(false);
        attentionArea.setFont(attentionArea.getFont().deriveFont(13f));
        attentionArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JPanel attention = new JPanel(new BorderLayout());
        attention.setBorder(BorderFactory.createTitledBorder(
                "ATTENTION REQUIRED"));
        attention.add(attentionArea, BorderLayout.CENTER);

        buildQuickActions(moduleOpener);
        JPanel actions = new JPanel(new BorderLayout());
        actions.setBorder(BorderFactory.createTitledBorder("QUICK ACTIONS"));
        actions.add(actionsPanel, BorderLayout.NORTH);

        JPanel lower = new JPanel(new GridLayout(1, 2, 12, 0));
        lower.add(attention);
        lower.add(actions);

        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        statusLabel.setForeground(new Color(40, 110, 40));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createTitledBorder("SYSTEM STATUS"));
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(refreshButton, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(cardArea, BorderLayout.CENTER);

        JPanel southStack = new JPanel();
        southStack.setLayout(new BoxLayout(southStack, BoxLayout.Y_AXIS));
        southStack.add(lower);
        southStack.add(Box.createVerticalStrut(10));
        southStack.add(statusBar);
        add(southStack, BorderLayout.SOUTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        ActionResult result = controller.getSnapshot();
        if (!result.isSuccess()) {
            statusLabel.setText("Database problem: " + result.getMessage());
            statusLabel.setForeground(new Color(150, 30, 30));
            attentionArea.setText(result.getMessage());
            return;
        }
        statusLabel.setForeground(new Color(40, 110, 40));
        statusLabel.setText("ResQHub database connected  \u2022  "
                + "Emergency operations system active");

        StatsController.Snapshot s = (StatsController.Snapshot) result.getData();
        updatedLabel.setText("Last updated: "
                + LocalDateTime.now().format(STAMP));

        cardGrid.removeAll();
        cardGrid.add(statCard("ACTIVE DISASTERS",
                String.valueOf(s.activeDisasters),
                "of " + s.totalDisasters + " total", true));
        cardGrid.add(statCard("CRITICAL VICTIMS",
                String.valueOf(s.criticalVictims),
                "of " + s.totalVictims + " registered", true));
        cardGrid.add(statCard("PENDING REQUESTS",
                String.valueOf(s.pendingRequests),
                s.pendingRequests == 0
                        ? "No action now" : "Awaiting assignment", true));
        cardGrid.add(statCard("TEAMS AVAILABLE",
                String.valueOf(s.availableTeams),
                s.availableTeams == 0
                        ? "None free right now" : "Ready for rescue", false));
        cardGrid.add(statCard("SHELTERS ACCEPTING",
                String.valueOf(s.sheltersAccepting),
                "of " + s.totalShelters + " shelters", false));
        cardGrid.add(statCard("HOSPITALS ACCEPTING",
                String.valueOf(s.hospitalsAccepting),
                "of " + s.totalHospitals + " hospitals", false));
        cardGrid.add(statCard("VOLUNTEERS AVAILABLE",
                String.valueOf(s.volunteersAvailable),
                "of " + s.totalVolunteers + " volunteers", false));
        cardGrid.add(statCard("ELIGIBLE DONORS",
                String.valueOf(s.eligibleDonors),
                "ready to donate blood", false));
        cardGrid.revalidate();
        cardGrid.repaint();

        List<String> alerts = new ArrayList<>();
        if (s.criticalRequests > 0) {
            alerts.add("\u26A0 " + s.criticalRequests
                    + " CRITICAL rescue request"
                    + (s.criticalRequests == 1 ? "" : "s")
                    + " need immediate attention!");
        }
        if (s.criticalVictims > 0) {
            alerts.add("\u2022 " + s.criticalVictims + " Critical victim"
                    + (s.criticalVictims == 1 ? "" : "s"));
        }
        if (s.activeDisasters > 0) {
            alerts.add("\u2022 " + s.activeDisasters + " Active disaster"
                    + (s.activeDisasters == 1 ? "" : "s"));
        }
        if (s.deployedTeams > 0) {
            alerts.add("\u2022 " + s.deployedTeams + " Team currently deployed");
        }
        if (s.pendingRequests > 0) {
            alerts.add("\u2022 " + s.pendingRequests
                    + " Pending rescue request"
                    + (s.pendingRequests == 1 ? "" : "s"));
        } else {
            alerts.add("\u2022 No pending rescue requests");
        }
        if (s.sheltersNearCapacity > 0) {
            alerts.add("\u2022 " + s.sheltersNearCapacity + " shelter"
                    + (s.sheltersNearCapacity == 1 ? "" : "s")
                    + " near capacity");
        }
        if (s.openReferrals > 0) {
            alerts.add("\u2022 " + s.openReferrals + " open hospital referral"
                    + (s.openReferrals == 1 ? "" : "s"));
        }
        if (s.foodShortages > 0) {
            alerts.add("\u26A0 " + s.foodShortages + " food shortage alert"
                    + (s.foodShortages == 1 ? "" : "s")
                    + " in the distribution module");
        }
        if (s.bloodShortages > 0) {
            alerts.add("\u26A0 " + s.bloodShortages + " blood shortage alert"
                    + (s.bloodShortages == 1 ? "" : "s")
                    + " in the donor module");
        }
        if (s.resourcesLow > 0 || s.resourcesOut > 0) {
            alerts.add("\u2022 Resource stock low: " + s.resourcesLow
                    + " below reorder, " + s.resourcesOut + " out of stock");
        }
        if (s.pendingDeletions > 0) {
            alerts.add("\u2022 " + s.pendingDeletions
                    + " Account deletion request"
                    + (s.pendingDeletions == 1 ? "" : "s")
                    + " awaiting review");
        }
        try {
            int unread = new com.resqhub.controller.NotificationController()
                    .countUnread();
            if (unread > 0) {
                alerts.add("\u2022 " + unread + " unread notification"
                        + (unread == 1 ? "" : "s")
                        + " - check the Notification Center");
            }
        } catch (Exception ignored) {
            // notifications are additive; a reporting failure must not
            // break the overview
        }
        if (alerts.size() == 1 && s.pendingRequests == 0
                && s.criticalVictims == 0 && s.activeDisasters == 0
                && s.deployedTeams == 0 && s.sheltersNearCapacity == 0
                && s.openReferrals == 0 && s.foodShortages == 0
                && s.bloodShortages == 0 && s.resourcesLow == 0
                && s.resourcesOut == 0 && s.pendingDeletions == 0) {
            alerts.set(0, "All clear - no alerts");
        }
        attentionArea.setText(String.join("\n\n", alerts));
    }

    /** One bordered number tile used across the overview grid. */
    private JPanel statCard(String title, String value, String subtitle,
                            boolean alarm) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(230, 120));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setAlignmentX(CENTER_ALIGNMENT);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 32f));
        valueLabel.setForeground(alarm && !"0".equals(value)
                ? new Color(190, 30, 30) : new Color(20, 70, 120));
        JLabel subLabel = new JLabel(subtitle);
        subLabel.setAlignmentX(CENTER_ALIGNMENT);
        subLabel.setFont(subLabel.getFont().deriveFont(12f));
        subLabel.setForeground(new Color(90, 90, 90));

        card.add(Box.createVerticalGlue());
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(subLabel);
        card.add(Box.createVerticalGlue());
        return card;
    }

    /** Role-appropriate shortcuts into the modules behind the overview. */
    private void buildQuickActions(java.util.function.Consumer<String> opener) {
        List<String[]> actions = new ArrayList<>();
        SessionManager session = SessionManager.getInstance();
        if (session.hasRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER)) {
            actions.add(new String[]{"Report Emergency", "requests"});
            actions.add(new String[]{"View Rescue Requests", "requests"});
            actions.add(new String[]{"Register Victim", "victims"});
            actions.add(new String[]{"Manage Rescue Teams", "teams"});
            actions.add(new String[]{"Open Shelters", "shelters"});
            actions.add(new String[]{"Blood Donors", "blood"});
            actions.add(new String[]{"Hospitals", "hospitals"});
        } else if (session.hasRole(RoleType.CAMP_MANAGER)) {
            actions.add(new String[]{"Manage Shelters", "shelters"});
            actions.add(new String[]{"Food Distribution", "food"});
            actions.add(new String[]{"Hospitals", "hospitals"});
            actions.add(new String[]{"Resources & Inventory", "resources"});
            actions.add(new String[]{"View Rescue Requests", "requests"});
        }
        if (session.hasRole(RoleType.ADMIN)) {
            actions.add(new String[]{"Review Deletion Requests", "users"});
        }
        for (String[] action : actions) {
            JButton button = new JButton(action[0]);
            button.addActionListener(e -> opener.accept(action[1]));
            actionsPanel.add(button);
        }
    }
}
