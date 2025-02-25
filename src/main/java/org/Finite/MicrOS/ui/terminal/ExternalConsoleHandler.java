package org.Finite.MicrOS.ui.terminal;

import java.io.IOException;

/**
 * Implementation of TerminalHandler for external terminal applications
 */
public class ExternalConsoleHandler implements TerminalHandler {
    
    private final String terminalApp;
    private final String title;
    
    public ExternalConsoleHandler(String terminalApp, String title) {
        this.terminalApp = terminalApp;
        this.title = title;
    }
    
    @Override
    public String executeCommand(String command) {
        try {
            // Launch the external terminal with the command
            ProcessBuilder pb = new ProcessBuilder(terminalApp, "-e", command);
            Process process = pb.start();
            
            // Return a unique ID for the execution
            return "external-" + System.currentTimeMillis();
        } catch (IOException e) {
            System.err.println("Error launching external terminal: " + e.getMessage());
            return "error-" + System.currentTimeMillis();
        }
    }
    
    @Override
    public void appendText(String text) {
        // External terminals can't append text directly
        // This is a no-op
    }
    
    @Override
    public void clear() {
        // External terminals can't be cleared directly
        // This is a no-op
    }
    
    @Override
    public void setPrompt(String prompt) {
        // External terminals can't have their prompt changed directly
        // This is a no-op
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public String getType() {
        return "external:" + terminalApp;
    }
}