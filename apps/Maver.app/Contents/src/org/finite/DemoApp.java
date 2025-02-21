package org.finite;

import javax.swing.*;
import java.awt.*;
import org.Finite.MicrOS.apps.MicrOSApp;

public class DemoApp extends MicrOSApp {
    private JLabel messageLabel;
    private JButton actionButton;
    private int clickCount = 0;

    @Override
    public JComponent createUI() {
        // Create a simple panel with a label and button
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        messageLabel = new JLabel("Welcome to the Demo App!", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Dialog", Font.PLAIN, 16));
        
        actionButton = new JButton("Click Me!");
        actionButton.addActionListener(e -> {
            clickCount++;
            messageLabel.setText("Button clicked " + clickCount + " times!");
        });

        panel.add(messageLabel, BorderLayout.CENTER);
        panel.add(actionButton, BorderLayout.SOUTH);
        
        return panel;
    }

    @Override
    public void onStart() {
        // Initialize any resources needed by the app
        System.out.println("Demo app started");
    }

    @Override
    public void onStop() {
        // Cleanup when app is closed
        System.out.println("Demo app stopped");
    }
}
