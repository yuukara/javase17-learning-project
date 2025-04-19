package com.example.javase17learningproject.config;

import com.example.javase17learningproject.model.AuditLog;
import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * キャッシュメトリクス設定クラス.
 */
@Configuration
public class CacheMetricsConfig {

    @Value("${spring.application.name:audit-log-service}")
    private String applicationName;

    /**
     * キャッシュメトリクスを構成します.
     *
     * @param auditLogCache 監査ログキャッシュ
     * @param meterRegistry メトリクスレジストリ
     */
    @Bean
    public void configureCacheMetrics(
            Cache<Long, AuditLog> auditLogCache,
            MeterRegistry meterRegistry) {
        
        CaffeineCacheMetrics.monitor(
            meterRegistry,
            auditLogCache,
            "auditLog",
            Collections.singleton(Tag.of("application", applicationName))
        );
    }

    /**
     * メトリクスレジストリのカスタマイズを構成します.
     *
     * @return メトリクスレジストリカスタマイザー
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", applicationName)
                .meterFilter(new MeterFilter() {
                    @Override
                    public io.micrometer.core.instrument.Meter.Id map(io.micrometer.core.instrument.Meter.Id id) {
                        // キャッシュ関連のメトリクスのフィルタリング
                        if (id.getName().startsWith("cache")) {
                            return id;
                        }
                        return id;
                    }
                });
    }

    /**
     * アラート条件のメトリクスを設定します.
     *
     * @param meterRegistry メトリクスレジストリ
     * @param auditLogCache 監査ログキャッシュ
     */
    @Bean
    public void configureAlerts(MeterRegistry meterRegistry, Cache<Long, AuditLog> auditLogCache) {
        // ヒット率のアラート条件
        meterRegistry.gauge(
            "cache.hit.rate.alert",
            auditLogCache,
            cache -> cache.stats().hitRate() < 0.8 ? 1.0 : 0.0
        );

        // メモリ使用量のアラート条件
        meterRegistry.gauge(
            "cache.memory.alert",
            auditLogCache,
            cache -> cache.estimatedSize() > cache.policy().eviction().get().getMaximum() * 0.9 ? 1.0 : 0.0
        );

        // 更新遅延のアラート条件
        meterRegistry.gauge(
            "cache.update.delay.alert",
            auditLogCache,
            cache -> {
                double loadTime = cache.stats().averageLoadPenalty();
                double threshold = TimeUnit.MINUTES.toNanos(5);
                return loadTime > threshold ? 1.0 : 0.0;
            }
        );
    }

    /**
     * デバッグ用のシンプルなメトリクスレジストリを構成します.
     *
     * @return シンプルなメトリクスレジストリ
     */
    @Bean
    public SimpleMeterRegistry simpleMeterRegistry() {
        return new SimpleMeterRegistry();
    }
}