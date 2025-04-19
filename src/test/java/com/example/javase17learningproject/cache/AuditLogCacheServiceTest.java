package com.example.javase17learningproject.cache;

import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.Severity;
import com.example.javase17learningproject.repository.AuditLogRepository;
import com.example.javase17learningproject.service.AuditLogCacheService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogCacheServiceTest {

    @Mock
    private AuditLogRepository repository;

    @Mock
    private AuditLogCacheManager cacheManager;

    private Cache<Long, AuditLog> cache;
    private AuditLogCacheService cacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // テスト用のキャッシュを作成
        cache = Caffeine.newBuilder()
                .maximumSize(100)
                .recordStats()
                .build();

        cacheService = new AuditLogCacheService(cache, repository, cacheManager);
    }

    @Test
    void findById_キャッシュミス時にデータベースから取得() {
        // テストデータの準備
        Long id = 1L;
        AuditLogEntity entity = createTestEntity(id);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        // 1回目の呼び出し（キャッシュミス）
        Optional<AuditLog> result1 = cacheService.findById(id);

        // 検証
        assertThat(result1).isPresent();
        assertThat(result1.get().getId()).isEqualTo(id);
        verify(repository, times(1)).findById(id);
        assertThat(cache.stats().hitCount()).isEqualTo(0);
        assertThat(cache.stats().missCount()).isEqualTo(1);

        // 2回目の呼び出し（キャッシュヒット）
        Optional<AuditLog> result2 = cacheService.findById(id);

        // 検証
        assertThat(result2).isPresent();
        assertThat(result2.get().getId()).isEqualTo(id);
        verify(repository, times(1)).findById(id); // リポジトリは1回だけ呼ばれる
        assertThat(cache.stats().hitCount()).isEqualTo(1);
    }

    @Test
    void save_キャッシュを更新() {
        // テストデータの準備
        AuditLog log = createTestLog(1L);
        AuditLogEntity entity = AuditLogEntity.fromModel(log);
        when(repository.save(any(AuditLogEntity.class))).thenReturn(entity);

        // 保存を実行
        AuditLog saved = cacheService.save(log);

        // 検証
        assertThat(saved.getId()).isEqualTo(log.getId());
        assertThat(cache.getIfPresent(log.getId())).isNotNull();
        verify(repository, times(1)).save(any(AuditLogEntity.class));
    }

    @Test
    void delete_キャッシュからも削除() {
        // テストデータの準備
        Long id = 1L;
        cache.put(id, createTestLog(id));

        // 削除を実行
        cacheService.delete(id);

        // 検証
        assertThat(cache.getIfPresent(id)).isNull();
        verify(repository, times(1)).deleteById(id);
        verify(cacheManager, times(1)).invalidate(id);
    }

    @Test
    void findByDateRangeAndType_検索結果をキャッシュ() {
        // テストデータの準備
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        String eventType = "TEST_EVENT";
        Severity severity = Severity.HIGH;

        List<AuditLogEntity> entities = Arrays.asList(
            createTestEntity(1L),
            createTestEntity(2L)
        );
        when(repository.findBySeverityAndCreatedAtBetween(severity, start, end))
            .thenReturn(entities);

        // 検索を実行
        List<AuditLog> results = cacheService.findByDateRangeAndType(
            start, end, eventType, severity);

        // 検証
        assertThat(results).hasSize(2);
        results.forEach(log -> 
            assertThat(cache.getIfPresent(log.getId())).isNotNull()
        );
    }

    @Test
    void verifyCache_整合性チェックを実行() {
        // キャッシュ検証を実行
        cacheService.verifyCache();

        // 検証
        verify(cacheManager, times(1)).verifyCache();
    }

    @Test
    void refreshCache_強制更新を実行() {
        // キャッシュ更新を実行
        cacheService.refreshCache();

        // 検証
        verify(cacheManager, times(1)).forceRefresh();
    }

    private AuditLogEntity createTestEntity(Long id) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(id);
        entity.setEventType("TEST_EVENT");
        entity.setSeverity(Severity.HIGH);
        entity.setUserId(1L);
        entity.setDescription("Test log " + id);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private AuditLog createTestLog(Long id) {
        AuditLog log = new AuditLog();
        log.setId(id);
        log.setEventType("TEST_EVENT");
        log.setSeverity(Severity.HIGH);
        log.setUserId(1L);
        log.setDescription("Test log " + id);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}