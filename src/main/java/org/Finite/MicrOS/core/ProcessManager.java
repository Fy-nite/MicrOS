package org.Finite.MicrOS.core;

import java.awt.Color;
import java.io.*;
import java.util.*;
import javax.swing.SwingUtilities;

import org.Finite.MicrOS.ui.Console;

public class ProcessManager {

    private final Console console;
    private final Map<Integer, Process> activeProcesses;
    // misc process manager stuff

    private int nextProcessId = 1;

    // Add new fields for thread management
    private final Map<Integer, Thread> appThreads = new HashMap<>();
    private final Map<Integer, String> threadNames = new HashMap<>();
    private int nextThreadId = 1;

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

    public void killAllProcesses() {
        for (Process process : activeProcesses.values()) {
            process.destroyForcibly();
        }
        killAllAppThreads();
        console.appendText("All processes and threads terminated\n", Color.YELLOW);
    }

    /**
     * Starts a new application thread with the given Runnable and name
     * @param runnable The Runnable to execute
     * @param name Name of the thread/application
     * @return The thread ID
     */
    public int startAppThread(Runnable runnable, String name) {
        int threadId = nextThreadId++;
        Thread thread = new Thread(() -> {
            try {
                console.appendText("[Thread " + threadId + "] Starting: " + name + "\n", Color.YELLOW);
                runnable.run();
                console.appendText("[Thread " + threadId + "] Completed: " + name + "\n", Color.GREEN);
            } catch (Exception e) {
                console.appendText("[Thread " + threadId + "] Error: " + e.getMessage() + "\n", Color.RED);
            } finally {
                appThreads.remove(threadId);
                threadNames.remove(threadId);
            }
        });
        
        appThreads.put(threadId, thread);
        threadNames.put(threadId, name);
        thread.start();
        return threadId;
    }

    /**
     * Kills a specific application thread
     * @param threadId The ID of the thread to kill
     * @return true if thread was killed, false if thread not found
     */
    public boolean killAppThread(int threadId) {
        Thread thread = appThreads.get(threadId);
        if (thread != null) {
            thread.interrupt();
            console.appendText("[Thread " + threadId + "] Terminated: " + threadNames.get(threadId) + "\n", Color.YELLOW);
            appThreads.remove(threadId);
            threadNames.remove(threadId);
            return true;
        }
        console.appendText("No thread found with ID: " + threadId + "\n", Color.RED);
        return false;
    }

    /**
     * Lists all running application threads
     */
    public void listAppThreads() {
        if (appThreads.isEmpty()) {
            console.appendText("No active application threads\n", Color.YELLOW);
        } else {
            console.appendText("Active application threads:\n", Color.CYAN);
            for (Map.Entry<Integer, Thread> entry : appThreads.entrySet()) {
                int threadId = entry.getKey();
                String name = threadNames.get(threadId);
                console.appendText(
                    String.format("TID: %d - %s (%s)\n", 
                        threadId, 
                        name, 
                        entry.getValue().isAlive() ? "Running" : "Stopped"),
                    Color.WHITE
                );
            }
        }
    }

    /**
     * Kills all application threads
     */
    public void killAllAppThreads() {
        for (Thread thread : appThreads.values()) {
            thread.interrupt();
        }
        console.appendText("All application threads terminated\n", Color.YELLOW);
        appThreads.clear();
        threadNames.clear();
    }

    /**
     * Checks if a specific thread ID is still running
     * @param threadId The thread ID to check
     * @return true if thread is running, false otherwise
     */
    public boolean isThreadRunning(int threadId) {
        Thread thread = appThreads.get(threadId);
        return thread != null && thread.isAlive();
    }

    /**
     * Gets the name of a running thread
     * @param threadId The thread ID
     * @return The thread name or null if not found
     */
    public String getThreadName(int threadId) {
        return threadNames.get(threadId);
    }
}
