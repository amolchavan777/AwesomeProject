package com.example.mapper.adapter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Adapters {
    private static final AdapterFactory factory = new AdapterFactory();

    static {
        factory.registerAdapter("routerLog", () -> new RouterLogAdapter(null));
        factory.registerAdapter("codebase", () -> new CodebaseAdapter(Path.of(".")));
        factory.registerAdapter("cicd", () -> new CICDAdapter(System.getenv()));
        factory.registerAdapter("apiGateway", () -> new ApiGatewayAdapter(""));
        factory.registerAdapter("telemetry", () -> new TelemetryAdapter(List.of()));
        factory.registerAdapter("networkLog", () -> new NetworkLogAdapter(null));
    }

    public static AdapterFactory getFactory() {
        return factory;
    }
}
