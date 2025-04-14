package com.example.javase17learningproject.model.audit;

/**
 * ユーザー関連の監査イベントを表す具象クラス。
 */
public final class UserAuditEvent implements AuditEvent {

    /**
     * 事前定義されたユーザー関連イベント。
     */
    public static final UserAuditEvent USER_CREATED = 
        new UserAuditEvent("USER_CREATED", Severity.MEDIUM);
    public static final UserAuditEvent USER_UPDATED = 
        new UserAuditEvent("USER_UPDATED", Severity.MEDIUM);
    public static final UserAuditEvent USER_DELETED = 
        new UserAuditEvent("USER_DELETED", Severity.HIGH);
    public static final UserAuditEvent ROLE_CHANGED = 
        new UserAuditEvent("ROLE_CHANGED", Severity.HIGH);

    private final String type;
    private final Severity severity;

    private UserAuditEvent(String type, Severity severity) {
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
        return "UserAuditEvent{" +
                "type='" + type + '\'' +
                ", severity=" + severity +
                '}';
    }
}