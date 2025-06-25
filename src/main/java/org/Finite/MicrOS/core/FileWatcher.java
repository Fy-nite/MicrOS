package org.Finite.MicrOS.core;

import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FileWatcher {
    private static final FileWatcher instance = new FileWatcher();
    private final Map<String, WatchInfo> watchers = new HashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    
    private static class WatchInfo {
        final Path path;
        final Consumer<Path> callback;
        long lastModified;
        
        WatchInfo(Path path, Consumer<Path> callback) {
            this.path = path;
            this.callback = callback;
            this.lastModified = path.toFile().lastModified();
        }
    }
    
    public static void watchFile(String path, Consumer<Path> onChange) {
        Path filePath = Paths.get(path);
        instance.watchers.put(path, new WatchInfo(filePath, onChange));
        
        // Check for changes every second
        instance.executor.scheduleAtFixedRate(() -> {
            WatchInfo info = instance.watchers.get(path);
            if (info != null) {
                long currentModified = info.path.toFile().lastModified();
                if (currentModified > info.lastModified) {
                    info.lastModified = currentModified;
                    info.callback.accept(info.path);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    public static void stopWatching(String path) {
        instance.watchers.remove(path);
    }
}
