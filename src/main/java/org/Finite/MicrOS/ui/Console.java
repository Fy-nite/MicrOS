package org.Finite.MicrOS.ui;

import javax.swing.*;
import javax.swing.text.*;

import org.Finite.MicrOS.core.VirtualFileSystem;
import org.Finite.MicrOS.core.WindowManager;
import org.Finite.MicrOS.ui.terminal.InternalConsoleHandler;
import org.Finite.MicrOS.ui.terminal.TerminalHandler;
import org.Finite.MicrOS.ui.terminal.TerminalHandlerFactory;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class Console extends JTextPane {
    private final CommandProcessor commandProcessor;
    private final StyleContext styleContext;
    private final List<String> commandHistory;
    private int historyIndex;
    private final int maxHistorySize = 1000;
    private int inputStart;
    private TerminalHandler terminalHandler;
    private WindowManager windowManager;
    
    // Colors for different elements
    private static final Color BG_COLOR = new Color(25, 25, 25);
    private static final Color TEXT_COLOR = new Color(220, 220, 220);
    private static final Color PROMPT_COLOR = new Color(50, 200, 50);
    private static final Color ERROR_COLOR = new Color(255, 80, 80);
    private static final Color SUCCESS_COLOR = new Color(80, 255, 80);
    private static final Color INFO_COLOR = new Color(80, 180, 255);
    
    public Console() {
        this(null);
    }
    
    public Console(WindowManager windowManager) {
        this.windowManager = windowManager;
        styleContext = StyleContext.getDefaultStyleContext();
        commandHistory = new ArrayList<>();
        historyIndex = 0;
        
        // Set up appearance
        setBackground(BG_COLOR);
        setCaretColor(TEXT_COLOR);
        setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        
        // Create command processor
        commandProcessor = new CommandProcessor(this, VirtualFileSystem.getInstance());
        
        // Initialize terminal handler
        if (windowManager != null) {
            TerminalHandlerFactory factory = new TerminalHandlerFactory(windowManager);
            terminalHandler = factory.getDefaultTerminalHandler("MicrOS Terminal", this);
        } else {
            terminalHandler = new InternalConsoleHandler(this, "MicrOS Terminal");
        }
        
        // Initialize with greeting
        SwingUtilities.invokeLater(() -> {
            appendText("MicrOS Terminal v1.0\n", INFO_COLOR);
            appendText("Type 'help' for available commands\n\n", INFO_COLOR);
            showPrompt();
        });
        
        // Add key listeners
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
        
        // Add caret listener to keep caret in input area
        addCaretListener(e -> {
            if (e.getDot() < inputStart) {
                setCaretPosition(getDocument().getLength());
            }
        });

        // Add document filter to prevent editing outside input area
        ((AbstractDocument) getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs) 
                    throws BadLocationException {
                if (offset >= inputStart) {
                    super.insertString(fb, offset, text, attrs);
                    highlightCommandSyntax();
                }
            }
            
            @Override
            public void remove(FilterBypass fb, int offset, int length) 
                    throws BadLocationException {
                if (offset >= inputStart) {
                    super.remove(fb, offset, length);
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) 
                    throws BadLocationException {
                if (offset >= inputStart) {
                    super.replace(fb, offset, length, text, attrs);
                    highlightCommandSyntax();
                }
            }
        });
    }

    /**
     * Set the WindowManager for this console
     * 
     * @param windowManager WindowManager instance
     */
    public void setWindowManager(WindowManager windowManager) {
        this.windowManager = windowManager;
        
        // Update terminal handler if needed
        if (windowManager != null && terminalHandler instanceof InternalConsoleHandler) {
            TerminalHandlerFactory factory = new TerminalHandlerFactory(windowManager);
            terminalHandler = factory.getDefaultTerminalHandler("MicrOS Terminal", this);
        }
    }
    
    /**
     * Change the terminal handler to use
     * 
     * @param handler The new terminal handler to use
     */
    public void setTerminalHandler(TerminalHandler handler) {
        this.terminalHandler = handler;
    }
    
    /**
     * Get the current terminal handler
     * 
     * @return The current terminal handler
     */
    public TerminalHandler getTerminalHandler() {
        return terminalHandler;
    }

    /**
     * Show the application chooser dialog to select a terminal application
     * 
     * @return True if terminal selection changed, false otherwise
     */
    public boolean selectTerminal() {
        if (windowManager == null) {
            return false;
        }
        
        TerminalHandlerFactory factory = new TerminalHandlerFactory(windowManager);
        TerminalHandler newHandler = factory.createTerminalHandler("MicrOS Terminal", this, this);
        
        if (newHandler != null && !newHandler.getType().equals(terminalHandler.getType())) {
            terminalHandler = newHandler;
            return true;
        }
        
        return false;
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            e.consume();
            String command = getCurrentCommand();
            if (!command.trim().isEmpty()) {
                commandHistory.add(0, command);
                if (commandHistory.size() > maxHistorySize) {
                    commandHistory.remove(commandHistory.size() - 1);
                }
                historyIndex = -1;
                
                appendText("\n", TEXT_COLOR);
                processCommand(command);
            } else {
                appendText("\n", TEXT_COLOR);
                showPrompt();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            e.consume();
            navigateHistory(1);
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            e.consume();
            navigateHistory(-1);
        } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
            e.consume();
            autoComplete();
        }
    }

    private void processCommand(String command) {
        // Use terminal handler to execute the command
        if (terminalHandler != null) {
            terminalHandler.executeCommand(command);
        } else {
            // Fallback to direct execution via CommandProcessor
            commandProcessor.processCommand(command);
        }
        showPrompt();
    }

    private void showPrompt() {
        appendText(commandProcessor.getPrompt(), PROMPT_COLOR);
        inputStart = getDocument().getLength();
        setCaretPosition(inputStart);
    }

    private String getCurrentCommand() {
        try {
            int lineStart = getDocument().getDefaultRootElement()
                    .getElement(getDocument().getDefaultRootElement().getElementCount() - 1)
                    .getStartOffset();
            int promptLength = commandProcessor.getPrompt().length();
            return getText(lineStart + promptLength, 
                    getDocument().getLength() - (lineStart + promptLength));
        } catch (BadLocationException e) {
            return "";
        }
    }

    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) return;
        
        historyIndex = Math.min(Math.max(-1, historyIndex + direction), 
                commandHistory.size() - 1);
        
        try {
            // Replace current command with history entry
            int lineStart = getDocument().getDefaultRootElement()
                    .getElement(getDocument().getDefaultRootElement().getElementCount() - 1)
                    .getStartOffset();
            int promptLength = commandProcessor.getPrompt().length();
            getDocument().remove(lineStart + promptLength, 
                    getDocument().getLength() - (lineStart + promptLength));
            
            if (historyIndex >= 0) {
                getDocument().insertString(getDocument().getLength(), 
                        commandHistory.get(historyIndex), null);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void autoComplete() {
        String command = getCurrentCommand();
        // TODO: Implement command auto-completion
    }

    private boolean isEditable(int offset) {
        return offset >= inputStart;
    }

    private void highlightCommandSyntax() {
        SwingUtilities.invokeLater(() -> {
            try {
                String command = getCurrentCommand();
                String[] parts = command.split("\\s+");
                if (parts.length == 0) return;

                // Highlight command name
                StyleConstants.setForeground(styleContext.getStyle(StyleContext.DEFAULT_STYLE), 
                        new Color(255, 200, 100));
                

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void setCaretPosition(int pos) {
        try {
            super.setCaretPosition(pos);
        } catch (IllegalArgumentException e) {
            super.setCaretPosition(getDocument().getLength());
        }
    }

    public void appendText(String text, Color color) {
        if (terminalHandler != null && !(terminalHandler instanceof InternalConsoleHandler)) {
            // If using an external terminal, we might not need to append text
            terminalHandler.appendText(text);
            return;
        }
        
        try {
            getDocument().insertString(getDocument().getLength(), text,
                    styleContext.addAttribute(SimpleAttributeSet.EMPTY, 
                            StyleConstants.Foreground, color));
            setCaretPosition(getDocument().getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        if (terminalHandler != null) {
            terminalHandler.clear();
        }
        
        try {
            getDocument().remove(0, getDocument().getLength());
            inputStart = 0;
            showPrompt();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void setPrompt(String prompt) {
        if (terminalHandler != null) {
            terminalHandler.setPrompt(prompt);
        }
        
        try {
            // Clear the current line
            int lineStart = getDocument().getDefaultRootElement()
                    .getElement(getDocument().getDefaultRootElement().getElementCount() - 1)
                    .getStartOffset();
            getDocument().remove(lineStart, getDocument().getLength() - lineStart);
            
            // Add new prompt
            appendText(prompt, PROMPT_COLOR);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
