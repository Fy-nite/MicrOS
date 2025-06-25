package org.Finite.MicrOS.ui;

import javax.swing.*;
import org.Finite.MicrOS.Desktop.BackgroundPanel;
import org.Finite.MicrOS.Desktop.Settings;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.apps.*;
import org.Finite.MicrOS.ui.SystemTray;
import org.Finite.MicrOS.util.AsmRunner;
import java.awt.*;
import java.io.File;

public class SettingsDialog extends MicrOSApp {
    private final JPanel mainPanel;
    private final Settings settings;
    private final JComboBox<String> lookAndFeelCombo;
    private final JComboBox<String> themeCombo;
    private JTextField backgroundField;
    private final VirtualFileSystem vfs;
    private final WindowManager windowManager;
    private JFileChooser fileChooser;

    public SettingsDialog(WindowManager windowManager) {
        this.windowManager = windowManager;
        this.settings = Settings.getInstance();
        this.vfs = VirtualFileSystem.getInstance();

        mainPanel = new JPanel(new BorderLayout());
        
        // Create basic manifest
        AppManifest manifest = new AppManifest();
        manifest.setName("Settings");
        manifest.setIdentifier("org.finite.micros.settings");
        manifest.setMainClass(getClass().getName());
        setManifest(manifest);
        
        // Initialize the fields
        lookAndFeelCombo = new JComboBox<>();
        themeCombo = new JComboBox<>();
        fileChooser = new JFileChooser(vfs.getRootPath().toFile());
    }

    @Override
    public JComponent createUI() {
        try {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Look and Feel selector
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Look and Feel:"), gbc);

            UIManager.LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels();
            String[] lookNames = new String[looks.length];
            for (int i = 0; i < looks.length; i++) {
                lookNames[i] = looks[i].getClassName();
            }
            lookAndFeelCombo.setModel(new DefaultComboBoxModel<>(lookNames));
            lookAndFeelCombo.setSelectedItem(settings.getLookAndFeel());
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(lookAndFeelCombo, gbc);

            // Theme selector
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            panel.add(new JLabel("Theme:"), gbc);

            themeCombo.setModel(new DefaultComboBoxModel<>(new String[]{"light", "dark"}));
            themeCombo.setSelectedItem(settings.getTheme());
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(themeCombo, gbc);

            // Background selector
            setupBackgroundControls(panel, gbc);

            // Buttons
            JPanel buttonPanel = new JPanel();
            JButton saveButton = new JButton("Save");
            saveButton.addActionListener(e -> saveSettings());
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> windowManager.closeWindow("settings"));
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            mainPanel.removeAll(); // Clear any existing components
            mainPanel.add(panel, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            return mainPanel;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create settings UI", e);
        }
    }

    @Override
    public void onStart() {
        // Nothing special needed on start
    }

    @Override
    public void onStop() {
        // Nothing special needed on stop
    }

    private void setupBackgroundControls(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("Background:"), gbc);

        JPanel bgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backgroundField = new JTextField(20);
        backgroundField.setText(settings.getBackground());
        
        JButton browseButton = new JButton("Choose Image");
        JButton colorButton = new JButton("Choose Color");
        
        browseButton.addActionListener(e -> browseBackground());
        colorButton.addActionListener(e -> chooseColor());
        
        bgPanel.add(backgroundField);
        bgPanel.add(browseButton);
        bgPanel.add(colorButton);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        panel.add(bgPanel, gbc);
    }

    private void browseBackground() {
        try {
            if (fileChooser.showOpenDialog(mainPanel.getTopLevelAncestor()) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String virtualPath = vfs.getVirtualPath(file.toPath());
                backgroundField.setText(virtualPath);
            }
        } catch (Exception e) {
            reportError("Error browsing for background", e);
        }
    }

    private void chooseColor() {
        Color currentColor = Color.BLACK;
        try {
            String current = settings.getBackground();
            if (current.startsWith("#")) {
                currentColor = Color.decode(current);
            }
        } catch (Exception ignored) {}
        
        Color color = JColorChooser.showDialog(mainPanel, 
            "Choose Background Color", currentColor);
        if (color != null) {
            String hex = String.format("#%02x%02x%02x", 
                color.getRed(), color.getGreen(), color.getBlue());
            backgroundField.setText(hex);
        }
    }

    private void saveSettings() {
        try {
            settings.setLookAndFeel((String) lookAndFeelCombo.getSelectedItem());
            settings.setTheme((String) themeCombo.getSelectedItem());
            settings.setBackground(backgroundField.getText());
            
            // Apply look and feel
            try {
                UIManager.setLookAndFeel(settings.getLookAndFeel());
                windowManager.updateLookAndFeel();
                
                // Update background
                windowManager.updateBackground(settings.getBackground());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            windowManager.closeWindow("app-" + getClass().getName()); // Use consistent window ID
        } catch (Exception e) {
            reportError("Error saving settings", e);
        }
    }
}
