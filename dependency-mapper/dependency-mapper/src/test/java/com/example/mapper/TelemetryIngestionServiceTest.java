package com.example.mapper;

import com.example.mapper.service.TelemetryIngestionService;
import com.example.mapper.service.WeightedConflictResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TelemetryIngestionServiceTest {

    @Autowired
    private TelemetryIngestionService ingestionService;

    @Autowired
    private WeightedConflictResolver resolver;

    @Test
    void ingestTelemetryFile() throws IOException {
        Path temp = Files.createTempFile("telemetry", ".csv");
        try (FileWriter w = new FileWriter(temp.toFile())) {
            w.write("2024-07-04T10:30:45Z,ServiceA,ServiceB,30\n");
            w.write("2024-07-04T10:31:45Z,ServiceA,ServiceB,20\n");
            w.write("bad,line\n");
        }
        ingestionService.ingestTelemetry(temp.toString());
        assertEquals(1, resolver.toList().size());
    }
}
