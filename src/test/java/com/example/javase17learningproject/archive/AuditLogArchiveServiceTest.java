package com.example.javase17learningproject.archive;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.javase17learningproject.archive.ArchiveStatistics;
import com.example.javase17learningproject.archive.impl.AuditLogArchiveServiceImpl;
import com.example.javase17learningproject.archive.impl.AuditLogArchiveStorageImpl;
import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;
import com.example.javase17learningproject.repository.AuditLogRepository;

import static java.nio.file.Files.createDirectories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

class AuditLogArchiveServiceTest {

    private static final AuditEvent.Severity TEST_SEVERITY = AuditEvent.Severity.HIGH;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogArchiveStorageImpl archiveStorage;

    private AuditLogArchiveService archiveService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        
        // ストレージ層のモック設定
        setupStorageMocks();

        // サービス層の初期化
        archiveService = new AuditLogArchiveServiceImpl(archiveStorage, auditLogRepository);
        
        // リポジトリのモック設定
        // 常に空のリストを返すようにデフォルト設定
        when(auditLogRepository.findByCreatedAtBetween(any(), any()))
            .thenReturn(List.of());
    }

    private void setupStorageMocks() throws IOException {
        // パスの基本ディレクトリを作成
        Path dailyDir = tempDir.resolve("daily");
        createDirectories(dailyDir);

        // パス解決のモック
        when(archiveStorage.getDailyArchivePath(any(LocalDate.class))).thenAnswer(invocation -> {
            LocalDate date = invocation.getArgument(0);
            return dailyDir.resolve(String.format("audit_log_%s.json.gz", date));
        });

        // アーカイブ操作のモック
        doAnswer(invocation -> {
            LocalDate date = invocation.getArgument(0);
            Path path = archiveStorage.getDailyArchivePath(date);
            Files.createFile(path);
            System.out.println("[DEBUG] Created daily archive at: " + path);
            return null;
        }).when(archiveStorage).createDailyArchive(any(), any());

        // アーカイブ検証のモック
        when(archiveStorage.verifyArchive(any())).thenReturn(true);
        when(archiveStorage.createMonthlyArchive(any(), any())).thenReturn(1);

        // 検索のモック
        List<AuditLog> testLogRecords = createTestLogs().stream()
            .map(AuditLogEntity::toRecord)
            .toList();
        when(archiveStorage.searchArchives(any(), any(), any(), any()))
            .thenReturn(testLogRecords);

        // 削除のモック
        when(archiveStorage.deleteOldArchives(any())).thenReturn(1);

        // 統計情報のモック
        lenient().when(archiveStorage.calculateStatistics())
            .thenReturn(new ArchiveStatistics(1, 2, 1000, 0.5,
                LocalDateTime.now(), LocalDateTime.now().minusDays(1)));
    }

    @Test
    void createDailyArchive_成功() throws IOException {
        // テストデータの準備
        LocalDate date = LocalDate.now();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<AuditLog> testLogs = createTestLogs().stream()
            .map(AuditLogEntity::toRecord)
            .toList();

        when(auditLogRepository.findByCreatedAtBetween(start, end))
            .thenReturn(createTestLogs());

        // テスト実行
        int result = archiveService.createDailyArchive(date);

        // 検証
        assertThat(result).isEqualTo(testLogs.size());
        verify(auditLogRepository).findByCreatedAtBetween(start, end);
        verify(archiveStorage).createDailyArchive(eq(date), any());
    }

    @Test
    void createMonthlyArchive_成功() throws IOException {
        // テストデータの準備
        YearMonth yearMonth = YearMonth.now();
        LocalDate firstDay = yearMonth.atDay(1);

        // データの準備
        List<AuditLogEntity> testLogs = createTestLogs();
        when(auditLogRepository.findByCreatedAtBetween(
            firstDay.atStartOfDay(),
            firstDay.atTime(LocalTime.MAX)
        )).thenReturn(testLogs);

        // 日次アーカイブを作成
        archiveService.createDailyArchive(firstDay);

        // テスト実行
        int result = archiveService.createMonthlyArchive(yearMonth);

        // 検証
        assertThat(result).isEqualTo(1);
        verify(archiveStorage).createDailyArchive(eq(firstDay), any());
        verify(archiveStorage).createMonthlyArchive(eq(yearMonth), any());
    }

    @Test
    void searchArchive_成功() throws IOException {
        // テストデータの準備
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(1);
        LocalDateTime end = now;
        String eventType = "TEST_EVENT";
        AuditEvent.Severity severity = TEST_SEVERITY;

        // テスト実行
        List<AuditLog> results = archiveService.searchArchive(start, end, eventType, severity);

        // 検証
        assertThat(results).isNotEmpty()
                          .allMatch(log -> log.eventType().equals(eventType))
                          .allMatch(log -> log.severity() == severity);

        verify(archiveStorage).searchArchives(start, end, eventType, severity);
    }

    @Test
    void verifyArchive_成功() throws IOException {
        // テストデータの準備
        LocalDate date = LocalDate.now();

        // テスト実行
        boolean result = archiveService.verifyArchive(date);

        // 検証
        assertThat(result).isTrue();
        verify(archiveStorage).verifyArchive(date);
        verify(auditLogRepository).findByCreatedAtBetween(
            date.atStartOfDay(),
            date.atTime(LocalTime.MAX)
        );
    }

    @Test
    void deleteOldArchives_成功() throws IOException {
        // テストデータの準備
        LocalDate cutoffDate = LocalDate.now().minusYears(1);

        // テスト実行
        int deletedCount = archiveService.deleteOldArchives(cutoffDate);

        // 検証
        assertThat(deletedCount).isPositive();
        verify(archiveStorage).deleteOldArchives(cutoffDate);
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
            new AuditLogEntity(
                1L, "TEST_EVENT", TEST_SEVERITY, 1L, null, "Test log 1", now.minusHours(1)
            ),
            new AuditLogEntity(
                2L, "TEST_EVENT", TEST_SEVERITY, 2L, null, "Test log 2", now.minusMinutes(30)
            )
        );
    }
}