package org.Finite.MicrOS.apps;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;

public class AppInstaller extends MicrOSApp {
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private File zipFile;
    
    public AppInstaller(File zipFile) {
        this.zipFile = zipFile;
        AppManifest manifest = new AppManifest();
        manifest.setName("App Installer");
        manifest.setAppType(AppType.APP_INSTALLER);
        setManifest(manifest);
    }

    @Override
    public JComponent createUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        statusLabel = new JLabel("Installing application...");
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        
        // Start installation process
        SwingUtilities.invokeLater(this::installApp);
        
        return panel;
    }

    private void installApp() {
        try {
            String appsDir = vfs.resolveVirtualPath("/apps").toString();
            File tempDir = Files.createTempDirectory("micros_install_").toFile();
            
            // Extract zip
            statusLabel.setText("Extracting application...");
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File newFile = new File(tempDir, entry.getName());
                    if (entry.isDirectory()) {
                        newFile.mkdirs();
                        continue;
                    }
                    newFile.getParentFile().mkdirs();
                    Files.copy(zis, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            
            // Validate extracted contents
            File[] appBundles = tempDir.listFiles((dir, name) -> name.endsWith(".app"));
            if (appBundles == null || appBundles.length == 0) {
                throw new Exception("No .app bundle found in archive");
            }
            
            // Move app bundle to apps directory
            statusLabel.setText("Installing application...");
            File appBundle = appBundles[0];
            File destDir = new File(appsDir, appBundle.getName());
            if (destDir.exists()) {
                int choice = JOptionPane.showConfirmDialog(
                    null,
                    "An application with this name already exists. Replace it?",
                    "Replace Application",
                    JOptionPane.YES_NO_OPTION
                );
                if (choice != JOptionPane.YES_OPTION) {
                    throw new Exception("Installation cancelled by user");
                }
                Files.walk(destDir.toPath())
                     .sorted((a, b) -> b.compareTo(a))
                     .map(Path::toFile)
                     .forEach(File::delete);
            }
            
            Files.move(appBundle.toPath(), destDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // Load the app
            statusLabel.setText("Loading application...");
            String appId = vfs.getAppLoader().loadAppFromPath(destDir);
            
            // Show success and offer to launch
            statusLabel.setText("Installation complete!");
            progressBar.setIndeterminate(false);
            progressBar.setValue(100);
            
            int launch = JOptionPane.showConfirmDialog(
                null,
                "Application installed successfully. Would you like to launch it now?",
                "Installation Complete",
                JOptionPane.YES_NO_OPTION
            );
            
            if (launch == JOptionPane.YES_OPTION) {
                windowManager.launchAppById(appId);
            }
            
            // Close installer window
            SwingUtilities.getWindowAncestor(statusLabel).dispose();
            
        } catch (Exception e) {
            statusLabel.setText("Installation failed: " + e.getMessage());
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
            reportError("Failed to install application", e);
        }
    }

    @Override
    public void onStart() {
        // Nothing to do
    }

    @Override
    public void onStop() {
        // Nothing to do
    }
}