# VirtualFileSystem.java

The `VirtualFileSystem` class provides a virtual file system for managing files and directories within MicrOS. It supports file creation, reading, writing, and deletion, as well as MIME type and icon management.

## Key Methods

- `VirtualFileSystem()`: Private constructor that initializes the virtual file system.
- `getMimeType(String virtualPath)`: Gets the MIME type of a file based on its extension.
- `getFileIcon(String virtualPath)`: Gets the icon for a file based on its MIME type.
- `isTextFile(String virtualPath)`: Checks if a file is a text file based on its MIME type.
- `isImageFile(String virtualPath)`: Checks if a file is an image file based on its MIME type.
- `createTextFile(String virtualPath, String content)`: Creates a text file with the specified content.
- `getFileExtension(String path)`: Gets the file extension from a file path.
- `getInstance()`: Gets the singleton instance of the VirtualFileSystem.
- `resolveVirtualPath(String virtualPath)`: Resolves a virtual path to an actual filesystem path.
- `getVirtualPath(Path actualPath)`: Gets the virtual path from an actual filesystem path.
- `createDirectory(String virtualPath)`: Creates a directory in the virtual file system.
- `createFile(String virtualPath, byte[] content)`: Creates a file with the specified content in the virtual file system.
- `readFile(String virtualPath)`: Reads the content of a file in the virtual file system.
- `deleteFile(String virtualPath)`: Deletes a file in the virtual file system.
- `listFiles(String virtualPath)`: Lists the files in a directory in the virtual file system.
- `exists(String virtualPath)`: Checks if a file or directory exists in the virtual file system.
- `getRootPath()`: Gets the root path of the virtual file system.
- `registerExtensionRunner(String extension, FileRunner runner)`: Registers a file runner for a specific file extension.
- `hasExtensionRunner(String fileName)`: Checks if a file runner is registered for a specific file extension.
- `runFile(File file)`: Runs a file using the registered file runner.
- `registerProgram(String name, ProgramExecutor executor)`: Registers a program executor for a specific program name.
- `executeProgram(String name, String[] args)`: Executes a registered program with the specified arguments.
- `getShebang(String virtualPath)`: Gets the shebang line from a file.

## Usage

The `VirtualFileSystem` class is used internally by the MicrOS system to manage files and directories. It is not typically used directly by applications.
