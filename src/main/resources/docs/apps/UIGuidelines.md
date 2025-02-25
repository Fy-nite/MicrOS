# MicrOS UI Design Guidelines

## Core Principles
- Consistency with system look and feel
- Responsive and intuitive interfaces
- Efficient use of screen space
- Clear visual hierarchy

## Layout Guidelines
1. Use standard margins (10px)
2. Follow grid-based layouts
3. Maintain proper component spacing
4. Use BorderLayout for main window structure

## Component Usage
### Recommended Components
```java
// Example layout structure
JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

// Content area
JPanel contentPanel = new JPanel(new GridBagLayout());
// ... add components with GridBagConstraints

// Toolbar area
JToolBar toolbar = new JToolBar();
toolbar.setFloatable(false);
```

## Color Guidelines
- Use system colors when possible:
```java
Color background = UIManager.getColor("Panel.background");
Color foreground = UIManager.getColor("Panel.foreground");
```

## Typography
- Default system fonts for consistency
- Minimum text size: 12pt
- Use proper font weights for hierarchy

## Icons and Images
- Standard sizes: 16x16, 24x24, 32x32
- Support high DPI displays
- Use vector graphics when possible
- Follow system icon style

## Accessibility
- Include keyboard shortcuts
- Support screen readers
- Maintain proper contrast ratios
- Add tooltips to controls

## Responsive Design
- Handle window resizing gracefully
- Use appropriate layout managers
- Support minimum window sizes
- Scale UI elements appropriately

## JavaFX Integration Guidelines

### Component Choice
- Use JavaFX when you need modern UI controls and better styling options
- Use Swing when you need deeper integration with the MicrOS desktop environment
- Consider hybrid approach for complex apps that need both

### Visual Consistency
- Match JavaFX component styling with MicrOS theme
- Use consistent colors and fonts across Swing and JavaFX
- Follow these color guidelines for JavaFX:
  ```css
  .root {
      -fx-background-color: #1e1e1e;
      -fx-text-fill: #e6e6e6;
  }
  .button {
      -fx-background-color: #2d2d2d;
      -fx-text-fill: #e6e6e6;
  }
  .button:hover {
      -fx-background-color: #3c3c3c;
  }
  ```

### Performance
- Lazy-load JavaFX components when possible
- Use JavaFX's CSS instead of programmatic styling
- Cache frequently used nodes
- Use appropriate layout managers (e.g., VBox, HBox) instead of complex custom layouts

### Thread Management
- Always use Platform.runLater() for JavaFX UI updates
- Always use SwingUtilities.invokeLater() for Swing UI updates
- Keep heavy processing off both the JavaFX and Swing EDT

### Resource Management
- Dispose of JavaFX resources in onStop()
- Use weak references for event listeners
- Clean up any Platform.runLater() scheduled tasks

### Hybrid App Structure
```
root (JPanel)
├── swingComponents (JPanel)
│   └── Native Swing components
├── javaFXPanel (JavaFXPanel)
│   └── JavaFX scene graph
└── sharedResources
    └── Common resources
