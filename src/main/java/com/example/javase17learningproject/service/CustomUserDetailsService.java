package com.example.javase17learningproject.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.javase17learningproject.repository.UserRepository;

/**
 * カスタムUserDetailsServiceの実装クラス。
 * データベースからユーザー情報を取得し、認証に使用します。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("ユーザー認証を試行: {}", email);
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("ユーザーが見つかりません: {}", email);
                    return new UsernameNotFoundException("ユーザーが見つかりません: " + email);
                });
    }
}