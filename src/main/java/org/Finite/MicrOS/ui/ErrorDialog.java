package org.Finite.MicrOS.ui;

import javax.swing.*;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorDialog {

    public static void showError(JDesktopPane desktop, String message, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        JTextArea textArea = new JTextArea(stackTrace);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JInternalFrame errorFrame = new JInternalFrame("Error", true, true, true, true);
        errorFrame.setLayout(new BorderLayout());
        errorFrame.add(new JLabel(message), BorderLayout.NORTH);
        errorFrame.add(scrollPane, BorderLayout.CENTER);
        errorFrame.pack();
        errorFrame.setVisible(true);

        desktop.add(errorFrame);
        try {
            errorFrame.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {
            e.printStackTrace();
        }
    }
}
