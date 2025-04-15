package com.example.javase17learningproject.scheduler.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * タスクの実行結果を表すレコード。
 */
public record TaskResult(
    String taskId,
    TaskStatus status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String errorMessage,
    int retryCount,
    Map<String, Object> results
) {
    /**
     * 成功結果を作成します。
     */
    public static TaskResult success(
        String taskId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Map<String, Object> results
    ) {
        return new TaskResult(
            taskId,
            TaskStatus.SUCCEEDED,
            startTime,
            endTime,
            null,
            0,
            results
        );
    }

    /**
     * 失敗結果を作成します。
     */
    public static TaskResult failure(
        String taskId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String errorMessage,
        int retryCount
    ) {
        return new TaskResult(
            taskId,
            TaskStatus.FAILED,
            startTime,
            endTime,
            errorMessage,
            retryCount,
            Collections.emptyMap()
        );
    }

    /**
     * タイムアウト結果を作成します。
     */
    public static TaskResult timeout(
        String taskId,
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {
        return new TaskResult(
            taskId,
            TaskStatus.TIMEOUT,
            startTime,
            endTime,
            "Task execution timed out",
            0,
            Collections.emptyMap()
        );
    }

    /**
     * タスクの実行時間を取得します。
     */
    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    /**
     * このタスク結果の文字列表現を返します。
     */
    @Override
    public String toString() {
        return String.format(
            "TaskResult[id=%s, status=%s, duration=%s, retries=%d]",
            taskId,
            status,
            getDuration(),
            retryCount
        );
    }
}