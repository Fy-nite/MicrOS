package org.finite.texteditor;

import javax.swing.*;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.finite.texteditor.ui.TextEditorPanel;

public class TextEditorApp extends MicrOSApp {
    private TextEditorPanel editorPanel;

    @Override
    public JComponent createUI() {
        editorPanel = new TextEditorPanel(vfs);
        return editorPanel;
    }

    @Override
    public void onStart() {
        // Initialize any needed resources
    }

    @Override
    public void onStop() {
        // Cleanup resources
        if (editorPanel != null) {
            editorPanel.cleanup();
        }
    }
     /**
     * Sets text content in a text editor window.
     *
     * @param windowId Text editor window identifier
     * @param text Text to set
     * @return true if successful, false if window not found/not a text editor
     */
    public boolean setEditorText(String windowId, String text) {
        JInternalFrame frame = windows.get(windowId);
        if (frame != null) {
            TextEditor editor = (TextEditor) frame.getClientProperty("editor");
            if (editor != null) {
                editor.setText(text);
                return true;
            }
        }
        return false;
    }

    /**
     * Opens a file in a text editor window using the VFS.
     *
     * @param virtualPath Path to the file in the VFS
     * @return The created window frame
     */
    public JInternalFrame openFileInEditor(String virtualPath) {
        String windowId = "editor-" + virtualPath.hashCode();
        JInternalFrame frame = createWindow(windowId, virtualPath, "texteditor");
        TextEditor editor = (TextEditor) frame.getContentPane().getComponent(0); // Get editor component
        String fileContent = "";
        try {
            fileContent = new String(vfs.readFile(virtualPath));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // Convert byte[] to String
        editor.setText(fileContent);
        return frame;
    }

    public void openFile(String virtualPath) {
        editorPanel.openFile(virtualPath);
    }
}
