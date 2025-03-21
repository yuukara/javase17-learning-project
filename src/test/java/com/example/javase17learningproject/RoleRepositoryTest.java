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
 * RoleRepositoryのテストクラス。
 * データベース操作の検証を行います。
 */
@DataJpaTest
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        roleRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        roleRepository.deleteAll();
    }

    @Test
    void testSaveRole() {
        // テストデータの準備
        Role role = new Role("ADMIN");

        // 保存の実行
        Role savedRole = roleRepository.save(role);

        // 検証
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo("ADMIN");
    }

    @Test
    void testFindByName() {
        // テストデータの準備
        Role role1 = new Role("ADMIN");
        Role role2 = new Role("USER");
        roleRepository.saveAll(List.of(role1, role2));

        // 検索の実行
        Optional<Role> foundRole = roleRepository.findByName("ADMIN");

        // 検証
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo("ADMIN");
    }

    @Test
    void testDeleteRole() {
        // テストデータの準備
        Role role = new Role("ADMIN");
        Role savedRole = roleRepository.save(role);

        // 削除の実行
        roleRepository.deleteById(savedRole.getId());

        // 検証
        Optional<Role> deletedRole = roleRepository.findById(savedRole.getId());
        assertThat(deletedRole).isEmpty();
    }

    @Test
    void testFindByNameNotFound() {
        // 存在しない名前での検索実行
        Optional<Role> notFoundRole = roleRepository.findByName("NON_EXISTENT_ROLE");

        // 検証
        assertThat(notFoundRole).isEmpty();
    }

    @Test
    void testFindAll() {
        // テストデータの準備
        Role role1 = new Role("ADMIN");
        Role role2 = new Role("USER");
        Role role3 = new Role("MODERATOR");
        roleRepository.saveAll(List.of(role1, role2, role3));

        // 全件検索の実行
        List<Role> roles = roleRepository.findAll();

        // 検証
        assertThat(roles).hasSize(3);
        assertThat(roles).extracting("name")
                        .containsExactlyInAnyOrder("ADMIN", "USER", "MODERATOR");
    }
}