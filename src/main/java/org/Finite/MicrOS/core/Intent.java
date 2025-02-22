package org.Finite.MicrOS.core;

import java.util.HashMap;
import java.util.Map;

public class Intent {
    private final String targetAppId;
    private final Map<String, Object> extras = new HashMap<>();
    
    public Intent(String targetAppId) {
        this.targetAppId = targetAppId;
    }
    
    public void putExtra(String key, Object value) {
        extras.put(key, value);
    }
    
    public Object getExtra(String key) {
        return extras.get(key);
    }
    
    public String getTargetAppId() {
        return targetAppId;
    }
    
    public Map<String, Object> getExtras() {
        return new HashMap<>(extras);
    }
}
