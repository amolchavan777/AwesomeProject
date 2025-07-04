package com.example.mapper;

import com.example.mapper.service.LogIngestionService;
import com.example.mapper.service.DependencyResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LogIngestionServiceTest {

    @Autowired
    private LogIngestionService ingestionService;

    @Autowired
    private DependencyResolver resolver;

    @Test
    void testIngestAndResolve() throws IOException {
        Path temp = Files.createTempFile("log", ".txt");
        try (FileWriter w = new FileWriter(temp.toFile())) {
            w.write("ServiceA->ServiceB\n");
            w.write("ServiceA->ServiceC\n");
            w.write("ServiceA->ServiceB\n");
        }
        ingestionService.ingestLog(temp.toString(), "test", 0.8);
        assertEquals(2, resolver.toList().size());
    }
}
