package org.Finite.MicrOS;

import java.awt.*;
import javax.swing.*;

import org.Finite.MicrOS.Desktop.BackgroundPanel;
import org.Finite.MicrOS.Desktop.Settings;
import org.Finite.MicrOS.Desktop.Taskbar;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.apps.AppManifest; 
import org.Finite.MicrOS.apps.AppType;      
import org.Finite.MicrOS.ui.Console;
import org.Finite.MicrOS.util.AsmRunner;
import org.finite.Common.common;
import org.finite.ModuleManager.ModuleInit;

import org.Finite.MicrOS.ui.ErrorDialog; 
import org.Finite.MicrOS.ui.SplashScreen; 
import org.Finite.MicrOS.core.ProcessManager; 
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.ui.SettingsDialog; 

import java.io.IOException;
import javafx.application.Platform;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import java.nio.file.*;
import java.io.File;

import com.beust.jcommander.JCommander;
import org.Finite.MicrOS.cli.CommandLineArgs;
import org.Finite.MicrOS.cli.LaunchOptions;

// Add these imports at the top
import com.formdev.flatlaf.*;
import com.formdev.flatlaf.intellijthemes.*;

/**
 * Main class for launching the MicrOS desktop environment.
 */
public class Main {

    private static WindowManager windowManager;
    private static final String VERSION = "1.0.0";
    private static JDesktopPane desktop; // Define desktop here
    private static SplashScreen splash;  // Add this field
    private static JFrame mainFrame; // Add this field

    /**
     * Main method to set the look and feel and launch the desktop environment.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Add this at the beginning of main()
            // Register FlatLaf themes

            FlatLightLaf.setup();  // Set FlatLight as default
            
            // Register additional themes
            UIManager.installLookAndFeel("FlatLaf Light", FlatLightLaf.class.getName());
            UIManager.installLookAndFeel("FlatLaf Dark", FlatDarkLaf.class.getName());
            UIManager.installLookAndFeel("FlatLaf IntelliJ", FlatIntelliJLaf.class.getName());
            UIManager.installLookAndFeel("FlatLaf Darcula", FlatDarculaLaf.class.getName());

            UIManager.installLookAndFeel("FlatLaf Arc Dark", FlatArcDarkIJTheme.class.getName());
            UIManager.installLookAndFeel("FlatLaf Arc Dark Orange", FlatArcDarkOrangeIJTheme.class.getName());
            
            // Continue with existing code
            LaunchOptions options = new LaunchOptions();
            JCommander commander = JCommander.newBuilder()
                .addObject(options)
                .build();
            commander.parse(args);

            if (options.isHelp()) {
                commander.usage();
                return;
            }

            if (options.getAppPath() != null) {
                launchStandaloneAppFromPath(options.getAppPath());
            } else if (options.getAppId() != null) {
                launchStandaloneApp(options.getAppId());
            } else {
                CommandLineArgs cliArgs = new CommandLineArgs();
                commander = JCommander.newBuilder()
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
        } catch (Exception e) {
            // Use null parent since desktop isn't initialized yet
            ErrorDialog.showError(null, "An error occurred during startup:", e);
            System.exit(1);
        }
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
        // Create and show splash screen first
        splash = new SplashScreen(mainFrame); // Pass mainFrame reference
        splash.show();

        // Add small delay before creating main window
        Timer timer = new Timer(500, e -> {
            ((Timer)e.getSource()).stop();
            createMainWindow();
        });
        timer.start();
    }

    private static void createMainWindow() {
        // Create main frame
        mainFrame = new JFrame("MicrOS"); // Initialize mainFrame
        mainFrame.setUndecorated(true);
        
        desktop = new JDesktopPane();
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.add(desktop, BorderLayout.CENTER);
        
        // Add shutdown hook
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                initiateShutdown();
            }
        });

        // Register Alt+F4 handler
        KeyStroke altF4 = KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK);
        mainFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(altF4, "exit");
        mainFrame.getRootPane().getActionMap().put("exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initiateShutdown();
            }
        });

        // Setup frame size
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        
        if (gd.isFullScreenSupported() && Settings.getInstance().getIsfullscreen()) {
            mainFrame.setVisible(true);
            gd.setFullScreenWindow(mainFrame);
        } else {
            mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            Rectangle bounds = ge.getMaximumWindowBounds();
            mainFrame.setBounds(bounds);
            mainFrame.setVisible(true);
        }

        // Start initialization after frame is visible
        SwingUtilities.invokeLater(() -> {
            initializeSystem();
        });
    }

    public static void initiateShutdown() {
        // Create and show shutdown splash
 
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

    public static void initializeSystem() {
        try {
            splash.setStatus("Initializing virtual filesystem...");
            VirtualFileSystem vfs = VirtualFileSystem.getInstance();
            
            splash.setStatus("Creating window manager...");
            windowManager = new WindowManager(desktop, vfs);
            
            splash.setStatus("Setting up background...");
            Settings settings = Settings.getInstance();
            BackgroundPanel backgroundPanel = new BackgroundPanel(settings.getBackground());
            backgroundPanel.setLayout(new BorderLayout());
            desktop.add(backgroundPanel, JLayeredPane.FRAME_CONTENT_LAYER);
            desktop.setLayer(backgroundPanel, JLayeredPane.DEFAULT_LAYER);
            backgroundPanel.setBounds(0, 0, desktop.getWidth(), desktop.getHeight());
            
            splash.setStatus("Creating taskbar...");
            Taskbar taskbar = new Taskbar(windowManager);
            desktop.getParent().add(taskbar, BorderLayout.SOUTH);
            windowManager.setTaskbar(taskbar);
            
            // Register standard apps
            splash.setStatus("Registering system applications...");
            registerStandardApps(taskbar);
            
            // Launch startup apps
            splash.setStatus("Launching startup applications...");
            launchStartupApps();
            
            // Final cleanup
            splash.setStatus("Startup complete");
            Thread.sleep(500); // Brief pause to show completion
            splash.disposeSplash();
            
        } catch (Exception e) {
            splash.disposeSplash();
            ErrorDialog.showError(desktop, "Error during system initialization:", e);
        }
    }

    private static void registerStandardApps(Taskbar taskbar) {
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
    }

    private static void launchStartupApps() {
        // SwingUtilities.invokeLater(() -> {
        //     try {
        //         JInternalFrame maverFrame = windowManager.launchAppById("org.finite.micros.maver.launcher");
        //         if (maverFrame == null) {
        //             throw new Exception("Failed to launch Maver App Launcher");
        //         }
        //     } catch (Exception e) {
        //         ErrorDialog.showError(desktop, "Failed to launch startup applications:", e);
        //     }
        // });
    }

    /**
     * Gets the WindowManager instance.
     *
     * @return WindowManager instance
     */
    public static WindowManager getWindowManager() {
        return windowManager;
    }

