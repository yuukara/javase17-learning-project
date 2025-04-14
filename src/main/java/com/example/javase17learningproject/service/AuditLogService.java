package com.example.javase17learningproject.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

/**
 * 監査ログのビジネスロジックを提供するサービス。
 */
public interface AuditLogService {

    /**
     * 監査ログを保存します。
     * メモリキャッシュとデータベースの両方に保存されます。
     */
    AuditLog save(AuditLog log);

    /**
     * 指定されたIDの監査ログを取得します。
     * まずメモリキャッシュを確認し、存在しない場合はデータベースから取得します。
     */
    AuditLog findById(Long id);

    /**
     * 指定された条件で監査ログを検索します。
     */
    Page<AuditLog> searchLogs(
        String eventType,
        Severity severity,
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * 最近の監査ログを取得します。
     * メモリキャッシュから直接取得するため、高速です。
     */
    List<AuditLog> findLatestLogs(int limit);

    /**
     * 指定された日付より古い監査ログをアーカイブし、
     * データベースから削除します。
     */
    void archiveOldLogs(LocalDateTime before);

    /**
     * インメモリストレージのログをデータベースに移行します。
     * 移行完了後、インメモリストレージはクリアされます。
     */
    void migrateToDatabase();

    /**
     * メモリキャッシュをクリアし、最新のデータで再構築します。
     */
    void refreshCache();

    /**
     * 指定された期間の監査ログを集計します。
     * @return イベントタイプごとの件数
     */
    Map<String, Long> aggregateByEventType(LocalDateTime start, LocalDateTime end);

    /**
     * 重要度レベルごとの監査ログ件数を取得します。
     */
    Map<Severity, Long> countBySeverity(LocalDateTime start, LocalDateTime end);
}