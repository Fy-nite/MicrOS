
```java
// Example usage in an application
ProcessManager pm = ...;

// Start a thread
int threadId = pm.startAppThread(() -> {
    // Your application code here
    while (!Thread.interrupted()) {
        // Do work
        try {
            Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }, "MyApp");
    
    // Check if thread is running
    if (pm.isThreadRunning(threadId)) {
        System.out.println("Thread is still running");
    }
    
    // List all threads
    pm.listAppThreads();
    
    // Kill specific thread
    pm.killAppThread(threadId);
    
    // Kill all threads
    pm.killAllAppThreads();
}
```
