# ProcessManager.java

The `ProcessManager` class handles the creation and management of processes within MicrOS. It supports starting, stopping, and listing processes, as well as managing application threads.

## Key Methods

- `ProcessManager(Console console)`: Constructor that initializes the ProcessManager with the given console.
- `startProcess(String command)`: Starts a new process with the specified command.
- `killProcess(int processId)`: Kills a specific process by its ID.
- `listProcesses()`: Lists all active processes.
- `killAllProcesses()`: Kills all active processes.
- `startAppThread(Runnable runnable, String appId)`: Starts a new application thread with the given Runnable and appId.
- `killAppThread(int threadId)`: Kills a specific application thread by its ID.
- `listAppThreads()`: Lists all running application threads.
- `killAllAppThreads()`: Kills all application threads.
- `isThreadRunning(int threadId)`: Checks if a specific thread ID is still running.
- `getThreadName(int threadId)`: Gets the name of a running thread by its ID.
- `cleanup()`: Cleans up all processes and threads.
- `closeout()`: Closes out all processes and threads gracefully.

## Usage

The `ProcessManager` class is used internally by the MicrOS system to manage processes and application threads. It is not typically used directly by applications.
