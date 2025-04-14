package com.example.javase17learningproject.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.RoleRepository;
import com.example.javase17learningproject.repository.UserRepository;
import com.example.javase17learningproject.service.PasswordEncodingService;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/cleanup.sql")
public class SecurityConfigTest {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordEncodingService passwordEncodingService;

    private Long userId;
    private static final String TEST_PASSWORD = "password123";
    private String encodedPassword;

    @BeforeEach
    @Transactional
    @SuppressWarnings("unused")  // 「使われていない」警告を抑制
    void prepareTestUserData() {  // より具体的な名前に変更
        userRepository.deleteAll();
        roleRepository.deleteAll();

        encodedPassword = passwordEncodingService.encodePassword(TEST_PASSWORD);
        logger.info("テストパスワード: {}", TEST_PASSWORD);
        logger.info("エンコード後パスワード: {}", encodedPassword);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole = roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setName("USER");
        userRole = roleRepository.save(userRole);

        User adminUser = new User();
        adminUser.setName("Admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(encodedPassword);
        adminUser.getRoles().add(adminRole);
        User savedUser = userRepository.save(adminUser);
        userId = savedUser.getId();

        User testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("user@example.com");
        testUser.setPassword(encodedPassword);
        testUser.getRoles().add(userRole);
        userRepository.save(testUser);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        
        // ログイン実行
        MvcResult result = mockMvc.perform(post("/login")
                .session(session)
                .param("username", username)
                .param("password", password)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        // 更新されたセッションを取得
        MockHttpSession updatedSession = (MockHttpSession) result.getRequest().getSession();
        
        // セッション情報をログ出力
        if (updatedSession == null) {
            logger.warn("ログイン実行: user={}, 元のsessionId={}, 新しいセッションがnullです", 
                    username, session.getId());
            throw new IllegalStateException("新しいセッションがnullです");
        }
        logger.info("ログイン実行: user={}, 元のsessionId={}, 新しいsessionId={}", 
                username, session.getId(), updatedSession.getId());

        // SecurityContextの状態を確認
        SecurityContext securityContext = (SecurityContext) updatedSession.getAttribute("SPRING_SECURITY_CONTEXT");
        logger.info("認証状態: {}", securityContext);

        // セッションの生存確認
        mockMvc.perform(get("/")
                .session(updatedSession))
                .andReturn();

        return updatedSession;
    }

    @Test
    void アクセス制御_ADMIN権限_全アクセス可能() throws Exception {
        MockHttpSession session = login("admin@example.com", TEST_PASSWORD);

        mockMvc.perform(get("/users")
                .session(session))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/users/new")
                .session(session))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/users/" + userId)
                .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void アクセス制御_USER権限_制限付きアクセス() throws Exception {
        MockHttpSession session = login("user@example.com", TEST_PASSWORD);

        mockMvc.perform(get("/users")
                .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/new")
                .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void アクセス制御_USER権限_他のユーザー情報へのアクセス制限() throws Exception {
        MockHttpSession session = login("user@example.com", TEST_PASSWORD);

        mockMvc.perform(get("/users/" + userId + "/edit")
                .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void アクセス制御_未認証_ログインページにリダイレクト() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void ログイン_正しい認証情報_認証成功() throws Exception {
        User user = userRepository.findByEmail("admin@example.com").orElse(null);
        assertNotNull(user, "ユーザーがデータベースに存在する必要があります");
        logger.info("認証前の保存パスワード: {}", user.getPassword());
        logger.info("認証用パスワード（平文）: {}", TEST_PASSWORD);
        logger.info("パスワード一致確認: {}", passwordEncodingService.matches(TEST_PASSWORD, user.getPassword()));

        assertTrue(passwordEncodingService.matches(TEST_PASSWORD, user.getPassword()),
                  "パスワードが正しくエンコードされている必要があります");

        MockHttpSession session = login("admin@example.com", TEST_PASSWORD);
        
        mockMvc.perform(get("/users")
                .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void ログイン_誤った認証情報_認証失敗() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "wrong@example.com")
                .param("password", "wrongpassword")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void ログアウト() throws Exception {
        MockHttpSession session = login("admin@example.com", TEST_PASSWORD);

        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        mockMvc.perform(get("/users")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void 認証_ユーザー情報確認() throws Exception {
        User user = userRepository.findByEmail("admin@example.com").orElse(null);
        assertNotNull(user, "ユーザーがデータベースに存在する必要があります");
        assertNotNull(user.getPassword(), "パスワードが設定されている必要があります");
        
        logger.info("保存されているパスワード: {}", user.getPassword());
        logger.info("パスワード一致確認: {}", passwordEncodingService.matches(TEST_PASSWORD, user.getPassword()));
        
        assertTrue(passwordEncodingService.matches(TEST_PASSWORD, user.getPassword()),
                  "パスワードが正しくエンコードされている必要があります");
        
        MockHttpSession session = login("admin@example.com", TEST_PASSWORD);
        
        mockMvc.perform(get("/users")
                .session(session))
                .andExpect(status().isOk());
    }
}