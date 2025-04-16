package com.example.javase17learningproject.metrics;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * 監査ログメトリクスのテストクラス。
 * シンプルなメーターレジストリを使用してメトリクスの記録を検証します。
 */
class AuditLogMetricsTest {

    private MeterRegistry registry;
    private AuditLogMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AuditLogMetrics(registry);
    }

    /**
     * 日次アーカイブの成功カウントをテストします。
     */
    @Test
    void testDailyArchiveSuccessCount() {
        // When
        metrics.recordDailyArchiveSuccess();
        metrics.recordDailyArchiveSuccess();

        // Then
        double count = registry.get("audit.archive.daily.success")
            .counter()
            .count();
        assertThat(count).isEqualTo(2.0);
    }

    /**
     * 日次アーカイブの失敗カウントをテストします。
     */
    @Test
    void testDailyArchiveFailureCount() {
        // When
        metrics.recordDailyArchiveFailure();

        // Then
        double count = registry.get("audit.archive.daily.failure")
            .counter()
            .count();
        assertThat(count).isEqualTo(1.0);
    }

    /**
     * 月次アーカイブの成功カウントをテストします。
     */
    @Test
    void testMonthlyArchiveSuccessCount() {
        // When
        metrics.recordMonthlyArchiveSuccess();
        metrics.recordMonthlyArchiveSuccess();
        metrics.recordMonthlyArchiveSuccess();

        // Then
        double count = registry.get("audit.archive.monthly.success")
            .counter()
            .count();
        assertThat(count).isEqualTo(3.0);
    }

    /**
     * 月次アーカイブの失敗カウントをテストします。
     */
    @Test
    void testMonthlyArchiveFailureCount() {
        // When
        metrics.recordMonthlyArchiveFailure();
        metrics.recordMonthlyArchiveFailure();

        // Then
        double count = registry.get("audit.archive.monthly.failure")
            .counter()
            .count();
        assertThat(count).isEqualTo(2.0);
    }

    /**
     * 日次アーカイブの処理時間記録をテストします。
     */
    @Test
    void testDailyArchiveDuration() {
        // When
        metrics.recordDailyArchiveDuration(100, TimeUnit.MILLISECONDS);
        metrics.recordDailyArchiveDuration(200, TimeUnit.MILLISECONDS);

        // Then
        double totalTime = registry.get("audit.archive.daily.duration")
            .timer()
            .totalTime(TimeUnit.MILLISECONDS);
        assertThat(totalTime).isEqualTo(300.0);
    }

    /**
     * 月次アーカイブの処理時間記録をテストします。
     */
    @Test
    void testMonthlyArchiveDuration() {
        // When
        metrics.recordMonthlyArchiveDuration(1, TimeUnit.SECONDS);
        metrics.recordMonthlyArchiveDuration(2, TimeUnit.SECONDS);

        // Then
        double totalTime = registry.get("audit.archive.monthly.duration")
            .timer()
            .totalTime(TimeUnit.SECONDS);
        assertThat(totalTime).isEqualTo(3.0);
    }

    /**
     * 複数のメトリクスを組み合わせたシナリオをテストします。
     */
    @Test
    void testCombinedMetrics() {
        // When
        // 成功と失敗の両方を記録
        metrics.recordDailyArchiveSuccess();
        metrics.recordDailyArchiveFailure();
        metrics.recordDailyArchiveDuration(150, TimeUnit.MILLISECONDS);

        // Then
        assertThat(registry.get("audit.archive.daily.success")
            .counter()
            .count()).isEqualTo(1.0);
        assertThat(registry.get("audit.archive.daily.failure")
            .counter()
            .count()).isEqualTo(1.0);
        assertThat(registry.get("audit.archive.daily.duration")
            .timer()
            .totalTime(TimeUnit.MILLISECONDS)).isEqualTo(150.0);
    }
}