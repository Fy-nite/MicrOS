package org.Finite.MicrOS.ui.terminal;

import java.awt.Color;
import org.Finite.MicrOS.ui.Console;

/**
 * Implementation of TerminalHandler using the built-in Console
 */
public class InternalConsoleHandler implements TerminalHandler {
    
    private final Console console;
    private final String title;
    
    public InternalConsoleHandler(Console console, String title) {
        this.console = console;
        this.title = title;
    }
    
    @Override
    public String executeCommand(String command) {
        // Execute command in the internal console
        // The CommandProcessor in Console class will handle this
        console.appendText(command + "\n", new Color(220, 220, 220));
        return "internal-" + System.currentTimeMillis();
    }
    
    @Override
    public void appendText(String text) {
        console.appendText(text, new Color(220, 220, 220));
    }
    
    @Override
    public void clear() {
        console.clear();
    }
    
    @Override
    public void setPrompt(String prompt) {
        console.setPrompt(prompt);
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public String getType() {
        return "internal";
    }
}