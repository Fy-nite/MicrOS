package org.Finite.MicrOS.apps;

public class AppManifest {
    private String name;
    private String version;
    private String mainClass;
    private String icon; // Path to the app's icon
    private String description;
    private String identifier;  // e.g. "com.example.myapp"
    private String category;    // e.g. "Utilities"
    private String[] authors;
    private String[] supportedFileTypes;
    private String minimumOSVersion;
    private AppPermissions permissions;
    private AppType appType = AppType.CUSTOM;
    private boolean pinToTaskbar = false;
    private boolean startOnLaunch = false;
    private boolean isCLI = false;
    private String cliCommand;  // Command to invoke the app from terminal
    private String[] cliAliases = new String[0];  // Alternative command names

    public static class AppPermissions {
        public boolean fileSystemAccess;
        public boolean networkAccess;
        public boolean shellAccess;
        
        // Add getters and setters
        public boolean getFileSystemAccess() { return fileSystemAccess; }
        public void setFileSystemAccess(boolean access) { this.fileSystemAccess = access; }
        public boolean getNetworkAccess() { return networkAccess; }
        public void setNetworkAccess(boolean access) { this.networkAccess = access; }
        public boolean getShellAccess() { return shellAccess; }
        public void setShellAccess(boolean access) { this.shellAccess = access; }
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getMainClass() { return mainClass; }
    public void setMainClass(String mainClass) { this.mainClass = mainClass; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String[] getAuthors() { return authors; }
    public void setAuthors(String[] authors) { this.authors = authors; }
    public String[] getSupportedFileTypes() { return supportedFileTypes; }
    public void setSupportedFileTypes(String[] types) { this.supportedFileTypes = types; }
    public String getMinimumOSVersion() { return minimumOSVersion; }
    public void setMinimumOSVersion(String version) { this.minimumOSVersion = version; }
    public AppPermissions getPermissions() { return permissions; }
    public void setPermissions(AppPermissions permissions) { this.permissions = permissions; }
    public AppType getAppType() {
        return appType;
    }

    public void setAppType(AppType appType) {
        this.appType = appType;
    }

    public boolean isPinnedToTaskbar() {
        return pinToTaskbar;
    }

    public void setPinToTaskbar(boolean pinToTaskbar) {
        this.pinToTaskbar = pinToTaskbar;
    }

    public boolean isStartOnLaunch() {
        return startOnLaunch;
    }

    public void setStartOnLaunch(boolean startOnLaunch) {
        this.startOnLaunch = startOnLaunch;
    }

    public boolean isCLI() { return isCLI; }
    public void setCLI(boolean cli) { this.isCLI = cli; }
    public String getCLICommand() { return cliCommand; }
    public void setCLICommand(String command) { this.cliCommand = command; }
    public String[] getCLIAliases() { return cliAliases; }
    public void setCLIAliases(String[] aliases) { this.cliAliases = aliases; }
}
