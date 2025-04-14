package com.example.javase17learningproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.annotation.Audited;
import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.repository.UserRepository;

/**
 * ユーザー管理サービス。
 * 監査ログのテスト用にメソッドに@Auditedアノテーションを付与しています。
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * ユーザー名を更新します。
     * 監査ログに記録される操作です。
     */
    @Transactional
    @Audited(eventType = "USER_UPDATED", severity = Severity.MEDIUM)
    public User updateUserName(Long userId, String newName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setName(newName);
        return userRepository.save(user);
    }

    /**
     * ユーザーを削除します。
     * 監査ログに記録される操作です。
     */
    @Transactional
    @Audited(eventType = "USER_DELETED", severity = Severity.HIGH)
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        userRepository.delete(user);
    }

    /**
     * カスタムイベントタイプでユーザーを作成します。
     * テスト用のメソッドです。
     */
    @Transactional
    @Audited(
        eventType = "CUSTOM_USER_CREATED",
        severity = Severity.HIGH,
        description = "カスタムユーザー作成処理"
    )
    public User createUserWithCustomAudit(User user) {
        return userRepository.save(user);
    }
}