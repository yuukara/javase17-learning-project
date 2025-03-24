package com.example.javase17learningproject;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.RoleRepository;
import com.example.javase17learningproject.repository.UserRepository;
import com.example.javase17learningproject.service.AccessControlService;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/cleanup.sql")
public class UserControllerTest {
    
    private static final Logger log = LoggerFactory.getLogger(UserControllerTest.class);

    @Autowired
    private MockMvc mockMvc;
@Autowired
private UserRepository userRepository;

@Autowired
private RoleRepository roleRepository;

@MockBean
private AccessControlService accessControlService;

@BeforeEach
void setUpMocks() {
    // 検索のモック設定を先に行う（より具体的な設定が優先される）
    when(accessControlService.canViewUsersByRole("ADMIN")).thenReturn(true);
    when(accessControlService.canViewUsersByRole(anyString())).thenReturn(true);
    
    when(accessControlService.canEditUser(anyLong())).thenReturn(new AuthorizationDecision(true));
    when(accessControlService.canDeleteUser(anyLong())).thenReturn(new AuthorizationDecision(true));
}

private Long userId;
    private Role adminRole;

    @BeforeEach
    @Transactional
    public void setUp() {
        log.info("テストデータのセットアップを開始");

        // データのクリーンアップ
        log.debug("既存データのクリーンアップ");
        userRepository.deleteAll();
        roleRepository.deleteAll();
        log.debug("データベースのクリーンアップが完了しました");
        
        // 管理者ロールの作成
        log.debug("管理者ロールの作成");
        adminRole = new Role("ADMIN");
        adminRole.setPrefix("ROLE_");  // プレフィックスの設定
        adminRole = roleRepository.save(adminRole);
        log.debug("管理者ロール作成完了: ID={}, 名前={}, プレフィックス={}", 
                adminRole.getId(), adminRole.getName(), adminRole.getPrefix());
        
        // テスト用管理者ユーザーの作成（テストで使用する認証ユーザーと同じ）
        log.debug("テスト用管理者ユーザーの作成");
        // ユーザー名とメールアドレスを同じ値に設定
        User adminUser = new User("admin", "admin@example.com", "password");
        adminUser.setRoles(Set.of(adminRole));
        userRepository.save(adminUser);
        log.debug("テスト用管理者ユーザー作成完了: 名前={}, メール={}, ロール={}", 
                adminUser.getName(), adminUser.getEmail(), 
                adminUser.getRoles().stream().map(Role::getName).toList());
        
        // テストユーザーの作成と保存
        log.debug("テストユーザーの作成を開始");
        User user = new User("testUser", "test@example.com", "password123");
        log.debug("テストユーザーの基本情報設定: 名前={}, メール={}", user.getName(), user.getEmail());
        
        user.setRoles(Set.of(adminRole));  // Setを使用して設定
        log.debug("テストユーザーに管理者ロールを割り当て: ロール={}", adminRole.getName());
        
        User savedUser = userRepository.save(user);
        userId = savedUser.getId();
        log.debug("テストユーザー保存完了: ID={}, 名前={}, メール={}", savedUser.getId(), savedUser.getName(), savedUser.getEmail());
        log.debug("テストユーザーのロール: {}", savedUser.getRoles().stream()
                .map(role -> role.getPrefix() + role.getName())
                .toList());

        log.info("テストデータのセットアップ完了: ユーザー数={}, ロール数={}", 
                userRepository.count(), roleRepository.count());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    public void testListUsers() throws Exception {
        log.info("ユーザー一覧表示テストを開始");
        
        try {
            log.debug("GET /users を実行");
            mockMvc.perform(get("/users"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("users"))
                    .andExpect(model().attributeExists("users"))
                    .andExpect(model().attribute("users", hasSize(2)))
                    .andDo(print());
            
            log.info("ユーザー一覧表示テストが成功");
        } catch (Exception e) {
            log.error("テスト実行中にエラーが発生: {}", e.getMessage());
            throw e;
        }
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    public void testShowUserDetail() throws Exception {
        log.info("ユーザー詳細表示テストを開始");
        
        try {
            log.debug("GET /users/{} を実行", userId);
            mockMvc.perform(get("/users/" + userId))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user_detail"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attribute("user", hasProperty("id", is(userId))))
                    .andDo(print());
            
            log.info("ユーザー詳細表示テストが成功");
        } catch (Exception e) {
            log.error("テスト実行中にエラーが発生: {}", e.getMessage());
            throw e;
        }
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    public void testShowUserCreateForm() throws Exception {
        log.info("ユーザー作成フォーム表示テストを開始");
        
        try {
            log.debug("GET /users/new を実行");
            mockMvc.perform(get("/users/new"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user_create"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attributeExists("roles"))
                    .andDo(print());
            
            log.info("ユーザー作成フォーム表示テストが成功");
        } catch (Exception e) {
            log.error("テスト実行中にエラーが発生: {}", e.getMessage());
            throw e;
        }
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    public void testShowUserEditForm() throws Exception {
        log.info("ユーザー編集フォーム表示テストを開始");
        
        try {
            log.debug("GET /users/{}/edit を実行", userId);
            mockMvc.perform(get("/users/" + userId + "/edit"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user_edit"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attribute("user", hasProperty("id", is(userId))))
                    .andExpect(model().attributeExists("roles"))
                    .andDo(print());
            
            log.info("ユーザー編集フォーム表示テストが成功");
        } catch (Exception e) {
            log.error("テスト実行中にエラーが発生: {}", e.getMessage());
            throw e;
        }
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    @Transactional
    public void testUpdateUserRole() throws Exception {
        mockMvc.perform(post("/users/" + userId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "updatedName")
                .param("email", "updated@example.com")
                .param("role", "ADMIN")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"))
                .andDo(print());

        User updatedUser = userRepository.findById(userId).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getRoles().iterator().next().getName()).isEqualTo("ADMIN");
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    @Transactional
    public void testSearchUsers() throws Exception {
        log.info("ユーザー検索テストを開始");
        
        // データベース内のユーザーを確認
        log.debug("テスト前のデータベース内ユーザー:");
        List<User> existingUsers = userRepository.findAll();
        existingUsers.forEach(u -> 
            log.debug(" - ユーザー: id={}, 名前={}, メール={}, ロール={}", 
                u.getId(), u.getName(), u.getEmail(), 
                u.getRoles().stream().map(Role::getName).toList())
        );
        int initialUserCount = existingUsers.size();
        log.debug("既存ユーザー数: {}", initialUserCount);
        
        // テスト用のユーザーをいくつか作成
        Role userRole = new Role("USER");
        userRole.setPrefix("ROLE_"); // プレフィックスの設定
        userRole = roleRepository.save(userRole);

        User user1 = new User("testUser1", "test1@example.com", "password123");
        User user2 = new User("testUser2", "test2@example.com", "password123");
        User user3 = new User("anotherUser", "another@example.com", "password123");

        user1.setRoles(Set.of(userRole)); // getRoles().addの代わりにsetRolesを使用
        user2.setRoles(Set.of(adminRole));
        user3.setRoles(Set.of(userRole));

        userRepository.saveAll(List.of(user1, user2, user3));
        log.debug("テスト用ユーザーを作成: user1={}, user2={}, user3={}", user1.getName(), user2.getName(), user3.getName());

        // データベースの状態を再確認
        log.debug("テスト用データ追加後のデータベース内ユーザー:");
        List<User> allUsers = userRepository.findAll();
        allUsers.forEach(u -> 
            log.debug(" - ユーザー: id={}, 名前={}, メール={}, ロール={}", 
                u.getId(), u.getName(), u.getEmail(), 
                u.getRoles().stream().map(Role::getName).toList())
        );
        
        // ADMIN権限を持つユーザー数を確認
        long adminUserCount = allUsers.stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN")))
                .count();
        log.debug("ADMIN権限を持つユーザー数: {}", adminUserCount);
        
        // testUserで始まるユーザー数を確認
        long testUserCount = allUsers.stream()
                .filter(u -> u.getName().startsWith("testUser"))
                .count();
        log.debug("testUserで始まるユーザー数: {}", testUserCount);

        // ユーザー名で検索
        log.debug("ユーザー名で検索を実行: 検索キーワード='testUser'");
        mockMvc.perform(get("/users/search")
                .param("name", "testUser")
                .param("role", "ADMIN")
                .with(csrf())
                .with(user("admin").roles("ADMIN"))
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andDo(result -> {
                    // レスポンスの詳細をログ出力
                    if (result.getResponse().getStatus() != 200) {
                        log.error("レスポンスエラー: ステータス={}, 内容={}", 
                                result.getResponse().getStatus(), 
                                result.getResponse().getContentAsString());
                    }
                })
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(2)))
                .andDo(print());
        log.debug("ユーザー名による検索テスト成功");

        // メールアドレスで検索
        log.debug("メールアドレスで検索を実行: 検索キーワード='test1@example.com'");
        mockMvc.perform(get("/users/search")
                .param("email", "test1@example.com")
                .param("role", "USER")  // USERロールを持つユーザーを検索
                .with(csrf())
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(1)))
                .andDo(print());
        log.debug("メールアドレスによる検索テスト成功");

        // 役割で検索
        log.debug("ロールで検索を実行: 検索キーワード='ADMIN'");
        mockMvc.perform(get("/users/search")
                .param("role", "ADMIN")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize((int)adminUserCount)))
                .andDo(print());
        log.debug("ロールによる検索テスト成功");
        
        log.info("ユーザー検索テストが成功");
    }
}
