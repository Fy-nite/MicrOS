# MicrOS System APIs

## Core Services

### Window Management
```java
// Get window manager instance
WindowManager wm = getWindowManager();

// Create new window
JInternalFrame window = wm.createWindow("unique-id", "Window Title", "window-type");

// Access virtual filesystem
VirtualFileSystem vfs = VirtualFileSystem.getInstance();
```

### File System Access
```java
// Read file
byte[] data = vfs.readFile("/path/to/file");

// Write file
vfs.writeFile("/path/to/file", data);

// List directory
List<String> files = vfs.listFiles("/path");
```

### Process Management
```java
// Get process manager
ProcessManager pm = ProcessManager.getInstance();

// Start process
int pid = pm.startProcess("command");

// Monitor thread
boolean isRunning = pm.isThreadRunning(threadId);
```

### Settings Management
```java
// Access settings
Settings settings = Settings.getInstance();
String theme = settings.getTheme();
```

## Security and Permissions

### Permission Types
- `fileSystem`: File read/write access
- `network`: Network access
- `shell`: Command execution

### Requesting Permissions
Add to manifest.json:
```json
"permissions": {
    "fileSystem": true,
    "network": false,
    "shell": false
}
```
