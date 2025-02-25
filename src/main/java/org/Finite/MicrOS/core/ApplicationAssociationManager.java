package org.Finite.MicrOS.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Manages application associations for file types and actions
 */
public class ApplicationAssociationManager {
    
    // Key: action ID or file extension, Value: list of applications that can handle it
    private final Map<String, List<ApplicationAssociation>> associations;
    
    // Key: action ID or file extension, Value: ID of the user's preferred application
    private final Map<String, String> userPreferences;
    
    // The VFS reference for accessing settings
    private final VirtualFileSystem vfs;
    
    // System preferences
    private Preferences prefs;
    
    // Constants for association types
    public static final String FILE_TYPE = "filetype:";
    public static final String ACTION_TYPE = "action:";
    
    /**
     * Creates a new ApplicationAssociationManager
     * 
     * @param vfs The virtual file system reference
     */
    public ApplicationAssociationManager(VirtualFileSystem vfs) {
        this.vfs = vfs;
        this.associations = new HashMap<>();
        this.userPreferences = new HashMap<>();
        this.prefs = Preferences.userNodeForPackage(ApplicationAssociationManager.class);
        
        loadAssociations();
        loadUserPreferences();
    }
    
    /**
     * Registers an application that can handle a specific file type
     * 
     * @param extension File extension (without dot)
     * @param app The application association
     */
    public void registerFileTypeAssociation(String extension, ApplicationAssociation app) {
        registerAssociation(FILE_TYPE + extension.toLowerCase(), app);
    }
    
    /**
     * Registers an application that can handle a specific action
     * 
     * @param actionId Unique action identifier
     * @param app The application association
     */
    public void registerActionAssociation(String actionId, ApplicationAssociation app) {
        registerAssociation(ACTION_TYPE + actionId, app);
    }
    
    /**
     * Generic method to register an association
     */
    private void registerAssociation(String key, ApplicationAssociation app) {
        List<ApplicationAssociation> apps = associations.computeIfAbsent(key, k -> new ArrayList<>());
        
        // Don't add duplicates
        if (!apps.contains(app)) {
            apps.add(app);
            
            // If this is the first app or marked as system default, set it as the default
            if (apps.size() == 1 || app.isSystemDefault()) {
                for (ApplicationAssociation existingApp : apps) {
                    existingApp.setSystemDefault(existingApp.equals(app));
                }
            }
        }
    }
    
    /**
     * Gets all applications that can handle a specific file type
     * 
     * @param extension File extension (without dot)
     * @return List of application associations
     */
    public List<ApplicationAssociation> getFileTypeAssociations(String extension) {
        return getAssociations(FILE_TYPE + extension.toLowerCase());
    }
    
    /**
     * Gets all applications that can handle a specific action
     * 
     * @param actionId Unique action identifier
     * @return List of application associations
     */
    public List<ApplicationAssociation> getActionAssociations(String actionId) {
        return getAssociations(ACTION_TYPE + actionId);
    }
    
    /**
     * Generic method to get associations
     */
    private List<ApplicationAssociation> getAssociations(String key) {
        return associations.getOrDefault(key, new ArrayList<>());
    }
    
    /**
     * Gets the user's preferred application for a file type
     * 
     * @param extension File extension (without dot)
     * @return The preferred application or null if none is set
     */
    public ApplicationAssociation getPreferredFileTypeApplication(String extension) {
        return getPreferredApplication(FILE_TYPE + extension.toLowerCase());
    }
    
    /**
     * Gets the user's preferred application for an action
     * 
     * @param actionId Unique action identifier
     * @return The preferred application or null if none is set
     */
    public ApplicationAssociation getPreferredActionApplication(String actionId) {
        return getPreferredApplication(ACTION_TYPE + actionId);
    }
    
    /**
     * Generic method to get preferred application
     */
    private ApplicationAssociation getPreferredApplication(String key) {
        String preferredAppId = userPreferences.get(key);
        
        List<ApplicationAssociation> apps = associations.get(key);
        if (apps != null) {
            // Try to find user's preferred app
            if (preferredAppId != null) {
                for (ApplicationAssociation app : apps) {
                    if (app.getId().equals(preferredAppId)) {
                        return app;
                    }
                }
            }
            
            // If no user preference or preferred app not found, return the system default
            for (ApplicationAssociation app : apps) {
                if (app.isSystemDefault()) {
                    return app;
                }
            }
            
            // If no system default, return the first app
            if (!apps.isEmpty()) {
                return apps.get(0);
            }
        }
        
        return null;
    }
    
