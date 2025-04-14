package com.example.javase17learningproject.repository;

import java.util.List;
import java.util.Optional;

import com.example.javase17learningproject.model.AuditLog;

/**
 * 監査ログのインメモリストレージを提供するインターフェース。
 */
public interface AuditLogInMemoryStorage {

    /**
     * 監査ログを保存します。
     */
    AuditLog save(AuditLog log);

    /**
     * 指定されたIDの監査ログを取得します。
     */
    Optional<AuditLog> findById(Long id);

    /**
     * イベントタイプに基づいて監査ログを検索します。
     */
    List<AuditLog> findByEventType(String eventType);

    /**
     * 最新の監査ログを指定された件数取得します。
     */
    List<AuditLog> findLatestLogs(int limit);

    /**
     * 全ての監査ログを取得します。
     */
    List<AuditLog> findAll();

    /**
     * メモリ内の全ての監査ログを削除します。
     */
    void clear();
}