package com.example.javase17learningproject.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.javase17learningproject.service.AccessControlService;

/**
 * セキュリティ設定クラス。
 * Spring Securityの設定を行います。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private AccessControlService accessControlService;

    /**
     * パスワードエンコーダーのBeanを定義します。
     * BCryptアルゴリズムを使用してパスワードをハッシュ化します。
     *
     * @return BCryptPasswordEncoderのインスタンス
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * セキュリティフィルターチェーンの設定を行います。
     * CSRF対策、セッション管理、URLごとのアクセス制御などを設定します。
     *
     * @param http HttpSecurityオブジェクト
     * @return 設定されたSecurityFilterChain
     * @throws Exception 設定中にエラーが発生した場合
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll() // 開発環境用
                .requestMatchers("/").permitAll() // トップページを許可
                .requestMatchers("/users").permitAll()
                .requestMatchers("/users/search").hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER")
                .requestMatchers("/users/new").hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR")
                .requestMatchers("/users/{id}").hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER")
                .requestMatchers("/users/{id}/edit").access((authentication, context) ->
                    accessControlService.canEditUser(Long.parseLong(context.getVariables().get("id"))))
                .requestMatchers("/users/{id}/delete").access((authentication, context) ->
                    accessControlService.canDeleteUser(Long.parseLong(context.getVariables().get("id"))))
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**") // 開発環境用
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame
                    .sameOrigin() // H2コンソール用
                )
            );

        return http.build();
    }
}