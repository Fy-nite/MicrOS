package org.Finite.MicrOS.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Registry {
    private static final Registry instance = new Registry();
    private final ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Consumer<Object>> watchers = new ConcurrentHashMap<>();
    
    public static void put(String key, Object value) {
        instance.data.put(key, value);
        if (instance.watchers.containsKey(key)) {
            instance.watchers.get(key).accept(value);
        }
    }
    
    public static Object get(String key) {
        return instance.data.get(key);
    }
    
    public static void watch(String key, Consumer<Object> onChange) {
        instance.watchers.put(key, onChange);
    }
}
