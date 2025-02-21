package org.Finite.MicrOS.apps;

import javax.swing.*;

import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;

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
}
