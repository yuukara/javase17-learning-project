package com.example.javase17learningproject.model;

import com.example.javase17learningproject.model.audit.AuditEvent;

/**
 * @deprecated 代わりに {@link AuditEvent.Severity} を使用してください。
 */
@Deprecated(since = "2.0", forRemoval = true)
public enum Severity {
    /** 重大 */
    HIGH,
    /** 中程度 */
    MEDIUM,
    /** 低 */
    LOW,
    /** 情報 */
    INFO;

    /**
     * AuditEvent.Severityに変換します.
     *
     * @return 変換後のSeverity
     */
    public AuditEvent.Severity toAuditEventSeverity() {
        return switch (this) {
            case HIGH -> AuditEvent.Severity.HIGH;
            case MEDIUM -> AuditEvent.Severity.MEDIUM;
            case LOW -> AuditEvent.Severity.LOW;
            case INFO -> AuditEvent.Severity.MEDIUM; // INFOはMEDIUMにマッピング
        };
    }

    /**
     * AuditEvent.Severityから変換します.
     *
     * @param severity 変換元のSeverity
     * @return 変換後のSeverity
     */
    public static Severity fromAuditEventSeverity(AuditEvent.Severity severity) {
        return switch (severity) {
            case HIGH -> HIGH;
            case MEDIUM -> MEDIUM;
            case LOW -> LOW;
            case CRITICAL -> HIGH; // CRITICALはHIGHにマッピング
        };
    }
}