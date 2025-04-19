package com.example.javase17learningproject.archive.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;

public class ArchiveSearchUtils {

    /**
     * 日次アーカイブを検索します.
     */
    public static List<AuditLog> searchDailyArchive(
            Path archivePath,
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity
    ) throws IOException {
        byte[] compressed = Files.readAllBytes(archivePath);
        String content = GzipUtils.decompress(compressed);
        Map<String, Object> archiveData = JsonUtils.fromJson(content, Map.class);
        List<Map<String, Object>> logs = (List<Map<String, Object>>) archiveData.get("logs");

        List<AuditLog> results = new ArrayList<>();
        for (Map<String, Object> log : logs) {
            if (matchesSearchCriteria(log, start, end, eventType, severity)) {
                results.add(convertToAuditLog(log));
            }
        }

        return results;
    }

    /**
     * 月次アーカイブを検索します.
     */
    public static List<AuditLog> searchMonthlyArchive(
            Path archivePath,
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity
    ) throws IOException {
        byte[] compressed = Files.readAllBytes(archivePath);
        String content = GzipUtils.decompress(compressed);
        List<Map<String, Object>> logs = JsonUtils.fromJson(content, List.class);

        List<AuditLog> results = new ArrayList<>();
        for (Map<String, Object> log : logs) {
            if (matchesSearchCriteria(log, start, end, eventType, severity)) {
                results.add(convertToAuditLog(log));
            }
        }

        return results;
    }

    private static boolean matchesSearchCriteria(
            Map<String, Object> log,
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity) {
        LocalDateTime logTime = LocalDateTime.parse((String) log.get("createdAt"));
        String logEventType = (String) log.get("eventType");
        AuditEvent.Severity logSeverity = AuditEvent.Severity.valueOf((String) log.get("severity"));

        return logTime.isAfter(start) &&
               logTime.isBefore(end) &&
               (eventType == null || eventType.equals(logEventType)) &&
               (severity == null || severity == logSeverity);
    }

    private static AuditLog convertToAuditLog(Map<String, Object> log) {
        return new AuditLog(
            ((Number) log.get("id")).longValue(),
            (String) log.get("eventType"),
            AuditEvent.Severity.valueOf((String) log.get("severity")),
            ((Number) log.get("userId")).longValue(),
            ((Number) log.get("targetId")).longValue(),
            (String) log.get("description"),
            LocalDateTime.parse((String) log.get("createdAt"))
        );
    }
}