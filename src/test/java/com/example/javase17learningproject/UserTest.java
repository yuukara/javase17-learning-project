package com.example.javase17learningproject;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Userエンティティのテストクラス。
 * エンティティの振る舞いとバリデーションを検証します。
 */
public class UserTest {

    private Validator validator;
    private Role userRole;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        userRole = new Role("USER");
    }

    @Test
    void testValidUser() {
        // 有効なユーザーの作成
        User user = new User("testUser", "test@example.com", "password123");
        user.getRoles().add(userRole);

        // バリデーション実行
        var violations = validator.validate(user);

        // 検証
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void testInvalidUserName(String name) {
        // 無効な名前でユーザーを作成
        User user = new User(name, "test@example.com", "password123");
        user.getRoles().add(userRole);

        // バリデーション実行
        var violations = validator.validate(user);

        // 検証
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                           .map(v -> v.getPropertyPath().toString()))
                           .contains("name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "test@", "@example.com", "test@.com"})
    void testInvalidEmail(String email) {
        // 無効なメールアドレスでユーザーを作成
        User user = new User("testUser", email, "password123");
        user.getRoles().add(userRole);

        // バリデーション実行
        var violations = validator.validate(user);

        // 検証
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                           .map(v -> v.getPropertyPath().toString()))
                           .contains("email");
    }

    @Test
    void testPasswordEncryption() {
        // 平文パスワードでユーザーを作成
        User user = new User("testUser", "test@example.com", "password123");
        user.getRoles().add(userRole);

        // パスワードがハッシュ化されていることを確認
        assertThat(user.getPassword()).isNotEqualTo("password123");
    }

    @Test
    void testNullRole() {
        // ロールなしでユーザーを作成
        User user = new User("testUser", "test@example.com", "password123");
        // ロールは設定しない（デフォルトで空のSetになっている）

        // バリデーション実行
        var violations = validator.validate(user);

        // 検証
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                           .map(v -> v.getPropertyPath().toString()))
                           .contains("roles");
    }

    @Test
    void testEqualsAndHashCode() {
        // 同じIDを持つ2つのユーザーを作成
        User user1 = new User("testUser1", "test1@example.com", "password123");
        user1.setId(1L);
        user1.getRoles().add(userRole);

        User user2 = new User("testUser2", "test2@example.com", "password456");
        user2.setId(1L);
        user2.getRoles().add(userRole);

        // 検証
        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    void testToString() {
        // ユーザーを作成
        User user = new User("testUser", "test@example.com", "password123");
        user.setId(1L);
        user.getRoles().add(userRole);

        // 検証
        String toString = user.toString();
        assertThat(toString)
            .contains("testUser")
            .contains("test@example.com")
            .doesNotContain("password123"); // パスワードは含まれるべきでない
    }
}