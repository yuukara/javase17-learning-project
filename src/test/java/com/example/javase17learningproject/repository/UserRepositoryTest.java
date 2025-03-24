package com.example.javase17learningproject.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.config.TestConfig;
import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;

@DataJpaTest
@Import(TestConfig.class)
@Transactional
class UserRepositoryTest {
    
    private static final Logger log = LoggerFactory.getLogger(UserRepositoryTest.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        log.info("setUp: データベースのクリーンアップを開始");
        
        // まず既存のデータをクリーンアップ
        long userCount = userRepository.count();
        long roleCount = roleRepository.count();
        log.info("クリーンアップ前の状態 - ユーザー数: {}, ロール数: {}", userCount, roleCount);
        
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // データベースの状態を確認
        assertThat(userRepository.count()).isEqualTo(0);
        assertThat(roleRepository.count()).isEqualTo(0);
        log.info("クリーンアップ完了 - データベースが空になったことを確認");

        // ロールを1つずつ作成
        log.info("ロールの作成を開始");
        userRole = roleRepository.save(new Role("ROLE_USER"));
        log.info("ROLE_USER作成完了: {}", userRole);
        
        adminRole = roleRepository.save(new Role("ROLE_ADMIN"));
        log.info("ROLE_ADMIN作成完了: {}", adminRole);
        
        log.info("setUp完了 - 作成されたロール数: {}", roleRepository.count());
    }

    @AfterEach
    void tearDown() {
        log.info("tearDown: クリーンアップを開始");
        log.info("クリーンアップ前の状態 - ユーザー数: {}, ロール数: {}",
            userRepository.count(), roleRepository.count());

        userRepository.deleteAll();
        roleRepository.deleteAll();

        log.info("tearDown完了 - ユーザー数: {}, ロール数: {}",
            userRepository.count(), roleRepository.count());
    }

    @Test
    void testSaveUser() {
        // テストデータの準備
        User user = createUser("testUser", userRole);

        // 保存の実行
        User savedUser = userRepository.save(user);

        // 検証
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("testUser");
        assertThat(savedUser.getEmail()).isNotNull();
        assertThat(savedUser.getEmail()).contains("@example.com");
        assertThat(savedUser.getRoles()).containsExactly(userRole);
    }

    @Test
    void testFindByEmail() {
        // テストデータの準備
        User user = createUser("testUser", userRole);
        User savedUser = userRepository.save(user);
        String savedEmail = savedUser.getEmail();

        // 検索の実行
        Optional<User> foundUser = userRepository.findByEmail(savedEmail);

        // 検証
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("testUser");
        assertThat(foundUser.get().getEmail()).isEqualTo(savedEmail);
    }

    @Test
    void testFindByNameContaining() {
        // テストデータの準備
        User user1 = createUser("testUser1", userRole);
        User user2 = createUser("testUser2", adminRole);
        User user3 = createUser("otherUser", userRole);
        userRepository.saveAll(List.of(user1, user2, user3));

        // 検索の実行
        List<User> foundUsers = userRepository.findByNameContaining("test");

        // 検証
        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers).extracting("name").containsExactlyInAnyOrder("testUser1", "testUser2");
    }

    @Test
    void testSearchByRole() {
        // テストデータの準備
        User user1 = createUser("testUser1", userRole);
        User user2 = createUser("testUser2", adminRole);
        User user3 = createUser("testUser3", userRole);
        userRepository.saveAll(List.of(user1, user2, user3));

        // 検索の実行
        List<User> userRoleUsers = userRepository.findByRoles("ROLE_USER");
        List<User> adminRoleUsers = userRepository.findByRoles("ROLE_ADMIN");

        // 検証
        assertThat(userRoleUsers).extracting("name").containsExactlyInAnyOrder("testUser1", "testUser3");
        assertThat(adminRoleUsers).extracting("name").containsExactlyInAnyOrder("testUser2");
    }

    private User createUser(String name, Role role) {
        User user = new User();
        user.setName(name);
        // メールアドレスをユニークにするため、タイムスタンプを追加
        user.setEmail(name.toLowerCase() + System.currentTimeMillis() + "@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRoles(Set.of(role));
        return user;
    }
}
