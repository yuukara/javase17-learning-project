package com.example.javase17learningproject.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.javase17learningproject.model.User;

/**
 * ユーザー情報のリポジトリインターフェース。
 */
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * メールアドレスでユーザーを検索します。
     *
     * @param email 検索対象のメールアドレス
     * @return ユーザー情報（Optional）
     */
    Optional<User> findByEmail(String email);

    /**
     * 名前に指定された文字列を含むユーザーを検索します。
     *
     * @param name 検索対象の名前
     * @return ユーザーのリスト
     */
    List<User> findByNameContaining(String name);

    /**
     * 指定された役割を持つユーザーを検索します。
     *
     * @param roleName 検索対象の役割名
     * @return ユーザーのリスト
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoles(@Param("roleName") String roleName);

    /**
     * 指定された条件でユーザーを検索します。
     * nullの条件は無視されます。
     *
     * @param name 名前（部分一致）
     * @param email メールアドレス（完全一致）
     * @param role 役割名
     * @return 条件に一致するユーザーのリスト
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r " +
           "WHERE (:name IS NULL OR u.name LIKE %:name%) " +
           "AND (:email IS NULL OR u.email = :email) " +
           "AND (:role IS NULL OR r.name = :role)")
    List<User> searchUsers(
        @Param("name") String name,
        @Param("email") String email,
        @Param("role") String role
    );
}