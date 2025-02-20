package org.Finite.MicrOS.Files;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;

import org.Finite.MicrOS.core.VirtualFileSystem;
public class FileMetadata {
    private final String name;
    private final String virtualPath;
    private final String mimeType;
    private final long size;
    private final Date created;
    private final Date modified;
    private final boolean isDirectory;

    public FileMetadata(String virtualPath, Path realPath, VirtualFileSystem vfs) throws Exception {
        this.virtualPath = virtualPath;
        this.name = realPath.getFileName().toString();
        this.mimeType = vfs.getMimeType(virtualPath);
        
        BasicFileAttributes attrs = Files.readAttributes(realPath, BasicFileAttributes.class);
        this.size = attrs.size();
        this.created = new Date(attrs.creationTime().toMillis());
        this.modified = new Date(attrs.lastModifiedTime().toMillis());
        this.isDirectory = attrs.isDirectory();
    }

    public String getName() { return name; }
    public String getVirtualPath() { return virtualPath; }
    public String getMimeType() { return mimeType; }
    public long getSize() { return size; }
    public Date getCreated() { return created; }
    public Date getModified() { return modified; }
    public boolean isDirectory() { return isDirectory; }

    public String getFormattedSize() {
        if (isDirectory) return "<DIR>";
        if (size < 1024) return size + " B";
        if (size < 1024*1024) return String.format("%.1f KB", size/1024.0);
        return String.format("%.1f MB", size/(1024.0*1024.0));
    }
}
