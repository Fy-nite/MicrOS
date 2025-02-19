package org.Finite.MicrOS;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class FileManager extends JPanel {

    private final ApplicationLauncher appLauncher;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private File currentDirectory;
    private JTextField pathField;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss"
    );
    private final VirtualFileSystem vfs;

    public FileManager(WindowManager windowManager) {
        this.appLauncher = new ApplicationLauncher(windowManager);
        this.vfs = VirtualFileSystem.getInstance();
        setLayout(new BorderLayout());
        currentDirectory = vfs.getRootPath().toFile();

        // Create toolbar
        createToolbar();

        // Create table
        createFileTable();

        // Load initial directory
        loadDirectory(currentDirectory);
    }

    private void openFile(File file) {
        appLauncher.openApplication(file);
    }

    private void createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Back button
        JButton backButton = new JButton("←");
        backButton.addActionListener(e -> navigateUp());

        // Path field
        pathField = new JTextField(currentDirectory.getAbsolutePath());
        pathField.addActionListener(e ->
            navigateTo(new File(pathField.getText()))
        );

        // Refresh button
        JButton refreshButton = new JButton("⟳");
        refreshButton.addActionListener(e -> refresh());

        // New folder button
        JButton newFolderButton = new JButton("New Folder");
        newFolderButton.addActionListener(e -> createNewFolder());

        toolbar.add(backButton);
        toolbar.add(pathField);
        toolbar.add(refreshButton);
        toolbar.add(newFolderButton);

        add(toolbar, BorderLayout.NORTH);
    }

    private void createFileTable() {
        String[] columns = { "Name", "Size", "Type", "Last Modified" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        fileTable = new JTable(tableModel);
        fileTable.setShowGrid(false);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Double click listener
        fileTable.addMouseListener(
            new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        int row = fileTable.rowAtPoint(evt.getPoint());
                        String fileName = (String) tableModel.getValueAt(
                            row,
                            0
                        );
                        File selected = new File(currentDirectory, fileName);
                        if (selected.isDirectory()) {
                            navigateTo(selected);
                        } else {
                            openFile(selected);
                        }
                    }
                }
            }
        );

        JScrollPane scrollPane = new JScrollPane(fileTable);
        add(scrollPane, BorderLayout.CENTER);
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

                tableModel.addRow(new Object[] { name, size, type, modified });
            }
        }
    }

    private void navigateTo(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            loadDirectory(directory);
        }
    }

    private void navigateUp() {
        File parent = currentDirectory.getParentFile();
        if (parent != null && parent.getPath().startsWith(vfs.getRootPath().toString())) {
            navigateTo(parent);
        }
    }

    private void refresh() {
        loadDirectory(currentDirectory);
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
