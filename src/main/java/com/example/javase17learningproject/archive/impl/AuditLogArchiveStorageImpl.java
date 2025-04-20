package com.example.javase17learningproject.archive.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.javase17learningproject.archive.ArchiveStatistics;
import com.example.javase17learningproject.archive.model.ArchiveMetadata;
import com.example.javase17learningproject.archive.util.ArchiveSearchUtils;
import com.example.javase17learningproject.archive.util.ChecksumUtils;
import com.example.javase17learningproject.archive.util.GzipUtils;
import com.example.javase17learningproject.archive.util.JsonUtils;
import com.example.javase17learningproject.archive.util.TarUtils;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;

/**
 * アーカイブファイルの保存と検証を管理するコンポーネント.
 *
 * <p>アーカイブファイルの保存、パス管理、整合性検証などの
 * 低レベルな処理を担当します。以下の責務に特化しています：</p>
 *
 * <ul>
 *   <li>ファイルシステム操作
 *     <ul>
 *       <li>アーカイブファイルの作成・削除</li>
 *       <li>ディレクトリ構造の管理</li>
 *       <li>ファイルの圧縮・解凍</li>
 *     </ul>
 *   </li>
 *   <li>データ整合性管理
 *     <ul>
 *       <li>チェックサムの計算と検証</li>
 *       <li>メタデータの生成と管理</li>
 *       <li>アーカイブの物理的な整合性チェック</li>
 *     </ul>
 *   </li>
 *   <li>ファイルシステムレベルの検索
 *     <ul>
 *       <li>アーカイブファイルの検索</li>
 *       <li>アーカイブ内容の検索</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>このクラスはファイルシステムレベルの操作のみを行い、
 * ビジネスロジックや整合性チェックは {@link AuditLogArchiveServiceImpl} に委ねます。</p>
 */
