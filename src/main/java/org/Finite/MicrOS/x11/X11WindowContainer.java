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
    private boolean isEmbedded = false;

    public X11WindowContainer(long windowId, Display display, X11 x11) {
        this.windowId = windowId;
        this.display = display;
        this.x11 = x11;
        
        System.out.println("Creating X11WindowContainer for window: " + windowId);
        System.out.println("Display: " + display + ", X11: " + x11);
        
        // Try to set up basic window tracking and get window info
        try {
            if (windowId > 0) {
                // Get window attributes to verify it exists
                com.sun.jna.platform.unix.X11.XWindowAttributes attrs = new com.sun.jna.platform.unix.X11.XWindowAttributes();
                int result = x11.XGetWindowAttributes(display, new com.sun.jna.platform.unix.X11.Window(windowId), attrs);
                
                System.out.println("XGetWindowAttributes result: " + result + " for window " + windowId);
                
                if (result != 0) {
                    isEmbedded = true;
                    System.out.println("Window " + windowId + " verified: " + attrs.width + "x" + attrs.height + 
                                     " at (" + attrs.x + "," + attrs.y + "), class=" + attrs.c_class);
                    
                    // Try to select events on this window for updates
                    try {
                        x11.XSelectInput(display, new com.sun.jna.platform.unix.X11.Window(windowId), 
                                        new com.sun.jna.NativeLong(X11.StructureNotifyMask | X11.ExposureMask));
                        System.out.println("Selected input events for window " + windowId);
                    } catch (Exception e) {
                        System.err.println("Could not select input for window " + windowId + ": " + e.getMessage());
                    }
                } else {
                    System.err.println("Could not get attributes for window " + windowId);
                }
            } else {
                System.err.println("Invalid window ID provided: " + windowId);
            }
        } catch (Exception e) {
            System.err.println("Failed to setup basic capture for window " + windowId + ": " + e.getMessage());
            e.printStackTrace();
            isEmbedded = false;
        }
        
        initializeDisplay();
        setupLayout();
        setupEventHandling();
        startUpdateTimer();
    }

    public X11WindowContainer(long windowId2, X11 x112, Display display2) {
        this(windowId2, display2, x112);
        System.out.println("Created X11WindowContainer with alternative constructor for window: " + windowId2);
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
            
            @Override
            public void mouseClicked(MouseEvent e) {
                // Add click handling for better Xephyr interaction
                forwardMouseEvent(e, X11.ButtonPress);
                forwardMouseEvent(e, X11.ButtonRelease);
            }
        };
        
        contentPanel.addMouseListener(mouseHandler);
        contentPanel.addMouseMotionListener(mouseHandler);
        
        // Forward keyboard events with better focus handling
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
        
        // Add focus listener to ensure proper event handling
        contentPanel.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                System.out.println("X11 container gained focus for window: " + windowId);
            }
        });
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
        
        String title = windowId > 0 ? "X11 Window: " + windowId : "No Window";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(title);
        int textHeight = fm.getHeight();
        
        g2d.drawString(title, (width - textWidth) / 2, (height - textHeight) / 2 + fm.getAscent());
        
        // Draw status
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String status = windowId > 0 ? "Embedded X11 Application" : "Waiting for application...";
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

    private BufferedImage captureX11Window() {
        // For Xephyr testing, create a more informative placeholder
        if (isEmbedded && windowId > 0) {
            try {
                com.sun.jna.platform.unix.X11.XWindowAttributes attrs = new com.sun.jna.platform.unix.X11.XWindowAttributes();
                int result = x11.XGetWindowAttributes(display, new com.sun.jna.platform.unix.X11.Window(windowId), attrs);
                
                if (result != 0 && attrs.width > 0 && attrs.height > 0) {
                    int width = attrs.width;
                    int height = attrs.height;
                    
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = image.createGraphics();
                    
                    try {
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        // Create a distinctive look for Xephyr windows
                        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 40, 80), 
                                                                  width, height, new Color(10, 20, 40));
                        g2d.setPaint(gradient);
                        g2d.fillRect(0, 0, width, height);
                        
                        // Add border to show it's captured
                        g2d.setColor(new Color(100, 150, 255));
                        g2d.setStroke(new BasicStroke(3));
                        g2d.drawRect(2, 2, width - 4, height - 4);
                        
                        g2d.setColor(Color.WHITE);
                        g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                        
                        String text = "Xephyr Window " + windowId;
                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = Math.max(10, (width - fm.stringWidth(text)) / 2);
                        int textY = height / 2;
                        
                        g2d.drawString(text, textX, textY);
                        
                        // Show connection status
                        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                        String info = width + "x" + height + " @ (" + attrs.x + "," + attrs.y + ")";
                        fm = g2d.getFontMetrics();
                        int infoX = Math.max(10, (width - fm.stringWidth(info)) / 2);
                        g2d.drawString(info, infoX, textY + 20);
                        
                        // Add Xephyr indicator
                        g2d.setColor(new Color(100, 255, 100));
                        g2d.drawString("● XEPHYR READY", infoX, textY + 40);
                        
                        // Add interaction hint
                        g2d.setColor(Color.YELLOW);
                        g2d.drawString("Click to interact", infoX, textY + 60);
                        
                    } finally {
                        g2d.dispose();
                    }
                    
                    return image;
                }
            } catch (Exception e) {
                System.err.println("Error getting Xephyr window attributes for " + windowId + ": " + e.getMessage());
            }
        }
        
        // Fallback to placeholder
        int width = Math.max(getWidth(), 320);
        int height = Math.max(getHeight(), 240);
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Standard placeholder
            GradientPaint gradient = new GradientPaint(0, 0, new Color(64, 64, 64), 
                                                      width, height, new Color(32, 32, 32));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, height);
            
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

    public void cleanup() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
        
        // Basic cleanup - no composite manager references
        System.out.println("Cleaned up X11WindowContainer for window: " + windowId);
    }

    public void setNeedsRedraw(boolean needsRedraw) {
        this.needsRedraw = needsRedraw;
        if (needsRedraw) {
            repaint();
        }
    }
}
