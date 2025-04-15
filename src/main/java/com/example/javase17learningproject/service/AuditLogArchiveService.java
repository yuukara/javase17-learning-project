package com.example.javase17learningproject.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

/**
 * 監査ログのアーカイブ処理を担当するサービス。
 */
public interface AuditLogArchiveService {

    /**
     * 指定された日付の監査ログをアーカイブします。
     * 
     * @param date アーカイブ対象の日付
     * @return アーカイブされたログの件数
     */
    int createDailyArchive(LocalDate date) throws IOException;

    /**
     * 指定された月の監査ログをアーカイブします。
     * すでに作成された日次アーカイブを月次アーカイブにまとめます。
     * 
     * @param yearMonth アーカイブ対象の年月
     * @return アーカイブされたログの件数
     */
    int createMonthlyArchive(YearMonth yearMonth) throws IOException;

    /**
     * アーカイブから監査ログを検索します。
     * 
     * @param startDateTime 検索開始日時
     * @param endDateTime 検索終了日時
     * @param eventType イベントタイプ（nullの場合は全て）
     * @param severity 重要度（nullの場合は全て）
     * @return 検索条件に一致する監査ログのリスト
     */
    List<AuditLog> searchArchive(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String eventType,
        Severity severity
    ) throws IOException;

    /**
     * 指定された期間よりも古いアーカイブを削除します。
     * 月次アーカイブが作成済みの場合は、対応する日次アーカイブも削除します。
     * 
     * @param beforeDate この日付より前のアーカイブを削除
     * @return 削除されたアーカイブファイルの数
     */
    int deleteOldArchives(LocalDate beforeDate) throws IOException;

    /**
     * アーカイブの整合性を検証します。
     * チェックサムの確認とメタデータの検証を行います。
     * 
     * @param date 検証対象の日付
     * @return 検証結果（true: 正常、false: 異常あり）
     */
    boolean verifyArchive(LocalDate date) throws IOException;

    /**
     * アーカイブの統計情報を取得します。
     * 
     * @return アーカイブの統計情報
     */
    ArchiveStatistics getStatistics();

    /**
     * アーカイブの統計情報を表す記録クラス
     */
    record ArchiveStatistics(
        long totalArchiveFiles,
        long totalArchivedLogs,
        long totalSizeInBytes,
        double averageCompressionRatio,
        LocalDateTime lastArchiveDate,
        LocalDateTime oldestArchiveDate
    ) {}
}