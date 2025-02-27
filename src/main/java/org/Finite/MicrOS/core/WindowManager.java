/**
 * Manages the creation, tracking and manipulation of windows in the MicrOS desktop environment.
 * Provides factory-based window creation and management of console windows.
 */
package org.Finite.MicrOS.core;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.Finite.MicrOS.Desktop.BackgroundPanel;
import org.Finite.MicrOS.Desktop.Taskbar;
import org.Finite.MicrOS.Files.FileManager;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.apps.JavaFXApp;
import org.Finite.MicrOS.ui.ApplicationChooserDialog;
import org.Finite.MicrOS.ui.Console;
import org.Finite.MicrOS.ui.WebViewer;
import org.Finite.MicrOS.util.AsmRunner;
import org.Finite.MicrOS.apps.AppManifest;
import org.Finite.MicrOS.ui.ErrorDialog;
import org.Finite.MicrOS.ui.SettingsDialog;

/**
 * Core window management class for MicrOS desktop environment.
 */
public class WindowManager {

    /** Reference to the taskbar for window tracking */
    private Taskbar taskbar;

    /** The main desktop pane that contains all windows and icons */
    private final JDesktopPane desktop;

    /** Map of window IDs to window instances */
    private final Map<String, JInternalFrame> windows;

    /** Map of window type names to their factory implementations */
    private final Map<String, WindowFactory> windowFactories;

    /** Reference to the virtual file system */
    private final VirtualFileSystem vfs;

    /** Map of executable file types to window types */
    private final Map<String, String> executableFileTypes = new HashMap<>();

    /** Map of file extensions to associated window types */
    private final Map<String, Set<String>> fileTypeAssociations = new HashMap<>();

    private final ProcessManager processManager;

    private final Set<String> startupApps = new HashSet<>();
    private final Map<String, String> startupWindows = new HashMap<>(); // windowId -> type

    /** Application association manager */
    private final ApplicationAssociationManager appAssociationManager;

