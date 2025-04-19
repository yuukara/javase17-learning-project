package com.example.javase17learningproject.archive;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 監査ログアーカイブサービスのインターフェース.
 * 古い監査ログデータのアーカイブ作成と検索機能を提供します。
 */
public interface AuditLogArchiveService {

    /**
     * 指定された日付の日次アーカイブを作成します.
     *
     * @param date アーカイブを作成する日付
     * @return アーカイブされたレコード数
     * @throws IOException アーカイブ作成中にIOエラーが発生した場合
     */
    int createDailyArchive(LocalDate date) throws IOException;

    /**
     * 指定された月の月次アーカイブを作成します.
     *
     * @param yearMonth アーカイブを作成する年月
     * @return アーカイブされたレコード数
     * @throws IOException アーカイブ作成中にIOエラーが発生した場合
     */
    int createMonthlyArchive(YearMonth yearMonth) throws IOException;

    /**
     * アーカイブから条件に合致する監査ログを検索します.
     *
     * @param start 検索開始日時
     * @param end 検索終了日時
     * @param eventType イベントタイプ
     * @param severity 重要度
     * @return 検索条件に合致する監査ログのリスト
     * @throws IOException 検索処理中にIOエラーが発生した場合
     */
    List<AuditLog> searchArchive(
            LocalDateTime start,
            LocalDateTime end,
            String eventType,
            Severity severity
    ) throws IOException;

    /**
     * 指定された日付のアーカイブファイルの整合性を検証します.
     *
     * @param date 検証対象の日付
     * @return 検証が成功した場合はtrue、失敗した場合はfalse
     * @throws IOException 検証処理中にIOエラーが発生した場合
     */
    boolean verifyArchive(LocalDate date) throws IOException;

    /**
     * 指定された日付より古いアーカイブを削除します.
     *
     * @param beforeDate この日付より前のアーカイブが削除対象
     * @return 削除されたアーカイブファイルの数
     * @throws IOException 削除処理中にIOエラーが発生した場合
     */
    int deleteOldArchives(LocalDate beforeDate) throws IOException;

    /**
     * 現在のアーカイブの統計情報を取得します.
     *
     * @return アーカイブの統計情報
     */
    ArchiveStatistics getStatistics();
}