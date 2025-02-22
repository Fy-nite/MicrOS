package org.Finite.MicrOS.apps;

import javax.swing.*;

import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.ui.ErrorDialog;

public abstract class MicrOSApp {
    protected WindowManager windowManager;
    protected VirtualFileSystem vfs;
    private AppManifest manifest;  // Changed to private with getter/setter
    
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
}
