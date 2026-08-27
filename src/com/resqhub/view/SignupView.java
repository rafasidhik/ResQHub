package com.resqhub.view;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;

/**
 * Citizen self-signup screen. New accounts are always CITIZEN role;
 * staff accounts can only be created by an administrator.
 */
public class SignupView extends JFrame {

    private final AuthController authController = new AuthController();
    private final JTextField fullNameField = new JTextField(16);
    private final JTextField emailField = new JTextField(16);
    private final JTextField phoneField = new JTextField(16);
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JPasswordField confirmField = new JPasswordField(16);
    private final JLabel messageLabel = new JLabel(" ");

    public SignupView(String title) {
        super(title);
        buildUi();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void buildUi() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 28, 20, 28));
        setContentPane(root);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("Create Citizen Account", javax.swing.SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        root.add(header, gbc);
        gbc.gridwidth = 1;

        addRow(root, gbc, 1, "Full name:", fullNameField);
        addRow(root, gbc, 2, "Email:", emailField);
        addRow(root, gbc, 3, "Phone (optional):", phoneField);
        addRow(root, gbc, 4, "Username:", usernameField);
        addRow(root, gbc, 5, "Password:", passwordField);
        addRow(root, gbc, 6, "Confirm password:", confirmField);

        JLabel hint = new JLabel("At least 8 characters with letters and digits");
        hint.setFont(hint.getFont().deriveFont(11f));
        hint.setForeground(java.awt.Color.GRAY);
        gbc.gridx = 1; gbc.gridy = 7;
        root.add(hint, gbc);

        JButton createButton = new JButton("Create account");
        JButton backButton = new JButton("Back to login");
        JPanel buttons = new JPanel();
        buttons.add(createButton);
        buttons.add(backButton);
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        root.add(buttons, gbc);
        getRootPane().setDefaultButton(createButton);

        messageLabel.setForeground(java.awt.Color.RED);
        gbc.gridy = 9;
        root.add(messageLabel, gbc);

        createButton.addActionListener(event -> createAccount());
        backButton.addActionListener(event -> returnToLogin());

        pack();
        // Full-screen presentation: maximise and centre the form.
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void addRow(JPanel root, GridBagConstraints gbc, int row,
                        String label, javax.swing.JComponent field) {
        gbc.gridx = 0; gbc.gridy = row;
        root.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        root.add(field, gbc);
    }

    private void createAccount() {
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());
        if (!password.equals(confirm)) {
            messageLabel.setText("Passwords do not match");
            return;
        }

        ActionResult result = authController.registerCitizen(
                usernameField.getText(), password,
                fullNameField.getText(), emailField.getText(),
                phoneField.getText());

        if (result.isSuccess()) {
            ViewUtil.info(this, result.getMessage());
            returnToLogin();
        } else {
            messageLabel.setText(result.getMessage());
        }
    }

    private void returnToLogin() {
        LoginView.launch("ResQHub - Integrated Disaster Response "
                + "Coordination System");
        dispose();
    }

    /** Entry helper used by LoginView to open this screen on the EDT. */
    public static void launch(final String title) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            SignupView view = new SignupView(title);
            view.setVisible(true);
            view.setExtendedState(JFrame.MAXIMIZED_BOTH);
        });
    }
}
