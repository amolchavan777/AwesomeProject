package com.enterprise.dependency.adapter;

import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RouterLogAdapterTest {

    @Test
    void parseValidLine() {
        String line = "10.0.0.1 - - [01/Jan/2024:12:00:00 +0000] \"GET /index.html HTTP/1.1\" 200 123 192.168.1.10:80";
        RouterLogAdapter adapter = new RouterLogAdapter();
        Claim claim = adapter.parseLine(line);
        assertNotNull(claim);
        assertEquals("10.0.0.1", claim.getSourceIp());
        assertEquals("192.168.1.10", claim.getTargetIp());
        assertEquals(1.0, claim.getConfidence(), 0.001);
    }

    @Test
    void parseInvalidIp() {
        String line = "badip - - [01/Jan/2024:12:00:00 +0000] \"GET /index.html HTTP/1.1\" 200 123 192.168.1.10:80";
        RouterLogAdapter adapter = new RouterLogAdapter();
        assertNull(adapter.parseLine(line));
    }

    @Test
    void parseMultipleLines() throws Exception {
        String logs = String.join("\n",
            "10.0.0.1 - - [01/Jan/2024:12:00:00 +0000] \"GET / HTTP/1.1\" 200 123 192.168.1.10:80",
            "10.0.0.2 - - [01/Jan/2024:12:00:01 +0000] \"GET /foo HTTP/1.1\" 500 99 192.168.1.11:80"
        );
        RouterLogAdapter adapter = new RouterLogAdapter();
        List<Claim> claims = adapter.parse(new StringReader(logs));
        assertEquals(2, claims.size());
        assertEquals(1.0, claims.get(0).getConfidence(), 0.001);
        assertEquals(0.5, claims.get(1).getConfidence(), 0.001);
    }
}
