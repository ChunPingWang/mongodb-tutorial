package com.mongodb.course.m20.infrastructure.saga;

import java.util.HashMap;
import java.util.Map;

public final class SagaContext {

    private final Map<String, Object> data;

    public SagaContext() {
        this.data = new HashMap<>();
    }

    public SagaContext(Map<String, Object> initialData) {
        this.data = new HashMap<>(initialData);
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (type == Long.class && value instanceof Integer intVal) {
            return (T) Long.valueOf(intVal.longValue());
        }
        return type.cast(value);
    }

    public Map<String, Object> toMap() {
        return Map.copyOf(data);
    }
}