@Component
public class AuditLogArchiveStorageImpl {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogArchiveStorageImpl.class);
    private static final String DEFAULT_ARCHIVE_BASE_DIR = "archives";
    private static final String DAILY_ARCHIVE_DIR = "daily";
    private static final String MONTHLY_ARCHIVE_DIR = "monthly";

    private final String archiveBaseDir;

    public AuditLogArchiveStorageImpl() {
        this(DEFAULT_ARCHIVE_BASE_DIR);
    }

    public AuditLogArchiveStorageImpl(String archiveBaseDir) {
        this.archiveBaseDir = archiveBaseDir;
        initializeArchiveDirectories();
    }

    /**
     * 日次アーカイブを作成します.
     *
     * @param date 対象日付
     * @param logs アーカイブ対象のログ
     * @throws IOException アーカイブの作成に失敗した場合
     */
    public void createDailyArchive(LocalDate date, List<AuditLog> logs) throws IOException {
        Path archivePath = getDailyArchivePath(date);
        createDirectoriesIfNotExist(archivePath.getParent());

        ArchiveMetadata metadata = new ArchiveMetadata(
            ArchiveMetadata.ArchiveType.DAILY,
            date.atStartOfDay(),
            date.atTime(23, 59, 59),
            logs.size(),
            0L,
            ""
        );

        metadata = createMetadata(metadata, logs);
        Map<String, Object> archiveData = Map.of(
            "metadata", metadata,
            "logs", logs
        );

        saveArchiveData(archivePath, JsonUtils.toJson(archiveData));
        logger.info("Created daily archive: {}", archivePath);
    }

    /**
     * 月次アーカイブを作成します.
     *
     * @param yearMonth 対象年月
     * @return アーカイブされたファイル数
     * @throws IOException アーカイブの作成に失敗した場合
     */
    /**
     * 月次アーカイブを作成します.
     *
     * <p>この実装は純粋にファイル操作のみを行い、
     * アーカイブファイルの検証や整合性チェックは呼び出し元に委ねます。</p>
     *
     * @param yearMonth 対象年月
     * @param dailyArchives 月次アーカイブに含める日次アーカイブのパスのリスト
     * @return 作成されたアーカイブに含まれるファイル数
     * @throws IOException アーカイブの作成に失敗した場合
     */
    public int createMonthlyArchive(YearMonth yearMonth, List<Path> dailyArchives) throws IOException {
        if (dailyArchives.isEmpty()) {
            return 0;
        }

        // 月次アーカイブのパスを準備
        Path monthlyArchivePath = getMonthlyArchivePath(yearMonth);
        createDirectoriesIfNotExist(monthlyArchivePath.getParent());

        // 作業用の一時ディレクトリを作成
        Path tempDir = Files.createTempDirectory("monthly_archive_");
        try {
            // メタデータを作成
            ArchiveMetadata metadata = createMonthlyArchiveMetadata(yearMonth, dailyArchives);
            Path metadataPath = saveMetadataToTempFile(tempDir, metadata);

            // アーカイブファイルの作成
            int fileCount = createCompressedArchive(
                monthlyArchivePath,
                combineArchiveFiles(dailyArchives, metadataPath)
            );

            logger.info("Created monthly archive: {} with {} files", monthlyArchivePath, fileCount);
            return fileCount;

        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * 月次アーカイブのメタデータを作成します.
     */
    private ArchiveMetadata createMonthlyArchiveMetadata(YearMonth yearMonth, List<Path> dailyArchives) {
        return new ArchiveMetadata(
            ArchiveMetadata.ArchiveType.MONTHLY,
            yearMonth.atDay(1).atStartOfDay(),
            yearMonth.atEndOfMonth().atTime(23, 59, 59),
            dailyArchives.size(),
            0,
            calculateArchivesChecksum(dailyArchives)
        );
    }

    /**
     * メタデータを一時ファイルに保存します.
     */
    private Path saveMetadataToTempFile(Path tempDir, ArchiveMetadata metadata) throws IOException {
        Path metadataPath = tempDir.resolve("metadata.json");
        Files.writeString(metadataPath, JsonUtils.toJson(metadata));
        return metadataPath;
    }

    /**
     * アーカイブファイルリストを結合します.
     */
    private List<Path> combineArchiveFiles(List<Path> dailyArchives, Path metadataPath) {
        List<Path> archiveFiles = new ArrayList<>(dailyArchives);
        archiveFiles.add(metadataPath);
        return archiveFiles;
    }

    /**
     * 圧縮されたアーカイブを作成します.
     */
    private int createCompressedArchive(Path archivePath, List<Path> files) throws IOException {
        // TARアーカイブを作成
        Path tarPath = Files.createTempFile("archive_", ".tar");
        try {
            TarUtils.createTarArchive(
                files.stream().map(Path::toFile).toList(),
                tarPath.toFile()
            );

            // GZIP圧縮して保存
            byte[] tarContent = Files.readAllBytes(tarPath);
            byte[] compressed = GzipUtils.compress(new String(tarContent));
            Files.write(archivePath, compressed);

            return files.size();
        } finally {
            Files.deleteIfExists(tarPath);
        }
    }

    /**
     * 一時ディレクトリを再帰的に削除します.
     */
    private void cleanupTempDirectory(Path tempDir) {
        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete temporary file: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.warn("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }

    /**
     * アーカイブファイルのチェックサムを計算します.
     */
    private String calculateArchivesChecksum(List<Path> archives) {
        try {
            String content = archives.stream()
                .map(this::readArchiveContent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining());
            return ChecksumUtils.calculateChecksum(content);
        } catch (Exception e) {
            logger.warn("Failed to calculate archives checksum", e);
            return "";
        }
    }

    /**
     * アーカイブファイルの内容を読み込みます.
     */
    private Optional<String> readArchiveContent(Path path) {
        try {
            return Optional.of(readArchiveData(path));
        } catch (IOException e) {
            logger.warn("Failed to read archive: {}", path, e);
            return Optional.empty();
        }
    }

    /**
     * アーカイブを検索します.
     *
     * @param start 開始日時
     * @param end 終了日時
     * @param eventType イベントタイプ
     * @param severity 重要度
     * @return 検索結果
     * @throws IOException 検索に失敗した場合
     */
    /**
     * アーカイブファイルから指定された条件に一致するログを検索します.
     *
     * <p>この実装は純粋にファイルシステムレベルの検索のみを行い、
     * 検索結果の後処理やビジネスロジックは呼び出し元に委ねます。</p>
     *
     * @param start 検索開始日時
     * @param end 検索終了日時
     * @param eventType イベントタイプ
     * @param severity 重要度
     * @return 検索結果のリスト
     * @throws IOException 検索に失敗した場合
     */
    public List<AuditLog> searchArchives(
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            AuditEvent.Severity severity) throws IOException {
        List<AuditLog> results = new ArrayList<>();
        
        // 対象期間のアーカイブファイルを収集
        List<Path> archiveFiles = collectArchiveFiles(start, end);
        
        // 各アーカイブファイルから条件に一致するログを抽出
        for (Path archiveFile : archiveFiles) {
            try {
                if (isMonthlyArchive(archiveFile)) {
                    results.addAll(ArchiveSearchUtils.searchMonthlyArchive(
                        archiveFile, start, end, eventType, severity));
                } else {
                    results.addAll(ArchiveSearchUtils.searchDailyArchive(
                        archiveFile, start, end, eventType, severity));
                }
            } catch (IOException e) {
                logger.warn("Failed to search archive file: {}", archiveFile, e);
                // 個別のファイル検索エラーは無視して続行
            }
        }

        return results;
    }

    /**
     * 指定された期間に該当するアーカイブファイルを収集します.
     *
     * @param start 開始日時
     * @param end 終了日時
     * @return アーカイブファイルのパスのリスト
     * @throws IOException ファイル収集に失敗した場合
     */
    private List<Path> collectArchiveFiles(LocalDateTime start, LocalDateTime end) throws IOException {
        List<Path> files = new ArrayList<>();
        
        // 日次アーカイブの収集
        Path dailyPath = Paths.get(archiveBaseDir, DAILY_ARCHIVE_DIR);
        if (Files.exists(dailyPath)) {
            try (Stream<Path> paths = Files.walk(dailyPath)) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> isArchiveInDateRange(p, start, end))
                     .forEach(files::add);
            }
        }

        // 月次アーカイブの収集
        Path monthlyPath = Paths.get(archiveBaseDir, MONTHLY_ARCHIVE_DIR);
        if (Files.exists(monthlyPath)) {
            try (Stream<Path> paths = Files.walk(monthlyPath)) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> isArchiveInDateRange(p, start, end))
                     .forEach(files::add);
            }
        }

        return files;
    }

    /**
     * 指定されたパスが月次アーカイブかどうかを判定します.
     */
    private boolean isMonthlyArchive(Path path) {
        return path.toString().contains(MONTHLY_ARCHIVE_DIR);
    }

    /**
     * アーカイブファイルが指定された日付範囲に含まれるかどうかを判定します.
     */
    private boolean isArchiveInDateRange(Path path, LocalDateTime start, LocalDateTime end) {
        try {
            String content = readArchiveData(path);
            Map<String, Object> archiveData = JsonUtils.fromJson(content, Map.class);
            Map<String, Object> metadata = (Map<String, Object>) archiveData.get("metadata");
            
            String dateStr = ((String) metadata.get("startDate")).replace("Z", "");
            LocalDateTime archiveDate = LocalDateTime.parse(dateStr);
            
            return !archiveDate.isBefore(start) && !archiveDate.isAfter(end);
        } catch (Exception e) {
            logger.warn("Failed to read archive metadata: {}", path, e);
            return false;
        }
    }

    /**
     * 古いアーカイブを削除します.
     *
     * @param beforeDate この日付より前のアーカイブを削除
     * @return 削除したファイル数
     * @throws IOException 削除に失敗した場合
     */
    public int deleteOldArchives(LocalDate beforeDate) throws IOException {
        int count = 0;
        Path dailyPath = Paths.get(archiveBaseDir, DAILY_ARCHIVE_DIR);

        if (Files.exists(dailyPath)) {
            List<Path> filesToDelete;
            try (Stream<Path> paths = Files.walk(dailyPath)) {
                filesToDelete = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> isOldArchive(p, beforeDate))
                    .toList();
            }

            for (Path path : filesToDelete) {
                if (deleteArchive(path) > 0) {
                    count++;
                    logger.info("Deleted old archive: {}", path);
                }
            }
        }

        return count;
    }

    /**
     * アーカイブファイルを検証します.
     *
     * @param date 対象日付
     * @return 検証結果
     * @throws IOException 検証に失敗した場合
     */
    public boolean verifyArchive(LocalDate date) throws IOException {
        Path archivePath = getDailyArchivePath(date);
        if (!Files.exists(archivePath)) {
            return false;
        }

        String content = readArchiveData(archivePath);
        Map<String, Object> archiveData = JsonUtils.fromJson(content, Map.class);

        if (!archiveData.containsKey("metadata") || !archiveData.containsKey("logs")) {
            return false;
        }

        Map<String, Object> metadata = (Map<String, Object>) archiveData.get("metadata");
        List<Map<String, Object>> logs = (List<Map<String, Object>>) archiveData.get("logs");

        String storedChecksum = (String) metadata.get("checksum");
        String calculatedChecksum = ChecksumUtils.calculateChecksum(JsonUtils.toJson(logs));

        return storedChecksum.equals(calculatedChecksum);
    }

    /**
     * アーカイブの統計情報を計算します.
     *
     * @return 統計情報
     * @throws IOException 計算に失敗した場合
     */
    public ArchiveStatistics calculateStatistics() throws IOException {
        long totalFiles = 0;
        long totalLogs = 0;
        long totalSize = 0;
        LocalDateTime lastArchiveDate = null;
        LocalDateTime oldestArchiveDate = null;

        Path baseDir = Paths.get(archiveBaseDir);
        Path dailyDir = baseDir.resolve(DAILY_ARCHIVE_DIR);

        if (Files.exists(dailyDir)) {
            List<Path> archiveFiles;
            try (Stream<Path> paths = Files.walk(dailyDir)) {
                archiveFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json.gz"))
                    .sorted()
                    .toList();
            }

            totalFiles = archiveFiles.size();
            
            for (Path path : archiveFiles) {
                try {
                    totalSize += Files.size(path);
                    String content = readArchiveData(path);
                    Map<String, Object> archiveData = JsonUtils.fromJson(content, Map.class);
                    
                    if (archiveData != null && archiveData.containsKey("metadata")) {
                        Map<String, Object> metadata = (Map<String, Object>) archiveData.get("metadata");
                        Number recordCount = (Number) metadata.get("recordCount");
                        if (recordCount != null) {
                            totalLogs += recordCount.longValue();
                        }

                        String dateStr = ((String) metadata.get("startDate"));
                        if (dateStr != null) {
                            dateStr = dateStr.replace("Z", "");
                            LocalDateTime archiveDate = LocalDateTime.parse(dateStr);
                            if (lastArchiveDate == null || archiveDate.isAfter(lastArchiveDate)) {
                                lastArchiveDate = archiveDate;
                            }
                            if (oldestArchiveDate == null || archiveDate.isBefore(oldestArchiveDate)) {
                                oldestArchiveDate = archiveDate;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to process archive file: {}", path, e);
                }
            }
        }

        double compressionRatio = totalSize > 0 ? 
            (double) totalLogs * 200 / totalSize : 0.0;

        return new ArchiveStatistics(
            totalFiles,
            totalLogs,
            totalSize,
            compressionRatio,
            lastArchiveDate,
            oldestArchiveDate
        );
    }

    /**
     * アーカイブディレクトリ構造を初期化します.
     *
     * <p>以下のディレクトリ構造を作成します：
     * <ul>
     *   <li>archives/ - ベースディレクトリ</li>
     *   <li>archives/daily/ - 日次アーカイブ用</li>
     *   <li>archives/monthly/ - 月次アーカイブ用</li>
     * </ul></p>
     *
     * @throws RuntimeException ディレクトリの作成に失敗した場合
     */
    private void initializeArchiveDirectories() {
        try {
            createDirectoriesIfNotExist(Paths.get(archiveBaseDir));
            createDirectoriesIfNotExist(Paths.get(archiveBaseDir, DAILY_ARCHIVE_DIR));
            createDirectoriesIfNotExist(Paths.get(archiveBaseDir, MONTHLY_ARCHIVE_DIR));
            logger.info("Archive directories initialized successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize archive directories", e);
        }
    }

    /**
     * 指定されたパスにディレクトリが存在しない場合、作成します.
     *
     * @param path 作成するディレクトリのパス
     * @throws IOException ディレクトリの作成に失敗した場合
     */
    private void createDirectoriesIfNotExist(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("Created directory: {}", path);
        }
    }

    /**
     * 指定された日付の日次アーカイブファイルのパスを取得します.
     *
     * @param date 対象日付
     * @return アーカイブファイルのパス（archives/daily/YYYY/MM/audit_log_YYYY-MM-DD.json.gz）
     */
    public Path getDailyArchivePath(LocalDate date) {
        return Paths.get(
            archiveBaseDir,
            DAILY_ARCHIVE_DIR,
            String.valueOf(date.getYear()),
            String.format("%02d", date.getMonthValue()),
            String.format("audit_log_%s.json.gz", date)
        );
    }

    /**
     * 指定された年月の月次アーカイブファイルのパスを取得します.
     *
     * @param yearMonth 対象年月
     * @return アーカイブファイルのパス（archives/monthly/YYYY/audit_log_YYYYMM.tar.gz）
     */
    private Path getMonthlyArchivePath(YearMonth yearMonth) {
        return Paths.get(
            archiveBaseDir,
            MONTHLY_ARCHIVE_DIR,
            String.valueOf(yearMonth.getYear()),
            String.format("audit_log_%d%02d.tar.gz", yearMonth.getYear(), yearMonth.getMonthValue())
        );
    }

    /**
     * アーカイブデータを圧縮して保存します.
     *
     * @param path 保存先のパス
     * @param data 保存するJSONデータ
     * @throws IOException データの圧縮または保存に失敗した場合
     */
    private void saveArchiveData(Path path, String data) throws IOException {
        byte[] compressed = GzipUtils.compress(data);
        Files.write(path, compressed);
    }

    /**
     * アーカイブファイルを読み込み、解凍します.
     *
     * @param path アーカイブファイルのパス
     * @return 解凍されたJSONデータ
     * @throws IOException ファイルの読み込みまたは解凍に失敗した場合
     */
    private String readArchiveData(Path path) throws IOException {
        byte[] compressed = Files.readAllBytes(path);
        return GzipUtils.decompress(compressed);
    }

    /**
     * アーカイブのメタデータを作成します.
     *
     * <p>既存のメタデータに基づいて、データのチェックサムを計算し、
     * 新しいメタデータオブジェクトを生成します。</p>
     *
     * @param metadata 既存のメタデータ
     * @param data メタデータを作成する対象データ
     * @return 更新されたメタデータ（チェックサムと実際のサイズを含む）
     */
    private ArchiveMetadata createMetadata(ArchiveMetadata metadata, Object data) {
        String jsonContent = JsonUtils.toJson(data);
        String checksum = ChecksumUtils.calculateChecksum(jsonContent);

        // ArchiveMetadataの変更
        return new ArchiveMetadata(
            metadata.getArchiveType(),
            metadata.getStartDate(),
            metadata.getEndDate(),
            metadata.getRecordCount(),
            jsonContent.length(),
            checksum
        );
    }

    /**
     * 指定された年月の日次アーカイブファイルを収集します.
     *
     * <p>指定された月の全ての日について、存在する日次アーカイブファイルを収集します。
     * その際、各アーカイブファイルの整合性を検証し、無効なファイルは除外します。</p>
     *
     * @param yearMonth 対象年月
     * @return 有効な日次アーカイブファイルのパスのリスト
     * @throws IOException ファイルの読み込みまたは検証に失敗した場合
     */
    private List<Path> collectDailyArchives(YearMonth yearMonth) throws IOException {
        List<Path> archives = new ArrayList<>();
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Path archivePath = getDailyArchivePath(date);
            if (Files.exists(archivePath) && Files.isRegularFile(archivePath)) {
                try {
                    // アーカイブの整合性を検証
                    if (verifyArchive(date)) {
                        archives.add(archivePath);
                    } else {
                        logger.warn("Skipping invalid archive: {}", archivePath);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to verify archive: {}", archivePath, e);
                }
            }
        }

        return archives;
    }

    /**
     * アーカイブファイルが指定された日付より古いかどうかを判定します.
     *
     * <p>アーカイブのメタデータから開始日時を取得し、指定された日付と比較します。
     * ファイルの読み込みや解析に失敗した場合は、falseを返します。</p>
     *
     * @param path アーカイブファイルのパス
     * @param beforeDate この日付より前かどうかを判定
     * @return 指定された日付より古い場合はtrue
     */
    private boolean isOldArchive(Path path, LocalDate beforeDate) {
        try {
            String content = readArchiveData(path);
            Map<String, Object> archiveData = JsonUtils.fromJson(content, Map.class);
            Map<String, Object> metadata = (Map<String, Object>) archiveData.get("metadata");
            String dateStr = ((String) metadata.get("startDate")).replace("Z", "");
            LocalDateTime archiveDate = LocalDateTime.parse(dateStr);
            return archiveDate.toLocalDate().isBefore(beforeDate);
        } catch (Exception e) {
            logger.warn("Failed to read archive metadata: {}", path, e);
            return false;
        }
    }

    /**
     * アーカイブファイルを削除します.
     *
     * @param path 削除するファイルのパス
     * @return 削除に成功した場合は1、失敗した場合は0
     */
    private int deleteArchive(Path path) {
        try {
            Files.delete(path);
            return 1;
        } catch (IOException e) {
            logger.error("Failed to delete archive: {}", path, e);
            return 0;
        }
    }
}