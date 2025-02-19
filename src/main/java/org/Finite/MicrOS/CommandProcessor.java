package org.Finite.MicrOS;

import java.util.HashMap;
import java.util.Map;

public class CommandProcessor {

    private final Console console;
    private final Map<String, Command> commands;

    public CommandProcessor(Console console) {
        this.console = console;
        this.commands = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        // Register basic commands
        commands.put("help", this::helpCommand);
        commands.put("clear", this::clearCommand);
        commands.put("echo", this::echoCommand);
        commands.put("exit", this::exitCommand);
    }

    public void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        Command cmd = commands.get(command);
        if (cmd != null) {
            cmd.execute(args);
        } else {
            console.appendText("Unknown command: " + command + "\n");
            console.appendText("Type 'help' for available commands\n");
        }
    }

    private void helpCommand(String args) {
        console.appendText("Available commands:\n");
        console.appendText("  help  - Show this help message\n");
        console.appendText("  clear - Clear the console\n");
        console.appendText("  echo  - Echo the arguments\n");
        console.appendText("  exit  - Exit the application\n");
    }

    private void clearCommand(String args) {
        console.setText("");
        console.appendText("$ ");
    }

    private void echoCommand(String args) {
        console.appendText(args + "\n");
    }

    private void exitCommand(String args) {
        System.exit(0);
    }

    @FunctionalInterface
    interface Command {
        void execute(String args);
    }
}
