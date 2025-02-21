package org.finite;

import javax.swing.*;
import java.awt.*;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.Desktop.Settings;

public class DemoApp extends MicrOSApp {
    private Settings settings;

    @Override
    public JComponent createUI() {
        settings = Settings.getInstance();

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Background setting
        JLabel backgroundLabel = new JLabel("Background:");
        JTextField backgroundField = new JTextField(settings.getBackground());
        JButton backgroundButton = new JButton("Set Background");
        backgroundButton.addActionListener(e -> {
            settings.setBackground(backgroundField.getText());
            JOptionPane.showMessageDialog(panel, "Background updated!");
        });

        // Wallpaper setting
        JLabel wallpaperLabel = new JLabel("Wallpaper:");
        JTextField wallpaperField = new JTextField(settings.getWallpaper());
        JButton wallpaperButton = new JButton("Set Wallpaper");
        wallpaperButton.addActionListener(e -> {
            settings.setWallpaper(wallpaperField.getText());
            JOptionPane.showMessageDialog(panel, "Wallpaper updated!");
        });

        // Add components to panel
        panel.add(backgroundLabel);
        panel.add(backgroundField);
        panel.add(new JLabel()); // Empty cell
        panel.add(backgroundButton);

        panel.add(wallpaperLabel);
        panel.add(wallpaperField);
        panel.add(new JLabel()); // Empty cell
        panel.add(wallpaperButton);

        // Add more settings as needed

        return panel;
    }

    @Override
    public void onStart() {
        // Initialize any resources needed by the app
        System.out.println("Settings app started");
    }

    @Override
    public void onStop() {
        // Cleanup when app is closed
        System.out.println("Settings app stopped");
    }
}
