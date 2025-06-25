package org.Finite.MicrOS.x11;

import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.Display;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class X11WindowContainer extends JPanel {
    private final long windowId;
    private final Display display;
    private final X11 x11;
    private JPanel contentPanel;
    private BufferedImage windowImage;
    private boolean needsRedraw = true;
    private final Object imageLock = new Object();
    private Timer updateTimer;
    private boolean useOpenGL = false;

    public X11WindowContainer(long windowId, Display display, X11 x11) {
        this.windowId = windowId;
        this.display = display;
        this.x11 = x11;
        
        initializeDisplay();
        setupLayout();
        setupEventHandling();
        startUpdateTimer();
    }

    private void initializeDisplay() {
        // Skip OpenGL entirely - use simple Swing rendering
        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintWindowContent(g);
            }
        };
        
        contentPanel.setBackground(Color.DARK_GRAY);
        contentPanel.setOpaque(true);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
        
        // Add resize listener to update X11 window
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension size = getSize();
                resizeX11Window(size.width, size.height);
                needsRedraw = true;
                repaint();
            }
        });
    }

    private void setupEventHandling() {
        // Forward mouse events to X11 window
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                forwardMouseEvent(e, X11.ButtonPress);
                contentPanel.requestFocusInWindow();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                forwardMouseEvent(e, X11.ButtonRelease);
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                forwardMotionEvent(e);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                forwardMotionEvent(e);
            }
        };
        
        contentPanel.addMouseListener(mouseHandler);
        contentPanel.addMouseMotionListener(mouseHandler);
        
        // Forward keyboard events
        KeyAdapter keyHandler = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                forwardKeyEvent(e, X11.KeyPress);
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                forwardKeyEvent(e, X11.KeyRelease);
            }
        };
        
        contentPanel.addKeyListener(keyHandler);
        contentPanel.setFocusable(true);
    }

    private void startUpdateTimer() {
        // Update window content periodically
        updateTimer = new Timer(100, e -> updateWindowImage());
        updateTimer.start();
    }

    private void paintWindowContent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        try {
            // Enable antialiasing for better text rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            synchronized (imageLock) {
                if (windowImage != null) {
                    // Draw the captured window image
                    g2d.drawImage(windowImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    // Draw placeholder content
                    drawPlaceholder(g2d);
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private void drawPlaceholder(Graphics2D g2d) {
        int width = getWidth();
        int height = getHeight();
        
        // Draw background
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, width, height);
        
        // Draw border
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawRect(0, 0, width - 1, height - 1);
        
        // Draw window info
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        
        String title = "X11 Window: " + windowId;
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(title);
        int textHeight = fm.getHeight();
        
        g2d.drawString(title, (width - textWidth) / 2, (height - textHeight) / 2 + fm.getAscent());
        
        // Draw status
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String status = "Embedded X11 Application";
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(status);
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString(status, (width - textWidth) / 2, (height - textHeight) / 2 + fm.getAscent() + 20);
    }

    private void resizeX11Window(int width, int height) {
        if (x11 == null || display == null) return;
        
        try {
            // Safely resize X11 window
            if (width > 0 && height > 0) {
                // Commented out to prevent crashes
                // x11.XResizeWindow(display, new X11.Window(windowId), width, height);
                x11.XSync(display, false);
            }
        } catch (Exception e) {
            System.err.println("Error resizing X11 window: " + e.getMessage());
        }
    }

    private void forwardMouseEvent(MouseEvent e, int type) {
        try {
            Point point = e.getPoint();
            SwingUtilities.convertPointToScreen(point, contentPanel);
            
            int button = convertSwingButtonToX11(e.getButton());
            sendMouseEventToX11(type, point.x, point.y, button);
        } catch (Exception ex) {
            System.err.println("Error forwarding mouse event: " + ex.getMessage());
        }
    }

    private void forwardMotionEvent(MouseEvent e) {
        try {
            Point point = e.getPoint();
            SwingUtilities.convertPointToScreen(point, contentPanel);
            sendMotionEventToX11(point.x, point.y);
        } catch (Exception ex) {
            System.err.println("Error forwarding motion event: " + ex.getMessage());
        }
    }

    private void forwardKeyEvent(KeyEvent e, int type) {
        try {
            int keyCode = convertSwingKeyToX11(e.getKeyCode());
            sendKeyEventToX11(type, keyCode);
        } catch (Exception ex) {
            System.err.println("Error forwarding key event: " + ex.getMessage());
        }
    }

    private int convertSwingButtonToX11(int swingButton) {
        switch (swingButton) {
            case MouseEvent.BUTTON1: return 1; // Left
            case MouseEvent.BUTTON2: return 2; // Middle
            case MouseEvent.BUTTON3: return 3; // Right
            default: return 1;
        }
    }

    private int convertSwingKeyToX11(int swingKey) {
        // Simple key mapping - would need expansion for full support
        return swingKey;
    }

    private void sendMouseEventToX11(int type, int x, int y, int button) {
        // Placeholder for X11 event sending
        // Implementation would create proper XButtonEvent and send it
    }

    private void sendMotionEventToX11(int x, int y) {
        // Placeholder for X11 motion event sending
    }

    private void sendKeyEventToX11(int type, int keyCode) {
        // Placeholder for X11 key event sending
    }

    public void updateWindowImage() {
        if (!needsRedraw) return;
        
        SwingUtilities.invokeLater(() -> {
            try {
                BufferedImage newImage = captureX11Window();
                synchronized (imageLock) {
                    windowImage = newImage;
                    needsRedraw = false;
                }
                contentPanel.repaint();
            } catch (Exception e) {
                System.err.println("Error updating window image: " + e.getMessage());
            }
        });
    }

    public void cleanup() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
        
        // Additional cleanup if needed
        // For example, releasing X11 resources
    }

    private BufferedImage captureX11Window() {
        // Create a simple placeholder image
        int width = Math.max(getWidth(), 320);
        int height = Math.max(getHeight(), 240);
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw gradient background
            GradientPaint gradient = new GradientPaint(0, 0, new Color(64, 64, 64), 
                                                      width, height, new Color(32, 32, 32));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, height);
            
            // Draw window content placeholder
            g2d.setColor(new Color(128, 128, 128, 100));
            g2d.fillRoundRect(10, 10, width - 20, height - 20, 10, 10);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            
            String text = "X11 Window " + windowId;
            FontMetrics fm = g2d.getFontMetrics();
            int textX = (width - fm.stringWidth(text)) / 2;
            int textY = height / 2;
            
            g2d.drawString(text, textX, textY);
            
        } finally {
            g2d.dispose();
        }
        
        return image;
    }

    
    public void setNeedsRedraw(boolean needsRedraw) {
        this.needsRedraw = needsRedraw;
        if (needsRedraw) {
            repaint();
        }
    }
}
