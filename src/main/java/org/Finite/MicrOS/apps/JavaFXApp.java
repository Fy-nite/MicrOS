package org.Finite.MicrOS.apps;

import javafx.scene.layout.Region;
import javax.swing.JComponent;
import org.Finite.MicrOS.ui.JavaFXPanel;

/**
 * Base class for JavaFX-based MicrOS applications.
 * Provides a bridge between the MicrOS app framework and JavaFX UI components.
 */
public abstract class JavaFXApp extends MicrOSApp {
    private JavaFXPanel fxPanel;
    
    @Override
    public final JComponent createUI() {
        fxPanel = new JavaFXPanel();
        Region root = createFXContent();
        // fxPanel.setContent(root); 
        return fxPanel;
    }
    
    /**
     * Create the JavaFX content for this application.
     * Implement this method instead of createUI().
     * @return The root node of the JavaFX scene
     */
    protected abstract Region createFXContent();
    
    /**
     * Get the JavaFXPanel instance for this app
     * @return The JavaFXPanel
     */
    protected JavaFXPanel getFXPanel() {
        return fxPanel;
    }
}