package com.example.javase17learningproject.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @InjectMocks
    private AccessControlService accessControlService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private User adminUser;
    private User moderatorUser;
    private User normalUser1;
    private User normalUser2;
    private Role adminRole;
    private Role moderatorRole;
    private Role userRole;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        // 役割の設定
        adminRole = new Role("ADMIN");
        adminRole.setId(1L);

        moderatorRole = new Role("MODERATOR");
        moderatorRole.setId(2L);

        userRole = new Role("USER");
        userRole.setId(3L);

        // ユーザーの設定
        adminUser = new User("Admin", "admin@example.com", "password");
        adminUser.setId(1L);
        adminUser.getRoles().add(adminRole);

        moderatorUser = new User("Moderator", "moderator@example.com", "password");
        moderatorUser.setId(2L);
        moderatorUser.getRoles().add(moderatorRole);

        normalUser1 = new User("User1", "user1@example.com", "password");
        normalUser1.setId(3L);
        normalUser1.getRoles().add(userRole);

        normalUser2 = new User("User2", "user2@example.com", "password");
        normalUser2.setId(4L);
        normalUser2.getRoles().add(userRole);

        // セキュリティコンテキストの設定
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("管理者は全てのユーザーを編集可能")
    void adminCanEditAnyUser() {
        when(authentication.getPrincipal()).thenReturn(adminUser);

        assertTrue(accessControlService.canEditUser(moderatorUser));
        assertTrue(accessControlService.canEditUser(normalUser1));
        assertTrue(accessControlService.canEditUser(adminUser));
    }

    @Test
    @DisplayName("管理補助者は一般ユーザーのみ編集可能")
    void moderatorCanEditOnlyNormalUsers() {
        when(authentication.getPrincipal()).thenReturn(moderatorUser);

        assertFalse(accessControlService.canEditUser(adminUser));
        assertFalse(accessControlService.canEditUser(moderatorUser));
        assertTrue(accessControlService.canEditUser(normalUser1));
    }

    @Test
    @DisplayName("一般ユーザーは自分自身のみ編集可能")
    void normalUserCanOnlyEditSelf() {
        when(authentication.getPrincipal()).thenReturn(normalUser1);

        assertFalse(accessControlService.canEditUser(adminUser));
        assertFalse(accessControlService.canEditUser(moderatorUser));
        assertTrue(accessControlService.canEditUser(normalUser1));
        assertFalse(accessControlService.canEditUser(normalUser2));
    }

    @Test
    @DisplayName("管理者は全てのユーザーを削除可能")
    void adminCanDeleteAnyUser() {
        when(authentication.getPrincipal()).thenReturn(adminUser);

        assertTrue(accessControlService.canDeleteUser(moderatorUser));
        assertTrue(accessControlService.canDeleteUser(normalUser1));
        assertTrue(accessControlService.canDeleteUser(adminUser));
    }

    @Test
    @DisplayName("管理補助者は一般ユーザーのみ削除可能")
    void moderatorCanDeleteOnlyNormalUsers() {
        when(authentication.getPrincipal()).thenReturn(moderatorUser);

        assertFalse(accessControlService.canDeleteUser(adminUser));
        assertFalse(accessControlService.canDeleteUser(moderatorUser));
        assertTrue(accessControlService.canDeleteUser(normalUser1));
    }

    @Test
    @DisplayName("一般ユーザーは誰も削除できない")
    void normalUserCannotDeleteAnyone() {
        when(authentication.getPrincipal()).thenReturn(normalUser1);

        assertFalse(accessControlService.canDeleteUser(adminUser));
        assertFalse(accessControlService.canDeleteUser(moderatorUser));
        assertFalse(accessControlService.canDeleteUser(normalUser1));
        assertFalse(accessControlService.canDeleteUser(normalUser2));
    }

    @Test
    @DisplayName("管理者は全ての役割のユーザーを表示可能")
    void adminCanViewAllRoles() {
        when(authentication.getPrincipal()).thenReturn(adminUser);

        assertTrue(accessControlService.canViewUsersByRole("ADMIN"));
        assertTrue(accessControlService.canViewUsersByRole("MODERATOR"));
        assertTrue(accessControlService.canViewUsersByRole("USER"));
    }

    @Test
    @DisplayName("管理補助者は一般ユーザーのみ表示可能")
    void moderatorCanOnlyViewUsers() {
        when(authentication.getPrincipal()).thenReturn(moderatorUser);

        assertFalse(accessControlService.canViewUsersByRole("ADMIN"));
        assertFalse(accessControlService.canViewUsersByRole("MODERATOR"));
        assertTrue(accessControlService.canViewUsersByRole("USER"));
    }

    @Test
    @DisplayName("一般ユーザーは一般ユーザーのみ表示可能")
    void normalUserCanOnlyViewUsers() {
        when(authentication.getPrincipal()).thenReturn(normalUser1);

        assertFalse(accessControlService.canViewUsersByRole("ADMIN"));
        assertFalse(accessControlService.canViewUsersByRole("MODERATOR"));
        assertTrue(accessControlService.canViewUsersByRole("USER"));
    }

    @Test
    @DisplayName("管理者は全ての役割のユーザーを作成可能")
    void adminCanCreateAllRoles() {
        when(authentication.getPrincipal()).thenReturn(adminUser);

        assertTrue(accessControlService.canCreateUserWithRole("ADMIN"));
        assertTrue(accessControlService.canCreateUserWithRole("MODERATOR"));
        assertTrue(accessControlService.canCreateUserWithRole("USER"));
    }

    @Test
    @DisplayName("管理補助者は一般ユーザーのみ作成可能")
    void moderatorCanOnlyCreateUsers() {
        when(authentication.getPrincipal()).thenReturn(moderatorUser);

        assertFalse(accessControlService.canCreateUserWithRole("ADMIN"));
        assertFalse(accessControlService.canCreateUserWithRole("MODERATOR"));
        assertTrue(accessControlService.canCreateUserWithRole("USER"));
    }

    @Test
    @DisplayName("一般ユーザーは誰も作成できない")
    void normalUserCannotCreateAnyRole() {
        when(authentication.getPrincipal()).thenReturn(normalUser1);

        assertFalse(accessControlService.canCreateUserWithRole("ADMIN"));
        assertFalse(accessControlService.canCreateUserWithRole("MODERATOR"));
        assertFalse(accessControlService.canCreateUserWithRole("USER"));
    }
}