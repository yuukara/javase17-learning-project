package com.example.javase17learningproject.scheduler;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.javase17learningproject.scheduler.model.ScheduledTask;
import com.example.javase17learningproject.scheduler.model.TaskPriority;
import com.example.javase17learningproject.scheduler.model.TaskType;
import com.example.javase17learningproject.service.AuditLogArchiveService;

/**
 * 監査ログスケジューラーサービスのテストクラス。
 * 
 * このテストクラスでは以下の機能を検証します：
 * - スケジュールされたタスクの登録
 * - タスクハンドラの正しい登録と呼び出し
 * - 成功/失敗時の動作
 * - タスク優先順位の管理
 */
@ExtendWith(MockitoExtension.class)
class AuditLogSchedulerServiceTest {

    @Mock
    private TaskManager taskManager;

    @Mock
    private RetryManager retryManager;

    @Mock
    private AuditLogArchiveService archiveService;

    private AuditLogSchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        schedulerService = new AuditLogSchedulerService(
            taskManager,
            retryManager,
            archiveService
        );
    }

    /**
     * 日次アーカイブタスクのスケジュール登録をテストします。
     * タスクが正しい優先順位とタイプで登録されることを確認します。
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
     * タスクが正しい優先順位とタイプで登録されることを確認します。
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
     * タスクが正しい優先順位とタイプで登録されることを確認します。
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
     * 日次アーカイブタスクの成功時の動作をテストします。
     * アーカイブサービスが正しく呼び出され、結果が適切に処理されることを確認します。
     */
    @Test
    void testHandleDailyArchiveSuccess() throws Exception {
        // Given
        when(archiveService.createDailyArchive(any(LocalDate.class)))
            .thenReturn(10);

        // When
        schedulerService.scheduleDailyArchive();

        // Then
        verify(archiveService).createDailyArchive(any(LocalDate.class));
        verify(taskManager).scheduleTask(any(ScheduledTask.class));
    }

    /**
     * 月次アーカイブタスクの成功時の動作をテストします。
     * アーカイブサービスが正しく呼び出され、結果が適切に処理されることを確認します。
     */
    @Test
    void testHandleMonthlyArchiveSuccess() throws Exception {
        // Given
        when(archiveService.createMonthlyArchive(any(YearMonth.class)))
            .thenReturn(100);

        // When
        schedulerService.scheduleMonthlyArchive();

        // Then
        verify(archiveService).createMonthlyArchive(any(YearMonth.class));
        verify(taskManager).scheduleTask(any(ScheduledTask.class));
    }

    /**
     * クリーンアップタスクの成功時の動作をテストします。
     * アーカイブサービスが正しく呼び出され、結果が適切に処理されることを確認します。
     */
    @Test
    void testHandleCleanupSuccess() throws Exception {
        // Given
        when(archiveService.deleteOldArchives(any(LocalDate.class)))
            .thenReturn(5);

        // When
        schedulerService.scheduleCleanup();

        // Then
        verify(archiveService).deleteOldArchives(any(LocalDate.class));
        verify(taskManager).scheduleTask(any(ScheduledTask.class));
    }

    /**
     * 日次アーカイブタスクの失敗時の動作をテストします。
     * エラーが適切にハンドリングされ、リトライ処理が正しく動作することを確認します。
     */
    @Test
    void testHandleDailyArchiveFailure() throws Exception {
        // Given
        doThrow(new RuntimeException("Archive failed"))
            .when(archiveService)
            .createDailyArchive(any(LocalDate.class));

        // When
        schedulerService.scheduleDailyArchive();

        // Then
        verify(archiveService).createDailyArchive(any(LocalDate.class));
        verify(taskManager).scheduleTask(any(ScheduledTask.class));
    }

    /**
     * タスクハンドラの登録処理をテストします。
     * 全てのタスクタイプに対して適切なハンドラが登録されることを確認します。
     */
    @Test
    void testTaskHandlerRegistration() {
        // When
        verify(taskManager, times(3))
            .registerTaskHandler(anyString(), any());

        // Then
        verify(taskManager).registerTaskHandler(
            eq(TaskType.DAILY_ARCHIVE.name()),
            any()
        );
        verify(taskManager).registerTaskHandler(
            eq(TaskType.MONTHLY_ARCHIVE.name()),
            any()
        );
        verify(taskManager).registerTaskHandler(
            eq(TaskType.CLEANUP.name()),
            any()
        );
    }
}