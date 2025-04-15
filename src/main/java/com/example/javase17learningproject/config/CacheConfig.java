package com.example.javase17learningproject.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * キャッシュ設定クラス。
 * Caffeineをキャッシュプロバイダーとして使用します。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 監査ログキャッシュのデフォルト設定
     */
    private static final int DEFAULT_MAXIMUM_SIZE = 1000;
    private static final int DEFAULT_EXPIRE_AFTER_WRITE = 15;
    private static final int DEFAULT_INITIAL_CAPACITY = 100;

    /**
     * キャッシュマネージャーの構成。
     * デフォルトのキャッシュ設定を適用します。
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * 監査ログ専用のキャッシュマネージャー。
     * 監査ログの特性に合わせた設定を適用します。
     */
    @Bean
    public CacheManager auditLogCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(DEFAULT_MAXIMUM_SIZE)
            .expireAfterWrite(DEFAULT_EXPIRE_AFTER_WRITE, TimeUnit.MINUTES)
            .initialCapacity(DEFAULT_INITIAL_CAPACITY)
            .recordStats());
        return cacheManager;
    }

    /**
     * デフォルトのCaffeineキャッシュビルダー。
     * 一般的なキャッシュ設定を提供します。
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats();
    }

    /**
     * カスタムキャッシュ設定を提供するビルダー。
     * 必要に応じて特定のキャッシュに異なる設定を適用できます。
     */
    public static Caffeine<Object, Object> customCacheBuilder(
        int maximumSize,
        int expireAfterWrite,
        TimeUnit timeUnit
    ) {
        return Caffeine.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(expireAfterWrite, timeUnit)
            .recordStats();
    }
}