package org.Finite.MicrOS;

import java.awt.*;
import javax.swing.*;

/**
 * Main class for launching the MicrOS desktop environment.
 */
public class Main {

    private static WindowManager windowManager;

    /**
     * Main method to set the look and feel and launch the desktop environment.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(Main::Desktopenviroment);
    }

    /**
     * Initializes and displays the desktop environment.
     */
    public static void Desktopenviroment() {
        JFrame frame = new JFrame("MicrOS");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JDesktopPane desktop = new JDesktopPane();
        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        windowManager = new WindowManager(desktop, vfs);

        // Add background panel
        BackgroundPanel backgroundPanel = new BackgroundPanel("/images/image.png");
        backgroundPanel.setLayout(new BorderLayout());
        desktop.add(backgroundPanel, JLayeredPane.FRAME_CONTENT_LAYER);
        desktop.setLayer(backgroundPanel, JLayeredPane.DEFAULT_LAYER);
        backgroundPanel.setBounds(0, 0, desktop.getWidth(), desktop.getHeight());
        desktop.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                backgroundPanel.setBounds(0, 0, desktop.getWidth(), desktop.getHeight());
            }
        });

        Taskbar taskbar = new Taskbar(windowManager);
        frame.add(taskbar, BorderLayout.SOUTH);
        windowManager.setTaskbar(taskbar);

        // Register apps
        taskbar.addApp("Text Editor", "texteditor");
        taskbar.addApp("Image Viewer", "imageviewer");
        taskbar.addApp("Web Viewer", "webviewer");

        // Register executable file types
        windowManager.registerExecutableFileType("txt", "texteditor");
        windowManager.registerExecutableFileType("png", "imageviewer");
        windowManager.registerExecutableFileType("jpg", "imageviewer");

        frame.add(desktop, BorderLayout.CENTER); // Ensure desktop is added to the frame

        frame.setVisible(true);

        // Create initial windows
        windowManager.createWindow("main", "Main Console", true);
        windowManager.writeToConsole("main", "MicroAssembly Interpreter v1.0");
        
        // Create and demonstrate text editor
        JInternalFrame editorFrame = windowManager.createWindow("editor1", "Text Editor Demo", "texteditor");
        windowManager.setEditorText("editor1", "Hello World!\n\nThis is a demo of the text editor.");
        
        // After 2 seconds, read and display the content in console
        new Timer(2000, e -> {
            String content = windowManager.getEditorText("editor1");
            windowManager.writeToConsole("main", "\nText Editor content:\n" + content);
            ((Timer)e.getSource()).stop();
        }).start();
    }

    /**
     * Gets the WindowManager instance.
     *
     * @return WindowManager instance
     */
    public static WindowManager getWindowManager() {
        return windowManager;
    }
}
