package org.Finite.MicrOS.core;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import org.Finite.MicrOS.AsmRunner;
import org.Finite.MicrOS.apps.AppLoader;

/**
 * A virtual file system for managing files and directories within the MicrOS environment.
 */
public class VirtualFileSystem {
    private final Path rootDirectory;
    private static VirtualFileSystem instance;
    private final Map<String, String> mimeTypes;
    private final Map<String, Icon> fileIcons;
    private final Map<String, FileRunner> extensionRunners;
    private final Map<String, ProgramExecutor> programRegistry = new HashMap<>();
    
    private static final int THUMBNAIL_SIZE = 64;

    @FunctionalInterface
    public interface FileRunner {
        void run(File file) throws IOException;
    }

    @FunctionalInterface
    public interface ProgramExecutor {
        void execute(String[] args) throws IOException;
    }

    private AppLoader appLoader;

    /**
     * Private constructor to initialize the virtual file system.
     */
    private VirtualFileSystem() {
        // Get the location of the running JAR file
        Path jarLocation;
        try {
            jarLocation = Paths.get(VirtualFileSystem.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI())
                .getParent();
        } catch (Exception e) {
            // Fallback to user.dir if we can't get JAR location
            jarLocation = Paths.get(System.getProperty("user.dir"));
        }
        
        this.rootDirectory = jarLocation.resolve("filesystem");
        this.mimeTypes = new HashMap<>();
        this.fileIcons = new HashMap<>();
        this.extensionRunners = new HashMap<>();
        initializeFileSystem();
        initializeMimeTypes();
        registerDefaultRunners();
    }

    /**
     * Initializes the MIME types for different file extensions.
     */
    private void initializeMimeTypes() {
        // Text files
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("md", "text/markdown");
        mimeTypes.put("java", "text/x-java-source");
        mimeTypes.put("json", "application/json");
        mimeTypes.put("xml", "application/xml");
        mimeTypes.put("html", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "text/javascript");

        // Image files
        mimeTypes.put("png", "image/png");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("bmp", "image/bmp");

        // Other common types
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("zip", "application/zip");
    }

