package org.Finite.MicrOS.ui.theme;

import javafx.scene.Parent;
import javafx.scene.Scene;
import org.Finite.MicrOS.core.VirtualFileSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper class for managing JavaFX themes in MicrOS
 */
public class JavaFXTheme {
    private static final String DEFAULT_THEME_PATH = "/system/texteditor/themes/javafx-dark.css";
    
    /**
     * Apply the default MicrOS theme to a JavaFX scene or node
     * @param target The Scene or Parent node to apply the theme to
     */
    public static void applyDefaultTheme(Scene target) {
        try {
            String css = loadThemeContent(DEFAULT_THEME_PATH);
            target.getStylesheets().clear();
            target.getStylesheets().add("data:text/css," + css.replace("\n", ""));
        } catch (IOException e) {
            System.err.println("Failed to load JavaFX theme: " + e.getMessage());
        }
    }
    
    /**
     * Apply the default MicrOS theme to a JavaFX node
     * @param target The Parent node to apply the theme to
     */
    public static void applyDefaultTheme(Parent target) {
        try {
            String css = loadThemeContent(DEFAULT_THEME_PATH);
            target.getStylesheets().clear();
            target.getStylesheets().add("data:text/css," + css.replace("\n", ""));
        } catch (IOException e) {
            System.err.println("Failed to load JavaFX theme: " + e.getMessage());
        }
    }
    
    private static String loadThemeContent(String themePath) throws IOException {
        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        Path path = vfs.resolveVirtualPath(themePath);
        return Files.readString(path);
    }
}