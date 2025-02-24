package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JPanel {
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JLabel logoLabel;
    private final Color BG_COLOR = new Color(25, 25, 25);
    private final Color TEXT_COLOR = new Color(220, 220, 220);
    private JFrame splashFrame;
    private final JFrame mainWindow;
    
    public SplashScreen(JFrame mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(10, 10));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 40, 40), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Logo/title panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        logoLabel = new JLabel("MicrOS", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        logoLabel.setForeground(TEXT_COLOR);
        headerPanel.add(logoLabel, BorderLayout.CENTER);
        
        // Version label
        JLabel versionLabel = new JLabel("v1.0.0", SwingConstants.CENTER);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(180, 180, 180));
        headerPanel.add(versionLabel, BorderLayout.SOUTH);
        
        add(headerPanel, BorderLayout.CENTER);
        
        // Bottom panel for progress
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setOpaque(false);
        
        statusLabel = new JLabel("Starting...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_COLOR);
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(300, 3));
        progressBar.setForeground(new Color(0, 150, 136));
        progressBar.setBackground(new Color(40, 40, 40));
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        setPreferredSize(new Dimension(400, 200));
        createWindow();
    }
    
    public void setStatus(String status) {
        if (SwingUtilities.isEventDispatchThread()) {
            statusLabel.setText(status);
        } else {
            SwingUtilities.invokeLater(() -> statusLabel.setText(status));
        }
    }

    public void setShutdownMode() {
        logoLabel.setText("Shutting Down");
        logoLabel.setForeground(new Color(255, 100, 100)); // Reddish color
        statusLabel.setForeground(new Color(255, 200, 200));
        progressBar.setForeground(new Color(255, 100, 100));
        setStatus("Please wait...");
    }

    public void disposeSplash() {
        if (splashFrame != null) {
            splashFrame.setVisible(false);
            splashFrame.dispose();
            splashFrame = null;
        }
    }

    private void createWindow() {
        splashFrame = new JFrame();
        splashFrame.setUndecorated(true);
        splashFrame.add(this);
        splashFrame.pack();
        splashFrame.setLocationRelativeTo(mainWindow); // Position relative to main window
    }

    public void show() {
        if (splashFrame != null) {
            splashFrame.setVisible(true);
            splashFrame.toFront();
        }
    }
}
