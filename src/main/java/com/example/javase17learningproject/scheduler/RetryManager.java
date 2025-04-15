package com.example.javase17learningproject.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.javase17learningproject.scheduler.model.ScheduledTask;
import com.example.javase17learningproject.scheduler.model.TaskResult;

/**
 * タスクのリトライ処理を管理するコンポーネント。
 */
@Component
public class RetryManager {

    private static final Logger logger = LoggerFactory.getLogger(RetryManager.class);

    // リトライ間隔の設定（指数バックオフ）
    private static final Duration INITIAL_DELAY = Duration.ofMinutes(1);
    private static final int BACKOFF_MULTIPLIER = 2;
    private static final Duration MAX_DELAY = Duration.ofMinutes(15);

    // リトライ状態の管理
    private final Map<String, Integer> retryCount;
    private final Map<String, LocalDateTime> nextRetryTime;

    public RetryManager() {
        this.retryCount = new ConcurrentHashMap<>();
        this.nextRetryTime = new ConcurrentHashMap<>();
    }

    /**
     * タスクの失敗を記録し、リトライが必要かどうかを判定します。
     */
    public boolean handleFailure(ScheduledTask task, TaskResult result) {
        String taskId = task.taskId();
        int currentRetries = retryCount.getOrDefault(taskId, 0);
        
        if (currentRetries >= task.maxRetries()) {
            logger.warn("Task {} has reached max retries ({})", taskId, task.maxRetries());
            return false;
        }

        // リトライ回数をインクリメント
        currentRetries++;
        retryCount.put(taskId, currentRetries);

        // 次回のリトライ時刻を計算
        Duration delay = calculateDelay(currentRetries);
        LocalDateTime nextTime = LocalDateTime.now().plus(delay);
        nextRetryTime.put(taskId, nextTime);

        logger.info("Scheduled retry #{} for task {} at {}", 
            currentRetries, taskId, nextTime);
        return true;
    }

    /**
     * 指数バックオフによるリトライ間隔を計算します。
     */
    private Duration calculateDelay(int retryCount) {
        Duration delay = INITIAL_DELAY.multipliedBy(
            (long) Math.pow(BACKOFF_MULTIPLIER, retryCount - 1)
        );
        return delay.compareTo(MAX_DELAY) > 0 ? MAX_DELAY : delay;
    }

    /**
     * タスクの次回リトライ時刻を取得します。
     */
    public LocalDateTime getNextRetryTime(String taskId) {
        return nextRetryTime.get(taskId);
    }

    /**
     * タスクの現在のリトライ回数を取得します。
     */
    public int getCurrentRetryCount(String taskId) {
        return retryCount.getOrDefault(taskId, 0);
    }

    /**
     * タスクのリトライ状態をリセットします。
     */
    public void resetTask(String taskId) {
        retryCount.remove(taskId);
        nextRetryTime.remove(taskId);
        logger.debug("Reset retry state for task {}", taskId);
    }

    /**
     * 全てのリトライ状態をリセットします。
     */
    public void reset() {
        retryCount.clear();
        nextRetryTime.clear();
        logger.info("All retry states have been reset");
    }

    /**
     * タスクが現在リトライ待ちかどうかを判定します。
     */
    public boolean isWaitingForRetry(String taskId) {
        LocalDateTime next = nextRetryTime.get(taskId);
        return next != null && LocalDateTime.now().isBefore(next);
    }

    /**
     * タスクがリトライ可能かどうかを判定します。
     */
    public boolean canRetry(ScheduledTask task) {
        int current = getCurrentRetryCount(task.taskId());
        return current < task.maxRetries() && !task.isExpired();
    }
}