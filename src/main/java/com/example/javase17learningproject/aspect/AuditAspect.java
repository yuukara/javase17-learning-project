package com.example.javase17learningproject.aspect;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.javase17learningproject.annotation.Audited;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.AuditLogInMemoryStorage;

/**
 * 監査ログを記録するためのアスペクト。
 * @Auditedアノテーションが付与されたメソッドの実行を監視し、ログを記録します。
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditLogInMemoryStorage auditLogStorage;

    public AuditAspect(AuditLogInMemoryStorage auditLogStorage) {
        this.auditLogStorage = auditLogStorage;
    }

    @Around("@annotation(com.example.javase17learningproject.annotation.Audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // メソッドとアノテーションの情報を取得
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audited auditedAnnotation = method.getAnnotation(Audited.class);

        // 現在のユーザーIDを取得
        Long userId = getCurrentUserId();

        // メソッドの実行
        Object result = joinPoint.proceed();

        // 対象IDの取得（メソッドの最初の引数がLongまたはIDを持つオブジェクトと仮定）
        Long targetId = extractTargetId(joinPoint.getArgs());

        // 監査ログの作成と保存
        AuditLog auditLog = new AuditLog(
            null,
            auditedAnnotation.eventType(),
            auditedAnnotation.severity(),
            userId,
            targetId,
            buildDescription(auditedAnnotation.description(), method, joinPoint.getArgs()),
            LocalDateTime.now()
        );

        auditLogStorage.save(auditLog);

        return result;
    }

    private Long getCurrentUserId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getPrincipal)
            .filter(principal -> principal instanceof User)
            .map(principal -> ((User) principal).getId())
            .orElse(null);
    }

    private Long extractTargetId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        Object firstArg = args[0];
        if (firstArg instanceof Long) {
            return (Long) firstArg;
        }

        // IDを持つオブジェクトの場合、getIdメソッドの呼び出しを試みる
        try {
            Method getIdMethod = firstArg.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(firstArg);
            if (id instanceof Long) {
                return (Long) id;
            }
        } catch (Exception e) {
            // IDの抽出に失敗した場合は無視
        }

        return null;
    }

    private String buildDescription(String baseDescription, Method method, Object[] args) {
        if (!baseDescription.isEmpty()) {
            return baseDescription;
        }

        StringBuilder description = new StringBuilder();
        description.append("Method: ").append(method.getName());
        
        if (args != null && args.length > 0) {
            description.append(", Args: [");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    description.append(", ");
                }
                description.append(String.valueOf(args[i]));
            }
            description.append("]");
        }

        return description.toString();
    }
}