package com.example.javase17learningproject.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.example.javase17learningproject.model.Role;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.RoleRepository;
import com.example.javase17learningproject.repository.UserRepository;
import com.example.javase17learningproject.service.PasswordEncodingService;

/**
 * ユーザーコントローラー。 ユーザー関連のAPIエンドポイントを提供します。
 */
@Controller
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired 
    private UserRepository userRepository;
    
    @Autowired 
    private RoleRepository roleRepository;
    
    @Autowired 
    private PasswordEncodingService passwordEncodingService;

    /**
     * 全てのユーザーを取得し、ユーザー一覧画面を表示します。
     *
     * @param model モデル
     * @return ユーザー一覧画面のテンプレート名
     */
    @GetMapping
    public String getAllUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "users";
    }

    /**
     * 指定されたIDのユーザーを取得し、ユーザー詳細画面を表示します。
     *
     * @param id ユーザーID
     * @param model モデル
     * @return ユーザー詳細画面のテンプレート名
     * @throws ResponseStatusException ユーザーが見つからない場合
     */
    @GetMapping("/{id}")
    public String getUserById(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        model.addAttribute("user", user);
        return "user_detail";
    }

    /**
     * ユーザー編集画面を表示します。
     *
     * @param id ユーザーID
     * @param model モデル
     * @return ユーザー編集画面のテンプレート名
     * @throws ResponseStatusException ユーザーが見つからない場合
     */
    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        model.addAttribute("user", user);
        model.addAttribute("roles", roleRepository.findAll());
        return "user_edit";
    }

    /**
     * ユーザー情報を更新します。
     *
     * @param id ユーザーID
     * @param name 更新後のユーザー名
     * @param email 更新後のメールアドレス
     * @param role 更新後のロール名
     * @return 更新成功時はユーザー一覧画面へリダイレクト
     * @throws ResponseStatusException バリデーションエラーまたはユーザーが見つからない場合
     */
    @PostMapping("/{id}")
    public String updateUser(@PathVariable Long id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String role,
            Model model) {

        // バリデーション
        if (name == null || name.trim().isEmpty()) {
            User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            model.addAttribute("user", existingUser);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("errorMessage", "Name is required");
            return "user_edit";
        }

        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            model.addAttribute("user", existingUser);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("errorMessage", "Invalid email format");
            return "user_edit";
        }

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // メールアドレスの重複チェック（自分自身は除く）
        Optional<User> userWithEmail = userRepository.findByEmail(email);
        if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(id)) {
            model.addAttribute("user", existingUser);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("errorMessage", "Email already exists");
            return "user_edit";
        }

        Optional<Role> newRole = roleRepository.findByName(role);
        if (newRole.isEmpty()) {
            model.addAttribute("user", existingUser);
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("errorMessage", "Invalid role");
            return "user_edit";
        }

        existingUser.setName(name);
        existingUser.setEmail(email);
        Set<Role> roles = new HashSet<>();
        roles.add(newRole.get());
        existingUser.setRoles(roles);
        
        userRepository.save(existingUser);
        return "redirect:/users";
    }

    /**
     * ユーザー削除確認画面を表示します。
     *
     * @param id ユーザーID
     * @param model モデル
     * @return ユーザー削除確認画面のテンプレート名
     * @throws ResponseStatusException ユーザーが見つからない場合
     */
    @GetMapping("/{id}/delete")
    public String deleteUserConfirmation(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        model.addAttribute("user", user);
        return "user_delete";
    }

    /**
     * ユーザーを削除します。
     *
     * @param id ユーザーID
     * @return ユーザー一覧画面にリダイレクト
     * @throws ResponseStatusException ユーザーが見つからない場合
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
        return "redirect:/users";
    }

    /**
     * 新規ユーザー作成画面を表示します。
     *
     * @param model モデル
     * @return 新規ユーザー作成画面のテンプレート名
     */
    @GetMapping("/new")
    public String newUser(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", roleRepository.findAll());
        return "user_create";
    }

    /**
     * 新しいユーザーを作成します。
     *
     * @param name ユーザー名
     * @param email ユーザーのメールアドレス
     * @param role ユーザーの役割
     * @return ユーザー一覧画面にリダイレクト
     * @throws ResponseStatusException バリデーションエラーの場合
     */
    @PostMapping
    public String createUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String role,
            Model model) {

        // バリデーション
        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("user", new User());
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("errorMessage", "Name is required");
            return "user_create";
        }

        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            model.addAttribute("user", new User());
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("errorMessage", "Invalid email format");
            return "user_create";
        }

        // メールアドレスの重複チェック
        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("user", new User());
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("errorMessage", "Email already exists");
            return "user_create";
        }

        Optional<Role> userRole = roleRepository.findByName(role);
        if (userRole.isEmpty()) {
            model.addAttribute("user", new User());
            model.addAttribute("roles", roleRepository.findAll());
            model.addAttribute("errorMessage", "Invalid role");
            return "user_create";
        }

        String temporaryPassword = "tempPass123";
        String encodedPassword = passwordEncodingService.encodePassword(temporaryPassword);
        
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPassword(encodedPassword);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole.get());
        newUser.setRoles(roles);
        userRepository.save(newUser);
        
        return "redirect:/users";
    }

    /**
     * ユーザーを検索します。
     *
     * @param name 検索するユーザー名（オプション）
     * @param email 検索するメールアドレス（オプション）
     * @param roleName 検索するロール名（オプション）
     * @param model モデル
     * @return ユーザー一覧画面のテンプレート名
     */
    @GetMapping("/search")
    public String searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String roleName,
            Model model) {
        try {
            Role role = null;
            List<User> users;

            // 検索条件の正規化
            String normalizedName = (name != null && !name.trim().isEmpty()) ? name.trim() : null;
            String normalizedEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : null;

            // ロールによる検索
            if (roleName != null && !roleName.trim().isEmpty()) {
                role = roleRepository.findByName(roleName).orElse(null);
            }

            // 検索の実行
            users = userRepository.searchUsers(normalizedName, normalizedEmail, role);

            // 検索結果をモデルに設定
            model.addAttribute("users", users);
            model.addAttribute("searchParams", Map.of(
                "name", name != null ? name : "",
                "email", email != null ? email : "",
                "role", roleName != null ? roleName : ""
            ));
            return "users";
        } catch (Exception e) {
            log.error("Search failed", e);
            model.addAttribute("errorMessage", "検索中にエラーが発生しました");
            model.addAttribute("users", Collections.emptyList());
            return "users";
        }
    }
}