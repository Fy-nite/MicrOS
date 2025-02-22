package org.Finite.MicrOS.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MessageBus {
    private static final MessageBus instance = new MessageBus();
    private final Map<String, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();
    
    public static void send(String targetId, Object message) {
        instance.subscribers
            .getOrDefault(targetId, Collections.emptyList())
            .forEach(handler -> handler.accept(message));
    }
    
    public static void subscribe(String messageType, Consumer<Object> handler) {
        instance.subscribers
            .computeIfAbsent(messageType, k -> new ArrayList<>())
            .add(handler);
    }
    
    public static void unsubscribe(String messageType, Consumer<Object> handler) {
        instance.subscribers.computeIfPresent(messageType, (k, handlers) -> {
            handlers.remove(handler);
            return handlers.isEmpty() ? null : handlers;
        });
    }
}
