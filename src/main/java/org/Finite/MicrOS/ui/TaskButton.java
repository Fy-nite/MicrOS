package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.apps.AppManifest;

public class TaskButton extends JToggleButton {
    private final JInternalFrame frame;
    private final Color HOVER_COLOR = new Color(70, 70, 75);
    private final Color SELECTED_COLOR = new Color(80, 80, 85);
    private final Color PINNED_BG = new Color(40, 40, 45);
    private final Color DEFAULT_BG = new Color(50, 50, 55);
    private final Color TEXT_COLOR = new Color(220, 220, 220);
    private boolean isHovered = false;
    private static final int ICON_SIZE = 32; // Increased icon size for better visibility

    public TaskButton(JInternalFrame frame) {
        this.frame = frame;
        
        // Get app info if available
        MicrOSApp app = (MicrOSApp) frame.getClientProperty("app");
        if (app != null && app.getManifest() != null) {
            AppManifest manifest = app.getManifest();
            
            // Set icon if available
            if (manifest.getIcon() != null && !manifest.getIcon().isEmpty()) {
                try {
                    ImageIcon icon = new ImageIcon(manifest.getIcon());
                    Image scaled = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
                    setIcon(new ImageIcon(scaled));
                } catch (Exception e) {
                    setText(manifest.getName()); // Fallback to text
                }
            } else {
                setText(manifest.getName()); // Fallback to text
            }
        } else {
            setText(frame.getTitle());
        }
        
        // Setup button appearance
        setFocusPainted(false);
        setBorderPainted(false);
        setPreferredSize(new Dimension(ICON_SIZE + 10, ICON_SIZE + 10));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setOpaque(false);
        
        setupListeners();
    }

    private void setupListeners() {
        addActionListener(e -> handleClick());
        
        // Add hover effect with smooth transition
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                startAnimation(true);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                startAnimation(false);
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
                repaint();
            }
            
            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent e) {
                setSelected(false);
                repaint();
            }
            
            public void internalFrameIconified(javax.swing.event.InternalFrameEvent e) {
                setSelected(false);
                repaint();
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

    private void startAnimation(boolean mouseEntered) {
        Color startColor = getBackground();
        Color targetColor = mouseEntered ? HOVER_COLOR : 
                          (frame.getClientProperty("pinned") != null ? PINNED_BG : DEFAULT_BG);
        
        Timer timer = new Timer(20, null);
        float[] progress = {0f};
        
        timer.addActionListener(e -> {
            progress[0] += 0.2f;
            if (progress[0] >= 1f) {
                setBackground(targetColor);
                timer.stop();
            } else {
                Color currentColor = interpolateColor(startColor, targetColor, progress[0]);
                setBackground(currentColor);
            }
            repaint();
        });
        
        timer.start();
    }

    private Color interpolateColor(Color c1, Color c2, float ratio) {
        int red = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int green = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int blue = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(red, green, blue);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (isSelected()) {
            g.setColor(new Color(80, 80, 85));
        } else {
            g.setColor(new Color(50, 50, 55));
        }
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
    }
}
