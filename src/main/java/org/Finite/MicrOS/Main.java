package org.Finite.MicrOS;

import java.awt.*;
import javax.swing.*;

import org.Finite.MicrOS.Desktop.Settings;
import org.Finite.MicrOS.Desktop.Taskbar;
import org.finite.ModuleManager.ModuleInit;

import java.io.IOException;
import javafx.application.Platform;

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
            // Initialize JavaFX platform
            Platform.startup(() -> {});
            
            ModuleInit.initallmodules();
            
            // Initialize settings and apply look and feel
            Settings settings = Settings.getInstance();
            UIManager.setLookAndFeel(settings.getLookAndFeel());
            
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

        // Register ASM interpreter as a virtual program
        vfs.registerProgram("asm", args -> {
            if (args.length > 1) {
                try {
                    // Convert virtual path to absolute path
                    String absolutePath = vfs.resolveVirtualPath(args[1]).toAbsolutePath().toString();
                    String output = AsmRunner.RunASMFromFile(absolutePath);
                    JInternalFrame mainConsole = windowManager.getWindow("main");
                    if (mainConsole != null) {
                        Console console = (Console) mainConsole.getClientProperty("console");
                        if (console != null) {
                            console.appendText(output, Color.WHITE);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Use background from settings
        Settings settings = Settings.getInstance();
        BackgroundPanel backgroundPanel = new BackgroundPanel(settings.getBackground());
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
        windowManager.registerExecutableFileType("jpeg", "imageviewer");
        windowManager.registerExecutableFileType("gif", "imageviewer");
        windowManager.registerExecutableFileType("html", "webviewer");
        windowManager.registerExecutableFileType("htm", "webviewer");
        windowManager.registerExecutableFileType("masm", "texteditor");
        windowManager.registerExecutableFileType("asm", "texteditor");

        frame.add(desktop, BorderLayout.CENTER); // Ensure desktop is added to the frame

        frame.setVisible(true);

        // Create initial windows
        windowManager.createWindow("main", "Main Console", true);
        windowManager.writeToConsole("main", "MicroAssembly Interpreter v1.0");
        

        // Create and demonstrate text editor
        JInternalFrame editorFrame = windowManager.createWindow("editor1", "Text Editor", "texteditor");
        // windowManager.setEditorText("editor1", "Hello World!\n\nThis is a demo of the text editor.");
        
        // After 2 seconds, read and display the content in console
        // new Timer(2000, e -> {
        //     String content = windowManager.getEditorText("editor1");
        //     windowManager.writeToConsole("main", "\nText Editor content:\n" + content);
        //     ((Timer)e.getSource()).stop();
        // }).start();
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
