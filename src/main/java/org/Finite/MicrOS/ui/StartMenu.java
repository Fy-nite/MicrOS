package org.Finite.MicrOS.ui;

import javax.swing.*;
import javax.swing.border.*;
import org.Finite.MicrOS.core.WindowManager;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.Finite.MicrOS.apps.AppManifest;
import org.Finite.MicrOS.ui.SplashScreen;
import org.Finite.MicrOS.Main;
import org.Finite.MicrOS.core.MessageBus;
import org.Finite.MicrOS.core.Intent;
import org.Finite.MicrOS.apps.AppType;

public class StartMenu extends JPopupMenu {
    private final WindowManager windowManager;
    private final Color BG_COLOR = new Color(25, 25, 25);
    private final Color HOVER_COLOR = new Color(45, 45, 45);
    private final Color SECTION_COLOR = new Color(150, 150, 150);
    private final Color TEXT_COLOR = new Color(220, 220, 220);
    private final int MENU_WIDTH = 250;

    public StartMenu(WindowManager windowManager) {
        this.windowManager = windowManager;
        setBackground(BG_COLOR);
        setBorder(new CompoundBorder(
            new LineBorder(new Color(60, 60, 60), 1),
            new EmptyBorder(5, 2, 5, 2)
        ));
        setPreferredSize(new Dimension(MENU_WIDTH, 300));
        setupMenu();
    }

    private void setupMenu() {
        // Applications section
        add(createSection("Applications"));
        add(createAppMenuItem("Text Editor", AppType.TEXT_EDITOR));
        add(createAppMenuItem("Web Browser", AppType.WEB_VIEWER));
        add(createAppMenuItem("File Manager", AppType.FILE_MANAGER));
        add(createAppMenuItem("Image Viewer", AppType.IMAGE_VIEWER));
        createStyledSeparator();
        
        // System section
        add(createSection("System"));
        add(createAppMenuItem("App Launcher", "org.finite.micros.maver.launcher", "🚀"));
        add(createAppMenuItem("Settings", AppType.SETTINGS));
        add(createAppMenuItem("Terminal", AppType.CONSOLE));



        addSeparator();
        
        // Power section
        add(createSection("Power"));
        add(createMenuItem("Exit", e -> Main.initiateShutdown(), "🚪"));
    }

    private JLabel createSection(String text) {
        JLabel label = new JLabel("  " + text);
        label.setForeground(SECTION_COLOR);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        return label;
    }

    private JMenuItem createAppMenuItem(String text, AppType appType) {
        AppManifest manifest = new AppManifest();
        manifest.setName(text);
        manifest.setAppType(appType);
        manifest.setIdentifier(appType.getIdentifier());
        return createAppMenuItem(text, manifest.getIdentifier(), appType.getIdentifier().startsWith("org.") ? "📦" : "🔧");
    }

    private JMenuItem createAppMenuItem(String text, String identifier, String icon) {
        JMenuItem item = new JMenuItem(icon + "  " + text);
        styleMenuItem(item);
        item.addActionListener(e -> {
            setVisible(false);
            windowManager.launchAppById(identifier);
        });
        return item;
    }

    private JMenuItem createMenuItem(String text, ActionListener action, String icon) {
        JMenuItem item = new JMenuItem(icon + "  " + text);
        styleMenuItem(item);
        item.addActionListener(action);
        return item;
    }

    public JPopupMenu.Separator createStyledSeparator() {
        JPopupMenu.Separator separator = new JPopupMenu.Separator();
        separator.setBackground(new Color(60, 60, 60));
        separator.setForeground(new Color(60, 60, 60));
        addSeparator();
        return separator;
    }

    private void styleMenuItem(JMenuItem item) {
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.setBackground(BG_COLOR);
        item.setForeground(TEXT_COLOR);
        item.setBorder(new EmptyBorder(8, 15, 8, 15));
        item.setPreferredSize(new Dimension(MENU_WIDTH - 4, 35));
        
        // Add hover effect
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                item.setBackground(HOVER_COLOR);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                item.setBackground(BG_COLOR);
            }
        });
        
        // Remove the default menu item look
        item.setOpaque(true);
        item.setContentAreaFilled(true);
        item.setBorderPainted(false);
    }
}
