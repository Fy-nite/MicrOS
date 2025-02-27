package org.Finite.MicrOS.Files;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.Finite.MicrOS.apps.AppManifest;
import org.Finite.MicrOS.apps.JavaFXApp;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.util.AppInstaller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileManagerFX extends JavaFXApp {
    
    private VirtualFileSystem vfs;
    private WindowManager windowManager;
    private File currentDirectory;
    private TextField pathField;
    private TableView<File> fileTable;
    private Label statusLabel;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public FileManagerFX() {
        AppManifest manifest = new AppManifest();
        manifest.setName("File Explorer FX");
        manifest.setIdentifier("org.finite.micros.filemanager.fx");
        setManifest(manifest);
    }
    
    @Override
    public void onStart() {
        this.vfs = VirtualFileSystem.getInstance();
        this.windowManager = getWindowManagerFromApp(); // Fixed method name
        this.currentDirectory = vfs.getRootPath().toFile();
        loadDirectory(currentDirectory);
    }
    
    @Override
    public void onStop() {
        // Cleanup if needed
    }
    
    @Override
    protected Region createFXContent() {
        BorderPane root = new BorderPane();
        root.setPrefSize(800, 600);
        
        // Create toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Create file table
        fileTable = createFileTable();
        root.setCenter(fileTable);
        
        // Create status bar
        statusLabel = new Label("Ready");
        root.setBottom(statusLabel);
        
        // Setup drag and drop
        setupDragAndDrop(root);
        
        return root;
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(5);
        toolbar.setPadding(new Insets(5));
        
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> navigateUp());
        
        pathField = new TextField(currentDirectory.getAbsolutePath());
        pathField.setOnAction(e -> navigateTo(new File(pathField.getText())));
        HBox.setHgrow(pathField, Priority.ALWAYS);
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refresh());
        
        Button newFolderButton = new Button("New Folder");
        newFolderButton.setOnAction(e -> createNewFolder());
        
        Button newFileButton = new Button("New File");
        newFileButton.setOnAction(e -> createNewFile());
        
        toolbar.getChildren().addAll(
            backButton, pathField, refreshButton, newFolderButton, newFileButton
        );
        
        return toolbar;
    }
    
    private TableView<File> createFileTable() {
        TableView<File> table = new TableView<>();
        
        // Name column
        TableColumn<File, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName()));
        nameCol.setPrefWidth(250);
        
        // Size column
        TableColumn<File, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(cellData -> {
            File file = cellData.getValue();
            if (file.isDirectory()) {
                return new SimpleStringProperty("<DIR>");
            } else {
                return new SimpleStringProperty(String.format("%d KB", file.length() / 1024));
            }
        });
        sizeCol.setPrefWidth(100);
        
        // Type column
        TableColumn<File, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> {
            File file = cellData.getValue();
            if (file.isDirectory()) {
                return new SimpleStringProperty("Folder");
            } else {
                String name = file.getName();
                int lastIndexOf = name.lastIndexOf(".");
                if (lastIndexOf == -1) {
                    return new SimpleStringProperty("File");
                }
                return new SimpleStringProperty(name.substring(lastIndexOf + 1).toUpperCase());
            }
        });
        typeCol.setPrefWidth(100);
        
        // Modified column
        TableColumn<File, String> modifiedCol = new TableColumn<>("Last Modified");
        modifiedCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(dateFormat.format(new Date(cellData.getValue().lastModified()))));
        modifiedCol.setPrefWidth(150);
        
        table.getColumns().addAll(nameCol, sizeCol, typeCol, modifiedCol);
        
        // Double-click handler
        table.setRowFactory(tv -> {
            TableRow<File> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    File file = row.getItem();
                    if (file.isDirectory()) {
                        navigateTo(file);
                    } else {
                        openFile(file);
                    }
                }
            });
            
            // Context menu
            row.setContextMenu(createContextMenu(row));
            
            return row;
        });
        
        return table;
    }
    
    private ContextMenu createContextMenu(TableRow<File> row) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(e -> {
            File file = row.getItem();
            if (file != null) {
                if (file.isDirectory()) {
                    navigateTo(file);
                } else {
                    openFile(file);
                }
            }
        });
        
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            File file = row.getItem();
            if (file != null) {
                deleteFile(file);
            }
        });
        
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> {
            File file = row.getItem();
            if (file != null) {
                renameFile(file);
            }
        });
        
        contextMenu.getItems().addAll(openItem, deleteItem, renameItem);
        
        // "Open With" submenu (populated when context menu is about to be shown)
        contextMenu.setOnShowing(e -> {
            File file = row.getItem();
            if (file != null && !file.isDirectory()) {
                // Remove any existing "Open With" menu
                contextMenu.getItems().removeIf(item -> item instanceof Menu && ((Menu)item).getText().equals("Open With"));
                
                String extension = getFileExtension(file);
                var associations = windowManager.getAppAssociationManager().getFileTypeAssociations(extension);
                
                if (!associations.isEmpty()) {
                    Menu openWithMenu = new Menu("Open With");
                    String virtualPath = vfs.getVirtualPath(file.toPath());
                    
                    associations.forEach(app -> {
                        MenuItem appItem = new MenuItem(app.getDisplayName());
                        appItem.setOnAction(evt -> windowManager.openFileWith(virtualPath, app.getId()));
                        openWithMenu.getItems().add(appItem);
                    });
                    
                    openWithMenu.getItems().add(new SeparatorMenuItem());
                    
                    MenuItem chooseAppItem = new MenuItem("Choose Application...");
                    chooseAppItem.setOnAction(evt -> windowManager.openFile(virtualPath, null));
                    openWithMenu.getItems().add(chooseAppItem);
                    
                    contextMenu.getItems().add(1, openWithMenu);
                }
            }
        });
        
        return contextMenu;
    }
    
    private void setupDragAndDrop(Region root) {
        // Setup drag over behavior
        root.setOnDragOver(event -> {
            if (event.getGestureSource() != root && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        
        // Setup drag dropped behavior
        root.setOnDragDropped(event -> handleFileDrop(event));
    }
    
    private void handleFileDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        
        if (db.hasFiles()) {
            List<File> files = db.getFiles();
            success = true;
            
            // Track installation results for reporting
            int successCount = 0;
            int failureCount = 0;
            
            statusLabel.setText("Processing dropped files...");
            
            for (File file : files) {
                try {
                    // Handle zip files that might contain apps
                    if (file.getName().toLowerCase().endsWith(".zip")) {
                        statusLabel.setText("Installing app from " + file.getName() + "...");
                        
                        Path tempDir = Files.createTempDirectory("app_install");
                        AppInstaller.extractZip(file, tempDir);
                        
                        // Look for .app directories in the extracted content
                        boolean appFound = false;
                        try (var paths = Files.walk(tempDir)) {
                            for (Path path : paths.filter(AppInstaller::isAppDirectory).toList()) {
                                appFound = true;
                                if (AppInstaller.installApp(path)) {
                                    successCount++;
                                } else {
                                    failureCount++;
                                }
                            }
                        }
                        
                        if (!appFound) {
                            showAlert(
                                Alert.AlertType.WARNING,
                                "Installation Failed", 
                                "No valid MicrOS applications found in " + file.getName()
                            );
                        }
                        
                        // Clean up temp directory
                        try {
                            AppInstaller.deleteDirectory(tempDir); // Now using public method
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    // Handle direct .app directories
                    else if (file.isDirectory() && file.getName().toLowerCase().endsWith(".app")) {
                        statusLabel.setText("Installing app " + file.getName() + "...");
                        
                        if (AppInstaller.installApp(file.toPath())) {
                            successCount++;
                        } else {
                            failureCount++;
                        }
                    }
                    // Normal file handling (copy to current directory)
                    else if (file.isFile()) {
                        String targetVirtualPath = vfs.getVirtualPath(currentDirectory.toPath()) + "/" + file.getName();
                        
                        vfs.createFile(targetVirtualPath, Files.readAllBytes(file.toPath()));
                    }
                } catch (IOException e) {
                    showAlert(
                        Alert.AlertType.ERROR,
                        "Copy Failed",
                        "Error copying file: " + e.getMessage()
                    );
                }
            }
            
            // Report installation results
            if (successCount > 0 || failureCount > 0) {
                StringBuilder message = new StringBuilder();
                if (successCount > 0) {
                    message.append(successCount).append(" application(s) installed successfully.\n");
                }
                if (failureCount > 0) {
                    message.append(failureCount).append(" application(s) failed to install.");
                }
                
                showAlert(
                    failureCount > 0 ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION,
                    "Installation Results",
                    message.toString()
                );
            }
            
            // Refresh view after installations
            refresh();
            statusLabel.setText("Ready");
        }
        
        event.setDropCompleted(success);
        event.consume();
    }
    
    private void loadDirectory(File directory) {
        if (!directory.isDirectory()) {
            return;
        }
        
        currentDirectory = directory;
        String virtualPath = vfs.getVirtualPath(directory.toPath());
        pathField.setText("/" + virtualPath);
        
        File[] files = vfs.listFiles(virtualPath);
        ObservableList<File> fileList = FXCollections.observableArrayList();
        
        if (files != null) {
            fileList.addAll(files);
        }
        
        fileTable.setItems(fileList);
        updateStatusBar();
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
    
    private void openFile(File file) {
        String virtualPath = vfs.getVirtualPath(file.toPath());
        windowManager.openFile(virtualPath, null);
    }
    
    private void createNewFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a new folder");
        dialog.setContentText("Enter folder name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(folderName -> {
            if (!folderName.trim().isEmpty()) {
                String virtualPath = vfs.getVirtualPath(currentDirectory.toPath()) + "/" + folderName;
                if (vfs.createDirectory(virtualPath)) {
                    refresh();
                } else {
                    showAlert(
                        Alert.AlertType.ERROR, 
                        "Error", 
                        "Could not create folder"
                    );
                }
            }
        });
    }
    
    private void createNewFile() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New File");
        dialog.setHeaderText("Create a new file");
        dialog.setContentText("Enter file name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(fileName -> {
            if (!fileName.trim().isEmpty()) {
                String virtualPath = vfs.getVirtualPath(currentDirectory.toPath()) + "/" + fileName;
                if (vfs.createFile(virtualPath)) {
                    refresh();
                } else {
                    showAlert(
                        Alert.AlertType.ERROR, 
                        "Error", 
                        "Could not create file"
                    );
                }
            }
        });
    }
    
    private void deleteFile(File file) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete File");
        alert.setContentText("Are you sure you want to delete this file?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (file.delete()) {
                refresh();
            } else {
                showAlert(
                    Alert.AlertType.ERROR, 
                    "Error", 
                    "Could not delete file"
                );
            }
        }
    }
    
    private void renameFile(File file) {
        TextInputDialog dialog = new TextInputDialog(file.getName());
        dialog.setTitle("Rename File");
        dialog.setHeaderText("Rename File");
        dialog.setContentText("Enter new name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                File newFile = new File(file.getParent(), newName);
                if (file.renameTo(newFile)) {
                    refresh();
                } else {
                    showAlert(
                        Alert.AlertType.ERROR, 
                        "Error", 
                        "Could not rename file"
                    );
                }
            }
        });
    }
    
    private void updateStatusBar() {
        int itemCount = fileTable.getItems().size();
        statusLabel.setText(String.format("%s - %d item(s)", currentDirectory.getAbsolutePath(), itemCount));
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf + 1).toLowerCase();
    }
    
    // Added method to get WindowManager
    private WindowManager getWindowManagerFromApp() {
        return super.getWindowManager();
    }
}
