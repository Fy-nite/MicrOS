package org.finite.texteditor.ui;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import org.Finite.MicrOS.core.VirtualFileSystem;
import org.finite.texteditor.syntax.SyntaxHighlighter;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

public class TextEditorPanel extends JPanel {
    private final JTextPane textArea;
    private final VirtualFileSystem vfs;
    private final JLabel statusBar;
    private final LineNumberComponent lineNumbers;
    private boolean darkMode = true;
    private final SyntaxHighlighter syntaxHighlighter;
    private String currentFilePath = null;
    private boolean hasUnsavedChanges = false;
    
    private static final Color DARK_BG = new Color(30, 30, 30);
    private static final Color DARK_FG = new Color(220, 220, 220);
    private static final Color LIGHT_BG = new Color(250, 250, 250);
    private static final Color LIGHT_FG = new Color(20, 20, 20);

    public TextEditorPanel(VirtualFileSystem vfs) {  // Changed from TextEditor to TextEditorPanel
        this.vfs = vfs;
        setLayout(new BorderLayout());

        // Create text area with custom StyledDocument
        textArea = new JTextPane();
        syntaxHighlighter = new SyntaxHighlighter(textArea);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Create scroll pane first
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        // Create line numbers with scroll pane reference
        lineNumbers = new LineNumberComponent(textArea, scrollPane);
        
        // Create status bar
        statusBar = new JLabel(" Line: 1, Column: 1");
        
        // Create toolbar
        JToolBar toolbar = createToolbar();

        // Layout components
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.add(lineNumbers, BorderLayout.WEST);
        editorPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(toolbar, BorderLayout.NORTH);
        add(editorPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // Add listeners
        setupListeners();
        
        // Initial appearance
        setDarkMode(darkMode);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // File management buttons
        JButton openBtn = new JButton("Open");
        JButton saveBtn = new JButton("Save");
        JButton saveAsBtn = new JButton("Save As");
        JButton newBtn = new JButton("New");

        // Add file management actions
        openBtn.addActionListener(e -> openFile());
        saveBtn.addActionListener(e -> saveFile());
        saveAsBtn.addActionListener(e -> saveFileAs());
        newBtn.addActionListener(e -> newFile());

        // Add file management buttons
        toolbar.add(newBtn);
        toolbar.add(openBtn);
        toolbar.add(saveBtn);
        toolbar.add(saveAsBtn);
        toolbar.addSeparator();

        // Dark mode toggle
        JToggleButton darkModeBtn = new JToggleButton("Dark Mode");
        darkModeBtn.setSelected(darkMode);
        darkModeBtn.addActionListener(e -> setDarkMode(darkModeBtn.isSelected()));

        // Find/Replace
        JButton findBtn = new JButton("Find");
        findBtn.addActionListener(e -> showFindDialog());

        // Auto-indent
        JButton indentBtn = new JButton("Auto-indent");
        indentBtn.addActionListener(e -> autoIndent());

        toolbar.add(darkModeBtn);
        toolbar.add(findBtn);
        toolbar.add(indentBtn);
        return toolbar;
    }

    private void setupListeners() {
        // Update status bar and syntax highlighting
        textArea.addCaretListener(e -> {
            updateStatusBar();
            syntaxHighlighter.highlightSyntax();
        });

        // Add key bindings
        InputMap inputMap = textArea.getInputMap();
        ActionMap actionMap = textArea.getActionMap();

        // Ctrl+F for find
        KeyStroke findKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK);
        inputMap.put(findKey, "find");
        actionMap.put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFindDialog();
            }
        });

        // Tab key for indentation
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    e.consume();
                    insertTab();
                }
            }
        });

        // Add document change listener
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { documentChanged(); }
            public void removeUpdate(DocumentEvent e) { documentChanged(); }
            public void changedUpdate(DocumentEvent e) { documentChanged(); }
        });
    }

    private void updateStatusBar() {
        try {
            int pos = textArea.getCaretPosition();
            int line = textArea.getDocument().getDefaultRootElement()
                    .getElementIndex(pos) + 1;
            int column = pos - textArea.getDocument().getDefaultRootElement()
                    .getElement(line - 1).getStartOffset() + 1;
            statusBar.setText(String.format(" Line: %d, Column: %d", line, column));
        } catch (Exception e) {
            statusBar.setText(" Error getting position");
        }
    }

    private void setDarkMode(boolean dark) {
        darkMode = dark;
        Color bgColor = dark ? DARK_BG : LIGHT_BG;
        Color fgColor = dark ? DARK_FG : LIGHT_FG;
        Color lineNumBg = dark ? new Color(40, 40, 40) : new Color(230, 230, 230);
        Color lineNumFg = dark ? new Color(180, 180, 180) : new Color(90, 90, 90);
        Color borderColor = dark ? new Color(60, 60, 60) : new Color(200, 200, 200);

        textArea.setBackground(bgColor);
        textArea.setForeground(fgColor);
        textArea.setCaretColor(fgColor);
        
        lineNumbers.setBackground(lineNumBg);
        lineNumbers.setForeground(lineNumFg);
        lineNumbers.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor));
        
        statusBar.setBackground(bgColor);
        statusBar.setForeground(fgColor);
        syntaxHighlighter.updateColors();
    }

    private void showFindDialog() {
        JDialog dialog = new JDialog((Frame)null, "Find/Replace", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField findField = new JTextField();
        JTextField replaceField = new JTextField();
        JCheckBox matchCase = new JCheckBox("Match case");
        
        panel.add(new JLabel("Find:"));
        panel.add(findField);
        panel.add(new JLabel("Replace with:"));
        panel.add(replaceField);
        panel.add(matchCase);
        
        JPanel buttons = new JPanel();
        JButton findNext = new JButton("Find Next");
        JButton replace = new JButton("Replace");
        JButton replaceAll = new JButton("Replace All");
        
        findNext.addActionListener(e -> findText(findField.getText(), matchCase.isSelected()));
        replace.addActionListener(e -> replaceText(findField.getText(), 
                                                 replaceField.getText(), 
                                                 matchCase.isSelected()));
        replaceAll.addActionListener(e -> replaceAllText(findField.getText(), 
                                                       replaceField.getText(), 
                                                       matchCase.isSelected()));
        
        buttons.add(findNext);
        buttons.add(replace);
        buttons.add(replaceAll);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void findText(String text, boolean matchCase) {
        String content = textArea.getText();
        String searchText = matchCase ? text : text.toLowerCase();
        if (!matchCase) content = content.toLowerCase();
        
        int start = textArea.getCaretPosition();
        int index = content.indexOf(searchText, start);
        
        if (index >= 0) {
            textArea.setCaretPosition(index);
            textArea.moveCaretPosition(index + text.length());
            textArea.requestFocusInWindow();
        } else {
            JOptionPane.showMessageDialog(this, "Text not found");
        }
    }

    private void replaceText(String find, String replace, boolean matchCase) {
        if (textArea.getSelectedText() != null) {
            String selection = textArea.getSelectedText();
            if (matchCase ? selection.equals(find) : selection.equalsIgnoreCase(find)) {
                textArea.replaceSelection(replace);
            }
        }
        findText(find, matchCase);
    }

    private void replaceAllText(String find, String replace, boolean matchCase) {
        String content = textArea.getText();
        String result;
        if (matchCase) {
            result = content.replace(find, replace);
        } else {
            result = content.replaceAll("(?i)" + Pattern.quote(find), replace);
        }
        textArea.setText(result.toString());
    }

    private void autoIndent() {
        String[] lines = textArea.getText().split("\n");
        StringBuilder result = new StringBuilder();
        int indent = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.endsWith("{")) {
                result.append("    ".repeat(indent)).append(trimmed).append("\n");
                indent++;
            } else if (trimmed.startsWith("}")) {
                indent = Math.max(0, indent - 1);
                result.append("    ".repeat(indent)).append(trimmed).append("\n");
            } else {
                result.append("    ".repeat(indent)).append(trimmed).append("\n");
            }
        }
        
        textArea.setText(result.toString());
    }

    private void insertTab() {
        try {
            Document doc = textArea.getDocument();
            doc.insertString(textArea.getCaretPosition(), "    ", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void setText(String text) {
        textArea.setText(text);
        syntaxHighlighter.highlightSyntax();
    }

    public String getText() {
        return textArea.getText();
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser(vfs.getRootPath().toFile());
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String virtualPath = vfs.getVirtualPath(file.toPath());
                byte[] content = vfs.readFile(virtualPath);
                setText(new String(content));
                currentFilePath = virtualPath;
                hasUnsavedChanges = false;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening file: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void openFile(String virtualPath) {
        try {
            String content = new String(vfs.readFile(virtualPath));
            setText(content);
            syntaxHighlighter.setFileType(virtualPath);
            currentFilePath = virtualPath;
            hasUnsavedChanges = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFile() {
        if (currentFilePath == null) {
            saveFileAs();
        } else {
            try {
                vfs.createFile(currentFilePath, getText().getBytes());
                hasUnsavedChanges = false;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error saving file: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFileAs() {
        JFileChooser fileChooser = new JFileChooser(vfs.getRootPath().toFile());
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String virtualPath = vfs.getVirtualPath(file.toPath());
                vfs.createFile(virtualPath, getText().getBytes());
                currentFilePath = virtualPath;
                hasUnsavedChanges = false;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error saving file: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void newFile() {
        if (hasUnsavedChanges) {
            int response = JOptionPane.showConfirmDialog(this,
                "Do you want to save changes?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION);
            
            if (response == JOptionPane.YES_OPTION) {
                saveFile();
            } else if (response == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        setText("");
        currentFilePath = null;
        hasUnsavedChanges = false;
    }

    private void documentChanged() {
        hasUnsavedChanges = true;
    }
}
