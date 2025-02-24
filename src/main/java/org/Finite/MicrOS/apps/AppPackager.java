package org.Finite.MicrOS.apps;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class AppPackager extends MicrOSApp {
    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private File currentAppDir;
    
    public AppPackager() {
        AppManifest manifest = new AppManifest();
        manifest.setName("App Packager");
        manifest.setAppType(AppType.APP_PACKAGER);
        manifest.setIdentifier("org.finite.micros.apppackager");  // Add proper identifier
        setManifest(manifest);
    }
    
    @Override
    public JComponent createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        
        JButton newButton = new JButton("New App Bundle");
        newButton.addActionListener(e -> createNewBundle());
        
        JButton openButton = new JButton("Open App Bundle");
        openButton.addActionListener(e -> openBundle());
        
        JButton packButton = new JButton("Pack Bundle");
        packButton.addActionListener(e -> packBundle());
        
        toolbar.add(newButton);
        toolbar.add(openButton);
        toolbar.add(packButton);
        
        // Create file list with drag & drop support
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(fileList);
        
        // Add drag & drop support
        fileList.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List<File> droppedFiles = (java.util.List<File>) 
                        evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    
                    if (currentAppDir == null) {
                        JOptionPane.showMessageDialog(
                            SwingUtilities.getWindowAncestor(fileList),
                            "Please create or open an app bundle first",
                            "No Bundle Open",
                            JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                    
                    for (File file : droppedFiles) {
                        // Copy file to app bundle
                        Path targetPath = currentAppDir.toPath().resolve(file.getName());
                        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    refreshFileList();
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(fileList),
                        "Error importing files: " + e.getMessage(),
                        "Import Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });
        
        // Add a "Drop files here" label when list is empty
        fileList.addListSelectionListener(e -> {
            if (listModel.isEmpty()) {
                fileList.setBackground(new Color(245, 245, 245));
                JLabel emptyLabel = new JLabel("Drop files here to add to bundle", SwingConstants.CENTER);
                emptyLabel.setForeground(Color.GRAY);
                scrollPane.setViewportView(emptyLabel);
            } else {
                fileList.setBackground(UIManager.getColor("List.background"));
                scrollPane.setViewportView(fileList);
            }
        });
        
        mainPanel.add(toolbar, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private void createNewBundle() {
        String appName = JOptionPane.showInputDialog(
            SwingUtilities.getWindowAncestor(fileList),
            "Enter app name (without .app extension):",
            "New App Bundle",
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (appName != null && !appName.trim().isEmpty()) {
            if (!appName.endsWith(".app")) {
                appName += ".app";
            }
            
            File appsDir = new File(vfs.resolveVirtualPath("/apps").toString());
            currentAppDir = new File(appsDir, appName);
            
            if (currentAppDir.exists()) {
                JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(fileList),
                    "An app with this name already exists.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            
            currentAppDir.mkdirs();
            refreshFileList();
        }
    }
    
    private void openBundle() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select App Bundle");
        
        if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(fileList)) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected.getName().endsWith(".app")) {
                currentAppDir = selected;
                refreshFileList();
            } else {
                JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(fileList),
                    "Please select a valid .app bundle directory.",
                    "Invalid Selection",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    private void packBundle() {
        if (currentAppDir == null) {
            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(fileList),
                "No app bundle is currently open.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(currentAppDir.getName().replace(".app", ".zip")));
            chooser.setDialogTitle("Save App Bundle As");
            
            if (chooser.showSaveDialog(SwingUtilities.getWindowAncestor(fileList)) == JFileChooser.APPROVE_OPTION) {
                File zipFile = chooser.getSelectedFile();
                if (!zipFile.getName().toLowerCase().endsWith(".zip")) {
                    zipFile = new File(zipFile.getParentFile(), zipFile.getName() + ".zip");
                }
                
                try (FileOutputStream fos = new FileOutputStream(zipFile);
                     ZipOutputStream zos = new ZipOutputStream(fos)) {
                    
                    Path sourcePath = currentAppDir.toPath();
                    Files.walk(sourcePath)
                         .filter(path -> !Files.isDirectory(path))
                         .forEach(path -> {
                             try {
                                 String relativePath = sourcePath.relativize(path).toString();
                                 ZipEntry entry = new ZipEntry(currentAppDir.getName() + "/" + relativePath);
                                 zos.putNextEntry(entry);
                                 Files.copy(path, zos);
                                 zos.closeEntry();
                             } catch (IOException e) {
                                 throw new RuntimeException(e);
                             }
                         });
                }
                
                JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(fileList),
                    "App bundle packed successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(fileList),
                "Error packing app bundle: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private void refreshFileList() {
        listModel.clear();
        if (currentAppDir != null && currentAppDir.exists()) {
            try {
                Files.walk(currentAppDir.toPath())
                     .filter(path -> !Files.isDirectory(path))
                     .map(path -> currentAppDir.toPath().relativize(path).toString())
                     .forEach(listModel::addElement);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(fileList),
                    "Error reading app bundle: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    @Override
    public void onStart() {
        // Nothing to do
    }

    @Override
    public void onStop() {
        // Nothing to do
    }
}