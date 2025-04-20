package com.example.javase17learningproject.archive.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;

/**
 * アーカイブファイルの検索ユーティリティクラス.
 * 日次および月次アーカイブファイルからの監査ログの検索機能を提供します.
 */
public class ArchiveSearchUtils {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveSearchUtils.class);

    /**
     * 日次アーカイブから条件に一致する監査ログを検索します.
     *
     * @param archivePath 日次アーカイブファイルのパス
     * @param start 検索開始日時
     * @param end 検索終了日時
     * @param eventType イベントタイプ（nullの場合は条件に含めない）
     * @param severity 重要度（nullの場合は条件に含めない）
     * @return 条件に一致する監査ログのリスト
     * @throws IOException アーカイブファイルの読み込みに失敗した場合
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
     * 月次アーカイブから条件に一致する監査ログを検索します.
     *
     * @param archivePath 月次アーカイブファイルのパス
     * @param start 検索開始日時
     * @param end 検索終了日時
     * @param eventType イベントタイプ（nullの場合は条件に含めない）
     * @param severity 重要度（nullの場合は条件に含めない）
     * @return 条件に一致する監査ログのリスト
     * @throws IOException アーカイブファイルの読み込みに失敗した場合
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

    /**
     * 指定された監査ログが検索条件に一致するかを判定します.
     *
     * @param log 判定対象の監査ログデータ
     * @param start 検索開始日時
     * @param end 検索終了日時
     * @param eventType イベントタイプ（nullの場合は条件に含めない）
     * @param severity 重要度（nullの場合は条件に含めない）
     * @return 検索条件に一致する場合はtrue、それ以外はfalse
     */
    private static boolean matchesSearchCriteria(
            Map<String, Object> log,
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity) {
        Object createdAt = log.get("createdAt");
        LocalDateTime logTime;
        if (createdAt instanceof String) {
            logTime = LocalDateTime.parse(((String) createdAt).replace("Z", ""));
        } else {
            return false;
        }

        Object eventTypeObj = log.get("eventType");
        if (!(eventTypeObj instanceof String)) {
            return false;
        }
        String logEventType = (String) eventTypeObj;

        Object severityObj = log.get("severity");
        if (!(severityObj instanceof String)) {
            return false;
        }
        AuditEvent.Severity logSeverity = AuditEvent.Severity.valueOf((String) severityObj);

        return logTime.isAfter(start) &&
               logTime.isBefore(end) &&
               (eventType == null || eventType.equals(logEventType)) &&
               (severity == null || severity == logSeverity);
    }

    /**
     * マップ形式の監査ログデータをAuditLogオブジェクトに変換します.
     *
     * @param log 変換対象の監査ログデータ
     * @return 変換後のAuditLogオブジェクト
     * @throws IllegalArgumentException 必須フィールドが存在しないか、形式が不正な場合
     */
    private static AuditLog convertToAuditLog(Map<String, Object> log) {
        // 必須フィールドのチェックと変換
        Object idObj = log.get("id");
        if (!(idObj instanceof Number)) {
            throw new IllegalArgumentException("Invalid id format");
        }
        Long id = ((Number) idObj).longValue();

        Object eventTypeObj = log.get("eventType");
        if (!(eventTypeObj instanceof String)) {
            throw new IllegalArgumentException("Invalid eventType format");
        }
        String eventType = (String) eventTypeObj;

        Object severityObj = log.get("severity");
        if (!(severityObj instanceof String)) {
            throw new IllegalArgumentException("Invalid severity format");
        }
        AuditEvent.Severity severity = AuditEvent.Severity.valueOf((String) severityObj);

        Object userIdObj = log.get("userId");
        Long userId = userIdObj instanceof Number ? ((Number) userIdObj).longValue() : null;

        Object targetIdObj = log.get("targetId");
        Long targetId = targetIdObj instanceof Number ? ((Number) targetIdObj).longValue() : null;

        Object descriptionObj = log.get("description");
        String description = descriptionObj instanceof String ? (String) descriptionObj : null;

        Object createdAtObj = log.get("createdAt");
        if (!(createdAtObj instanceof String)) {
            throw new IllegalArgumentException("Invalid createdAt format");
        }
        LocalDateTime createdAt = LocalDateTime.parse(((String) createdAtObj).replace("Z", ""));

        return new AuditLog(
            id,
            eventType,
            severity,
            userId,
            targetId,
            description,
            createdAt
        );
    }
}