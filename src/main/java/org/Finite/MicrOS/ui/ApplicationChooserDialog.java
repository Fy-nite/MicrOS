package org.Finite.MicrOS.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.Finite.MicrOS.core.ApplicationAssociation;
import org.Finite.MicrOS.core.VirtualFileSystem;

/**
 * Dialog for selecting an application to open a file or perform an action
 */
public class ApplicationChooserDialog extends JDialog {
    
    private final VirtualFileSystem vfs;
    private final List<ApplicationAssociation> applications;
    private final String fileExtension;
    private final String actionId;
    
    private String selectedAppId;
    private boolean rememberChoice;
    
    /**
     * Creates a dialog for choosing an application to open a file
     * 
     * @param parent The parent component
     * @param vfs Virtual file system reference
     * @param applications List of compatible applications
     * @param fileName Name of the file to open
     * @param fileExtension Extension of the file
     */
    public ApplicationChooserDialog(Component parent, VirtualFileSystem vfs, 
                                    List<ApplicationAssociation> applications,
                                    String fileName, String fileExtension) {
        super(getFrame(parent), "Open With", true);
        this.vfs = vfs;
        this.applications = applications;
        this.fileExtension = fileExtension;
        this.actionId = null;
        
        initialize("Open \"" + fileName + "\" with:");
    }
    
    // /**
    //  * Creates a dialog for choosing an application to perform an action
    //  * 
    //  * @param parent The parent component
    //  * @param vfs Virtual file system reference
    //  * @param applications List of compatible applications
    //  * @param actionId The action identifier
    //  * @param actionName User-friendly name of the action
    //  */
    // public ApplicationChooserDialog(Component parent, VirtualFileSystem vfs, 
    //                                 List<ApplicationAssociation> applications,
    //                                 String actionId, String actionName) {
    //     super(getFrame(parent), "Choose Application", true);
    //     this.vfs = vfs;
    //     this.applications = applications;
    //     this.fileExtension = null;
    //     this.actionId = actionId;
        
    //     initialize("Choose application for " + actionName + ":");
    // }
    
    /**
     * Helper method to find the parent frame
     */
    private static JFrame getFrame(Component parent) {
        if (parent == null) {
            return null;
        }
        if (parent instanceof JFrame) {
            return (JFrame) parent;
        }
        return (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, parent);
    }
    
    /**
     * Initializes the dialog components
     */
    private void initialize(String headerText) {
        setLayout(new BorderLayout(10, 10));
        setResizable(true);
        setMinimumSize(new Dimension(400, 300));
        
        // Header
        JLabel headerLabel = new JLabel(headerText);
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(headerLabel, BorderLayout.NORTH);
        
        // Application list
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        
        ButtonGroup group = new ButtonGroup();
        boolean hasSelection = false;
        
        for (ApplicationAssociation app : applications) {
            JRadioButton radioBtn = new JRadioButton();
            radioBtn.setActionCommand(app.getId());
            
            // If this is the system default, preselect it
            if (app.isSystemDefault() && !hasSelection) {
                radioBtn.setSelected(true);
                selectedAppId = app.getId();
                hasSelection = true;
            }
            
            group.add(radioBtn);
            
            // Create the app item panel with icon and description
            JPanel appPanel = new JPanel(new BorderLayout(10, 5));
            appPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            // Add the icon if available
            if (app.getIconPath() != null && !app.getIconPath().isEmpty()) {
                try {
                    byte[] iconData = vfs.readFile(app.getIconPath());
                    ImageIcon icon = new ImageIcon(iconData);
                    // Scale the icon to a reasonable size
                    Image scaledImage = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                    JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));
                    appPanel.add(iconLabel, BorderLayout.WEST);
                } catch (Exception e) {
                    // Cannot load icon, will just show the name
                }
            }
            
            // Add app name and description
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            
            JLabel nameLabel = new JLabel(app.getDisplayName());
            nameLabel.setFont(new Font("Dialog", Font.BOLD, 12));
            textPanel.add(nameLabel);
            
            if (app.getDescription() != null && !app.getDescription().isEmpty()) {
                JLabel descLabel = new JLabel(app.getDescription());
                descLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
                textPanel.add(descLabel);
            }
            
            appPanel.add(textPanel, BorderLayout.CENTER);
            appPanel.add(radioBtn, BorderLayout.EAST);
            
            // Make the whole panel clickable to select the radio button
            appPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    radioBtn.setSelected(true);
                    selectedAppId = app.getId();
                }
            });
            
            listPanel.add(appPanel);
            listPanel.add(Box.createVerticalStrut(5));
        }
        
        // If nothing was selected, select the first item
        if (!hasSelection && !applications.isEmpty()) {
            ((JRadioButton)((JPanel)listPanel.getComponent(0)).getComponent(2)).setSelected(true);
            selectedAppId = applications.get(0).getId();
        }
        
        // Make the radio buttons update the selection
        for (ApplicationAssociation app : applications) {
            for (Component comp : listPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    Component radioComp = ((JPanel)comp).getComponent(2);
                    if (radioComp instanceof JRadioButton) {
                        JRadioButton radio = (JRadioButton)radioComp;
                        radio.addActionListener(e -> selectedAppId = e.getActionCommand());
                    }
                }
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(scrollPane, BorderLayout.CENTER);
        
        // Footer with "Remember this choice" checkbox and buttons
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JCheckBox rememberCheckbox = new JCheckBox("Always use this app for this purpose");
        rememberCheckbox.addActionListener(e -> rememberChoice = rememberCheckbox.isSelected());
        footerPanel.add(rememberCheckbox, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            selectedAppId = null;
            dispose();
        });
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        
        footerPanel.add(buttonPanel, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(getOwner());
    }
    
    /**
     * Gets the ID of the selected application
     * 
     * @return Application ID or null if canceled
     */
    public String getSelectedApplicationId() {
        return selectedAppId;
    }
    
    /**
     * Checks if the user opted to remember this choice
     * 
     * @return true if the choice should be remembered
     */
    public boolean isRememberChoice() {
        return rememberChoice;
    }
    
    /**
     * Gets the file extension this dialog was created for
     * 
     * @return File extension or null if this is for an action
     */
    public String getFileExtension() {
        return fileExtension;
    }
    
    /**
     * Gets the action ID this dialog was created for
     * 
     * @return Action ID or null if this is for a file
     */
    public String getActionId() {
        return actionId;
    }
}