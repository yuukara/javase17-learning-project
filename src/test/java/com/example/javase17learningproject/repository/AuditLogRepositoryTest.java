package com.example.javase17learningproject.repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

import jakarta.validation.ConstraintViolationException;

@DataJpaTest
@ActiveProfiles("test")
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        baseTime = LocalDateTime.now();
        
        // テストデータの作成
        createTestData();
    }

    private void createTestData() {
        // 高重要度のログ
        auditLogRepository.save(new AuditLogEntity(
            null, "USER_DELETE", Severity.HIGH, 1L, 2L,
            "ユーザーを削除しました", baseTime.minusHours(1)
        ));

        // 中重要度のログ
        auditLogRepository.save(new AuditLogEntity(
            null, "USER_UPDATE", Severity.MEDIUM, 1L, 2L,
            "ユーザー情報を更新しました", baseTime.minusHours(2)
        ));

        // 低重要度のログ
        auditLogRepository.save(new AuditLogEntity(
            null, "USER_LOGIN", Severity.LOW, 1L, null,
            "ユーザーがログインしました", baseTime.minusHours(3)
        ));

        // 異なるユーザーのログ
        auditLogRepository.save(new AuditLogEntity(
            null, "USER_LOGIN", Severity.LOW, 2L, null,
            "別ユーザーがログインしました", baseTime.minusHours(4)
        ));
    }

    @Test
    void testSaveAuditLog() {
        // Given
        AuditLogEntity log = new AuditLogEntity(
            null, "TEST_EVENT", Severity.MEDIUM, 1L, 2L,
            "テストイベント", LocalDateTime.now()
        );

        // When
        AuditLogEntity saved = auditLogRepository.save(log);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("TEST_EVENT");
    }

    @Test
    void testSaveInvalidAuditLog() {
        // Given
        AuditLogEntity log = new AuditLogEntity(
            null, null, Severity.MEDIUM, 1L, 2L,
            "テストイベント", LocalDateTime.now()
        );

        // When/Then
        assertThatThrownBy(() -> auditLogRepository.save(log))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void testFindByEventType() {
        // When
        List<AuditLogEntity> logs = auditLogRepository.findByEventType("USER_LOGIN");

        // Then
        assertThat(logs).hasSize(2);
        assertThat(logs).allMatch(log -> "USER_LOGIN".equals(log.getEventType()));
    }

    @Test
    void testFindByUserId() {
        // When
        List<AuditLogEntity> logs = auditLogRepository.findByUserId(1L);

        // Then
        assertThat(logs).hasSize(3);
        assertThat(logs).allMatch(log -> log.getUserId().equals(1L));
    }

    @Test
    void testFindByCreatedAtBetween() {
        // When
        List<AuditLogEntity> logs = auditLogRepository.findByCreatedAtBetween(
            baseTime.minusHours(3),
            baseTime
        );

        // Then
        assertThat(logs).hasSize(3);
    }

    @Test
    void testFindBySeverityAndCreatedAtBetween() {
        // When
        List<AuditLogEntity> logs = auditLogRepository.findBySeverityAndCreatedAtBetween(
            Severity.HIGH,
            baseTime.minusHours(2),
            baseTime
        );

        // Then
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getSeverity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void testSearchLogsWithPagination() {
        // When
        Page<AuditLogEntity> page = auditLogRepository.searchLogs(
            "USER_LOGIN",
            Severity.LOW,
            null,
            baseTime.minusHours(5),
            baseTime,
            PageRequest.of(0, 10)
        );

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(log -> 
            "USER_LOGIN".equals(log.getEventType()) && 
            Severity.LOW.equals(log.getSeverity())
        );
    }

    @Test
    void testDeleteOldLogs() {
        // When
        auditLogRepository.deleteByCreatedAtBefore(baseTime.minusHours(3));

        // Then
        long remainingCount = auditLogRepository.count();
        assertThat(remainingCount).isEqualTo(3);
    }

    @Test
    void testCountOldLogs() {
        // When
        long count = auditLogRepository.countByCreatedAtBefore(baseTime.minusHours(3));

        // Then
        assertThat(count).isEqualTo(1);
    }
}