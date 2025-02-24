# Registry.java

The `Registry` class provides a centralized registry for storing and retrieving key-value pairs. It supports watching for changes to specific keys and notifying registered listeners.

## Key Methods

- `put(String key, Object value)`: Puts a key-value pair into the registry and notifies any registered listeners.
- `get(String key)`: Gets the value associated with a key from the registry.
- `watch(String key, Consumer<Object> onChange)`: Registers a listener to be notified when the value associated with a key changes.

## Usage

The `Registry` class is used internally by the MicrOS system to manage key-value pairs. It is not typically used directly by applications.
