package com.example.mapper.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class ApiGatewayAdapter implements DataSourceAdapter<Map<String, Object>> {
    private final String endpoint;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiGatewayAdapter(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Map<String, Object> fetch() throws IOException, InterruptedException {
        if (endpoint == null || endpoint.isEmpty()) {
            return Map.of();
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint)).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            return Map.of();
        }
        return mapper.readValue(res.body(), Map.class);
    }
}
