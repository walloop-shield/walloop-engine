package com.walloop.engine.workflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WorkflowContext {

    private final Map<String, Object> values;

    public WorkflowContext() {
        this.values = new HashMap<>();
    }

    public WorkflowContext(Map<String, Object> initialValues) {
        this.values = new HashMap<>(initialValues == null ? Map.of() : initialValues);
    }

    public Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(values);
    }

    public void put(String key, Object value) {
        values.put(key, value);
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    public <T> T require(String key, Class<T> type) {
        return get(key, type).orElseThrow(() -> new IllegalStateException("Missing or invalid context key: " + key));
    }
}

