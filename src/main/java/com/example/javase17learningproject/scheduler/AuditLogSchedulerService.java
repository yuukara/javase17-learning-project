package com.example.javase17learningproject.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.javase17learningproject.metrics.AuditLogMetrics;
import com.example.javase17learningproject.scheduler.model.ScheduledTask;
import com.example.javase17learningproject.scheduler.model.TaskPriority;
import com.example.javase17learningproject.scheduler.model.TaskResult;
import com.example.javase17learningproject.scheduler.model.TaskStatus;
import com.example.javase17learningproject.scheduler.model.TaskType;
import com.example.javase17learningproject.archive.AuditLogArchiveService;

/**
 * 監査ログのアーカイブ処理をスケジュールするサービス。
 */
@Service
public class AuditLogSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogSchedulerService.class);

    private final TaskManager taskManager;
    private final RetryManager retryManager;
    private final AuditLogArchiveService archiveService;
    private final AuditLogMetrics metrics;

    public AuditLogSchedulerService(
        TaskManager taskManager,
        RetryManager retryManager,
        AuditLogArchiveService archiveService,
        AuditLogMetrics metrics
    ) {
        this.taskManager = taskManager;
        this.retryManager = retryManager;
        this.archiveService = archiveService;
        this.metrics = metrics;
        registerTaskHandlers();
    }

    /**
     * タスクハンドラを登録します。
     */
    private void registerTaskHandlers() {
        taskManager.registerTaskHandler(
            TaskType.DAILY_ARCHIVE.name(),
            this::handleDailyArchive
        );
        taskManager.registerTaskHandler(
            TaskType.MONTHLY_ARCHIVE.name(),
            this::handleMonthlyArchive
        );
        taskManager.registerTaskHandler(
            TaskType.CLEANUP.name(),
            this::handleCleanup
        );
    }

    /**
     * 日次アーカイブタスクをスケジュールします。
     */
    @Scheduled(cron = "0 0 1 * * ?")  // 毎日午前1時
    public void scheduleDailyArchive() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        ScheduledTask task = ScheduledTask.create(
            TaskType.DAILY_ARCHIVE,
            TaskPriority.HIGH,
            LocalDateTime.now(),
            Map.of("date", targetDate)
        );
        taskManager.scheduleTask(task);
        logger.info("日次アーカイブタスクをスケジュール: {}", targetDate);
    }

    /**
     * 月次アーカイブタスクをスケジュールします。
     */
    @Scheduled(cron = "0 0 2 1 * ?")  // 毎月1日午前2時
    public void scheduleMonthlyArchive() {
        YearMonth targetMonth = YearMonth.now().minusMonths(1);
        ScheduledTask task = ScheduledTask.create(
            TaskType.MONTHLY_ARCHIVE,
            TaskPriority.HIGH,
            LocalDateTime.now(),
            Map.of("yearMonth", targetMonth)
        );
        taskManager.scheduleTask(task);
        logger.info("月次アーカイブタスクをスケジュール: {}", targetMonth);
    }

    /**
     * クリーンアップタスクをスケジュールします。
     */
    @Scheduled(cron = "0 0 3 * * ?")  // 毎日午前3時
    public void scheduleCleanup() {
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        ScheduledTask task = ScheduledTask.create(
            TaskType.CLEANUP,
            TaskPriority.MEDIUM,
            LocalDateTime.now(),
            Map.of("cutoffDate", cutoffDate)
        );
        taskManager.scheduleTask(task);
        logger.info("クリーンアップタスクをスケジュール: {} より前を削除", cutoffDate);
    }

    /**
     * 日次アーカイブタスクを処理します。
     * メトリクスとして処理時間と成功/失敗回数を記録します。
     */
    private TaskResult handleDailyArchive(Map<String, Object> params) {
        LocalDateTime startTime = LocalDateTime.now();
        String taskId = params.get("taskId").toString();
        LocalDate targetDate = (LocalDate) params.get("date");

        try {
            long processingStartTime = System.nanoTime();
            int count = archiveService.createDailyArchive(targetDate);
            long duration = System.nanoTime() - processingStartTime;
            
            metrics.recordDailyArchiveSuccess();
            metrics.recordDailyArchiveDuration(duration, TimeUnit.NANOSECONDS);

            return TaskResult.success(
                taskId,
                startTime,
                LocalDateTime.now(),
                Map.of("archivedCount", count)
            );
        } catch (Exception e) {
            logger.error("日次アーカイブ作成エラー: {}", targetDate, e);
            metrics.recordDailyArchiveFailure();
            return TaskResult.failure(
                taskId,
                startTime,
                LocalDateTime.now(),
                e.getMessage(),
                0
            );
        }
    }

    /**
     * 月次アーカイブタスクを処理します。
     * メトリクスとして処理時間と成功/失敗回数を記録します。
     */
    private TaskResult handleMonthlyArchive(Map<String, Object> params) {
        LocalDateTime startTime = LocalDateTime.now();
        String taskId = params.get("taskId").toString();
        YearMonth targetMonth = (YearMonth) params.get("yearMonth");

        try {
            long processingStartTime = System.nanoTime();
            int count = archiveService.createMonthlyArchive(targetMonth);
            long duration = System.nanoTime() - processingStartTime;
            
            metrics.recordMonthlyArchiveSuccess();
            metrics.recordMonthlyArchiveDuration(duration, TimeUnit.NANOSECONDS);

            return TaskResult.success(
                taskId,
                startTime,
                LocalDateTime.now(),
                Map.of("archivedCount", count)
            );
        } catch (Exception e) {
            logger.error("月次アーカイブ作成エラー: {}", targetMonth, e);
            metrics.recordMonthlyArchiveFailure();
            return TaskResult.failure(
                taskId,
                startTime,
                LocalDateTime.now(),
                e.getMessage(),
                0
            );
        }
    }

    /**
     * クリーンアップタスクを処理します。
     */
    private TaskResult handleCleanup(Map<String, Object> params) {
        LocalDateTime startTime = LocalDateTime.now();
        String taskId = params.get("taskId").toString();
        LocalDate cutoffDate = (LocalDate) params.get("cutoffDate");

        try {
            int count = archiveService.deleteOldArchives(cutoffDate);
            return TaskResult.success(
                taskId,
                startTime,
                LocalDateTime.now(),
                Map.of("deletedCount", count)
            );
        } catch (Exception e) {
            logger.error("アーカイブ削除エラー: {}", cutoffDate, e);
            return TaskResult.failure(
                taskId,
                startTime,
                LocalDateTime.now(),
                e.getMessage(),
                0
            );
        }
    }

    /**
     * 非同期でタスクを実行します。
     */
    private CompletableFuture<TaskResult> executeTaskAsync(ScheduledTask task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeWithRetry(task);
            } catch (Exception e) {
                logger.error("タスク実行エラー: {}", task, e);
                return TaskResult.failure(
                    task.taskId(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    e.getMessage(),
                    0
                );
            }
        });
    }

    /**
     * リトライ処理付きでタスクを実行します。
     */
    private TaskResult executeWithRetry(ScheduledTask task) {
        TaskResult result = null;
        int attempts = 0;

        while (attempts <= task.maxRetries()) {
            try {
                result = handleDailyArchive(Map.of(
                    "taskId", task.taskId(),
                    "params", task.parameters()
                ));
                if (result.status() == TaskStatus.SUCCEEDED) {
                    break;
                }
            } catch (Exception e) {
                logger.warn("タスク実行失敗（リトライ {}/{}）: {}", 
                    attempts, task.maxRetries(), task, e);
                result = TaskResult.failure(
                    task.taskId(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    e.getMessage(),
                    attempts
                );
            }

            attempts++;
            if (attempts <= task.maxRetries()) {
                try {
                    Thread.sleep(calculateRetryDelay(attempts));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return result;
    }

    /**
     * リトライ間隔を計算します。
     */
    private long calculateRetryDelay(int attempt) {
        return Math.min(1000L * (long) Math.pow(2, attempt - 1), 15 * 60 * 1000L);
    }
}