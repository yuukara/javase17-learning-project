package com.example.javase17learningproject.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.javase17learningproject.annotation.Audited;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.model.audit.AuditEvent;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.model.audit.SecurityAuditEvent;
import com.example.javase17learningproject.model.audit.SystemAuditEvent;
import com.example.javase17learningproject.model.audit.UserAuditEvent;
import com.example.javase17learningproject.service.AuditLogService;

/**
 * 監査ログを記録するためのアスペクト。
 * @Auditedアノテーションが付与されたメソッドの実行を監視し、監査ログを記録します。
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Autowired
    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String eventType = getEventType(joinPoint, audited);
        Long userId = getCurrentUserId();
        Long targetId = getTargetId(joinPoint);
        String description = getDescription(joinPoint, audited);

        try {
            // メソッドを実行
            Object result = joinPoint.proceed();

            // 成功した場合の監査ログを記録
            auditLogService.logEvent(
                determineAuditEvent(eventType, audited.severity()),
                userId,
                targetId,
                description
            );

            return result;
        } catch (Throwable e) {
            // 失敗した場合の監査ログを記録
            auditLogService.logEvent(
                determineAuditEvent(eventType + "_FAILED", audited.severity()),
                userId,
                targetId,
                description + " - Error: " + e.getMessage()
            );
            throw e;
        }
    }

    private String getEventType(ProceedingJoinPoint joinPoint, Audited audited) {
        return audited.eventType().isEmpty() 
            ? joinPoint.getSignature().getName().toUpperCase()
            : audited.eventType();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    private Long getTargetId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long) {
            return (Long) args[0];
        }
        return null;
    }

    private String getDescription(ProceedingJoinPoint joinPoint, Audited audited) {
        if (!audited.description().isEmpty()) {
            return audited.description();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return "Method: " + signature.getDeclaringType().getSimpleName() + 
               "." + signature.getName();
    }

    private AuditEvent determineAuditEvent(String type, Severity severity) {
        return switch (type.toUpperCase()) {
            // ユーザー関連イベント
            case "USER_CREATED", "CREATE_USER" -> UserAuditEvent.USER_CREATED;
            case "USER_UPDATED", "UPDATE_USER" -> UserAuditEvent.USER_UPDATED;
            case "USER_DELETED", "DELETE_USER" -> UserAuditEvent.USER_DELETED;
            case "ROLE_CHANGED", "CHANGE_ROLE" -> UserAuditEvent.ROLE_CHANGED;

            // セキュリティ関連イベント
            case "LOGIN_SUCCESS" -> SecurityAuditEvent.LOGIN_SUCCESS;
            case "LOGIN_FAILED" -> SecurityAuditEvent.LOGIN_FAILED;
            case "ACCESS_DENIED" -> SecurityAuditEvent.ACCESS_DENIED;
            case "ACCOUNT_LOCKED" -> SecurityAuditEvent.ACCOUNT_LOCKED;
            case "PASSWORD_CHANGED" -> SecurityAuditEvent.PASSWORD_CHANGED;

            // システム関連イベント
            case "SYSTEM_STARTED" -> SystemAuditEvent.SYSTEM_STARTED;
            case "SYSTEM_STOPPED" -> SystemAuditEvent.SYSTEM_STOPPED;
            case "CONFIG_CHANGED" -> SystemAuditEvent.CONFIG_CHANGED;
            case "DATABASE_OPERATION" -> SystemAuditEvent.DATABASE_OPERATION;
            case "ERROR_OCCURRED" -> SystemAuditEvent.ERROR_OCCURRED;

            // その他のイベントはSystemAuditEventとして扱う
            default -> SystemAuditEvent.ERROR_OCCURRED;
        };
    }
}