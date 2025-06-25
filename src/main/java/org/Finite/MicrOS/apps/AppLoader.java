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

public class AppLoader {
    private final Map<String, AppManifest> loadedApps = new HashMap<>();
    private final Map<String, ClassLoader> appClassLoaders = new HashMap<>();
    private final String appDirectory;

    public AppLoader(String appDirectory) {
        this.appDirectory = appDirectory;
    }

    public void loadApps() {
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
        
        // Create class loader for all JARs in Resources directory
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
        
        // Store app info
        loadedApps.put(manifest.getIdentifier(), manifest);
        appClassLoaders.put(manifest.getIdentifier(), classLoader);
        
        System.out.println("Loaded app: " + manifest.getIdentifier() + 
                         " with main class: " + manifest.getMainClass());
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

    public MicrOSApp createAppInstance(String appName) throws Exception {
        AppManifest manifest = loadedApps.get(appName);
        if (manifest == null) {
            throw new Exception("App not found: " + appName);
        }

        ClassLoader loader = appClassLoaders.get(appName);
        Class<?> mainClass = loader.loadClass(manifest.getMainClass());
        
        if (!MicrOSApp.class.isAssignableFrom(mainClass)) {
            throw new Exception("Invalid app main class: " + manifest.getMainClass());
        }

        return (MicrOSApp) mainClass.getDeclaredConstructor().newInstance();
    }

    public Collection<AppManifest> getLoadedApps() {
        return loadedApps.values();
    }
}
