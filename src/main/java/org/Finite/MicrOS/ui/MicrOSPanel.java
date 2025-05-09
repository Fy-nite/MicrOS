package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;

public class MicrOSPanel {

    public static void showInternalDialog(JDesktopPane desktop, String title, String message, int messageType) {
        JInternalFrame dialogFrame = new JInternalFrame(title, true, true, false, false);
        dialogFrame.setLayout(new BorderLayout());
        dialogFrame.add(new JLabel(message, SwingConstants.CENTER), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialogFrame.dispose());
        buttonPanel.add(okButton);

        dialogFrame.add(buttonPanel, BorderLayout.SOUTH);
        dialogFrame.pack();
        dialogFrame.setLocation(
            (desktop.getWidth() - dialogFrame.getWidth()) / 2,
            (desktop.getHeight() - dialogFrame.getHeight()) / 2
        );

        desktop.add(dialogFrame);
        dialogFrame.setVisible(true);

        try {
            dialogFrame.setSelected(true);
        } catch (java.beans.PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }
}
