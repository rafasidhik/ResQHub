package com.resqhub.view;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;

/**
 * Login screen.
 *
 * EVENT HANDLING (Delegation Event Model):
 *  - Event source : loginButton / window
 *  - Event object : ActionEvent / WindowEvent fired by the source
 *  - Listener     : the lambda ActionListener and WindowAdapter below are
 *                   notified by Swing when the user acts on the source.
 */
public class LoginView extends JFrame {

    private final AuthController authController = new AuthController();
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JLabel messageLabel = new JLabel(" ");

    public LoginView(String title) {
        super(title);
        buildUi();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 32, 24, 32));
        setContentPane(root);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("ResQHub Emergency Coordination", javax.swing.SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        root.add(header, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        root.add(new JLabel("Username / Email:"), gbc);
        gbc.gridx = 1;
        root.add(usernameField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        root.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        root.add(passwordField, gbc);

        JButton loginButton = new JButton("Login");
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        root.add(loginButton, gbc);
        getRootPane().setDefaultButton(loginButton);

        messageLabel.setForeground(java.awt.Color.RED);
        gbc.gridy = 4;
        root.add(messageLabel, gbc);

        // Event source: button. Listener: this lambda (ActionListener).
        loginButton.addActionListener(event -> attemptLogin());

        JButton signupButton = new JButton("New citizen? Create an account");
        gbc.gridy = 5;
        root.add(signupButton, gbc);
        signupButton.addActionListener(event -> {
            SignupView.launch("ResQHub - Create Citizen Account");
            dispose();
        });

        pack();
        // Full-screen presentation: maximise and centre the form.
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void attemptLogin() {
        ActionResult result = authController.login(
                usernameField.getText(), new String(passwordField.getPassword()));

        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            new MainDashboard().setVisible(true);
            dispose();   // login window is done; dashboard takes over
        } else {
            messageLabel.setText(result.getMessage());
        }
    }

    /** Entry helper used by ResQHubApplication to start the GUI safely on the EDT. */
    public static void launch(final String title) {
        SwingUtilities.invokeLater(() -> {
            LoginView view = new LoginView(title);
            view.setVisible(true);
            view.setExtendedState(JFrame.MAXIMIZED_BOTH);
        });
    }
}
