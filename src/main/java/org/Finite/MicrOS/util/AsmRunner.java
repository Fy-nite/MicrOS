package org.Finite.MicrOS.util;

import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.ui.Console;
import org.finite.*;
import org.finite.*;
import org.finite.interp;
import org.finite.ModuleManager.ModuleInit;
import org.finite.interp.instructions;

import java.io.*;

public class AsmRunner {
    public static interp interp = new interp();


    // Create an asm runner class with a VFS and WindowManager
    private final VirtualFileSystem vfs;
    private final WindowManager windowManager;
    private final Console console;
    
    public AsmRunner(VirtualFileSystem vfs, WindowManager windowManager, Console console) {
        this.vfs = vfs;
        this.windowManager = windowManager;
        this.console = console;
    }

    public static void DEMORunASMFromInstructions(String asmCode) {
        interp interp = new interp();
        String[] instructions = asmCode.split("\n");
        instructions instr = interp.parseInstructions(instructions);
        interp.ExecuteAllInstructions(instr);
    }

    public static String RunASMFromFile(String asmFile) throws IOException {
        common.exitOnHLT = false;
        // Convert to absolute path if needed
        File file = new File(asmFile);
        String absolutePath = file.getAbsolutePath();

        // Capture stdout
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        System.setOut(ps);
        
        try {
            interp.runFile(absolutePath);
            System.out.flush();
            return baos.toString();
        } finally {
            System.setOut(old);
        }
    }
}
