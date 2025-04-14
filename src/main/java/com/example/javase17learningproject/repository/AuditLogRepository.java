package com.example.javase17learningproject.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.javase17learningproject.model.AuditLogEntity;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

/**
 * 監査ログのデータベースアクセスを担当するリポジトリ。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * イベントタイプで監査ログを検索します。
     */
    List<AuditLogEntity> findByEventType(String eventType);

    /**
     * 指定されたユーザーの監査ログを検索します。
     */
    List<AuditLogEntity> findByUserId(Long userId);

    /**
     * 指定された期間の監査ログを検索します。
     */
    List<AuditLogEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 指定された重要度と期間の監査ログを検索します。
     */
    List<AuditLogEntity> findBySeverityAndCreatedAtBetween(
        Severity severity,
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * 指定された期間の監査ログをページネーション付きで取得します。
     */
    Page<AuditLogEntity> findByCreatedAtBetween(
        LocalDateTime start,
        LocalDateTime end,
        Pageable pageable
    );

    /**
     * 指定された条件で監査ログを検索します。
     * パラメータがnullの場合、その条件は無視されます。
     */
    @Query("""
        SELECT a FROM AuditLogEntity a
        WHERE (:eventType IS NULL OR a.eventType = :eventType)
        AND (:severity IS NULL OR a.severity = :severity)
        AND (:userId IS NULL OR a.userId = :userId)
        AND (:startDate IS NULL OR a.createdAt >= :startDate)
        AND (:endDate IS NULL OR a.createdAt <= :endDate)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLogEntity> searchLogs(
        @Param("eventType") String eventType,
        @Param("severity") Severity severity,
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * 指定された日付より古い監査ログを削除します。
     */
    void deleteByCreatedAtBefore(LocalDateTime date);

    /**
     * 指定された日付より古い監査ログの件数を取得します。
     */
    long countByCreatedAtBefore(LocalDateTime date);
}