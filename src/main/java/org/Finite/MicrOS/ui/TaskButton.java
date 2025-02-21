package org.Finite.MicrOS.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.apps.AppManifest;

public class TaskButton extends JToggleButton {
    private final JInternalFrame frame;
    private final Color HOVER_COLOR = new Color(70, 70, 70);
    private final Color SELECTED_COLOR = new Color(80, 80, 80);
    private boolean isHovered = false;
    private static final int ICON_SIZE = 24;

    public TaskButton(JInternalFrame frame) {
        this.frame = frame;
        
        // Get app info if available
        MicrOSApp app = (MicrOSApp) frame.getClientProperty("app");
        if (app != null && app.getManifest() != null) {
            AppManifest manifest = app.getManifest();
            setText(manifest.getName());
            
            // Set icon if available
            if (manifest.getIcon() != null && !manifest.getIcon().isEmpty()) {
                try {
                    ImageIcon icon = new ImageIcon(manifest.getIcon());
                    Image scaled = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
                    setIcon(new ImageIcon(scaled));
                } catch (Exception e) {
                    // Fallback to default icon
                    setText("📱 " + getText());
                }
            } else {
                setText("📱 " + getText());
            }
        } else {
            setText(frame.getTitle());
        }

        setFocusPainted(false);
        setBorderPainted(false);
        setBackground(new Color(45, 45, 45));
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.PLAIN, 12));
        setPreferredSize(new Dimension(150, 32));
        setBorder(new EmptyBorder(4, 8, 4, 8));
        setHorizontalAlignment(SwingConstants.LEFT);
        
        setupListeners();
    }

    private void setupListeners() {
        addActionListener(e -> handleClick());
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
        
        // Only add frame listener if this is not a pinned app
        if (frame.getClientProperty("pinned") == null) {
            setupFrameListener();
        }
    }

    private void setupFrameListener() {
        frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            public void internalFrameActivated(javax.swing.event.InternalFrameEvent e) {
                setSelected(true);
            }
            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent e) {
                setSelected(false);
            }
            public void internalFrameIconified(javax.swing.event.InternalFrameEvent e) {
                setSelected(false);
            }
        });
    }

    protected void handleClick() {
        try {
            // Check if this is a pinned app
            if (frame.getClientProperty("pinned") != null) {
                return; // Let subclass handle click for pinned apps
            }
            
            if (frame.isIcon()) {
                frame.setIcon(false);
                frame.setSelected(true);
            } else if (frame.isVisible()) {
                if (frame.isSelected()) {
                    frame.setIcon(true);
                } else {
                    frame.setSelected(true);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        if (isSelected()) {
            g2.setColor(SELECTED_COLOR);
        } else if (isHovered) {
            g2.setColor(HOVER_COLOR);
        } else {
            // Use slightly different color for pinned apps
            g2.setColor(frame.getClientProperty("pinned") != null ? 
                new Color(40, 40, 40) : 
                new Color(50, 50, 50));
        }
        
        g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 6, 6);

        // Draw icon and text
        if (getIcon() != null) {
            getIcon().paintIcon(this, g2, 8, (getHeight() - ICON_SIZE) / 2);
            FontMetrics fm = g2.getFontMetrics();
            int textX = ICON_SIZE + 12;
            int textY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), textX, textY);
        } else {
            FontMetrics fm = g2.getFontMetrics();
            int x = 8;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), x, y);
        }

        g2.dispose();
    }
}