    static {
        // Initialize JavaFX platform
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Platform already initialized, ignore
        }
    }

    /**
     * Creates a new WindowManager for the given desktop pane and VFS.
     *
     * @param desktop The JDesktopPane that will contain the windows
     * @param vfs The VirtualFileSystem instance
     */
    public WindowManager(JDesktopPane desktop, VirtualFileSystem vfs) {
        this.desktop = desktop;
        this.vfs = vfs;
        this.windows = new HashMap<>();
        this.windowFactories = new HashMap<>();
        this.processManager = new ProcessManager(null); // Initialize ProcessManager
        this.appAssociationManager = new ApplicationAssociationManager(vfs);
        registerDefaultFactories();
    }

    /**
     * Registers the default window factory implementations.
     */
    private void registerDefaultFactories() {
        // Register default window factory first
        registerWindowFactory("default", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.add(new JLabel("Window: " + title, SwingConstants.CENTER));
            frame.setContentPane(contentPanel);
            return frame;
        });

        // Register console window factory
        registerWindowFactory("console", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            Console console = new Console();
            JScrollPane scrollPane = new JScrollPane(console);
            frame.add(scrollPane);
            frame.putClientProperty("console", console);
            return frame;
        });

        // Update texteditor factory to use app ID
        registerWindowFactory("org.finite.texteditor", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            try {
                MicrOSApp app = vfs.getAppLoader().createAppInstance("org.finite.texteditor");
                app.initialize(this, vfs);
                frame.add(app.createUI());
                frame.putClientProperty("app", app);
                return frame;
            } catch (Exception e) {
                reportError("Failed to create text editor", e, "org.finite.texteditor");
                return createBaseFrame(title); // Fallback to basic frame
            }
        });

        // Add image viewer factory
        registerWindowFactory("imageviewer", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            frame.setLayout(new BorderLayout());
            return frame;
        });

        // Update web viewer factory
        registerWindowFactory("webviewer", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            WebViewer webViewer = new WebViewer(vfs);
            frame.add(webViewer);
            frame.putClientProperty("webviewer", webViewer);
            return frame;
        });

        // Add file manager factory
        registerWindowFactory("filemanager", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            FileManager fileManager = new FileManager(this);
            frame.add(fileManager);
            return frame;
        });

        // Update settings window factory to use launchAppById instead
        registerWindowFactory("settings", (windowId, title) -> {
            MicrOSApp app = new SettingsDialog(this);
            AppManifest manifest = new AppManifest();
            manifest.setName("Settings");
            manifest.setIdentifier("org.finite.micros.settings");
            manifest.setMainClass(SettingsDialog.class.getName());
            app.setManifest(manifest);
            app.initialize(this, vfs);
            
            JInternalFrame frame = createBaseFrame("Settings");
            frame.setResizable(false);
            frame.setSize(800, 600);
            
            try {
                JComponent ui = app.createUI();
                frame.add(ui);
                frame.putClientProperty("app", app);
            } catch (Exception e) {
                reportError("Failed to create settings UI", e, "settings");
            }
            
            return frame;
        });

        // Register default JavaFX app factory
        registerWindowFactory("javafx-app", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            try {
                MicrOSApp app = vfs.getAppLoader().createAppInstance(windowId);
                if (app instanceof JavaFXApp) {
                    app.initialize(this, vfs);
                    frame.add(app.createUI());
                    frame.putClientProperty("app", app);
                }
                return frame;
            } catch (Exception e) {
                reportError("Failed to create JavaFX app", e, windowId);
                return createBaseFrame(title);
            }
        });

        // Register FileManagerFX
        registerWindowFactory("filemanagerfx", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            try {
                MicrOSApp app = vfs.getAppLoader().createAppInstance("org.finite.micros.filemanager.fx");
                app.initialize(this, vfs);
                frame.add(app.createUI());
                frame.putClientProperty("app", app);
                return frame;
            } catch (Exception e) {
                reportError("Failed to create FileManagerFX", e, windowId);
                return createBaseFrame(title);
            }
        });

        // Register default file associations
        registerFileAssociation("txt", "org.finite.micros.texteditor.fx");
        registerFileAssociation("md", "org.finite.micros.texteditor.fx");
        registerFileAssociation("java", "org.finite.micros.texteditor.fx");
        registerFileAssociation("masm", "org.finite.micros.texteditor.fx");
        registerFileAssociation("asm", "org.finite.micros.texteditor.fx");
        
        registerFileAssociation("png", "imageviewer");
        registerFileAssociation("jpg", "imageviewer");
        registerFileAssociation("jpeg", "imageviewer");
        registerFileAssociation("gif", "imageviewer");
        
        registerFileAssociation("html", "webviewer");
        registerFileAssociation("htm", "webviewer");
        
        // Allow text files to be opened in webviewer too
        registerFileAssociation("html", "org.finite.texteditor");
        registerFileAssociation("htm", "org.finite.texteditor");

        // Register default file associations in app association manager
        registerDefaultApplicationAssociations();
    }

    /**
     * Registers default application associations for common file types
     */
    private void registerDefaultApplicationAssociations() {
        // Text editor associations
        ApplicationAssociation textEditorApp = new ApplicationAssociation(
            "org.finite.micros.texteditor.fx", 
            "Text Editor", 
            "MicrOS Text Editor", 
            "/system/icons/texteditor.png",
            true
        );
        
        appAssociationManager.registerFileTypeAssociation("txt", textEditorApp);
        appAssociationManager.registerFileTypeAssociation("md", textEditorApp);
        appAssociationManager.registerFileTypeAssociation("java", textEditorApp);
        appAssociationManager.registerFileTypeAssociation("asm", textEditorApp);
        appAssociationManager.registerFileTypeAssociation("masm", textEditorApp);
        appAssociationManager.registerFileTypeAssociation("html", textEditorApp);
        appAssociationManager.registerFileTypeAssociation("htm", textEditorApp);
        
        // Image viewer associations
        ApplicationAssociation imageViewerApp = new ApplicationAssociation(
            "imageviewer", 
            "Image Viewer", 
            "MicrOS Image Viewer", 
            "/system/icons/imageviewer.png",
            true
        );
        
        appAssociationManager.registerFileTypeAssociation("png", imageViewerApp);
        appAssociationManager.registerFileTypeAssociation("jpg", imageViewerApp);
        appAssociationManager.registerFileTypeAssociation("jpeg", imageViewerApp);
        appAssociationManager.registerFileTypeAssociation("gif", imageViewerApp);
        
        // Web viewer associations
        ApplicationAssociation webViewerApp = new ApplicationAssociation(
            "webviewer", 
            "Web Viewer", 
            "MicrOS Web Browser", 
            "/system/icons/webviewer.png",
            true
        );
        
        appAssociationManager.registerFileTypeAssociation("html", webViewerApp);
        appAssociationManager.registerFileTypeAssociation("htm", webViewerApp);
        
        // Terminal action association
        ApplicationAssociation internalTerminalApp = new ApplicationAssociation(
            "console", 
            "Internal Terminal", 
            "Built-in MicrOS Terminal", 
            "/system/icons/terminal.png",
            true
        );
        
        ApplicationAssociation externalKonsoleApp = new ApplicationAssociation(
            "external:konsole", 
            "Konsole", 
            "KDE Terminal Emulator", 
            "/system/icons/konsole.png",
            false
        );
        
        ApplicationAssociation externalGnomeTerminalApp = new ApplicationAssociation(
            "external:gnome-terminal", 
            "GNOME Terminal", 
            "GNOME Terminal Emulator", 
            "/system/icons/gnome-terminal.png",
            false
        );
        
        appAssociationManager.registerActionAssociation("terminal", internalTerminalApp);
        appAssociationManager.registerActionAssociation("terminal", externalKonsoleApp);
        appAssociationManager.registerActionAssociation("terminal", externalGnomeTerminalApp);

        // Add File Manager FX
        ApplicationAssociation fileManagerFXApp = new ApplicationAssociation(
            "filemanagerfx", 
            "File Explorer FX", 
            "JavaFX File Explorer", 
            "/system/icons/filemanager.png",
            false
        );
        
        appAssociationManager.registerActionAssociation("filemanager", fileManagerFXApp);
    }

    /**
     * Registers a new window factory for creating windows of a specific type.
     *
     * @param type Factory type identifier
     * @param factory WindowFactory implementation
     */
    public void registerWindowFactory(String type, WindowFactory factory) {
        windowFactories.put(type, factory);
    }

    /**
     * Sets the taskbar reference for window tracking.
     *
     * @param taskbar The taskbar instance
     */
    public void setTaskbar(Taskbar taskbar) {
        this.taskbar = taskbar;
    }

    /**
     * Creates a basic internal frame with default settings.
     *
     * @param title Title of the window
     * @return Configured JInternalFrame
     */
    public JInternalFrame createBaseFrame(String title) {
        JInternalFrame frame = new JInternalFrame(
            title,
            true,
            true,
            true,
            true
        );
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        int offset = 30 * windows.size();
        frame.setLocation(offset, offset);

        return frame;
    }

    /**
     * Creates a new window using the specified factory type.
     *
     * @param windowId Unique identifier for the window
     * @param title Window title
     * @param type Window factory type identifier
     * @return The created window frame
     * @throws IllegalArgumentException if window type is not registered
     */
    public JInternalFrame createWindow(
        String windowId,
        String title,
        String type
    ) {
        try {
            System.out.println("Creating window: " + windowId + ", Title: " + title + ", Type: " + type);
            WindowFactory factory = windowFactories.get(type);
            if (factory == null) {
                ErrorDialog.showError(desktop, "Unknown window type: " + type,
                    new IllegalArgumentException("No factory registered for window type: " + type));
                return null;
            }

            JInternalFrame frame = factory.createWindow(windowId, title);

            frame.addInternalFrameListener(
                new javax.swing.event.InternalFrameAdapter() {
                    @Override
                    public void internalFrameClosed(
                        javax.swing.event.InternalFrameEvent e
                    ) {
                        if (taskbar != null) {
                            taskbar.removeWindow(windowId);
                        }
                    }
                }
            );

            if (taskbar != null) {
                taskbar.addWindow(windowId, frame);
            }

            desktop.add(frame);
            windows.put(windowId, frame);
            frame.setVisible(true); // Ensure the frame is visible
            System.out.println("Window created and added to desktop: " + windowId);
            return frame;
        } catch (Exception e) {
            ErrorDialog.showError(desktop, "Error creating window", e);
            return null;
        }
    }

    /**
     * Legacy window creation method for backwards compatibility.
     *
     * @param windowId Unique window identifier
     * @param title Window title
     * @param isConsole Whether to create a console window
     * @return The created window frame
     */
    public JInternalFrame createWindow(
        String windowId,
        String title,
        boolean isConsole
    ) {
        return createWindow(windowId, title, isConsole ? "console" : "default");
    }

    /**
     * Retrieves a window by its ID.
     *
     * @param windowId Window identifier
     * @return The window frame or null if not found
     */
    public JInternalFrame getWindow(String windowId) {
        return windows.get(windowId);
    }



    // get window manager

    

    /**
     * Writes text to a console window.
     *
     * @param windowId Console window identifier
     * @param text Text to write
     */
    public void writeToConsole(String windowId, String text) {
        JInternalFrame frame = windows.get(windowId);
        if (frame != null) {
            Console console = (Console) frame.getClientProperty("console");
            if (console != null) {
                console.appendText(text + "\n", Color.WHITE);  // Add Color parameter
            }
        }
    }

    /**
     * Clears the content of a console window.
     *
     * @param windowId Console window identifier
     */
    public void clearConsole(String windowId) {
        JInternalFrame frame = windows.get(windowId);
        if (frame != null) {
            JTextArea console = (JTextArea) frame.getClientProperty("console");
            if (console != null) {
                console.setText("");
            }
        }
    }

    /**
     * Gets text content from a text editor window.
     *
     * @param windowId Text editor window identifier
     * @return The text content or null if window not found/not a text editor
     */
    public String getEditorText(String windowId) {
        JInternalFrame frame = windows.get(windowId);
        if (frame != null) {
            MicrOSApp app = (MicrOSApp) frame.getClientProperty("app");
            if (app != null) {
                try {
                    // Assuming the app has a getText() method
                    return (String) app.getClass().getMethod("getText").invoke(app);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

   
    /**
     * Closes and disposes a specific window.
     *
     * @param windowId Window identifier to close
     */
    public void closeWindow(String windowId) {
        JInternalFrame frame = windows.remove(windowId);
        if (frame != null) {
            // Fire closing event before disposal
            frame.doDefaultCloseAction();
            frame.dispose();
            
            // Remove from desktop if needed
            if (desktop != null) {
                desktop.remove(frame);
                desktop.repaint();
            }
            
            // Handle any app cleanup
            MicrOSApp app = (MicrOSApp) frame.getClientProperty("app");
            if (app != null) {
                app.onStop();
            }
        }
    }

    /**
     * Closes all windows managed by this WindowManager.
     */
    public void closeAllWindows() {
        for (JInternalFrame frame : windows.values()) {
            frame.dispose();
        }
        windows.clear();
    }

    /**
     * Registers a new executable file type and its associated window type.
     *
     * @param extension File extension
     * @param windowType Window type identifier
     */
    public void registerExecutableFileType(String extension, String windowType) {
        executableFileTypes.put(extension, windowType);
    }

    /**
     * Runs an executable file by creating a window of the associated type.
     *
     * @param virtualPath Path to the executable file in the VFS
     */
    public void runExecutable(String virtualPath) {
        String extension = vfs.getFileExtension(virtualPath);
        String windowType = executableFileTypes.get(extension);
        if (windowType != null) {
            createWindow("exec-" + virtualPath.hashCode(), virtualPath, windowType);
        } else {
            System.out.println("No registered executable for file type: " + extension);
        }
    }

    /**
     * Sets the URL for a web viewer window
     */
    public void setWebViewerUrl(String windowId, String url) {
        JInternalFrame frame = windows.get(windowId);
        if (frame != null) {
            WebViewer webViewer = (WebViewer) frame.getClientProperty("webviewer");
            if (webViewer != null) {
                webViewer.loadUrl(url);
            }
        }
    }

    /**
     * Registers a new file association for a specific file extension and window type.
     *
     * @param extension File extension
     * @param windowType Window type identifier
     */
    public void registerFileAssociation(String extension, String windowType) {
        fileTypeAssociations.computeIfAbsent(extension, k -> new HashSet<>()).add(windowType);
    }

    /**
     * Retrieves the set of window types associated with a specific file extension.
     *
     * @param extension File extension
     * @return Set of associated window types
     */
    public Set<String> getFileAssociations(String extension) {
        return fileTypeAssociations.getOrDefault(extension, new HashSet<>());
    }

    /**
     * Opens a file using the default associated application, or prompts user to choose
     * 
     * @param virtualPath Path to the file in the VFS
     * @param parentComponent Parent component for dialogs
     * @return The opened window frame or null if canceled or failed
     */
    public JInternalFrame openFile(String virtualPath, Component parentComponent) {
        String extension = vfs.getFileExtension(virtualPath).toLowerCase();
        String fileName = virtualPath.substring(virtualPath.lastIndexOf('/') + 1);
        
        List<ApplicationAssociation> associations = appAssociationManager.getFileTypeAssociations(extension);
        
        if (associations.isEmpty()) {
            // No registered applications
            int option = JOptionPane.showConfirmDialog(
                parentComponent,
                "No applications registered for this file type. Would you like to open it with Text Editor?",
                "Open File",
                JOptionPane.YES_NO_OPTION
            );
            
            if (option == JOptionPane.YES_OPTION) {
                return openFileWith(virtualPath, "org.finite.micros.texteditor.fx");
            } else {
                return null;
            }
        } else if (associations.size() == 1) {
            // Only one application available
            return openFileWith(virtualPath, associations.get(0).getId());
        } else {
            // Multiple applications available, check for default
            ApplicationAssociation preferredApp = appAssociationManager.getPreferredFileTypeApplication(extension);
            
            if (preferredApp != null) {
                return openFileWith(virtualPath, preferredApp.getId());
            } else {
                // Show application chooser dialog
                ApplicationChooserDialog dialog = new ApplicationChooserDialog(
                    parentComponent, vfs, associations, fileName, extension
                );
                dialog.setVisible(true);
                
                String selectedAppId = dialog.getSelectedApplicationId();
                if (selectedAppId != null) {
                    // Save preference if requested
                    if (dialog.isRememberChoice()) {
                        appAssociationManager.setPreferredFileTypeApplication(extension, selectedAppId);
                        appAssociationManager.saveUserPreferences();
                    }
                    
                    return openFileWith(virtualPath, selectedAppId);
                } else {
                    return null; // User canceled
                }
            }
        }
    }
    
    /**
     * Performs an action using the default associated application, or prompts user to choose
     * 
     * @param actionId Unique action identifier
     * @param actionName User-friendly action name
     * @param parentComponent Parent component for dialogs
     * @return The application ID that was chosen, or null if canceled
     */
    public String performAction(String actionId, String actionName, Component parentComponent) {
        List<ApplicationAssociation> associations = appAssociationManager.getActionAssociations(actionId);
        
        if (associations.isEmpty()) {
            // No registered applications
            JOptionPane.showMessageDialog(
                parentComponent,
                "No applications available for this action.",
                "Action Not Available",
                JOptionPane.ERROR_MESSAGE
            );
            return null;
        } else if (associations.size() == 1) {
            // Only one application available
            return associations.get(0).getId();
        } else {
            // Multiple applications available, check for default
            ApplicationAssociation preferredApp = appAssociationManager.getPreferredActionApplication(actionId);
            
            if (preferredApp != null) {
                return preferredApp.getId();
            } else {
                // Show application chooser dialog
                ApplicationChooserDialog dialog = new ApplicationChooserDialog(
                    parentComponent, vfs, associations, actionId, actionName
                );
                dialog.setVisible(true);
                
                String selectedAppId = dialog.getSelectedApplicationId();
                if (selectedAppId != null && dialog.isRememberChoice()) {
                    // Save preference if requested
                    appAssociationManager.setPreferredActionApplication(actionId, selectedAppId);
                    appAssociationManager.saveUserPreferences();
                }
                
                return selectedAppId;
            }
        }
    }
    
    /**
     * Opens a file with a specific window type.
     *
     * @param virtualPath Path to the file in the VFS
     * @param windowType Window type identifier
     * @return The created window frame
     */
    public JInternalFrame openFileWith(String virtualPath, String windowType) {
        // Check if this is an external application
        if (windowType.startsWith("external:")) {
            String externalApp = windowType.substring("external:".length());
            openFileWithExternalApp(virtualPath, externalApp);
            return null; // No internal window created
        }
        
        // Open with internal application
        String windowId = windowType + "-" + virtualPath.hashCode();
        JInternalFrame frame = createWindow(windowId, virtualPath, windowType);
        
        if (windowType.equals("org.finite.texteditor") || windowType.equals("org.finite.micros.texteditor.fx")) {
            try {
                String content = new String(vfs.readFile(virtualPath));
                MicrOSApp app = (MicrOSApp) frame.getClientProperty("app");
                if (app != null) {
                    app.getClass().getMethod("setText", String.class).invoke(app, content);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (windowType.equals("imageviewer")) {
            try {
                byte[] imageData = vfs.readFile(virtualPath);
                ImageIcon icon = new ImageIcon(imageData);
                JLabel imageLabel = new JLabel(icon);
                JScrollPane scrollPane = new JScrollPane(imageLabel);
                frame.add(scrollPane, BorderLayout.CENTER);
                frame.revalidate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (windowType.equals("webviewer")) {
            try {
                setWebViewerUrl(windowId, "file:" + vfs.getRealPath(virtualPath).toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return frame;
    }

    /**
     * Opens a file with an external application
     * 
     * @param virtualPath Virtual path to the file
     * @param externalApp Name of the external application
     */
    private void openFileWithExternalApp(String virtualPath, String externalApp) {
        try {
            // Get the real path to the file
            java.nio.file.Path realPath = vfs.getRealPath(virtualPath);
            if (realPath != null) {
                // Build the command to open the file with the external app
                ProcessBuilder pb = new ProcessBuilder(externalApp, realPath.toString());
                pb.inheritIO(); // Redirect I/O to console
                pb.start();
            } else {
                System.err.println("Could not resolve real path for: " + virtualPath);
            }
        } catch (IOException e) {
            System.err.println("Error launching external application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a terminal with the given title
     * 
     * @param title Terminal title
     * @param parentComponent Parent component for dialogs
     * @return The created terminal window or null if external terminal was launched
     */
    public JInternalFrame createTerminal(String title, Component parentComponent) {
        String selectedApp = performAction("terminal", "Terminal", parentComponent);
        if (selectedApp == null) {
            return null; // User canceled
        }
        
        if (selectedApp.startsWith("external:")) {
            // Launch external terminal
            String externalTerminal = selectedApp.substring("external:".length());
            launchExternalTerminal(externalTerminal);
            return null;
        } else {
            // Create internal terminal window
            String windowId = "terminal-" + System.currentTimeMillis();
            return createWindow(windowId, title != null ? title : "Terminal", "console");
        }
    }
    
    /**
     * Launches an external terminal application
     * 
     * @param terminalApp Terminal application name
     */
    private void launchExternalTerminal(String terminalApp) {
        try {
            ProcessBuilder pb = new ProcessBuilder(terminalApp);
            pb.inheritIO(); // Redirect I/O to console
            pb.start();
        } catch (IOException e) {
            System.err.println("Error launching external terminal: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the application association manager
     * 
     * @return Application association manager instance
     */
    public ApplicationAssociationManager getAppAssociationManager() {
        return appAssociationManager;
    }

    /**
     * Updates the Look and Feel for all windows
     */
    public void updateLookAndFeel() {
        for (JInternalFrame frame : desktop.getAllFrames()) {
            SwingUtilities.updateComponentTreeUI(frame);
            // Revalidate and repaint to ensure proper rendering
            frame.revalidate();
            frame.repaint();
        }
        // Update the desktop pane itself
        SwingUtilities.updateComponentTreeUI(desktop);
        desktop.revalidate();
        desktop.repaint();
    }

    /**
     * Recreates a window with the same type and properties
     */
    public JInternalFrame recreateWindow(String windowId, String title) {
        // Get the original window to determine its type
        JInternalFrame oldFrame = windows.get(windowId);
        if (oldFrame == null) return null;

        // Determine window type from client properties
        String type = "default";
        if (oldFrame.getClientProperty("console") != null) type = "console";
        else if (oldFrame.getClientProperty("editor") != null) type = "texteditor";
        else if (oldFrame.getClientProperty("webviewer") != null) type = "webviewer";
        
        // Create new window
        return createWindow(windowId, title, type);
    }

    /**
     * Updates the desktop background
     */
    public void updateBackground(String background) {
        for (Component comp : desktop.getComponents()) {
            if (comp instanceof BackgroundPanel) {
                desktop.remove(comp);
                BackgroundPanel newBg = new BackgroundPanel(background);
                desktop.add(newBg, JLayeredPane.FRAME_CONTENT_LAYER);
                desktop.setLayer(newBg, JLayeredPane.DEFAULT_LAYER);
                newBg.setBounds(0, 0, desktop.getWidth(), desktop.getHeight());
                desktop.revalidate();
                desktop.repaint();
                break;
            }
        }
    }

    /**
     * Launches a MicrOS application in a new window.
     *
     * @param app The MicrOSApp instance to launch
     * @return The created internal frame
     */
    public JInternalFrame launchApp(MicrOSApp app) {
        String windowId = "app-" + System.currentTimeMillis();
        String title = app.getManifest() != null ? app.getManifest().getName() : "Application";
        JInternalFrame frame = createBaseFrame(title);
        
        try {
            app.onStart(); // Call lifecycle method
            JComponent ui = app.createUI();
            frame.add(ui);
            frame.putClientProperty("app", app);
            
            // Add frame listener to handle app lifecycle
            frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
                @Override
                public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
                    app.onStop(); // Call lifecycle method when window is closing
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            frame.add(new JLabel("Error launching app: " + e.getMessage()));
        }

        desktop.add(frame);
        windows.put(windowId, frame);
        frame.setVisible(true);
        
        return frame;
    }

    /**
     * Launches an app by its identifier
     */
    public JInternalFrame launchAppById(String identifier) {
        try {
            if (identifier == null || identifier.isEmpty()) {
                ErrorDialog.showError(desktop, "Invalid app identifier", 
                    new IllegalArgumentException("App identifier cannot be null or empty"));
                return null;
            }
            
            // Check if app is already running
            for (JInternalFrame frame : windows.values()) {
                MicrOSApp existingApp = (MicrOSApp) frame.getClientProperty("app");
                if (existingApp != null && 
                    existingApp.getManifest() != null && 
                    identifier.equals(existingApp.getManifest().getIdentifier())) {
                    frame.toFront();
                    return frame;
                }
            }
            
            MicrOSApp app = vfs.getAppLoader().createAppInstance(identifier);
            if (app == null) {
                ErrorDialog.showError(desktop, "Failed to create app instance", 
                    new RuntimeException("Could not create instance of app: " + identifier));
                return null;
            }
            
            AppManifest manifest = vfs.getAppLoader().getLoadedApps()
                .stream()
                .filter(m -> identifier.equals(m.getIdentifier()))
                .findFirst()
                .orElse(null);
                
            if (manifest != null) {
                app.setManifest(manifest);
            }
            
            app.initialize(this, vfs);
            
            // Create window and start app
            JInternalFrame frame = createBaseFrame(app.getManifest().getName());
            JComponent ui = app.createUI();
            frame.add(ui);
            frame.putClientProperty("app", app);
            
            // Start the app in a managed thread
            int threadId = processManager.startAppThread(() -> {
                try {
                    app.onStart();
                } catch (Exception e) {
                    reportError("Error starting app", e, identifier);
                }
            }, identifier);  // Pass appId instead of name
            
            app.setThreadId(threadId);
            
            // Add window cleanup on close
            frame.addInternalFrameListener(new InternalFrameAdapter() {
                @Override
                public void internalFrameClosing(InternalFrameEvent e) {
                    app.onStop();
                    app.setThreadId(-1);
                    processManager.killAppThread(threadId);
                }
            });
            
            frame.setVisible(true);
            desktop.add(frame);
            windows.put("app-" + System.currentTimeMillis(), frame);
            return frame;
            
        } catch (Exception e) {
            reportError("Failed to launch application", e, identifier);
            return null;
        }
    }

    // Add method to launch native apps
    public JInternalFrame launchNativeApp(String command) {
        try {
            String windowId = "native-" + System.currentTimeMillis();
            String title = "Native App: " + command;
            return createWindow(windowId, title, "native");
        } catch (Exception e) {
            ErrorDialog.showError(desktop, "Failed to launch native application", e);
            return null;
        }
    }

    public boolean isAppThreadRunning(int threadId) {
        return processManager.isThreadRunning(threadId);
    }

    public void stopAppThread(int threadId) {
        processManager.killAppThread(threadId);
    }

    private void reportError(String message, Exception e, String appId) {
        ErrorDialog.showError(desktop, message + (appId != null ? " [" + appId + "]" : ""), e);
    }

    /**
     * Factory interface for creating window instances.
     */
    @FunctionalInterface
    public interface WindowFactory {
        /**
         * Creates a new window instance.
         *
         * @param windowId Unique window identifier
         * @param title Window title
         * @return Created window frame
         */
        JInternalFrame createWindow(String windowId, String title);
    }

    /**
     * Gets the desktop pane managed by this WindowManager
     * @return JDesktopPane instance
     */
    public JDesktopPane getDesktop() {
        return desktop;
    }

    public void registerStartupApp(String appId) {
        startupApps.add(appId);
    }

    public void registerStartupWindow(String windowId, String type) {
        startupWindows.put(windowId, type);
    }

    public void initializeStartupItems() {
        // Launch registered startup apps
        for (String appId : startupApps) {
            try {
                launchAppById(appId);
            } catch (Exception e) {
                reportError("Failed to launch startup app: " + appId, e, null);
            }
        }

        // Create registered startup windows
        for (Map.Entry<String, String> entry : startupWindows.entrySet()) {
            try {
                createWindow(entry.getKey(), entry.getKey(), entry.getValue());
            } catch (Exception e) {
                reportError("Failed to create startup window: " + entry.getKey(), e, null);
            }
        }
    }

    public JInternalFrame launchAppWithIntent(Intent intent) {
        try {
            JInternalFrame frame = launchAppById(intent.getTargetAppId());
            if (frame != null) {
                MicrOSApp app = (MicrOSApp) frame.getClientProperty("app");
                if (app != null) {
                    app.handleIntent(intent);
                }
            }
            return frame;
        } catch (Exception e) {
            reportError("Failed to launch app with intent", e, intent.getTargetAppId());
            return null;
        }
    }

    /**
     * Launches a JavaFX-based application in a new window.
     *
     * @param app The JavaFXApp instance to launch
     * @return The created internal frame
     */
    public JInternalFrame launchJavaFXApp(JavaFXApp app) {
        String windowId = "app-" + System.currentTimeMillis();
        String title = app.getManifest() != null ? app.getManifest().getName() : "JavaFX Application";
        
        JInternalFrame frame = createBaseFrame(title);
        try {
            app.onStart();
            JComponent ui = app.createUI();
            frame.add(ui);
            frame.putClientProperty("app", app);
            
            frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
                @Override
                public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
                    app.onStop();
                    if (desktop != null) {
                        desktop.remove(frame);
                        desktop.repaint();
                    }
                }
            });
            
            desktop.add(frame);
            windows.put(windowId, frame);
            frame.setVisible(true);
            return frame;
        } catch (Exception e) {
            reportError("Failed to launch JavaFX application", e, app.getManifest().getIdentifier());
            return null;
        }
    }
}