    private static void launchStandaloneApp(String appId) {
        common.print("Initializing standalone app environment");
        JFrame frame = null;
        try {
            frame = new JFrame("MicrOS App: " + appId);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            common.print("Creating desktop pane\n");
            JDesktopPane desktop = new JDesktopPane();
            frame.setContentPane(desktop);
            
            common.print("Initializing virtual file system\n");
            VirtualFileSystem vfs = VirtualFileSystem.getInstance();
            
            common.print("Creating window manager\n");
            WindowManager windowManager = new WindowManager(desktop, vfs);
            
            common.print("Configuring app loader\n");
            vfs.getAppLoader().setWindowManager(windowManager);
            
            common.print("Loading app:\n " + appId);
            vfs.getAppLoader().loadAppById(appId);
            
            common.print("\nLaunching app window\n");
            JInternalFrame appFrame = windowManager.launchAppById(appId);
            
            if (appFrame != null) {
                common.print("App launched successfully\n");
                appFrame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
                    @Override
                    public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
                        common.print("App window closed, shutting down\n");
                   
                        System.exit(0);
                    }
                });
                
                frame.setSize(800, 600);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                common.print("App window displayed\n");
            } else {
                throw new RuntimeException("Failed to create app window");
            }
        } catch (SecurityException e) {
            common.print("Security error while launching app\n", e);
            if (frame != null) frame.dispose();
            System.exit(2);
        } catch (Exception e) {
            common.print("Unexpected error while launching app", e);
            if (frame != null) frame.dispose();
            System.exit(1);
        }
    }

    private static void launchStandaloneAppFromPath(String appPath) {
        common.print("Initializing standalone app environment for path: " + appPath);
        JFrame frame = null;
        try {
            frame = new JFrame("MicrOS App: " + appPath);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            JDesktopPane desktop = new JDesktopPane();
            frame.setContentPane(desktop);
            
            VirtualFileSystem vfs = VirtualFileSystem.getInstance();
            WindowManager windowManager = new WindowManager(desktop, vfs);
            vfs.getAppLoader().setWindowManager(windowManager);
            
            // Load app from custom path
            File appFile = new File(appPath);
            if (!appFile.isAbsolute()) {
                appFile = new File(System.getProperty("user.dir"), appPath);
            }
            
            if (!appFile.exists() || !appFile.getName().endsWith(".app")) {
                throw new RuntimeException("Invalid .app path: " + appFile.getAbsolutePath());
            }
            
            String appId = vfs.getAppLoader().loadAppFromPath(appFile);
            JInternalFrame appFrame = windowManager.launchAppById(appId);
            
            if (appFrame != null) {
                appFrame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
                    @Override
                    public void internalFrameClosed(javax.swing.event.InternalFrameEvent e) {
                        System.exit(0);
                    }
                });
                
                frame.setSize(800, 600);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } else {
                throw new RuntimeException("Failed to create app window");
            }
        } catch (Exception e) {
            common.print("Error launching app from path", e);
            if (frame != null) frame.dispose();
            System.exit(1);
        }
    }
}
