package org.finite.texteditor;

import javax.swing.*;
import java.io.IOException;
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
        // No cleanup needed yet
    }

    public void setText(String text) {
        if (editorPanel != null) {
            editorPanel.setText(text);
        }
    }

    public String getText() {
        return editorPanel != null ? editorPanel.getText() : "";
    }

    public void openFile(String virtualPath) {
        if (editorPanel != null) {
            editorPanel.openFile(virtualPath);
        }
    }
}
