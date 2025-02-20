package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;

public class SystemTray extends JPanel {
    public SystemTray() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        setPreferredSize(new Dimension(100, 30));
    }

    public void addTrayIcon(Icon icon, String tooltip) {
        JLabel label = new JLabel(icon);
        label.setToolTipText(tooltip);
        add(label);
        revalidate();
    }
}
