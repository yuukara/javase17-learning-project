package com.example.javase17learningproject.model;

import java.time.LocalDateTime;

import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 監査ログのエンティティクラス。
 * データベースでの永続化を担当します。
 */
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_severity_created_at", columnList = "severity,created_at")
    }
)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * デフォルトコンストラクタ（JPAで必要）
     */
    protected AuditLogEntity() {
    }

    /**
     * 全フィールドを指定するコンストラクタ。
     */
    public AuditLogEntity(
        Long id,
        String eventType,
        Severity severity,
        Long userId,
        Long targetId,
        String description,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.eventType = eventType;
        this.severity = severity != null ? severity : Severity.MEDIUM;
        this.userId = userId;
        this.targetId = targetId;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * AuditLogレコードからエンティティを作成するファクトリメソッド。
     */
    public static AuditLogEntity fromRecord(AuditLog record) {
        return new AuditLogEntity(
            record.id(),
            record.eventType(),
            record.severity(),
            record.userId(),
            record.targetId(),
            record.description(),
            record.createdAt()
        );
    }

    /**
     * エンティティをAuditLogレコードに変換するメソッド。
     */
    public AuditLog toRecord() {
        return new AuditLog(
            this.id,
            this.eventType,
            this.severity,
            this.userId,
            this.targetId,
            this.description,
            this.createdAt
        );
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters for non-final fields
    public void setDescription(String description) {
        this.description = description;
    }
}