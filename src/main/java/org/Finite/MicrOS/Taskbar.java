package org.Finite.MicrOS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Taskbar class for displaying available apps and managing window actions.
 */
public class Taskbar extends JPanel {
    private final WindowManager windowManager;
    private final Map<String, String> registeredApps;

    public Taskbar(WindowManager windowManager) {
        this.windowManager = windowManager;
        this.registeredApps = new HashMap<>();
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBackground(new Color(192, 192, 192));
        setPreferredSize(new Dimension(800, 40));
    }

    public void addApp(String appName, String windowType) {
        registeredApps.put(appName, windowType);
        JButton appButton = new JButton(appName);
        appButton.addActionListener(e -> windowManager.createWindow(appName, appName, windowType));
        add(appButton);
        revalidate();
        repaint();
    }

    public void removeWindow(String windowId) {
        // Implement window removal logic if needed
    }

    public void addWindow(String windowId, JInternalFrame frame) {
        // Implement window addition logic if needed
    }
}
