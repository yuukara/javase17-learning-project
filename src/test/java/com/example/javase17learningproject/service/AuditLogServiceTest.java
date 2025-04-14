package com.example.javase17learningproject.service;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.model.audit.SecurityAuditEvent;
import com.example.javase17learningproject.model.audit.SystemAuditEvent;
import com.example.javase17learningproject.model.audit.UserAuditEvent;
import com.example.javase17learningproject.repository.AuditLogRepository;

/**
 * AuditLogServiceのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now();
    }

    @Test
    void testLogUserEvent() {
        // Given
        Long userId = 1L;
        Long targetId = 2L;
        String description = "ユーザーが作成されました";
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AuditLog log = auditLogService.logEvent(
            UserAuditEvent.USER_CREATED,
            userId,
            targetId,
            description
        );

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog captured = auditLogCaptor.getValue();
        assertThat(captured.eventType()).isEqualTo(UserAuditEvent.USER_CREATED.getType());
        assertThat(captured.severity()).isEqualTo(UserAuditEvent.USER_CREATED.getSeverity());
        assertThat(captured.userId()).isEqualTo(userId);
        assertThat(captured.targetId()).isEqualTo(targetId);
        assertThat(captured.description()).isEqualTo(description);
    }

    @Test
    void testLogSecurityEvent() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AuditLog log = auditLogService.logEvent(
            SecurityAuditEvent.LOGIN_FAILED,
            1L,
            null,
            "ログイン失敗"
        );

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(log.severity()).isEqualTo(SecurityAuditEvent.LOGIN_FAILED.getSeverity());
        assertThat(log.eventType()).isEqualTo(SecurityAuditEvent.LOGIN_FAILED.getType());
    }

    @Test
    void testFindCriticalEvents() {
        // Given
        List<AuditLog> expectedLogs = List.of(
            new AuditLog(1L, "ERROR", Severity.CRITICAL, null, null, "エラー1", baseTime),
            new AuditLog(2L, "WARN", Severity.HIGH, null, null, "警告1", baseTime)
        );
        when(auditLogRepository.findBySeverityGreaterThanEqual(Severity.HIGH))
            .thenReturn(expectedLogs);

        // When
        List<AuditLog> logs = auditLogService.findCriticalEvents();

        // Then
        assertThat(logs).hasSize(2);
        assertThat(logs).allMatch(log -> log.severity().compareTo(Severity.HIGH) >= 0);
    }

    @Test
    void testFindBySeverityAndDateRange() {
        // Given
        LocalDateTime start = baseTime.minusHours(1);
        LocalDateTime end = baseTime.plusHours(1);
        List<AuditLog> expectedLogs = List.of(
            new AuditLog(1L, "TEST", Severity.HIGH, 1L, 2L, "テスト", baseTime)
        );
        when(auditLogRepository.findBySeverityAndDateRange(Severity.HIGH, start, end))
            .thenReturn(expectedLogs);

        // When
        List<AuditLog> logs = auditLogService.findBySeverityAndDateRange(
            Severity.HIGH, start, end);

        // Then
        assertThat(logs).hasSize(1);
        verify(auditLogRepository).findBySeverityAndDateRange(Severity.HIGH, start, end);
    }

    @Test
    void testLogSystemEvent() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AuditLog log = auditLogService.logEvent(
            SystemAuditEvent.ERROR_OCCURRED,
            null,
            null,
            "システムエラー"
        );

        // Then
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(log.severity()).isEqualTo(SystemAuditEvent.ERROR_OCCURRED.getSeverity());
        assertThat(log.eventType()).isEqualTo(SystemAuditEvent.ERROR_OCCURRED.getType());
    }

    @Test
    void testFindBySeverityGreaterThanEqual() {
        // Given
        List<AuditLog> expectedLogs = List.of(
            new AuditLog(1L, "ERROR", Severity.CRITICAL, null, null, "エラー", baseTime)
        );
        when(auditLogRepository.findBySeverityGreaterThanEqual(Severity.CRITICAL))
            .thenReturn(expectedLogs);

        // When
        List<AuditLog> logs = auditLogService.findBySeverityGreaterThanEqual(Severity.CRITICAL);

        // Then
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).severity()).isEqualTo(Severity.CRITICAL);
    }
}