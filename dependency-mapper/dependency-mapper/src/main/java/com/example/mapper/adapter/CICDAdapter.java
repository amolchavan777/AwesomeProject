package com.example.mapper.adapter;

import java.util.HashMap;
import java.util.Map;

public class CICDAdapter implements DataSourceAdapter<Map<String, String>> {
    private final Map<String, String> env;

    public CICDAdapter(Map<String, String> env) {
        this.env = env;
    }

    @Override
    public Map<String, String> fetch() {
        String[] keys = {"CI", "GITHUB_ACTIONS", "TRAVIS"};
        Map<String, String> data = new HashMap<>();
        for (String k : keys) {
            if (env.containsKey(k)) {
                data.put(k, env.get(k));
            }
        }
        return data;
    }
}
