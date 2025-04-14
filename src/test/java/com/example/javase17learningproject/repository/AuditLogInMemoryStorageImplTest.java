package com.example.javase17learningproject.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

@Execution(ExecutionMode.CONCURRENT)
class AuditLogInMemoryStorageImplTest {

    private AuditLogInMemoryStorageImpl storage;

    @BeforeEach
    void setUp() {
        storage = new AuditLogInMemoryStorageImpl();
    }

    @Test
    void testSaveAndFindById() {
        // Given
        AuditLog log = new AuditLog(null, "TEST_EVENT", Severity.MEDIUM, 1L, 2L, "Test description", LocalDateTime.now());

        // When
        AuditLog savedLog = storage.save(log);

        // Then
        assertThat(savedLog.id()).isNotNull();
        Optional<AuditLog> found = storage.findById(savedLog.id());
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(savedLog);
    }

    @Test
    void testFindLatestLogs() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            AuditLog log = new AuditLog(null, "EVENT_" + i, Severity.MEDIUM, 1L, 2L, "Description " + i, 
                now.plusMinutes(i));
            storage.save(log);
        }

        // When
        List<AuditLog> latestLogs = storage.findLatestLogs(3);

        // Then
        assertThat(latestLogs).hasSize(3);
        assertThat(latestLogs.get(0).createdAt()).isAfter(latestLogs.get(1).createdAt());
        assertThat(latestLogs.get(1).createdAt()).isAfter(latestLogs.get(2).createdAt());
    }

    @Test
    void testFindByEventType() {
        // Given
        String targetEventType = "TARGET_EVENT";
        AuditLog log1 = new AuditLog(null, targetEventType, Severity.HIGH, 1L, 2L, "Description 1", LocalDateTime.now());
        AuditLog log2 = new AuditLog(null, "OTHER_EVENT", Severity.MEDIUM, 1L, 2L, "Description 2", LocalDateTime.now());
        AuditLog log3 = new AuditLog(null, targetEventType, Severity.LOW, 1L, 2L, "Description 3", LocalDateTime.now());
        storage.save(log1);
        storage.save(log2);
        storage.save(log3);

        // When
        List<AuditLog> found = storage.findByEventType(targetEventType);

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(log -> targetEventType.equals(log.eventType()));
    }

    @Test
    void testFindAll() {
        // Given
        AuditLog log1 = new AuditLog(null, "EVENT1", Severity.HIGH, 1L, 2L, "Description 1", LocalDateTime.now());
        AuditLog log2 = new AuditLog(null, "EVENT2", Severity.MEDIUM, 1L, 2L, "Description 2", LocalDateTime.now());
        storage.save(log1);
        storage.save(log2);

        // When
        List<AuditLog> all = storage.findAll();

        // Then
        assertThat(all).hasSize(2);
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // Given
        int threadCount = 10;
        int logsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < logsPerThread; j++) {
                        AuditLog log = new AuditLog(
                            null,
                            "EVENT_" + threadId + "_" + j,
                            Severity.MEDIUM,
                            (long) threadId,
                            (long) j,
                            "Description",
                            LocalDateTime.now()
                        );
                        storage.save(log);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertThat(storage.findAll()).hasSize(threadCount * logsPerThread);
    }
}