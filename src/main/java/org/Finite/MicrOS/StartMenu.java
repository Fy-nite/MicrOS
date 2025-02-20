package org.Finite.MicrOS;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionListener;  // Add this import

public class StartMenu extends JPopupMenu {
    private final WindowManager windowManager;

    public StartMenu(WindowManager windowManager) {
        this.windowManager = windowManager;
        setBackground(new Color(33, 33, 33));
        setBorder(new CompoundBorder(
            new LineBorder(new Color(60, 60, 60)),
            new EmptyBorder(5, 5, 5, 5)
        ));
        
        setupMenu();
    }

    private void setupMenu() {
        // Applications section
        add(createSection("Applications"));
        add(createMenuItem("Text Editor", "texteditor", "start-texteditor"));
        add(createMenuItem("Web Browser", "webviewer", "start-webviewer"));
        add(createMenuItem("File Manager", "filemanager", "start-filemanager"));
        add(createMenuItem("Image Viewer", "imageviewer", "start-imageviewer"));
        addSeparator();
        
        // System section
        add(createSection("System"));
        add(createMenuItem("Settings", e -> {
            Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
            new SettingsDialog(frame).setVisible(true);
        }));
        addSeparator();
        
        // Power section
        add(createSection("Power"));
        add(createMenuItem("Exit", e -> System.exit(0)));
    }

    private JLabel createSection(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(150, 150, 150));
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setBorder(new EmptyBorder(5, 5, 5, 5));
        return label;
    }

    private JMenuItem createMenuItem(String text, String windowType, String windowId) {
        JMenuItem item = new JMenuItem(text);
        styleMenuItem(item);
        item.addActionListener(e -> windowManager.createWindow(windowId, text, windowType));
        return item;
    }

    private JMenuItem createMenuItem(String text, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        styleMenuItem(item);
        item.addActionListener(action);
        return item;
    }

    private void styleMenuItem(JMenuItem item) {
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        item.setBackground(new Color(33, 33, 33));
        item.setForeground(Color.WHITE);
        item.setBorder(new EmptyBorder(5, 15, 5, 15));
    }
}
