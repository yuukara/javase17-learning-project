package com.example.javase17learningproject.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.javase17learningproject.model.Role;

/**
 * 役割情報のリポジトリインターフェース。
 */
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * 指定された名前の役割を検索します。
     *
     * @param name 検索対象の役割名
     * @return 役割情報（Optional）
     */
    Optional<Role> findByName(String name);
}