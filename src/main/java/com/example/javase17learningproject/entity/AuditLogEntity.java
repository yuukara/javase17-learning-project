package com.example.javase17learningproject.entity;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;
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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDateTime;

/**
 * 監査ログのエンティティクラス.
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
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
    private AuditEvent.Severity severity;

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
     * 全フィールドを指定するコンストラクタ.
     */
    public AuditLogEntity(
        Long id,
        String eventType,
        AuditEvent.Severity severity,
        Long userId,
        Long targetId,
        String description,
        LocalDateTime createdAt
    ) {
        this.id = id;
        this.eventType = eventType;
        this.severity = severity != null ? severity : AuditEvent.Severity.MEDIUM;
        this.userId = userId;
        this.targetId = targetId;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * レコードからエンティティを作成します.
     *
     * @param record 変換元のAuditLogレコード
     * @return 作成されたエンティティ
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
     * エンティティをレコードに変換します.
     *
     * @return 変換されたAuditLogレコード
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

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public AuditEvent.Severity getSeverity() {
        return severity;
    }

    public void setSeverity(AuditEvent.Severity severity) {
        this.severity = severity;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}