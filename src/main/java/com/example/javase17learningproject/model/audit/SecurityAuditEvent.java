package com.example.javase17learningproject.model.audit;

/**
 * セキュリティ関連の監査イベントを表す具象クラス。
 */
public final class SecurityAuditEvent implements AuditEvent {

    /**
     * 事前定義されたセキュリティ関連イベント。
     */
    public static final SecurityAuditEvent LOGIN_SUCCESS = 
        new SecurityAuditEvent("LOGIN_SUCCESS", Severity.LOW);
    public static final SecurityAuditEvent LOGIN_FAILED = 
        new SecurityAuditEvent("LOGIN_FAILED", Severity.MEDIUM);
    public static final SecurityAuditEvent ACCOUNT_LOCKED = 
        new SecurityAuditEvent("ACCOUNT_LOCKED", Severity.HIGH);
    public static final SecurityAuditEvent ACCESS_DENIED = 
        new SecurityAuditEvent("ACCESS_DENIED", Severity.HIGH);
    public static final SecurityAuditEvent PASSWORD_CHANGED = 
        new SecurityAuditEvent("PASSWORD_CHANGED", Severity.MEDIUM);

    private final String type;
    private final Severity severity;

    private SecurityAuditEvent(String type, Severity severity) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("イベントタイプは必須です");
        }
        if (severity == null) {
            throw new IllegalArgumentException("重要度は必須です");
        }
        this.type = type;
        this.severity = severity;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return "SecurityAuditEvent{" +
                "type='" + type + '\'' +
                ", severity=" + severity +
                '}';
    }
}