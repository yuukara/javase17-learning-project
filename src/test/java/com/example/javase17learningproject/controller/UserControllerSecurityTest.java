package com.example.javase17learningproject.controller;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.RoleRepository;
import com.example.javase17learningproject.repository.UserRepository;
import com.example.javase17learningproject.service.AccessControlService;
import com.example.javase17learningproject.service.CustomUserDetailsService;
import com.example.javase17learningproject.service.PasswordEncodingService;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc
@Import({AccessControlService.class, PasswordEncodingService.class})
class UserControllerSecurityTest {

    private static final Logger log = LoggerFactory.getLogger(UserControllerSecurityTest.class);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private AccessControlService accessControlService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private PasswordEncodingService passwordEncodingService;

    private Role userRole;
    private Role adminRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        log.info("テストデータのセットアップを開始");

        userRole = new Role("USER");
        userRole.setId(1L);
        adminRole = new Role("ADMIN");
        adminRole.setId(2L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRoles(Set.of(userRole));

        // セキュリティ関連の設定
        when(passwordEncodingService.encodePassword(any())).thenReturn("encodedPassword");
        when(userDetailsService.loadUserByUsername(any())).thenReturn(
            new org.springframework.security.core.userdetails.User(
                "testuser", "encodedPassword",
                Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))
            )
        );

        log.debug("テストデータのセットアップ完了");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testAccessDeniedForUserCreation() throws Exception {
        log.info("権限のないユーザーによる作成テストを開始");

        // アクセス制御の設定
        when(accessControlService.canCreateUserWithRole("USER")).thenReturn(false);
        when(accessControlService.canViewUsersByRole(anyString())).thenReturn(false);

        mockMvc.perform(post("/users")
                .param("name", "Test User")
                .param("email", "test@example.com")
                .param("role", "USER"))
                .andExpect(status().isForbidden());

        log.info("権限のないユーザーによる作成テスト成功");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testAccessDeniedForUserDeletion() throws Exception {
        log.info("権限のないユーザーによる削除テストを開始");

        // 対象ユーザーの設定
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accessControlService.canDeleteUser(testUser)).thenReturn(false);
        when(accessControlService.canDeleteUser(any(Long.class)))
            .thenReturn(new AuthorizationDecision(false));

        mockMvc.perform(post("/users/1/delete"))
                .andExpect(status().isForbidden());

        log.info("権限のないユーザーによる削除テスト成功");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testAccessDeniedForUserUpdate() throws Exception {
        log.info("権限のないユーザーによる更新テストを開始");

        // 対象ユーザーの設定
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(accessControlService.canEditUser(testUser)).thenReturn(false);
        when(accessControlService.canEditUser(1L))
            .thenReturn(new AuthorizationDecision(false));

        mockMvc.perform(post("/users/1")
                .param("name", "Updated Name")
                .param("email", "updated@example.com")
                .param("role", "USER"))
                .andExpect(status().isForbidden());

        log.info("権限のないユーザーによる更新テスト成功");
    }
}