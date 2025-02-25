package org.Finite.MicrOS.apps.template;

import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import org.Finite.MicrOS.apps.JavaFXApp;
import org.Finite.MicrOS.apps.AppManifest;

/**
 * Template for creating new JavaFX-based MicrOS applications.
 * Copy this file and modify it to create your own app.
 */
public class JavaFXAppTemplate extends JavaFXApp {
    
    public JavaFXAppTemplate() {
        // Set up app manifest
        AppManifest manifest = new AppManifest();
        manifest.setName("My JavaFX App");
        manifest.setIdentifier("org.finite.micros.myapp");
        manifest.setDescription("Description of my app");
        setManifest(manifest);
    }
    
    @Override
    protected Region createFXContent() {
        // Create the root layout container
        VBox root = new VBox(10); // 10 pixels spacing between elements
        root.setStyle("-fx-padding: 10; -fx-background-color: #2d2d2d;");
        
        // Add some UI elements
        Label title = new Label("My JavaFX App");
        title.setStyle("-fx-font-size: 18; -fx-text-fill: #e6e6e6;");
        
        Button button = new Button("Click Me");
        button.setOnAction(e -> handleButtonClick());
        
        // Add elements to root
        root.getChildren().addAll(title, button);
        
        return root;
    }
    
    private void handleButtonClick() {
        // Handle button click event
        System.out.println("Button clicked!");
    }
    
    @Override
    public void onStart() {
        // Initialize your app's resources here
    }
    
    @Override
    public void onStop() {
        // Clean up resources when the app is closed
    }
}