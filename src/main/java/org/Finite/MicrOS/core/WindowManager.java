/**
 * Manages the creation, tracking and manipulation of windows in the MicrOS desktop environment.
 * Provides factory-based window creation and management of console windows.
 */
package org.Finite.MicrOS.core;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;

import org.Finite.MicrOS.Desktop.BackgroundPanel;
import org.Finite.MicrOS.Desktop.Taskbar;
import org.Finite.MicrOS.Files.FileManager;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.ui.Console;
import org.Finite.MicrOS.ui.SettingsDialog;
import org.Finite.MicrOS.ui.WebViewer;

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

        // Update text editor factory to use app
        registerWindowFactory("texteditor", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            try {
                MicrOSApp app = vfs.getAppLoader().createAppInstance("org.finite.texteditor");
                app.initialize(this, vfs);
                JComponent ui = app.createUI();
                frame.add(ui);
                frame.putClientProperty("app", app);
            } catch (Exception e) {
                e.printStackTrace();
                frame.add(new JLabel("Error loading Text Editor app"));
            }
            return frame;
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

        // Add settings window factory
        registerWindowFactory("settings", (windowId, title) -> {
            JInternalFrame frame = createBaseFrame(title);
            frame.setResizable(false);
            frame.add(new SettingsDialog(this));
            return frame;
        });

        // Register default file associations
        registerFileAssociation("txt", "texteditor");
        registerFileAssociation("md", "texteditor");
        registerFileAssociation("java", "texteditor");
        registerFileAssociation("masm", "texteditor");
        registerFileAssociation("asm", "texteditor");
        
        registerFileAssociation("png", "imageviewer");
        registerFileAssociation("jpg", "imageviewer");
        registerFileAssociation("jpeg", "imageviewer");
        registerFileAssociation("gif", "imageviewer");
        
        registerFileAssociation("html", "webviewer");
        registerFileAssociation("htm", "webviewer");
        
        // Allow text files to be opened in webviewer too
        registerFileAssociation("html", "texteditor");
        registerFileAssociation("htm", "texteditor");
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
        System.out.println("Creating window: " + windowId + ", Title: " + title + ", Type: " + type);
        WindowFactory factory = windowFactories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown window type: " + type);
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
            frame.dispose();
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
     * Opens a file with a specific window type.
     *
     * @param virtualPath Path to the file in the VFS
     * @param windowType Window type identifier
     */
    public void openFileWith(String virtualPath, String windowType) {
        String windowId = windowType + "-" + virtualPath.hashCode();
        JInternalFrame frame = createWindow(windowId, virtualPath, windowType);
        
        switch (windowType) {
            case "texteditor":
                try {
                    String content = new String(vfs.readFile(virtualPath));
                    MicrOSApp app = (MicrOSApp) frame.getClientProperty("app");
                    if (app != null) {
                        app.getClass().getMethod("setText", String.class).invoke(app, content);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
                
            case "webviewer":
                WebViewer webViewer = (WebViewer) frame.getClientProperty("webviewer");
                if (webViewer != null) {
                    webViewer.loadUrl(virtualPath);
                }
                break;
                
            case "imageviewer":
                // TODO: Implement image viewer loading
                break;
        }
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
}
