package com.example.javase17learningproject.model;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.model.audit.UserAuditEvent;

/**
 * AuditLogエンティティのテストクラス。
 */
class AuditLogTest {

    @Test
    void testCreateAuditLog() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        // When
        AuditLog log = new AuditLog(
            1L,
            "USER_CREATED",
            Severity.HIGH,
            2L,
            3L,
            "ユーザーが作成されました",
            now
        );

        // Then
        assertThat(log.id()).isEqualTo(1L);
        assertThat(log.eventType()).isEqualTo("USER_CREATED");
        assertThat(log.severity()).isEqualTo(Severity.HIGH);
        assertThat(log.userId()).isEqualTo(2L);
        assertThat(log.targetId()).isEqualTo(3L);
        assertThat(log.description()).isEqualTo("ユーザーが作成されました");
        assertThat(log.createdAt()).isEqualTo(now);
    }

    @Test
    void testCreateAuditLogWithNullEventType() {
        // When/Then
        assertThatThrownBy(() -> 
            new AuditLog(1L, null, Severity.MEDIUM, 2L, 3L, "説明", LocalDateTime.now())
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("イベントタイプは必須です");
    }

    @Test
    void testCreateAuditLogWithBlankEventType() {
        // When/Then
        assertThatThrownBy(() -> 
            new AuditLog(1L, "", Severity.MEDIUM, 2L, 3L, "説明", LocalDateTime.now())
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("イベントタイプは必須です");
    }

    @Test
    void testCreateAuditLogWithNullSeverity() {
        // When
        AuditLog log = new AuditLog(
            1L,
            "TEST_EVENT",
            null,
            2L,
            3L,
            "説明",
            LocalDateTime.now()
        );

        // Then
        assertThat(log.severity()).isEqualTo(Severity.MEDIUM);
    }

    @Test
    void testCreateAuditLogWithNullCreatedAt() {
        // When
        AuditLog log = new AuditLog(
            1L,
            "TEST_EVENT",
            Severity.LOW,
            2L,
            3L,
            "説明",
            null
        );

        // Then
        assertThat(log.createdAt()).isNotNull();
        assertThat(log.createdAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void testFactoryMethodWithEvent() {
        // When
        AuditLog log = AuditLog.of(
            UserAuditEvent.USER_CREATED,
            2L,
            3L,
            "説明"
        );

        // Then
        assertThat(log.id()).isNull();
        assertThat(log.eventType()).isEqualTo(UserAuditEvent.USER_CREATED.getType());
        assertThat(log.severity()).isEqualTo(UserAuditEvent.USER_CREATED.getSeverity());
        assertThat(log.userId()).isEqualTo(2L);
        assertThat(log.targetId()).isEqualTo(3L);
        assertThat(log.description()).isEqualTo("説明");
        assertThat(log.createdAt()).isNotNull();
        assertThat(log.createdAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void testFactoryMethodWithEventType() {
        // When
        AuditLog log = AuditLog.of("TEST_EVENT", 2L, 3L, "説明");

        // Then
        assertThat(log.id()).isNull();
        assertThat(log.eventType()).isEqualTo("TEST_EVENT");
        assertThat(log.severity()).isEqualTo(Severity.MEDIUM);
        assertThat(log.userId()).isEqualTo(2L);
        assertThat(log.targetId()).isEqualTo(3L);
        assertThat(log.description()).isEqualTo("説明");
        assertThat(log.createdAt()).isNotNull();
        assertThat(log.createdAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}