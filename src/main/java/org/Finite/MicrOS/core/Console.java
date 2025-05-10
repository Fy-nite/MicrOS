package org.Finite.MicrOS.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Console {
    private static final VirtualFileSystem vfs = VirtualFileSystem.getInstance();
    private static String currentDir = "/";
    private static final List<String> commandHistory = new ArrayList<>();

    public static void startConsoleMode() {
        System.out.println("MicrOS Console mode started. Type 'help' for commands.");
        boolean running = true;

        while (running) {
            try {
                System.out.print(currentDir + " > ");
                String input = System.console().readLine();
                if (input == null || input.trim().isEmpty()) continue;

                String[] parts = input.trim().split("\\s+");
                String command = parts[0].toLowerCase();
                commandHistory.add(input); // Track command history

                switch (command) {
                    case "exit":
                        running = false;
                        break;
                    case "clear":
                        clearTerminal();
                        break;
                    case "help":
                        showHelp();
                        break;
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
                    case "history":
                        showCommandHistory();
                        break;
                    default:
                        System.out.println("Unknown command: " + command);
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    private static void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  ls [path]       - List files in the directory");
        System.out.println("  cd <directory>  - Change directory");
        System.out.println("  pwd             - Print current directory");
        System.out.println("  cat <file>      - Display file content");
        System.out.println("  mkdir <name>    - Create a new directory");
        System.out.println("  touch <name>    - Create a new file");
        System.out.println("  rm <name>       - Remove a file or directory");
        System.out.println("  exit            - Exit console mode");
        System.out.println("  clear           - Clear the terminal screen");
        System.out.println("  history         - Show command history");
    }

    private static void listFiles(String[] parts) {
        String path = parts.length > 1 ? resolvePath(parts[1]) : currentDir;
        File[] files = vfs.listFiles(path);
        if (files != null) {
            for (File file : files) {
                System.out.println((file.isDirectory() ? "[DIR] " : "      ") + file.getName());
            }
        } else {
            System.out.println("Directory not found: " + path);
        }
    }

    private static void changeDirectory(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: cd <directory>");
            return;
        }
        String newPath = resolvePath(parts[1]);
        if (vfs.exists(newPath) && vfs.resolveVirtualPath(newPath).toFile().isDirectory()) {
            currentDir = newPath;
        } else {
            System.out.println("Directory not found: " + parts[1]);
        }
    }

    private static void printWorkingDirectory() {
        System.out.println(currentDir);
    }

    private static void catFile(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: cat <file>");
            return;
        }
        String path = resolvePath(parts[1]);
        try {
            byte[] content = vfs.readFile(path);
            System.out.println(new String(content));
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private static void makeDirectory(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: mkdir <name>");
            return;
        }
        String path = resolvePath(parts[1]);
        if (vfs.createDirectory(path)) {
            System.out.println("Directory created: " + path);
        } else {
            System.out.println("Failed to create directory: " + path);
        }
    }

    private static void createFile(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: touch <name>");
            return;
        }
        String path = resolvePath(parts[1]);
        if (vfs.createFile(path)) {
            System.out.println("File created: " + path);
        } else {
            System.out.println("Failed to create file: " + path);
        }
    }

    private static void removeFile(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: rm <name>");
            return;
        }
        String path = resolvePath(parts[1]);
        if (vfs.deleteFile(path)) {
            System.out.println("Deleted: " + path);
        } else {
            System.out.println("Failed to delete: " + path);
        }
    }

    private static String resolvePath(String path) {
        if (path.startsWith("/")) {
            return path;
        }
        return currentDir.equals("/") ? "/" + path : currentDir + "/" + path;
    }

    private static void clearTerminal() {
        try {
            // Clear the terminal screen
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("Failed to clear terminal: " + e.getMessage());
        }
    }

    private static void showCommandHistory() {
        if (commandHistory.isEmpty()) {
            System.out.println("No commands in history.");
        } else {
            for (int i = 0; i < commandHistory.size(); i++) {
                System.out.println((i + 1) + ": " + commandHistory.get(i));
            }
        }
    }
}
