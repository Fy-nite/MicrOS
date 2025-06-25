package org.Finite.MicrOS.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AppInstaller {

    public static void extractZip(File zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath);
                }
                zis.closeEntry();
            }
        }
    }

    public static boolean isAppDirectory(Path dir) {
        return Files.isDirectory(dir) && dir.getFileName().toString().endsWith(".app");
    }

    public static void installApp(Path appDir) {
        // Implement the installation logic here
        // For example, move the app directory to the appropriate location
    }
}
