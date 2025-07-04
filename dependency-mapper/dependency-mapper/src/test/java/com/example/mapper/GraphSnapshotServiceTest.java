package com.example.mapper;

import com.example.mapper.service.GraphSnapshotService;
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
public class GraphSnapshotServiceTest {

    @Autowired
    private LogIngestionService ingestionService;

    @Autowired
    private GraphSnapshotService snapshotService;

    @Autowired
    private DependencyResolver resolver;

    @Test
    void testExportSnapshot() throws IOException {
        Path temp = Files.createTempFile("log", ".txt");
        try (FileWriter w = new FileWriter(temp.toFile())) {
            w.write("ServiceA->ServiceB\n");
        }
        ingestionService.ingestLog(temp.toString(), "test", 1.0);
        Path file = snapshotService.exportSnapshot();
        assertTrue(Files.exists(file));
        // call again to ensure retention logic (should keep at most 3)
        snapshotService.exportSnapshot();
        snapshotService.exportSnapshot();
        snapshotService.exportSnapshot();
        long count = Files.list(file.getParent()).filter(p -> p.toString().endsWith(".graphml")).count();
        assertTrue(count <= 3);
    }
}
