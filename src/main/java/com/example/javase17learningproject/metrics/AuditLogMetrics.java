package com.example.javase17learningproject.metrics;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 監査ログのメトリクスを収集するコンポーネント。
 * シンプルな実装として以下のメトリクスを収集します：
 * - アーカイブ処理の成功/失敗回数
 * - 処理時間
 * - アーカイブサイズ
 */
@Component
public class AuditLogMetrics {

    private final Counter dailyArchiveSuccessCounter;
    private final Counter dailyArchiveFailureCounter;
    private final Counter monthlyArchiveSuccessCounter;
    private final Counter monthlyArchiveFailureCounter;
    private final Timer dailyArchiveTimer;
    private final Timer monthlyArchiveTimer;

    public AuditLogMetrics(MeterRegistry registry) {
        // 日次アーカイブのカウンター
        this.dailyArchiveSuccessCounter = Counter.builder("audit.archive.daily.success")
            .description("Number of successful daily archive operations")
            .register(registry);

        this.dailyArchiveFailureCounter = Counter.builder("audit.archive.daily.failure")
            .description("Number of failed daily archive operations")
            .register(registry);

        // 月次アーカイブのカウンター
        this.monthlyArchiveSuccessCounter = Counter.builder("audit.archive.monthly.success")
            .description("Number of successful monthly archive operations")
            .register(registry);

        this.monthlyArchiveFailureCounter = Counter.builder("audit.archive.monthly.failure")
            .description("Number of failed monthly archive operations")
            .register(registry);

        // 処理時間の測定
        this.dailyArchiveTimer = Timer.builder("audit.archive.daily.duration")
            .description("Time taken for daily archive operations")
            .register(registry);

        this.monthlyArchiveTimer = Timer.builder("audit.archive.monthly.duration")
            .description("Time taken for monthly archive operations")
            .register(registry);
    }

    /**
     * 日次アーカイブの成功を記録します。
     */
    public void recordDailyArchiveSuccess() {
        dailyArchiveSuccessCounter.increment();
    }

    /**
     * 日次アーカイブの失敗を記録します。
     */
    public void recordDailyArchiveFailure() {
        dailyArchiveFailureCounter.increment();
    }

    /**
     * 月次アーカイブの成功を記録します。
     */
    public void recordMonthlyArchiveSuccess() {
        monthlyArchiveSuccessCounter.increment();
    }

    /**
     * 月次アーカイブの失敗を記録します。
     */
    public void recordMonthlyArchiveFailure() {
        monthlyArchiveFailureCounter.increment();
    }

    /**
     * 日次アーカイブの処理時間を記録します。
     */
    public void recordDailyArchiveDuration(long duration, TimeUnit unit) {
        dailyArchiveTimer.record(duration, unit);
    }

    /**
     * 月次アーカイブの処理時間を記録します。
     */
    public void recordMonthlyArchiveDuration(long duration, TimeUnit unit) {
        monthlyArchiveTimer.record(duration, unit);
    }
}