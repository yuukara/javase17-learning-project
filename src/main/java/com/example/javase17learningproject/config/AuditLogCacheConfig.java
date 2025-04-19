package com.example.javase17learningproject.config;

import com.example.javase17learningproject.model.AuditLog;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 監査ログのキャッシュ設定クラス.
 */
@Configuration
@EnableCaching
public class AuditLogCacheConfig {

    /** キャッシュの最大エントリ数 */
    private static final long MAX_CACHE_SIZE = 1000L;

    /** キャッシュエントリの有効期間（分） */
    private static final long CACHE_EXPIRY_MINUTES = 15L;

    /**
     * 監査ログのキャッシュを構成します.
     *
     * @return 設定されたキャッシュインスタンス
     */
    @Bean
    public Cache<Long, AuditLog> auditLogCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRY_MINUTES))
                .recordStats()
                .build();
    }

    /**
     * Hibernateセカンドレベルキャッシュの設定を構成します.
     *
     * @return 設定されたCacheManagerインスタンス
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRY_MINUTES))
                .recordStats());
        return cacheManager;
    }

    /**
     * Caffeineのキャッシュ設定を取得します.
     *
     * @return Caffeineの設定ビルダー
     */
    private Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterWrite(Duration.ofMinutes(CACHE_EXPIRY_MINUTES))
                .recordStats();
    }
}