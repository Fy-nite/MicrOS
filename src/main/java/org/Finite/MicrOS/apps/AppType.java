package org.Finite.MicrOS.apps;

public enum AppType {
    DEFAULT("default", "Default Application"),
    CONSOLE("console", "Console"),
    TEXT_EDITOR("texteditor", "Text Editor"),
    IMAGE_VIEWER("imageviewer", "Image Viewer"),
    WEB_VIEWER("webviewer", "Web Viewer"),
    FILE_MANAGER("filemanager", "File Manager"),
    SETTINGS("settings", "Settings"),
    CUSTOM("custom", "Custom Application");

    private final String identifier;
    private final String displayName;

    AppType(String identifier, String displayName) {
        this.identifier = identifier;
        this.displayName = displayName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AppType fromIdentifier(String identifier) {
        for (AppType type : values()) {
            if (type.identifier.equals(identifier)) {
                return type;
            }
        }
        return CUSTOM;
    }
}
