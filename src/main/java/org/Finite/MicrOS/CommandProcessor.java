package org.Finite.MicrOS;

import java.io.File;
import java.io.IOException;
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
                runFile(parts);
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

    private void runFile(String[] parts) {
        if (parts.length < 2) {
            console.appendText("Usage: run <file>\n", Color.RED);
            return;
        }

        String path = resolvePath(parts[1]);
        if (!vfs.exists(path)) {
            console.appendText("File not found: " + parts[1] + "\n", Color.RED);
            return;
        }

        try {
            String shebang = vfs.getShebang(path);
            if (shebang != null) {
                String[] shebangParts = shebang.split("\\s+");
                if (shebangParts.length > 0) {
                    String program = shebangParts[0];
                    
                    String[] allArgs = new String[shebangParts.length + parts.length - 1];
                    allArgs[0] = program;
                    System.arraycopy(shebangParts, 1, allArgs, 1, shebangParts.length - 1);
                    allArgs[shebangParts.length] = path;
                    System.arraycopy(parts, 2, allArgs, shebangParts.length + 1, parts.length - 2);

                    if (!vfs.executeProgram(program, allArgs)) {
                        console.appendText("Unknown interpreter: " + program + "\n", Color.RED);
                    }
                }
            } else {
                console.appendText("No shebang found in file\n", Color.RED);
            }
        } catch (IOException e) {
            console.appendText("Error reading file: " + e.getMessage() + "\n", Color.RED);
        }
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
