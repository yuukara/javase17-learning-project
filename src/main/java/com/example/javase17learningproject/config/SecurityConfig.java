package com.example.javase17learningproject.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import com.example.javase17learningproject.service.AccessControlService;
import com.example.javase17learningproject.service.CustomUserDetailsService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * セキュリティ設定クラス。
 * Spring Securityの設定を行います。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * パスワードエンコーダーのBeanを定義します。
     * BCryptアルゴリズムを使用してパスワードをハッシュ化します。
     *
     * @return BCryptPasswordEncoderのインスタンス
     */
    @Bean
    @ConditionalOnMissingBean
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
    /**
     * 認証成功時のハンドラー
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                logger.info("認証成功: user={}", authentication.getName());
                response.sendRedirect("/");
            }
        };
    }

    /**
     * 認証失敗時のハンドラー
     */
    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException exception) throws IOException, ServletException {
                logger.warn("認証失敗: {}", exception.getMessage());
                response.sendRedirect("/login?error");
            }
        };
    }

    /**
     * アクセス拒否時のハンドラー
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response,
                    org.springframework.security.access.AccessDeniedException accessDeniedException)
                    throws IOException, ServletException {
                logger.warn("アクセス拒否: {}, path={}", accessDeniedException.getMessage(), request.getRequestURI());
                response.sendRedirect("/error/403");
            }
        };
    }

    /**
     * ログアウト成功時のハンドラー
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new LogoutSuccessHandler() {
            @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                if (authentication != null) {
                    logger.info("ログアウト成功: user={}", authentication.getName());
                }
                response.sendRedirect("/login?logout");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/css/**", "/js/**", "/images/**", "/login").permitAll()
                .requestMatchers("/h2-console/**").permitAll() // 開発環境用
                .requestMatchers("/users/search").access((authentication, context) -> {
                    String role = context.getRequest().getParameter("role");
                    boolean canView = accessControlService.canViewUsersByRole(role);
                    return new AuthorizationDecision(canView);
                })
                .requestMatchers("/users/new").hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR")
                .requestMatchers("/users/{id}").hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER")
                .requestMatchers("/users/{id}/edit").access((authentication, context) -> {
                    Long userId = Long.parseLong(context.getVariables().get("id"));
                    return accessControlService.canEditUser(userId);
                })
                .requestMatchers("/users/{id}/delete").access((authentication, context) -> {
                    Long userId = Long.parseLong(context.getVariables().get("id"));
                    return accessControlService.canDeleteUser(userId);
                })
                .requestMatchers("/users/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll()
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
            )
            .logout(logout -> logout
                .permitAll()
                .logoutSuccessHandler(logoutSuccessHandler())
            )
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler())
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**") // 開発環境用
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame
                    .sameOrigin() // H2コンソール用
                )
            )
            .userDetailsService(userDetailsService); // カスタムUserDetailsServiceを設定

        return http.build();
    }

    /**
     * AuthenticationManagerを構成します。
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * DaoAuthenticationProviderを構成します。
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}