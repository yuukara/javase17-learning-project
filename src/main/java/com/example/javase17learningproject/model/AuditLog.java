package com.example.javase17learningproject.model;

import java.time.LocalDateTime;

import com.example.javase17learningproject.model.audit.AuditEvent;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

/**
 * 監査ログを表すレコードクラス。
 */
public record AuditLog(
    Long id,
    String eventType,
    Severity severity,
    Long userId,
    Long targetId,
    String description,
    LocalDateTime createdAt
) {
    /**
     * コンパクトコンストラクタ。
     * パラメータの検証とデフォルト値の設定を行います。
     */
    public AuditLog {
        // イベントタイプは必須
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("イベントタイプは必須です");
        }

        // 重要度がnullの場合はMEDIUMを設定
        if (severity == null) {
            severity = Severity.MEDIUM;
        }
        
        // 作成日時がnullの場合は現在時刻を設定
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * 監査ログエントリを作成するファクトリメソッド。
     * 
     * @param event 監査イベント
     * @param userId 操作を実行したユーザーのID
     * @param targetId 操作の対象となったリソースのID
     * @param description 操作の説明
     * @return 新しい監査ログエントリ
     */
    public static AuditLog of(AuditEvent event, Long userId, Long targetId, String description) {
        return new AuditLog(
            null,
            event.getType(),
            event.getSeverity(),
            userId,
            targetId,
            description,
            LocalDateTime.now()
        );
    }

    /**
     * イベントタイプとその他のパラメータから監査ログエントリを作成するファクトリメソッド。
     */
    public static AuditLog of(String eventType, Long userId, Long targetId, String description) {
        return new AuditLog(
            null,
            eventType,
            Severity.MEDIUM,
            userId,
            targetId,
            description,
            LocalDateTime.now()
        );
    }
}