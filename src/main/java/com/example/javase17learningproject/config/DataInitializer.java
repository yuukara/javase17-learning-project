package com.example.javase17learningproject.config;

import java.util.Collections;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.RoleRepository;
import com.example.javase17learningproject.repository.UserRepository;

/**
 * データの初期化を行うクラス。
 * アプリケーション起動時にマスターデータの初期化を行います。
 */
@Configuration
public class DataInitializer {

    /**
     * 役割の初期データを登録します。
     * データが存在しない場合のみ実行されます。
     *
     * @return 保存されたRole
     */
    @Bean
    @Order(1)
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

    /**
     * 管理者ユーザーの初期データを登録します。
     * データが存在しない場合のみ実行されます。
     */
    @Bean
    @Order(2)
    public CommandLineRunner initializeUsers(UserRepository userRepository,
                                          RoleRepository roleRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                Role adminRole = roleRepository.findByName("admin")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
                
                User adminUser = new User("管理者", "admin@example.com", passwordEncoder.encode("admin123"));
                adminUser.setRoles(Collections.singleton(adminRole));
                
                userRepository.save(adminUser);
            }
        };
    }
}