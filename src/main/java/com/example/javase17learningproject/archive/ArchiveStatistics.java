package com.example.javase17learningproject.archive;

import java.time.LocalDateTime;

/**
 * アーカイブの統計情報を表すレコード.
 * 
 * @param totalFiles アーカイブファイルの総数
 * @param totalLogs アーカイブされたログレコードの総数
 * @param totalSize アーカイブの合計サイズ（バイト）
 * @param compressionRatio 圧縮率
 * @param lastArchiveDate 最新のアーカイブ日時
 * @param oldestArchiveDate 最古のアーカイブ日時
 */
public record ArchiveStatistics(
    long totalFiles,
    long totalLogs,
    long totalSize,
    double compressionRatio,
    LocalDateTime lastArchiveDate,
    LocalDateTime oldestArchiveDate
) {
    /**
     * 空の統計情報を作成します.
     * @return 初期値が設定された統計情報
     */
    public static ArchiveStatistics empty() {
        return new ArchiveStatistics(0, 0, 0, 0.0, null, null);
    }

    /**
     * 人間が読みやすい形式でサイズを返します.
     * @return フォーマットされたサイズ文字列
     */
    public String getFormattedSize() {
        if (totalSize < 1024) {
            return totalSize + " B";
        } else if (totalSize < 1024 * 1024) {
            return String.format("%.2f KB", totalSize / 1024.0);
        } else if (totalSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", totalSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", totalSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * フォーマットされた圧縮率を返します.
     * @return 圧縮率のパーセンテージ表示
     */
    public String getFormattedCompressionRatio() {
        return String.format("%.1f%%", compressionRatio * 100);
    }
}