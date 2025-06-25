package org.Finite.MicrOS;

import java.awt.*;
import javax.swing.*;

import org.Finite.MicrOS.Desktop.BackgroundPanel;
import org.Finite.MicrOS.Desktop.Settings;
import org.Finite.MicrOS.Desktop.Taskbar;
import org.Finite.MicrOS.core.ProcessManager;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.apps.AppManifest;  // Add this import
import org.Finite.MicrOS.apps.AppType;      // Add this import
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.ui.Console;
import org.Finite.MicrOS.util.AsmRunner;
import org.finite.ModuleManager.ModuleInit;

import org.Finite.MicrOS.ui.ErrorDialog; // Add this import
import org.Finite.MicrOS.ui.SplashScreen;
import org.Finite.MicrOS.x11.X11Manager;

import java.io.IOException;
import javafx.application.Platform;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import com.beust.jcommander.JCommander;
import org.Finite.MicrOS.cli.CommandLineArgs;

/**
 * Main class for launching the MicrOS desktop environment.
 */
public class Main {

    private static WindowManager windowManager;
    private static final String VERSION = "1.0.0";
    private static JDesktopPane desktop; // Define desktop here
    private static CommandLineArgs commandLineArgs;
    /**
     * Main method to set the look and feel and launch the desktop environment.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        CommandLineArgs cliArgs = new CommandLineArgs();
        commandLineArgs = cliArgs; // Store the command line args for later use
        JCommander commander = JCommander.newBuilder()
            .addObject(cliArgs)
            .build();
        
        commander.setProgramName("MicrOS");

        try {
            commander.parse(args);

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


            // Update fullscreen setting based on CLI argument
            Settings settings = Settings.getInstance();
            settings.setIsfullscreen(cliArgs.isFullscreen());

            // Normal startup
            // Initialize JavaFX platform
           // Platform.startup(() -> {});
            
            ModuleInit.initallmodules();
            
            // Initialize settings and apply look and feel
            UIManager.setLookAndFeel(settings.getLookAndFeel());
            
        } catch (Exception e) {
            ErrorDialog.showError(desktop, "An error occurred during startup:", e);
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
            System.err.println("Failed to initialize filesystem: " + e.getMessage());
            System.exit(1);
        }
    }
    public static String getOS() {
        return System.getProperty("os.name");
    }
    private static void startConsoleMode() {
        // TODO: Implement console-only mode
        System.out.println("Console mode not yet implemented");
    }

    private static boolean isAndroid() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("android");
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
        if (gd.isFullScreenSupported() && Settings.getInstance().getIsfullscreen()) {
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

        desktop = new JDesktopPane(); // Initialize desktop here
        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        windowManager = new WindowManager(desktop, vfs);

        // Initialize X11 Manager if on Linux
        if (commandLineArgs.isX11()) {
            try {
                X11Manager x11Manager = X11Manager.getInstance();
                x11Manager.initialize(desktop);
                
                // Add shutdown hook to clean up X11
                Runtime.getRuntime().addShutdownHook(new Thread(x11Manager::shutdown));
                
                System.out.println("X11 integration enabled");
            } catch (Exception e) {
                System.err.println("Failed to initialize X11 integration: " + e.getMessage());
                System.out.println("Running without X11 integration");
            }
        }

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

        frame.setVisible(true);

        // Auto-start registered apps
        SwingUtilities.invokeLater(() -> {
            try {
                // // Launch Maver
                // JInternalFrame maverFrame = windowManager.launchAppById("org.finite.micros.maver.launcher");
                // if (maverFrame == null) {
                //     throw new Exception("Failed to launch Maver App Launcher");
                // }
            } catch (Exception e) {
                e.printStackTrace();
                ErrorDialog.showError(desktop, "An error occurred while launching the app:", e);
            }
        });

        // Remove auto-start of text editor
        // JInternalFrame editorFrame = windowManager.createWindow("editor1", "Text Editor", "texteditor");
    }
     public static void initiateShutdown() {
        // Create and show shutdown splash
        Window ancestor = SwingUtilities.getWindowAncestor(desktop);
        if (!(ancestor instanceof JFrame)) {
            System.err.println("Error: Main window is not a JFrame");
            System.exit(1);
        }
        SplashScreen splash = new SplashScreen((JFrame)ancestor);
        splash.setShutdownMode();
        splash.show();

        // Create shutdown thread
        Thread shutdownThread = new Thread(() -> {
            try {
                WindowManager wm = getWindowManager();
                if (wm != null) {
                    splash.setStatus("Stopping applications...");
                    Thread.sleep(200); // Brief pause to show message
                    
                    // Get list of running apps before we start closing them
                    java.util.List<String> runningApps = new java.util.ArrayList<>();
                    for (JInternalFrame frame : wm.getDesktop().getAllFrames()) {
                        MicrOSApp app = (MicrOSApp) frame.getClientProperty("app");
                        if (app != null && app.getManifest() != null) {
                            runningApps.add(app.getManifest().getName());
                        }
                    }

                    // Close each app with status update
                    for (String appName : runningApps) {
                        splash.setStatus("Stopping " + appName + "...");
                        Thread.sleep(100); // Brief pause between apps
                    }

                    splash.setStatus("Cleaning up processes...");
                    ProcessManager.getInstance().closeout();
                    Thread.sleep(200);

                    splash.setStatus("Saving system state...");
                    Thread.sleep(300);
                }
                
                splash.setStatus("Goodbye!");
                Thread.sleep(500);
                
                splash.disposeSplash();
                System.exit(0);
                
            } catch (Exception e) {
                System.err.println("Error during shutdown: " + e.getMessage());
                System.exit(1);
            }
        });
        
        shutdownThread.start();
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
