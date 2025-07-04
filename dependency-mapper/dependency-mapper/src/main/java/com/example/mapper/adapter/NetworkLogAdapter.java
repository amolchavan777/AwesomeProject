package com.example.mapper.adapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NetworkLogAdapter implements DataSourceAdapter<String> {
    private final Path logFile;

    public NetworkLogAdapter(Path logFile) {
        this.logFile = logFile;
    }

    @Override
    public String fetch() throws IOException {
        if (logFile == null || !Files.exists(logFile)) {
            return null;
        }
        return Files.readString(logFile);
    }
}
