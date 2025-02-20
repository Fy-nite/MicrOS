package org.Finite.MicrOS.ui;

import javax.swing.*;

import org.Finite.MicrOS.Desktop.BackgroundPanel;
import org.Finite.MicrOS.Desktop.Settings;
import org.Finite.MicrOS.core.VirtualFileSystem;

import java.awt.*;
import java.io.File;

public class SettingsDialog extends JDialog {
    private final Settings settings;
    private final JComboBox<String> lookAndFeelCombo;
    private final JComboBox<String> themeCombo;
    private JTextField backgroundField;
    private final VirtualFileSystem vfs;

    public SettingsDialog(Frame owner) {
        super(owner, "Settings", true);
        this.settings = Settings.getInstance();
        this.vfs = VirtualFileSystem.getInstance();

        setLayout(new BorderLayout());
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
        lookAndFeelCombo = new JComboBox<>(lookNames);
        lookAndFeelCombo.setSelectedItem(settings.getLookAndFeel());
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(lookAndFeelCombo, gbc);

        // Theme selector
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Theme:"), gbc);

        themeCombo = new JComboBox<>(new String[]{"light", "dark"});
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
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
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
        JFileChooser chooser = new JFileChooser(vfs.getRootPath().toFile());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String virtualPath = vfs.getVirtualPath(file.toPath());
            backgroundField.setText(virtualPath);
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
        
        Color color = JColorChooser.showDialog(this, 
            "Choose Background Color", currentColor);
        if (color != null) {
            String hex = String.format("#%02x%02x%02x", 
                color.getRed(), color.getGreen(), color.getBlue());
            backgroundField.setText(hex);
        }
    }

    private void saveSettings() {
        settings.setLookAndFeel((String) lookAndFeelCombo.getSelectedItem());
        settings.setTheme((String) themeCombo.getSelectedItem());
        settings.setBackground(backgroundField.getText());
        
        // Apply look and feel
        try {
            UIManager.setLookAndFeel(settings.getLookAndFeel());
            
            // Update the main frame
            SwingUtilities.updateComponentTreeUI(getOwner());
            
            // Update all internal frames
            if (getOwner() instanceof JFrame) {
                JFrame frame = (JFrame) getOwner();
                for (Component comp : frame.getContentPane().getComponents()) {
                    if (comp instanceof JDesktopPane) {
                        JDesktopPane desktop = (JDesktopPane) comp;
                        // Update each internal frame
                        for (JInternalFrame internalFrame : desktop.getAllFrames()) {
                            SwingUtilities.updateComponentTreeUI(internalFrame);
                            // Refresh the frame to prevent visual artifacts
                            internalFrame.repaint();
                        }
                        // Update the desktop itself
                        SwingUtilities.updateComponentTreeUI(desktop);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Update background
        if (getOwner() instanceof JFrame) {
            JFrame frame = (JFrame) getOwner();
            for (Component comp : frame.getContentPane().getComponents()) {
                if (comp instanceof JDesktopPane) {
                    JDesktopPane desktop = (JDesktopPane) comp;
                    for (Component bg : desktop.getComponents()) {
                        if (bg instanceof BackgroundPanel) {
                            desktop.remove(bg);
                            BackgroundPanel newBg = new BackgroundPanel(settings.getBackground());
                            desktop.add(newBg, JLayeredPane.FRAME_CONTENT_LAYER);
                            desktop.setLayer(newBg, JLayeredPane.DEFAULT_LAYER);
                            newBg.setBounds(0, 0, desktop.getWidth(), desktop.getHeight());
                            desktop.revalidate();
                            desktop.repaint();
                            break;
                        }
                    }
                }
            }
        }

        dispose();
    }
}
