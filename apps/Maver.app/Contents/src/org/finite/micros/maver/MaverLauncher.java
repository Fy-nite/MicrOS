package org.finite.micros.maver;

import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.apps.AppManifest;
import org.Finite.MicrOS.core.VirtualFileSystem;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class MaverLauncher extends MicrOSApp {
    private JPanel appGrid;
    private VirtualFileSystem vfs;

    @Override
    public JComponent createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create header
        JLabel headerLabel = new JLabel("MicrOS App Launcher", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create scrollable app grid
        appGrid = new JPanel(new GridLayout(0, 3, 10, 10));
        appGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(appGrid);
        
        mainPanel.add(headerLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Load apps
        loadApps();
        
        return mainPanel;
    }

    private void loadApps() {
        vfs = VirtualFileSystem.getInstance();
        Collection<AppManifest> apps = vfs.getAppLoader().getLoadedApps();
        
        for (AppManifest app : apps) {
            JPanel appPanel = createAppButton(app);
            appGrid.add(appPanel);
        }
    }

    private JPanel createAppButton(AppManifest app) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        // Create app button with icon if available
        JButton appButton = new JButton();
        appButton.setLayout(new BorderLayout());
        
        if (!app.getIcon().isEmpty()) {
            ImageIcon icon = new ImageIcon(app.getIcon());
            Image scaled = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
            appButton.setIcon(new ImageIcon(scaled));
        }
        
        // Add app name and description
        JLabel nameLabel = new JLabel(app.getName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        
        panel.add(appButton, BorderLayout.CENTER);
        panel.add(nameLabel, BorderLayout.SOUTH);
        
        // Launch app on click
        appButton.addActionListener(e -> {
            try {
                MicrOSApp instance = vfs.getAppLoader().createAppInstance(app.getIdentifier());
                instance.setManifest(app); // Set manifest first
                instance.initialize(windowManager, vfs); // Then initialize
                windowManager.launchApp(instance); // Finally launch
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel,
                    "Failed to launch " + app.getName() + ": " + ex.getMessage(),
                    "Launch Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        return panel;
    }

    @Override
    public void onStart() {
        System.out.println("Maver Launcher started");
    }

    @Override
    public void onStop() {
        System.out.println("Maver Launcher stopped");
    }
}
