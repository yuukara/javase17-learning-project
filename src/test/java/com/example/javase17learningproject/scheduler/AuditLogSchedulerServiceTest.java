package com.example.javase17learningproject.scheduler;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.javase17learningproject.metrics.AuditLogMetrics;
import com.example.javase17learningproject.scheduler.model.ScheduledTask;
import com.example.javase17learningproject.scheduler.model.TaskPriority;
import com.example.javase17learningproject.scheduler.model.TaskResult;
import com.example.javase17learningproject.scheduler.model.TaskType;
import com.example.javase17learningproject.archive.AuditLogArchiveService;

/**
 * 監査ログスケジューラーサービスのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
class AuditLogSchedulerServiceTest {

    @Mock
    private TaskManager taskManager;

    @Mock
    private RetryManager retryManager;

    @Mock
    private AuditLogArchiveService archiveService;

    @Mock
    private AuditLogMetrics metrics;

    @Captor
    private ArgumentCaptor<Function<Map<String, Object>, TaskResult>> handlerCaptor;

    private AuditLogSchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        schedulerService = new AuditLogSchedulerService(
            taskManager,
            retryManager,
            archiveService,
            metrics
        );
    }

    /**
     * 日次アーカイブタスクのスケジュール登録をテストします。
     */
    @Test
    void testScheduleDailyArchive() {
        // When
        schedulerService.scheduleDailyArchive();

        // Then
        verify(taskManager).scheduleTask(argThat(task -> 
            task.type() == TaskType.DAILY_ARCHIVE &&
            task.priority() == TaskPriority.HIGH
        ));
    }

    /**
     * 月次アーカイブタスクのスケジュール登録をテストします。
     */
    @Test
    void testScheduleMonthlyArchive() {
        // When
        schedulerService.scheduleMonthlyArchive();

        // Then
        verify(taskManager).scheduleTask(argThat(task ->
            task.type() == TaskType.MONTHLY_ARCHIVE &&
            task.priority() == TaskPriority.HIGH
        ));
    }

    /**
     * クリーンアップタスクのスケジュール登録をテストします。
     */
    @Test
    void testScheduleCleanup() {
        // When
        schedulerService.scheduleCleanup();

        // Then
        verify(taskManager).scheduleTask(argThat(task ->
            task.type() == TaskType.CLEANUP &&
            task.priority() == TaskPriority.MEDIUM
        ));
    }

    /**
     * 日次アーカイブタスクハンドラーの成功時の動作をテストします。
     */
    @Test
    void testHandleDailyArchiveSuccess() throws Exception {
        // Given
        LocalDate targetDate = LocalDate.now().minusDays(1);
        when(archiveService.createDailyArchive(eq(targetDate)))
            .thenReturn(10);

        // When
        verify(taskManager).registerTaskHandler(
            eq(TaskType.DAILY_ARCHIVE.name()),
            handlerCaptor.capture()
        );
        Function<Map<String, Object>, TaskResult> handler = handlerCaptor.getValue();
        
        // タスクハンドラーを実行
        Map<String, Object> params = Map.of(
            "taskId", "test-task-1",
            "date", targetDate
        );
        handler.apply(params);

        // Then
        verify(archiveService).createDailyArchive(eq(targetDate));
        verify(metrics).recordDailyArchiveSuccess();
        verify(metrics).recordDailyArchiveDuration(any(Long.class), eq(TimeUnit.NANOSECONDS));
    }

    /**
     * 月次アーカイブタスクハンドラーの成功時の動作をテストします。
     */
    @Test
    void testHandleMonthlyArchiveSuccess() throws Exception {
        // Given
        YearMonth targetMonth = YearMonth.now().minusMonths(1);
        when(archiveService.createMonthlyArchive(eq(targetMonth)))
            .thenReturn(100);

        // When
        verify(taskManager).registerTaskHandler(
            eq(TaskType.MONTHLY_ARCHIVE.name()),
            handlerCaptor.capture()
        );
        Function<Map<String, Object>, TaskResult> handler = handlerCaptor.getValue();
        
        // タスクハンドラーを実行
        Map<String, Object> params = Map.of(
            "taskId", "test-task-2",
            "yearMonth", targetMonth
        );
        handler.apply(params);

        // Then
        verify(archiveService).createMonthlyArchive(eq(targetMonth));
        verify(metrics).recordMonthlyArchiveSuccess();
        verify(metrics).recordMonthlyArchiveDuration(any(Long.class), eq(TimeUnit.NANOSECONDS));
    }

    /**
     * クリーンアップタスクハンドラーの成功時の動作をテストします。
     */
    @Test
    void testHandleCleanupSuccess() throws Exception {
        // Given
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        when(archiveService.deleteOldArchives(eq(cutoffDate)))
            .thenReturn(5);

        // When
        verify(taskManager).registerTaskHandler(
            eq(TaskType.CLEANUP.name()),
            handlerCaptor.capture()
        );
        Function<Map<String, Object>, TaskResult> handler = handlerCaptor.getValue();
        
        // タスクハンドラーを実行
        Map<String, Object> params = Map.of(
            "taskId", "test-task-3",
            "cutoffDate", cutoffDate
        );
        handler.apply(params);

        // Then
        verify(archiveService).deleteOldArchives(eq(cutoffDate));
    }

    /**
     * 日次アーカイブタスクハンドラーの失敗時の動作をテストします。
     */
    @Test
    void testHandleDailyArchiveFailure() throws Exception {
        // Given
        LocalDate targetDate = LocalDate.now().minusDays(1);
        doThrow(new RuntimeException("Archive failed"))
            .when(archiveService)
            .createDailyArchive(eq(targetDate));

        // When
        verify(taskManager).registerTaskHandler(
            eq(TaskType.DAILY_ARCHIVE.name()),
            handlerCaptor.capture()
        );
        Function<Map<String, Object>, TaskResult> handler = handlerCaptor.getValue();
        
        // タスクハンドラーを実行
        Map<String, Object> params = Map.of(
            "taskId", "test-task-4",
            "date", targetDate
        );
        TaskResult result = handler.apply(params);

        // Then
        verify(archiveService).createDailyArchive(eq(targetDate));
        verify(metrics).recordDailyArchiveFailure();
    }
}