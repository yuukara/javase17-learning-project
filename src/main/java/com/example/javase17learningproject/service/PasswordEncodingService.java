package com.example.javase17learningproject.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * パスワードのエンコーディングを担当するサービス。
 * パスワードの暗号化に関する責任を集中管理します。
 */
@Service
public class PasswordEncodingService {
    
    private final PasswordEncoder passwordEncoder;

    public PasswordEncodingService() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * パスワードをエンコードします。
     * 既にBCryptでエンコードされている場合は、そのまま返します。
     * 
     * @param password エンコードするパスワード（平文またはエンコード済み）
     * @return エンコードされたパスワード
     */
    public String encodePassword(String password) {
        if (password == null) {
            return null;
        }
        
        // BCryptエンコード済みかどうかをチェック
        if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            return password;  // エンコード済みの場合はそのまま返す
        }
        
        return passwordEncoder.encode(password);  // 平文の場合はエンコード
    }

    /**
     * パスワードが一致するかチェックします。
     * 
     * @param rawPassword 平文のパスワード
     * @param encodedPassword エンコードされたパスワード
     * @return パスワードが一致する場合はtrue
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}