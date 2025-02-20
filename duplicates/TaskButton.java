package org.Finite.MicrOS.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class TaskButton extends JToggleButton {
    private final JInternalFrame frame;
    private final Color HOVER_COLOR = new Color(70, 70, 70);
    private final Color SELECTED_COLOR = new Color(80, 80, 80);
    private boolean isHovered = false;

    public TaskButton(JInternalFrame frame) {
        this.frame = frame;
        setText(frame.getTitle());
        setFont(new Font("Segoe UI", Font.PLAIN, 12));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setPreferredSize(new Dimension(150, 32));
        setBorder(new EmptyBorder(4, 8, 4, 8));
        setHorizontalAlignment(SwingConstants.LEFT);
        
        setupListeners();
        setupFrameListener();
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

    private void handleClick() {
        try {
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
            g2.setColor(new Color(50, 50, 50));
        }
        
        g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 6, 6);

        // Draw text
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int x = 8;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), x, y);

        g2.dispose();
    }
}
