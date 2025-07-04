package com.example.mapper.adapter.builders;

import com.example.mapper.adapter.RouterLog;
import java.util.Arrays;

public class RouterLogBuilder {
    private RouterLog log = new RouterLog();

    public RouterLogBuilder parse(String raw) {
        String[] parts = raw.trim().split("\\|");
        if (parts.length > 0) log.setTimestamp(parts[0]);
        if (parts.length > 1) log.setLevel(parts[1]);
        if (parts.length > 2) log.setMessage(String.join("|", Arrays.copyOfRange(parts, 2, parts.length)));
        return this;
    }

    public RouterLog build() {
        RouterLog result = log;
        log = new RouterLog();
        return result;
    }
}
