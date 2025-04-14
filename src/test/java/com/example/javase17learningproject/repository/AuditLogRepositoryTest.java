package com.example.javase17learningproject.repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.model.audit.SecurityAuditEvent;
import com.example.javase17learningproject.model.audit.UserAuditEvent;

/**
 * AuditLogRepositoryのテストクラス。
 */
@DataJpaTest
@Sql("/cleanup.sql")
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        baseTime = LocalDateTime.now();

        // テストデータの準備
        auditLogRepository.saveAll(List.of(
            new AuditLog(null, UserAuditEvent.USER_CREATED.getType(), 
                Severity.MEDIUM, 1L, 2L, "ユーザー作成", baseTime),
            new AuditLog(null, UserAuditEvent.USER_DELETED.getType(), 
                Severity.HIGH, 1L, 2L, "ユーザー削除", baseTime.plusMinutes(1)),
            new AuditLog(null, SecurityAuditEvent.LOGIN_FAILED.getType(), 
                Severity.HIGH, 1L, null, "ログイン失敗", baseTime.plusMinutes(2)),
            new AuditLog(null, SecurityAuditEvent.LOGIN_SUCCESS.getType(), 
                Severity.LOW, 1L, null, "ログイン成功", baseTime.plusMinutes(3)),
            new AuditLog(null, "SYSTEM_EVENT", 
                Severity.CRITICAL, null, null, "システムエラー", baseTime.plusMinutes(4))
        ));
    }

    @Test
    void testFindByEventType() {
        // When
        List<AuditLog> logs = auditLogRepository.findByEventType(UserAuditEvent.USER_CREATED.getType());

        // Then
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).eventType()).isEqualTo(UserAuditEvent.USER_CREATED.getType());
        assertThat(logs.get(0).severity()).isEqualTo(Severity.MEDIUM);
    }

    @Test
    void testFindByUserId() {
        // When
        List<AuditLog> logs = auditLogRepository.findByUserId(1L);

        // Then
        assertThat(logs).hasSize(4);
        assertThat(logs).allMatch(log -> log.userId() != null && log.userId().equals(1L));
    }

    @Test
    void testFindBySeverityGreaterThanEqual() {
        // When
        List<AuditLog> logs = auditLogRepository.findBySeverityGreaterThanEqual(Severity.HIGH);

        // Then
        assertThat(logs).hasSize(3);
        assertThat(logs).allMatch(log -> 
            log.severity().compareTo(Severity.HIGH) >= 0
        );
    }

    @Test
    void testFindByDateRange() {
        // Given
        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(5);

        // When
        List<AuditLog> logs = auditLogRepository.findByDateRange(start, end);

        // Then
        assertThat(logs).hasSize(5);
        assertThat(logs).allMatch(log -> 
            !log.createdAt().isBefore(start) && !log.createdAt().isAfter(end)
        );
    }

    @Test
    void testFindLatestLogs() {
        // When
        List<AuditLog> logs = auditLogRepository.findLatestLogs(3);

        // Then
        assertThat(logs).hasSize(3);
        // 作成日時の降順で取得されていることを確認
        assertThat(logs).isSortedAccordingTo(
            (a, b) -> b.createdAt().compareTo(a.createdAt())
        );
    }

    @Test
    void testFindByTargetId() {
        // When
        List<AuditLog> logs = auditLogRepository.findByTargetId(2L);

        // Then
        assertThat(logs).hasSize(2);
        assertThat(logs).allMatch(log -> log.targetId() != null && log.targetId().equals(2L));
    }

    @Test
    void testFindBySeverityAndDateRange() {
        // Given
        LocalDateTime start = baseTime.minusMinutes(1);
        LocalDateTime end = baseTime.plusMinutes(5);

        // When
        List<AuditLog> logs = auditLogRepository.findBySeverityAndDateRange(
            Severity.HIGH, start, end);

        // Then
        assertThat(logs).hasSize(3);
        assertThat(logs).allMatch(log -> 
            log.severity().compareTo(Severity.HIGH) >= 0 &&
            !log.createdAt().isBefore(start) && 
            !log.createdAt().isAfter(end)
        );
    }

    @Test
    void testFindByEventTypeWhenNoMatches() {
        // When
        List<AuditLog> logs = auditLogRepository.findByEventType("NON_EXISTENT_EVENT");

        // Then
        assertThat(logs).isEmpty();
    }

    @Test
    void testFindByDateRangeWhenNoMatches() {
        // Given
        LocalDateTime start = baseTime.plusDays(1);
        LocalDateTime end = baseTime.plusDays(2);

        // When
        List<AuditLog> logs = auditLogRepository.findByDateRange(start, end);

        // Then
        assertThat(logs).isEmpty();
    }
}