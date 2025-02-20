package org.Finite.MicrOS.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import java.util.regex.*;

import org.Finite.MicrOS.core.VirtualFileSystem;
import org.json.*;
import java.io.*;
import java.util.List;
public class SyntaxHighlighter {
    private final JTextPane textPane;
    private final StyleContext styleContext;
    private final Map<String, SyntaxConfig> syntaxConfigs;
    private final VirtualFileSystem vfs;
    private SyntaxConfig currentConfig;

    public SyntaxHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        this.styleContext = StyleContext.getDefaultStyleContext();
        this.vfs = VirtualFileSystem.getInstance();
        this.syntaxConfigs = new HashMap<>();
        
        // Load syntax configurations
        loadSyntaxConfigs();
    }

    private void loadSyntaxConfigs() {
        try {
            // Create settings directory if it doesn't exist
            vfs.createDirectory("/system/texteditor/syntax");
            
            // Load all .json files from syntax directory
            File[] files = vfs.listFiles("/system/texteditor/syntax");
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".json")) {
                        loadSyntaxConfig(file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSyntaxConfig(File file) {
        try {
            String content = new String(vfs.readFile(vfs.getVirtualPath(file.toPath())));
            JSONObject json = new JSONObject(content);
            
            SyntaxConfig config = new SyntaxConfig();
            config.fileExtensions = new ArrayList<>();
            JSONArray extensions = json.getJSONArray("extensions");
            for (int i = 0; i < extensions.length(); i++) {
                config.fileExtensions.add(extensions.getString(i));
            }
            
            // Load syntax patterns
            JSONObject patterns = json.getJSONObject("patterns");
            config.patterns = new HashMap<>();
            
            for (String key : patterns.keySet()) {
                JSONObject pattern = patterns.getJSONObject(key);
                String regex = pattern.getString("regex");
                Color color = Color.decode(pattern.getString("color"));
                config.patterns.put(regex, styleContext.addAttribute(
                    SimpleAttributeSet.EMPTY,
                    StyleConstants.Foreground,
                    color
                ));
            }
            
            // Add config for each extension
            for (String ext : config.fileExtensions) {
                syntaxConfigs.put(ext, config);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFileType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        currentConfig = syntaxConfigs.getOrDefault(ext, null);
        highlightSyntax();
    }

    public void highlightSyntax() {
        if (currentConfig == null) return;
        
        SwingUtilities.invokeLater(() -> {
            String text = textPane.getText();
            StyledDocument doc = textPane.getStyledDocument();
            
            // Reset styles
            doc.setCharacterAttributes(0, text.length(), 
                SimpleAttributeSet.EMPTY, true);
    
            // Apply patterns
            for (Map.Entry<String, AttributeSet> entry : currentConfig.patterns.entrySet()) {
                Pattern pattern = Pattern.compile(entry.getKey(), 
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(text);
                
                while (matcher.find()) {
                    doc.setCharacterAttributes(
                        matcher.start(),
                        matcher.end() - matcher.start(),
                        entry.getValue(),
                        true
                    );
                }
            }
        });
    }

    public void updateColors() {
        // Reload current syntax configuration to refresh colors
        if (currentConfig != null) {
            String text = textPane.getText();
            textPane.setText(""); // Clear text to force refresh
            textPane.setText(text);
            highlightSyntax();
        }
    }

    private static class SyntaxConfig {
        List<String> fileExtensions;
        Map<String, AttributeSet> patterns;
    }
}
