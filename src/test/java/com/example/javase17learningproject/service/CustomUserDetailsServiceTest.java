package com.example.javase17learningproject.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.javase17learningproject.model.User;
import com.example.javase17learningproject.repository.UserRepository;

class CustomUserDetailsServiceTest {

    private CustomUserDetailsService userDetailsService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new CustomUserDetailsService();
        // リフレクションを使用してprivateフィールドを設定
        try {
            var field = CustomUserDetailsService.class.getDeclaredField("userRepository");
            field.setAccessible(true);
            field.set(userDetailsService, userRepository);
        } catch (Exception e) {
            fail("テストのセットアップに失敗しました: " + e.getMessage());
        }
    }

    @Test
    void loadUserByUsername_存在するユーザー_正常にロード() {
        // 準備
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // 実行
        UserDetails result = userDetailsService.loadUserByUsername(email);

        // 検証
        assertNotNull(result);
        assertEquals(email, result.getUsername());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_存在しないユーザー_例外をスロー() {
        // 準備
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // 実行と検証
        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(email);
        });

        assertTrue(exception.getMessage().contains(email));
        verify(userRepository).findByEmail(email);
    }
}