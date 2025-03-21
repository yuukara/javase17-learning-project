package com.example.javase17learningproject;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * UserRepositoryのテストクラス。
 * データベース操作の検証を行います。
 */
@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role("USER");
        adminRole = new Role("ADMIN");
        roleRepository.saveAll(List.of(userRole, adminRole));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void testSaveUser() {
        // テストデータの準備
        User user = new User("testUser", "test@example.com", userRole, "password123");

        // 保存の実行
        User savedUser = userRepository.save(user);

        // 検証
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("testUser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getRole().getName()).isEqualTo("USER");
    }

    @Test
    void testFindByEmail() {
        // テストデータの準備
        User user = new User("testUser", "test@example.com", userRole, "password123");
        userRepository.save(user);

        // 検索の実行
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // 検証
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("testUser");
    }

    @Test
    void testFindByNameContaining() {
        // テストデータの準備
        User user1 = new User("testUser1", "test1@example.com", userRole, "password123");
        User user2 = new User("testUser2", "test2@example.com", adminRole, "password123");
        User user3 = new User("otherUser", "other@example.com", userRole, "password123");
        userRepository.saveAll(List.of(user1, user2, user3));

        // 検索の実行
        List<User> foundUsers = userRepository.findByNameContaining("test");

        // 検証
        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers).extracting("name").containsExactlyInAnyOrder("testUser1", "testUser2");
    }

    @Test
    void testFindByRole() {
        // テストデータの準備
        User user1 = new User("testUser1", "test1@example.com", userRole, "password123");
        User user2 = new User("testUser2", "test2@example.com", adminRole, "password123");
        User user3 = new User("testUser3", "test3@example.com", userRole, "password123");
        userRepository.saveAll(List.of(user1, user2, user3));

        // 検索の実行
        List<User> userRoleUsers = userRepository.findByRoles(userRole);
        List<User> adminRoleUsers = userRepository.findByRoles(adminRole);

        // 検証
        assertThat(userRoleUsers).hasSize(2);
        assertThat(adminRoleUsers).hasSize(1);
        assertThat(userRoleUsers).extracting("name").containsExactlyInAnyOrder("testUser1", "testUser3");
        assertThat(adminRoleUsers).extracting("name").containsExactly("testUser2");
    }

    @Test
    void testDeleteUser() {
        // テストデータの準備
        User user = new User("testUser", "test@example.com", userRole, "password123");
        User savedUser = userRepository.save(user);

        // 削除の実行
        userRepository.deleteById(savedUser.getId());

        // 検証
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();
    }
}