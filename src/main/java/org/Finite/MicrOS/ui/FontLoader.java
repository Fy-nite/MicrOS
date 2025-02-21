package org.Finite.MicrOS.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontLoader {
    private static final Map<String, Font> loadedFonts = new HashMap<>();
    
    public static void loadSystemFont(String name, String path) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, new File(path));
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            loadedFonts.put(name, font);
        } catch (FontFormatException | IOException e) {
            System.err.println("Error loading font: " + name + " - " + e.getMessage());
        }
    }
    
    public static void loadResourceFont(String name, String resourcePath) {
        try {
            InputStream is = FontLoader.class.getResourceAsStream(resourcePath);
            if (is == null) {
                throw new IOException("Font resource not found: " + resourcePath);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            loadedFonts.put(name, font);
        } catch (FontFormatException | IOException e) {
            System.err.println("Error loading font resource: " + name + " - " + e.getMessage());
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
