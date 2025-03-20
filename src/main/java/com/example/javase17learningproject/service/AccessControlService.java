package com.example.javase17learningproject.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.javase17learningproject.User;

/**
 * アクセス制御サービス。
 * ユーザーの役割に基づいて操作の可否を判断します。
 */
@Service
public class AccessControlService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccessControlService.class);

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
            logger.info("管理補助者によるユーザー削除: target={}", targetUser.getEmail());
            return true;
        }

        logger.warn("不正なユーザー削除の試行: user={}, target={}", 
                   currentUser.getEmail(), targetUser.getEmail());
        return false;
    }
}