package com.example.javase17learningproject.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * アプリケーション起動時に監査ログの環境を初期化するコンポーネント。
 * - アーカイブディレクトリの作成
 * - 現在の年月のディレクトリ構造の作成
 * - 初期状態の検証
 */
@Component
public class AuditLogInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogInitializer.class);

    private final ArchiveConfig archiveConfig;

    public AuditLogInitializer(ArchiveConfig archiveConfig) {
        this.archiveConfig = archiveConfig;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logger.info("監査ログ環境の初期化を開始");
        try {
            initializeArchiveDirectories();
            verifyEnvironment();
            logger.info("監査ログ環境の初期化が完了しました");
        } catch (IOException e) {
            logger.error("監査ログ環境の初期化に失敗しました", e);
            throw new RuntimeException("Failed to initialize audit log environment", e);
        }
    }

    /**
     * アーカイブディレクトリ構造を作成します。
     */
    private void initializeArchiveDirectories() throws IOException {
        logger.debug("アーカイブディレクトリの作成を開始");

        // 基本ディレクトリの作成
        List<Path> requiredPaths = List.of(
            archiveConfig.archiveBasePath(),
            archiveConfig.dailyArchivePath(),
            archiveConfig.monthlyArchivePath()
        );

        for (Path path : requiredPaths) {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.debug("ディレクトリを作成: {}", path);
            }
        }

        // 現在の年月のディレクトリ構造を作成
        YearMonth current = YearMonth.now();
        Path yearMonthPath = archiveConfig.dailyArchivePath()
            .resolve(String.valueOf(current.getYear()))
            .resolve(String.format("%02d", current.getMonthValue()));
        
        Files.createDirectories(yearMonthPath);
        logger.debug("現在の年月のディレクトリを作成: {}", yearMonthPath);
    }

    /**
     * 監査ログ環境の状態を検証します。
     */
    private void verifyEnvironment() throws IOException {
        logger.debug("環境の検証を開始");

        // 必要なディレクトリの存在確認
        verifyDirectory(archiveConfig.archiveBasePath(), "ベースディレクトリ");
        verifyDirectory(archiveConfig.dailyArchivePath(), "日次アーカイブディレクトリ");
        verifyDirectory(archiveConfig.monthlyArchivePath(), "月次アーカイブディレクトリ");

        // 書き込み権限の確認
        verifyWritePermission(archiveConfig.dailyArchivePath());
        verifyWritePermission(archiveConfig.monthlyArchivePath());

        logger.debug("環境の検証が完了");
    }

    private void verifyDirectory(Path path, String description) {
        if (!Files.exists(path)) {
            throw new IllegalStateException(
                String.format("%sが存在しません: %s", description, path)
            );
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException(
                String.format("%sがディレクトリではありません: %s", description, path)
            );
        }
    }

    private void verifyWritePermission(Path path) throws IOException {
        Path testFile = path.resolve(".write-test");
        try {
            Files.writeString(testFile, "test");
        } finally {
            Files.deleteIfExists(testFile);
        }
    }
}