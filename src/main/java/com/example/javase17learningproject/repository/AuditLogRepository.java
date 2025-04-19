package com.example.javase17learningproject.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.audit.AuditEvent;

import jakarta.persistence.QueryHint;

/**
 * 監査ログのデータベースアクセスを担当するリポジトリ.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * イベントタイプで監査ログを検索します.
     *
     * @param eventType イベントタイプ
     * @return 該当する監査ログのリスト
     */
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "audit.log.byEventType")
    })
    List<AuditLogEntity> findByEventType(String eventType);

    /**
     * 指定されたユーザーの監査ログを検索します.
     *
     * @param userId ユーザーID
     * @return 該当する監査ログのリスト
     */
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "audit.log.byUserId")
    })
    List<AuditLogEntity> findByUserId(Long userId);

    /**
     * 指定された期間の監査ログを検索します.
     *
     * @param start 開始日時
     * @param end 終了日時
     * @return 該当する監査ログのリスト
     */
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "audit.log.byDateRange")
    })
    List<AuditLogEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 指定された重要度と期間の監査ログを検索します.
     *
     * @param severity 重要度
     * @param start 開始日時
     * @param end 終了日時
     * @return 該当する監査ログのリスト
     */
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "audit.log.bySeverityAndDate")
    })
    List<AuditLogEntity> findBySeverityAndCreatedAtBetween(
            AuditEvent.Severity severity,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 指定された期間の監査ログをページネーション付きで取得します.
     *
     * @param start 開始日時
     * @param end 終了日時
     * @param pageable ページネーション情報
     * @return 該当する監査ログのページ
     */
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "audit.log.byDateRangePaged")
    })
    Page<AuditLogEntity> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    /**
     * 指定された条件で監査ログを検索します.
     * パラメータがnullの場合、その条件は無視されます.
     *
     * @param eventType イベントタイプ
     * @param severity 重要度
     * @param userId ユーザーID
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @param pageable ページネーション情報
     * @return 該当する監査ログのページ
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
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "audit.log.search")
    })
    Page<AuditLogEntity> searchLogs(
            @Param("eventType") String eventType,
            @Param("severity") AuditEvent.Severity severity,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 指定された日付より古い監査ログを削除します.
     *
     * @param date 基準日時
     */
    void deleteByCreatedAtBefore(LocalDateTime date);

    /**
     * 指定された日付より古い監査ログの件数を取得します.
     *
     * @param date 基準日時
     * @return 該当する監査ログの件数
     */
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "audit.log.count")
    })
    long countByCreatedAtBefore(LocalDateTime date);
}