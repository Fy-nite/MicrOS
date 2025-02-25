package org.Finite.MicrOS.apps.javafx;

import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.Finite.MicrOS.apps.JavaFXApp;
import org.Finite.MicrOS.apps.AppManifest;

public class TextEditorFX extends JavaFXApp {
    private TextArea textArea;
    private String currentContent = "";
    
    public TextEditorFX() {
        AppManifest manifest = new AppManifest();
        manifest.setName("Text Editor FX");
        manifest.setIdentifier("org.finite.micros.texteditor.fx");
        manifest.setDescription("JavaFX-based text editor");
        setManifest(manifest);
    }

    @Override
    protected Region createFXContent() {
        BorderPane root = new BorderPane();
        
        // Create menu bar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> save());
        fileMenu.getItems().add(saveItem);
        menuBar.getMenus().add(fileMenu);
        
        // Create text area
        textArea = new TextArea();
        textArea.setText(currentContent);
        textArea.setWrapText(true);
        
        root.setTop(menuBar);
        root.setCenter(textArea);
        
        return root;
    }
    
    public void setText(String text) {
        currentContent = text;
        if (textArea != null) {
            textArea.setText(text);
        }
    }
    
    public String getText() {
        return textArea != null ? textArea.getText() : currentContent;
    }
    
    private void save() {
        // Add save functionality
        currentContent = textArea.getText();
        // TODO: Implement actual file saving
    }

    @Override
    public void onStart() {
        // Initialize any resources or state
    }

    @Override
    public void onStop() {
        // Clean up resources
    }
}