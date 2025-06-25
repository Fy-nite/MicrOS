package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MicrOSFileSelector {

    public static void showMicrOSFileChooser(JDesktopPane desktop, String title, boolean selectDirectories, FileSelectionCallback callback) {
        JInternalFrame fileChooserFrame = new JInternalFrame(title, true, true, false, false);
        fileChooserFrame.setLayout(new BorderLayout());

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(selectDirectories ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);

        // Customize JFileChooser for MicrOS
        fileChooser.setDialogTitle(title);
        fileChooser.setApproveButtonText("Select");
        fileChooser.setBackground(new Color(45, 45, 50));
        fileChooser.setForeground(new Color(220, 220, 220));

        fileChooser.addActionListener(e -> {
            if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                File selectedFile = fileChooser.getSelectedFile();
                callback.onFileSelected(selectedFile);
                fileChooserFrame.dispose();
            } else if (JFileChooser.CANCEL_SELECTION.equals(e.getActionCommand())) {
                fileChooserFrame.dispose();
            }
        });

        fileChooserFrame.add(fileChooser, BorderLayout.CENTER);
        fileChooserFrame.pack();
        fileChooserFrame.setLocation(
            (desktop.getWidth() - fileChooserFrame.getWidth()) / 2,
            (desktop.getHeight() - fileChooserFrame.getHeight()) / 2
        );

        desktop.add(fileChooserFrame);
        fileChooserFrame.setVisible(true);

        try {
            fileChooserFrame.setSelected(true);
        } catch (java.beans.PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }

    public interface FileSelectionCallback {
        void onFileSelected(File file);
    }
}
