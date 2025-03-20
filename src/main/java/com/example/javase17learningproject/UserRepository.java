package com.example.javase17learningproject;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ユーザーリポジトリ。
 * ユーザーエンティティのデータベース操作を提供します。
 */
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE " +
            "(:name is null or u.name LIKE %:name%) AND " +
            "(:email is null or u.email LIKE %:email%) AND " +
            "(:role is null or u.role.name LIKE %:role%)")
    List<User> searchUsers(@Param("name") String name,
                             @Param("email") String email,
                             @Param("role") String role);
}
