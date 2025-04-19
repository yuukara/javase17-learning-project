package com.example.javase17learningproject.archive;

import com.example.javase17learningproject.archive.impl.AuditLogArchiveServiceImpl;
import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.Severity;
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

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogArchiveService archiveService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        archiveService = new AuditLogArchiveServiceImpl();
        ReflectionTestUtils.setField(archiveService, "auditLogRepository", auditLogRepository);
        ReflectionTestUtils.setField(archiveService, "ARCHIVE_BASE_DIR", tempDir.toString());
    }

    @Test
    void createDailyArchive_成功() {
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
    void createMonthlyArchive_成功() {
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
    void searchArchive_成功() {
        // テストデータの準備
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(1);
        LocalDateTime end = now;
        String eventType = "TEST_EVENT";
        Severity severity = Severity.HIGH;

        // アーカイブの作成
        List<AuditLogEntity> testLogs = createTestLogs();
        when(auditLogRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(testLogs);
        archiveService.createDailyArchive(now.toLocalDate());

        // テスト実行
        List<AuditLog> results = archiveService.searchArchive(start, end, eventType, severity);

        // 検証
        assertThat(results).isNotEmpty()
                          .allMatch(log -> log.getEventType().equals(eventType))
                          .allMatch(log -> log.getSeverity() == severity)
                          .allMatch(log -> log.getCreatedAt().isAfter(start))
                          .allMatch(log -> log.getCreatedAt().isBefore(end));
    }

    private List<AuditLogEntity> createTestLogs() {
        AuditLogEntity log1 = new AuditLogEntity();
        log1.setId(1L);
        log1.setEventType("TEST_EVENT");
        log1.setSeverity(Severity.HIGH);
        log1.setUserId(1L);
        log1.setDescription("Test log 1");
        log1.setCreatedAt(LocalDateTime.now().minusHours(1));

        AuditLogEntity log2 = new AuditLogEntity();
        log2.setId(2L);
        log2.setEventType("TEST_EVENT");
        log2.setSeverity(Severity.HIGH);
        log2.setUserId(2L);
        log2.setDescription("Test log 2");
        log2.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        return Arrays.asList(log1, log2);
    }
}