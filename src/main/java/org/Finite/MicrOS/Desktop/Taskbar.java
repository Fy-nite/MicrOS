package org.Finite.MicrOS.Desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalButtonUI;

import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.apps.AppManifest;  // Add this import
import org.Finite.MicrOS.apps.AppType;      // Add this import
import org.Finite.MicrOS.ui.ClockPanel;
import org.Finite.MicrOS.ui.StartMenu;
import org.Finite.MicrOS.ui.SystemTray;
import org.Finite.MicrOS.ui.TaskButton;
import org.Finite.MicrOS.ui.WrapLayout;
import org.Finite.MicrOS.apps.MicrOSApp;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Taskbar extends JPanel {
    private final WindowManager windowManager;
    private final Map<String, TaskButton> taskButtons;
    private final StartMenu startMenu;
    private final JPanel startArea;
    private final JPanel taskArea;
    private final SystemTray systemTray;
    private final ClockPanel clockPanel;
    
    public Taskbar(WindowManager windowManager) {
        this.windowManager = windowManager;
        this.taskButtons = new HashMap<>();
        
        // Set up main panel
        setLayout(new BorderLayout(5, 0));
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setPreferredSize(new Dimension(800, 40));
        
        // Important: Make sure taskbar panel is opaque
        setOpaque(true);
        setBackground(new Color(33, 33, 33));
        
        // Initialize components
        startArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        taskArea = new JPanel(new WrapLayout(FlowLayout.LEFT, 5, 2));
        
        // Make sure all panels are opaque and have proper background
        startArea.setOpaque(true);
        startArea.setBackground(new Color(33, 33, 33));
        
        taskArea.setOpaque(true);
        taskArea.setBackground(new Color(33, 33, 33));
        taskArea.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        
        systemTray = new SystemTray();
        clockPanel = new ClockPanel();
        startMenu = new StartMenu(windowManager);
        
        // Configure components
        startArea.setOpaque(false);
        taskArea.setOpaque(false);
        
        // Create and configure start button
        JButton startButton = createStartButton();
        startArea.add(startButton);
        
        // Layout components
        add(startArea, BorderLayout.WEST);
        add(taskArea, BorderLayout.CENTER);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(systemTray);
        rightPanel.add(clockPanel);
        add(rightPanel, BorderLayout.EAST);
    }

    private JButton createStartButton() {
        JButton startButton = new JButton("Start");
        startButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        startButton.setFocusPainted(false);
        startButton.setBorder(new EmptyBorder(5, 15, 5, 15));
        startButton.setOpaque(true); // Make start button opaque
        startButton.setBackground(new Color(45, 45, 45));
        startButton.setForeground(new Color(220, 220, 220));
        
        // Custom button UI
        startButton.setUI(new MetalButtonUI() {
            @Override
            protected void paintButtonPressed(Graphics g, AbstractButton b) {
                g.setColor(new Color(60, 60, 60));
                g.fillRect(0, 0, b.getWidth(), b.getHeight());
            }
        });
        
        startButton.addActionListener(e -> {
            if (!startMenu.isVisible()) {
                startMenu.show(startButton, 0, -startMenu.getPreferredSize().height);
            }
        });
        
        return startButton;
    }

    public void addWindow(String windowId, JInternalFrame frame) {
        if (!taskButtons.containsKey(windowId)) {
            TaskButton button = new TaskButton(frame);
            button.setOpaque(true); // Make button opaque
            taskButtons.put(windowId, button);
            
            // Add button to task area with animation
            taskArea.add(button);
            button.setVisible(true);
            
            // Update animation to maintain opacity
            javax.swing.Timer timer = new javax.swing.Timer(10, null);
            float[] alpha = { 0.0f };
            timer.addActionListener(e -> {
                alpha[0] += 0.1f;
                button.setBackground(new Color(45, 45, 45));
                button.setForeground(new Color(220, 220, 220));
                if (alpha[0] >= 1.0f) {
                    timer.stop();
                }
            });
            timer.start();
            
            revalidate();
            repaint();
        }
    }

    public void removeWindow(String windowId) {
        TaskButton button = taskButtons.remove(windowId);
        if (button != null) {
            taskArea.remove(button);
            revalidate();
            repaint();
        }
    }

    public void addAppFromManifest(AppManifest manifest) {
        String appName = manifest.getName();
        String identifier = manifest.getIdentifier();
        AppType appType = manifest.getAppType();
        
        // Create a custom internal frame for the pinned app
        JInternalFrame dummyFrame = new JInternalFrame(appName);
        dummyFrame.putClientProperty("pinned", true);
        
        // Create a dummy app instance to store manifest
        MicrOSApp dummyApp = new MicrOSApp() {
            @Override public JComponent createUI() { return new JPanel(); }
            @Override public void onStart() {}
            @Override public void onStop() {}
        };
        dummyApp.setManifest(manifest);
        dummyFrame.putClientProperty("app", dummyApp);
        
        // Create TaskButton for pinned app
        TaskButton pinnedButton = new TaskButton(dummyFrame) {
            @Override
            protected void handleClick() {
                windowManager.createWindow("app-" + identifier, appName, appType.getIdentifier());
            }
        };
        
        // Add to start area with same style as regular task buttons
        startArea.add(pinnedButton);
        revalidate();
        repaint();
    }

    // Deprecate old method but keep for compatibility
    @Deprecated
    public void addApp(String appName, String windowType) {
        addAppFromManifest(createLegacyManifest(appName, windowType));
    }

    private AppManifest createLegacyManifest(String appName, String windowType) {
        AppManifest manifest = new AppManifest();
        manifest.setName(appName);
        manifest.setIdentifier("legacy." + windowType + "." + appName.toLowerCase());
        manifest.setAppType(AppType.fromIdentifier(windowType));
        return manifest;
    }
}
