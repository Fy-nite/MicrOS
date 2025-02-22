package org.Finite.MicrOS.apps;

import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.*;
import org.Finite.MicrOS.ui.ErrorDialog;
import org.Finite.MicrOS.core.WindowManager;


public class AppLoader {
    private final Map<String, AppManifest> loadedApps = new HashMap<>();
    private final Map<String, ClassLoader> appClassLoaders = new HashMap<>();
    private final String appDirectory;
    private WindowManager windowManager; // Add WindowManager reference

    public AppLoader(String appDirectory) {
        this.appDirectory = appDirectory;
    }

    // Add setter for WindowManager
    public void setWindowManager(WindowManager windowManager) {
        this.windowManager = windowManager;
    }

    public void loadApps() {
        // Load bundled system apps first
        loadSystemApps();
        
        // Then load external apps
        File dir = new File(appDirectory);
        if (!dir.exists() || !dir.isDirectory()) return;

        for (File file : dir.listFiles((d, name) -> name.endsWith(".app"))) {
            try {
                loadApp(file);
            } catch (Exception e) {
                ErrorDialog.showError(null, "Failed to load app: " + file.getName(), e);
            }
        }
    }

    private void loadSystemApps() {
        // Register built-in settings app
        AppManifest settingsManifest = new AppManifest();
        settingsManifest.setName("Settings");
        settingsManifest.setIdentifier("org.finite.micros.settings");
        settingsManifest.setMainClass("org.Finite.MicrOS.ui.SettingsDialog");
        settingsManifest.setVersion("1.0");
        loadedApps.put(settingsManifest.getIdentifier(), settingsManifest);
        
        // Add any other system apps here
    }

    private void loadApp(File appBundle) throws Exception {
        if (!appBundle.isDirectory() || !appBundle.getName().endsWith(".app")) {
            return;
        }

        File contentsDir = new File(appBundle, "Contents");
        File manifestFile = new File(contentsDir, "manifest.json");
        File resourcesDir = new File(contentsDir, "Resources");

        if (!manifestFile.exists() || !resourcesDir.exists()) {
            throw new Exception("Invalid app bundle structure: " + appBundle.getName());
        }

        // Read manifest
        String jsonContent = new String(Files.readAllBytes(manifestFile.toPath()));
        JSONObject json = new JSONObject(jsonContent);
        AppManifest manifest = parseManifest(json);
        
        // Create class loader FIRST
        List<URL> urls = new ArrayList<>();
        File[] jarFiles = resourcesDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles != null) {
            for (File jar : jarFiles) {
                urls.add(jar.toURI().toURL());
            }
        }
        
        URLClassLoader classLoader = new URLClassLoader(
            urls.toArray(new URL[0]), 
            getClass().getClassLoader()
        );
        
        // Store app info using identifier
        String appId = manifest.getIdentifier();
        loadedApps.put(appId, manifest);
        appClassLoaders.put(appId, classLoader);
        
        // Register for startup if needed and if WindowManager is available
        if (json.optBoolean("startOnLaunch", false) && windowManager != null) {
            windowManager.registerStartupApp(appId);
        }
        
