# MicrOS Demo App Template

This is a simple template app demonstrating how to create applications for the MicrOS platform.

## Features

- Basic UI with a label and button
- Demonstrates app lifecycle methods
- Shows how to handle user interactions
- Includes proper manifest configuration

## Getting Started

1. Copy this entire directory to create your new app
2. Update the following files:
   - `pom.xml`: Change groupId, artifactId, and other Maven coordinates
   - `manifest.json`: Update app metadata (name, identifier, description, etc.)
   - Rename and modify source files in `Contents/src`

## Project Structure

```
Demo.app/
├── Contents/
│   ├── manifest.json     # App metadata and configuration
│   ├── src/             # Source code directory, remember to remove if not needed
│   │   └── DemoApp.java  # Main app class
│   └── resources/       # Resources (icons, etc.)
├── pom.xml              # Maven build configuration
└── README.md           # This file
```

## Building

To build the app:

```bash
mvn clean package
```

The built app will be available in `Contents/Resources/`.

## Best Practices

1. Use the MicrOSApp base class for your main app class
2. Handle app lifecycle in onStart() and onStop()
3. Keep UI creation code in createUI()
4. Request only needed permissions in manifest.json

## Dependencies

- Java 17 or higher
- MicrOS Core Library
- Maven for building

## License

[Your license here]
