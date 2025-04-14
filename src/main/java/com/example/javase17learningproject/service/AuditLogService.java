package com.example.javase17learningproject.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.repository.AuditLogRepository;

/**
 * 監査ログのサービスクラス。
 * 監査ログの記録と検索機能を提供します。
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 監査イベントを記録します。
     */
    @Transactional
    public AuditLog logEvent(AuditEvent event, Long userId, Long targetId, String description) {
        return auditLogRepository.save(new AuditLog(
            null,
            event.getType(),
            event.getSeverity(),
            userId,
            targetId,
            description,
            LocalDateTime.now()
        ));
    }

    /**
     * イベントタイプで監査ログを検索します。
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findByEventType(String eventType) {
        return auditLogRepository.findByEventType(eventType);
    }

    /**
     * ユーザーIDで監査ログを検索します。
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findByUserId(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    /**
     * 指定された重要度以上の監査ログを検索します。
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findBySeverityGreaterThanEqual(Severity minSeverity) {
        return auditLogRepository.findBySeverityGreaterThanEqual(minSeverity);
    }

    /**
     * 期間を指定して監査ログを検索します。
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findByDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return auditLogRepository.findByDateRange(startDateTime, endDateTime);
    }

    /**
     * 最新の監査ログを取得します。
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getLatestLogs(int limit) {
        return auditLogRepository.findLatestLogs(limit);
    }

    /**
     * 対象IDに関する監査ログを検索します。
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findByTargetId(Long targetId) {
        return auditLogRepository.findByTargetId(targetId);
    }

    /**
     * 指定された重要度と期間の監査ログを検索します。
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findBySeverityAndDateRange(
            Severity minSeverity, 
            LocalDateTime startDateTime, 
            LocalDateTime endDateTime) {
        return auditLogRepository.findBySeverityAndDateRange(
            minSeverity, startDateTime, endDateTime);
    }

    /**
     * 重大なイベントを検索します（重要度HIGH以上）。
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findCriticalEvents() {
        return auditLogRepository.findBySeverityGreaterThanEqual(Severity.HIGH);
    }

    /**
     * 指定された期間内の重大なイベントを検索します。
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findCriticalEventsInDateRange(
            LocalDateTime startDateTime, 
            LocalDateTime endDateTime) {
        return auditLogRepository.findBySeverityAndDateRange(
            Severity.HIGH, startDateTime, endDateTime);
    }
}