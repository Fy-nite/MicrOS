package org.Finite.MicrOS.core;

import java.util.*;
import org.Finite.MicrOS.apps.AppManifest;
import org.Finite.MicrOS.apps.MicrOSApp;

public class CLIRegistry {
    private static CLIRegistry instance = new CLIRegistry();
    private final Map<String, String> commandToAppId = new HashMap<>();
    private WindowManager windowManager;
    
    private CLIRegistry() {
        this.windowManager = null; // Will be set later
    }
    
    public static CLIRegistry getInstance() {
        return instance;
    }
    
    public void setWindowManager(WindowManager wm) {
        if (instance.windowManager == null) {
            instance.windowManager = wm;
        }
    }
    
    public void registerCLIApp(AppManifest manifest) {
        if (manifest.isCLI() && manifest.getCLICommand() != null) {
            commandToAppId.put(manifest.getCLICommand(), manifest.getIdentifier());
            
            // Register aliases
            if (manifest.getCLIAliases() != null) {
                for (String alias : manifest.getCLIAliases()) {
                    commandToAppId.put(alias, manifest.getIdentifier());
                }
            }
        }
    }
    
    public boolean executeCommand(String command, String[] args, String consoleId) {
        String appId = commandToAppId.get(command);
        if (appId != null && windowManager != null) {
            Intent intent = new Intent(appId);
            intent.putExtra("cli", true);
            intent.putExtra("args", args);
            intent.putExtra("consoleId", consoleId);
            windowManager.launchAppWithIntent(intent);
            return true;
        }
        return false;
    }
    
    public Set<String> getAvailableCommands() {
        return new HashSet<>(commandToAppId.keySet());
    }
}
