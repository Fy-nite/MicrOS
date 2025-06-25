package org.Finite.MicrOS.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javax.swing.*;

import org.Finite.MicrOS.core.VirtualFileSystem;

import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class WebViewer extends JPanel {
    private final JFXPanel jfxPanel;
    private WebView webView;
    private WebEngine webEngine;
    private final VirtualFileSystem vfs;
    private TextField urlField;
    private String currentLocation;

    public WebViewer(VirtualFileSystem vfs) {
        this.vfs = vfs;
        setLayout(new BorderLayout());

        // Create Swing toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        add(toolbar, BorderLayout.NORTH);

        // Initialize JavaFX
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        // Initialize JavaFX components on JavaFX thread
        Platform.runLater(() -> {
            initializeJavaFX();
        });
    }

    private void initializeJavaFX() {
        // Create the WebView and WebEngine
        webView = new WebView();
        webEngine = webView.getEngine();

        // Create toolbar controls
        BorderPane borderPane = new BorderPane();
        HBox toolbar = new HBox(5);
        toolbar.setStyle("-fx-padding: 5; -fx-background-color: #f0f0f0;");

        Button backButton = new Button("←");
        Button forwardButton = new Button("→");
        Button refreshButton = new Button("⟳");
        urlField = new TextField();
        
        // Set button actions
        backButton.setOnAction(e -> webEngine.getHistory().go(-1));
        forwardButton.setOnAction(e -> webEngine.getHistory().go(1));
        refreshButton.setOnAction(e -> webEngine.reload());
        urlField.setOnAction(e -> loadUrl(urlField.getText()));

        // Make URL field expand
        HBox.setHgrow(urlField, javafx.scene.layout.Priority.ALWAYS);

        // Add controls to toolbar
        toolbar.getChildren().addAll(backButton, forwardButton, refreshButton, urlField);

        // Layout
        borderPane.setTop(toolbar);
        borderPane.setCenter(webView);

        // Create scene
        Scene scene = new Scene(borderPane);
        jfxPanel.setScene(scene);

        // Add web engine listeners
        webEngine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc != null) {
                currentLocation = newLoc;
                Platform.runLater(() -> urlField.setText(newLoc));
            }
        });
    }

    public void loadUrl(String url) {
        Platform.runLater(() -> {
            try {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    webEngine.load(url);
                } else {
                    // Load local file from VFS
                    String virtualPath = url.startsWith("/") ? url : "/" + url;
                    byte[] content = vfs.readFile(virtualPath);
                    String html = new String(content, StandardCharsets.UTF_8);
                    
                    // Create base URL for relative paths
                    Path fullPath = vfs.resolveVirtualPath(virtualPath);
                    Path parentPath = fullPath.getParent();
                    String baseUrl = parentPath.toUri().toString();
                    
                    webEngine.loadContent(html, "text/html");
                    // Set base URL for relative paths
                    webEngine.setUserStyleSheetLocation(baseUrl);
                }
            } catch (Exception e) {
                webEngine.loadContent(
                    "<html><body><h2>Error</h2><p style='color: red'>" + 
                    e.getMessage() + "</p></body></html>", 
                    "text/html"
                );
            }
        });
    }

    public void loadContent(String html) {
        Platform.runLater(() -> {
            webEngine.loadContent(html, "text/html");
        });
    }
}
