package com.example.javase17learningproject.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

/**
 * 監査ログのリポジトリインターフェース。
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 指定されたイベントタイプの監査ログを検索します。
     */
    List<AuditLog> findByEventType(String eventType);

    /**
     * 指定されたユーザーの監査ログを検索します。
     */
    List<AuditLog> findByUserId(Long userId);

    /**
     * 指定された重要度以上の監査ログを検索します。
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.severity >= :minSeverity 
        ORDER BY a.createdAt DESC
        """)
    List<AuditLog> findBySeverityGreaterThanEqual(@Param("minSeverity") Severity minSeverity);

    /**
     * 指定された期間の監査ログを検索します。
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.createdAt BETWEEN :startDateTime AND :endDateTime 
        ORDER BY a.createdAt DESC
        """)
    List<AuditLog> findByDateRange(
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * 最新の監査ログを指定された件数取得します。
     */
    @Query("""
        SELECT a FROM AuditLog a 
        ORDER BY a.createdAt DESC 
        LIMIT :limit
        """)
    List<AuditLog> findLatestLogs(@Param("limit") int limit);

    /**
     * 指定された対象IDに関する監査ログを検索します。
     */
    List<AuditLog> findByTargetId(Long targetId);

    /**
     * 指定された重要度と期間の監査ログを検索します。
     */
    @Query("""
        SELECT a FROM AuditLog a 
        WHERE a.severity >= :minSeverity 
        AND a.createdAt BETWEEN :startDateTime AND :endDateTime 
        ORDER BY a.createdAt DESC
        """)
    List<AuditLog> findBySeverityAndDateRange(
        @Param("minSeverity") Severity minSeverity,
        @Param("startDateTime") LocalDateTime startDateTime,
        @Param("endDateTime") LocalDateTime endDateTime
    );
}