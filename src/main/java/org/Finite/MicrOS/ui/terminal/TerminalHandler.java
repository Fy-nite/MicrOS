package org.Finite.MicrOS.ui.terminal;

/**
 * Interface representing a terminal handler
 * Implementations can either use the built-in console or external terminal applications
 */
public interface TerminalHandler {
    
    /**
     * Execute a command in the terminal
     * 
     * @param command The command to execute
     * @return ID of the execution for tracking
     */
    String executeCommand(String command);
    
    /**
     * Append text to the terminal output
     * 
     * @param text Text to append
     */
    void appendText(String text);
    
    /**
     * Clear the terminal
     */
    void clear();
    
    /**
     * Set the prompt shown in the terminal
     * 
     * @param prompt The prompt text
     */
    void setPrompt(String prompt);
    
    /**
     * Get the terminal window title
     * 
     * @return Terminal title
     */
    String getTitle();
    
    /**
     * Get the type of terminal
     * 
     * @return Terminal type identifier
     */
    String getType();
}