        System.out.println("Loaded app: " + appId + " with main class: " + manifest.getMainClass());
    }

    public String loadAppFromPath(File appBundle) throws Exception {
        if (!appBundle.isDirectory() || !appBundle.getName().endsWith(".app")) {
            throw new IllegalArgumentException("Invalid app bundle: " + appBundle.getAbsolutePath());
        }

        File contentsDir = new File(appBundle, "Contents");
        File manifestFile = new File(contentsDir, "manifest.json");
        
        if (!manifestFile.exists()) {
            throw new IllegalArgumentException("Missing manifest in app bundle: " + appBundle.getAbsolutePath());
        }

        // Read and parse manifest
        String jsonContent = new String(Files.readAllBytes(manifestFile.toPath()));
        JSONObject json = new JSONObject(jsonContent);
        AppManifest manifest = parseManifest(json);
        
        // Create class loader from Resources directory
        File resourcesDir = new File(contentsDir, "Resources");
        if (!resourcesDir.exists()) {
            throw new IllegalArgumentException("Missing Resources directory in app bundle");
        }

        List<URL> urls = new ArrayList<>();
        File[] jarFiles = resourcesDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles != null) {
            for (File jar : jarFiles) {
                urls.add(jar.toURI().toURL());
            }
        }
        
        URLClassLoader classLoader = new URLClassLoader(
            urls.toArray(new URL[0]),
            getClass().getClassLoader()
        );
        
        // Register app
        String appId = manifest.getIdentifier();
        loadedApps.put(appId, manifest);
        appClassLoaders.put(appId, classLoader);
        
        return appId;
    }

    private AppManifest parseManifest(JSONObject json) {
        AppManifest manifest = new AppManifest();
        manifest.setName(json.getString("name"));
        manifest.setIdentifier(json.getString("identifier"));
        manifest.setVersion(json.getString("version"));
        manifest.setMainClass(json.getString("mainClass"));
        manifest.setDescription(json.optString("description", ""));
        manifest.setIcon(json.optString("icon", ""));
        manifest.setCategory(json.optString("category", "Applications"));
        manifest.setMinimumOSVersion(json.optString("minimumOSVersion", "1.0"));
        manifest.setStartOnLaunch(json.optBoolean("startOnLaunch", false));
        
        // Parse arrays
        if (json.has("authors")) {
            JSONArray authors = json.getJSONArray("authors");
            manifest.setAuthors(toStringArray(authors));
        }
        
        if (json.has("supportedFileTypes")) {
            JSONArray fileTypes = json.getJSONArray("supportedFileTypes");
            manifest.setSupportedFileTypes(toStringArray(fileTypes));
        }
        
        // Parse permissions
        if (json.has("permissions")) {
            JSONObject perms = json.getJSONObject("permissions");
            AppManifest.AppPermissions permissions = new AppManifest.AppPermissions();
            permissions.fileSystemAccess = perms.optBoolean("fileSystem", false);
            permissions.networkAccess = perms.optBoolean("network", false);
            permissions.shellAccess = perms.optBoolean("shell", false);
            manifest.setPermissions(permissions);
        }
        
        return manifest;
    }

    private String[] toStringArray(JSONArray array) {
        String[] result = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            result[i] = array.getString(i);
        }
        return result;
    }

    public MicrOSApp createAppInstance(String appId) throws Exception {
        AppManifest manifest = loadedApps.get(appId);
        if (manifest == null) {
            throw new Exception("App not found with ID: " + appId);
        }

        ClassLoader loader = appClassLoaders.get(appId);
        Class<?> mainClass = loader.loadClass(manifest.getMainClass());
        
        if (!MicrOSApp.class.isAssignableFrom(mainClass)) {
            throw new Exception("Invalid app main class for app ID " + appId + ": " + manifest.getMainClass());
        }

        return (MicrOSApp) mainClass.getDeclaredConstructor().newInstance();
    }

    public Collection<AppManifest> getLoadedApps() {
        return loadedApps.values();
    }

    public void loadAppById(String appId) {
        // Check system apps first
        if (loadedApps.containsKey(appId)) {
            return; // Already loaded
        }

        // Look in apps directory
        File dir = new File(appDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException("Apps directory not found");
        }

        // Find app bundle with matching ID
        for (File appBundle : dir.listFiles((d, name) -> name.endsWith(".app"))) {
            try {
                File manifestFile = new File(new File(appBundle, "Contents"), "manifest.json");
                if (manifestFile.exists()) {
                    JSONObject json = new JSONObject(new String(Files.readAllBytes(manifestFile.toPath())));
                    if (appId.equals(json.getString("identifier"))) {
                        loadApp(appBundle);
                        return;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load app " + appId, e);
            }
        }
        
        throw new RuntimeException("App not found: " + appId);
    }
}
