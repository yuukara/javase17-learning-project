package com.example.javase17learningproject.scheduler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.javase17learningproject.scheduler.model.ScheduledTask;
import com.example.javase17learningproject.scheduler.model.TaskResult;
import com.example.javase17learningproject.scheduler.model.TaskStatus;

/**
 * スケジュールされたタスクの管理を行うコンポーネント。
 */
@Component
public class TaskManager {

    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

    private final PriorityQueue<ScheduledTask> taskQueue;
    private final Map<String, TaskStatus> taskStatuses;
    private final Map<String, TaskResult> taskResults;
    private final Map<String, Function<Map<String, Object>, TaskResult>> taskHandlers;

    public TaskManager() {
        this.taskQueue = new PriorityQueue<>((t1, t2) -> {
            int priorityCompare = t1.priority().compareByValue(t2.priority());
            return priorityCompare != 0 ? priorityCompare :
                   t1.scheduledTime().compareTo(t2.scheduledTime());
        });
        this.taskStatuses = new ConcurrentHashMap<>();
        this.taskResults = new ConcurrentHashMap<>();
        this.taskHandlers = new ConcurrentHashMap<>();
    }

    /**
     * タスクハンドラを登録します。
     */
    public void registerTaskHandler(
        String taskType,
        Function<Map<String, Object>, TaskResult> handler
    ) {
        taskHandlers.put(taskType, handler);
    }

    /**
     * タスクをスケジュールします。
     */
    public synchronized void scheduleTask(ScheduledTask task) {
        taskQueue.offer(task);
        taskStatuses.put(task.taskId(), TaskStatus.PENDING);
        logger.info("Task scheduled: {}", task);
    }

    /**
     * 次に実行可能なタスクを取得します。
     */
    public synchronized Optional<ScheduledTask> getNextTask() {
        ScheduledTask task = taskQueue.peek();
        if (task != null && task.isReadyToRun()) {
            task = taskQueue.poll();
            taskStatuses.put(task.taskId(), TaskStatus.RUNNING);
            return Optional.of(task);
        }
        return Optional.empty();
    }

    /**
     * タスクの状態を更新します。
     */
    public void updateTaskStatus(String taskId, TaskStatus status) {
        taskStatuses.put(taskId, status);
        logger.debug("Task status updated: {} -> {}", taskId, status);
    }

    /**
     * タスクの結果を記録します。
     */
    public void recordTaskResult(TaskResult result) {
        taskResults.put(result.taskId(), result);
        taskStatuses.put(result.taskId(), result.status());
        logger.info("Task completed: {}", result);
    }

    /**
     * タスクの状態を取得します。
     */
    public TaskStatus getTaskStatus(String taskId) {
        return taskStatuses.getOrDefault(taskId, TaskStatus.PENDING);
    }

    /**
     * タスクの結果を取得します。
     */
    public Optional<TaskResult> getTaskResult(String taskId) {
        return Optional.ofNullable(taskResults.get(taskId));
    }

    /**
     * 定期的に期限切れのタスクをクリーンアップします。
     */
    @Scheduled(fixedRate = 3600000) // 1時間ごと
    public synchronized void cleanupExpiredTasks() {
        LocalDateTime now = LocalDateTime.now();
        taskQueue.removeIf(task -> {
            if (task.isExpired()) {
                TaskResult timeoutResult = TaskResult.timeout(
                    task.taskId(),
                    task.scheduledTime(),
                    now
                );
                recordTaskResult(timeoutResult);
                logger.warn("Task expired and removed: {}", task);
                return true;
            }
            return false;
        });
    }

    /**
     * 全てのタスクの状態をリセットします。
     */
    public synchronized void reset() {
        taskQueue.clear();
        taskStatuses.clear();
        taskResults.clear();
        logger.info("Task manager reset completed");
    }
}