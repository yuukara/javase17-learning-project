package com.example.javase17learningproject.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessControlServiceTest {
    
    private static final Logger log = LoggerFactory.getLogger(AccessControlServiceTest.class);

    private AccessControlService accessControlService;

    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private UserRepository userRepository;

    private User adminUser;
    private User moderatorUser;
    private User normalUser1;
    private User normalUser2;
    private Role adminRole;
    private Role moderatorRole;
    private Role userRole;


    /**
     * テスト用の認証ユーザーを設定します。
     * このメソッドは以下の設定を行います：
     * 1. 認証情報（Authentication）の設定
     * 2. UserRepositoryのモック設定
     * 3. セキュリティコンテキストの検証
     *
     * @param user 認証ユーザーとして設定するユーザー
     * @throws AssertionError セキュリティコンテキストの検証に失敗した場合
     */
    private void setAuthenticatedUser(User user) {
        final String email = user.getEmail();
        final Set<Role> roles = user.getRoles();
        
        log.debug("認証ユーザーを設定 - user: {}, email: {}, roles: {}",
            user.getName(), email,
            roles.stream().map(Role::getName).collect(Collectors.joining(", ")));

        // 認証情報の設定
        when(authentication.getName()).thenReturn(email);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.isAuthenticated()).thenReturn(true);
        
        // UserRepositoryのモック設定
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        
        // セキュリティコンテキストの設定を確認
        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context).isNotNull();
        assertThat(context.getAuthentication()).isNotNull()
            .isEqualTo(authentication);
    }

    @BeforeEach
    void setup() {
        log.info("テストのセットアップを開始");

        // AccessControlServiceの初期化
        log.debug("AccessControlServiceの初期化");
        accessControlService = new AccessControlService();
        ReflectionTestUtils.setField(accessControlService, "userRepository", userRepository);

        // セキュリティコンテキストの初期化
        log.debug("セキュリティコンテキストの初期化");
        SecurityContextHolder.clearContext();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // ロールの初期化
        log.debug("ロールの初期化");
        adminRole = new Role("ADMIN");
        adminRole.setPrefix("ROLE_");
        adminRole.setId(1L);

        moderatorRole = new Role("MODERATOR");
        moderatorRole.setPrefix("ROLE_");
        moderatorRole.setId(2L);

        userRole = new Role("USER");
        userRole.setPrefix("ROLE_");
        userRole.setId(3L);

        // ユーザーの初期化
        log.debug("ユーザーの初期化");
        adminUser = new User("Admin", "admin@example.com", "password");
        adminUser.setId(1L);
        adminUser.setRoles(Set.of(adminRole));

        moderatorUser = new User("Moderator", "moderator@example.com", "password");
        moderatorUser.setId(2L);
        moderatorUser.setRoles(Set.of(moderatorRole));

        normalUser1 = new User("User1", "user1@example.com", "password");
        normalUser1.setId(3L);
        normalUser1.setRoles(Set.of(userRole));

        normalUser2 = new User("User2", "user2@example.com", "password");
        normalUser2.setId(4L);
        normalUser2.setRoles(Set.of(userRole));

        // UserRepositoryのモック設定
        log.debug("UserRepositoryのモック設定");
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail(moderatorUser.getEmail())).thenReturn(Optional.of(moderatorUser));
        when(userRepository.findByEmail(normalUser1.getEmail())).thenReturn(Optional.of(normalUser1));
        when(userRepository.findByEmail(normalUser2.getEmail())).thenReturn(Optional.of(normalUser2));

        log.info("テストのセットアップ完了");
    }

    @Test
    @DisplayName("管理者は全てのユーザーを編集可能")
    void adminCanEditAnyUser() {
        log.info("管理者のユーザー編集権限テストを開始");
        
        // 管理者として認証
        setAuthenticatedUser(adminUser);

        try {
            // 各種ユーザータイプに対する編集権限をテスト
            log.debug("管理者の編集権限をテスト - 対象: 管理補助者");
            assertTrue(accessControlService.canEditUser(moderatorUser),
                "管理者は管理補助者を編集できるべき");

            log.debug("管理者の編集権限をテスト - 対象: 一般ユーザー");
            assertTrue(accessControlService.canEditUser(normalUser1),
                "管理者は一般ユーザーを編集できるべき");

            log.debug("管理者の編集権限をテスト - 対象: 他の管理者");
            assertTrue(accessControlService.canEditUser(adminUser),
                "管理者は他の管理者を編集できるべき");

            log.info("管理者のユーザー編集権限テストが成功しました");
        } catch (AssertionError e) {
            log.error("テスト失敗: アサーション違反 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("テスト実行中に予期せぬエラー: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("管理補助者は一般ユーザーのみ編集可能")
    void moderatorCanEditOnlyNormalUsers() {
        log.info("管理補助者のユーザー編集権限テストを開始");
        
        // 管理補助者として認証
        setAuthenticatedUser(moderatorUser);

        try {
            log.debug("管理補助者の編集権限をテスト - 対象: 管理者");
            assertFalse(accessControlService.canEditUser(adminUser),
                "管理補助者は管理者を編集できないはず");

            log.debug("管理補助者の編集権限をテスト - 対象: 他の管理補助者");
            assertFalse(accessControlService.canEditUser(moderatorUser),
                "管理補助者は他の管理補助者を編集できないはず");

            log.debug("管理補助者の編集権限をテスト - 対象: 一般ユーザー");
            assertTrue(accessControlService.canEditUser(normalUser1),
                "管理補助者は一般ユーザーを編集できるはず");

            log.info("管理補助者のユーザー編集権限テストが成功");
        } catch (AssertionError e) {
            log.error("テスト失敗: アサーション違反 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("テスト実行中に予期せぬエラー: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("一般ユーザーは自分自身のみ編集可能")
    void normalUserCanOnlyEditSelf() {
        log.info("一般ユーザーのユーザー編集権限テストを開始");
        // 一般ユーザーとして認証
        setAuthenticatedUser(normalUser1);

        try {
            assertFalse(accessControlService.canEditUser(adminUser),
                "一般ユーザーは管理者を編集できないはず");
            assertFalse(accessControlService.canEditUser(moderatorUser),
                "一般ユーザーは管理補助者を編集できないはず");
            assertTrue(accessControlService.canEditUser(normalUser1),
                "一般ユーザーは自分自身を編集できるはず");
            assertFalse(accessControlService.canEditUser(normalUser2),
                "一般ユーザーは他の一般ユーザーを編集できないはず");
            log.info("一般ユーザーのユーザー編集権限テストが成功");
        } catch (Exception e) {
            log.error("テスト実行中にエラー: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("管理者は全てのユーザーを削除可能")
    void adminCanDeleteAnyUser() {
        log.info("管理者のユーザー削除権限テストを開始");
        // 管理者として認証
        setAuthenticatedUser(adminUser);

        try {
            assertTrue(accessControlService.canDeleteUser(moderatorUser),
                "管理者は管理補助者を削除できるべき");
            assertTrue(accessControlService.canDeleteUser(normalUser1),
                "管理者は一般ユーザーを削除できるべき");
            assertTrue(accessControlService.canDeleteUser(adminUser),
                "管理者は他の管理者を削除できるべき");
            log.info("管理者のユーザー削除権限テストが成功");
        } catch (Exception e) {
            log.error("テスト実行中にエラー: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("管理補助者は一般ユーザーのみ削除可能")
    void moderatorCanDeleteOnlyNormalUsers() {
        log.info("管理補助者のユーザー削除権限テストを開始");
        // 管理補助者として認証
        setAuthenticatedUser(moderatorUser);

        try {
            assertFalse(accessControlService.canDeleteUser(adminUser),
                "管理補助者は管理者を削除できないはず");
            assertFalse(accessControlService.canDeleteUser(moderatorUser),
                "管理補助者は他の管理補助者を削除できないはず");
            assertTrue(accessControlService.canDeleteUser(normalUser1),
                "管理補助者は一般ユーザーを削除できるはず");
            log.info("管理補助者のユーザー削除権限テストが成功");
        } catch (Exception e) {
            log.error("テスト実行中にエラー: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("一般ユーザーは誰も削除できない")
    void normalUserCannotDeleteAnyone() {
        log.info("一般ユーザーの削除権限テストを開始");
        
        // 一般ユーザーとして認証
        setAuthenticatedUser(normalUser1);

        try {
            log.debug("一般ユーザーの削除権限をテスト - 対象: 管理者");
            assertFalse(accessControlService.canDeleteUser(adminUser),
                "一般ユーザーは管理者を削除できないはず");

            log.debug("一般ユーザーの削除権限をテスト - 対象: 管理補助者");
            assertFalse(accessControlService.canDeleteUser(moderatorUser),
                "一般ユーザーは管理補助者を削除できないはず");

            log.debug("一般ユーザーの削除権限をテスト - 対象: 自分自身");
            assertFalse(accessControlService.canDeleteUser(normalUser1),
                "一般ユーザーは自分自身を削除できないはず");

            log.debug("一般ユーザーの削除権限をテスト - 対象: 他の一般ユーザー");
            assertFalse(accessControlService.canDeleteUser(normalUser2),
                "一般ユーザーは他の一般ユーザーを削除できないはず");

            log.info("一般ユーザーの削除権限テストが成功");
        } catch (AssertionError e) {
            log.error("テスト失敗: アサーション違反 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("テスト実行中に予期せぬエラー: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("管理者は全ての役割のユーザーを表示可能")
    void adminCanViewAllRoles() {
        log.info("管理者のロール表示権限テストを開始");
        
        // 管理者として認証
        setAuthenticatedUser(adminUser);

        try {
            log.debug("管理者の表示権限をテスト - 対象ロール: ROLE_ADMIN");
            assertTrue(accessControlService.canViewUsersByRole("ROLE_ADMIN"),
                "管理者は管理者ロールを表示できるべき");

            log.debug("管理者の表示権限をテスト - 対象ロール: ROLE_MODERATOR");
            assertTrue(accessControlService.canViewUsersByRole("ROLE_MODERATOR"),
                "管理者は管理補助者ロールを表示できるべき");

            log.debug("管理者の表示権限をテスト - 対象ロール: ROLE_USER");
            assertTrue(accessControlService.canViewUsersByRole("ROLE_USER"),
                "管理者は一般ユーザーロールを表示できるべき");

            log.info("管理者のロール表示権限テストが成功");
        } catch (AssertionError e) {
            log.error("テスト失敗: アサーション違反 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("テスト実行中に予期せぬエラー: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("管理補助者は一般ユーザーのみ表示可能")
    void moderatorCanOnlyViewUsers() {
        log.info("管理補助者のロール表示権限テストを開始");
        // 管理補助者として認証
        setAuthenticatedUser(moderatorUser);

        try {
            assertFalse(accessControlService.canViewUsersByRole("ROLE_ADMIN"),
                "管理補助者は管理者ロールを表示できないはず");
            assertFalse(accessControlService.canViewUsersByRole("ROLE_MODERATOR"),
                "管理補助者は管理補助者ロールを表示できないはず");
            assertTrue(accessControlService.canViewUsersByRole("ROLE_USER"),
                "管理補助者は一般ユーザーロールを表示できるはず");
            log.info("管理補助者のロール表示権限テストが成功");
        } catch (Exception e) {
            log.error("テスト実行中にエラー: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @DisplayName("一般ユーザーは一般ユーザーのみ表示可能")
    void normalUserCanOnlyViewUsers() {
        log.info("一般ユーザーのロール表示権限テストを開始");
        // 一般ユーザーとして認証
        setAuthenticatedUser(normalUser1);

        try {
            assertFalse(accessControlService.canViewUsersByRole("ROLE_ADMIN"),
                "一般ユーザーは管理者ロールを表示できないはず");
            assertFalse(accessControlService.canViewUsersByRole("ROLE_MODERATOR"),
                "一般ユーザーは管理補助者ロールを表示できないはず");
            assertTrue(accessControlService.canViewUsersByRole("ROLE_USER"),
                "一般ユーザーは一般ユーザーロールを表示できるはず");
            log.info("一般ユーザーのロール表示権限テストが成功");
        } catch (Exception e) {
            log.error("テスト実行中にエラーが発生: {}", e.getMessage());
            throw e;
        }
    }

    @Test
    @DisplayName("管理者は全ての役割のユーザーを作成可能")
    void adminCanCreateAllRoles() {
        log.info("管理者のユーザー作成権限テストを開始");
        // 管理者として認証
        setAuthenticatedUser(adminUser);

        assertTrue(accessControlService.canCreateUserWithRole("ROLE_ADMIN"),
            "管理者は管理者ユーザーを作成できるべき");
        assertTrue(accessControlService.canCreateUserWithRole("ROLE_MODERATOR"),
            "管理者は管理補助者ユーザーを作成できるべき");
        assertTrue(accessControlService.canCreateUserWithRole("ROLE_USER"),
            "管理者は一般ユーザーを作成できるべき");
        log.info("管理者のユーザー作成権限テストが完了");
    }

    @Test
    @DisplayName("管理補助者は一般ユーザーのみ作成可能")
    void moderatorCanOnlyCreateUsers() {
        log.info("管理補助者のユーザー作成権限テストを開始");
        // 管理補助者として認証
        setAuthenticatedUser(moderatorUser);

        assertFalse(accessControlService.canCreateUserWithRole("ROLE_ADMIN"),
            "管理補助者は管理者ユーザーを作成できないはず");
        assertFalse(accessControlService.canCreateUserWithRole("ROLE_MODERATOR"),
            "管理補助者は管理補助者ユーザーを作成できないはず");
        assertTrue(accessControlService.canCreateUserWithRole("ROLE_USER"),
            "管理補助者は一般ユーザーを作成できるはず");
        log.info("管理補助者のユーザー作成権限テストが完了");
    }

    @Test
    @DisplayName("一般ユーザーは誰も作成できない")
    void normalUserCannotCreateAnyRole() {
        log.info("一般ユーザーのユーザー作成権限テストを開始");
        // 一般ユーザーとして認証
        setAuthenticatedUser(normalUser1);

        assertFalse(accessControlService.canCreateUserWithRole("ROLE_ADMIN"),
            "一般ユーザーは管理者ユーザーを作成できないはず");
        assertFalse(accessControlService.canCreateUserWithRole("ROLE_MODERATOR"),
            "一般ユーザーは管理補助者ユーザーを作成できないはず");
        assertFalse(accessControlService.canCreateUserWithRole("ROLE_USER"),
            "一般ユーザーは一般ユーザーを作成できないはず");
        log.info("一般ユーザーのユーザー作成権限テストが完了");
    }
}