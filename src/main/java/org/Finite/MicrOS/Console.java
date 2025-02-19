package org.Finite.MicrOS;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.text.*;

public class Console extends JTextPane {

    private final String prompt = "$ ";
    private final List<String> commandHistory = new ArrayList<>();
    private int commandHistoryIndex = 0;
    private final CommandProcessor commandProcessor;
    private final StyleContext styleContext;
    private final DefaultStyledDocument doc;

    public Console() {
        styleContext = StyleContext.getDefaultStyleContext();
        doc = new DefaultStyledDocument();
        setDocument(doc);

        setBackground(Color.BLACK);
        setForeground(Color.GREEN);
        setCaretColor(Color.GREEN);
        setFont(new Font("Monospace", Font.PLAIN, 12));

        commandProcessor = new CommandProcessor(this);

        // Initialize with prompt
        SwingUtilities.invokeLater(() -> {
            appendText(prompt, Color.GREEN);
            setCaretPosition(getDocument().getLength());
        });

        // Add key listeners
        addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    handleKeyPress(e);
                }
            }
        );
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            e.consume(); // Prevent default enter behavior
            processCommand();
        } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            // Prevent backspace before prompt
            if (getCaretPosition() <= getPromptPosition()) {
                e.consume();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            // Navigate command history
            e.consume();
            if (commandHistoryIndex > 0) {
                commandHistoryIndex--;
                showHistoryCommand();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            // Navigate command history
            e.consume();
            if (commandHistoryIndex < commandHistory.size()) {
                commandHistoryIndex++;
                showHistoryCommand();
            }
        }
    }

    private void processCommand() {
        String command = getCurrentCommand().trim();
        if (!command.isEmpty()) {
            commandHistory.add(command);
            commandHistoryIndex = commandHistory.size();

            appendText("\n", Color.GREEN);
            commandProcessor.processCommand(command);
            appendText("\n" + prompt, Color.GREEN);
        } else {
            appendText("\n" + prompt, Color.GREEN);
        }
    }

    private String getCurrentCommand() {
        try {
            int promptPosition = getPromptPosition();
            return getText(
                promptPosition,
                getDocument().getLength() - promptPosition
            );
        } catch (BadLocationException e) {
            e.printStackTrace();
            return "";
        }
    }

    private int getPromptPosition() {
        return (
            getDocument().getLength() -
            getCurrentLine().length() +
            prompt.length()
        );
    }

    private String getCurrentLine() {
        try {
            int lineStart = getDocument().getLength() - 1;
            while (lineStart >= 0) {
                String text = getText(lineStart, 1);
                if (text.equals("\n")) {
                    break;
                }
                lineStart--;
            }
            return getText(
                lineStart + 1,
                getDocument().getLength() - (lineStart + 1)
            );
        } catch (BadLocationException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void showHistoryCommand() {
        try {
            // Replace current command with history command
            int promptPosition = getPromptPosition();
            getDocument()
                .remove(
                    promptPosition,
                    getDocument().getLength() - promptPosition
                );

            String historyCommand = commandHistoryIndex < commandHistory.size()
                ? commandHistory.get(commandHistoryIndex)
                : "";
            appendText(historyCommand, Color.GREEN);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void appendText(String text, Color color) {
        AttributeSet attrs = styleContext.addAttribute(
            SimpleAttributeSet.EMPTY,
            StyleConstants.Foreground,
            color
        );

        try {
            doc.insertString(doc.getLength(), text, attrs);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void appendText(String text) {
        appendText(text, Color.GREEN);
    }
}
