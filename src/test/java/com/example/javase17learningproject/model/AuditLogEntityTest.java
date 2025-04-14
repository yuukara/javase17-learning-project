package com.example.javase17learningproject.model;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
class AuditLogEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testCreateValidEntity() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        AuditLogEntity entity = new AuditLogEntity(
            null,
            "TEST_EVENT",
            Severity.MEDIUM,
            1L,
            2L,
            "Test description",
            now
        );

        // When
        AuditLogEntity saved = entityManager.persistAndFlush(entity);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("TEST_EVENT");
        assertThat(saved.getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTargetId()).isEqualTo(2L);
        assertThat(saved.getDescription()).isEqualTo("Test description");
        assertThat(saved.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void testCreateEntityWithNullEventType() {
        // Given
        AuditLogEntity entity = new AuditLogEntity(
            null,
            null,
            Severity.MEDIUM,
            1L,
            2L,
            "Test description",
            LocalDateTime.now()
        );

        // When/Then
        assertThatThrownBy(() -> entityManager.persistAndFlush(entity))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void testCreateEntityWithNullSeverity() {
        // Given
        AuditLogEntity entity = new AuditLogEntity(
            null,
            "TEST_EVENT",
            null,
            1L,
            2L,
            "Test description",
            LocalDateTime.now()
        );

        // When/Then
        assertThatThrownBy(() -> entityManager.persistAndFlush(entity))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void testCreateEntityWithNullCreatedAt() {
        // Given
        AuditLogEntity entity = new AuditLogEntity(
            null,
            "TEST_EVENT",
            Severity.MEDIUM,
            1L,
            2L,
            "Test description",
            null
        );

        // When/Then
        assertThatThrownBy(() -> entityManager.persistAndFlush(entity))
            .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void testRecordConversion() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        AuditLogEntity entity = new AuditLogEntity(
            1L,
            "TEST_EVENT",
            Severity.HIGH,
            1L,
            2L,
            "Test description",
            now
        );

        // When
        AuditLog record = entity.toRecord();

        // Then
        assertThat(record.id()).isEqualTo(entity.getId());
        assertThat(record.eventType()).isEqualTo(entity.getEventType());
        assertThat(record.severity()).isEqualTo(entity.getSeverity());
        assertThat(record.userId()).isEqualTo(entity.getUserId());
        assertThat(record.targetId()).isEqualTo(entity.getTargetId());
        assertThat(record.description()).isEqualTo(entity.getDescription());
        assertThat(record.createdAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    void testCreateFromRecord() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        AuditLog record = new AuditLog(
            1L,
            "TEST_EVENT",
            Severity.HIGH,
            1L,
            2L,
            "Test description",
            now
        );

        // When
        AuditLogEntity entity = AuditLogEntity.fromRecord(record);

        // Then
        assertThat(entity.getId()).isEqualTo(record.id());
        assertThat(entity.getEventType()).isEqualTo(record.eventType());
        assertThat(entity.getSeverity()).isEqualTo(record.severity());
        assertThat(entity.getUserId()).isEqualTo(record.userId());
        assertThat(entity.getTargetId()).isEqualTo(record.targetId());
        assertThat(entity.getDescription()).isEqualTo(record.description());
        assertThat(entity.getCreatedAt()).isEqualTo(record.createdAt());
    }

    @Test
    void testDefaultSeverity() {
        // Given
        AuditLogEntity entity = new AuditLogEntity(
            null,
            "TEST_EVENT",
            null,  // severity is null
            1L,
            2L,
            "Test description",
            LocalDateTime.now()
        );

        // Then
        assertThat(entity.getSeverity()).isEqualTo(Severity.MEDIUM);
    }

    @Test
    void testDefaultCreatedAt() {
        // Given
        AuditLogEntity entity = new AuditLogEntity(
            null,
            "TEST_EVENT",
            Severity.MEDIUM,
            1L,
            2L,
            "Test description",
            null  // createdAt is null
        );

        // Then
        assertThat(entity.getCreatedAt()).isNotNull();
    }
}