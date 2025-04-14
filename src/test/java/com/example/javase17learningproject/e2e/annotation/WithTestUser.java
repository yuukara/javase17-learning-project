package com.example.javase17learningproject.e2e.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * テストユーザーとしてテストを実行するためのアノテーション。
 * Spring Securityのテストサポートを利用します。
 * 
 * 使用例：
 * {@code @WithTestUser(value = "admin@example.com", roles = {"ADMIN"})}
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithTestUserSecurityContextFactory.class)
public @interface WithTestUser {

    /**
     * テストユーザーのメールアドレス
     */
    String value() default "admin@example.com";

    /**
     * テストユーザーのロール
     */
    String[] roles() default { "ADMIN" };
}