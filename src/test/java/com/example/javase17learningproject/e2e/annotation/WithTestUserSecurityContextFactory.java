package com.example.javase17learningproject.e2e.annotation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * WithTestUserアノテーションの実装クラス。
 * テストユーザーのセキュリティコンテキストを作成します。
 */
public class WithTestUserSecurityContextFactory 
    implements WithSecurityContextFactory<WithTestUser> {

    @Override
    public SecurityContext createSecurityContext(WithTestUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // アノテーションで指定されたロールをSpring Securityの権限に変換
        List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.roles())
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());

        // テストユーザーの認証情報を作成
        // パスワードは検証されないため、任意の値を設定可能
        Authentication auth = new UsernamePasswordAuthenticationToken(
            annotation.value(), // ユーザー名（メールアドレス）
            "test-password",   // パスワード（実際には使用されない）
            authorities        // 権限リスト
        );

        // 認証情報をセキュリティコンテキストに設定
        context.setAuthentication(auth);
        return context;
    }
}