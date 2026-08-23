package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.service.SessionManager;

/**
 * Role-aware main window. Module panels are hosted in a CardLayout so
 * only one module is visible at a time (CARDLAYOUT demonstration).
 */
public class MainDashboard extends JFrame {

    private final AuthController authController = new AuthController();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final JLabel statusLabel = new JLabel();

    private final User currentUser = SessionManager.getInstance().getCurrentUser();

    public MainDashboard() {
        super("ResQHub - Operations Dashboard");
        buildMenuBar();
        buildHeader();

        add(cardHost, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                confirmExit();
            }
        });

        setSize(1024, 680);
        setLocationRelativeTo(null);
    }

    private boolean has(RoleType... roles) {
        return authController.hasRole(roles);
    }

    /** Opens a module lazily; the panel class is only loaded on first use. */
    private void openModule(String cardName, JPanel modulePanel) {
        boolean added = false;
        for (java.awt.Component component : cardHost.getComponents()) {
            if (cardName.equals(component.getName())) {
                added = true;
            }
        }
        if (!added) {
            modulePanel.setName(cardName);
            cardHost.add(modulePanel, cardName);
        }
        cards.show(cardHost, cardName);
        statusLabel.setText(" Active module: " + cardName
                + "   |   Logged in: " + currentUser.getUsername()
                + " (" + currentUser.getRole().getLabel() + ")");
    }

    private void buildHeader() {
        JLabel welcome = new JLabel("ResQHub - Integrated Disaster Response Coordination");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 16f));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(welcome);
        add(north, BorderLayout.NORTH);
    }

    private void buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu modulesMenu = new JMenu("Modules");

        if (has(RoleType.ADMIN, RoleType.RESCUE_OFFICER)) {
            modulesMenu.add(item("Disasters", () ->
                    openModule("disasters", new DisasterPanel())));
            modulesMenu.add(item("Victims", () ->
                    openModule("victims", new VictimPanel(true))));
            modulesMenu.add(item("Rescue Requests", () ->
                    openModule("requests", new RescueRequestPanel(true))));
            modulesMenu.add(item("Rescue Teams", () ->
                    openModule("teams", new RescueTeamPanel())));
            if (has(RoleType.ADMIN)) {
                modulesMenu.add(item("Users", () ->
                        openModule("users", new UserPanel())));
            }
        } else if (has(RoleType.CAMP_MANAGER)) {
            modulesMenu.add(item("Victims", () ->
                    openModule("victims", new VictimPanel(true))));
            modulesMenu.add(item("Rescue Requests", () ->
                    openModule("requests", new RescueRequestPanel(true))));
        } else {
            // CITIZEN / VOLUNTEER / MEDICAL_OFFICER / BLOOD_COORDINATOR:
            // submission-only view of rescue requests
            modulesMenu.add(item("Report Emergency", () ->
                    openModule("requests", new RescueRequestPanel(false))));
        }

        JMenu accountMenu = new JMenu("Account");
        accountMenu.add(item("Change Password...", this::showChangePasswordDialog));
        accountMenu.add(item("Logout", this::logout));

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(item("About ResQHub", this::showAbout));

        menuBar.add(modulesMenu);
        menuBar.add(accountMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private JMenuItem item(String label, Runnable action) {
        JMenuItem menuItem = new JMenuItem(label);
        menuItem.addActionListener(event -> action.run());
        return menuItem;
    }

    private void logout() {
        authController.logout();
        LoginView.launch("ResQHub - Login");
        dispose();
    }

    private void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Exit ResQHub?", "Confirm exit",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    /** JDialog example: modal password change form. */
    private void showChangePasswordDialog() {
        final JDialog dialog = new JDialog(this, "Change Password", true);
        JPanel form = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(6, 6, 6, 6);
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;

        final JPasswordField oldField = new JPasswordField(16);
        final JPasswordField newField = new JPasswordField(16);
        final JPasswordField repeatField = new JPasswordField(16);

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Current password:"), gbc);
        gbc.gridx = 1; form.add(oldField, gbc);
        gbc.gridy = 1; gbc.gridx = 0; form.add(new JLabel("New password:"), gbc);
        gbc.gridx = 1; form.add(newField, gbc);
        gbc.gridy = 2; gbc.gridx = 0; form.add(new JLabel("Repeat new:"), gbc);
        gbc.gridx = 1; form.add(repeatField, gbc);

        JButton saveButton = new JButton("Save");
        gbc.gridy = 3; gbc.gridx = 1; form.add(saveButton, gbc);
        dialog.setContentPane(form);

        saveButton.addActionListener(event -> {
            String newPassword = new String(newField.getPassword());
            if (!newPassword.equals(new String(repeatField.getPassword()))) {
                ViewUtil.error(dialog, "New passwords do not match");
                return;
            }
            ActionResult result = authController.changeOwnPassword(
                    new String(oldField.getPassword()), newPassword);
            if (result.isSuccess()) {
                ViewUtil.info(dialog, result.getMessage());
                dialog.dispose();
            } else {
                ViewUtil.error(dialog, result.getMessage());
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showAbout() {
        ViewUtil.info(this,
                "ResQHub v" + com.resqhub.main.ResQHubApplication.APP_VERSION + "\n"
                + "Integrated Disaster Response Coordination System\n"
                + "Core & Emergency Operations: Rafa | Shelters: Ameya | "
                + "Medical: Malavika | Volunteers: Stina");
    }
}
