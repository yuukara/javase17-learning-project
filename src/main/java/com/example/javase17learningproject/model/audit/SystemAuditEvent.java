package com.example.javase17learningproject.model.audit;

/**
 * システム関連の監査イベントを表す具象クラス。
 */
public final class SystemAuditEvent implements AuditEvent {

    /**
     * 事前定義されたシステム関連イベント。
     */
    public static final SystemAuditEvent SYSTEM_STARTED = 
        new SystemAuditEvent("SYSTEM_STARTED", Severity.MEDIUM);
    public static final SystemAuditEvent SYSTEM_STOPPED = 
        new SystemAuditEvent("SYSTEM_STOPPED", Severity.MEDIUM);
    public static final SystemAuditEvent CONFIG_CHANGED = 
        new SystemAuditEvent("CONFIG_CHANGED", Severity.HIGH);
    public static final SystemAuditEvent DATABASE_OPERATION = 
        new SystemAuditEvent("DATABASE_OPERATION", Severity.HIGH);
    public static final SystemAuditEvent BACKUP_COMPLETED = 
        new SystemAuditEvent("BACKUP_COMPLETED", Severity.MEDIUM);
    public static final SystemAuditEvent ERROR_OCCURRED = 
        new SystemAuditEvent("ERROR_OCCURRED", Severity.CRITICAL);

    private final String type;
    private final Severity severity;

    private SystemAuditEvent(String type, Severity severity) {
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
        return "SystemAuditEvent{" +
                "type='" + type + '\'' +
                ", severity=" + severity +
                '}';
    }
}