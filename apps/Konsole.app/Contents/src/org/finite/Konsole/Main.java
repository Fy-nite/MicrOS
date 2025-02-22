package org.finite.Konsole;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.apps.AppManifest;
import org.Finite.MicrOS.core.CLIRegistry;
import org.Finite.MicrOS.core.Intent;
import java.util.Arrays;

public class Main extends MicrOSApp {
    private JTextArea console;
    private JScrollPane scrollPane;
    private StringBuilder inputBuffer;
    private String prompt = "$ ";
    
    @Override
    public JComponent createUI() {
        console = new JTextArea(20, 80);
        console.setFont(new Font("Monospaced", Font.PLAIN, 12));
        console.setBackground(Color.BLACK);
        console.setForeground(Color.GREEN);
        console.setCaretColor(Color.GREEN);
        console.setEditable(true);
        inputBuffer = new StringBuilder();
        
        console.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    String command = inputBuffer.toString().trim();
                    processCommand(command);
                    inputBuffer.setLength(0);
                    displayPrompt();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    if (inputBuffer.length() > 0) {
                        inputBuffer.setLength(inputBuffer.length() - 1);
                    }
                    e.consume();
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

    private void processCommand(String command) {
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
            default:
                // Try executing as CLI app
                if (!CLIRegistry.getInstance().executeCommand(cmd, args, getManifest().getIdentifier())) {
                    println("Unknown command: " + cmd);
                }
        }
        displayPrompt();
    }
    
    private void showHelp() {
        println("Available commands:");
        println("  clear  - Clear the terminal");
        println("  help   - Show this help message");
        println("  exit   - Exit the terminal");
        println("\nCLI Applications:");
        for (String cmd : CLIRegistry.getInstance().getAvailableCommands()) {
            println("  " + cmd);
        }
    }

    private void println(String text) {
        console.append(text + "\n");
        console.setCaretPosition(console.getDocument().getLength());
    }

    private void displayPrompt() {
        console.append("\n" + prompt);
        console.setCaretPosition(console.getDocument().getLength());
    }

    @Override
    public void onStart() {
        System.out.println("Konsole started");
        println("MicrOS Konsole v1.0");
        displayPrompt();
    }

    @Override
    public void onStop() {
        System.out.println("Konsole stopped");
    }
}