    /**
     * Initializes the virtual file system by creating default directories and files.
     */
    private void initializeFileSystem() {
        try {
            // Only create the directory structure if root doesn't exist
            if (!Files.exists(rootDirectory)) {
                Files.createDirectory(rootDirectory);
                
                // Create default directories
                createDirectory("/home");
                createDirectory("/system");
                createDirectory("/apps");
                createDirectory("/docs");
                createDirectory("/images");
                createDirectory("/bin");

                // Copy the background image
                copyResourceFile("/images/bg.png", "/images/background.png");

                // Create TextEditor configuration directory and copy default configs
                createDirectory("/system/texteditor/syntax");
                copyResourceFile("/default_configs/system/texteditor/syntax/asm.json", 
                                "/system/texteditor/syntax/asm.json");
                copyResourceFile("/default_configs/system/texteditor/syntax/java.json", 
                                "/system/texteditor/syntax/java.json");
            }

            // Initialize app directory
            createDirectory("/apps");
            appLoader = new AppLoader(resolveVirtualPath("/apps").toString());
            appLoader.loadApps();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copies a resource file from the JAR to the virtual file system.
     *
     * @param resourcePath Path to the resource file in the JAR
     * @param virtualPath Path to the file in the virtual file system
     */
    private void copyResourceFile(String resourcePath, String virtualPath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                createFile(virtualPath, is.readAllBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the MIME type of a file based on its extension.
     *
     * @param virtualPath Path to the file in the virtual file system
     * @return MIME type of the file
     */
    public String getMimeType(String virtualPath) {
        String extension = getFileExtension(virtualPath).toLowerCase();
        return mimeTypes.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * Gets the icon for a file based on its MIME type.
     *
     * @param virtualPath Path to the file in the virtual file system
     * @return Icon representing the file
     */
    public Icon getFileIcon(String virtualPath) {
        try {
            String mimeType = getMimeType(virtualPath);
            
            // Return cached icon if available
            if (fileIcons.containsKey(virtualPath)) {
                return fileIcons.get(virtualPath);
            }

            // Generate thumbnail for images
            if (mimeType.startsWith("image/")) {
                Icon icon = createImageThumbnail(virtualPath);
                fileIcons.put(virtualPath, icon);
                return icon;
            }

            // Return default icon based on mime type
            return getDefaultIcon(mimeType);
        } catch (Exception e) {
            return UIManager.getIcon("FileView.fileIcon");
        }
    }

    /**
     * Creates a thumbnail icon for an image file.
     *
     * @param virtualPath Path to the image file in the virtual file system
     * @return Thumbnail icon for the image
     * @throws IOException If an error occurs while reading the image file
     */
    private Icon createImageThumbnail(String virtualPath) throws IOException {
        BufferedImage img = ImageIO.read(resolveVirtualPath(virtualPath).toFile());
        if (img == null) return UIManager.getIcon("FileView.fileIcon");

        double scale = Math.min(
            THUMBNAIL_SIZE / (double) img.getWidth(),
            THUMBNAIL_SIZE / (double) img.getHeight()
        );
        
        int w = (int) (img.getWidth() * scale);
        int h = (int) (img.getHeight() * scale);

        BufferedImage thumb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = thumb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();

        return new ImageIcon(thumb);
    }

    /**
     * Gets the default icon for a file based on its MIME type.
     *
     * @param mimeType MIME type of the file
     * @return Default icon for the file
     */
    public Icon getDefaultIcon(String mimeType) {
        if (mimeType.startsWith("text/")) {
            return UIManager.getIcon("FileView.textFileIcon");
        } else if (mimeType.startsWith("image/")) {
            return UIManager.getIcon("FileView.imageFileIcon");
        } else {
            return UIManager.getIcon("FileView.fileIcon");
        }
    }

    /**
     * Checks if a file is a text file based on its MIME type.
     *
     * @param virtualPath Path to the file in the virtual file system
     * @return true if the file is a text file, false otherwise
     */
    public boolean isTextFile(String virtualPath) {
        String mimeType = getMimeType(virtualPath);
        return mimeType.startsWith("text/") || 
               mimeType.equals("application/json") || 
               mimeType.equals("application/xml");
    }

    /**
     * Checks if a file is an image file based on its MIME type.
     *
     * @param virtualPath Path to the file in the virtual file system
     * @return true if the file is an image file, false otherwise
     */
    public boolean isImageFile(String virtualPath) {
        return getMimeType(virtualPath).startsWith("image/");
    }

    /**
     * Creates a text file with the specified content.
     *
     * @param virtualPath Path to the file in the virtual file system
     * @param content Content to write to the file
     * @throws IOException If an error occurs while creating the file
     */
    public void createTextFile(String virtualPath, String content) throws IOException {
        createFile(virtualPath, content.getBytes());
    }

    /**
     * Gets the file extension from a file path.
     *
     * @param path File path
     * @return File extension
     */
    public String getFileExtension(String path) {
        int i = path.lastIndexOf('.');
        if (i > 0) {
            return path.substring(i + 1);
        }
        return "";
    }

    /**
     * Gets the singleton instance of the VirtualFileSystem.
     *
     * @return VirtualFileSystem instance
     */
    public static VirtualFileSystem getInstance() {
        if (instance == null) {
            instance = new VirtualFileSystem();
        }
        return instance;
    }

    /**
     * Resolves a virtual path to an actual filesystem path.
     *
     * @param virtualPath Path in the virtual file system
     * @return Resolved filesystem path
     */
    public Path resolveVirtualPath(String virtualPath) {
        // Convert virtual path to actual filesystem path
        String normalizedPath = virtualPath.replace('/', File.separatorChar);
        if (normalizedPath.startsWith(File.separator)) {
            normalizedPath = normalizedPath.substring(1);
        }
        return rootDirectory.resolve(normalizedPath);
    }
    
    /**
     * Gets the virtual path from an actual filesystem path.
     *
     * @param actualPath Actual filesystem path
     * @return Virtual path
     */
    public String getVirtualPath(Path actualPath) {
        return rootDirectory.relativize(actualPath)
                          .toString()
                          .replace(File.separatorChar, '/');
    }

    /**
     * Creates a directory in the virtual file system.
     *
     * @param virtualPath Path to the directory in the virtual file system
     * @return true if the directory was created successfully, false otherwise
     */
    public boolean createDirectory(String virtualPath) {
        try {
            Files.createDirectories(resolveVirtualPath(virtualPath));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a file with the specified content in the virtual file system.
     *
     * @param virtualPath Path to the file in the virtual file system
     * @param content Content to write to the file
     * @return true if the file was created successfully, false otherwise
     */
    public boolean createFile(String virtualPath, byte[] content) {
        try {
            Path path = resolveVirtualPath(virtualPath);
            Files.createDirectories(path.getParent());
            Files.write(path, content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reads the content of a file in the virtual file system.
     *
     * @param virtualPath Path to the file in the virtual file system
     * @return Content of the file
     * @throws IOException If an error occurs while reading the file
     */
    public byte[] readFile(String virtualPath) throws IOException {
        return Files.readAllBytes(resolveVirtualPath(virtualPath));
    }

    /**
     * Deletes a file in the virtual file system.
     *
     * @param virtualPath Path to the file in the virtual file system
     * @return true if the file was deleted successfully, false otherwise
     */
    public boolean deleteFile(String virtualPath) {
        try {
            return Files.deleteIfExists(resolveVirtualPath(virtualPath));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lists the files in a directory in the virtual file system.
     *
     * @param virtualPath Path to the directory in the virtual file system
     * @return Array of files in the directory
     */
    public File[] listFiles(String virtualPath) {
        try {
            Path dir = resolveVirtualPath(virtualPath);
            return Files.list(dir)
                       .map(Path::toFile)
                       .collect(Collectors.toList())
                       .toArray(new File[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return new File[0];
        }
    }

    /**
     * Checks if a file or directory exists in the virtual file system.
     *
     * @param virtualPath Path to the file or directory in the virtual file system
     * @return true if the file or directory exists, false otherwise
     */
    public boolean exists(String virtualPath) {
        return Files.exists(resolveVirtualPath(virtualPath));
    }

    /**
     * Gets the root path of the virtual file system.
     *
     * @return Root path of the virtual file system
     */
    public Path getRootPath() {
        return rootDirectory;
    }

    private void registerDefaultRunners() {
        // Register .masm file runner
        registerExtensionRunner("masm", file -> {
            Path asmCode = resolveVirtualPath(file.getPath());
            AsmRunner.RunASMFromFile(asmCode.toString());
        });
    }

    public void registerExtensionRunner(String extension, FileRunner runner) {
        extensionRunners.put(extension.toLowerCase(), runner);
    }

    public boolean hasExtensionRunner(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        return extensionRunners.containsKey(ext);
    }

    public void runFile(File file) {
        String ext = getFileExtension(file.getName()).toLowerCase();
        FileRunner runner = extensionRunners.get(ext);
        if (runner != null) {
            try {
                runner.run(file);
            } catch (IOException e) {
                e.printStackTrace();
                //TODO: You might want to show an error dialog here
            }
        }
    }

    public void registerProgram(String name, ProgramExecutor executor) {
        programRegistry.put(name, executor);
    }

    public boolean executeProgram(String name, String[] args) {
        ProgramExecutor executor = programRegistry.get(name);
        if (executor != null) {
            try {
                executor.execute(args);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public String getShebang(String virtualPath) throws IOException {
        byte[] content = readFile(virtualPath);
        if (content.length > 2 && content[0] == '#' && content[1] == '!') {
            // Read first line
            int newline = -1;
            for (int i = 2; i < content.length; i++) {
                if (content[i] == '\n') {
                    newline = i;
                    break;
                }
            }
            if (newline != -1) {
                return new String(content, 2, newline - 2).trim();
            }
        }
        return null;
    }

    public AppLoader getAppLoader() {
        return appLoader;
    }
}
