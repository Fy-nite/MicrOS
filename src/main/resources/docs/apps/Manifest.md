# Creating Apps for MicrOS

## Application Bundle Structure
MicrOS apps follow a bundle structure similar to macOS:

```
YourApp.app/
├── Contents/
│   ├── manifest.json       # App manifest file
│   ├── src/               # Source code directory
│   │   └── org/your/package/
│   │       └── Main.java  # Main app class
│   └── Resources/         # Resources directory
│       ├── icon.png       # App icon
│       └── *.jar          # Compiled app JAR
```

## Manifest File Structure
The manifest.json file defines your app's properties and requirements:

```json
{
    "name": "Your App Name",
    "identifier": "org.your.unique.identifier",
    "version": "1.0.0",
    "mainClass": "org.your.package.Main",
    "description": "Description of your app",
    "category": "Category",
    "icon": "resources/icon.png",
    "appType": "custom",
    "pinToTaskbar": false,
    "startOnLaunch": false,
    "authors": ["Your Name"],
    "minimumOSVersion": "1.0",
    "supportedFileTypes": ["txt", "json"],
    "permissions": {
        "fileSystem": false,
        "network": false,
        "shell": false
    }
}
```

### Manifest Fields
- `name`: Display name of your app
- `identifier`: Unique identifier (reverse domain format)
- `version`: App version following semver
- `mainClass`: Full class path to your app's main class
- `description`: Short description of your app
- `category`: App category (System, Utilities, Development, etc.)
- `icon`: Path to app icon (relative to Resources)
- `appType`: Type of app (custom, console, etc.)
- `pinToTaskbar`: Whether to pin to taskbar by default
- `startOnLaunch`: Whether to auto-start with OS
- `authors`: List of app authors
- `minimumOSVersion`: Minimum required MicrOS version
- `supportedFileTypes`: File extensions this app can open
- `permissions`: Required system permissions

## Creating Your First App

1. Create the app bundle structure:
```bash
mkdir -p MyApp.app/Contents/{src,Resources}
```

2. Create the manifest.json file

3. Create your main app class extending MicrOSApp:
```java
package org.your.package;

import javax.swing.*;
import org.Finite.MicrOS.apps.MicrOSApp;

public class Main extends MicrOSApp {
    @Override
    public JComponent createUI() {
        // Create and return your app's UI
        return new JPanel();
    }

    @Override 
    public void onStart() {
        // Initialize app
    }

    @Override
    public void onStop() {
        // Cleanup
    }
}
```

4. Create a Maven pom.xml for your app:
```xml
<project>
    <groupId>org.your.group</groupId>
    <artifactId>your-app</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <dependency>
            <groupId>org.Finite</groupId>
            <artifactId>MicrOS</artifactId>
            <version>1.0-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    
    <build>
        <sourceDirectory>Contents/src</sourceDirectory>
        <outputDirectory>Contents/Resources</outputDirectory>
    </build>
</project>
```

5. Build your app:
```bash
mvn clean package
```

## App Lifecycle
- `createUI()`: Called when app window is created
- `onStart()`: Called when app starts
- `onStop()`: Called when app closes

## Best Practices
- Use meaningful identifiers
- Include proper error handling
- Clean up resources in onStop()
- Request minimum required permissions
- Follow UI design guidelines
- Include app documentation
- Test thoroughly before distribution

## Distribution
Apps can be distributed as .app bundles and installed by copying to:
```
/media/cat/MicrOS/filesystem/apps/
```

MicrOS will automatically detect and load new apps on next startup.
