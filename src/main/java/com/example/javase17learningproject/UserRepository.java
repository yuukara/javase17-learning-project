package com.example.javase17learningproject;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ユーザーリポジトリ。
 * ユーザーエンティティのデータベース操作を提供します。
 */
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r IN :role")
    List<User> findByRoles(@Param("role") Role role);

    List<User> findByNameContaining(String name);
    
    Optional<User> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE " +
            "(:name is null or u.name LIKE %:name%) AND " +
            "(:email is null or u.email LIKE %:email%) AND " +
            "(:role is null or r.name LIKE %:role%)")
    List<User> searchUsers(@Param("name") String name,
                         @Param("email") String email,
                         @Param("role") String role);
}
