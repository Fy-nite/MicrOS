package org.finite.texteditor.ui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

public class LineNumberComponent extends JPanel {
    private final JTextPane textArea;
    private final JLabel lineLabel;
    private static final int MARGIN = 5;
    private final JScrollPane scrollPane;

    public LineNumberComponent(JTextPane textArea, JScrollPane scrollPane) {
        this.textArea = textArea;
        this.scrollPane = scrollPane;
        setPreferredWidth();
        
        lineLabel = new JLabel();
        lineLabel.setFont(textArea.getFont());
        lineLabel.setForeground(new Color(180, 180, 180));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(60, 60, 60)));
        setBackground(new Color(40, 40, 40));
        setOpaque(true);
        
        // Use BoxLayout to stack components vertically
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(lineLabel);

        // Listen for both document changes and key events
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateLineNumbers(); }
            public void removeUpdate(DocumentEvent e) { updateLineNumbers(); }
            public void changedUpdate(DocumentEvent e) { updateLineNumbers(); }
        });

        // Listen for scrolling
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> updateLineNumbers());

        // Add key listener for Enter key
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> updateLineNumbers());
                }
            }
        });
    }

    private void setPreferredWidth() {
        int lineCount = textArea.getText().split("\n").length;
        int width = getFontMetrics(textArea.getFont())
                   .stringWidth(String.valueOf(lineCount)) + 2 * MARGIN;
        setPreferredSize(new Dimension(width, 0));
    }

    private void updateLineNumbers() {
        try {
            Rectangle visible = textArea.getVisibleRect();
            int startOffset = textArea.viewToModel(visible.getLocation());
            int endOffset = textArea.viewToModel(new Point(
                visible.x, visible.y + visible.height));
            
            Document doc = textArea.getDocument();
            Element root = doc.getDefaultRootElement();
            
            int startLine = root.getElementIndex(startOffset);
            int endLine = root.getElementIndex(endOffset);
            
            StringBuilder numbers = new StringBuilder("<html>");
            numbers.append("<div style='text-align: right;'>");
            
            for (int i = startLine; i <= endLine; i++) {
                numbers.append(String.format("<div style='padding: 0 %dpx;'>%d</div>", 
                    MARGIN, i + 1));
            }
            
            numbers.append("</div></html>");
            lineLabel.setText(numbers.toString());
            setPreferredWidth();
            revalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
