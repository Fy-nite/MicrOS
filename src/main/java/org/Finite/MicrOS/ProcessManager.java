package org.Finite.MicrOS;

import java.awt.Color;
import java.io.*;
import java.util.*;
import javax.swing.SwingUtilities;

public class ProcessManager {

    private final Console console;
    private final Map<Integer, Process> activeProcesses;
    private int nextProcessId = 1;

    public ProcessManager(Console console) {
        this.console = console;
        this.activeProcesses = new HashMap<>();
    }

    public int startProcess(String command) {
        int processId = nextProcessId++;

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder();

                // Handle different OS shells
                if (
                    System.getProperty("os.name")
                        .toLowerCase()
                        .contains("windows")
                ) {
                    pb.command("cmd.exe", "/c", command);
                } else {
                    pb.command("sh", "-c", command);
                }

                console.appendText(
                    "[" + processId + "] Starting: " + command + "\n",
                    Color.YELLOW
                );

                Process process = pb.start();
                activeProcesses.put(processId, process);

                // Handle process output in separate threads
                startOutputReader(
                    process.getInputStream(),
                    Color.WHITE,
                    processId
                );
                startOutputReader(
                    process.getErrorStream(),
                    Color.RED,
                    processId
                );

                // Wait for process to complete
                int exitCode = process.waitFor();
                activeProcesses.remove(processId);

                console.appendText(
                    "[" +
                    processId +
                    "] Process completed with exit code: " +
                    exitCode +
                    "\n",
                    exitCode == 0 ? Color.GREEN : Color.RED
                );
            } catch (IOException | InterruptedException e) {
                console.appendText(
                    "[" + processId + "] Error: " + e.getMessage() + "\n",
                    Color.RED
                );
            }
        }).start();

        return processId;
    }

    private void startOutputReader(
        InputStream inputStream,
        Color color,
        int processId
    ) {
        new Thread(() -> {
            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream)
                )
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    SwingUtilities.invokeLater(() ->
                        console.appendText(
                            "[" + processId + "] " + output + "\n",
                            color
                        )
                    );
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                    console.appendText(
                        "[" +
                        processId +
                        "] Error reading output: " +
                        e.getMessage() +
                        "\n",
                        Color.RED
                    )
                );
            }
        }).start();
    }

    public void killProcess(int processId) {
        Process process = activeProcesses.get(processId);
        if (process != null) {
            process.destroyForcibly();
            console.appendText(
                "[" + processId + "] Process terminated\n",
                Color.YELLOW
            );
        } else {
            console.appendText(
                "No process found with ID: " + processId + "\n",
                Color.RED
            );
        }
    }

    public void listProcesses() {
        if (activeProcesses.isEmpty()) {
            console.appendText("No active processes\n", Color.YELLOW);
        } else {
            console.appendText("Active processes:\n", Color.CYAN);
            for (Map.Entry<
                Integer,
                Process
            > entry : activeProcesses.entrySet()) {
                console.appendText(
                    "PID: " + entry.getKey() + " (Running)\n",
                    Color.WHITE
                );
            }
        }
    }
}
