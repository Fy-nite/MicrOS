package org.Finite.MicrOS.syntax;

import javax.swing.text.*;
import javax.swing.JTextPane;
import org.json.JSONObject;
import java.awt.Color;
import java.util.*;
import java.util.regex.*;

public class SyntaxHighlighter {
    private final JTextPane textPane;
    private final StyleContext styleContext;
    private final Map<String, SyntaxConfig> syntaxConfigs;
    private final Map<String, Color> themeColors;
    private SyntaxConfig currentConfig;
    
    public SyntaxHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        this.styleContext = StyleContext.getDefaultStyleContext();
        this.syntaxConfigs = new HashMap<>();
        this.themeColors = new HashMap<>();
    }

    public void updateTheme(JSONObject syntaxTheme) {
        themeColors.clear();
        for (String key : syntaxTheme.keySet()) {
            themeColors.put("${syntax." + key + "}", Color.decode(syntaxTheme.getString(key)));
        }
        highlightSyntax(); // Reapply with new colors
    }

    public void loadSyntaxConfig(JSONObject config) {
        SyntaxConfig syntaxConfig = new SyntaxConfig();
        syntaxConfig.patterns = new HashMap<>();

        JSONObject patterns = config.getJSONObject("patterns");
        for (String key : patterns.keySet()) {
            JSONObject pattern = patterns.getJSONObject(key);
            String regex = pattern.getString("regex");
            String colorVar = pattern.getString("color");
            syntaxConfig.patterns.put(Pattern.compile(regex, Pattern.MULTILINE), colorVar);
        }

        for (String ext : config.getJSONArray("extensions").toList().toArray(new String[0])) {
            syntaxConfigs.put(ext.toLowerCase(), syntaxConfig);
        }
    }

    public void setFileType(String filename) {
        String ext = getFileExtension(filename);
        currentConfig = syntaxConfigs.get(ext.toLowerCase());
    }

    public void highlightSyntax() {
        if (currentConfig == null) return;

        String text = textPane.getText();
        StyledDocument doc = textPane.getStyledDocument();
        
        // Reset styles
        Style defaultStyle = styleContext.getStyle(StyleContext.DEFAULT_STYLE);
        doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, true);

        // Apply each pattern
        for (Map.Entry<Pattern, String> entry : currentConfig.patterns.entrySet()) {
            Matcher matcher = entry.getKey().matcher(text);
            Color color = themeColors.get(entry.getValue());
            if (color == null) continue;

            AttributeSet attrs = styleContext.addAttribute(defaultStyle, 
                StyleConstants.Foreground, color);

            while (matcher.find()) {
                doc.setCharacterAttributes(matcher.start(), 
                    matcher.end() - matcher.start(), attrs, false);
            }
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private static class SyntaxConfig {
        Map<Pattern, String> patterns;
    }
}
