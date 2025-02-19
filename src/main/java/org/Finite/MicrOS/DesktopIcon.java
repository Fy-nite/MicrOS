package org.Finite.MicrOS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DesktopIcon extends JLabel {

    private final String windowId;
    private final WindowManager windowManager;

    public DesktopIcon(String windowId, String title, Icon icon, WindowManager windowManager) {
        super(title, icon, JLabel.CENTER);
        this.windowId = windowId;
        this.windowManager = windowManager;
        setVerticalTextPosition(JLabel.BOTTOM);
        setHorizontalTextPosition(JLabel.CENTER);
        setPreferredSize(new Dimension(80, 80));
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    JInternalFrame existingWindow = windowManager.getWindow(windowId);
                    if (existingWindow != null) {
                        existingWindow.toFront();
                        try {
                            existingWindow.setSelected(true);
                        } catch (java.beans.PropertyVetoException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        windowManager.createWindow(windowId, title, "default");
                    }
                }
            }
        });
    }
}
