package org.Finite.MicrOS.Desktop;

import javax.swing.*;
import javax.swing.border.*;
import org.Finite.MicrOS.core.WindowManager;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.Finite.MicrOS.Main;

public class StartMenu extends JPopupMenu {
    private final WindowManager windowManager;
    private final Color BG_COLOR = new Color(0, 0, 0);
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
        add(createMenuItem("Text Editor", "texteditor", "start-texteditor", "📝"));
        add(createMenuItem("Web Browser", "webviewer", "start-webviewer", "🌐"));
        add(createMenuItem("File Manager", "filemanager", "start-filemanager", "📁"));
        add(createMenuItem("Image Viewer", "imageviewer", "start-imageviewer", "🖼️"));
        createStyledSeparator();
        
        // System section
        add(createSection("System"));
        add(createMenuItem("App Launcher", e -> windowManager.launchAppById("org.finite.micros.maver.launcher"), "🚀"));
        add(createMenuItem("Settings", "settings", "settings", "⚙️"));
        addSeparator();
        
        // Power section
        add(createSection("Power"));
        add(createMenuItem("Shut Down", e -> Main.initiateShutdown(), "🚪"));
    }

    private JLabel createSection(String text) {
        JLabel label = new JLabel("  " + text);
        label.setForeground(SECTION_COLOR);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        return label;
    }

    private JMenuItem createMenuItem(String text, String windowType, String windowId, String icon) {
        JMenuItem item = new JMenuItem(icon + "  " + text);
        styleMenuItem(item);
        item.addActionListener(e -> windowManager.createWindow(windowId, text, windowType));
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
