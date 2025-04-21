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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.Captor;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.service.impl.AuditLogServiceImpl;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.repository.AuditLogInMemoryStorage;
import com.example.javase17learningproject.repository.AuditLogRepository;

/**
 * 監査ログサービスのテストクラス。
 * 以下の機能のテストを提供します：
 *
 * - 監査ログの保存と取得
 * - インメモリキャッシュとデータベースの連携
 * - ログの集計と検索
 * - アーカイブ処理
 */
@ExtendWith(SpringExtension.class)
class AuditLogServiceTest {

    /** データベースアクセス用のモックリポジトリ */
    @MockBean
    private AuditLogRepository repository;

    /** インメモリストレージのモック */
    @MockBean
    private AuditLogInMemoryStorage memoryStorage;

    /** エンティティのキャプチャ用 */
    @Captor
    private ArgumentCaptor<AuditLogEntity> entityCaptor;

    /** テスト対象のサービス */
    private AuditLogService service;

    /** テスト用の基準時刻 */
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AuditLogServiceImpl(repository, memoryStorage);
        baseTime = LocalDateTime.now();

        // 基本的なモックの設定
        when(memoryStorage.findLatestLogs(anyInt())).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    /**
     * 監査ログの保存をテストします。
     * メモリキャッシュとデータベースの両方に正しく保存されることを確認します。
     */
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

    /**
     * メモリキャッシュからの監査ログ取得をテストします。
     * キャッシュヒット時にデータベースにアクセスしないことを確認します。
     */
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

    /**
     * データベースからの監査ログ取得をテストします。
     * キャッシュミス時にデータベースから正しく取得できることを確認します。
     */
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

    /**
     * 古いログのアーカイブ処理をテストします。
     * 指定期間より古いログが正しく削除されることを確認します。
     */
    @Test
    void testArchiveOldLogs() {
        // Given
        when(repository.countByCreatedAtBefore(any())).thenReturn(10L);

        // When
        service.archiveOldLogs(baseTime.minusDays(90));

        // Then
        verify(repository).deleteByCreatedAtBefore(any());
    }

    /**
     * データベースへの移行処理をテストします。
     * メモリキャッシュのデータが正しくデータベースに移行されることを確認します。
     */
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

    /**
     * キャッシュの更新処理をテストします。
     * データベースの最新データでキャッシュが更新されることを確認します。
     */
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

    /**
     * イベントタイプごとの集計をテストします。
     * ログが正しく集計されることを確認します。
     */
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

    /**
     * 監査ログの検索機能をテストします。
     * 指定された条件で正しく検索できることを確認します。
     */
    @Test
    void testSearchLogs() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<AuditLogEntity> entities = List.of(
            new AuditLogEntity(1L, "TEST1", Severity.HIGH, 1L, 2L, "test1", baseTime),
            new AuditLogEntity(2L, "TEST1", Severity.MEDIUM, 1L, 2L, "test2", baseTime)
        );
        Page<AuditLogEntity> page = new PageImpl<>(entities, pageRequest, entities.size());
        
        when(repository.searchLogs(
            eq("TEST1"), eq(Severity.HIGH), eq(1L),
            eq(baseTime), eq(baseTime.plusDays(1)), eq(pageRequest)
        )).thenReturn(page);

        // When
        Page<AuditLog> result = service.searchLogs(
            "TEST1", Severity.HIGH, 1L,
            baseTime, baseTime.plusDays(1), pageRequest
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent())
            .hasSize(2)
            .allMatch(log -> log.eventType().equals("TEST1"));

        verify(repository).searchLogs(
            eq("TEST1"), eq(Severity.HIGH), eq(1L),
            eq(baseTime), eq(baseTime.plusDays(1)), eq(pageRequest)
        );
    }

    /**
     * 最新の監査ログ取得をテストします。
     * メモリキャッシュから最新のログを取得できることを確認します。
     */
    @Test
    void testFindLatestLogs() {
        // Given
        List<AuditLog> logs = List.of(
            new AuditLog(1L, "TEST1", Severity.HIGH, 1L, 2L, "test1", baseTime),
            new AuditLog(2L, "TEST2", Severity.MEDIUM, 1L, 2L, "test2", baseTime)
        );
        when(memoryStorage.findLatestLogs(2)).thenReturn(logs);

        // When
        List<AuditLog> result = service.findLatestLogs(2);

        // Then
        assertThat(result).hasSize(2);
        verify(memoryStorage).findLatestLogs(2);
    }

    /**
     * 重要度ごとの集計をテストします。
     * ログが重要度レベルごとに正しく集計されることを確認します。
     */
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