# CLI App Development for MicrOS

MicrOS supports creating command-line interface (CLI) applications that can be run from the system console. This guide will walk you through creating a CLI app.

## Getting Started

### 1. Basic App Structure
Create a new MicrOS app as usual, but configure it to be a CLI app in the manifest:

```java
public class MyCliApp extends MicrOSApp {
    @Override
    public void onStart() {
        AppManifest manifest = getManifest();
        manifest.setCLI(true);
        manifest.setCLICommand("mycmd");
        manifest.setCLIAliases(new String[]{"mc", "mycmd2"}); // Optional aliases
    }
    
    @Override
    public void handleIntent(Intent intent) {
        if (intent.hasExtra("cli")) {
            // Get CLI arguments
            String[] args = intent.getStringArrayExtra("args");
            String consoleId = intent.getStringExtra("consoleId");
            
            // Process the command
            handleCommand(args);
        }
    }
}
```

### 2. Registering Commands
- Set `setCLI(true)` to mark your app as a CLI app
- Use `setCLICommand()` to set the main command name
- Optionally set command aliases with `setCLIAliases()`

### 3. Handling Command Arguments
When your CLI app is invoked, it receives:
- `args`: String array of command-line arguments
- `consoleId`: ID of the console that invoked the command

### 4. Interacting with the Console
You can use the MessageBus to send output back to the console:

```java
private void sendOutput(String consoleId, String text) {
    MessageBus.send(consoleId, text);
}

private void sendError(String consoleId, String error) {
    MessageBus.send(consoleId, "\u001B[31m" + error + "\u001B[0m"); // Red text
}
```

## Example CLI App

Here's a complete example of a simple "echo" command:

```java
public class EchoApp extends MicrOSApp {
    @Override
    public void onStart() {
        AppManifest manifest = getManifest();
        manifest.setCLI(true);
        manifest.setCLICommand("echo");
    }
    
    @Override
    public void handleIntent(Intent intent) {
        if (intent.hasExtra("cli")) {
            String[] args = intent.getStringArrayExtra("args");
            String consoleId = intent.getStringExtra("consoleId");
            
            // Join all arguments with spaces
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            MessageBus.send(consoleId, message + "\n");
        }
    }
}
```

## Best Practices

1. **Command Names**
   - Use lowercase for command names
   - Keep names short and memorable
   - Use hyphens for multi-word commands (e.g., `file-search`)

2. **Error Handling**
   - Always provide feedback for errors
   - Use color coding (red for errors, yellow for warnings)
   - Include helpful usage messages

3. **Help Documentation**
   - Implement --help flag support
   - Document all command options
   - Provide usage examples

4. **Exit Codes**
   - Return appropriate exit status via MessageBus
   - 0 for success, non-zero for errors

## Available System Commands

Your CLI app can interact with these built-in commands:
- `ls [path]` - List files in directory
- `cd <path>` - Change directory
- `pwd` - Print working directory
- `cat <file>` - Display file contents
- `mkdir <dir>` - Create directory
- `touch <file>` - Create empty file
- `rm <file>` - Delete file
- `clear` - Clear screen
- `help` - Show help
- `run <file>` - Execute file with shebang