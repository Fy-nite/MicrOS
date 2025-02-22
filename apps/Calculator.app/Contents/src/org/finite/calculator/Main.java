package org.finite.calculator;

import javax.swing.*;
import org.Finite.MicrOS.apps.MicrOSApp;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.core.Intent;

public class Main extends MicrOSApp {
    private boolean cliMode = false;
    private String consoleId;
    
    @Override
    public JComponent createUI() {
        if (cliMode) {
            return new JPanel(); // Minimal UI for CLI mode
        }
        // Normal GUI implementation
        return new JPanel();
    }
    
    @Override 
    public void handleIntent(Intent intent) {
        if (intent.getExtra("cli") != null) {
            cliMode = true;
            consoleId = (String) intent.getExtra("consoleId");
            String[] args = (String[]) intent.getExtra("args");
            handleCLICommand(args);
        }
    }
    
    private void handleCLICommand(String[] args) {
        if (args.length < 3) {
            printToConsole("Usage: calc <number> <operator> <number>");
            return;
        }
        
        try {
            double a = Double.parseDouble(args[0]);
            double b = Double.parseDouble(args[2]);
            String result = "";
            
            switch (args[1]) {
                case "+": result = String.valueOf(a + b); break;
                case "-": result = String.valueOf(a - b); break;
                case "*": result = String.valueOf(a * b); break;
                case "/": result = String.valueOf(a / b); break;
                default:
                    printToConsole("Invalid operator. Use +, -, *, or /");
                    return;
            }
            
            printToConsole(args[0] + " " + args[1] + " " + args[2] + " = " + result);
        } catch (NumberFormatException e) {
            printToConsole("Invalid numbers provided");
        }
    }
    
    private void printToConsole(String text) {
        if (cliMode && consoleId != null) {
            windowManager.writeToConsole(consoleId, text);
        }
    }
    
    @Override public void onStart() {}
    @Override public void onStop() {}
}
