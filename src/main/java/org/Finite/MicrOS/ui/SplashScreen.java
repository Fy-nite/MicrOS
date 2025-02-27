package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class SplashScreen extends JPanel {
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JLabel logoLabel;
    private final Color BG_COLOR = new Color(25, 25, 25);
    private final Color TEXT_COLOR = new Color(220, 220, 220);
    private JFrame splashFrame;
    private final JFrame mainWindow;
    private boolean usingTextLogo = false;
    private static final String LOGO_PATH = "/logo.png"; // Path to logo in resources
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    private long showTime = 0;
    private static final int MIN_DISPLAY_TIME = 1500; // Minimum time to show splash in milliseconds
    
    public SplashScreen(JFrame mainWindow) {
        System.out.println("Initializing SplashScreen...");
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(10, 10));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 40, 40), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Logo/title panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        // Create logo with image (or text as fallback)
        logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadLogoImage();
        headerPanel.add(logoLabel, BorderLayout.CENTER);
        
        // Version label
        JLabel versionLabel = new JLabel("v1.0.0", SwingConstants.CENTER);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(180, 180, 180));
        headerPanel.add(versionLabel, BorderLayout.SOUTH);
        
        add(headerPanel, BorderLayout.CENTER);
        
        // Bottom panel for progress
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setOpaque(false);
        
        statusLabel = new JLabel("Starting...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_COLOR);
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(300, 3));
        progressBar.setForeground(new Color(0, 150, 136));
        progressBar.setBackground(new Color(40, 40, 40));
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        setPreferredSize(new Dimension(400, 200));
        createWindow();
    }
    
    private void loadLogoImage() {
        try {
            URL imageUrl = getClass().getResource(LOGO_PATH);
            if (imageUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imageUrl);
                // Scale the image if needed
                if (originalIcon.getIconWidth() > 300) {
                    Image scaledImage = originalIcon.getImage().getScaledInstance(
                            300, -1, Image.SCALE_SMOOTH);
                    logoLabel.setIcon(new ImageIcon(scaledImage));
                } else {
                    logoLabel.setIcon(originalIcon);
                }
                usingTextLogo = false;
            } else {
                setTextLogo("MicrOS");
            }
        } catch (Exception e) {
            setTextLogo("MicrOS");
            System.err.println("Error loading logo image: " + e.getMessage());
        }
    }
    
    private void setTextLogo(String text) {
        logoLabel.setIcon(null);
        logoLabel.setText(text);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        logoLabel.setForeground(TEXT_COLOR);
        usingTextLogo = true;
    }
    
    public void setStatus(String status) {
        if (SwingUtilities.isEventDispatchThread()) {
            statusLabel.setText(status);
        } else {
            SwingUtilities.invokeLater(() -> statusLabel.setText(status));
        }
    }

    public void setShutdownMode() {
        if (usingTextLogo) {
            logoLabel.setText("Shutting Down");
            logoLabel.setForeground(new Color(255, 100, 100)); // Reddish color
        } else {
            // If using image, optionally switch to text for shutdown
            setTextLogo("Shutting Down");
            logoLabel.setForeground(new Color(255, 100, 100));
        }
        
        statusLabel.setForeground(new Color(255, 200, 200));
        progressBar.setForeground(new Color(255, 100, 100));
        setStatus("Please wait...");
    }

    public void disposeSplash() {
        // Only process if not already disposed to avoid race conditions
        if (isDisposed.compareAndSet(false, true)) {
            System.out.println("Disposing splash screen...");
            
            // Calculate how long the splash has been shown
            long displayTime = System.currentTimeMillis() - showTime;
            long remainingTime = Math.max(0, MIN_DISPLAY_TIME - displayTime);
            
            // If we haven't shown the splash for the minimum time, delay the disposal
            if (remainingTime > 0) {
                System.out.println("Delaying splash disposal for " + remainingTime + "ms");
                Timer disposalTimer = new Timer((int)remainingTime, e -> actuallyDisposeSplash());
                disposalTimer.setRepeats(false);
                disposalTimer.start();
            } else {
                // We've shown it long enough, dispose immediately
                actuallyDisposeSplash();
            }
        }
    }
    
    private void actuallyDisposeSplash() {
        if (splashFrame != null) {
            SwingUtilities.invokeLater(() -> {
                splashFrame.setVisible(false);
                splashFrame.dispose();
                splashFrame = null;
                System.out.println("Splash screen disposed");
            });
        }
    }

    private void createWindow() {
        System.out.println("Creating splash window...");
        try {
            splashFrame = new JFrame("MicrOS");
            splashFrame.setUndecorated(true);
            
            // Make sure it always stays on top
            splashFrame.setAlwaysOnTop(true);
            
            // Set window to be displayed in the top layer
            splashFrame.setType(Window.Type.UTILITY); // Usually stays above most windows
            
            splashFrame.add(this);
            splashFrame.pack();
            splashFrame.setLocationRelativeTo(mainWindow);
            System.out.println("Splash window created successfully");
        } catch (Exception e) {
            System.err.println("Error creating splash window: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void show() {
        System.out.println("Attempting to show splash screen...");
        if (splashFrame != null) {
            try {
                // Record when we started showing the splash
                showTime = System.currentTimeMillis();
                
                splashFrame.setVisible(true);
                splashFrame.toFront(); // Bring to front
                
                // Force the splash to be the foremost window
                if (splashFrame.isAlwaysOnTopSupported()) {
                    splashFrame.setAlwaysOnTop(true);
                }
                
                System.out.println("Splash screen should now be visible");
                
                // Make sure splash is rendered by processing pending events
                SwingUtilities.invokeLater(() -> {
                    splashFrame.repaint();
                });
            } catch (Exception e) {
                System.err.println("Error showing splash screen: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Cannot show splash screen: frame is null");
        }
    }
    
    // Add a test method to verify the splash screen works
    public static void testSplash() {
        SwingUtilities.invokeLater(() -> {
            try {
                final SplashScreen splash = new SplashScreen(null);
                splash.show();
                
                // Keep it visible for 5 seconds for testing
                new Timer(5000, e -> splash.disposeSplash()).start();
                
  
            } catch (Exception e) {
                System.err.println("Error in splash test: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
