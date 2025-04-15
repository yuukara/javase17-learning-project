package com.example.javase17learningproject.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * アーカイブ機能の設定クラス。
 * アーカイブの保存場所やファイル名のフォーマットなどを管理します。
 */
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "audit.archive")
public class ArchiveConfig {

    /**
     * アーカイブのベースディレクトリ
     */
    @Value("${audit.archive.base-path:archives}")
    private String basePath;

    /**
     * 日次アーカイブのサブディレクトリ
     */
    @Value("${audit.archive.daily-dir:daily}")
    private String dailyDir;

    /**
     * 月次アーカイブのサブディレクトリ
     */
    @Value("${audit.archive.monthly-dir:monthly}")
    private String monthlyDir;

    /**
     * アーカイブファイルの保持期間（日）
     */
    @Value("${audit.archive.retention-days:90}")
    private int retentionDays;

    /**
     * 日次アーカイブのファイル名フォーマット
     */
    public static final DateTimeFormatter DAILY_FILE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 月次アーカイブのファイル名フォーマット
     */
    public static final DateTimeFormatter MONTHLY_FILE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * アーカイブのベースディレクトリを取得します。
     */
    @Bean
    public Path archiveBasePath() {
        return Paths.get(basePath);
    }

    /**
     * 日次アーカイブディレクトリを取得します。
     */
    @Bean
    public Path dailyArchivePath() {
        return Paths.get(basePath, dailyDir);
    }

    /**
     * 月次アーカイブディレクトリを取得します。
     */
    @Bean
    public Path monthlyArchivePath() {
        return Paths.get(basePath, monthlyDir);
    }

    // Getters and Setters
    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getDailyDir() {
        return dailyDir;
    }

    public void setDailyDir(String dailyDir) {
        this.dailyDir = dailyDir;
    }

    public String getMonthlyDir() {
        return monthlyDir;
    }

    public void setMonthlyDir(String monthlyDir) {
        this.monthlyDir = monthlyDir;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    /**
     * 日次アーカイブファイルの完全パスを生成します。
     */
    public Path getDailyArchiveFilePath(java.time.LocalDate date) {
        String fileName = String.format("audit_log_%s.json.gz", 
            date.format(DAILY_FILE_FORMAT));
        return Paths.get(basePath, dailyDir, 
            String.valueOf(date.getYear()),
            String.format("%02d", date.getMonthValue()),
            fileName);
    }

    /**
     * 月次アーカイブファイルの完全パスを生成します。
     */
    public Path getMonthlyArchiveFilePath(java.time.YearMonth yearMonth) {
        String fileName = String.format("audit_log_%s.tar.gz",
            yearMonth.format(MONTHLY_FILE_FORMAT));
        return Paths.get(basePath, monthlyDir,
            String.valueOf(yearMonth.getYear()),
            fileName);
    }
}