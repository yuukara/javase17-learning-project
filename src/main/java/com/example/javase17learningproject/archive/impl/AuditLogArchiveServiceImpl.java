package com.example.javase17learningproject.archive.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.archive.ArchiveStatistics;
import com.example.javase17learningproject.archive.AuditLogArchiveService;
import com.example.javase17learningproject.archive.model.ArchiveMetadata;
import com.example.javase17learningproject.archive.util.ArchiveSearchUtils;
import com.example.javase17learningproject.archive.util.ChecksumUtils;
import com.example.javase17learningproject.archive.util.GzipUtils;
import com.example.javase17learningproject.archive.util.JsonUtils;
import com.example.javase17learningproject.archive.util.TarUtils;
import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;
import com.example.javase17learningproject.repository.AuditLogRepository;

@Service
public class AuditLogArchiveServiceImpl implements AuditLogArchiveService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogArchiveServiceImpl.class);
    private static final String ARCHIVE_BASE_DIR = "archives";
    private static final String DAILY_ARCHIVE_DIR = "daily";
    private static final String MONTHLY_ARCHIVE_DIR = "monthly";

    private final AtomicReference<ArchiveStatistics> statistics;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public AuditLogArchiveServiceImpl() {
        initializeArchiveDirectories();
        this.statistics = new AtomicReference<>(new ArchiveStatistics(0, 0, 0, 0.0, null, null));
        updateStatistics();
    }

    @Override
    @Transactional
    public int createDailyArchive(LocalDate date) throws IOException {
        logger.info("Starting daily archive creation for date: {}", date);

        // 指定日のログを取得
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);
        List<AuditLogEntity> logEntities = auditLogRepository.findByCreatedAtBetween(startDateTime, endDateTime);
        
        if (logEntities.isEmpty()) {
            logger.info("No logs found for date: {}", date);
            return 0;
        }

        // エンティティをモデルに変換
        List<AuditLog> logs = logEntities.stream()
                .map(AuditLogEntity::toRecord)
                .toList();

        // アーカイブファイルを作成
        Path archivePath = getDailyArchivePath(date);
        createDirectoriesIfNotExist(archivePath.getParent());

        // JSONデータの作成
        Map<String, Object> archiveData = Map.of(
            "metadata", createMetadata(ArchiveMetadata.ArchiveType.DAILY, startDateTime, endDateTime, logs.size()),
            "logs", logs
        );
        String jsonContent = JsonUtils.toJson(archiveData);

        // GZIP圧縮して保存
        byte[] compressed = GzipUtils.compress(jsonContent);
        Files.write(archivePath, compressed);

        updateStatistics();
        logger.info("Daily archive created successfully: {} ({} records)", archivePath, logs.size());
        return logs.size();
    }

    @Override
    @Transactional
    public int createMonthlyArchive(YearMonth yearMonth) throws IOException {
        logger.info("Starting monthly archive creation for: {}", yearMonth);

        // 日次アーカイブを収集
        List<File> dailyArchives = new ArrayList<>();
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Path archivePath = getDailyArchivePath(date);
            if (Files.exists(archivePath)) {
                dailyArchives.add(archivePath.toFile());
            }
        }

        if (dailyArchives.isEmpty()) {
            logger.info("No daily archives found for month: {}", yearMonth);
            return 0;
        }

        // 月次アーカイブを作成
        Path monthlyArchivePath = getMonthlyArchivePath(yearMonth);
        createDirectoriesIfNotExist(monthlyArchivePath.getParent());

        // 作業用の一時ディレクトリを作成
        Path tempDir = Files.createTempDirectory("monthly_archive_");
        try {
            // メタデータを作成
            ArchiveMetadata metadata = createMetadata(
                ArchiveMetadata.ArchiveType.MONTHLY,
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.atEndOfMonth().atTime(LocalTime.MAX),
                dailyArchives.size()
            );

            // メタデータをJSON形式で保存
            File metadataFile = tempDir.resolve("metadata.json").toFile();
            Files.writeString(metadataFile.toPath(), JsonUtils.toJson(metadata));

            // TARアーカイブを作成
            dailyArchives.add(metadataFile);
            File tarFile = tempDir.resolve("archive.tar").toFile();
            TarUtils.createTarArchive(dailyArchives, tarFile);

            // GZIP圧縮して保存
            byte[] tarContent = Files.readAllBytes(tarFile.toPath());
            byte[] compressed = GzipUtils.compress(new String(tarContent));
            Files.write(monthlyArchivePath, compressed);

            updateStatistics();
            logger.info("Monthly archive created successfully: {} ({} files)", monthlyArchivePath, dailyArchives.size());
            return dailyArchives.size();

        } finally {
            // 一時ファイルを削除
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        logger.warn("Failed to delete temporary file: {}", p, e);
                    }
                });
        }
    }

    @Override
    public List<AuditLog> searchArchive(
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity
    ) throws IOException {
        logger.info("Searching archives from {} to {}", start, end);
        List<AuditLog> results = new ArrayList<>();

        // 日次アーカイブを検索
        Path dailyArchiveBase = Paths.get(ARCHIVE_BASE_DIR, DAILY_ARCHIVE_DIR);
        if (Files.exists(dailyArchiveBase)) {
            List<AuditLog> dailyResults = searchDailyArchives(dailyArchiveBase, start, end, eventType, severity);
            results.addAll(dailyResults);
        }

        // 月次アーカイブを検索
        Path monthlyArchiveBase = Paths.get(ARCHIVE_BASE_DIR, MONTHLY_ARCHIVE_DIR);
        if (Files.exists(monthlyArchiveBase)) {
            List<AuditLog> monthlyResults = searchMonthlyArchives(monthlyArchiveBase, start, end, eventType, severity);
            results.addAll(monthlyResults);
        }

        // 結果をソート
        results.sort(Comparator.comparing(AuditLog::createdAt));
        logger.info("Found {} matching logs", results.size());

        return results;
    }

    @Override
    public boolean verifyArchive(LocalDate date) throws IOException {
        Path archivePath = getDailyArchivePath(date);
        if (!Files.exists(archivePath)) {
            logger.warn("Archive not found: {}", archivePath);
            return false;
        }

        try {
            byte[] compressed = Files.readAllBytes(archivePath);
            String content = GzipUtils.decompress(compressed);
            Map<String, Object> archiveData = JsonUtils.fromJson(content, Map.class);
            Map<String, Object> metadata = (Map<String, Object>) archiveData.get("metadata");
            String storedChecksum = (String) metadata.get("checksum");

            return ChecksumUtils.verifyChecksum(content, storedChecksum);
        } catch (Exception e) {
            logger.error("Archive verification failed: {}", archivePath, e);
            return false;
        }
    }

    @Override
    public int deleteOldArchives(LocalDate beforeDate) throws IOException {
        logger.info("Starting deletion of archives before: {}", beforeDate);
        int deletedCount = 0;

        // 日次アーカイブの削除
        try (Stream<Path> paths = Files.walk(Paths.get(ARCHIVE_BASE_DIR, DAILY_ARCHIVE_DIR))) {
            deletedCount = paths
                .filter(Files::isRegularFile)
                .filter(p -> {
                    try {
                        byte[] compressed = Files.readAllBytes(p);
                        String content = GzipUtils.decompress(compressed);
                        Map<String, Object> archiveData = JsonUtils.fromJson(content, Map.class);
                        Map<String, Object> metadata = (Map<String, Object>) archiveData.get("metadata");
                        LocalDateTime archiveDate = LocalDateTime.parse((String) metadata.get("startDate"));
                        return archiveDate.toLocalDate().isBefore(beforeDate);
                    } catch (Exception e) {
                        logger.warn("Failed to read archive metadata: {}", p, e);
                        return false;
                    }
                })
                .mapToInt(p -> {
                    try {
                        Files.delete(p);
                        return 1;
                    } catch (IOException e) {
                        logger.error("Failed to delete archive: {}", p, e);
                        return 0;
                    }
                })
                .sum();
        }

        updateStatistics();
        logger.info("Deleted {} archive files", deletedCount);
        return deletedCount;
    }

    @Override
    public ArchiveStatistics getStatistics() {
        return statistics.get();
    }

    private void initializeArchiveDirectories() {
        try {
            createDirectoriesIfNotExist(Paths.get(ARCHIVE_BASE_DIR));
            createDirectoriesIfNotExist(Paths.get(ARCHIVE_BASE_DIR, DAILY_ARCHIVE_DIR));
            createDirectoriesIfNotExist(Paths.get(ARCHIVE_BASE_DIR, MONTHLY_ARCHIVE_DIR));
            logger.info("Archive directories initialized successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize archive directories", e);
        }
    }

    private void createDirectoriesIfNotExist(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("Created directory: {}", path);
        }
    }

    private Path getDailyArchivePath(LocalDate date) {
        return Paths.get(
            ARCHIVE_BASE_DIR,
            DAILY_ARCHIVE_DIR,
            String.valueOf(date.getYear()),
            String.format("%02d", date.getMonthValue()),
            String.format("audit_log_%s.json.gz", date)
        );
    }

    private Path getMonthlyArchivePath(YearMonth yearMonth) {
        return Paths.get(
            ARCHIVE_BASE_DIR,
            MONTHLY_ARCHIVE_DIR,
            String.valueOf(yearMonth.getYear()),
            String.format("audit_log_%d%02d.tar.gz", yearMonth.getYear(), yearMonth.getMonthValue())
        );
    }

    private ArchiveMetadata createMetadata(
            ArchiveMetadata.ArchiveType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            long recordCount) {
        String content = JsonUtils.toJson(recordCount);
        String checksum = ChecksumUtils.calculateChecksum(content);

        return new ArchiveMetadata(
            type,
            startDate,
            endDate,
            recordCount,
            content.length(),
            checksum
        );
    }

    private List<AuditLog> searchDailyArchives(
            Path baseDir,
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity) throws IOException {
        List<AuditLog> results = new ArrayList<>();
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Path archivePath = getDailyArchivePath(date);
            if (Files.exists(archivePath)) {
                List<AuditLog> logs = ArchiveSearchUtils.searchDailyArchive(
                    archivePath, start, end, eventType, severity);
                results.addAll(logs);
            }
        }

        return results;
    }

    private List<AuditLog> searchMonthlyArchives(
            Path baseDir,
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity) throws IOException {
        List<AuditLog> results = new ArrayList<>();
        YearMonth startMonth = YearMonth.from(start);
        YearMonth endMonth = YearMonth.from(end);

        for (YearMonth yearMonth = startMonth; !yearMonth.isAfter(endMonth); yearMonth = yearMonth.plusMonths(1)) {
            Path archivePath = getMonthlyArchivePath(yearMonth);
            if (Files.exists(archivePath)) {
                List<AuditLog> logs = ArchiveSearchUtils.searchMonthlyArchive(
                    archivePath, start, end, eventType, severity);
                results.addAll(logs);
            }
        }

        return results;
    }

    private void updateStatistics() {
        try {
            long totalFiles = 0;
            long totalSize = 0;
            long totalLogs = 0;
            LocalDateTime lastArchiveDate = null;
            LocalDateTime oldestArchiveDate = null;

            Path dailyPath = Paths.get(ARCHIVE_BASE_DIR, DAILY_ARCHIVE_DIR);
            if (Files.exists(dailyPath)) {
                try (Stream<Path> paths = Files.walk(dailyPath)) {
                    List<Path> archiveFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json.gz"))
                        .toList();

                    totalFiles = archiveFiles.size();
                    
                    for (Path path : archiveFiles) {
                        totalSize += Files.size(path);
                        byte[] compressed = Files.readAllBytes(path);
                        String content = GzipUtils.decompress(compressed);
                        Map<String, Object> archiveData = JsonUtils.fromJson(content, Map.class);
                        Map<String, Object> metadata = (Map<String, Object>) archiveData.get("metadata");
                        
                        totalLogs += ((Number) metadata.get("recordCount")).longValue();
                        
                        LocalDateTime archiveDate = LocalDateTime.parse((String) metadata.get("startDate"));
                        if (lastArchiveDate == null || archiveDate.isAfter(lastArchiveDate)) {
                            lastArchiveDate = archiveDate;
                        }
                        if (oldestArchiveDate == null || archiveDate.isBefore(oldestArchiveDate)) {
                            oldestArchiveDate = archiveDate;
                        }
                    }
                }
            }

            double compressionRatio = totalSize > 0 ? 
                (double) totalLogs * 200 / totalSize : 0.0;

            statistics.set(new ArchiveStatistics(
                totalFiles,
                totalLogs,
                totalSize,
                compressionRatio,
                lastArchiveDate,
                oldestArchiveDate
            ));

            logger.debug("Statistics updated: files={}, logs={}, size={}", 
                totalFiles, totalLogs, totalSize);

        } catch (Exception e) {
            logger.error("Failed to update statistics", e);
        }
    }
}