package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JPanel {
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JLabel logoLabel;
    private final Color BG_COLOR = new Color(25, 25, 25);
    private final Color TEXT_COLOR = new Color(220, 220, 220);
    private JInternalFrame splashFrame;
    private JDesktopPane desktop;
    
    public SplashScreen() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Logo/title
        logoLabel = new JLabel("MicrOS", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        logoLabel.setForeground(TEXT_COLOR);
        add(logoLabel, BorderLayout.CENTER);
        
        // Bottom panel for progress
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setOpaque(false);
        
        // Status label
        statusLabel = new JLabel("Starting...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_COLOR);
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(300, 5));
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        setPreferredSize(new Dimension(400, 200));
    }

    private void centerInDesktop(JDesktopPane desktop) {
        Dimension desktopSize = desktop.getSize();
        
        // Wait for desktop to be properly sized
        if (desktopSize.width == 0 || desktopSize.height == 0) {
            // Try to get size from desktop's parent
            Container parent = desktop.getParent();
            if (parent != null) {
                desktopSize = parent.getSize();
            }
            // If still no size, default to screen size
            if (desktopSize.width == 0 || desktopSize.height == 0) {
                desktopSize = Toolkit.getDefaultToolkit().getScreenSize();
            }
        }
        
        Dimension frameSize = splashFrame.getSize();
        int x = Math.max(0, (desktopSize.width - frameSize.width) / 2);
        int y = Math.max(0, (desktopSize.height - frameSize.height) / 2);
        splashFrame.setLocation(x, y);
    }

    public void showSplash(JDesktopPane desktop, boolean isStartup) {
        this.desktop = desktop;
        
        if (isStartup) {
            logoLabel.setText("MicrOS");
            statusLabel.setText("Starting...");
        } else {
            logoLabel.setText("Shutting Down");
            statusLabel.setText("Please wait...");
        }

        splashFrame = new JInternalFrame();
        splashFrame.putClientProperty("JInternalFrame.isPalette", Boolean.TRUE);
        splashFrame.setBorder(null);
        ((javax.swing.plaf.basic.BasicInternalFrameUI) splashFrame.getUI()).setNorthPane(null);
        splashFrame.setContentPane(this);
        
        // Ensure proper size
        splashFrame.pack();
        splashFrame.setSize(getPreferredSize());
        
        // Add to desktop first
        desktop.add(splashFrame);
        desktop.setLayer(splashFrame, Integer.MAX_VALUE);
        
        // Center after adding to desktop
        centerInDesktop(desktop);
        
        // Force to front
        splashFrame.moveToFront();
        try {
            splashFrame.setSelected(true);
            splashFrame.setVisible(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }

        desktop.revalidate();
        desktop.repaint();
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
    
    public void disposeSplash() {
        if (splashFrame != null) {
            desktop.remove(splashFrame);
            desktop.repaint();
        }
    }

    public void moveTo(JDesktopPane newDesktop) {
        if (splashFrame != null && desktop != null) {
            desktop.remove(splashFrame);
            desktop.repaint();
            
            this.desktop = newDesktop;
            newDesktop.add(splashFrame);
            newDesktop.setLayer(splashFrame, Integer.MAX_VALUE);
            
            // Recenter using the improved method
            centerInDesktop(newDesktop);
            
            splashFrame.moveToFront();
            try {
                splashFrame.setSelected(true);
            } catch (java.beans.PropertyVetoException e) {
                e.printStackTrace();
            }
            newDesktop.revalidate();
            newDesktop.repaint();
        }
    }
}
