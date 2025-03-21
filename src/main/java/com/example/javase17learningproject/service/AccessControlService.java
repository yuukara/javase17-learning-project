package com.example.javase17learningproject.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.javase17learningproject.User;
import com.example.javase17learningproject.UserRepository;

/**
 * アクセス制御サービス。
 * ユーザーの役割に基づいて操作の可否を判断します。
 */
@Service
public class AccessControlService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccessControlService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * 指定されたIDのユーザーを取得します。
     * 
     * @param id ユーザーID
     * @return 該当するユーザー、存在しない場合はnull
     */
    private User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * 現在のユーザーが指定されたユーザーを編集できるかチェックします。
     * 
     * @param targetUser 編集対象のユーザー
     * @return 編集可能な場合はtrue
     */
    public boolean canEditUser(User targetUser) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        // 管理者は全てのユーザーを編集可能
        if (currentUser.getRole().getName().equals("ADMIN")) {
            logger.debug("管理者によるユーザー編集: target={}", targetUser.getEmail());
            return true;
        }

        // 管理補助者は一般ユーザーのみ編集可能
        if (currentUser.getRole().getName().equals("MODERATOR") &&
            targetUser.getRole().getName().equals("USER")) {
            logger.debug("管理補助者によるユーザー編集: target={}", targetUser.getEmail());
            return true;
        }

        // 一般ユーザーは自分自身のみ編集可能
        if (currentUser.getRole().getName().equals("USER") &&
            currentUser.getId().equals(targetUser.getId())) {
            logger.debug("ユーザーによる自身の編集: user={}", currentUser.getEmail());
            return true;
        }

        logger.warn("不正なユーザー編集の試行: user={}, target={}", 
                   currentUser.getEmail(), targetUser.getEmail());
        return false;
    }

    /**
     * 現在のユーザーが指定されたIDのユーザーを編集できるかチェックします。
     * 
     * @param userId 編集対象のユーザーID
     * @return 認可の判断結果
     */
    public AuthorizationDecision canEditUser(Long userId) {
        User targetUser = getUserById(userId);
        if (targetUser == null) {
            logger.warn("存在しないユーザーの編集が試行されました: id={}", userId);
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(canEditUser(targetUser));
    }

    /**
     * 現在のユーザーが指定されたユーザーを削除できるかチェックします。
     * 
     * @param targetUser 削除対象のユーザー
     * @return 削除可能な場合はtrue
     */
    public boolean canDeleteUser(User targetUser) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        // 管理者は全てのユーザーを削除可能
        if (currentUser.getRole().getName().equals("ADMIN")) {
            logger.info("管理者によるユーザー削除: target={}", targetUser.getEmail());
            return true;
        }

        // 管理補助者は一般ユーザーのみ削除可能
        if (currentUser.getRole().getName().equals("MODERATOR") &&
            targetUser.getRole().getName().equals("USER")) {
            logger.info("管理補助者によるユーザー削除: target={}", targetUser.getEmail());
            return true;
        }

        logger.warn("不正なユーザー削除の試行: user={}, target={}", 
                   currentUser.getEmail(), targetUser.getEmail());
        return false;
    }

    /**
     * 現在のユーザーが指定されたIDのユーザーを削除できるかチェックします。
     * 
     * @param userId 削除対象のユーザーID
     * @return 認可の判断結果
     */
    public AuthorizationDecision canDeleteUser(Long userId) {
        User targetUser = getUserById(userId);
        if (targetUser == null) {
            logger.warn("存在しないユーザーの削除が試行されました: id={}", userId);
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(canDeleteUser(targetUser));
    }
    /**
     * 指定された役割のユーザー一覧を表示できるかチェックします。
     *
     * @param role 表示対象の役割名
     * @return 表示可能な場合はtrue
     */
    public boolean canViewUsersByRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        // 管理者は全ての役割のユーザーを表示可能
        if (currentUser.getRole().getName().equals("ADMIN")) {
            logger.debug("管理者によるユーザー一覧表示: role={}", role);
            return true;
        }

        // 管理補助者は一般ユーザーのみ表示可能
        if (currentUser.getRole().getName().equals("MODERATOR") &&
            role.equals("USER")) {
            logger.debug("管理補助者による一般ユーザー一覧表示");
            return true;
        }

        // 一般ユーザーは一般ユーザーのみ表示可能
        if (currentUser.getRole().getName().equals("USER") &&
            role.equals("USER")) {
            logger.debug("一般ユーザーによる一般ユーザー一覧表示");
            return true;
        }

        logger.warn("不正なユーザー一覧表示の試行: user={}, role={}",
                   currentUser.getEmail(), role);
        return false;
    }

    /**
     * 指定された役割でユーザーを作成できるかチェックします。
     *
     * @param role 作成するユーザーの役割名
     * @return 作成可能な場合はtrue
     */
    public boolean canCreateUserWithRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();

        // 管理者は全ての役割のユーザーを作成可能
        if (currentUser.getRole().getName().equals("ADMIN")) {
            logger.info("管理者によるユーザー作成: role={}", role);
            return true;
        }

        // 管理補助者は一般ユーザーのみ作成可能
        if (currentUser.getRole().getName().equals("MODERATOR") &&
            role.equals("USER")) {
            logger.info("管理補助者による一般ユーザー作成");
            return true;
        }

        logger.warn("不正なユーザー作成の試行: user={}, role={}",
                   currentUser.getEmail(), role);
        return false;
    }
}