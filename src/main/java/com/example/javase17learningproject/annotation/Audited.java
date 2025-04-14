package com.example.javase17learningproject.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

/**
 * 監査ログを記録するメソッドを示すアノテーション。
 * このアノテーションが付与されたメソッドの実行は監査ログに記録されます。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    /**
     * 監査イベントのタイプ。
     */
    String eventType();

    /**
     * 監査イベントの重要度。
     * デフォルトはMEDIUM。
     */
    Severity severity() default Severity.MEDIUM;

    /**
     * イベントの説明。
     * デフォルトは空文字列。
     */
    String description() default "";
}