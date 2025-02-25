package org.Finite.MicrOS.core;

/**
 * Represents an application that can be associated with certain file types or actions
 */
public class ApplicationAssociation {
    private String id;
    private String displayName;
    private String description;
    private String iconPath;
    private boolean isSystemDefault;
    
    /**
     * Creates a new application association
     * 
     * @param id Unique identifier for this application
     * @param displayName Human-readable name for the application
     * @param description Description of the application
     * @param iconPath Path to the application icon in VFS
     * @param isSystemDefault Whether this association is a system default
     */
    public ApplicationAssociation(String id, String displayName, String description, String iconPath, boolean isSystemDefault) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.iconPath = iconPath;
        this.isSystemDefault = isSystemDefault;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }

    public String getIconPath() {
        return iconPath;
    }
    
    public boolean isSystemDefault() {
        return isSystemDefault;
    }
    
    public void setSystemDefault(boolean isSystemDefault) {
        this.isSystemDefault = isSystemDefault;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ApplicationAssociation that = (ApplicationAssociation) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}