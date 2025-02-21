package org.finite.micros.maver;

import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.apps.AppManifest;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.ui.WrapLayout; // Add this import

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

public class MaverLauncher extends MicrOSApp {
    private JPanel appGrid;
    private JPanel mainPanel; // Add this field
    private VirtualFileSystem vfs;
    private static final Color BG_COLOR = new Color(245, 245, 245);
    private static final Color CARD_COLOR = new Color(255, 255, 255);
    private static final Color HOVER_COLOR = new Color(240, 240, 240);
    private static final int GRID_HGAP = 20;
    private static final int GRID_VGAP = 20;
    private static final int ICON_SIZE = 64;

    @Override
    public JComponent createUI() {
        mainPanel = new JPanel(new BorderLayout());  // Store reference
        mainPanel.setBackground(BG_COLOR);
        
        // Create header with modern style
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(CARD_COLOR);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));
        
        JLabel headerLabel = new JLabel("Applications", SwingConstants.LEFT);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setForeground(new Color(50, 50, 50));
        headerPanel.add(headerLabel, BorderLayout.WEST);
        
        // Create scrollable app grid with padding
        appGrid = new JPanel(new WrapLayout(FlowLayout.LEFT, GRID_HGAP, GRID_VGAP));
        appGrid.setBackground(BG_COLOR);
        appGrid.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JScrollPane scrollPane = new JScrollPane(appGrid);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BG_COLOR);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Load apps
        loadApps();
        
        return mainPanel;
    }

    private void loadApps() {
        vfs = VirtualFileSystem.getInstance();
        Collection<AppManifest> apps = vfs.getAppLoader().getLoadedApps();
        
        for (AppManifest app : apps) {
            JPanel appPanel = createAppButton(app);
            appGrid.add(appPanel);
        }
    }

    private JPanel createAppButton(AppManifest app) {
        // Create card panel with shadow effect
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setPreferredSize(new Dimension(200, 120));
        card.setBackground(CARD_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(10, new Color(0, 0, 0, 20)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Icon and name panel
        JPanel iconPanel = new JPanel(new BorderLayout(5, 5));
        iconPanel.setOpaque(false);
        
        // Create app icon
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        if (app.getIcon() != null && !app.getIcon().isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(app.getIcon());
                Image scaled = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaled));
            } catch (Exception e) {
                iconLabel.setText("📦");
                iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, ICON_SIZE/2));
            }
        } else {
            iconLabel.setText("📦");
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, ICON_SIZE/2));
        }
        
        // App name with ellipsis if too long
        JLabel nameLabel = new JLabel(app.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(new Color(50, 50, 50));
        
        // Description with ellipsis
        JLabel descLabel = new JLabel(app.getDescription() != null ? 
            "<html><body style='width: 150px'>" + app.getDescription() + "</body></html>" : "");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(new Color(120, 120, 120));
        
        iconPanel.add(iconLabel, BorderLayout.CENTER);
        iconPanel.add(nameLabel, BorderLayout.SOUTH);
        
        card.add(iconPanel, BorderLayout.CENTER);
        card.add(descLabel, BorderLayout.SOUTH);
        
        // Add hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(HOVER_COLOR);
                mainPanel.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Use mainPanel instead of this
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(CARD_COLOR);
                mainPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); // Use mainPanel instead of this
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                launchApp(app);
            }
        });
        
        return card;
    }

    private void launchApp(AppManifest app) {
        try {
            MicrOSApp instance = vfs.getAppLoader().createAppInstance(app.getIdentifier());
            instance.setManifest(app);
            instance.initialize(windowManager, vfs);
            windowManager.launchApp(instance);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel, // Use mainPanel instead of this
                "Failed to launch " + app.getName() + ": " + ex.getMessage(),
                "Launch Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Custom rounded border implementation
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color shadowColor;
        
        public RoundedBorder(int radius, Color shadowColor) {
            this.radius = radius;
            this.shadowColor = shadowColor;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw shadow
            g2.setColor(shadowColor);
            g2.fillRoundRect(x+2, y+2, width-4, height-4, radius, radius);
            
            // Draw border
            g2.setColor(CARD_COLOR);
            g2.fillRoundRect(x, y, width-2, height-2, radius, radius);
            
            g2.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 4, 4, 4);
        }
    }

    @Override
    public void onStart() {
        System.out.println("Maver Launcher started");
    }

    @Override
    public void onStop() {
        System.out.println("Maver Launcher stopped");
    }
}
