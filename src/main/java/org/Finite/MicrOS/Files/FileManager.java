package org.Finite.MicrOS.Files;

import org.Finite.MicrOS.util.AppInstaller;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.DataFlavor;
import java.nio.file.Files;
import java.nio.file.*;
import java.io.File;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import javax.swing.ImageIcon;


public class FileManager extends JPanel {

    private JTable fileTable;
    private DefaultTableModel tableModel;
    private File currentDirectory;
    private JTextField pathField;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss"
    );
    private final VirtualFileSystem vfs;
    private final WindowManager windowManager;
    private JLabel statusBar;

    public FileManager(WindowManager windowManager) {
        this.vfs = VirtualFileSystem.getInstance();
        this.windowManager = windowManager;
        setLayout(new BorderLayout(10, 10)); // Add padding
        setPreferredSize(new Dimension(800, 600)); // Set preferred size
        currentDirectory = vfs.getRootPath().toFile();

        // Create status bar
        createStatusBar();

        // Create toolbar
        createToolbar();

        // Create table
        createFileTable();

        // Load initial directory
        loadDirectory(currentDirectory);

        // Add drag-and-drop support
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : droppedFiles) {
                        if (file.getName().endsWith(".zip")) {
                            Path tempDir = Files.createTempDirectory("app_install");
                            AppInstaller.extractZip(file, tempDir);
                            Files.walk(tempDir)
                                .filter(AppInstaller::isAppDirectory)
                                .findFirst()
                                .ifPresent(AppInstaller::installApp);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void openFile(File file) {
        String extension = vfs.getFileExtension(file.getName()).toLowerCase();
        Set<String> associations = windowManager.getFileAssociations(extension);
        
        if (!associations.isEmpty()) {
            // Use the first association as default
            String defaultApp = associations.iterator().next();
            windowManager.openFileWith(vfs.getVirtualPath(file.toPath()), defaultApp);
        }
    }

    private void createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Back button
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> navigateUp());

        // Path field
        pathField = new JTextField(currentDirectory.getAbsolutePath());
        pathField.addActionListener(e ->
            navigateTo(new File(pathField.getText()))
        );

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());

        // New folder button
        JButton newFolderButton = new JButton("New folder");
        newFolderButton.addActionListener(e -> createNewFolder());

        // New file button
        JButton newFileButton = new JButton("New file");
        newFileButton.addActionListener(e -> createNewFile());

        toolbar.add(backButton);
        toolbar.addSeparator(new Dimension(5, 0));
        toolbar.add(pathField);
        toolbar.addSeparator(new Dimension(5, 0));
        toolbar.add(refreshButton);
        toolbar.addSeparator(new Dimension(5, 0));
        toolbar.add(newFolderButton);
        toolbar.addSeparator(new Dimension(5, 0));
        toolbar.add(newFileButton);

        add(toolbar, BorderLayout.NORTH);
    }

    private void createFileTable() {
        String[] columns = { "", "Name", "Size", "Type", "Last Modified" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        fileTable = new JTable(tableModel);
        fileTable.setShowGrid(false);
        fileTable.setRowHeight(30);
        fileTable.setIntercellSpacing(new Dimension(5, 5));
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add popup menu
        fileTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = fileTable.rowAtPoint(evt.getPoint());
                    String fileName = (String) tableModel.getValueAt(row, 1);
                    File selected = new File(currentDirectory, fileName);
                    if (selected.isDirectory()) {
                        navigateTo(selected);
                    } else {
                        openFile(selected);
                    }
                }
            }

            public void mousePressed(java.awt.event.MouseEvent evt) {
                showContextMenu(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                showContextMenu(evt);
            }
        });

        JScrollPane scrollPane = new JScrollPane(fileTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void showContextMenu(java.awt.event.MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            int row = fileTable.rowAtPoint(evt.getPoint());
            if (row >= 0) {
                fileTable.setRowSelectionInterval(row, row);
                String fileName = (String) tableModel.getValueAt(row, 1);
                File selected = new File(currentDirectory, fileName);
                
                if (!selected.isDirectory()) {
                    JPopupMenu contextMenu = createFileContextMenu(selected);
                    contextMenu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }
        }
    }

    private JPopupMenu createFileContextMenu(File file) {
        JPopupMenu menu = new JPopupMenu();
        String extension = vfs.getFileExtension(file.getName()).toLowerCase();
        String virtualPath = vfs.getVirtualPath(file.toPath());
        
        // Get all registered applications for this file type
        Set<String> associations = windowManager.getFileAssociations(extension);
        
        // Add "Open" item that uses default application
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> openFile(file));
        menu.add(openItem);
        
        // Add "Open With" submenu
        if (!associations.isEmpty()) {
            JMenu openWithMenu = new JMenu("Open With");
            
            // Add all associated applications
            for (String appType : associations) {
                String appName = getAppDisplayName(appType);
                JMenuItem appItem = new JMenuItem(appName);
                appItem.addActionListener(e -> windowManager.openFileWith(virtualPath, appType));
                openWithMenu.add(appItem);
            }
            
            menu.add(openWithMenu);
        }
        
        // Add "Delete" item
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> deleteFile(file));
        menu.add(deleteItem);

        // Add "Rename" item
        JMenuItem renameItem = new JMenuItem("Rename");
        renameItem.addActionListener(e -> renameFile(file));
        menu.add(renameItem);

        return menu;
    }

    private void deleteFile(File file) {
        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this file?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            if (file.delete()) {
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Could not delete file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void renameFile(File file) {
        String newName = JOptionPane.showInputDialog(this, "Enter new name:", file.getName());
        if (newName != null && !newName.trim().isEmpty()) {
            File newFile = new File(file.getParent(), newName);
            if (file.renameTo(newFile)) {
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Could not rename file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String getAppDisplayName(String appType) {
        switch (appType) {
            case "texteditor": return "Text Editor";
            case "webviewer": return "Web Viewer";
            case "imageviewer": return "Image Viewer";
            default: return appType;
        }
    }

    private void loadDirectory(File directory) {
        if (!directory.isDirectory()) {
            return;
        }

        currentDirectory = directory;
        String virtualPath = vfs.getVirtualPath(directory.toPath());
        pathField.setText("/" + virtualPath);
        tableModel.setRowCount(0);

        File[] files = vfs.listFiles(virtualPath);
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String size = file.isDirectory()
                    ? "<DIR>"
                    : String.format("%d KB", file.length() / 1024);
                String type = file.isDirectory()
                    ? "Folder"
                    : getFileExtension(file);
                String modified = dateFormat.format(
                    new Date(file.lastModified())
                );
                ImageIcon icon = new ImageIcon(file.isDirectory() ? "Folder" : "File");

                tableModel.addRow(new Object[] { icon, name, size, type, modified });
            }
        }

        updateStatusBar();
    }

    private void navigateTo(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            loadDirectory(directory);
        }

        updateStatusBar();
    }

    private void navigateUp() {
        File parent = currentDirectory.getParentFile();
        if (parent != null && parent.getPath().startsWith(vfs.getRootPath().toString())) {
            navigateTo(parent);
        }

        updateStatusBar();
    }

    private void refresh() {
        loadDirectory(currentDirectory);
        updateStatusBar();
    }

    private void createNewFolder() {
        String folderName = JOptionPane.showInputDialog(this, "Enter folder name:");
        if (folderName != null && !folderName.trim().isEmpty()) {
            String virtualPath = vfs.getVirtualPath(currentDirectory.toPath()) + "/" + folderName;
            if (vfs.createDirectory(virtualPath)) {
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Could not create folder", "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }

        updateStatusBar();
    }

    private void createNewFile() {
        String fileName = JOptionPane.showInputDialog(this, "Enter file name:");
        if (fileName != null && !fileName.trim().isEmpty()) {
            String virtualPath = vfs.getVirtualPath(currentDirectory.toPath()) + "/" + fileName;
            if (vfs.createFile(virtualPath)) {
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Could not create file", "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }

        updateStatusBar();
    }

    private void createStatusBar() {
        statusBar = new JLabel("Ready");
        add(statusBar, BorderLayout.SOUTH);
    }

    private void updateStatusBar() {
        String path = currentDirectory.getAbsolutePath();
        int itemCount = tableModel.getRowCount();
        statusBar.setText(String.format("%s - %d item(s)", path, itemCount));
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "File";
        }
        return name.substring(lastIndexOf + 1).toUpperCase();
    }
}
