package org.Finite.MicrOS.Desktop;

import javax.swing.*;
import javax.swing.plaf.metal.MetalButtonUI;

import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.apps.AppManifest;
import org.Finite.MicrOS.apps.AppType;
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
    
    private static final Color TASKBAR_BG = new Color(28, 28, 32);
    private static final Color BUTTON_BG = new Color(45, 45, 50);
    private static final Color BUTTON_HOVER = new Color(60, 60, 65);
    private static final Color BUTTON_ACTIVE = new Color(70, 70, 75);
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    
    public Taskbar(WindowManager windowManager) {
        this.windowManager = windowManager;
        this.taskButtons = new HashMap<>();
        
        // Set up main panel with gradient background
        setLayout(new BorderLayout(5, 0));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setPreferredSize(new Dimension(800, 48)); // Increased height for better button visibility
        
        // Important: Make sure taskbar panel is opaque and has nice background
        setOpaque(true);
        setBackground(TASKBAR_BG);
        
        // Initialize components with updated styling
        startArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        taskArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        
        // Make sure all panels are opaque and have proper background
        startArea.setOpaque(false);
        taskArea.setOpaque(false);
        
        systemTray = new SystemTray();
        clockPanel = new ClockPanel();
        startMenu = new StartMenu(windowManager);
        
        // Layout components
        JButton startButton = initializeStartButton();
        startArea.add(startButton);
        add(startArea, BorderLayout.WEST);
        add(taskArea, BorderLayout.CENTER);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        rightPanel.setOpaque(false);
        rightPanel.add(systemTray);
        rightPanel.add(Box.createHorizontalStrut(4));
        rightPanel.add(clockPanel);
        add(rightPanel, BorderLayout.EAST);
    }

    private JButton initializeStartButton() {
        JButton startButton = new JButton("Start");
        startButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startButton.setFocusPainted(false);
        startButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 55), 1),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        startButton.setOpaque(true);
        startButton.setBackground(BUTTON_BG);
        startButton.setForeground(TEXT_COLOR);
        
        startButton.setUI(new MetalButtonUI() {
            @Override
            protected void paintButtonPressed(Graphics g, AbstractButton b) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BUTTON_ACTIVE);
                g2.fillRoundRect(0, 0, b.getWidth(), b.getHeight(), 6, 6);
                g2.dispose();
            }
        });
        
        startButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                startButton.setBackground(BUTTON_HOVER);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                startButton.setBackground(BUTTON_BG);
            }
        });
        
        startButton.addActionListener(e -> {
            if (!startMenu.isVisible()) {
                startMenu.show(startButton, 0, -startMenu.getPreferredSize().height);
            }
        });
        
        return startButton;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(33, 33, 37),
            0, getHeight(), TASKBAR_BG
        );
        
        g2.setPaint(gradient);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
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

    public void addAppFromManifest(AppManifest manifest) {
        String appName = manifest.getName();
        String identifier = manifest.getIdentifier();
        AppType appType = manifest.getAppType();
        
        JInternalFrame dummyFrame = new JInternalFrame(appName);
        dummyFrame.putClientProperty("pinned", true);
        
        MicrOSApp dummyApp = new MicrOSApp() {
            @Override public JComponent createUI() { return new JPanel(); }
            @Override public void onStart() {}
            @Override public void onStop() {}
        };
        dummyApp.setManifest(manifest);
        dummyFrame.putClientProperty("app", dummyApp);
        
        TaskButton pinnedButton = new TaskButton(dummyFrame) {
            @Override
            protected void handleClick() {
                windowManager.createWindow("app-" + identifier, appName, appType.getIdentifier());
            }
        };
        
        // Ensure the button is properly sized and visible
        pinnedButton.setPreferredSize(new Dimension(80, 40));
        pinnedButton.setMinimumSize(new Dimension(60, 40));
        pinnedButton.setMaximumSize(new Dimension(100, 40));
        
        startArea.add(pinnedButton);
        startArea.revalidate();
        startArea.repaint();
        revalidate();
        repaint();
    }
}
