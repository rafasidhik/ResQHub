package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import com.resqhub.controller.AccountDeletionRequestController;
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
    private final java.util.function.Consumer<String> moduleOpener;

    private final JPanel cardsRowTop = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 16, 8));
    private final JPanel cardsRowBottom = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 16, 8));
    private final JTextArea attentionArea = new JTextArea(6, 30);
    private final JPanel actionsPanel = new JPanel(new GridLayout(0, 1, 6, 6));
    private final JLabel updatedLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();

    public StatsPanel(java.util.function.Consumer<String> moduleOpener) {
        this.moduleOpener = moduleOpener;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel headline =
                new JLabel("LIVE SITUATION OVERVIEW");
        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 17f));
        updatedLabel.setFont(updatedLabel.getFont().deriveFont(12f));
        updatedLabel.setForeground(new Color(90, 90, 90));
        JPanel header = new JPanel(new BorderLayout());
        header.add(headline, BorderLayout.WEST);
        header.add(updatedLabel, BorderLayout.EAST);

        JPanel cardArea = new JPanel(new BorderLayout(0, 4));
        cardArea.add(cardsRowTop, BorderLayout.NORTH);
        cardArea.add(cardsRowBottom, BorderLayout.CENTER);

        attentionArea.setEditable(false);
        attentionArea.setFont(attentionArea.getFont().deriveFont(13f));
        JPanel attention = new JPanel(new BorderLayout());
        attention.setBorder(BorderFactory.createTitledBorder(
                "ATTENTION REQUIRED"));
        attention.add(attentionArea, BorderLayout.CENTER);

        JButton reportButton = new JButton("Report Emergency");
        JButton requestsButton = new JButton("View Rescue Requests");
        JButton victimsButton = new JButton("Register Victim");
        JButton teamsButton = new JButton("Manage Rescue Teams");
        JButton deletionButton = new JButton("Review Deletion Requests");
        reportButton.addActionListener(e -> moduleOpener.accept("requests"));
        requestsButton.addActionListener(e -> moduleOpener.accept("requests"));
        victimsButton.addActionListener(e -> moduleOpener.accept("victims"));
        teamsButton.addActionListener(e -> moduleOpener.accept("teams"));
        deletionButton.addActionListener(e -> moduleOpener.accept("users"));
        List<JButton> buttonList = new ArrayList<>();
        buttonList.add(reportButton);
        buttonList.add(requestsButton);
        buttonList.add(victimsButton);
        buttonList.add(teamsButton);
        if (SessionManager.getInstance().hasRole(RoleType.ADMIN)) {
            buttonList.add(deletionButton);
        }
        for (JButton button : buttonList) {
            actionsPanel.add(button);
        }
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

        cardsRowTop.removeAll();
        cardsRowTop.add(statCard("ACTIVE DISASTERS",
                String.valueOf(s.activeDisasters),
                "of " + s.totalDisasters + " total"));
        cardsRowTop.add(statCard("CRITICAL VICTIMS",
                String.valueOf(s.criticalVictims),
                "of " + s.totalVictims + " registered"));
        cardsRowTop.add(statCard("PENDING REQUESTS",
                String.valueOf(s.pendingRequests),
                s.pendingRequests == 0
                        ? "No action now" : "Awaiting assignment"));

        cardsRowBottom.removeAll();
        cardsRowBottom.add(statCard("TEAMS AVAILABLE",
                String.valueOf(s.availableTeams),
                s.availableTeams == 0
                        ? "None free right now" : "Ready for rescue"));
        cardsRowBottom.add(statCard("TEAMS DEPLOYED",
                String.valueOf(s.deployedTeams),
                "of " + s.totalTeams + " total"));
        cardsRowTop.revalidate();
        cardsRowTop.repaint();
        cardsRowBottom.revalidate();
        cardsRowBottom.repaint();

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
                && s.deployedTeams == 0) {
            alerts.set(0, "All clear - no alerts");
        }
        attentionArea.setText(String.join("\n\n", alerts));
    }

    /** One bordered number tile used across the overview rows. */
    private JPanel statCard(String title, String value, String subtitle) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(230, 130));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setAlignmentX(CENTER_ALIGNMENT);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 34f));
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
}
