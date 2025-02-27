package org.Finite.MicrOS.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JOptionPane;

import org.Finite.MicrOS.Main;
import org.Finite.MicrOS.apps.AppLoader;
import org.Finite.MicrOS.core.VirtualFileSystem;

public class AppInstaller {

    public static void extractZip(File zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath);
                }
                zis.closeEntry();
            }
        }
    }

    public static boolean isAppDirectory(Path dir) {
        return Files.isDirectory(dir) && dir.getFileName().toString().endsWith(".app");
    }

    public static boolean installApp(Path appDir) {
        try {
            // Verify this is a valid app directory
            if (!isAppDirectory(appDir)) {
                JOptionPane.showMessageDialog(null, 
                    "Not a valid MicrOS application: " + appDir.getFileName(),
                    "Installation Failed", 
                    JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            // Check for manifest file
            Path manifestPath = appDir.resolve("Contents/manifest.json");
            if (!Files.exists(manifestPath)) {
                JOptionPane.showMessageDialog(null, 
                    "Missing manifest.json in application: " + appDir.getFileName(),
                    "Installation Failed", 
                    JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            // Get the VFS and determine the target directory
            VirtualFileSystem vfs = VirtualFileSystem.getInstance();
            Path appsDir = vfs.resolveVirtualPath("/apps");
            
            // Create apps directory if it doesn't exist
            if (!Files.exists(appsDir)) {
                Files.createDirectories(appsDir);
            }
            
            // Target path for the app
            Path targetAppDir = appsDir.resolve(appDir.getFileName());
            
            // Check if app already exists
            if (Files.exists(targetAppDir)) {
                int response = JOptionPane.showConfirmDialog(null,
                    "Application already exists. Do you want to replace it?",
                    "Confirm Replace",
                    JOptionPane.YES_NO_OPTION);
                
                if (response != JOptionPane.YES_OPTION) {
                    return false;
                }
                
                // Delete existing app
                deleteDirectory(targetAppDir);
            }
            
            // Copy app directory to apps directory
            copyDirectory(appDir, targetAppDir);
            
            // Register the app with the system
            AppLoader appLoader = vfs.getAppLoader();
            if (appLoader != null) {
                appLoader.loadApp(targetAppDir.toFile()); // Fixed: Converting Path to File
                
                JOptionPane.showMessageDialog(null,
                    "Application installed successfully. It will be available after restart.",
                    "Installation Complete",
                    JOptionPane.INFORMATION_MESSAGE);
                
                return true;
            } else {
                JOptionPane.showMessageDialog(null,
                    "App installed but couldn't be registered with the system.",
                    "Installation Warning",
                    JOptionPane.WARNING_MESSAGE);
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error installing application: " + e.getMessage(),
                "Installation Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }
    
    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectory(targetPath);
                        }
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }
    
    // Changed to public to allow access from other classes
    public static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        
        Files.walk(directory)
            .sorted((path1, path2) -> -path1.compareTo(path2)) // Delete files before directories
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }
}
