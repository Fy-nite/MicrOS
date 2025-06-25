package org.Finite.MicrOS.apps;

import javax.swing.*;
import java.util.function.Consumer;

import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.ui.ErrorDialog;
import org.Finite.MicrOS.util.AppLauncher;
import org.Finite.MicrOS.core.MessageBus;
import org.Finite.MicrOS.core.Intent;
public abstract class MicrOSApp {
    protected WindowManager windowManager;
    protected VirtualFileSystem vfs;
    private AppManifest manifest;
    private int threadId = -1;  // Add this field

    public void initialize(WindowManager windowManager, VirtualFileSystem vfs) {
        this.windowManager = windowManager;
        this.vfs = vfs;
    }
    
    public void setManifest(AppManifest manifest) {
        this.manifest = manifest;
    }
    
    public AppManifest getManifest() {
        return manifest;
    }
    protected boolean isRunningInMicrOS() {
        return AppLauncher.isRunningInMicrOS();
    }
    
    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public boolean isRunning() {
        return threadId != -1 && windowManager.isAppThreadRunning(threadId);
    }

    protected void cleanupThread() {
        if (threadId != -1) {
            windowManager.stopAppThread(threadId);
            threadId = -1;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        cleanupThread();
        super.finalize();
    }

    public abstract JComponent createUI();
    public abstract void onStart();
    public abstract void onStop();
    
    /**
     * Reports an error in the app context
     */
    protected void reportError(String message, Throwable error) {
        if (windowManager != null) {
            ErrorDialog.showError(windowManager.getDesktop(), message, error);
        } else {
            // Fallback if windowManager not initialized
            error.printStackTrace();
        }
    }

    /**
     * Safe execution wrapper for app methods
     */
    protected void safeExecute(String operation, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            reportError("Error during " + operation, e);
        }
    }

    public void handleIntent(Intent intent) {
        // Default implementation does nothing
        // Override in your app to handle intents
    }
    
    protected void sendMessage(String targetId, Object message) {
        MessageBus.send(targetId, message);
    }
    
    protected void subscribeToMessages(Consumer<Object> handler) {
        MessageBus.subscribe(getManifest().getIdentifier(), handler);
    }
}
