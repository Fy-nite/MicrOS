package org.Finite.MicrOS.apps;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

import javax.swing.JComponent;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.core.VirtualFileSystem;

/**
 * Base class for JavaFX-based MicrOS applications
 */
public abstract class JavaFXApp extends MicrOSApp {
    
    private JFXPanel jfxPanel;
    private WindowManager windowManager;
    private VirtualFileSystem vfs;
    
    @Override
    public void initialize(WindowManager windowManager, VirtualFileSystem vfs) {
        this.windowManager = windowManager;
        this.vfs = vfs;
        
        // Initialize JavaFX if not already initialized
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Platform already initialized, ignore
        }
        
        // Create the JFXPanel that will host the JavaFX content
        jfxPanel = new JFXPanel();
    }
    
    @Override
    public JComponent createUI() {
        // Create JavaFX content on the JavaFX Application Thread
        Platform.runLater(() -> {
            Region root = createFXContent();
            Scene scene = new Scene(root);
            jfxPanel.setScene(scene);
        });
        
        return jfxPanel;
    }
    
    /**
     * This method must be implemented by subclasses to provide 
     * the JavaFX content for the application
     * 
     * @return The root JavaFX Region (e.g., BorderPane, VBox, etc.)
     */
    protected abstract Region createFXContent();
    
    /**
     * Get the window manager instance
     * 
     * @return The window manager
     */
    protected WindowManager getWindowManager() {
        return this.windowManager;
    }
    
    /**
     * Get the virtual file system instance
     * 
     * @return The virtual file system
     */
    protected VirtualFileSystem getVFS() {
        return this.vfs;
    }
}