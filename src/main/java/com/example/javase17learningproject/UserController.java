package com.example.javase17learningproject;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ユーザーコントローラー。 ユーザー関連のAPIエンドポイントを提供します。
 */
@Controller
@RequestMapping("/users")
public class UserController {

  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;

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
   * @return ユーザー詳細画面のテンプレート名。見つからない場合は404 Not Found。
   */
  @GetMapping("/{id}")
  public String getUserById(@PathVariable Long id, Model model) {
    Optional<User> user = userRepository.findById(id);
    if (user.isPresent()) {
      model.addAttribute("user", user.get());
      return "user_detail";
    } else {
      return "error/404"; // 404エラー画面を表示
    }
  }

  /**
   * ユーザー編集画面を表示します。
   *
   * @param id ユーザーID
   * @param model モデル
   * @return ユーザー編集画面のテンプレート名。見つからない場合は404 Not Found。
   */
  @GetMapping("/{id}/edit")
  public String editUser(@PathVariable Long id, Model model) {
    Optional<User> user = userRepository.findById(id);
    if (user.isPresent()) {
      model.addAttribute("user", user.get());
      model.addAttribute("roles", roleRepository.findAll());
      return "user_edit";
    } else {
      return "error/404"; // 404エラー画面を表示
    }
  }

  /**
   * ユーザーを更新します。
   *
   * @param id ユーザーID
   * @param userDetails 更新するユーザー情報
   * @return ユーザー一覧画面にリダイレクト
   */
  @PostMapping("/{id}")
  public String updateUser(@PathVariable Long id, @RequestParam String name, @RequestParam String email, @RequestParam("role") String role) {
    Optional<User> user = userRepository.findById(id);
    if (user.isPresent()) {
      User existingUser = user.get();
      existingUser.setName(name);
      existingUser.setEmail(email);
      Optional<Role> newRole = roleRepository.findByName(role);
      newRole.ifPresent(existingUser::setRole);
      userRepository.save(existingUser);
      return "redirect:/users";
    } else {
      return "error/404"; // 404エラー画面を表示
    }
  }

  /**
   * ユーザー削除確認画面を表示します。
   *
   * @param id ユーザーID
   * @param model モデル
   * @return ユーザー削除確認画面のテンプレート名。見つからない場合は404 Not Found。
   */
  @GetMapping("/{id}/delete")
  public String deleteUserConfirmation(@PathVariable Long id, Model model) {
    Optional<User> user = userRepository.findById(id);
    if (user.isPresent()) {
      model.addAttribute("user", user.get());
      return "user_delete"; // ユーザー削除確認画面のテンプレート
    } else {
      return "error/404"; // 404エラー画面を表示
    }
  }

  /**
   * ユーザーを削除します。
   *
   * @param id ユーザーID
   * @return ユーザー一覧画面にリダイレクト
   */
  @PostMapping("/{id}/delete")
  public String deleteUser(@PathVariable Long id) {
    if (userRepository.existsById(id)) {
      userRepository.deleteById(id);
      return "redirect:/users";
    } else {
      return "error/404"; // 404エラー画面を表示
    }
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
   */
  @PostMapping
  public String createUser(@RequestParam String name, @RequestParam String email, @RequestParam("role") String role) {
    Optional<Role> newRole = roleRepository.findByName(role);
    newRole.ifPresent(roleValue -> {
        User newUser = new User(name, email, roleValue);
        userRepository.save(newUser);
    });
    return "redirect:/users";
  }

  /**
   * ユーザーを検索し、検索結果をユーザー一覧画面に表示します。
   *
   * @param name ユーザー名
   * @param email ユーザーのメールアドレス
   * @param role ユーザーの役割
   * @param model モデル
   * @return ユーザー一覧画面のテンプレート名
   */
  @GetMapping("/search")
  public String searchUsers(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String role,
      Model model) {
    List<User> users = userRepository.searchUsers(name, email, role);
    model.addAttribute("users", users);
    return "users";
  }
}
