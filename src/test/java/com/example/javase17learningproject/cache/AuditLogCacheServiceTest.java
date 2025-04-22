package com.example.javase17learningproject.cache;

import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;
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

/**
 * {@link AuditLogCacheService}のユニットテストクラス。
 *
 * <p>このテストクラスでは、監査ログのキャッシュ機能に関する以下の動作を検証します：
 * <ul>
 *   <li>キャッシュのヒット/ミス時の動作</li>
 *   <li>データの保存時のキャッシュ更新</li>
 *   <li>データの削除時のキャッシュ無効化</li>
 *   <li>検索結果のキャッシュ</li>
 *   <li>キャッシュの整合性チェックと強制更新</li>
 * </ul>
 */
class AuditLogCacheServiceTest {

    @Mock
    private AuditLogRepository repository;

    @Mock
    private AuditLogCacheManager cacheManager;

    private Cache<Long, AuditLog> cache;
    private AuditLogCacheService cacheService;

    /**
     * 各テストケース実行前の初期化処理。
     *
     * <p>テスト用のモックオブジェクトを初期化し、テスト用のキャッシュインスタンスを
     * 作成してAuditLogCacheServiceを初期化します。
     */
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

    /**
     * キャッシュミス時のfindByIdメソッドのテスト。
     *
     * <p>以下のシナリオを検証します：
     * <ul>
     *   <li>1回目の呼び出し：キャッシュミスによりデータベースからデータを取得</li>
     *   <li>2回目の呼び出し：キャッシュヒットによりデータベースアクセスなし</li>
     * </ul>
     */
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
        assertThat(result1.get().id()).isEqualTo(id);
        verify(repository, times(1)).findById(id);
        assertThat(cache.stats().hitCount()).isEqualTo(0);
        assertThat(cache.stats().missCount()).isEqualTo(1);

        // 2回目の呼び出し（キャッシュヒット）
        Optional<AuditLog> result2 = cacheService.findById(id);

        // 検証
        assertThat(result2).isPresent();
        assertThat(result2.get().id()).isEqualTo(id);
        verify(repository, times(1)).findById(id); // リポジトリは1回だけ呼ばれる
        assertThat(cache.stats().hitCount()).isEqualTo(1);
    }

    /**
     * 監査ログ保存時のキャッシュ更新テスト。
     *
     * <p>データベースへの保存と同時にキャッシュが正しく更新されることを検証します。
     */
    @Test
    void save_キャッシュを更新() {
        // テストデータの準備
        AuditLog log = createTestLog(1L);
        AuditLogEntity entity = AuditLogEntity.fromRecord(log);
        when(repository.save(any(AuditLogEntity.class))).thenReturn(entity);

        // 保存を実行
        AuditLog saved = cacheService.save(log);

        // 検証
        assertThat(saved.id()).isEqualTo(log.id());
        assertThat(cache.getIfPresent(log.id())).isNotNull();
        verify(repository, times(1)).save(any(AuditLogEntity.class));
    }

    /**
     * 監査ログ削除時のキャッシュ削除テスト。
     *
     * <p>データベースからの削除と同時にキャッシュからも対象データが削除されることを検証します。
     */
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

    /**
     * 日付範囲とタイプによる検索結果のキャッシュテスト。
     *
     * <p>検索結果が正しくキャッシュされ、各結果エントリが個別にキャッシュに
     * 保存されることを検証します。
     */
    @Test
    void findByDateRangeAndType_検索結果をキャッシュ() {
        // テストデータの準備
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        String eventType = "TEST_EVENT";
        AuditEvent.Severity severity = AuditEvent.Severity.HIGH;

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
            assertThat(cache.getIfPresent(log.id())).isNotNull()
        );
    }

    /**
     * キャッシュの整合性チェック機能のテスト。
     *
     * <p>キャッシュマネージャーの検証機能が正しく呼び出されることを確認します。
     */
    @Test
    void verifyCache_整合性チェックを実行() {
        // キャッシュ検証を実行
        cacheService.verifyCache();

        // 検証
        verify(cacheManager, times(1)).verifyCache();
    }

    /**
     * キャッシュの強制更新機能のテスト。
     *
     * <p>キャッシュマネージャーの強制更新機能が正しく呼び出されることを確認します。
     */
    @Test
    void refreshCache_強制更新を実行() {
        // キャッシュ更新を実行
        cacheService.refreshCache();

        // 検証
        verify(cacheManager, times(1)).forceRefresh();
    }

    /**
     * テスト用の監査ログエンティティを作成します。
     *
     * @param id エンティティのID
     * @return 指定されたIDを持つテスト用の監査ログエンティティ
     */
    private AuditLogEntity createTestEntity(Long id) {
        return new AuditLogEntity(
            id,
            "TEST_EVENT",
            AuditEvent.Severity.HIGH,
            1L,
            1L,
            "Test log " + id,
            LocalDateTime.now()
        );
    }

    /**
     * テスト用の監査ログモデルを作成します。
     *
     * @param id モデルのID
     * @return 指定されたIDを持つテスト用の監査ログモデル
     */
    private AuditLog createTestLog(Long id) {
        return new AuditLog(
            id,
            "TEST_EVENT",
            AuditEvent.Severity.HIGH,
            1L,
            1L,
            "Test log " + id,
            LocalDateTime.now()
        );
    }
}