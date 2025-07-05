package com.enterprise.dependency.adapter;

import com.enterprise.dependency.model.core.Claim;
import com.enterprise.dependency.model.core.ConfidenceScore;
import com.enterprise.dependency.model.core.DependencyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter for parsing user-provided custom dependency data in a simple text format.
 * Each line: fromService -> toService [confidence] [source] [timestamp]
 * Example:
 *   web-portal -> user-service 0.9 manual 2024-07-05T10:30:00Z
 *   user-service -> db 0.8 scan 2024-07-05T10:31:00Z
 */
@Component
public class CustomTextAdapter {
    private static final Logger log = LoggerFactory.getLogger(CustomTextAdapter.class);
    private static final Pattern LINE_PATTERN = Pattern.compile(
        "^([\\w\\-]+)\\s*->\\s*([\\w\\-]+)(?:\\s+([0-9.]+))?(?:\\s+(\\w+))?(?:\\s+([0-9T:Z\\-]+))?.*$"
    );

    public List<Claim> parseCustomText(String data) {
        List<Claim> claims = new ArrayList<>();
        if (data == null || data.trim().isEmpty()) return claims;
        String[] lines = data.split("\n");
        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            Matcher m = LINE_PATTERN.matcher(line);
            if (!m.matches()) {
                log.warn("CustomTextAdapter: Skipping malformed line {}: {}", lineNum, line);
                continue;
            }
            String from = m.group(1);
            String to = m.group(2);
            double confidence = m.group(3) != null ? Double.parseDouble(m.group(3)) : 0.8;
            String source = m.group(4) != null ? m.group(4) : "custom-text";
            Instant timestamp = m.group(5) != null ? Instant.parse(m.group(5)) : Instant.now();
            Claim claim = Claim.builder()
                .fromApplication(from)
                .toApplication(to)
                .dependencyType(DependencyType.RUNTIME)
                .confidence(ConfidenceScore.fromValue(confidence))
                .source(source)
                .timestamp(timestamp)
                .rawData(line)
                .build();
            claims.add(claim);
        }
        log.info("CustomTextAdapter: Parsed {} claims from custom text", claims.size());
        return claims;
    }
}
