package com.example.javase17learningproject.repository;

import java.util.List;
import java.util.Optional;

import com.example.javase17learningproject.model.AuditLog;

/**
 * 監査ログのインメモリストレージのインターフェース。
 * Phase 1での基本的なログ管理機能を提供します。
 */
public interface AuditLogInMemoryStorage {
    /**
     * 監査ログを保存します。
     *
     * @param log 保存する監査ログ
     * @return 保存された監査ログ
     */
    AuditLog save(AuditLog log);

    /**
     * 最新のログをLimit件数取得します。
     *
     * @param limit 取得する件数
     * @return 最新の監査ログのリスト
     */
    List<AuditLog> findLatestLogs(int limit);

    /**
     * 指定されたイベントタイプの監査ログを取得します。
     *
     * @param eventType イベントタイプ
     * @return 該当する監査ログのリスト
     */
    List<AuditLog> findByEventType(String eventType);

    /**
     * IDで監査ログを検索します。
     *
     * @param id 監査ログID
     * @return 該当する監査ログ（存在しない場合はEmpty）
     */
    Optional<AuditLog> findById(Long id);

    /**
     * 全ての監査ログを取得します。
     *
     * @return 全ての監査ログのリスト
     */
    List<AuditLog> findAll();
}