package org.Finite.MicrOS;

import javax.swing.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import org.Finite.MicrOS.VirtualFileSystem;

/**
 * A simple text editor component with basic file operations and undo/redo support.
 */
public class TextEditor extends JPanel {
    protected JTextArea textArea;
    protected UndoManager undoManager;
    protected JToolBar toolBar;
    protected String currentFilePath;
    protected boolean hasUnsavedChanges;
    protected final VirtualFileSystem vfs;

    /**
     * Constructs a new TextEditor with the specified VirtualFileSystem.
     *
     * @param vfs The VirtualFileSystem instance
     */
    public TextEditor(VirtualFileSystem vfs) {
        this.vfs = vfs;
        setLayout(new BorderLayout());
        
        // Initialize text area with undo support
        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        undoManager = new UndoManager();
        textArea.getDocument().addUndoableEditListener(e -> {
            undoManager.addEdit(e.getEdit());
            hasUnsavedChanges = true;
            updateTitle();
        });

        // Create toolbar
        toolBar = new JToolBar();
        initializeToolbar();

        // Add components
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Add key bindings
        addKeyBindings();
    }

    /**
     * Initializes the toolbar with file and edit operation buttons.
     */
    private void initializeToolbar() {
        // File operations
        JButton newButton = new JButton("New");
        JButton openButton = new JButton("Open");
        JButton saveButton = new JButton("Save");
        JButton saveAsButton = new JButton("Save As");

        // Edit operations
        JButton undoButton = new JButton("Undo");
        JButton redoButton = new JButton("Redo");
        JButton cutButton = new JButton("Cut");
        JButton copyButton = new JButton("Copy");
        JButton pasteButton = new JButton("Paste");

        // Add file operation listeners
        newButton.addActionListener(e -> newFile());
        openButton.addActionListener(e -> openFile());
        saveButton.addActionListener(e -> saveFile());
        saveAsButton.addActionListener(e -> saveFileAs());

        // Add edit operation listeners
        undoButton.addActionListener(e -> undo());
        redoButton.addActionListener(e -> redo());
        cutButton.addActionListener(e -> textArea.cut());
        copyButton.addActionListener(e -> textArea.copy());
        pasteButton.addActionListener(e -> textArea.paste());

        // Add buttons to toolbar
        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.add(saveAsButton);
        toolBar.addSeparator();
        toolBar.add(undoButton);
        toolBar.add(redoButton);
        toolBar.addSeparator();
        toolBar.add(cutButton);
        toolBar.add(copyButton);
        toolBar.add(pasteButton);
    }

    /**
     * Adds key bindings for undo, redo, and save operations.
     */
    private void addKeyBindings() {
        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = textArea.getActionMap();

        // Undo/Redo bindings
        KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        KeyStroke redoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

        inputMap.put(undoKeyStroke, "Undo");
        inputMap.put(redoKeyStroke, "Redo");

        actionMap.put("Undo", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                undo();
            }
        });
        actionMap.put("Redo", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                redo();
            }
        });

        // Add save key binding
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(saveKeyStroke, "Save");
        textArea.getActionMap().put("Save", new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                saveFile();
            }
        });
    }

    /**
     * Creates a new file, prompting the user to save changes if there are unsaved changes.
     */
    private void newFile() {
        if (hasUnsavedChanges) {
            int result = JOptionPane.showConfirmDialog(this,
                "Do you want to save changes?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                if (!saveFile()) return;
            } else if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        
        textArea.setText("");
        currentFilePath = null;
        hasUnsavedChanges = false;
        undoManager.discardAllEdits();
        updateTitle();
    }

    /**
     * Opens a file, prompting the user to enter the file path.
     */
    private void openFile() {
        // Create custom file chooser dialog
        String fileName = JOptionPane.showInputDialog(this, 
            "Enter file path to open (e.g., /docs/file.txt):", 
            "Open File", 
            JOptionPane.QUESTION_MESSAGE);
            
        if (fileName != null && !fileName.trim().isEmpty()) {
            try {
                byte[] content = vfs.readFile(fileName);
                textArea.setText(new String(content));
                currentFilePath = fileName;
                hasUnsavedChanges = false;
                undoManager.discardAllEdits();
                updateTitle();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error opening file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Saves the current file. If the file has not been saved before, prompts the user to enter a file path.
     *
     * @return true if the file was saved successfully, false otherwise
     */
    private boolean saveFile() {
        if (currentFilePath == null) {
            return saveFileAs();
        }
        
        try {
            vfs.createFile(currentFilePath, textArea.getText().getBytes());
            hasUnsavedChanges = false;
            updateTitle();
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error saving file: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Prompts the user to enter a file path and saves the file.
     *
     * @return true if the file was saved successfully, false otherwise
     */
    private boolean saveFileAs() {
        // Create custom file save dialog
        String fileName = JOptionPane.showInputDialog(this,
            "Enter file path to save (e.g., /docs/file.txt):",
            "Save File As",
            JOptionPane.QUESTION_MESSAGE);
            
        if (fileName != null && !fileName.trim().isEmpty()) {
            currentFilePath = fileName;
            return saveFile();
        }
        return false;
    }

    /**
     * Updates the title of the parent JInternalFrame to reflect the current file path and unsaved changes.
     */
    private void updateTitle() {
        if (getParent() instanceof JInternalFrame) {
            String title = currentFilePath != null ? 
                currentFilePath : "Untitled";
            if (hasUnsavedChanges) title += "*";
            ((JInternalFrame) getParent()).setTitle(title);
        }
    }

    /**
     * Sets the text content of the text editor.
     *
     * @param text The text content to set
     */
    public void setText(String text) {
        textArea.setText(text);
        undoManager.discardAllEdits();
    }

    /**
     * Gets the text content of the text editor.
     *
     * @return The text content
     */
    public String getText() {
        return textArea.getText();
    }

    /**
     * Undoes the last edit operation.
     */
    public void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
    }

    /**
     * Redoes the last undone edit operation.
     */
    public void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
    }

    /**
     * Checks if there are unsaved changes in the text editor.
     *
     * @return true if there are unsaved changes, false otherwise
     */
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    /**
     * Loads a file from the virtual file system into the text editor.
     *
     * @param virtualPath Path to the file in the virtual file system
     */
    public void loadFile(String virtualPath) {
        try {
            byte[] content = vfs.readFile(virtualPath);
            setText(new String(content));
            currentFilePath = virtualPath;
            hasUnsavedChanges = false;
            updateTitle();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading file: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
