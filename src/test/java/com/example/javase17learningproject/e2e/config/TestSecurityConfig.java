package com.example.javase17learningproject.e2e.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * E2Eテスト用のセキュリティ設定クラス。
 * テスト環境でのパスワードエンコーディングなどを設定します。
 */
@Configuration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}