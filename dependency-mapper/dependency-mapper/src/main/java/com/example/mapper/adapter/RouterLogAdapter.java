package com.example.mapper.adapter;

import com.example.mapper.adapter.builders.RouterLogBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RouterLogAdapter implements DataSourceAdapter<RouterLog> {
    private final Path logFile;
    private final RouterLogBuilder builder = new RouterLogBuilder();

    public RouterLogAdapter(Path logFile) {
        this.logFile = logFile;
    }

    @Override
    public RouterLog fetch() throws IOException {
        if (logFile == null || !Files.exists(logFile)) {
            return null;
        }
        String raw = Files.readString(logFile);
        return builder.parse(raw).build();
    }
}
