package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;

/**
 * MicrOSWindow is a custom internal window for MicrOS applications.
 * Developers can use this to open additional windows within their apps.
 */
public class MicrOSWindow extends JInternalFrame {
    public MicrOSWindow(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
        super(title, resizable, closable, maximizable, iconifiable);
        setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        // Call the decoration hook
        setupWindowDecoration();
        setVisible(true);
    }

    /**
     * Hook for custom window decorations. Override this method to provide your own decorations.
     * By default, does nothing (uses standard JInternalFrame decorations).
     */
    protected void setupWindowDecoration() {
        // Developers can override this to add custom title bars, borders, etc.
    }

    @Override
    public void setTitle(String title) {
        super.setTitle("MicrOS: " + title);
    }
}
