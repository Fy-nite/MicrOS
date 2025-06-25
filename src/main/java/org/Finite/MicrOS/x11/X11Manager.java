package org.Finite.MicrOS.x11;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class X11Manager {
    private static X11Manager instance;
    private X11Extended x11;
    private Display display;
    private com.sun.jna.platform.unix.X11.Window rootWindow;
    private final Map<Long, JInternalFrame> windowMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> parentChildMap = new ConcurrentHashMap<>();
    private JDesktopPane desktop;
    private boolean isRunning = false;
    private Thread eventThread;

    public interface X11Extended extends X11 {
        int XSetIOErrorHandler(XIOErrorHandler handler);

        int XConfigureWindow(Display display, Window w, int valueMask, XWindowAttributes values);

        interface XIOErrorHandler extends com.sun.jna.Callback {
            int apply(Pointer display);
        }
        
        @Structure.FieldOrder({"type", "serial", "send_event", "display", "window"})
        class XCreateWindowEvent extends Structure {
            public int type;
            public long serial;
            public int send_event;
            public Pointer display;
            public long window;
            public long parent;
            public int x, y;
            public int width, height;
            public int border_width;
            public int override_redirect;
        }
        
        @Structure.FieldOrder({"type", "serial", "send_event", "display", "window"})
        class XDestroyWindowEvent extends Structure {
            public int type;
            public long serial;
            public int send_event;
            public Pointer display;
            public long window;
        }
        
        @Structure.FieldOrder({"type", "serial", "send_event", "display", "window"})
        class XMapEvent extends Structure {
            public int type;
            public long serial;
            public int send_event;
            public Pointer display;
            public long window;
            public int override_redirect;
        }
        
        @Structure.FieldOrder({"type", "serial", "send_event", "display", "window"})
        class XUnmapEvent extends Structure {
            public int type;
            public long serial;
            public int send_event;
            public Pointer display;
            public long window;
            public int from_configure;
        }
        
        @Structure.FieldOrder({"type", "serial", "send_event", "display", "window"})
        class XConfigureEvent extends Structure {
            public int type;
            public long serial;
            public int send_event;
            public Pointer display;
            public long window;
            public int x, y;
            public int width, height;
            public int border_width;
            public long above;
            public int override_redirect;
        }
    }

    private X11Manager() {
        try {
            x11 = (X11Extended) Native.load("X11", X11Extended.class);
            display = x11.XOpenDisplay(null);
            if (display == null) {
                throw new RuntimeException("Cannot open X11 display - X11 not available or DISPLAY not set");
            }
            rootWindow = x11.XDefaultRootWindow(display);
            if (rootWindow == null) {
                throw new RuntimeException("Cannot get root window");
            }
            setupErrorHandlers();
        } catch (Exception e) {
            System.err.println("Failed to initialize X11Manager: " + e.getMessage());
            // Don't throw - allow graceful degradation
            display = null;
            x11 = null;
        }
    }

    public static synchronized X11Manager getInstance() {
        if (instance == null) {
            instance = new X11Manager();
        }
        return instance;
    }

    public void initialize(JDesktopPane desktop) {
        this.desktop = desktop;
        startEventLoop();
        setupWindowManager();
    }

    private void setupErrorHandlers() {
        if (x11 == null || display == null) return;
        
        try {
            X11Extended x11Ext = (X11Extended) x11;
            
            x11.XSetErrorHandler((display, event) -> {
                if (event != null) {
                    System.err.println("X11 Error: " + event.error_code + " (request: " + event.request_code + ")");
                }
                return 0; // Continue execution
            });
            
            x11Ext.XSetIOErrorHandler(display -> {
                System.err.println("X11 IO Error - connection lost");
                isRunning = false;
                return 0;
            });
        } catch (Exception e) {
            System.err.println("Failed to setup X11 error handlers: " + e.getMessage());
        }
    }

    private void setupWindowManager() {
        if (x11 == null || display == null || rootWindow == null) {
            System.err.println("Cannot setup window manager - X11 not properly initialized");
            return;
        }
        
        try {
            // Use safer event mask values - avoid complex masks that might cause BadValue
            long eventMask = X11.SubstructureNotifyMask | X11.PropertyChangeMask;
            
            // Validate root window before selecting input
            XWindowAttributes rootAttrs = new XWindowAttributes();
            int result = x11.XGetWindowAttributes(display, rootWindow, rootAttrs);
            if (result == 0) {
                System.err.println("Cannot get root window attributes");
                return;
            }
            
            x11.XSelectInput(display, rootWindow, new NativeLong(eventMask));
            x11.XSync(display, false);
            System.out.println("X11 window manager setup complete");
        } catch (Exception e) {
            System.err.println("Failed to setup window manager: " + e.getMessage());
        }
    }

    private void startEventLoop() {
        if (isRunning) return;
        
        isRunning = true;
        eventThread = new Thread(this::eventLoop, "X11-EventLoop");
        eventThread.setDaemon(true);
        eventThread.start();
    }

    private void eventLoop() {
        if (x11 == null || display == null) {
            System.err.println("Cannot start event loop - X11 not available");
            return;
        }
        
        XEvent event = new XEvent();
        
        while (isRunning) {
            try {
                if (x11.XPending(display) > 0) {
                    x11.XNextEvent(display, event);
                    handleEvent(event);
                }
                Thread.sleep(10); // Small delay to prevent busy waiting
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("Error in X11 event loop: " + e.getMessage());
                // Don't break on single errors - continue processing
            }
        }
    }

    private void handleEvent(XEvent event) {
        if (event == null) return;
        
        try {
            switch (event.type) {
                case X11.CreateNotify:
                    handleCreateNotify(event);
                    break;
                case X11.DestroyNotify:
                    handleDestroyNotify(event);
                    break;
                case X11.MapNotify:
                    handleMapNotify(event);
                    break;
                case X11.UnmapNotify:
                    handleUnmapNotify(event);
                    break;
                case X11.ConfigureNotify:
                    handleConfigureNotify(event);
                    break;
                case X11.MapRequest:
                    handleMapRequest(event);
                    break;
                case X11.ConfigureRequest:
                    handleConfigureRequest(event);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error handling X11 event type " + event.type + ": " + e.getMessage());
        }
    }

    private void handleCreateNotify(XEvent event) {
        SwingUtilities.invokeLater(() -> {
            try {
                long windowId = getWindowFromEvent(event);
                createInternalFrame(windowId);
            } catch (Exception e) {
                System.err.println("Error handling CreateNotify: " + e.getMessage());
            }
        });
    }

    private void handleMapRequest(XEvent event) {
        if (x11 == null || display == null) return;
        
        try {
            long windowId = getWindowFromEvent(event);
            if (windowId == 0 || !isValidWindow(windowId)) return;
            
            // Allow the window to be mapped
            x11.XMapWindow(display, new com.sun.jna.platform.unix.X11.Window(windowId));
            x11.XSync(display, false);
            
            SwingUtilities.invokeLater(() -> {
                JInternalFrame frame = windowMap.get(windowId);
                if (frame != null) {
                    frame.setVisible(true);
                    try {
                        frame.setSelected(true);
                    } catch (Exception e) {
                        // Ignore property veto exceptions
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error handling MapRequest: " + e.getMessage());
        }
    }

    private void handleConfigureRequest(XEvent event) {
        if (x11 == null || display == null) return;
        
        try {
            long windowId = getWindowFromEvent(event);
            if (windowId == 0 || !isValidWindow(windowId)) return;
            
            // Get window attributes safely
            Rectangle bounds = getWindowBounds(windowId);
            if (bounds.width <= 0 || bounds.height <= 0) return;
            
            SwingUtilities.invokeLater(() -> {
                JInternalFrame frame = windowMap.get(windowId);
                if (frame != null) {
                    frame.setBounds(bounds);
                }
            });
        } catch (Exception e) {
            System.err.println("Error handling ConfigureRequest: " + e.getMessage());
        }
    }

    private void createInternalFrame(long windowId) {
        if (windowMap.containsKey(windowId) || desktop == null) {
            return; // Already exists or no desktop
        }

        try {
            // Get window properties safely
            String title = getWindowTitle(windowId);
            Rectangle bounds = getWindowBounds(windowId);
            
            // Validate bounds
            if (bounds.width <= 0 || bounds.height <= 0) {
                bounds = new Rectangle(0, 0, 640, 480);
            }
            
            // Create JInternalFrame
            JInternalFrame frame = new JInternalFrame(title, true, true, true, true);
            frame.setBounds(bounds);
            
            // Create X11 window container only if X11 is available
            if (x11 != null && display != null) {
                X11WindowContainer container = new X11WindowContainer(windowId, display, x11);
                frame.setContentPane(container);
            } else {
                // Fallback to empty panel
                frame.setContentPane(new JPanel());
            }
            
            // Track the window
            windowMap.put(windowId, frame);
            
            // Add to desktop
            desktop.add(frame);
            frame.setVisible(true);
            
            // Setup frame listeners for synchronization
            setupFrameListeners(windowId, frame);
            
        } catch (Exception e) {
            System.err.println("Error creating internal frame for window " + windowId + ": " + e.getMessage());
        }
    }

    private void setupFrameListeners(long windowId, JInternalFrame frame) {
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                synchronizeFrameToX11(windowId, frame);
            }
            
            @Override
            public void componentResized(ComponentEvent e) {
                synchronizeFrameToX11(windowId, frame);
            }
        });
        
        frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
                destroyX11Window(windowId);
            }
        });
    }

    private void synchronizeFrameToX11(long windowId, JInternalFrame frame) {
        if (x11 == null || display == null) return;
        
        try {
            Rectangle bounds = frame.getBounds();
            if (bounds.width > 0 && bounds.height > 0) {
                // Commented out the actual X11 call to prevent crashes
                // x11.XMoveResizeWindow(display, new com.sun.jna.platform.unix.X11.Window(windowId), 
                //     bounds.x, bounds.y, bounds.width, bounds.height);
                x11.XSync(display, false);
            }
        } catch (Exception e) {
            System.err.println("Error synchronizing frame to X11: " + e.getMessage());
        }
    }

    private void destroyX11Window(long windowId) {
        if (x11 == null || display == null) return;
        
        try {
            x11.XDestroyWindow(display, new com.sun.jna.platform.unix.X11.Window(windowId));
            x11.XSync(display, false);
        } catch (Exception e) {
            System.err.println("Error destroying X11 window: " + e.getMessage());
        }
    }

    private void handleDestroyNotify(XEvent event) {
        long windowId = getWindowFromEvent(event);
        
        SwingUtilities.invokeLater(() -> {
            JInternalFrame frame = windowMap.remove(windowId);
            if (frame != null) {
                desktop.remove(frame);
                frame.dispose();
                desktop.revalidate();
                desktop.repaint();
            }
        });
    }

    private void handleMapNotify(XEvent event) {
        long windowId = getWindowFromEvent(event);
        
        SwingUtilities.invokeLater(() -> {
            JInternalFrame frame = windowMap.get(windowId);
            if (frame != null && !frame.isVisible()) {
                frame.setVisible(true);
            }
        });
    }

    private void handleUnmapNotify(XEvent event) {
        long windowId = getWindowFromEvent(event);
        
        SwingUtilities.invokeLater(() -> {
            JInternalFrame frame = windowMap.get(windowId);
            if (frame != null && frame.isVisible()) {
                frame.setVisible(false);
            }
        });
    }

    private void handleConfigureNotify(XEvent event) {
        long windowId = getWindowFromEvent(event);
        Rectangle bounds = getWindowBounds(windowId);
        
        SwingUtilities.invokeLater(() -> {
            JInternalFrame frame = windowMap.get(windowId);
            if (frame != null) {
                frame.setBounds(bounds);
            }
        });
    }

    private long getWindowFromEvent(XEvent event) {
        try {
            if (event != null && event.xany != null && event.xany.window != null) {
                return event.xany.window.longValue();
            }
        } catch (Exception e) {
            System.err.println("Error extracting window from event: " + e.getMessage());
        }
        return 0;
    }

    private String getWindowTitle(long windowId) {
        if (!isValidWindow(windowId)) {
            return "Invalid Window";
        }
        
        try {
            // Simplified title retrieval - avoid complex property operations
            return "X11 Window " + windowId;
        } catch (Exception e) {
            return "Unknown Window";
        }
    }

    private Rectangle getWindowBounds(long windowId) {
        if (x11 == null || display == null || windowId == 0 || !isValidWindow(windowId)) {
            return new Rectangle(0, 0, 640, 480);
        }
        
        try {
            XWindowAttributes attrs = new XWindowAttributes();
            int result = x11.XGetWindowAttributes(display, new com.sun.jna.platform.unix.X11.Window(windowId), attrs);
            if (result != 0 && attrs.width > 0 && attrs.height > 0) {
                return new Rectangle(attrs.x, attrs.y, attrs.width, attrs.height);
            }
        } catch (Exception e) {
            System.err.println("Error getting window bounds for " + windowId + ": " + e.getMessage());
        }
        return new Rectangle(0, 0, 640, 480);
    }

    // Add window validation method
    private boolean isValidWindow(long windowId) {
        if (x11 == null || display == null || windowId == 0) {
            return false;
        }
        
        try {
            XWindowAttributes attrs = new XWindowAttributes();
            int result = x11.XGetWindowAttributes(display, new com.sun.jna.platform.unix.X11.Window(windowId), attrs);
            return result != 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void shutdown() {
        isRunning = false;
        if (eventThread != null) {
            eventThread.interrupt();
            try {
                eventThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        
        // Cleanup all windows
        for (JInternalFrame frame : windowMap.values()) {
            try {
                if (frame.getContentPane() instanceof X11WindowContainer) {
                    ((X11WindowContainer) frame.getContentPane()).cleanup();
                }
                frame.dispose();
            } catch (Exception e) {
                System.err.println("Error cleaning up frame: " + e.getMessage());
            }
        }
        windowMap.clear();
        
        if (display != null && x11 != null) {
            try {
                x11.XCloseDisplay(display);
            } catch (Exception e) {
                System.err.println("Error closing X11 display: " + e.getMessage());
            }
            display = null;
        }
    }

    public boolean isX11Available() {
        return display != null && x11 != null;
    }

    public void reparentWindow(long windowId, long newParent) {
        if (x11 == null || display == null) return;
        
        try {
            // Safely reparent window
            // x11.XReparentWindow(display, new com.sun.jna.platform.unix.X11.Window(windowId), 
            //                     new com.sun.jna.platform.unix.X11.Window(newParent), 0, 0);
            x11.XSync(display, false);
        } catch (Exception e) {
            System.err.println("Error reparenting window: " + e.getMessage());
        }
    }

    public void updateWindowContent(long windowId) {
        SwingUtilities.invokeLater(() -> {
            JInternalFrame frame = windowMap.get(windowId);
            if (frame != null && frame.getContentPane() instanceof X11WindowContainer) {
                X11WindowContainer container = (X11WindowContainer) frame.getContentPane();
                container.setNeedsRedraw(true);
            }
        });
    }
}
