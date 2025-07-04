package com.enterprise.dependency.adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter that parses Apache/Nginx style access logs and converts them into
 * {@link Claim} objects.
 */
public class RouterLogAdapter {

    // example: 10.0.0.1 - - [01/Jan/2024:12:00:00 +0000] "GET / HTTP/1.1" 200 123 192.168.1.10:80
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\S+) - - \\[([^\\]]+)\\] \"\\S+ \\S+ [^\"]+\" (\\d{3}) \\d+(?: (\\S+))?"
    );

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z").withZone(ZoneOffset.UTC);

    /**
     * Parse all log lines from a {@link Reader} into a list of {@link Claim} objects.
     */
    public List<Claim> parse(Reader reader) throws IOException {
        List<Claim> claims = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                Claim c = parseLine(line);
                if (c != null) {
                    claims.add(c);
                }
            }
        }
        return claims;
    }

    /**
     * Parse a single log line into a {@link Claim}. Returns {@code null} if the
     * line does not match the expected format or contains invalid data.
     */
    public Claim parseLine(String line) {
        Matcher m = LOG_PATTERN.matcher(line);
        if (!m.find()) {
            return null;
        }
        String srcIp = m.group(1);
        String timestampStr = m.group(2);
        String statusStr = m.group(3);
        String target = m.group(4);

        if (!validIp(srcIp)) {
            return null;
        }
        String targetIp = target != null ? stripPort(target) : null;
        if (targetIp != null && !validIp(targetIp)) {
            return null;
        }
        Instant ts;
        try {
            ts = Instant.from(DATE_FORMAT.parse(timestampStr));
        } catch (Exception e) {
            return null;
        }

        Claim claim = new Claim();
        claim.setTimestamp(ts);
        claim.setSourceIp(srcIp);
        claim.setTargetIp(targetIp);
        int status = Integer.parseInt(statusStr);
        claim.setConfidence(status >= 200 && status < 400 ? 1.0 : 0.5);
        return claim;
    }

    private String stripPort(String ip) {
        int idx = ip.indexOf(':');
        return idx >= 0 ? ip.substring(0, idx) : ip;
    }

    private boolean validIp(String ip) {
        return ip.matches("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)(\\.(?!$)|$)){4}$");
    }
}
