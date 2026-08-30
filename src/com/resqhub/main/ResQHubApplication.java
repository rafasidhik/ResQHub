package com.resqhub.main;

import com.resqhub.view.LoginView;

/**
 * Application entry point.
 *
 * COMMAND LINE ARGUMENTS (startup configuration):
 *   --title=<text>  overrides the main window title
 * Example:
 *   run.bat --title=ResQHub District Control Room
 */
public class ResQHubApplication {

    public static final String APP_VERSION = "1.0";

    public static void main(String[] args) {
        String title = "ResQHub - Integrated Disaster Response "
                + "Coordination System v" + APP_VERSION;
        for (String arg : args) {
            if (arg.startsWith("--title=")) {
                title = arg.substring("--title=".length());
            }
        }

        // Swing components must be created on the Event Dispatch Thread
        final String windowTitle = title;
        javax.swing.SwingUtilities.invokeLater(
                () -> new LoginView(windowTitle).setVisible(true));
    }
}
