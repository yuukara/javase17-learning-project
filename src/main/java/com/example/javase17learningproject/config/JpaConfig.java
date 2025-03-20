package com.example.javase17learningproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPAの設定クラス。
 * エンティティの監査（作成日時、更新日時の自動設定）を有効にします。
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}