package org.Finite.MicrOS.ui;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.Finite.MicrOS.core.VirtualFileSystem;

public class FontLoader {
    private static final Map<String, Font> loadedFonts = new HashMap<>();
    private static final VirtualFileSystem vfs = VirtualFileSystem.getInstance();
    
    public static void loadSystemFont(String name, String virtualPath) {
        try {
            byte[] fontData = vfs.readFile(virtualPath);
            Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fontData));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            loadedFonts.put(name, font);
        } catch (FontFormatException | IOException e) {
            System.err.println("Error loading font: " + name + " - " + e.getMessage());
            // Fallback to default font
            loadedFonts.put(name, new Font("Dialog", Font.PLAIN, 12));
        }
    }
    
    public static void loadResourceFont(String name, String resourcePath) {
        // Convert resource path to VFS path
        String vfsPath = "/system/fonts" + resourcePath;
        loadSystemFont(name, vfsPath);
    }
    
    public static void initializeDefaultFonts() {
        // Create fonts directory if it doesn't exist
        try {
            if (!vfs.exists("/system/fonts")) {
                vfs.createDirectory("/system/fonts");
            }
            
            // Extract default fonts from resources to VFS
            copyResourceToVFS("/fonts/JetBrainsMono-Regular.ttf", "/system/fonts/JetBrainsMono-Regular.ttf");
            copyResourceToVFS("/fonts/SegoeUI.ttf", "/system/fonts/SegoeUI.ttf");
            copyResourceToVFS("/fonts/SegoeUI-Bold.ttf", "/system/fonts/SegoeUI-Bold.ttf");
        } catch (IOException e) {
            System.err.println("Error initializing fonts: " + e.getMessage());
        }
    }
    
    private static void copyResourceToVFS(String resourcePath, String vfsPath) throws IOException {
        try (var is = FontLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Font resource not found: " + resourcePath);
            }
            byte[] fontData = is.readAllBytes();
            vfs.createFile(vfsPath, fontData);
        }
    }

    public static Font getFont(String name, int style, float size) {
        Font baseFont = loadedFonts.get(name);
        if (baseFont == null) {
            return new Font("Dialog", style, (int)size);
        }
        return baseFont.deriveFont(style, size);
    }
}
