package com.example.javase17learningproject;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.repository.RoleRepository;

/**
 * RoleRepositoryのテストクラス。
 * データベース操作の検証を行います。
 */
@DataJpaTest
@Sql("/cleanup.sql")
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        roleRepository.deleteAll();
    }

    @Test
    @Transactional
    void testSaveRole() {
        // テストデータの準備
        Role role = new Role("ADMIN");

        // 保存の実行
        Role savedRole = roleRepository.save(role);

        // 検証
        assertThat(savedRole.getId()).isNotNull();
        assertThat(savedRole.getName()).isEqualTo("ADMIN");
        assertThat(savedRole.getFullName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @Transactional
    void testFindByName() {
        // テストデータの準備
        Role role1 = new Role("ADMIN");
        Role role2 = new Role("USER");
        roleRepository.save(role1);
        roleRepository.save(role2);

        // 検索の実行
        Optional<Role> foundRole = roleRepository.findByName("ADMIN");

        // 検証
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo("ADMIN");
        assertThat(foundRole.get().getFullName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @Transactional
    void testDeleteRole() {
        // テストデータの準備
        Role role = new Role("TEST_ROLE");
        Role savedRole = roleRepository.save(role);

        // 削除の実行
        roleRepository.deleteById(savedRole.getId());

        // 検証
        Optional<Role> deletedRole = roleRepository.findById(savedRole.getId());
        assertThat(deletedRole).isEmpty();
    }

    @Test
    @Transactional
    void testFindByNameNotFound() {
        // テストデータのクリーンアップ
        roleRepository.deleteAll();

        // 存在しない名前での検索実行
        Optional<Role> notFoundRole = roleRepository.findByName("NON_EXISTENT_ROLE");

        // 検証
        assertThat(notFoundRole).isEmpty();
    }

    @Test
    @Transactional
    void testFindAll() {
        // テストデータの準備
        Role role1 = new Role("TEST_ADMIN");
        Role role2 = new Role("TEST_USER");
        Role role3 = new Role("TEST_MODERATOR");
        
        roleRepository.save(role1);
        roleRepository.save(role2);
        roleRepository.save(role3);

        // 全件検索の実行
        List<Role> roles = roleRepository.findAll();

        // 検証
        assertThat(roles).hasSize(3);
        assertThat(roles).extracting("name")
                        .containsExactlyInAnyOrder("TEST_ADMIN", "TEST_USER", "TEST_MODERATOR");
    }
}