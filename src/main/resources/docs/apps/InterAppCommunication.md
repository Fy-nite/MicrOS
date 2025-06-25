# Inter-App Communication in MicrOS

## Message Passing
Apps can communicate using the system message bus:

```java
// Send message
MessageBus.send("target-app-id", message);

// Receive messages
MessageBus.subscribe("message-type", (message) -> {
    // Handle message
});
```

## Shared Data
Use the system registry for shared data:

```java
// Store data
Registry.put("key", value);

// Retrieve data
Object value = Registry.get("key");
```

## File-Based Communication
Share data through virtual filesystem:

```java
// Write to shared location
vfs.writeFile("/shared/myapp/data.json", jsonData);

// Monitor for changes
vfs.watchFile("/shared/otherapp/status.json", (path) -> {
    // Handle file change
});
```

## Intent System
Launch other apps with data:

```java
// Create intent
Intent intent = new Intent("org.finite.otherapp");
intent.putExtra("key", "value");

// Launch app
windowManager.launchAppWithIntent(intent);
```



some extra things

```java
// Send a message to another app
MessageBus.send("org.finite.otherapp", "Hello from MyApp!");

// Subscribe to messages
MessageBus.subscribe("org.finite.myapp", message -> {
    System.out.println("Received: " + message);
});

// Share data via registry
Registry.put("shared.data", "Some shared value");
Registry.watch("shared.data", value -> {
    System.out.println("Shared data changed: " + value);
});

// Watch file changes
FileWatcher.watchFile("/shared/data.txt", path -> {
    System.out.println("File changed: " + path);
});

// Launch app with intent
Intent intent = new Intent("org.finite.texteditor");
intent.putExtra("file", "/path/to/file.txt");
windowManager.launchAppWithIntent(intent);
```