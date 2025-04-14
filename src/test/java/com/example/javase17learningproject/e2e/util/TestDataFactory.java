package com.example.javase17learningproject.e2e.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.RoleRepository;
import com.example.javase17learningproject.repository.UserRepository;

import java.util.Set;

/**
 * E2Eテスト用のテストデータを管理するファクトリクラス。
 * テストユーザーの作成や管理を担当します。
 */
@Component
public class TestDataFactory {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public TestDataFactory(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * テスト用の管理者ユーザーを作成します。
     */
    @Transactional
    public User createAdminUser() {
        Role adminRole = roleRepository.findByName("ADMIN")
            .orElseGet(() -> roleRepository.save(new Role("ADMIN")));

        User admin = new User(
            "Admin User",
            "admin@example.com",
            passwordEncoder.encode("admin123")
        );
        admin.setRoles(Set.of(adminRole));
        return userRepository.save(admin);
    }

    /**
     * テスト用の一般ユーザーを作成します。
     */
    @Transactional
    public User createNormalUser(String email) {
        Role userRole = roleRepository.findByName("USER")
            .orElseGet(() -> roleRepository.save(new Role("USER")));

        User user = new User(
            "Test User",
            email,
            passwordEncoder.encode("password123")
        );
        user.setRoles(Set.of(userRole));
        return userRepository.save(user);
    }

    /**
     * テスト用の無効化されたユーザーを作成します。
     */
    @Transactional
    public User createDisabledUser(String email) {
        Role userRole = roleRepository.findByName("USER")
            .orElseGet(() -> roleRepository.save(new Role("USER")));

        User user = new User(
            "Disabled User",
            email,
            passwordEncoder.encode("password123")
        );
        user.setRoles(Set.of(userRole));
        user.setEnabled(false);
        return userRepository.save(user);
    }

    /**
     * すべてのテストデータをクリーンアップします。
     */
    @Transactional
    public void cleanupTestData() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }
}