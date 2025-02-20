package org.Finite.MicrOS;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.Finite.MicrOS.VirtualFileSystem;

public class BackgroundPanel extends JPanel {
    private BufferedImage backgroundImage;
    private Color backgroundColor;
    private final VirtualFileSystem vfs;
    private String currentPath;
    private boolean isColor;

    public BackgroundPanel(String background) {
        this.vfs = VirtualFileSystem.getInstance();
        setBackground(background);
    }

    public void setBackground(String background) {
        if (background.startsWith("#")) {
            try {
                this.backgroundColor = Color.decode(background);
                this.backgroundImage = null;
                this.isColor = true;
            } catch (NumberFormatException e) {
                this.backgroundColor = Color.BLACK;
            }
        } else {
            loadImage(background);
            this.isColor = false;
        }
        this.currentPath = background;
        repaint();
    }

    private void loadImage(String imagePath) {
        try {
            byte[] imageData = vfs.readFile(imagePath);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
                backgroundImage = ImageIO.read(bis);
            }
        } catch (Exception e) {
            System.err.println("Failed to load background image: " + e.getMessage());
            backgroundImage = null;
            backgroundColor = Color.BLACK;
            isColor = true;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (isColor) {
            g.setColor(backgroundColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        } else if (backgroundImage != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            // Calculate scaling to maintain aspect ratio and cover the entire panel
            double scaleX = (double) getWidth() / backgroundImage.getWidth();
            double scaleY = (double) getHeight() / backgroundImage.getHeight();
            double scale = Math.max(scaleX, scaleY);
            
            int scaledWidth = (int) (backgroundImage.getWidth() * scale);
            int scaledHeight = (int) (backgroundImage.getHeight() * scale);
            
            // Center the image
            int x = (getWidth() - scaledWidth) / 2;
            int y = (getHeight() - scaledHeight) / 2;
            
            g2d.drawImage(backgroundImage, x, y, scaledWidth, scaledHeight, null);
            g2d.dispose();
        }
    }
}
