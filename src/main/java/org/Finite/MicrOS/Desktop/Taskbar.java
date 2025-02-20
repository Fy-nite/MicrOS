package org.Finite.MicrOS.Desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalButtonUI;

import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.ui.ClockPanel;
import org.Finite.MicrOS.ui.StartMenu;
import org.Finite.MicrOS.ui.SystemTray;
import org.Finite.MicrOS.ui.TaskButton;
import org.Finite.MicrOS.ui.WrapLayout;

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
        
        // Create gradient background
        setBackground(new Color(33, 33, 33));
        
        // Initialize components
        startArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        taskArea = new JPanel(new WrapLayout(FlowLayout.LEFT, 5, 2));
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
            taskButtons.put(windowId, button);
            taskArea.add(button);
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

    // Add back the addApp method that was accidentally removed
    public void addApp(String appName, String windowType) {
        JButton appButton = new JButton(appName);
        appButton.addActionListener(e -> 
            windowManager.createWindow("app-" + appName.toLowerCase(), appName, windowType));
        startArea.add(appButton);
        revalidate();
        repaint();
    }
}
