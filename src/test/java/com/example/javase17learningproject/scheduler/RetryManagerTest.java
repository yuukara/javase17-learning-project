package com.example.javase17learningproject.scheduler;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.javase17learningproject.scheduler.model.ScheduledTask;
import com.example.javase17learningproject.scheduler.model.TaskPriority;
import com.example.javase17learningproject.scheduler.model.TaskResult;
import com.example.javase17learningproject.scheduler.model.TaskType;

class RetryManagerTest {

    private RetryManager retryManager;
    private ScheduledTask task;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        retryManager = new RetryManager();
        now = LocalDateTime.now();
        task = ScheduledTask.create(
            TaskType.DAILY_ARCHIVE,
            TaskPriority.HIGH,
            now,
            Map.of()
        );
    }

    @Test
    void testHandleFailure() {
        // Given
        TaskResult failureResult = TaskResult.failure(
            task.taskId(),
            now,
            now.plusMinutes(1),
            "Test failure",
            0
        );

        // When
        boolean canRetry = retryManager.handleFailure(task, failureResult);

        // Then
        assertThat(canRetry).isTrue();
        assertThat(retryManager.getCurrentRetryCount(task.taskId())).isEqualTo(1);
        assertThat(retryManager.getNextRetryTime(task.taskId()))
            .isAfter(now);
    }

    @Test
    void testMaxRetriesReached() {
        // Given
        TaskResult failureResult = TaskResult.failure(
            task.taskId(),
            now,
            now.plusMinutes(1),
            "Test failure",
            0
        );

        // When
        // 最大リトライ回数まで失敗を繰り返す
        for (int i = 0; i < task.maxRetries(); i++) {
            retryManager.handleFailure(task, failureResult);
        }
        boolean canRetry = retryManager.handleFailure(task, failureResult);

        // Then
        assertThat(canRetry).isFalse();
        assertThat(retryManager.getCurrentRetryCount(task.taskId()))
            .isEqualTo(task.maxRetries());
    }

    @Test
    void testExponentialBackoff() {
        // Given
        TaskResult failureResult = TaskResult.failure(
            task.taskId(),
            now,
            now.plusMinutes(1),
            "Test failure",
            0
        );

        // When
        retryManager.handleFailure(task, failureResult);
        LocalDateTime firstRetry = retryManager.getNextRetryTime(task.taskId());
        
        retryManager.handleFailure(task, failureResult);
        LocalDateTime secondRetry = retryManager.getNextRetryTime(task.taskId());

        // Then
        // 2回目のリトライ間隔は1回目の2倍以上
        long firstInterval = firstRetry.getMinute() - now.getMinute();
        long secondInterval = secondRetry.getMinute() - firstRetry.getMinute();
        assertThat(secondInterval).isGreaterThanOrEqualTo(firstInterval * 2);
    }

    @Test
    void testIsWaitingForRetry() {
        // Given
        TaskResult failureResult = TaskResult.failure(
            task.taskId(),
            now,
            now.plusMinutes(1),
            "Test failure",
            0
        );

        // When
        retryManager.handleFailure(task, failureResult);

        // Then
        assertThat(retryManager.isWaitingForRetry(task.taskId())).isTrue();
    }

    @Test
    void testResetTask() {
        // Given
        TaskResult failureResult = TaskResult.failure(
            task.taskId(),
            now,
            now.plusMinutes(1),
            "Test failure",
            0
        );
        retryManager.handleFailure(task, failureResult);

        // When
        retryManager.resetTask(task.taskId());

        // Then
        assertThat(retryManager.getCurrentRetryCount(task.taskId())).isZero();
        assertThat(retryManager.getNextRetryTime(task.taskId())).isNull();
    }

    @Test
    void testReset() {
        // Given
        TaskResult failureResult = TaskResult.failure(
            task.taskId(),
            now,
            now.plusMinutes(1),
            "Test failure",
            0
        );
        retryManager.handleFailure(task, failureResult);

        // When
        retryManager.reset();

        // Then
        assertThat(retryManager.getCurrentRetryCount(task.taskId())).isZero();
        assertThat(retryManager.getNextRetryTime(task.taskId())).isNull();
    }

    @Test
    void testCanRetry() {
        // Given
        ScheduledTask expiredTask = new ScheduledTask(
            "expired-task",
            TaskType.DAILY_ARCHIVE,
            TaskPriority.HIGH,
            now.minusHours(2),
            Map.of(),
            3,
            now.minusHours(1)  // 1時間前に期限切れ
        );

        // When/Then
        assertThat(retryManager.canRetry(task)).isTrue();
        assertThat(retryManager.canRetry(expiredTask)).isFalse();
    }
}