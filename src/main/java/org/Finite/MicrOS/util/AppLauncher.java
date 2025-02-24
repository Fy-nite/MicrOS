package org.Finite.MicrOS.util;

import javax.swing.*;
import java.awt.*;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.apps.AppManifest;

public class AppLauncher {
    private static boolean isMicrOSEnvironment = false;
    
    public static boolean isRunningInMicrOS() {
        return isMicrOSEnvironment;
    }
    public static void launchStandalone(Class<? extends MicrOSApp> appClass) {
        isMicrOSEnvironment = false;
        try {
            // Create minimal environment
            JFrame frame = new JFrame();
            JDesktopPane desktop = new JDesktopPane();
            frame.setContentPane(desktop);
            
            // Initialize core systems
            VirtualFileSystem vfs = VirtualFileSystem.getInstance();
            WindowManager wm = new WindowManager(desktop, vfs);
            
            // Create app instance
            MicrOSApp app = appClass.getDeclaredConstructor().newInstance();
            AppManifest manifest = new AppManifest();
            manifest.setName(appClass.getSimpleName());
            manifest.setIdentifier("standalone." + appClass.getName());
            manifest.setMainClass(appClass.getName());
            app.setManifest(manifest);
            
            // Initialize and start app
            app.initialize(wm, vfs);
            JComponent ui = app.createUI();
            app.onStart();
            
            // Setup frame
            frame.setTitle(manifest.getName());
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Add to desktop
            JInternalFrame internal = new JInternalFrame(manifest.getName());
            internal.setContentPane(ui);
            internal.setSize(780, 580);
            internal.setLocation(10, 10);
            internal.setVisible(true);
            desktop.add(internal);
            
            // Show frame
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Error launching app: " + e.getMessage(),
                "Launch Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public static boolean launchInMicrOS(WindowManager wm, Class<? extends MicrOSApp> appClass) {
        isMicrOSEnvironment = true;
        try {
            MicrOSApp app = appClass.getDeclaredConstructor().newInstance();
            AppManifest manifest = new AppManifest();
            manifest.setName(appClass.getSimpleName());
            manifest.setIdentifier("micros." + appClass.getName());
            manifest.setMainClass(appClass.getName());
            app.setManifest(manifest);
            
            wm.launchApp(app);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
