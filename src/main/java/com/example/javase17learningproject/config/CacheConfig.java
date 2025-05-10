package com.example.javase17learningproject.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.example.javase17learningproject.model.AuditLog;

/**
 * キャッシュ設定クラス。
 * Caffeineをキャッシュプロバイダーとして使用します。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final int AUDIT_LOG_MAX_SIZE = 1000;
    private static final int AUDIT_LOG_EXPIRE_MINUTES = 15;
    private static final int AUDIT_LOG_INITIAL_CAPACITY = 100;

    /**
     * アプリケーション全体で使用するキャッシュマネージャー。
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(AUDIT_LOG_MAX_SIZE)
            .expireAfterWrite(Duration.ofMinutes(AUDIT_LOG_EXPIRE_MINUTES))
            .initialCapacity(AUDIT_LOG_INITIAL_CAPACITY)
            .recordStats());
        return cacheManager;
    }

    /**
     * 監査ログ専用のキャッシュ。
     */
    @Bean
    public Cache<Long, AuditLog> auditLogCache() {
        return Caffeine.newBuilder()
            .maximumSize(AUDIT_LOG_MAX_SIZE)
            .expireAfterWrite(Duration.ofMinutes(AUDIT_LOG_EXPIRE_MINUTES))
            .initialCapacity(AUDIT_LOG_INITIAL_CAPACITY)
            .recordStats()
            .build();
    }
}