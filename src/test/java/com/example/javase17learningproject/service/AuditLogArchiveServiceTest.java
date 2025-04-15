package com.example.javase17learningproject.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.javase17learningproject.config.ArchiveConfig;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.AuditLogEntity;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.repository.AuditLogRepository;
import com.example.javase17learningproject.util.JsonArchiveUtils;

@ExtendWith(MockitoExtension.class)
class AuditLogArchiveServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ArchiveConfig archiveConfig;

    private JsonArchiveUtils jsonArchiveUtils;
    private AuditLogArchiveService archiveService;
    private LocalDateTime baseTime;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jsonArchiveUtils = new JsonArchiveUtils();
        archiveService = new AuditLogArchiveServiceImpl(archiveConfig, auditLogRepository, jsonArchiveUtils);
        baseTime = LocalDateTime.now();

        // アーカイブディレクトリの設定
        when(archiveConfig.dailyArchivePath()).thenReturn(tempDir.resolve("daily"));
        when(archiveConfig.monthlyArchivePath()).thenReturn(tempDir.resolve("monthly"));
    }

    @Test
    void testCreateDailyArchive() throws IOException {
        // Given
        LocalDate date = LocalDate.now();
        Path archivePath = tempDir.resolve("daily/2025/04/audit_log_20250416.json.gz");
        List<AuditLogEntity> entities = createTestEntities();
        
        when(archiveConfig.getDailyArchiveFilePath(date)).thenReturn(archivePath);
        when(auditLogRepository.findByCreatedAtBetween(any(), any())).thenReturn(entities);

        // When
        int count = archiveService.createDailyArchive(date);

        // Then
        assertThat(count).isEqualTo(entities.size());
        assertThat(Files.exists(archivePath)).isTrue();
        verify(auditLogRepository).findByCreatedAtBetween(any(), any());
    }

    @Test
    void testCreateMonthlyArchive() throws IOException {
        // Given
        YearMonth yearMonth = YearMonth.now();
        Path monthlyPath = tempDir.resolve("monthly/2025/audit_log_202504.tar.gz");
        List<AuditLog> logs = createTestLogs();

        when(archiveConfig.getMonthlyArchiveFilePath(yearMonth)).thenReturn(monthlyPath);
        mockDailyArchives(yearMonth, logs);

        // When
        int count = archiveService.createMonthlyArchive(yearMonth);

        // Then
        assertThat(count).isGreaterThan(0);
        assertThat(Files.exists(monthlyPath)).isTrue();
    }

    @Test
    void testSearchArchive() throws IOException {
        // Given
        LocalDateTime start = baseTime.minusDays(1);
        LocalDateTime end = baseTime.plusDays(1);
        List<AuditLog> logs = createTestLogs();

        // テスト用のアーカイブを作成
        Path archivePath = createTestArchive(logs);
        when(archiveConfig.getDailyArchiveFilePath(any())).thenReturn(archivePath);

        // When
        List<AuditLog> results = archiveService.searchArchive(
            start, end, "USER_LOGIN", Severity.LOW
        );

        // Then
        assertThat(results).isNotEmpty()
            .allMatch(log -> log.eventType().equals("USER_LOGIN"))
            .allMatch(log -> log.severity() == Severity.LOW)
            .allMatch(log -> log.createdAt().isAfter(start))
            .allMatch(log -> log.createdAt().isBefore(end));
    }

    @Test
    void testDeleteOldArchives() throws IOException {
        // Given
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        List<AuditLog> logs = createTestLogs();

        // 古いアーカイブファイルを作成
        Path oldArchivePath = createTestArchive(logs);
        ReflectionTestUtils.setField(
            jsonArchiveUtils.readMetadata(oldArchivePath),
            "createdAt",
            baseTime.minusDays(91)
        );

        // When
        int deletedCount = archiveService.deleteOldArchives(cutoffDate);

        // Then
        assertThat(deletedCount).isPositive();
        assertThat(Files.exists(oldArchivePath)).isFalse();
    }

    @Test
    void testVerifyArchive() throws IOException {
        // Given
        LocalDate date = LocalDate.now();
        List<AuditLog> logs = createTestLogs();
        Path archivePath = createTestArchive(logs);

        when(archiveConfig.getDailyArchiveFilePath(date)).thenReturn(archivePath);

        // When
        boolean isValid = archiveService.verifyArchive(date);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void testGetStatistics() throws IOException {
        // Given
        List<AuditLog> logs = createTestLogs();
        createTestArchive(logs);

        // When
        AuditLogArchiveService.ArchiveStatistics stats = archiveService.getStatistics();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.totalArchivedLogs()).isPositive();
        assertThat(stats.totalArchiveFiles()).isPositive();
        assertThat(stats.totalSizeInBytes()).isPositive();
    }

    private List<AuditLogEntity> createTestEntities() {
        return List.of(
            new AuditLogEntity(1L, "USER_LOGIN", Severity.LOW, 1L, null, "User logged in", baseTime),
            new AuditLogEntity(2L, "USER_UPDATE", Severity.MEDIUM, 1L, 2L, "User updated", baseTime.plusHours(1))
        );
    }

    private List<AuditLog> createTestLogs() {
        return createTestEntities().stream()
            .map(AuditLogEntity::toRecord)
            .toList();
    }

    private Path createTestArchive(List<AuditLog> logs) throws IOException {
        Path archivePath = tempDir.resolve("test_archive.json.gz");
        Files.createDirectories(archivePath.getParent());
        jsonArchiveUtils.saveToGzipJson(logs, archivePath);
        return archivePath;
    }

    private void mockDailyArchives(YearMonth yearMonth, List<AuditLog> logs) throws IOException {
        Path dailyPath = tempDir.resolve("daily/test.json.gz");
        Files.createDirectories(dailyPath.getParent());
        jsonArchiveUtils.saveToGzipJson(logs, dailyPath);
        
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            when(archiveConfig.getDailyArchiveFilePath(yearMonth.atDay(day)))
                .thenReturn(dailyPath);
        }
    }
}