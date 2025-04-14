package com.example.javase17learningproject.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import com.example.javase17learningproject.service.AccessControlService;
import com.example.javase17learningproject.service.CustomUserDetailsService;
import com.example.javase17learningproject.service.PasswordEncodingService;

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

    @Autowired
    private PasswordEncodingService passwordEncodingService;

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
        return (request, response, authentication) -> {
            logger.info("認証成功: user={}", authentication.getName());
            response.sendRedirect("/");
        };
    }

    /**
     * 認証失敗時のハンドラー
     */
    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            logger.warn("認証失敗: {}", exception.getMessage());
            response.sendRedirect("/login?error");
        };
    }

    /**
     * アクセス拒否時のハンドラー
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            logger.warn("アクセス拒否: {}, path={}", accessDeniedException.getMessage(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        };
    }

    /**
     * ログアウト成功時のハンドラー
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                logger.info("ログアウト成功: user={}", authentication.getName());
            }
            response.sendRedirect("/login?logout");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/css/**", "/js/**", "/images/**", "/login", "/").permitAll()
                .requestMatchers("/h2-console/**").permitAll() // 開発環境用
                .requestMatchers("/users/search").access((authentication, context) -> {
                    String role = context.getRequest().getParameter("role");
                    boolean canView = accessControlService.canViewUsersByRole(role);
                    return new AuthorizationDecision(canView);
                })
                .requestMatchers("/users/new").hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR")
                .requestMatchers("/users/{id}/edit").access((authentication, context) -> {
                    Long userId = Long.valueOf(context.getVariables().get("id"));
                    return accessControlService.canEditUser(userId);
                })
                .requestMatchers("/users/{id}/delete").access((authentication, context) -> {
                    Long userId = Long.valueOf(context.getVariables().get("id"));
                    return accessControlService.canDeleteUser(userId);
                })
                .requestMatchers("/users/{id}/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER")
                .requestMatchers("/users").hasAnyAuthority("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER")
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
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
                .logoutSuccessHandler(logoutSuccessHandler())
            )
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler())
            )
            // CSRF設定
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**") // 開発環境用
            )
            // H2コンソール用設定
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            // セキュリティフィルター設定
            .userDetailsService(userDetailsService)
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(new EnhancedAccessControlDebugFilter(accessControlService),
                UsernamePasswordAuthenticationFilter.class);
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
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider() {
            protected void additionalAuthenticationChecks(UserDetails userDetails,
                    UsernamePasswordAuthenticationToken authentication) throws BadCredentialsException {
                if (authentication.getCredentials() == null) {
                    logger.debug("認証失敗: 資格情報がnullです");
                    throw new BadCredentialsException("Bad credentials");
                }
                // 入力されたパスワードをログ出力
                String presentedPassword = authentication.getCredentials().toString();
                // ここでエンコードするのはログ表示のためだけであり、実際の比較にはpresentedPassword（平文）を使用すべき
                logger.info("入力されたパスワード（平文）: " + presentedPassword);
                logger.info("データベースのパスワード: " + userDetails.getPassword());
                // パスワードの一致を確認
                boolean matches = passwordEncodingService.matches(presentedPassword, userDetails.getPassword());
                logger.info("パスワードの一致確認: " + matches);
                
                if (!matches) {
                    throw new BadCredentialsException("パスワードが一致しません");
                }

                // ユーザーステータスの確認
                logger.info("アカウント有効: " + userDetails.isEnabled());
                logger.info("アカウントロック状態: " + (!userDetails.isAccountNonLocked()));
                logger.info("アカウント有効期限: " + userDetails.isAccountNonExpired());
                logger.info("認証情報有効期限: " + userDetails.isCredentialsNonExpired());
                logger.info("認証情報有効期限: " + userDetails.isCredentialsNonExpired());
                
                try {
                    // 親クラスの認証チェックを実行
                    super.additionalAuthenticationChecks(userDetails, authentication);
                    logger.info("認証チェック成功");
                } catch (BadCredentialsException e) {
                    logger.error("認証チェック失敗: " + e.getMessage());
                    throw e;
                }
            }
        };
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}