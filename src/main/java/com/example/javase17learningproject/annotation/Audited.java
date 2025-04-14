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
     * 監査イベントのタイプを指定します。
     * デフォルトは空文字列で、この場合はメソッド名が使用されます。
     */
    String eventType() default "";

    /**
     * イベントの重要度を指定します。
     * デフォルトはMEDIUMです。
     */
    Severity severity() default Severity.MEDIUM;

    /**
     * 監査ログに記録する説明を指定します。
     * デフォルトは空文字列です。
     */
    String description() default "";
}