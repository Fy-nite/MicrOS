# JavaFX Apps in MicrOS

MicrOS now supports creating JavaFX-based applications while maintaining full compatibility with the existing Swing-based app framework. This document explains how to create and use JavaFX apps within MicrOS.

## Overview

The JavaFX integration in MicrOS allows developers to:
- Create pure JavaFX applications
- Mix JavaFX and Swing components in the same app
- Use all existing MicrOS features (window management, app lifecycle, etc.)
- Maintain compatibility with existing Swing apps

## Creating a JavaFX App

To create a JavaFX app, extend the `JavaFXApp` base class instead of directly extending `MicrOSApp`:

```java
public class MyJavaFXApp extends JavaFXApp {
    @Override
    protected Region createFXContent() {
        // Create and return your JavaFX UI here
        VBox root = new VBox(10);
        root.getChildren().add(new Label("Hello from JavaFX!"));
        return root;
    }

    @Override
    public void onStart() {
        // Initialize resources
    }

    @Override
    public void onStop() {
        // Cleanup resources
    }
}
```

### Key Components

1. **JavaFXApp Base Class**
   - Extends MicrOSApp
   - Handles JavaFX initialization
   - Manages the JavaFX-Swing bridge
   - Provides seamless integration with the window system

2. **JavaFXPanel**
   - Wrapper around JavaFX's JFXPanel
   - Handles JavaFX scene management
   - Provides smooth integration with Swing containers

## Creating Hybrid Apps

You can mix JavaFX and Swing components in the same app by extending `MicrOSApp` and using `JavaFXPanel`:

```java
public class HybridApp extends MicrOSApp {
    @Override
    public JComponent createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Add Swing components
        mainPanel.add(new JButton("Swing Button"), BorderLayout.NORTH);
        
        // Add JavaFX components
        JavaFXPanel fxPanel = new JavaFXPanel();
        Platform.runLater(() -> {
            VBox fxContent = new VBox(10);
            fxContent.getChildren().add(new Button("JavaFX Button"));
            fxPanel.setContent(fxContent);
        });
        mainPanel.add(fxPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
}
```

## Best Practices

1. **Thread Safety**
   - Use `Platform.runLater()` for JavaFX UI updates
   - Use `SwingUtilities.invokeLater()` for Swing UI updates
   - Keep UI operations on their respective threads

2. **Resource Management**
   - Initialize JavaFX resources in `onStart()`
   - Clean up resources in `onStop()`
   - Properly dispose of heavy resources

3. **Scene Creation**
   - Return a `Region` from `createFXContent()`
   - Use layout managers appropriate for your UI
   - Consider using FXML for complex UIs

## Example: Text Editor

Here's a complete example of a JavaFX-based text editor:

```java
public class TextEditorFX extends JavaFXApp {
    private TextArea textArea;
    
    public TextEditorFX() {
        AppManifest manifest = new AppManifest();
        manifest.setName("JavaFX Text Editor");
        manifest.setIdentifier("org.finite.micros.texteditor.fx");
        setManifest(manifest);
    }
    
    @Override
    protected Region createFXContent() {
        BorderPane root = new BorderPane();
        
        // Create menu
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem saveItem = new MenuItem("Save");
        fileMenu.getItems().add(saveItem);
        menuBar.getMenus().add(fileMenu);
        
        // Create editor
        textArea = new TextArea();
        
        root.setTop(menuBar);
        root.setCenter(textArea);
        
        return root;
    }
}
```

## Launching JavaFX Apps

You can launch JavaFX apps using the regular MicrOS window management system:

```java
// Using WindowManager
windowManager.launchAppById("org.finite.micros.myjavafxapp");

// Or using the specialized method
JavaFXApp app = new MyJavaFXApp();
windowManager.launchJavaFXApp(app);
```

## Technical Details

### Initialization
- JavaFX platform is initialized automatically by WindowManager
- Each JavaFX app runs in its own Scene
- JavaFX content is bridged to Swing using JFXPanel

### Window Management
- JavaFX apps use the same window management as Swing apps
- Windows can be minimized, maximized, and restored
- All window operations work seamlessly with JavaFX content

### Performance Considerations
- JavaFX and Swing components can have different rendering pipelines
- Use appropriate layout managers to prevent reflow issues
- Consider using JavaFX's CSS styling for consistent look and feel

## Limitations

1. **Modality**
   - JavaFX dialogs should use SwingUtilities.getWindowAncestor()
   - Prefer Swing dialogs for system-wide modal dialogs

2. **Threading**
   - Must respect JavaFX's threading model
   - UI updates must be done on the JavaFX Application Thread

3. **Memory Usage**
   - JavaFX runtime adds additional memory overhead
   - Consider this when creating many JavaFX windows