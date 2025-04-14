package com.example.javase17learningproject.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.Captor;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.AuditLogEntity;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.repository.AuditLogInMemoryStorage;
import com.example.javase17learningproject.repository.AuditLogRepository;

@ExtendWith(SpringExtension.class)
class AuditLogServiceTest {

    @MockBean
    private AuditLogRepository repository;

    @MockBean
    private AuditLogInMemoryStorage memoryStorage;

    @Captor
    private ArgumentCaptor<AuditLogEntity> entityCaptor;

    private AuditLogService service;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        service = new AuditLogServiceImpl(repository, memoryStorage);
        baseTime = LocalDateTime.now();
    }

    @Test
    void testSaveAuditLog() {
        // Given
        AuditLog log = new AuditLog(null, "TEST", Severity.MEDIUM, 1L, 2L, "test", baseTime);
        AuditLogEntity entity = AuditLogEntity.fromRecord(log);
        when(repository.save(any())).thenReturn(entity);
        when(memoryStorage.save(any())).thenReturn(log);

        // When
        AuditLog saved = service.save(log);

        // Then
        verify(memoryStorage).save(log);
        verify(repository).save(any());
        assertThat(saved).isNotNull();
    }

    @Test
    void testFindByIdFromMemory() {
        // Given
        AuditLog log = new AuditLog(1L, "TEST", Severity.MEDIUM, 1L, 2L, "test", baseTime);
        when(memoryStorage.findById(1L)).thenReturn(Optional.of(log));

        // When
        AuditLog found = service.findById(1L);

        // Then
        assertThat(found).isEqualTo(log);
        verify(repository, never()).findById(any());
    }

    @Test
    void testFindByIdFromDatabase() {
        // Given
        when(memoryStorage.findById(1L)).thenReturn(Optional.empty());
        AuditLogEntity entity = new AuditLogEntity(1L, "TEST", Severity.MEDIUM, 1L, 2L, "test", baseTime);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        // When
        AuditLog found = service.findById(1L);

        // Then
        assertThat(found).isNotNull();
        verify(memoryStorage).findById(1L);
        verify(repository).findById(1L);
    }

    @Test
    void testArchiveOldLogs() {
        // Given
        when(repository.countByCreatedAtBefore(any())).thenReturn(10L);

        // When
        service.archiveOldLogs(baseTime.minusDays(90));

        // Then
        verify(repository).deleteByCreatedAtBefore(any());
    }

    @Test
    void testMigrateToDatabase() {
        // Given
        List<AuditLog> logs = List.of(
            new AuditLog(1L, "TEST1", Severity.HIGH, 1L, 2L, "test1", baseTime),
            new AuditLog(2L, "TEST2", Severity.MEDIUM, 1L, 2L, "test2", baseTime)
        );
        when(memoryStorage.findAll()).thenReturn(logs);

        // When
        service.migrateToDatabase();

        // Then
        verify(repository).saveAll(anyList());
        verify(memoryStorage).clear();
    }

    @Test
    void testRefreshCache() {
        // Given
        List<AuditLogEntity> entities = List.of(
            new AuditLogEntity(1L, "TEST1", Severity.HIGH, 1L, 2L, "test1", baseTime),
            new AuditLogEntity(2L, "TEST2", Severity.MEDIUM, 1L, 2L, "test2", baseTime)
        );
        Page<AuditLogEntity> page = new PageImpl<>(entities);
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        // When
        service.refreshCache();

        // Then
        verify(memoryStorage).clear();
        verify(repository).findAll(any(PageRequest.class));
        verify(memoryStorage, times(2)).save(any());
    }

    @Test
    void testAggregateByEventType() {
        // Given
        List<AuditLogEntity> logs = List.of(
            new AuditLogEntity(1L, "TEST1", Severity.HIGH, 1L, 2L, "test1", baseTime),
            new AuditLogEntity(2L, "TEST1", Severity.MEDIUM, 1L, 2L, "test2", baseTime),
            new AuditLogEntity(3L, "TEST2", Severity.LOW, 1L, 2L, "test3", baseTime)
        );
        when(repository.findByCreatedAtBetween(any(), any())).thenReturn(logs);

        // When
        Map<String, Long> result = service.aggregateByEventType(
            baseTime.minusDays(1),
            baseTime
        );

        // Then
        assertThat(result)
            .containsEntry("TEST1", 2L)
            .containsEntry("TEST2", 1L);
    }

    @Test
    void testCountBySeverity() {
        // Given
        List<AuditLogEntity> logs = List.of(
            new AuditLogEntity(1L, "TEST1", Severity.HIGH, 1L, 2L, "test1", baseTime),
            new AuditLogEntity(2L, "TEST2", Severity.HIGH, 1L, 2L, "test2", baseTime),
            new AuditLogEntity(3L, "TEST3", Severity.MEDIUM, 1L, 2L, "test3", baseTime)
        );
        when(repository.findByCreatedAtBetween(any(), any())).thenReturn(logs);

        // When
        Map<Severity, Long> result = service.countBySeverity(
            baseTime.minusDays(1),
            baseTime
        );

        // Then
        assertThat(result)
            .containsEntry(Severity.HIGH, 2L)
            .containsEntry(Severity.MEDIUM, 1L);
    }
}