package com.example.javase17learningproject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.RoleRepository;
import com.example.javase17learningproject.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/cleanup.sql")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Long userId;
    private Role adminRole;

    @BeforeEach
    @Transactional
    public void setUp() {
        // データのクリーンアップ
        userRepository.deleteAll();
        roleRepository.deleteAll();
        
        // 管理者ロールの作成
        adminRole = new Role("ADMIN");
        adminRole = roleRepository.save(adminRole);
        
        // テストユーザーの作成と保存
        User user = new User("testUser", "test@example.com", "password123");
        user.getRoles().add(adminRole);
        User savedUser = userRepository.save(user);
        userId = savedUser.getId();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testShowUserDetail() throws Exception {
        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(view().name("user_detail"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", hasProperty("id", is(userId))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testShowUserCreateForm() throws Exception {
        mockMvc.perform(get("/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_create"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testShowUserEditForm() throws Exception {
        mockMvc.perform(get("/users/" + userId + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", hasProperty("id", is(userId))))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    public void testUpdateUserRole() throws Exception {
        mockMvc.perform(post("/users/" + userId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "updatedName")
                .param("email", "updated@example.com")
                .param("role", "ADMIN")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        User updatedUser = userRepository.findById(userId).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getRoles().iterator().next().getName()).isEqualTo("ADMIN");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Transactional
    public void testSearchUsers() throws Exception {
        // テスト用のユーザーをいくつか作成
        Role userRole = new Role("USER");
        userRole = roleRepository.save(userRole);

        User user1 = new User("testUser1", "test1@example.com", "password123");
        User user2 = new User("testUser2", "test2@example.com", "password123");
        User user3 = new User("anotherUser", "another@example.com", "password123");

        user1.getRoles().add(userRole);
        user2.getRoles().add(adminRole);
        user3.getRoles().add(userRole);

        userRepository.saveAll(List.of(user1, user2, user3));

        // ユーザー名で検索
        mockMvc.perform(get("/users/search")
                .param("name", "testUser"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(2)));

        // メールアドレスで検索
        mockMvc.perform(get("/users/search")
                .param("email", "test1@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(1)));

        // 役割で検索
        mockMvc.perform(get("/users/search")
                .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(2)));
    }
}
