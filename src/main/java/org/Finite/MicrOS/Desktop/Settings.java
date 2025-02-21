package org.Finite.MicrOS.Desktop;

import javax.swing.*;

import org.Finite.MicrOS.core.VirtualFileSystem;

import java.awt.*;
import java.io.*;
import java.util.Properties;

public class Settings {
    private static Settings instance;
    private final Properties properties;
    private final String settingsPath;
    private final VirtualFileSystem vfs;

    private Settings() {
        this.vfs = VirtualFileSystem.getInstance();
        this.properties = new Properties();
        this.settingsPath = "/system/settings.properties";
        loadSettings();
    }

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    private void loadSettings() {
        try {
            if (vfs.exists(settingsPath)) {
                byte[] data = vfs.readFile(settingsPath);
                properties.load(new ByteArrayInputStream(data));
            } else {
                // Set defaults
                properties.setProperty("lookAndFeel", UIManager.getSystemLookAndFeelClassName());
                properties.setProperty("background", "#000000");  // Default black background
                properties.setProperty("theme", "dark");
                properties.setProperty("isFirstRun", "true");
                properties.setProperty("isfullscreen", "false");
                saveSettings();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveSettings() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            properties.store(out, "MicrOS Settings");
            vfs.createFile(settingsPath, out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLookAndFeel() {
        return properties.getProperty("lookAndFeel", UIManager.getSystemLookAndFeelClassName());
    }

    public void setLookAndFeel(String className) {
        properties.setProperty("lookAndFeel", className);
        saveSettings();
    }

    public String getBackground() {
        return properties.getProperty("background", "/images/background.png");
    }

    public void setBackground(String path) {
        properties.setProperty("background", path);
        saveSettings();
    }

    public String getWallpaper() {
        return properties.getProperty("wallpaper", "/images/wallpaper.png");
    }

    public void setWallpaper(String path) {
        properties.setProperty("wallpaper", path);
        saveSettings();
    }

    public String getIsFirstRun() {
        return properties.getProperty("isFirstRun", "true");
    }

    public void setIsFirstRun(String isFirstRun) {
        properties.setProperty("isFirstRun", isFirstRun);
        saveSettings();
    }

    public Boolean getIsfullscreen() {
        return Boolean.parseBoolean(properties.getProperty("isfullscreen", "false"));
    }

    public void setIsfullscreen(String isfullscreen) {
        properties.setProperty("isfullscreen", isfullscreen);
        saveSettings();
    }

    public String getTheme() {
        return properties.getProperty("theme", "dark");
    }

    public void setTheme(String theme) {
        properties.setProperty("theme", theme);
        saveSettings();
    }
}
