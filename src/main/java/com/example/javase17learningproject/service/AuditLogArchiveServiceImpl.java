package com.example.javase17learningproject.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.config.ArchiveConfig;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.AuditLogEntity;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.repository.AuditLogRepository;
import com.example.javase17learningproject.util.JsonArchiveUtils;

/**
 * 監査ログのアーカイブ処理を実装するサービスクラス。
 */
@Service
public class AuditLogArchiveServiceImpl implements AuditLogArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogArchiveServiceImpl.class);

    private final ArchiveConfig archiveConfig;
    private final AuditLogRepository auditLogRepository;
    private final JsonArchiveUtils jsonArchiveUtils;
    private final AtomicReference<ArchiveStatistics> statistics;

    public AuditLogArchiveServiceImpl(
        ArchiveConfig archiveConfig,
        AuditLogRepository auditLogRepository,
        JsonArchiveUtils jsonArchiveUtils
    ) {
        this.archiveConfig = archiveConfig;
        this.auditLogRepository = auditLogRepository;
        this.jsonArchiveUtils = jsonArchiveUtils;
        this.statistics = new AtomicReference<>(new ArchiveStatistics(0, 0, 0, 0.0, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public int createDailyArchive(LocalDate date) throws IOException {
        logger.info("日次アーカイブの作成を開始: {}", date);

        // 指定日のログを取得
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<AuditLogEntity> entities = auditLogRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        List<AuditLog> logs = entities.stream()
            .map(AuditLogEntity::toRecord)
            .toList();

        if (logs.isEmpty()) {
            logger.info("アーカイブ対象のログが存在しません: {}", date);
            return 0;
        }

        // アーカイブファイルの作成
        Path archivePath = archiveConfig.getDailyArchiveFilePath(date);
        Files.createDirectories(archivePath.getParent());
        jsonArchiveUtils.saveToGzipJson(logs, archivePath);

        updateStatistics();
        logger.info("日次アーカイブを作成しました: {} ({} レコード)", archivePath, logs.size());
        return logs.size();
    }

    @Override
    public int createMonthlyArchive(YearMonth yearMonth) throws IOException {
        logger.info("月次アーカイブの作成を開始: {}", yearMonth);
        Path monthlyArchivePath = archiveConfig.getMonthlyArchiveFilePath(yearMonth);
        Files.createDirectories(monthlyArchivePath.getParent());

        List<AuditLog> allLogs = new ArrayList<>();
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        // 日次アーカイブの読み込みとマージ
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Path dailyArchivePath = archiveConfig.getDailyArchiveFilePath(date);
            if (Files.exists(dailyArchivePath)) {
                List<AuditLog> logs = jsonArchiveUtils.loadFromGzipJson(dailyArchivePath);
                allLogs.addAll(logs);
            }
        }

        if (allLogs.isEmpty()) {
            logger.info("月次アーカイブ対象のログが存在しません: {}", yearMonth);
            return 0;
        }

        // 月次アーカイブの作成
        jsonArchiveUtils.saveToGzipJson(allLogs, monthlyArchivePath);
        updateStatistics();
        logger.info("月次アーカイブを作成しました: {} ({} レコード)", monthlyArchivePath, allLogs.size());
        return allLogs.size();
    }

    @Override
    public List<AuditLog> searchArchive(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String eventType,
        Severity severity
    ) throws IOException {
        List<AuditLog> results = new ArrayList<>();
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        // 日付範囲内のアーカイブを検索
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Path archivePath = archiveConfig.getDailyArchiveFilePath(date);
            if (Files.exists(archivePath)) {
                List<AuditLog> logs = jsonArchiveUtils.loadFromGzipJson(archivePath);
                logs.stream()
                    .filter(log -> isLogMatchingCriteria(log, startDateTime, endDateTime, eventType, severity))
                    .forEach(results::add);
            }
        }

        logger.info("アーカイブ検索結果: {} 件", results.size());
        return results;
    }

    private boolean isLogMatchingCriteria(
        AuditLog log,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String eventType,
        Severity severity
    ) {
        return log.createdAt().isAfter(startDateTime) &&
               log.createdAt().isBefore(endDateTime) &&
               (eventType == null || log.eventType().equals(eventType)) &&
               (severity == null || log.severity() == severity);
    }

    @Override
    public int deleteOldArchives(LocalDate beforeDate) throws IOException {
        logger.info("古いアーカイブの削除を開始: {} より前", beforeDate);
        int deletedCount = 0;

        // 日次アーカイブの削除
        try (Stream<Path> paths = Files.walk(archiveConfig.dailyArchivePath())) {
            deletedCount += paths
                .filter(Files::isRegularFile)
                .filter(path -> isArchiveBeforeDate(path, beforeDate))
                .mapToInt(path -> {
                    try {
                        Files.delete(path);
                        return 1;
                    } catch (IOException e) {
                        logger.error("アーカイブの削除に失敗: {}", path, e);
                        return 0;
                    }
                })
                .sum();
        }

        updateStatistics();
        logger.info("古いアーカイブを削除しました: {} ファイル", deletedCount);
        return deletedCount;
    }

    private boolean isArchiveBeforeDate(Path path, LocalDate date) {
        try {
            JsonArchiveUtils.ArchiveMetadata metadata = jsonArchiveUtils.readMetadata(path);
            return metadata.createdAt().toLocalDate().isBefore(date);
        } catch (IOException e) {
            logger.warn("メタデータの読み込みに失敗: {}", path, e);
            return false;
        }
    }

    @Override
    public boolean verifyArchive(LocalDate date) throws IOException {
        Path archivePath = archiveConfig.getDailyArchiveFilePath(date);
        if (!Files.exists(archivePath)) {
            logger.warn("アーカイブが存在しません: {}", archivePath);
            return false;
        }

        try {
            // アーカイブの読み込みを試行（チェックサム検証も含む）
            jsonArchiveUtils.loadFromGzipJson(archivePath);
            return true;
        } catch (IOException e) {
            logger.error("アーカイブの検証に失敗: {}", archivePath, e);
            return false;
        }
    }

    @Override
    public ArchiveStatistics getStatistics() {
        return statistics.get();
    }

    @Scheduled(fixedRate = 3600000) // 1時間ごと
    protected void updateStatistics() {
        try {
            long totalFiles = 0;
            long totalSize = 0;
            long totalLogs = 0;
            LocalDateTime lastArchiveDate = null;
            LocalDateTime oldestArchiveDate = null;

            // 全アーカイブファイルをスキャン
            try (Stream<Path> paths = Files.walk(archiveConfig.dailyArchivePath())) {
                List<Path> archiveFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json.gz"))
                    .toList();

                totalFiles = archiveFiles.size();
                
                for (Path path : archiveFiles) {
                    totalSize += Files.size(path);
                    JsonArchiveUtils.ArchiveMetadata metadata = jsonArchiveUtils.readMetadata(path);
                    totalLogs += metadata.recordCount();

                    if (lastArchiveDate == null || metadata.createdAt().isAfter(lastArchiveDate)) {
                        lastArchiveDate = metadata.createdAt();
                    }
                    if (oldestArchiveDate == null || metadata.createdAt().isBefore(oldestArchiveDate)) {
                        oldestArchiveDate = metadata.createdAt();
                    }
                }
            }

            // 圧縮率の計算（概算）
            double compressionRatio = totalSize > 0 ? 
                (double) totalLogs * 200 / totalSize : 0.0; // 1レコード約200バイトと仮定

            statistics.set(new ArchiveStatistics(
                totalFiles,
                totalLogs,
                totalSize,
                compressionRatio,
                lastArchiveDate,
                oldestArchiveDate
            ));

        } catch (IOException e) {
            logger.error("統計情報の更新に失敗しました", e);
        }
    }
}