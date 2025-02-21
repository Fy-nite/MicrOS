package org.Finite.MicrOS.ui;

import javax.swing.*;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.apps.AppManifest;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorDialog {
    private static final Color ERROR_COLOR = new Color(220, 53, 69);
    private static final Color BG_COLOR = new Color(248, 249, 250);
    private static final Color TEXT_COLOR = new Color(33, 37, 41);
    
    public static void showError(JDesktopPane desktop, String message, Throwable throwable) {
        showError(desktop, message, throwable, null);
    }

    public static void showError(JDesktopPane desktop, String message, Throwable throwable, MicrOSApp app) {
        JInternalFrame errorFrame = createErrorFrame(message, throwable, app);
        desktop.add(errorFrame);
        
        // Center the frame
        Dimension desktopSize = desktop.getSize();
        Dimension frameSize = errorFrame.getSize();
        errorFrame.setLocation(
            (desktopSize.width - frameSize.width) / 2,
            (desktopSize.height - frameSize.height) / 2
        );
        
        errorFrame.setVisible(true);
        try {
            errorFrame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    private static JInternalFrame createErrorFrame(String message, Throwable throwable, MicrOSApp app) {
        // Create stack trace text
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header panel with error icon and message
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        
        // Error icon
        JLabel iconLabel = new JLabel("⚠️");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setForeground(ERROR_COLOR);
        headerPanel.add(iconLabel, BorderLayout.WEST);

        // Error message and app info
        JPanel messagePanel = new JPanel(new GridLayout(0, 1, 5, 5));
        messagePanel.setOpaque(false);
        
        JLabel errorLabel = new JLabel("Error");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        errorLabel.setForeground(ERROR_COLOR);
        messagePanel.add(errorLabel);

        JLabel messageLabel = new JLabel("<html><body style='width: 300px'>" + message + "</body></html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setForeground(TEXT_COLOR);
        messagePanel.add(messageLabel);

        // Add app info if available
        if (app != null && app.getManifest() != null) {
            AppManifest manifest = app.getManifest();
            JLabel appLabel = new JLabel(String.format("Application: %s (v%s)", 
                manifest.getName(), manifest.getVersion()));
            appLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            appLabel.setForeground(new Color(108, 117, 125));
            messagePanel.add(appLabel);
        }

        headerPanel.add(messagePanel, BorderLayout.CENTER);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Stack trace in scrollable text area
        JTextArea textArea = new JTextArea(stackTrace);
        textArea.setFont(new Font("JetBrainsMono", Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setBackground(new Color(233, 236, 239));
        textArea.setForeground(TEXT_COLOR);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        if (app != null) {
            JButton restartButton = new JButton("Restart App");
            restartButton.addActionListener(e -> {
                Component source = (Component) e.getSource();
                JInternalFrame frame = (JInternalFrame) SwingUtilities.getAncestorOfClass(
                    JInternalFrame.class, source);
                if (frame != null) {
                    frame.dispose();
                }
                app.onStop();
                app.onStart();
            });
            buttonPanel.add(restartButton);
        }

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            Component source = (Component) e.getSource();
            JInternalFrame frame = (JInternalFrame) SwingUtilities.getAncestorOfClass(
                JInternalFrame.class, source);
            if (frame != null) {
                frame.dispose();
            }
        });
        buttonPanel.add(closeButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Create and setup frame
        JInternalFrame errorFrame = new JInternalFrame("Error", true, true, true, true);
        errorFrame.setContentPane(mainPanel);
        errorFrame.pack();
        
        return errorFrame;
    }
}
