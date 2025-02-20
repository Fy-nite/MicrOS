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
    private JPopupMenu startMenu;

    public Taskbar(WindowManager windowManager) {
        this.windowManager = windowManager;
        this.registeredApps = new HashMap<>();
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBackground(new Color(192, 192, 192));
        setPreferredSize(new Dimension(800, 40));

        // Create and add the Start button
        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> startMenu.show(startButton, 0, startButton.getHeight()));
        add(startButton);

        // Initialize the start menu
        startMenu = new JPopupMenu();
        initializeStartMenu();
    }

    private void initializeStartMenu() {
        // Add menu items to the start menu
        JMenuItem textEditorItem = new JMenuItem("Text Editor");
        textEditorItem.addActionListener(e -> windowManager.createWindow("start-texteditor", "Text Editor", "texteditor"));
        startMenu.add(textEditorItem);

        JMenuItem imageViewerItem = new JMenuItem("Image Viewer");
        imageViewerItem.addActionListener(e -> windowManager.createWindow("start-imageviewer", "Image Viewer", "imageviewer"));
        startMenu.add(imageViewerItem);

        JMenuItem webViewerItem = new JMenuItem("Web Viewer");
        webViewerItem.addActionListener(e -> windowManager.createWindow("start-webviewer", "Web Viewer", "webviewer"));
        startMenu.add(webViewerItem);

        JMenuItem fileManagerItem = new JMenuItem("File Manager");
        fileManagerItem.addActionListener(e -> windowManager.createWindow("start-filemanager", "File Manager", "filemanager"));
        startMenu.add(fileManagerItem);
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
