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

    public interface X11Extended extends X11, X11CompositeManager.X11Extended {
        int XSetIOErrorHandler(XIOErrorHandler handler);
        int XConfigureWindow(Display display, Window w, int valueMask, XWindowAttributes values);
        int XConfigureWindow(Display display, Window w, int valueMask, Structure values); // Accepts XWindowChanges

        // Embed X11 window into a native container
        int XReparentWindow(Display display, Window window, Window parent, int x, int y);
        
        // Get the native window ID from a Java component
        long getComponentWindow(Component component);

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
            
            // Initialize composite manager if extensions are available
            initializeCompositing();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize X11Manager: " + e.getMessage());
            display = null;
            x11 = null;
        }
    }

    private void initializeCompositing() {
        try {
            X11CompositeManager compositeManager = X11CompositeManager.getInstance(display, x11);
            if (compositeManager.isExtensionAvailable("COMPOSITE")) {
                System.out.println("X11 Composite extension available - enabling window capture");
            } else {
                System.out.println("X11 Composite extension not available - using fallback mode");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize compositing: " + e.getMessage());
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
            System.out.println("Setting up X11 integration for Xephyr display");
            
            // For Xephyr testing, don't try to become the window manager
            // Just monitor events without SubstructureRedirectMask
            long eventMask = X11.SubstructureNotifyMask |
                            X11.StructureNotifyMask |
                            X11.PropertyChangeMask;
            
            System.out.println("Using notification-only mode for Xephyr compatibility");
            x11.XSelectInput(display, rootWindow, new NativeLong(eventMask));
            x11.XSync(display, false);
            
            // Don't scan existing windows in Xephyr - wait for new ones
            System.out.println("X11 Xephyr integration setup complete");
        } catch (Exception e) {
            System.err.println("Failed to setup X11 for Xephyr: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void scanExistingWindows() {
        try {
            // Query existing windows and add them to our management
            com.sun.jna.platform.unix.X11.Window[] children = queryWindowTree(rootWindow);
            if (children != null) {
                for (com.sun.jna.platform.unix.X11.Window child : children) {
                    long windowId = child.longValue();
                    if (isValidWindow(windowId) && !windowMap.containsKey(windowId)) {
                        System.out.println("Found existing window: " + windowId);
                        SwingUtilities.invokeLater(() -> createInternalFrame(windowId));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning existing windows: " + e.getMessage());
        }
    }

    private com.sun.jna.platform.unix.X11.Window[] queryWindowTree(com.sun.jna.platform.unix.X11.Window window) {
        try {
            // This is a simplified version - you'd need proper XQueryTree implementation
            // For now, we'll rely on events to catch new windows
            return new com.sun.jna.platform.unix.X11.Window[0];
        } catch (Exception e) {
            return new com.sun.jna.platform.unix.X11.Window[0];
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
        
        System.out.println("Starting X11 event loop");
        XEvent event = new XEvent();
        
        while (isRunning) {
            try {
                int pending = x11.XPending(display);
                if (pending > 0) {
                    System.out.println("Processing " + pending + " pending events");
                    x11.XNextEvent(display, event);
                    
                    System.out.println("Received event type: " + event.type + 
                                     " (CreateNotify=" + X11.CreateNotify + 
                                     ", MapRequest=" + X11.MapRequest + 
                                     ", MapNotify=" + X11.MapNotify + ")");
                    
                    handleEvent(event);
                }
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("Error in X11 event loop: " + e.getMessage());
                e.printStackTrace();
                // Don't break on single errors - continue processing
            }
        }
        System.out.println("X11 event loop ended");
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
                System.out.println("CreateNotify received for window: " + windowId);
                
                if (windowId > 0) {
                    // Add more debugging info
                    System.out.println("Checking validity of window: " + windowId);
                    
                    // Wait a bit for the window to be fully created before validation
                    Timer timer = new Timer(200, e -> {
                        if (isValidWindow(windowId)) {
                            System.out.println("Creating frame for valid window: " + windowId);
                            createInternalFrame(windowId);
                        } else {
                            System.out.println("Skipping invalid/system window: " + windowId);
                            
                            // Add more detailed debugging for why window is invalid
                            try {
                                com.sun.jna.platform.unix.X11.Window window = new com.sun.jna.platform.unix.X11.Window(windowId);
                                XWindowAttributes attrs = new XWindowAttributes();
                                int result = x11.XGetWindowAttributes(display, window, attrs);
                                System.out.println("Window " + windowId + " attributes result: " + result + 
                                                 ", size: " + attrs.width + "x" + attrs.height + 
                                                 ", class: " + attrs.c_class +
                                                 ", root: " + (windowId == rootWindow.longValue()));
                            } catch (Exception ex) {
                                System.err.println("Error getting debug info for window " + windowId + ": " + ex.getMessage());
                            }
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                } else {
                    System.out.println("CreateNotify with invalid window ID: " + windowId);
                }
            } catch (Exception e) {
                System.err.println("Error handling CreateNotify: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleMapRequest(XEvent event) {
        if (x11 == null || display == null) return;
        
        try {
            long windowId = getWindowFromEvent(event);
            System.out.println("MapRequest received for window: " + windowId);
            
            if (windowId <= 0) {
                System.out.println("MapRequest with invalid window ID: " + windowId);
                return;
            }
            
            // Validate window before processing
            if (!isValidWindow(windowId)) {
                System.out.println("MapRequest for invalid/system window: " + windowId);
                // Still allow the map operation for system compatibility
                try {
                    x11.XMapWindow(display, new com.sun.jna.platform.unix.X11.Window(windowId));
                    x11.XSync(display, false);
                } catch (Exception e) {
                    System.err.println("Error mapping invalid window: " + e.getMessage());
                }
                return;
            }
            
            // Allow the window to be mapped
            x11.XMapWindow(display, new com.sun.jna.platform.unix.X11.Window(windowId));
            x11.XSync(display, false);
            
            // Create internal frame if it doesn't exist
            SwingUtilities.invokeLater(() -> {
                if (!windowMap.containsKey(windowId)) {
                    System.out.println("Creating frame for mapped window: " + windowId);
                    createInternalFrame(windowId);
                }
                
                JInternalFrame frame = windowMap.get(windowId);
                if (frame != null) {
                    frame.setVisible(true);
                    try {
                        frame.setSelected(true);
                        frame.moveToFront();
                    } catch (Exception e) {
                        // Ignore property veto exceptions
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error handling MapRequest: " + e.getMessage());
        }
    }

    private void handleMapNotify(XEvent event) {
        long windowId = getWindowFromEvent(event);
        System.out.println("MapNotify received for window: " + windowId);
        
        // Only process valid windows
        if (windowId == 0 || !isValidWindow(windowId)) {
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            // Create frame if it doesn't exist
            if (!windowMap.containsKey(windowId)) {
                createInternalFrame(windowId);
            }
            
            JInternalFrame frame = windowMap.get(windowId);
            if (frame != null && !frame.isVisible()) {
                frame.setVisible(true);
                frame.moveToFront();
            }
        });
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

    private void handleUnmapNotify(XEvent event) {
        long windowId = getWindowFromEvent(event);
        
        // Only process valid windows
        if (windowId == 0) {
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            JInternalFrame frame = windowMap.get(windowId);
            if (frame != null && frame.isVisible()) {
                frame.setVisible(false);
            }
        });
    }

    private void handleConfigureNotify(XEvent event) {
        long windowId = getWindowFromEvent(event);
        
        // Only process valid windows
        if (windowId == 0 || !isValidWindow(windowId)) {
            return;
        }
        
        Rectangle bounds = getWindowBounds(windowId);
        
        SwingUtilities.invokeLater(() -> {
            JInternalFrame frame = windowMap.get(windowId);
            if (frame != null && bounds.width > 1 && bounds.height > 1) {
                frame.setBounds(bounds);
            }
        });
    }

    private long getWindowFromEvent(XEvent event) {
        try {
            if (event == null) {
                System.err.println("Event is null");
                return 0;
            }
            
            System.out.println("Processing event type: " + event.type);
            
            // Try the generic xany structure first (most reliable)
            if (event.xany != null && event.xany.window != null) {
                long windowId = event.xany.window.longValue();
                System.out.println("Got window ID from xany: " + windowId);
                return windowId;
            }
            
            // If xany doesn't work, try type-specific structures
            switch (event.type) {
                case X11.CreateNotify:
                    if (event.xcreatewindow != null && event.xcreatewindow.window != null) {
                        long windowId = event.xcreatewindow.window.longValue();
                        System.out.println("Got window ID from xcreatewindow: " + windowId);
                        return windowId;
                    }
                    break;
                    
                case X11.DestroyNotify:
                    if (event.xdestroywindow != null && event.xdestroywindow.window != null) {
                        long windowId = event.xdestroywindow.window.longValue();
                        System.out.println("Got window ID from xdestroywindow: " + windowId);
                        return windowId;
                    }
                    break;
                    
                case X11.MapNotify:
                    if (event.xmap != null && event.xmap.window != null) {
                        long windowId = event.xmap.window.longValue();
                        System.out.println("Got window ID from xmap: " + windowId);
                        return windowId;
                    }
                    break;
                    
                case X11.UnmapNotify:
                    if (event.xunmap != null && event.xunmap.window != null) {
                        long windowId = event.xunmap.window.longValue();
                        System.out.println("Got window ID from xunmap: " + windowId);
                        return windowId;
                    }
                    break;
                    
                case X11.ConfigureNotify:
                    if (event.xconfigure != null && event.xconfigure.window != null) {
                        long windowId = event.xconfigure.window.longValue();
                        System.out.println("Got window ID from xconfigure: " + windowId);
                        return windowId;
                    }
                    break;
                    
                case X11.MapRequest:
                    if (event.xmaprequest != null && event.xmaprequest.window != null) {
                        long windowId = event.xmaprequest.window.longValue();
                        System.out.println("Got window ID from xmaprequest: " + windowId);
                        return windowId;
                    }
                    break;
                    
                case X11.ConfigureRequest:
                    if (event.xconfigurerequest != null && event.xconfigurerequest.window != null) {
                        long windowId = event.xconfigurerequest.window.longValue();
                        System.out.println("Got window ID from xconfigurerequest: " + windowId);
                        return windowId;
                    }
                    break;
            }
            
            System.err.println("Could not extract window ID from event type " + event.type);
            System.err.println("Event structures - xany: " + event.xany + 
                             ", xcreatewindow: " + event.xcreatewindow +
                             ", xmaprequest: " + event.xmaprequest);
            
        } catch (Exception e) {
            System.err.println("Exception extracting window from event type " + event.type + ": " + e.getMessage());
            e.printStackTrace();
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

    // Add better window validation for Xephyr
    private boolean isValidWindow(long windowId) {
        if (x11 == null || display == null || windowId == 0) {
            return false;
        }
        
        try {
            com.sun.jna.platform.unix.X11.Window window = new com.sun.jna.platform.unix.X11.Window(windowId);
            XWindowAttributes attrs = new XWindowAttributes();
            int result = x11.XGetWindowAttributes(display, window, attrs);
            
            if (result != 0 && attrs.width > 1 && attrs.height > 1) {
                // Filter out root window
                if (windowId == rootWindow.longValue()) {
                    return false;
                }
                
                // Filter out InputOnly windows
                if (attrs.c_class == X11.InputOnly) {
                    return false;
                }
                
                // In Xephyr, be more permissive with window sizes
                if (attrs.width < 20 || attrs.height < 20) {
                    return false;
                }
                
                // Additional check for Xephyr windows - look for WM_CLASS
                try {
                    // Simple check - if window has reasonable size and is InputOutput, accept it
                    return attrs.c_class == X11.InputOutput;
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error validating window " + windowId + ": " + e.getMessage());
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

    // Handle ConfigureRequest events from X11
    private void handleConfigureRequest(XEvent event) {
        if (x11 == null || display == null || event == null) return;

        try {
            // Extract window and requested configuration
            long windowId = getWindowFromEvent(event);
            if (windowId == 0) return;

            // XConfigureRequestEvent is a union field in XEvent
            X11.XConfigureRequestEvent req = event.xconfigurerequest;
            if (req == null) return;

            // Prepare values for XConfigureWindow
            XWindowChanges changes = new XWindowChanges();
            int valueMask = 0;


            long valueMaskLong = req.value_mask.longValue();

            if ((valueMaskLong & X11.CWY) != 0) {
                changes.y = req.y;
                valueMask |= X11.CWY;
            }
            if ((valueMaskLong & X11.CWWidth) != 0) {
                changes.width = req.width;
                valueMask |= X11.CWWidth;
            }
            if ((valueMaskLong & X11.CWHeight) != 0) {
                changes.height = req.height;
                valueMask |= X11.CWHeight;
            }
            if ((valueMaskLong & X11.CWSibling) != 0) {
                changes.sibling = req.above;
                valueMask |= X11.CWSibling;
            }
            if ((valueMaskLong & X11.CWStackMode) != 0) {
                changes.stack_mode = req.detail;
                valueMask |= X11.CWStackMode;
            }

            x11.XConfigureWindow(display, new com.sun.jna.platform.unix.X11.Window(windowId), valueMask, changes);
            x11.XSync(display, false);
        } catch (Exception e) {
            System.err.println("Error handling ConfigureRequest: " + e.getMessage());
        }
    }

    // Structure for XWindowChanges used in XConfigureWindow
    public static class XWindowChanges extends Structure {
        public int x;
        public int y;
        public int width;
        public int height;
        public com.sun.jna.platform.unix.X11.Window sibling;
        public int stack_mode;

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("x", "y", "width", "height", "sibling", "stack_mode");
        }
    }

    // Creates and adds a JInternalFrame for the given X11 window ID
    private void createInternalFrame(long windowId) {
        if (windowMap.containsKey(windowId) || desktop == null) {
            System.out.println("Frame already exists or desktop null for window: " + windowId);
            return;
        }
        
        if (!isValidWindow(windowId)) {
            System.out.println("Cannot create frame for invalid window: " + windowId);
            return;
        }
        
        try {
            String title = getWindowTitle(windowId);
            Rectangle bounds = getWindowBounds(windowId);
            
            System.out.println("Creating internal frame for window " + windowId + " with bounds: " + bounds);

            JInternalFrame frame = new JInternalFrame(title, true, true, true, true);
            frame.setBounds(bounds);
            frame.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);

            // Create container for X11 window content
            X11WindowContainer container = new X11WindowContainer(windowId, display, x11);
            frame.setContentPane(container);

            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateWindowContent(windowId);
                }
            });

            // Add internal frame listener to handle closing
            frame.addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
                @Override
                public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
                    System.out.println("Internal frame closing for window: " + windowId);
                    // Remove from our tracking but don't destroy the X11 window
                    windowMap.remove(windowId);
                }
            });

            desktop.add(frame);
            windowMap.put(windowId, frame);

            frame.setVisible(true);
            try {
                frame.setSelected(true);
                frame.moveToFront();
                System.out.println("Successfully created and displayed frame for window: " + windowId);
            } catch (Exception e) {
                System.err.println("Error selecting frame: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error creating internal frame for window " + windowId + ": " + e.getMessage());
        }
    }
}
 