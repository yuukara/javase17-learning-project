package com.example.javase17learningproject.archive.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * アーカイブメタデータクラス.
 */
public class ArchiveMetadata {

    /** アーカイブタイプ（日次/月次） */
    private ArchiveType archiveType;

    /** 作成日時 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;

    /** アーカイブ対象の開始日時 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startDate;

    /** アーカイブ対象の終了日時 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime endDate;

    /** ログレコード数 */
    private long recordCount;

    /** ファイルサイズ（バイト） */
    private long fileSize;

    /** チェックサム (SHA-256) */
    private String checksum;

    /** アーカイブバージョン */
    private String version = "1.0";

    /** 
     * アーカイブタイプの列挙型.
     */
    public enum ArchiveType {
        /** 日次アーカイブ */
        DAILY,
        /** 月次アーカイブ */
        MONTHLY
    }

    // デフォルトコンストラクタ
    public ArchiveMetadata() {
    }

    /**
     * コンストラクタ.
     *
     * @param archiveType アーカイブタイプ
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @param recordCount レコード数
     * @param fileSize ファイルサイズ
     * @param checksum チェックサム
     */
    public ArchiveMetadata(
            ArchiveType archiveType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            long recordCount,
            long fileSize,
            String checksum) {
        this.archiveType = archiveType;
        this.createdAt = LocalDateTime.now();
        this.startDate = startDate;
        this.endDate = endDate;
        this.recordCount = recordCount;
        this.fileSize = fileSize;
        this.checksum = checksum;
    }

    // Getters and Setters

    public ArchiveType getArchiveType() {
        return archiveType;
    }

    public void setArchiveType(ArchiveType archiveType) {
        this.archiveType = archiveType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(long recordCount) {
        this.recordCount = recordCount;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}