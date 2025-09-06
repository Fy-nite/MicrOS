package org.Finite.MicrOS.apps;

import java.awt.*;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

import org.Finite.MicrOS.core.ProcessManager;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.ui.Console;

public class ApplicationLauncher {

    private final WindowManager windowManager;
    private final VirtualFileSystem vfs;

    public ApplicationLauncher(WindowManager windowManager) {
        this.windowManager = windowManager;
        this.vfs = VirtualFileSystem.getInstance();
    }

    public void openApplication(File file) {
        String virtualPath = vfs.getVirtualPath(file.toPath());
        String extension = getFileExtension(file);
        String mimeType = getMimeType(file);

        // Create a new internal frame for the application
        String windowId = "MicrOS-" + System.currentTimeMillis();
        String title = file.getName();

        try {
            if (isTextFile(mimeType)) {
                openTextFile(file, windowId, title);
            } else if (isImageFile(mimeType)) {
                openImageFile(file, windowId, title);
            } else if (isWebFile(extension)) {
                openWebFile(file, windowId, title);
            } else {
                // Fall back to native application in a controlled process
                openNativeApplication(file, windowId, title);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                null,
                "Error opening file: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void openTextFile(File file, String windowId, String title)
        throws IOException {
        JInternalFrame frame = windowManager.createWindow(
            windowId,
            "Text Editor - " + title,
            true
        );
        String content = new String(vfs.readFile(vfs.getVirtualPath(file.toPath())));
        JTextArea textArea = new JTextArea(content);
        frame.add(new JScrollPane(textArea));
        frame.setVisible(true);
    }

    private void openImageFile(File file, String windowId, String title)
        throws IOException {
        JInternalFrame frame = windowManager.createWindow(
            windowId,
            "Image Viewer - " + title,
            false
        );
        ImageIcon image = new ImageIcon(file.getPath());
        JLabel label = new JLabel(image);
        frame.add(new JScrollPane(label));
        frame.setVisible(true);
    }

    private void openWebFile(File file, String windowId, String title) {
        JInternalFrame frame = windowManager.createWindow(
            windowId,
            "Web Viewer - " + title,
            false
        );

        JEditorPane webPane = new JEditorPane();
        webPane.setEditable(false);
        try {
            webPane.setPage(file.toURI().toURL());
        } catch (IOException e) {
            webPane.setText("Error loading page: " + e.getMessage());
        }
        frame.add(new JScrollPane(webPane));
        frame.setVisible(true);
    }

    private void openNativeApplication(
        File file,
        String windowId,
        String title
    ) throws IOException {
        if (!file.canExecute()) {
            // Try to make executable if possible
            file.setExecutable(true);
        }
        
        if (file.canExecute()) {
            windowManager.launchNativeApp(file.getAbsolutePath());
        } else {
            JOptionPane.showMessageDialog(null,
                "Cannot execute file: " + file.getName() + "\nFile is not executable.",
                "Execution Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf + 1).toLowerCase();
    }

    private String getMimeType(File file) {
        try {
            return java.nio.file.Files.probeContentType(file.toPath());
        } catch (IOException e) {
            return "";
        }
    }

    private boolean isTextFile(String mimeType) {
        return (
            mimeType != null &&
            (mimeType.startsWith("text/") ||
                mimeType.equals("application/json") ||
                mimeType.equals("application/xml"))
        );
    }

    private boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    private boolean isWebFile(String extension) {
        return extension.equals("html") || extension.equals("htm");
    }

    private boolean isNativeExecutable(File file) {
        String name = file.getName().toLowerCase();
        return file.canExecute() || 
               name.endsWith(".exe") || 
               name.endsWith(".sh") ||
               name.endsWith(".app") ||
               !name.contains(".");  // Unix executables often have no extension
    }
}
