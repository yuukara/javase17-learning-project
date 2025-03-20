package com.example.javase17learningproject.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.example.javase17learningproject.Role;
import com.example.javase17learningproject.RoleRepository;

/**
 * データの初期化を行うクラス。
 * アプリケーション起動時にマスターデータの初期化を行います。
 */
@Configuration
public class DataInitializer {

    /**
     * 役割の初期データを登録します。
     * データが存在しない場合のみ実行されます。
     */
    @Bean
    @Order(1) // 実行順序を指定（必要に応じて）
    public CommandLineRunner initializeRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.count() == 0) {
                Role adminRole = new Role("admin");
                Role userRole = new Role("user");
                roleRepository.save(adminRole);
                roleRepository.save(userRole);
            }
        };
    }
}