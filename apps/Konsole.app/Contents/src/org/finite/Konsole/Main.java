package org.finite.Konsole;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.Finite.MicrOS.apps.AppManifest;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.core.CLIRegistry;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.ui.FontLoader;

public class Main extends MicrOSApp {
    private JTextArea console;
    private JScrollPane scrollPane;
    private StringBuilder inputBuffer;
    private java.util.List<String> commandHistory;
    private int historyIndex;
    private String currentDirectory = "/";
    private String systemName = "MicrOS";
    private String prompt = "[%s@%s %s]$ ";  // format: [user@system path]$
    // get the system font

    public Font font = FontLoader.getFont("iJetBrainsMono-Regular.ttf", Font.PLAIN, 12);;
    @Override
    public JComponent createUI() {
        commandHistory = new java.util.ArrayList<>();
        historyIndex = -1;
        
        console = new JTextArea(20, 80);
        console.setFont(font);
        console.setBackground(Color.BLACK);
        console.setForeground(Color.cyan);
        console.setCaretColor(Color.GREEN);
        console.setEditable(true);
        inputBuffer = new StringBuilder();
        
        console.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    String command = inputBuffer.toString().trim();
                    if (!command.isEmpty()) {
                        commandHistory.add(command);
                        historyIndex = commandHistory.size();
                    }
                    processCommand(command);
                    inputBuffer.setLength(0);
                 
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    if (inputBuffer.length() > 0) {
                        inputBuffer.setLength(inputBuffer.length() - 1);
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    e.consume();
                    if (historyIndex > 0) {
                        historyIndex--;
                        String historyCommand = commandHistory.get(historyIndex);
                        updateInputLine(historyCommand);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    e.consume();
                    if (historyIndex < commandHistory.size() - 1) {
                        historyIndex++;
                        String historyCommand = commandHistory.get(historyIndex);
                        updateInputLine(historyCommand);
                    } else if (historyIndex == commandHistory.size() - 1) {
                        historyIndex = commandHistory.size();
                        updateInputLine("");
                    }
                } else if (e.getKeyChar() >= 32 && e.getKeyChar() < 127) {
                    inputBuffer.append(e.getKeyChar());
                }
            }
        });
        
        scrollPane = new JScrollPane(console);
        
        // Setup basic manifest
        AppManifest manifest = new AppManifest();
        manifest.setName("Konsole");
        manifest.setIdentifier("org.finite.Konsole");
        manifest.setMainClass(getClass().getName());
        setManifest(manifest);
        
        return scrollPane;
    }

    private void updatePrompt() {
        String user = System.getProperty("user.name", "user");
        prompt = String.format("[%s@%s %s]$ ", user, systemName, currentDirectory);
    }

    private void processCommand(String command) {
        println("");
        String[] parts = command.split("\\s+");
        if (parts.length == 0) return;
        
        String cmd = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        
        switch (cmd) {
            case "clear":
                console.setText("");
                break;
            case "help":
                showHelp();
                break;
            case "exit":
                onStop();
                break;
            case "cd":
                if (args.length > 0) {
                    changeDirectory(args[0]);
                } else {
                    currentDirectory = "/";
                }
                updatePrompt();
                break;
            case "pwd":
                println(currentDirectory);
                break;
            case "ls":
                listFiles(args);
                break;
            default:
                // Try executing as CLI app
                if (!CLIRegistry.getInstance().executeCommand(cmd, args, getManifest().getIdentifier())) {
                    println("\nUnknown command: " + cmd);
                }
        }
        displayPrompt();
    }

    private void changeDirectory(String path) {
        // Simple path handling - you might want to make this more sophisticated
        if (path.startsWith("/")) {
            currentDirectory = path;
        } else if (path.equals("..")) {
            int lastSlash = currentDirectory.lastIndexOf('/');
            if (lastSlash > 0) {
                currentDirectory = currentDirectory.substring(0, lastSlash);
            } else {
                currentDirectory = "/";
            }
        } else {
            currentDirectory = currentDirectory.equals("/") ? 
                "/" + path : currentDirectory + "/" + path;
        }
    }

    private void listFiles(String[] args) {
        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        String path = args.length > 0 ? args[0] : currentDirectory;
        
        File[] files = vfs.listFiles(path);
        if (files.length == 0) {
            println("Directory is empty");
            return;
        }

        // Calculate the longest filename for formatting
        int maxLength = Arrays.stream(files)
            .mapToInt(f -> f.getName().length())
            .max()
            .orElse(0);

        // Format and print each file
        for (File file : files) {
            String name = file.getName();
            String type = file.isDirectory() ? "DIR" : "FILE";
            String size = String.format("%5d", file.length());
            
            println(String.format("%-" + maxLength + "s  %5s  %s", 
                name, size, type));
        }
    }

    private void showHelp() {
        println("\nAvailable commands:");
        println("  clear  - Clear the terminal");
        println("  help   - Show this help message");
        println("  exit   - Exit the terminal");
        println("  cd    - Change directory");
        println("  pwd   - Print working directory");
        println("  ls    - List directory contents");
        println("\nCLI Applications:");
        for (String cmd : CLIRegistry.getInstance().getAvailableCommands()) {
            println("  " + cmd);
        }
    }

    private void println(String text) {
        console.append(text + "\n");
        console.setCaretPosition(console.getDocument().getLength());
    }


    

    private void print(String text) {
        console.append(text);
        console.setCaretPosition(console.getDocument().getLength());
    }

    private void displayPrompt() {
        console.append(prompt);
        console.setCaretPosition(console.getDocument().getLength());
    }

    private void updateInputLine(String text) {
        String currentText = console.getText();
        int lastPromptIndex = currentText.lastIndexOf(prompt);
        if (lastPromptIndex != -1) {
            console.replaceRange(text, lastPromptIndex + prompt.length(), console.getDocument().getLength());
            inputBuffer.setLength(0);
            inputBuffer.append(text);
        }
    }

    @Override
    public void onStart() {
        System.out.println("Konsole started");
        updatePrompt();
        println("MicrOS Konsole v1.0");
        displayPrompt();
    }

    @Override
    public void onStop() {
        System.out.println("Konsole stopped");
    }
}
