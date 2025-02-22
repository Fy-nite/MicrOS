package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;

public class ErrorDialog {
    public static void showError(Component parent, String message, Throwable error) {
        if (SwingUtilities.isEventDispatchThread()) {
            showErrorDialog(parent, message, error);
        } else {
            SwingUtilities.invokeLater(() -> showErrorDialog(parent, message, error));
        }
    }
    
    private static void showErrorDialog(Component parent, String message, Throwable error) {
        // If no parent component, create a temporary frame
        if (parent == null) {
            JFrame frame = new JFrame("Error");
            frame.setUndecorated(true);
            frame.setLocationRelativeTo(null);
            showErrorInDialog(frame, message, error);
            frame.dispose();
        } else {
            showErrorInDialog(parent, message, error);
        }
        
        // Also print to console for logging
        System.err.println(message);
        if (error != null) {
            error.printStackTrace();
        }
    }
    
    private static void showErrorInDialog(Component parent, String message, Throwable error) {
        String fullMessage = message;
        if (error != null) {
            fullMessage += "\n\nError: " + error.getMessage();
        }
        
        JOptionPane.showMessageDialog(
            parent,
            fullMessage,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
