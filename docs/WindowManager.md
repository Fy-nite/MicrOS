# WindowManager.java

The `WindowManager` class manages the creation, tracking, and manipulation of windows in the MicrOS desktop environment. It provides factory-based window creation and management of console windows.

## Key Methods

- `WindowManager(JDesktopPane desktop, VirtualFileSystem vfs)`: Constructor that initializes the WindowManager with the given desktop pane and virtual file system.
- `registerDefaultFactories()`: Registers the default window factory implementations.
- `registerWindowFactory(String type, WindowFactory factory)`: Registers a new window factory for creating windows of a specific type.
- `setTaskbar(Taskbar taskbar)`: Sets the taskbar reference for window tracking.
- `createBaseFrame(String title)`: Creates a basic internal frame with default settings.
- `createWindow(String windowId, String title, String type)`: Creates a new window using the specified factory type.
- `getWindow(String windowId)`: Retrieves a window by its ID.
- `writeToConsole(String windowId, String text)`: Writes text to a console window.
- `clearConsole(String windowId)`: Clears the content of a console window.
- `getEditorText(String windowId)`: Gets text content from a text editor window.
- `closeWindow(String windowId)`: Closes and disposes a specific window.
- `closeAllWindows()`: Closes all windows managed by this WindowManager.
- `registerExecutableFileType(String extension, String windowType)`: Registers a new executable file type and its associated window type.
- `runExecutable(String virtualPath)`: Runs an executable file by creating a window of the associated type.
- `setWebViewerUrl(String windowId, String url)`: Sets the URL for a web viewer window.
- `registerFileAssociation(String extension, String windowType)`: Registers a new file association for a specific file extension and window type.
- `getFileAssociations(String extension)`: Retrieves the set of window types associated with a specific file extension.
- `openFileWith(String virtualPath, String windowType)`: Opens a file with a specific window type.
- `updateLookAndFeel()`: Updates the Look and Feel for all windows.
- `recreateWindow(String windowId, String title)`: Recreates a window with the same type and properties.
- `updateBackground(String background)`: Updates the desktop background.
- `launchApp(MicrOSApp app)`: Launches a MicrOS application in a new window.
- `launchAppById(String identifier)`: Launches an app by its identifier.
- `launchNativeApp(String command)`: Launches a native application.
- `isAppThreadRunning(int threadId)`: Checks if a specific thread ID is still running.
- `stopAppThread(int threadId)`: Stops a specific application thread.
- `registerStartupApp(String appId)`: Registers a startup application.
- `registerStartupWindow(String windowId, String type)`: Registers a startup window.
- `initializeStartupItems()`: Initializes and launches registered startup items.
- `launchAppWithIntent(Intent intent)`: Launches an app with a specific intent.

## Usage

The `WindowManager` class is used internally by the MicrOS system to manage windows. It is not typically used directly by applications.
