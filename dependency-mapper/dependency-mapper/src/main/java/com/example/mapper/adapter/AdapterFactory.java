package com.example.mapper.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AdapterFactory {
    private final Map<String, Supplier<DataSourceAdapter<?>>> registry = new HashMap<>();

    public void registerAdapter(String name, Supplier<DataSourceAdapter<?>> supplier) {
        registry.put(name, supplier);
    }

    public DataSourceAdapter<?> createAdapter(String name) {
        Supplier<DataSourceAdapter<?>> supplier = registry.get(name);
        if (supplier == null) {
            throw new IllegalArgumentException("Adapter " + name + " not registered");
        }
        return supplier.get();
    }
}
