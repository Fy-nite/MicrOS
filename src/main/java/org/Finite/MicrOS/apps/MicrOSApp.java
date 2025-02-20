package org.Finite.MicrOS.apps;

import javax.swing.*;
import org.Finite.MicrOS.VirtualFileSystem;
import org.Finite.MicrOS.WindowManager;

public abstract class MicrOSApp {
    protected WindowManager windowManager;
    protected VirtualFileSystem vfs;
    
    public void initialize(WindowManager windowManager, VirtualFileSystem vfs) {
        this.windowManager = windowManager;
        this.vfs = vfs;
    }
    
    public abstract JComponent createUI();
    public abstract void onStart();
    public abstract void onStop();
}
