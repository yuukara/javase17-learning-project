package com.example.javase17learningproject.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.javase17learningproject.config.TestConfig;
import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;

@DataJpaTest
@Import(TestConfig.class)
class UserRepositoryTest {

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
        userRole = new Role("ROLE_USER");
        adminRole = new Role("ROLE_ADMIN");
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
        User user = createUser("testUser", "test@example.com", userRole);

        // 保存の実行
        User savedUser = userRepository.save(user);

        // 検証
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("testUser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getRoles()).containsExactly(userRole);
    }

    @Test
    void testFindByEmail() {
        // テストデータの準備
        User user = createUser("testUser", "test@example.com", userRole);
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
        User user1 = createUser("testUser1", "test1@example.com", userRole);
        User user2 = createUser("testUser2", "test2@example.com", adminRole);
        User user3 = createUser("otherUser", "other@example.com", userRole);
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
        User user1 = createUser("testUser1", "test1@example.com", userRole);
        User user2 = createUser("testUser2", "test2@example.com", adminRole);
        User user3 = createUser("testUser3", "test3@example.com", userRole);
        userRepository.saveAll(List.of(user1, user2, user3));

        // 検索の実行
        List<User> userRoleUsers = userRepository.findByRoles("ROLE_USER");
        List<User> adminRoleUsers = userRepository.findByRoles("ROLE_ADMIN");

        // 検証
        assertThat(userRoleUsers).hasSize(2);
        assertThat(adminRoleUsers).hasSize(1);
        assertThat(userRoleUsers).extracting("name").containsExactlyInAnyOrder("testUser1", "testUser3");
        assertThat(adminRoleUsers).extracting("name").containsExactly("testUser2");
    }

    private User createUser(String name, String email, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRoles(Set.of(role));
        return user;
    }
}
