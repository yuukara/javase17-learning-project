package com.example.javase17learningproject.controller;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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
class UserControllerValidationTest {

    private static final Logger log = LoggerFactory.getLogger(UserControllerValidationTest.class);

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

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        log.debug("テストデータのセットアップ完了");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUserValidationError() throws Exception {
        log.info("ユーザー作成のバリデーションエラーテストを開始");

        mockMvc.perform(post("/users")
                .param("name", "") // 空の名前
                .param("email", "invalid-email") // 不正なメールアドレス
                .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_create"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attributeExists("errorMessage"));

        log.info("ユーザー作成のバリデーションエラーテスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserValidationError() throws Exception {
        log.info("ユーザー更新のバリデーションエラーテストを開始");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/users/1")
                .param("name", "") // 空の名前
                .param("email", "invalid-email") // 不正なメールアドレス
                .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attributeExists("errorMessage"));

        log.info("ユーザー更新のバリデーションエラーテスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUserDuplicateEmail() throws Exception {
        log.info("重複メールアドレスによるユーザー作成テストを開始");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/users")
                .param("name", "New User")
                .param("email", "test@example.com") // 既存のメールアドレス
                .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_create"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attribute("errorMessage", "Email already exists"));

        log.info("重複メールアドレスによるユーザー作成テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateUserInvalidRole() throws Exception {
        log.info("無効なロールでのユーザー作成テストを開始");

        when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());

        mockMvc.perform(post("/users")
                .param("name", "Test User")
                .param("email", "test@example.com")
                .param("role", "INVALID_ROLE"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_create"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attribute("errorMessage", "Invalid role"));

        log.info("無効なロールでのユーザー作成テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateUserInvalidRole() throws Exception {
        log.info("無効なロールでのユーザー更新テストを開始");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());

        mockMvc.perform(post("/users/1")
                .param("name", "Updated Name")
                .param("email", "updated@example.com")
                .param("role", "INVALID_ROLE"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attribute("errorMessage", "Invalid role"));

        log.info("無効なロールでのユーザー更新テスト成功");
    }
}