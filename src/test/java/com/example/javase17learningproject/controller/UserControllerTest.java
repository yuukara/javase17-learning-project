package com.example.javase17learningproject.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.RoleRepository;
import com.example.javase17learningproject.repository.UserRepository;
import com.example.javase17learningproject.service.AccessControlService;
import com.example.javase17learningproject.service.CustomUserDetailsService;
import com.example.javase17learningproject.service.PasswordEncodingService;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AccessControlService.class, PasswordEncodingService.class})
class UserControllerTest {

    private static final Logger log = LoggerFactory.getLogger(UserControllerTest.class);

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

        // ロールの設定
        userRole = new Role("USER");
        userRole.setId(1L);
        adminRole = new Role("ADMIN");
        adminRole.setId(2L);
        
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findAll()).thenReturn(Arrays.asList(userRole, adminRole));
        
        // テストユーザーの設定
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRoles(Set.of(userRole));

        // リポジトリのモック設定
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        // セキュリティ関連の設定
        when(passwordEncodingService.encodePassword(anyString())).thenReturn("encodedPassword");
        when(accessControlService.canViewUsersByRole(anyString())).thenReturn(true);
        when(accessControlService.canEditUser(anyLong())).thenReturn(new AuthorizationDecision(true));
        when(accessControlService.canEditUser(any(User.class))).thenReturn(true);
        when(accessControlService.canDeleteUser(anyLong())).thenReturn(new AuthorizationDecision(true));
        when(accessControlService.canDeleteUser(any(User.class))).thenReturn(true);

        log.debug("テストデータのセットアップ完了");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUser() throws Exception {
        log.info("ユーザー作成テストを開始");

        String encodedPassword = "encodedPassword";
        when(passwordEncodingService.encodePassword("tempPass123")).thenReturn(encodedPassword);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getName()).isEqualTo("Test User");
            assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
            assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedUser.getRoles()).contains(userRole);
            return savedUser;
        });

        mockMvc.perform(post("/users")
                .param("name", "Test User")
                .param("email", "test@example.com")
                .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        log.info("ユーザー作成テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testListUsers() throws Exception {
        log.info("ユーザー一覧表示テストを開始");

        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", users));

        log.info("ユーザー一覧表示テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testShowUserDetail() throws Exception {
        log.info("ユーザー詳細表示テストを開始");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_detail"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", testUser));

        log.info("ユーザー詳細表示テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUser() throws Exception {
        log.info("ユーザー更新テストを開始");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertThat(savedUser.getId()).isEqualTo(1L);
            assertThat(savedUser.getName()).isEqualTo("Updated Name");
            assertThat(savedUser.getEmail()).isEqualTo("updated@example.com");
            assertThat(savedUser.getRoles()).containsExactly(adminRole);
            return savedUser;
        });

        mockMvc.perform(post("/users/1")
                .param("name", "Updated Name")
                .param("email", "updated@example.com")
                .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        log.info("ユーザー更新テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteUser() throws Exception {
        log.info("ユーザー削除テストを開始");

        when(userRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(post("/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        log.info("ユーザー削除テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testEditUser() throws Exception {
        log.info("ユーザー編集画面表示テストを開始");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findAll()).thenReturn(Arrays.asList(userRole, adminRole));

        mockMvc.perform(get("/users/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_edit"))
                .andExpect(model().attributeExists("user", "roles"));

        log.info("ユーザー編集画面表示テスト成功");
    }
}