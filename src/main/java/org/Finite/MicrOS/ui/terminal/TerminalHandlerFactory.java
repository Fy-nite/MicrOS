package org.Finite.MicrOS.ui.terminal;

import org.Finite.MicrOS.core.ApplicationAssociation;
import org.Finite.MicrOS.core.ApplicationAssociationManager;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.ui.ApplicationChooserDialog;
import org.Finite.MicrOS.ui.Console;
import java.awt.Component;

/**
 * Factory for creating terminal handlers based on user preferences
 */
public class TerminalHandlerFactory {
    
    private final WindowManager windowManager;
    
    public TerminalHandlerFactory(WindowManager windowManager) {
        this.windowManager = windowManager;
    }
    
    /**
     * Creates a terminal handler based on the user's preferences
     * 
     * @param title Title for the terminal
     * @param console Console instance for internal terminal (can be null if not needed)
     * @param parent Parent component for dialogs
     * @return An appropriate terminal handler
     */
    public TerminalHandler createTerminalHandler(String title, Console console, Component parent) {
        String selectedApp = windowManager.performAction("terminal", "Terminal", parent);
        
        if (selectedApp == null) {
            // If user canceled, default to internal console if available
            return console != null ? new InternalConsoleHandler(console, title) : null;
        }
        
        if (selectedApp.startsWith("external:")) {
            // Create external terminal handler
            String externalApp = selectedApp.substring("external:".length());
            return new ExternalConsoleHandler(externalApp, title);
        } else {
            // Create internal terminal handler
            return console != null ? new InternalConsoleHandler(console, title) : null;
        }
    }
    
    /**
     * Returns the default terminal handler without showing a chooser dialog
     * 
     * @param title Terminal title
     * @param console Console instance for internal terminal
     * @return The default terminal handler
     */
    public TerminalHandler getDefaultTerminalHandler(String title, Console console) {
        ApplicationAssociationManager manager = windowManager.getAppAssociationManager();
        ApplicationAssociation defaultApp = manager.getPreferredActionApplication("terminal");
        
        if (defaultApp == null) {
            // No user preference, find the system default
            for (ApplicationAssociation app : manager.getActionAssociations("terminal")) {
                if (app.isSystemDefault()) {
                    defaultApp = app;
                    break;
                }
            }
            
            // If still null, use first available app
            if (defaultApp == null && !manager.getActionAssociations("terminal").isEmpty()) {
                defaultApp = manager.getActionAssociations("terminal").get(0);
            }
        }
        
        // Create the appropriate handler
        if (defaultApp != null) {
            String id = defaultApp.getId();
            if (id.startsWith("external:")) {
                String externalApp = id.substring("external:".length());
                return new ExternalConsoleHandler(externalApp, title);
            }
        }
        
        // Default to internal console
        return new InternalConsoleHandler(console, title);
    }
}