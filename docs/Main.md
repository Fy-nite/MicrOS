# Main.java

The `Main.java` file is the entry point for the MicrOS desktop environment. It initializes the system, sets up the look and feel, and launches the desktop environment.

## Key Methods

- `main(String[] args)`: The main method that sets the look and feel and launches the desktop environment.
- `initializeFilesystem(String configPath)`: Initializes the virtual file system with the specified configuration path.
- `startConsoleMode()`: Starts the console-only mode (not yet implemented).
- `isAndroid()`: Checks if the operating system is Android.
- `Desktopenviroment()`: Initializes and displays the desktop environment.
- `createMainWindow()`: Creates the main window for the desktop environment.
- `initiateShutdown()`: Initiates the shutdown process for the desktop environment.
- `initializeSystem()`: Initializes the system components and launches startup applications.
- `registerStandardApps(Taskbar taskbar)`: Registers standard applications with the taskbar.
- `launchStartupApps()`: Launches startup applications.
- `launchStandaloneApp(String appId)`: Launches a standalone application by its ID.
- `launchStandaloneAppFromPath(String appPath)`: Launches a standalone application from a specified path.

## Usage

To launch the MicrOS desktop environment, run the `Main` class with the appropriate command-line arguments. For example:

```
java -cp MicrOS.jar org.Finite.MicrOS.Main
```
