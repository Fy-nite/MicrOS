package org.Finite.MicrOS.ui;

import java.io.File;
import java.io.IOException;

import org.Finite.MicrOS.core.VirtualFileSystem;

import java.awt.Color;

public class CommandProcessor {
    private final Console console;
    private final VirtualFileSystem vfs;
    private String currentDir = "/";

    public CommandProcessor(Console console, VirtualFileSystem vfs) {
        this.console = console;
        this.vfs = vfs;
    }

    public void processCommand(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) return;

        // Check for ./ execution
        if (parts[0].startsWith("./")) {
            String path = parts[0].substring(2); // Remove ./
            executeFile(path, parts);
            return;
        }

        switch (parts[0].toLowerCase()) {
            case "ls":
                listFiles(parts);
                break;
            case "cd":
                changeDirectory(parts);
                break;
            case "pwd":
                printWorkingDirectory();
                break;
            case "cat":
                catFile(parts);
                break;
            case "mkdir":
                makeDirectory(parts);
                break;
            case "touch":
                createFile(parts);
                break;
            case "rm":
                removeFile(parts);
                break;
            case "help":
                showHelp();
                break;
            case "clear":
                console.clear();
                break;
            case "run":
                String path = parts.length > 1 ? parts[1] : null;
                if (path != null) {
                    executeFile(path, parts);
                } else {
                    console.appendText("Usage: run <file>\n", Color.RED);
                }
                break;
            default:
                if (!executeCommand(parts)) {
                    console.appendText("Unknown command: " + parts[0] + "\n", Color.RED);
                }
        }
    }

    private boolean executeCommand(String[] parts) {
        return vfs.executeProgram(parts[0], parts);
    }

    private void listFiles(String[] parts) {
        String path = parts.length > 1 ? resolvePath(parts[1]) : currentDir;
        File[] files = vfs.listFiles(path);
        if (files != null) {
            for (File file : files) {
                String prefix = file.isDirectory() ? "d " : "- ";
                console.appendText(prefix + file.getName() + "\n", Color.CYAN);
            }
        }
    }

    private void changeDirectory(String[] parts) {
        if (parts.length < 2) {
            console.appendText("Usage: cd <directory>\n", Color.RED);
            return;
        }

        String newPath = resolvePath(parts[1]);
        if (vfs.exists(newPath) && new File(vfs.resolveVirtualPath(newPath).toString()).isDirectory()) {
            currentDir = newPath;
            console.setPrompt(formatPrompt()); // Update prompt when directory changes
        } else {
            console.appendText("Directory not found: " + parts[1] + "\n", Color.RED);
        }
    }

    private void printWorkingDirectory() {
        console.appendText(currentDir + "\n", Color.CYAN);
    }

    private void catFile(String[] parts) {
        if (parts.length < 2) {
            console.appendText("Usage: cat <file>\n", Color.RED);
            return;
        }

        String path = resolvePath(parts[1]);
        try {
            byte[] content = vfs.readFile(path);
            console.appendText(new String(content) + "\n", Color.WHITE);
        } catch (IOException e) {
            console.appendText("Error reading file: " + e.getMessage() + "\n", Color.RED);
        }
    }

    private void makeDirectory(String[] parts) {
        if (parts.length < 2) {
            console.appendText("Usage: mkdir <directory>\n", Color.RED);
            return;
        }

        String path = resolvePath(parts[1]);
        if (vfs.createDirectory(path)) {
            console.appendText("Directory created\n", Color.GREEN);
        } else {
            console.appendText("Failed to create directory\n", Color.RED);
        }
    }

    private void createFile(String[] parts) {
        if (parts.length < 2) {
            console.appendText("Usage: touch <file>\n", Color.RED);
            return;
        }

        String path = resolvePath(parts[1]);
        if (vfs.createFile(path, new byte[0])) {
            console.appendText("File created\n", Color.GREEN);
        } else {
            console.appendText("Failed to create file\n", Color.RED);
        }
    }

    private void removeFile(String[] parts) {
        if (parts.length < 2) {
            console.appendText("Usage: rm <file>\n", Color.RED);
            return;
        }

        String path = resolvePath(parts[1]);
        if (vfs.deleteFile(path)) {
            console.appendText("File deleted\n", Color.GREEN);
        } else {
            console.appendText("Failed to delete file\n", Color.RED);
        }
    }

    private void executeFile(String path, String[] originalArgs) {
        String fullPath = resolvePath(path);
        if (!vfs.exists(fullPath)) {
            console.appendText("File not found: " + path + "\n", Color.RED);
            return;
        }

        try {
            String shebang = vfs.getShebang(fullPath);
            if (shebang != null) {
                // Parse the shebang into components
                String[] shebangParts = parseShebang(shebang);
                if (shebangParts.length > 0) {
                    // Convert standard shebangs to internal programs
                    String program = mapShebangToProgram(shebangParts[0]);
                    
                    // Build complete argument list, accounting for ./ usage
                    String[] commandArgs = new String[originalArgs.length];
                    commandArgs[0] = path;
                    System.arraycopy(originalArgs, 1, commandArgs, 1, originalArgs.length - 1);
                    
                    String[] allArgs = buildArgumentList(program, shebangParts, commandArgs, fullPath);

                    if (!vfs.executeProgram(program, allArgs)) {
                        console.appendText("Unknown interpreter: " + shebangParts[0] + "\n", Color.RED);
                    }
                }
            } else {
                console.appendText("No shebang found in file\n", Color.RED);
            }
        } catch (IOException e) {
            console.appendText("Error reading file: " + e.getMessage() + "\n", Color.RED);
        }
    }

    private String[] parseShebang(String shebang) {
        // Handle both direct commands and env-style shebangs
        if (shebang.startsWith("/usr/bin/env ")) {
            return shebang.substring(13).trim().split("\\s+");
        }
        return shebang.split("\\s+");
    }

    private String mapShebangToProgram(String interpreter) {
        // Map standard shebangs to internal programs
        return switch (interpreter.toLowerCase()) {
            case "/usr/bin/asm", "/usr/bin/jmasm", "asm", "jmasm" -> "asm";
            case "/usr/bin/python", "python", "python3" -> "python";
            // Add more mappings as needed
            default -> interpreter;
        };
    }

    private String[] buildArgumentList(String program, String[] shebangParts, String[] cmdParts, String filePath) {
        // Calculate total size needed for combined arguments
        int totalSize = 1;  // Program name
        totalSize += shebangParts.length - 1;  // Shebang args (minus program name)
        totalSize += 1;  // Script path
        totalSize += Math.max(0, cmdParts.length - 2);  // Additional args from command line

        String[] allArgs = new String[totalSize];
        int pos = 0;

        // Add program name
        allArgs[pos++] = program;

        // Add interpreter arguments from shebang (skip the program name)
        for (int i = 1; i < shebangParts.length; i++) {
            allArgs[pos++] = shebangParts[i];
        }

        // Add script path
        allArgs[pos++] = filePath;

        // Add any additional arguments from command line
        for (int i = 2; i < cmdParts.length; i++) {
            allArgs[pos++] = cmdParts[i];
        }

        return allArgs;
    }

    private void showHelp() {
        console.appendText("Available commands:\n", Color.YELLOW);
        console.appendText("  ls [path]      - List files in directory\n", Color.YELLOW);
        console.appendText("  cd <path>      - Change directory\n", Color.YELLOW);
        console.appendText("  pwd            - Print working directory\n", Color.YELLOW);
        console.appendText("  cat <file>     - Display file contents\n", Color.YELLOW);
        console.appendText("  mkdir <dir>    - Create directory\n", Color.YELLOW);
        console.appendText("  touch <file>   - Create empty file\n", Color.YELLOW);
        console.appendText("  rm <file>      - Delete file\n", Color.YELLOW);
        console.appendText("  clear          - Clear screen\n", Color.YELLOW);
        console.appendText("  help           - Show this help\n", Color.YELLOW);
        console.appendText("  run <file>     - Execute file with shebang\n", Color.YELLOW);
    }

    private String resolvePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }
        if (currentDir.equals("/")) {
            return "/" + path;
        }
        return currentDir + "/" + path;
    }

    private String formatPrompt() {
        String shortDir = currentDir;
        if (currentDir.equals("/")) {
            shortDir = "/";
        } else {
            String[] parts = currentDir.split("/");
            if (parts.length > 2) {
                // Show only the last directory name
                shortDir = "..." + parts[parts.length - 1] + "/";
            }
        }
        return shortDir + " $ ";
    }

    public String getPrompt() {
        return formatPrompt();
    }
}
