package com.example.javase17learningproject;

import java.util.List;
import java.util.Optional;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Long userId;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (adminRole == null) {
            adminRole = new Role("ADMIN");
            roleRepository.save(adminRole);
        }
        User user = new User("testUser", "test@example.com", adminRole);
        User savedUser = userRepository.save(user);
        userId = savedUser.getId();
    }

    @Test
    public void testListUsers() throws Exception {
        List<User> users = userRepository.findAll();
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(users.size())));
    }

    @Test
    public void testShowUserDetail() throws Exception {
        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(view().name("user_detail"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", hasProperty("id", is(userId))));
    }

    @Test
    public void testShowUserCreateForm() throws Exception {
        mockMvc.perform(get("/users/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_create"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    public void testShowUserEditForm() throws Exception {
        mockMvc.perform(get("/users/" + userId + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", hasProperty("id", is(userId))))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    public void testUpdateUserRole() throws Exception {
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (adminRole == null) {
            adminRole = new Role("ADMIN");
            roleRepository.save(adminRole);
        }
        Optional<Role> foundRole = roleRepository.findByName("ADMIN");
        assertThat(foundRole).isPresent();
        User user = userRepository.findById(userId).orElse(null);
        assertThat(user).isNotNull();
        foundRole.ifPresent(user::setRole);
        userRepository.save(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/" + userId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "updatedName")
                .param("email", "updated@example.com")
                .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        User updatedUser = userRepository.findById(userId).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getRole().getName()).isEqualTo("ADMIN");
    }

    @Test
    public void testSearchUsers() throws Exception {
        // テスト前にデータをクリア
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // テスト用のユーザーをいくつか作成
        Role userRole = roleRepository.findByName("USER").orElse(null);
        if (userRole == null) {
            userRole = new Role("USER");
            userRole = roleRepository.save(userRole);
        }
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (adminRole == null) {
            adminRole = new Role("ADMIN");
            adminRole = roleRepository.save(adminRole);
        }
        User user1 = new User("testUser1", "test1@example.com", userRole);
        User user2 = new User("testUser2", "test2@example.com", adminRole);
        User user3 = new User("anotherUser", "another@example.com", userRole);
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);
        user3 = userRepository.save(user3);
        user1.setRole(userRole);
        user2.setRole(adminRole);
        user3.setRole(userRole);
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
                .andExpect(model().attribute("users", hasSize(1)));

        // 複数の条件で検索
        mockMvc.perform(get("/users/search")
                .param("name", "testUser")
                .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(1)));

        // 検索結果がない場合
        mockMvc.perform(get("/users/search")
                .param("name", "nonExistingUser"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(0)));
    }
}
