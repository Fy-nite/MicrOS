package org.Finite.MicrOS;

import java.awt.*;
import javax.swing.*;

import org.Finite.MicrOS.Desktop.BackgroundPanel;
import org.Finite.MicrOS.Desktop.Settings;
import org.Finite.MicrOS.Desktop.Taskbar;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.apps.AppManifest;  // Add this import
import org.Finite.MicrOS.apps.AppType;      // Add this import
import org.Finite.MicrOS.ui.Console;
import org.Finite.MicrOS.util.AsmRunner;
import org.finite.ModuleManager.ModuleInit;
import org.Finite.MicrOS.ui.FontLoader;
import org.Finite.MicrOS.ui.ErrorDialog;

import java.io.IOException;
import javafx.application.Platform;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.beust.jcommander.JCommander;
import org.Finite.MicrOS.cli.CommandLineArgs;

/**
 * Main class for launching the MicrOS desktop environment.
 */
public class Main {

    private static WindowManager windowManager;
    private static final String VERSION = "1.0.0";

    /**
     * Main method to set the look and feel and launch the desktop environment.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        CommandLineArgs cliArgs = new CommandLineArgs();
        JCommander commander = JCommander.newBuilder()
            .addObject(cliArgs)
            .build();
        
        commander.setProgramName("MicrOS");

        try {
            commander.parse(args);
            
            // Load system fonts
            FontLoader.loadResourceFont("SegoeUI", "/fonts/SegoeUI.ttf");
            FontLoader.loadResourceFont("SegoeUI-Bold", "/fonts/SegoeUI-Bold.ttf");
            FontLoader.loadResourceFont("JetBrainsMono", "/fonts/JetBrainsMono-Regular.ttf");
            
            // Set default UI font
            UIManager.put("Button.font", FontLoader.getFont("SegoeUI", Font.PLAIN, 12));
            UIManager.put("Label.font", FontLoader.getFont("SegoeUI", Font.PLAIN, 12));
            UIManager.put("MenuItem.font", FontLoader.getFont("SegoeUI", Font.PLAIN, 12));
            UIManager.put("Menu.font", FontLoader.getFont("SegoeUI", Font.PLAIN, 12));
            UIManager.put("TextArea.font", FontLoader.getFont("JetBrainsMono", Font.PLAIN, 13));
            UIManager.put("TextField.font", FontLoader.getFont("SegoeUI", Font.PLAIN, 12));

            if (cliArgs.isHelp()) {
                commander.usage();
                return;
            }

            if (cliArgs.isVersion()) {
                System.out.println("MicrOS version " + VERSION);
                return;
            }

            if (cliArgs.isDebug()) {
                // TODO: Enable debug logging
                System.setProperty("debug", "true");
            }

            if (cliArgs.isInit()) {
                initializeFilesystem(cliArgs.getConfigPath());
                return;
            }

            if (cliArgs.isConsoleOnly()) {
                startConsoleMode();
                return;
            }

            // Normal startup
            // Initialize JavaFX platform
            Platform.startup(() -> {});
            
            ModuleInit.initallmodules();
            
            // Initialize settings and apply look and feel
            Settings settings = Settings.getInstance();
            UIManager.setLookAndFeel(settings.getLookAndFeel());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            commander.usage();
            System.exit(1);
        }
        SwingUtilities.invokeLater(Main::Desktopenviroment);
    }

    private static void initializeFilesystem(String configPath) {
        try {
            VirtualFileSystem vfs = VirtualFileSystem.getInstance();
            if (configPath != null) {
                // TODO: Load custom config
                System.out.println("Initializing filesystem with config: " + configPath);
            }
            System.out.println("Filesystem initialized successfully");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Failed to initialize filesystem: " + e.getMessage(),
                "Initialization Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void startConsoleMode() {
        // TODO: Implement console-only mode
        System.out.println("Console mode not yet implemented");
    }

    /**
     * Initializes and displays the desktop environment.
     */
    public static void Desktopenviroment() {
        JFrame frame = new JFrame("MicrOS");
        
        // Set undecorated for borderless
        frame.setUndecorated(true);
        
        // Get the screen size
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        
        // Make sure the frame is using the screen's display mode
        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(frame);
        } else {
            // Fallback if full screen is not supported
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            // Set bounds to screen size
            Rectangle bounds = ge.getMaximumWindowBounds();
            frame.setBounds(bounds);
        }

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

        // Register standard apps
        for (AppType appType : AppType.values()) {
            if (appType != AppType.CUSTOM && appType != AppType.DEFAULT) {
                AppManifest manifest = new AppManifest();
                manifest.setName(appType.getDisplayName());
                manifest.setAppType(appType);
                manifest.setPinToTaskbar(true);
                taskbar.addAppFromManifest(manifest);
            }
        }

        // Register executable file types
        windowManager.registerExecutableFileType("txt", AppType.TEXT_EDITOR.getIdentifier());
        windowManager.registerExecutableFileType("png", AppType.IMAGE_VIEWER.getIdentifier());
        windowManager.registerExecutableFileType("jpg", AppType.IMAGE_VIEWER.getIdentifier());
        windowManager.registerExecutableFileType("html", AppType.WEB_VIEWER.getIdentifier());

        frame.add(desktop, BorderLayout.CENTER); // Ensure desktop is added to the frame

        try {
            // Auto-start registered apps
            SwingUtilities.invokeLater(() -> {
                try {
                    JInternalFrame maverFrame = windowManager.launchAppById("org.finite.micros.maver.launcher");
                    if (maverFrame == null) {
                        ErrorDialog.showError(desktop, "Failed to launch Maver App Launcher", 
                            new RuntimeException("App launcher initialization failed"));
                    }
                } catch (Exception e) {
                    ErrorDialog.showError(desktop, "Error during startup", e);
                }
            });

            frame.setVisible(true);
            
        } catch (Exception e) {
            // Show error dialog for initialization errors
            JOptionPane.showMessageDialog(null,
                "Critical error during initialization: " + e.getMessage(),
                "Startup Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }

        // Create initial windows
        windowManager.createWindow("main", "Main Console", true);
        windowManager.writeToConsole("main", "MicroAssembly Interpreter v1.0");

        // Auto-start registered apps
        SwingUtilities.invokeLater(() -> {
            try {
                // Launch Maver
                JInternalFrame maverFrame = windowManager.launchAppById("org.finite.micros.maver.launcher");
                if (maverFrame == null) {
                    System.err.println("Failed to launch Maver App Launcher");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Remove auto-start of text editor
        // JInternalFrame editorFrame = windowManager.createWindow("editor1", "Text Editor", "texteditor");
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
