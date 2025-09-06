package org.Finite.MicrOS.x11;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class X11CompositeManager {
    private static X11CompositeManager instance;
    private final X11Extended x11;
    private final Display display;
    private final com.sun.jna.platform.unix.X11.Window rootWindow;
    private final ConcurrentHashMap<Long, WindowCapture> capturedWindows = new ConcurrentHashMap<>();
    private final ScheduledExecutorService captureScheduler = Executors.newScheduledThreadPool(4);
    
    public interface X11Extended extends X11 {
        // XComposite extension
        int XCompositeRedirectWindow(Display display, com.sun.jna.platform.unix.X11.Window window, int update);
        int XCompositeRedirectSubwindows(Display display, com.sun.jna.platform.unix.X11.Window window, int update);
        int XCompositeUnredirectWindow(Display display, com.sun.jna.platform.unix.X11.Window window, int update);
        Pixmap XCompositeNameWindowPixmap(Display display, com.sun.jna.platform.unix.X11.Window window);
        
        // XDamage extension for efficient updates
        long XDamageCreate(Display display, Drawable drawable, int level);
        void XDamageDestroy(Display display, long damage);
        
        // XFixes extension
        void XFixesSetWindowShapeRegion(Display display, com.sun.jna.platform.unix.X11.Window window, int shapeKind, int xOff, int yOff, Pointer region);
        
        // XRender extension for better compositing
        int XRenderQueryExtension(Display display, Pointer eventBase, Pointer errorBase);
        
        // XImage methods
        Pointer XGetImage(Display display, Drawable drawable, int x, int y, int width, int height, long planeMask, int format);
        void XDestroyImage(Pointer ximage);
        
        // Constants
        int CompositeRedirectAutomatic = 0;
        int CompositeRedirectManual = 1;
        int DamageReportRawRectangles = 1;
    }
    
    private static class WindowCapture {
        private final long windowId;
        private final Display display;
        private final X11Extended x11;
        private Pixmap backingPixmap;
        private BufferedImage currentImage;
        private boolean needsUpdate = true;
        private long damageHandle;
        
        public WindowCapture(long windowId, Display display, X11Extended x11) {
            this.windowId = windowId;
            this.display = display;
            this.x11 = x11;
            setupCapture();
        }
        
        private void setupCapture() {
            try {
                com.sun.jna.platform.unix.X11.Window window = new com.sun.jna.platform.unix.X11.Window(windowId);
                
                // Redirect window to offscreen buffer
                x11.XCompositeRedirectWindow(display, window, X11Extended.CompositeRedirectManual);
                
                // Create damage tracking
                damageHandle = x11.XDamageCreate(display, window, X11Extended.DamageReportRawRectangles);
                
                // Get the backing pixmap
                updateBackingPixmap();
                
            } catch (Exception e) {
                System.err.println("Failed to setup window capture for " + windowId + ": " + e.getMessage());
            }
        }
        
        private void updateBackingPixmap() {
            try {
                com.sun.jna.platform.unix.X11.Window window = new com.sun.jna.platform.unix.X11.Window(windowId);
                backingPixmap = x11.XCompositeNameWindowPixmap(display, window);
            } catch (Exception e) {
                System.err.println("Failed to get backing pixmap for " + windowId + ": " + e.getMessage());
            }
        }
        
        public BufferedImage captureWindow() {
            if (!needsUpdate && currentImage != null) {
                return currentImage;
            }
            
            try {
                com.sun.jna.platform.unix.X11.Window window = new com.sun.jna.platform.unix.X11.Window(windowId);
                XWindowAttributes attrs = new XWindowAttributes();
                
                int result = x11.XGetWindowAttributes(display, window, attrs);
                if (result == 0 || attrs.width <= 0 || attrs.height <= 0) {
                    return null;
                }
                
                // Update backing pixmap if needed
                if (backingPixmap == null) {
                    updateBackingPixmap();
                }
                
                if (backingPixmap != null) {
                    currentImage = pixmapToBufferedImage(backingPixmap, attrs.width, attrs.height);
                    needsUpdate = false;
                }
                
            } catch (Exception e) {
                System.err.println("Error capturing window " + windowId + ": " + e.getMessage());
            }
            
            return currentImage;
        }
        
        private BufferedImage pixmapToBufferedImage(Pixmap pixmap, int width, int height) {
            try {
                // Create XImage from pixmap
                Pointer ximage = x11.XGetImage(display, pixmap, 0, 0, width, height, 0xFFFFFFFF, 2); // ZPixmap
                
                if (ximage == null) {
                    return createFallbackImage(width, height);
                }
                
                // Extract pixel data (this is a simplified version - real implementation would need proper XImage handling)
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = image.createGraphics();
                
                // For now, create a placeholder that shows the window is being captured
                g2d.setColor(new Color(64, 128, 64, 200));
                g2d.fillRect(0, 0, width, height);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 16));
                String text = "Captured Window " + windowId;
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (width - fm.stringWidth(text)) / 2;
                int textY = height / 2;
                g2d.drawString(text, textX, textY);
                g2d.dispose();
                
                // Cleanup XImage
                x11.XDestroyImage(ximage);
                
                return image;
                
            } catch (Exception e) {
                System.err.println("Error converting pixmap to image: " + e.getMessage());
                return createFallbackImage(width, height);
            }
        }
        
        private BufferedImage createFallbackImage(int width, int height) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Window " + windowId, 10, 30);
            g2d.dispose();
            return image;
        }
        
        public void markForUpdate() {
            needsUpdate = true;
        }
        
        public void cleanup() {
            try {
                if (damageHandle != 0) {
                    x11.XDamageDestroy(display, damageHandle);
                }
                if (backingPixmap != null) {
                    com.sun.jna.platform.unix.X11.Window window = new com.sun.jna.platform.unix.X11.Window(windowId);
                    x11.XCompositeUnredirectWindow(display, window, X11Extended.CompositeRedirectManual);
                }
            } catch (Exception e) {
                System.err.println("Error cleaning up window capture: " + e.getMessage());
            }
        }
    }
    
    private X11CompositeManager(Display display, X11Extended x11) {
        this.display = display;
        this.x11 = x11;
        this.rootWindow = x11.XDefaultRootWindow(display);
        setupCompositing();
    }
    
    public static synchronized X11CompositeManager getInstance(Display display, X11Extended x11) {
        if (instance == null) {
            instance = new X11CompositeManager(display, x11);
        }
        return instance;
    }
    
    private void setupCompositing() {
        try {
            // Enable compositing for the root window
          //  x11.XCompositeRedirectSubwindows(display, rootWindow, X11Extended.CompositeRedirectManual);
            
            // Start the capture update scheduler
            captureScheduler.scheduleAtFixedRate(this::updateAllCaptures, 0, 33, TimeUnit.MILLISECONDS); // ~30 FPS
            
            System.out.println("X11 compositing manager initialized");
        } catch (Exception e) {
            System.err.println("Failed to setup compositing: " + e.getMessage());
        }
    }
    
    public void captureWindow(long windowId) {
        if (!capturedWindows.containsKey(windowId)) {
            try {
                WindowCapture capture = new WindowCapture(windowId, display, x11);
                capturedWindows.put(windowId, capture);
                System.out.println("Started capturing window: " + windowId);
            } catch (Exception e) {
                System.err.println("Failed to start capturing window " + windowId + ": " + e.getMessage());
            }
        }
    }
    
    public void stopCapturing(long windowId) {
        WindowCapture capture = capturedWindows.remove(windowId);
        if (capture != null) {
            capture.cleanup();
            System.out.println("Stopped capturing window: " + windowId);
        }
    }
    
    public BufferedImage getWindowImage(long windowId) {
        WindowCapture capture = capturedWindows.get(windowId);
        return capture != null ? capture.captureWindow() : null;
    }
    
    public void markWindowForUpdate(long windowId) {
        WindowCapture capture = capturedWindows.get(windowId);
        if (capture != null) {
            capture.markForUpdate();
        }
    }
    
    private void updateAllCaptures() {
        for (WindowCapture capture : capturedWindows.values()) {
            try {
                capture.captureWindow(); // This will update if needed
            } catch (Exception e) {
                // Ignore individual capture errors to prevent stopping the scheduler
            }
        }
    }
    
    public void shutdown() {
        captureScheduler.shutdown();
        for (WindowCapture capture : capturedWindows.values()) {
            capture.cleanup();
        }
        capturedWindows.clear();
    }
    
    public boolean isExtensionAvailable(String extensionName) {
        // Check if required X11 extensions are available
        try {
            // This would need proper implementation to query X11 extensions
            return true; // Simplified for now
        } catch (Exception e) {
            return false;
        }
    }
}
