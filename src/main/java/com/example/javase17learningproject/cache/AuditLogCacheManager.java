package com.example.javase17learningproject.cache;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.repository.AuditLogRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 監査ログのキャッシュ管理クラス.
 */
@Component
public class AuditLogCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogCacheManager.class);
    private static final String CACHE_NAME = "auditLog";

    private final Cache<Long, AuditLog> cache;
    private final AuditLogRepository repository;
    private final MeterRegistry meterRegistry;

    private final Timer verifyTimer;
    private final Timer refreshTimer;

    @Autowired
    public AuditLogCacheManager(
            Cache<Long, AuditLog> auditLogCache,
            AuditLogRepository repository,
            MeterRegistry meterRegistry) {
        this.cache = auditLogCache;
        this.repository = repository;
        this.meterRegistry = meterRegistry;

        // メトリクスタイマーの初期化
        this.verifyTimer = Timer.builder("cache.verify")
                .tag("cache", CACHE_NAME)
                .description("キャッシュ検証の実行時間")
                .register(meterRegistry);
        this.refreshTimer = Timer.builder("cache.refresh")
                .tag("cache", CACHE_NAME)
                .description("キャッシュ更新の実行時間")
                .register(meterRegistry);

        // キャッシュ統計の登録
        registerCacheMetrics();
    }

    /**
     * キャッシュの整合性を検証します.
     * 不整合が見つかった場合は更新を行います.
     */
    public void verifyCache() {
        logger.info("Starting cache verification");
        verifyTimer.record(() -> {
            try {
                List<Long> cachedIds = cache.asMap().keySet().stream().collect(Collectors.toList());
                repository.findAllById(cachedIds).forEach(entity -> {
                    AuditLog cached = cache.getIfPresent(entity.getId());
                    if (cached != null && !cached.equals(entity.toRecord())) {
                        logger.warn("Cache inconsistency detected for ID: {}", entity.getId());
                        cache.put(entity.getId(), entity.toRecord());
                    }
                });
                logger.info("Cache verification completed");
            } catch (Exception e) {
                logger.error("Failed to verify cache", e);
            }
        });
    }

    /**
     * キャッシュを強制的に更新します.
     */
    public void forceRefresh() {
        logger.info("Starting forced cache refresh");
        refreshTimer.record(() -> {
            try {
                cache.invalidateAll();
                repository.findAll().forEach(entity ->
                    cache.put(entity.getId(), entity.toRecord())
                );
                logger.info("Cache refresh completed");
            } catch (Exception e) {
                logger.error("Failed to refresh cache", e);
            }
        });
    }

    /**
     * スケジュールされたキャッシュ検証を実行します.
     */
    @Scheduled(fixedRate = 900000) // 15分間隔
    public void scheduledVerification() {
        verifyCache();
    }

    /**
     * キャッシュ統計を取得します.
     *
     * @return キャッシュの統計情報
     */
    public CacheStats getCacheStats() {
        return cache.stats();
    }

    /**
     * 指定されたIDのエントリをキャッシュから削除します.
     *
     * @param id 削除するエントリのID
     */
    public void invalidate(Long id) {
        cache.invalidate(id);
        logger.debug("Invalidated cache entry for ID: {}", id);
    }

    /**
     * キャッシュの統計情報をメトリクスとして登録します.
     */
    private void registerCacheMetrics() {
        meterRegistry.gauge("cache.size", cache, Cache::estimatedSize);
        meterRegistry.gauge("cache.hit.ratio", cache, c -> c.stats().hitRate());
        meterRegistry.gauge("cache.miss.ratio", cache, c -> c.stats().missRate());
        meterRegistry.gauge("cache.eviction.count", cache, c -> c.stats().evictionCount());
        meterRegistry.gauge("cache.load.duration", cache, c -> c.stats().totalLoadTime() / TimeUnit.MILLISECONDS.toNanos(1));
    }
}