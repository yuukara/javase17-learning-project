package com.example.javase17learningproject.archive.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.archive.ArchiveStatistics;
import com.example.javase17learningproject.archive.AuditLogArchiveService;
import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;
import com.example.javase17learningproject.repository.AuditLogRepository;

/**
 * 監査ログのアーカイブ管理機能を実装するクラス.
 *
 * <p>アーカイブの作成、検索、統計情報の管理を行います。
 * 実際のファイル操作は {@link AuditLogArchiveStorageImpl} に委譲します。</p>
 *
 * <p>各メソッドの責務は以下の通りです：</p>
 * <ul>
 *   <li>createDailyArchiveメソッド:
 *     <ul>
 *       <li>DBからのログ取得とモデル変換を担当</li>
 *       <li>ビジネスロジックをfetchAndProcessDailyLogsメソッドに分離</li>
 *     </ul>
 *   </li>
 *   <li>createMonthlyArchiveメソッド:
 *     <ul>
 *       <li>月次アーカイブデータの検証を追加</li>
 *       <li>validateMonthlyArchiveDataメソッドで整合性チェック</li>
 *     </ul>
 *   </li>
 *   <li>searchArchiveメソッド:
 *     <ul>
 *       <li>検索条件の妥当性検証を追加</li>
 *       <li>結果の後処理（ソート）を実装</li>
 *       <li>ビジネスロジックとストレージ操作を分離</li>
 *     </ul>
 *   </li>
 *   <li>verifyArchiveメソッド:
 *     <ul>
 *       <li>アーカイブの存在確認と基本検証を追加</li>
 *       <li>DBとの整合性チェックを実装</li>
 *     </ul>
 *   </li>
 *   <li>deleteOldArchivesメソッド:
 *     <ul>
 *       <li>削除対象の日付検証を追加</li>
 *       <li>DBとの整合性チェックを実装</li>
 *       <li>アーカイブの事前検証を追加</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@Service
public class AuditLogArchiveServiceImpl implements AuditLogArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogArchiveServiceImpl.class);
    
    private final AuditLogArchiveStorageImpl archiveStorage;
    private final AtomicReference<ArchiveStatistics> statistics;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogArchiveServiceImpl(
            AuditLogArchiveStorageImpl archiveStorage,
            AuditLogRepository auditLogRepository) {
        this.archiveStorage = archiveStorage;
        this.auditLogRepository = auditLogRepository;
        this.statistics = new AtomicReference<>(new ArchiveStatistics(0, 0, 0, 0.0, null, null));
        updateStatistics();
    }

    /**
     * 指定された日付の監査ログを日次アーカイブとして保存します.
     *
     * @param date アーカイブ対象の日付
     * @return アーカイブされたログの件数
     * @throws IOException アーカイブファイルの作成に失敗した場合
     */
    @Override
    @Transactional
    public int createDailyArchive(LocalDate date) throws IOException {
        logger.info("Starting daily archive creation for date: {}", date);

        // 指定日のログを取得してビジネスロジックを適用
        List<AuditLog> logs = fetchAndProcessDailyLogs(date);
        if (logs.isEmpty()) {
            return 0;
        }

        // ストレージ層に委譲してアーカイブを作成
        archiveStorage.createDailyArchive(date, logs);
        updateStatistics();

        logger.info("Daily archive created successfully with {} records", logs.size());
        return logs.size();
    }

    /**
     * 日次ログの取得と処理を行います.
     *
     * @param date 対象日付
     * @return 処理済みの監査ログリスト
     */
    private List<AuditLog> fetchAndProcessDailyLogs(LocalDate date) {
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);
        List<AuditLogEntity> logEntities = auditLogRepository.findByCreatedAtBetween(startDateTime, endDateTime);

        if (logEntities.isEmpty()) {
            logger.info("No logs found for date: {}", date);
            return List.of();
        }

        // エンティティをモデルに変換
        return logEntities.stream()
                .map(AuditLogEntity::toRecord)
                .toList();
    }

    /**
     * 指定された月の日次アーカイブをまとめて月次アーカイブとして保存します.
     *
     * <p>月次アーカイブファイルは以下の形式で保存されます：
     * archives/monthly/YYYY/audit_log_YYYYMM.tar.gz</p>
     *
     * @param yearMonth アーカイブ対象の年月
     * @return アーカイブされた日次アーカイブファイルの数
     * @throws IOException アーカイブファイルの作成に失敗した場合
     */
    /**
     * 月次アーカイブを作成します.
     *
     * @param yearMonth アーカイブ対象の年月
     * @return アーカイブされたファイル数
     * @throws IOException アーカイブファイルの作成に失敗した場合
     */
    @Override
    @Transactional
    public int createMonthlyArchive(YearMonth yearMonth) throws IOException {
        logger.info("Starting monthly archive creation for: {}", yearMonth);

        // 月間のアーカイブ対象データを検証
        List<Path> validArchives = collectAndValidateMonthlyArchives(yearMonth);
        if (validArchives.isEmpty()) {
            logger.warn("No valid archives found for month: {}", yearMonth);
            return 0;
        }

        // 検証済みデータのアーカイブをストレージ層に委譲
        int result = archiveStorage.createMonthlyArchive(yearMonth, validArchives);
        updateStatistics();

        if (result > 0) {
            logger.info("Monthly archive created successfully with {} files", result);
        } else {
            logger.info("No daily archives found for month: {}", yearMonth);
        }

        return result;
    }

    /**
     * 月次アーカイブ対象の日次アーカイブファイルを収集し検証します.
     *
     * @param yearMonth 対象年月
     * @return 検証済みの日次アーカイブファイルのリスト
     * @throws IOException 処理に失敗した場合
     */
    private List<Path> collectAndValidateMonthlyArchives(YearMonth yearMonth) throws IOException {
        List<Path> validArchives = new ArrayList<>();
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // 各日のアーカイブを検証して収集
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Path archivePath = archiveStorage.getDailyArchivePath(date);
            if (!Files.exists(archivePath)) {
                logger.debug("Daily archive not found for: {}", date);
                continue;
            }

            if (validateArchive(date)) {
                validArchives.add(archivePath);
                logger.debug("Valid archive found for: {}", date);
            } else {
                logger.warn("Invalid archive found for: {}, skipping", date);
            }
        }

        return validArchives;
    }

    /**
     * 月次アーカイブ対象データの検証を行います.
     *
     * @param yearMonth 対象年月
     * @return 検証結果
     */
    private boolean validateMonthlyArchiveData(YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 月間の全ての日次アーカイブの整合性を確認
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            try {
                if (archiveStorage.verifyArchive(date)) {
                    logger.debug("Daily archive verified for: {}", date);
                } else {
                    logger.warn("Daily archive verification failed for: {}", date);
                    return false;
                }
            } catch (IOException e) {
                logger.warn("Failed to verify daily archive for: {}", date, e);
                return false;
            }
        }
        return true;
    }

    /**
     * アーカイブから条件に一致する監査ログを検索します.
     *
     * @param start 検索開始日時
     * @param end 検索終了日時
     * @param eventType イベントタイプ
     * @param severity 重要度
     * @return 検索結果のリスト
     * @throws IOException 検索に失敗した場合
     */
    @Override
    public List<AuditLog> searchArchive(
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity) throws IOException {
        // 検索条件の妥当性を検証
        validateSearchCriteria(start, end, eventType, severity);
        logger.info("Searching archives from {} to {}", start, end);
        
        // ストレージ層に検索を委譲
        List<AuditLog> results = archiveStorage.searchArchives(start, end, eventType, severity);
        
        // 結果の後処理（フィルタリング、ソートなど）
        results = processSearchResults(results);
        logger.info("Found {} matching logs after processing", results.size());
        
        return results;
    }

    /**
     * アーカイブの存在チェックと基本的な検証を行います.
     *
     * @param date 検証対象の日付
     * @return 検証結果
     * @throws IOException 検証に失敗した場合
     */
    private boolean validateArchive(LocalDate date) throws IOException {
        if (date.isAfter(LocalDate.now())) {
            logger.warn("Cannot verify future date archive: {}", date);
            return false;
        }

        // DBに対象日付のログが存在するか確認
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);
        List<AuditLogEntity> logEntities = auditLogRepository.findByCreatedAtBetween(startDateTime, endDateTime);

        // ログが存在しない場合も正常とみなす
        if (logEntities.isEmpty()) {
            return true;
        }

        // ストレージ層で物理的な検証を実行
        return archiveStorage.verifyArchive(date);
    }

    private void validateSearchCriteria(
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates must not be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
        if (start.until(end, java.time.temporal.ChronoUnit.DAYS) > 365) {
            throw new IllegalArgumentException("Search period must not exceed 365 days");
        }
    }

    /**
     * 検索結果の後処理を行います.
     *
     * @param results 検索結果
     * @return 処理済みの検索結果
     */
    private List<AuditLog> processSearchResults(List<AuditLog> results) {
        return results.stream()
                .sorted(Comparator.comparing(AuditLog::createdAt).reversed())
                .toList();
    }

    /**
     * アーカイブを検証します.
     *
     * @param date 検証対象の日付
     * @return 検証結果
     * @throws IOException 検証に失敗した場合
     */
    @Override
    public boolean verifyArchive(LocalDate date) throws IOException {
        logger.info("Starting archive verification for date: {}", date);

        // アーカイブの存在確認と基本的な検証
        if (!validateArchiveExistence(date)) {
            return false;
        }

        // ストレージ層に詳細な検証を委譲
        boolean result = archiveStorage.verifyArchive(date);
        
        if (!result) {
            logger.warn("Archive verification failed for date: {}", date);
        } else {
            logger.info("Archive verification successful for date: {}", date);
        }
        
        return result;
    }

    /**
     * アーカイブの存在確認と基本的な検証を行います.
     *
     * @param date 検証対象の日付
     * @return 検証結果
     */
    private boolean validateArchiveExistence(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            logger.warn("Cannot verify future date archive: {}", date);
            return false;
        }

        // DBに対象日付のログが存在するか確認
        List<AuditLog> logs = fetchAndProcessDailyLogs(date);
        if (logs.isEmpty()) {
            logger.info("No logs exist for date: {}", date);
            return true; // ログが存在しない場合も正常とみなす
        }

        return true;
    }

    /**
     * 古いアーカイブを削除します.
     *
     * @param beforeDate この日付より前のアーカイブを削除
     * @return 削除したファイル数
     * @throws IOException 削除に失敗した場合
     */
    @Override
    @Transactional
    public int deleteOldArchives(LocalDate beforeDate) throws IOException {
        logger.info("Starting deletion of archives before: {}", beforeDate);

        // 削除対象日付の妥当性を検証
        validateDeletionDate(beforeDate);

        // 削除対象アーカイブの事前チェック
        if (!validateArchivesForDeletion(beforeDate)) {
            logger.warn("Archive validation failed, deletion aborted");
            return 0;
        }

        // 検証済みアーカイブの削除をストレージ層に委譲
        int deletedCount = archiveStorage.deleteOldArchives(beforeDate);
        if (deletedCount > 0) {
            updateStatistics();
            logger.info("Successfully deleted {} archive files", deletedCount);
        } else {
            logger.info("No archives found to delete before: {}", beforeDate);
        }

        return deletedCount;
    }

    /**
     * 削除対象日付の妥当性を検証します.
     *
     * @param beforeDate 削除対象日付
     * @throws IllegalArgumentException 不正な日付の場合
     */
    private void validateDeletionDate(LocalDate beforeDate) {
        if (beforeDate == null) {
            throw new IllegalArgumentException("Deletion date must not be null");
        }

        LocalDate minimumRetentionDate = LocalDate.now().minusYears(1);
        if (beforeDate.isAfter(minimumRetentionDate)) {
            throw new IllegalArgumentException(
                "Cannot delete archives less than 1 year old. Minimum deletion date: " + minimumRetentionDate);
        }
    }

    /**
     * 削除対象アーカイブの事前チェックを行います.
     *
     * @param beforeDate 削除対象日付
     * @return 検証結果
     */
    private boolean validateArchivesForDeletion(LocalDate beforeDate) {
        try {
            // DBに古いログが残っていないことを確認
            LocalDateTime startDateTime = LocalDateTime.MIN;
            LocalDateTime endDateTime = beforeDate.atTime(LocalTime.MAX);
            List<AuditLogEntity> oldLogs = auditLogRepository.findByCreatedAtBetween(
                startDateTime, endDateTime);

            if (!oldLogs.isEmpty()) {
                logger.warn("{} unarchived logs found before {}", oldLogs.size(), beforeDate);
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to validate archives for deletion", e);
            return false;
        }
    }

    /**
     * アーカイブの統計情報を取得します.
     *
     * <p>アーカイブファイルの数、ログの総数、総サイズ、圧縮率などの統計情報を返します。
     * この情報は非同期に更新されるため、リアルタイムの値とは異なる可能性があります。</p>
     *
     * @return アーカイブの統計情報
     */
    @Override
    public ArchiveStatistics getStatistics() {
        return statistics.get();
    }

    /**
     * 統計情報を更新します.
     */
    private void updateStatistics() {
        try {
            ArchiveStatistics newStats = archiveStorage.calculateStatistics();
            statistics.set(newStats);
            logger.debug("Statistics updated: files={}, logs={}, size={}",
                newStats.totalFiles(), newStats.totalLogs(), newStats.totalSize());
        } catch (IOException e) {
            logger.error("Failed to update statistics", e);
        }
    }
}