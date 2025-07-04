package com.example.mapper.adapter;

import java.util.List;

public class TelemetryAdapter implements DataSourceAdapter<List<String>> {
    private final List<String> metrics;

    public TelemetryAdapter(List<String> metrics) {
        this.metrics = metrics;
    }

    @Override
    public List<String> fetch() {
        return metrics;
    }
}
