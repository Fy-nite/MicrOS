package org.Finite.MicrOS.ui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

/**
 * A wrapper around JFXPanel that simplifies using JavaFX in Swing applications
 */
public class JavaFXPanel extends JPanel {
    
    private final JFXPanel jfxPanel;
    private Scene scene;
    
    /**
     * Creates a new JavaFX panel
     */
    public JavaFXPanel() {
        setLayout(new BorderLayout());
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);
    }
    
    /**
     * Sets the JavaFX scene to display
     * 
     * @param scene The JavaFX scene
     */
    public void setScene(Scene scene) {
        this.scene = scene;
        jfxPanel.setScene(scene);
    }
    
    /**
     * Gets the current JavaFX scene
     * 
     * @return The current scene
     */
    public Scene getScene() {
        return scene;
    }
    
    /**
     * Gets the underlying JFXPanel
     * 
     * @return The JFXPanel instance
     */
    public JFXPanel getJFXPanel() {
        return jfxPanel;
    }
    
    /**
     * Runs a task on the JavaFX application thread
     * 
     * @param runnable The task to run
     */
    public void runInJavaFXThread(Runnable runnable) {
        Platform.runLater(runnable);
    }
    
    /**
     * Creates a default empty scene with a StackPane root
     * 
     * @return A new Scene with StackPane root
     */
    public Scene createDefaultScene() {
        StackPane root = new StackPane();
        Scene scene = new Scene(root);
        setScene(scene);
        return scene;
    }
}