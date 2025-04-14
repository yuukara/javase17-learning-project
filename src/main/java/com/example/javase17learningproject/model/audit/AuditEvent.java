package com.example.javase17learningproject.model.audit;

/**
 * 監査イベントタイプを表すシールドインターフェース。
 * このインターフェースは、許可された具象クラスのみが実装できます。
 */
public sealed interface AuditEvent 
    permits UserAuditEvent, SecurityAuditEvent, SystemAuditEvent {

    /**
     * イベントタイプの文字列表現を取得します。
     *
     * @return イベントタイプを表す文字列
     */
    String getType();

    /**
     * イベントの重要度を取得します。
     *
     * @return イベントの重要度
     */
    Severity getSeverity();

    /**
     * イベントの重要度を表す列挙型。
     */
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}