package com.example.javase17learningproject.scheduler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.javase17learningproject.scheduler.model.ScheduledTask;
import com.example.javase17learningproject.scheduler.model.TaskPriority;
import com.example.javase17learningproject.scheduler.model.TaskResult;
import com.example.javase17learningproject.scheduler.model.TaskStatus;
import com.example.javase17learningproject.scheduler.model.TaskType;

class TaskManagerTest {

    private TaskManager taskManager;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        taskManager = new TaskManager();
        now = LocalDateTime.now();
    }

    @Test
    void testScheduleAndGetTask() {
        // Given
        ScheduledTask task = ScheduledTask.create(
            TaskType.DAILY_ARCHIVE,
            TaskPriority.HIGH,
            now.minusMinutes(1),
            Map.of("date", now.toLocalDate())
        );

        // When
        taskManager.scheduleTask(task);
        Optional<ScheduledTask> nextTask = taskManager.getNextTask();

        // Then
        assertThat(nextTask).isPresent();
        assertThat(nextTask.get().taskId()).isEqualTo(task.taskId());
        assertThat(taskManager.getTaskStatus(task.taskId()))
            .isEqualTo(TaskStatus.RUNNING);
    }

    @Test
    void testPriorityOrder() {
        // Given
        ScheduledTask highPriorityTask = ScheduledTask.create(
            TaskType.DAILY_ARCHIVE,
            TaskPriority.HIGH,
            now,
            Map.of()
        );
        ScheduledTask mediumPriorityTask = ScheduledTask.create(
            TaskType.CLEANUP,
            TaskPriority.MEDIUM,
            now.minusMinutes(1),  // より早い時刻
            Map.of()
        );

        // When
        taskManager.scheduleTask(mediumPriorityTask);
        taskManager.scheduleTask(highPriorityTask);
        Optional<ScheduledTask> nextTask = taskManager.getNextTask();

        // Then
        assertThat(nextTask).isPresent();
        assertThat(nextTask.get().priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void testTaskResult() {
        // Given
        ScheduledTask task = ScheduledTask.create(
            TaskType.DAILY_ARCHIVE,
            TaskPriority.HIGH,
            now,
            Map.of()
        );
        TaskResult result = TaskResult.success(
            task.taskId(),
            now,
            now.plusMinutes(1),
            Map.of("archivedFiles", 10)
        );

        // When
        taskManager.scheduleTask(task);
        taskManager.recordTaskResult(result);

        // Then
        assertThat(taskManager.getTaskResult(task.taskId()))
            .isPresent()
            .get()
            .satisfies(r -> {
                assertThat(r.status()).isEqualTo(TaskStatus.SUCCEEDED);
                assertThat(r.results()).containsEntry("archivedFiles", 10);
            });
    }

    @Test
    void testExpiredTaskCleanup() {
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
        taskManager.scheduleTask(expiredTask);

        // When
        taskManager.cleanupExpiredTasks();

        // Then
        assertThat(taskManager.getTaskStatus(expiredTask.taskId()))
            .isEqualTo(TaskStatus.TIMEOUT);
        assertThat(taskManager.getNextTask()).isEmpty();
    }

    @Test
    void testTaskHandlerRegistration() {
        // Given
        String taskId = "test-task";
        taskManager.registerTaskHandler(
            TaskType.DAILY_ARCHIVE.name(),
            params -> TaskResult.success(taskId, now, now.plusMinutes(1), params)
        );

        // When
        ScheduledTask task = ScheduledTask.create(
            TaskType.DAILY_ARCHIVE,
            TaskPriority.HIGH,
            now,
            Map.of("testParam", "value")
        );
        taskManager.scheduleTask(task);

        // Then
        assertThat(taskManager.getNextTask()).isPresent();
    }

    @Test
    void testReset() {
        // Given
        ScheduledTask task = ScheduledTask.create(
            TaskType.DAILY_ARCHIVE,
            TaskPriority.HIGH,
            now,
            Map.of()
        );
        taskManager.scheduleTask(task);

        // When
        taskManager.reset();

        // Then
        assertThat(taskManager.getNextTask()).isEmpty();
        assertThat(taskManager.getTaskStatus(task.taskId()))
            .isEqualTo(TaskStatus.PENDING);
        assertThat(taskManager.getTaskResult(task.taskId())).isEmpty();
    }
}