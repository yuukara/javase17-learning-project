package com.example.javase17learningproject.scheduler.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * スケジューラーで管理されるタスクを表すレコード。
 */
public record ScheduledTask(
    String taskId,
    TaskType type,
    TaskPriority priority,
    LocalDateTime scheduledTime,
    Map<String, Object> parameters,
    int maxRetries,
    LocalDateTime deadline
) {
    /**
     * デフォルト値を使用してタスクを作成します。
     */
    public static ScheduledTask create(
        TaskType type,
        TaskPriority priority,
        LocalDateTime scheduledTime,
        Map<String, Object> parameters
    ) {
        return new ScheduledTask(
            UUID.randomUUID().toString(),
            type,
            priority,
            scheduledTime,
            parameters,
            3,  // デフォルトの最大リトライ回数
            scheduledTime.plusHours(1)  // デフォルトの期限（1時間後）
        );
    }

    /**
     * タスクの実行期限が切れているかどうかを判定します。
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(deadline);
    }

    /**
     * タスクを今すぐ実行可能かどうかを判定します。
     */
    public boolean isReadyToRun() {
        LocalDateTime now = LocalDateTime.now();
        return !isExpired() && 
               (now.isEqual(scheduledTime) || now.isAfter(scheduledTime));
    }

    /**
     * リトライ回数と期限を調整した新しいタスクを作成します。
     */
    public ScheduledTask withRetry(int retriesLeft, LocalDateTime newDeadline) {
        return new ScheduledTask(
            taskId,
            type,
            priority,
            scheduledTime,
            parameters,
            retriesLeft,
            newDeadline
        );
    }

    /**
     * このタスクの文字列表現を返します。
     */
    @Override
    public String toString() {
        return String.format(
            "Task[id=%s, type=%s, priority=%s, scheduled=%s, maxRetries=%d]",
            taskId,
            type,
            priority,
            scheduledTime,
            maxRetries
        );
    }
}