    /**
     * Sets the user's preferred application for a file type
     * 
     * @param extension File extension (without dot)
     * @param appId Application ID
     */
    public void setPreferredFileTypeApplication(String extension, String appId) {
        setPreferredApplication(FILE_TYPE + extension.toLowerCase(), appId);
    }
    
    /**
     * Sets the user's preferred application for an action
     * 
     * @param actionId Unique action identifier
     * @param appId Application ID
     */
    public void setPreferredActionApplication(String actionId, String appId) {
        setPreferredApplication(ACTION_TYPE + actionId, appId);
    }
    
    /**
     * Generic method to set preferred application
     */
    private void setPreferredApplication(String key, String appId) {
        userPreferences.put(key, appId);
        prefs.put("app_assoc:" + key, appId);
    }
    
    /**
     * Loads the user preferences from the system
     */
    private void loadUserPreferences() {
        // Load from Java Preferences API
        for (String key : associations.keySet()) {
            String prefKey = "app_assoc:" + key;
            String prefValue = prefs.get(prefKey, null);
            if (prefValue != null) {
                userPreferences.put(key, prefValue);
            }
        }
        
        // Also try to load from VFS settings
        try {
            String settingsPath = "/system/settings/app_associations.json";
            if (vfs.fileExists(settingsPath)) {
                String content = new String(vfs.readFile(settingsPath));
                JSONObject settings = new JSONObject(content);
                
                for (String key : settings.keySet()) {
                    userPreferences.put(key, settings.getString(key));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading application associations: " + e.getMessage());
        }
    }
    
    /**
     * Loads the system associations from the VFS
     */
    private void loadAssociations() {
        try {
            String associationsPath = "/system/settings/app_registry.json";
            if (vfs.fileExists(associationsPath)) {
                String content = new String(vfs.readFile(associationsPath));
                JSONObject registry = new JSONObject(content);
                
                // Load file type associations
                if (registry.has("fileTypes")) {
                    JSONObject fileTypes = registry.getJSONObject("fileTypes");
                    for (String extension : fileTypes.keySet()) {
                        JSONArray apps = fileTypes.getJSONArray(extension);
                        for (int i = 0; i < apps.length(); i++) {
                            JSONObject appObj = apps.getJSONObject(i);
                            ApplicationAssociation app = createAssociationFromJson(appObj);
                            registerFileTypeAssociation(extension, app);
                        }
                    }
                }
                
                // Load action associations
                if (registry.has("actions")) {
                    JSONObject actions = registry.getJSONObject("actions");
                    for (String actionId : actions.keySet()) {
                        JSONArray apps = actions.getJSONArray(actionId);
                        for (int i = 0; i < apps.length(); i++) {
                            JSONObject appObj = apps.getJSONObject(i);
                            ApplicationAssociation app = createAssociationFromJson(appObj);
                            registerActionAssociation(actionId, app);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading application registry: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to create an ApplicationAssociation from JSON
     */
    private ApplicationAssociation createAssociationFromJson(JSONObject json) {
        String id = json.getString("id");
        String displayName = json.getString("displayName");
        String description = json.optString("description", "");
        String iconPath = json.optString("iconPath", "");
        boolean isSystemDefault = json.optBoolean("isSystemDefault", false);
        
        return new ApplicationAssociation(id, displayName, description, iconPath, isSystemDefault);
    }
    
    /**
     * Saves all user preferences to the VFS
     */
    public void saveUserPreferences() {
        try {
            JSONObject settings = new JSONObject();
            
            for (Map.Entry<String, String> entry : userPreferences.entrySet()) {
                settings.put(entry.getKey(), entry.getValue());
            }
            
            String settingsPath = "/system/settings/app_associations.json";
            vfs.createDirectories("/system/settings");
            vfs.writeFile(settingsPath, settings.toString(4).getBytes());
        } catch (IOException e) {
            System.err.println("Error saving application associations: " + e.getMessage());
        }
    }
}