package com.example.javase17learningproject.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class UserControllerSearchTest {

    private static final Logger log = LoggerFactory.getLogger(UserControllerSearchTest.class);

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
        
        // テストユーザーの設定
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRoles(Set.of(userRole));

        log.debug("テストデータのセットアップ完了");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSearchByName() throws Exception {
        log.info("名前による検索テストを開始");
        List<User> usersWithName = Arrays.asList(testUser);

        when(userRepository.searchUsers(eq("Test"), isNull(), isNull()))
            .thenReturn(usersWithName);

        mockMvc.perform(get("/users/search")
                .param("name", "Test"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attribute("users", usersWithName))
                .andExpect(model().attribute("searchParams", Map.of(
                    "name", "Test",
                    "email", "",
                    "role", ""
                )));

        log.info("名前による検索テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSearchByRole() throws Exception {
        log.info("ロールによる検索テストを開始");
        List<User> usersWithRole = Arrays.asList(testUser);

        when(roleRepository.findByName("USER"))
            .thenReturn(Optional.of(userRole));
        when(userRepository.searchUsers(isNull(), isNull(), eq(userRole)))
            .thenReturn(usersWithRole);

        mockMvc.perform(get("/users/search")
                .param("roleName", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attribute("users", usersWithRole))
                .andExpect(model().attribute("searchParams", Map.of(
                    "name", "",
                    "email", "",
                    "role", "USER"
                )));

        log.info("ロールによる検索テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSearchWithEmptyParameters() throws Exception {
        log.info("パラメータなしの検索テストを開始");
        List<User> allUsers = Arrays.asList(testUser);

        when(userRepository.searchUsers(isNull(), isNull(), isNull()))
            .thenReturn(allUsers);

        mockMvc.perform(get("/users/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attribute("users", allUsers))
                .andExpect(model().attribute("searchParams", Map.of(
                    "name", "",
                    "email", "",
                    "role", ""
                )));

        log.info("パラメータなしの検索テスト成功");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSearchByEmail() throws Exception {
        log.info("メールアドレスによる検索テストを開始");
        List<User> usersWithEmail = Arrays.asList(testUser);

        when(userRepository.searchUsers(isNull(), eq("test@example.com"), isNull()))
            .thenReturn(usersWithEmail);

        mockMvc.perform(get("/users/search")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attribute("users", usersWithEmail))
                .andExpect(model().attribute("searchParams", Map.of(
                    "name", "",
                    "email", "test@example.com",
                    "role", ""
                )));

        log.info("メールアドレスによる検索テスト成功");
    }
}