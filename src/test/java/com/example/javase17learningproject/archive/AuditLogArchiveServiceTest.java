package com.example.javase17learningproject.archive;

import java.io.IOException;
import java.time.LocalDateTime;

import com.example.javase17learningproject.archive.impl.AuditLogArchiveServiceImpl;
import com.example.javase17learningproject.config.ArchiveConfig;
import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;
import com.example.javase17learningproject.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuditLogArchiveServiceTest {

    private static final AuditEvent.Severity TEST_SEVERITY = AuditEvent.Severity.HIGH;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ArchiveConfig archiveConfig;

    private AuditLogArchiveService archiveService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        archiveService = new AuditLogArchiveServiceImpl(tempDir.toString());
        ReflectionTestUtils.setField(archiveService, "auditLogRepository", auditLogRepository);
        when(auditLogRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(createTestLogs());
    }

    @Test
    void createDailyArchive_成功() throws IOException {
        // テストデータの準備
        LocalDate date = LocalDate.now();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<AuditLogEntity> testLogs = createTestLogs();
        when(auditLogRepository.findByCreatedAtBetween(start, end))
            .thenReturn(testLogs);

        // テスト実行
        archiveService.createDailyArchive(date);

        // 検証
        Path archivePath = tempDir.resolve("daily")
                                .resolve(String.valueOf(date.getYear()))
                                .resolve(String.format("%02d", date.getMonthValue()))
                                .resolve(String.format("audit_log_%s.json.gz", date));
        
        assertThat(archivePath).exists()
                              .isRegularFile();
    }

    @Test
    void createMonthlyArchive_成功() throws IOException {
        // テストデータの準備
        YearMonth yearMonth = YearMonth.now();
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // 日次アーカイブの作成
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            when(auditLogRepository.findByCreatedAtBetween(
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX)
            )).thenReturn(createTestLogs());
            archiveService.createDailyArchive(date);
        }

        // テスト実行
        archiveService.createMonthlyArchive(yearMonth);

        // 検証
        Path archivePath = tempDir.resolve("monthly")
                                .resolve(String.valueOf(yearMonth.getYear()))
                                .resolve(String.format("audit_log_%d%02d.tar.gz",
                                    yearMonth.getYear(),
                                    yearMonth.getMonthValue()));
        
        assertThat(archivePath).exists()
                              .isRegularFile();
    }

    @Test
    void searchArchive_成功() throws IOException {
        // テストデータの準備
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(1);
        LocalDateTime end = now;
        String eventType = "TEST_EVENT";
        AuditEvent.Severity severity = TEST_SEVERITY;

        // アーカイブの作成
        List<AuditLogEntity> testLogs = createTestLogs();
        when(auditLogRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(testLogs);
        archiveService.createDailyArchive(now.toLocalDate());

        // テスト実行
        List<AuditLog> results = archiveService.searchArchive(start, end, eventType, severity);

        // 検証
        assertThat(results).isNotEmpty()
                          .allMatch(log -> log.eventType().equals(eventType))
                          .allMatch(log -> log.severity() == severity)
                          .allMatch(log -> log.createdAt().isAfter(start))
                          .allMatch(log -> log.createdAt().isBefore(end));
    }

    @Test
    void verifyArchive_成功() throws IOException {
        // テストデータの準備
        LocalDate date = LocalDate.now();
        when(auditLogRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(createTestLogs());
        archiveService.createDailyArchive(date);

        // テスト実行
        boolean result = archiveService.verifyArchive(date);

        // 検証
        assertThat(result).isTrue();
    }

    @Test
    void deleteOldArchives_成功() throws IOException {
        // テストデータの準備
        LocalDateTime now = LocalDateTime.now();
        LocalDate oldDate = now.minusDays(91).toLocalDate();
        LocalDate cutoffDate = now.minusDays(90).toLocalDate();

        when(auditLogRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(createTestLogs());
        archiveService.createDailyArchive(oldDate);

        // テスト実行
        int deletedCount = archiveService.deleteOldArchives(cutoffDate);

        // 検証
        assertThat(deletedCount).isPositive();
        Path oldArchivePath = tempDir.resolve("daily")
            .resolve(String.valueOf(oldDate.getYear()))
            .resolve(String.format("%02d", oldDate.getMonthValue()))
            .resolve(String.format("audit_log_%s.json.gz", oldDate));
        assertThat(oldArchivePath).doesNotExist();
    }

    @Test
    void getStatistics_成功() throws IOException {
        // テストデータの準備
        LocalDate date = LocalDate.now();
        when(auditLogRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(createTestLogs());
        archiveService.createDailyArchive(date);

        // テスト実行
        ArchiveStatistics stats = archiveService.getStatistics();

        // 検証
        assertThat(stats).isNotNull();
        assertThat(stats.totalLogs()).isPositive();
        assertThat(stats.totalFiles()).isPositive();
        assertThat(stats.totalSize()).isPositive();
    }

    private List<AuditLogEntity> createTestLogs() {
        LocalDateTime now = LocalDateTime.now();
        return Arrays.asList(
            new AuditLogEntity(1L, "TEST_EVENT", TEST_SEVERITY, 1L, null, "Test log 1", now.minusHours(1)),
            new AuditLogEntity(2L, "TEST_EVENT", TEST_SEVERITY, 2L, null, "Test log 2", now.minusMinutes(30))
        );
    }
}