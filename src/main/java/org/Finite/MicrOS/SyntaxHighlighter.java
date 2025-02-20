package org.Finite.MicrOS;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.*;

public class SyntaxHighlighter {
    private final JTextPane textPane;
    private final StyleContext styleContext;
    private final AttributeSet keywordStyle;
    private final AttributeSet stringStyle;
    private final AttributeSet commentStyle;
    private final AttributeSet numberStyle;
    private final AttributeSet registerStyle;

    private static final String[] KEYWORDS = {
        "mov", "add", "sub", "mul", "div", "mod", "inc", "dec",
        "and", "or", "xor", "not", "shl", "shr", "push", "pop",
        "cmp", "jmp", "je", "jne", "jg", "jge", "jl", "jle",
        "call", "ret", "int", "iret", "hlt"
    };

    public SyntaxHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        this.styleContext = StyleContext.getDefaultStyleContext();
        
        // Initialize styles
        this.keywordStyle = styleContext.addAttribute(
            SimpleAttributeSet.EMPTY, 
            StyleConstants.Foreground, 
            new Color(86, 156, 214)
        );
        this.stringStyle = styleContext.addAttribute(
            SimpleAttributeSet.EMPTY, 
            StyleConstants.Foreground, 
            new Color(206, 145, 120)
        );
        this.commentStyle = styleContext.addAttribute(
            SimpleAttributeSet.EMPTY, 
            StyleConstants.Foreground, 
            new Color(87, 166, 74)
        );
        this.numberStyle = styleContext.addAttribute(
            SimpleAttributeSet.EMPTY, 
            StyleConstants.Foreground, 
            new Color(181, 206, 168)
        );
        this.registerStyle = styleContext.addAttribute(
            SimpleAttributeSet.EMPTY, 
            StyleConstants.Foreground, 
            new Color(220, 220, 170)
        );
    }

    public void highlightSyntax() {
        SwingUtilities.invokeLater(() -> {
            String text = textPane.getText();
            StyledDocument doc = textPane.getStyledDocument();
            
            // Reset styles
            doc.setCharacterAttributes(0, text.length(), 
                SimpleAttributeSet.EMPTY, true);
    
            // Highlight patterns
            highlightPattern(doc, text, "//.*$", commentStyle);
            highlightPattern(doc, text, "\"[^\"]*\"", stringStyle);
            highlightPattern(doc, text, "\\b\\d+\\b", numberStyle);
            
            // Add register pattern - common x86 registers
            highlightPattern(doc, text, "\\b(ax|bx|cx|dx|si|di|sp|bp|" +
                "al|ah|bl|bh|cl|ch|dl|dh|" +
                "RAX|RBX|RCX|RDX|RSI|RDI|RSP|RBP|" +
                "r\\d+[bdw]?)\\b", registerStyle);
            
            // Highlight keywords
            for (String keyword : KEYWORDS) {
                highlightPattern(doc, text, "\\b" + keyword + "\\b", keywordStyle);
            }
        });
    }

    private void highlightPattern(StyledDocument doc, String text, 
                                String patternStr, AttributeSet style) {
        Pattern pattern = Pattern.compile(patternStr, 
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(
                matcher.start(), 
                matcher.end() - matcher.start(), 
                style, 
                true
            );
        }
    }

    public void updateColors() {
        highlightSyntax();
    }
}
