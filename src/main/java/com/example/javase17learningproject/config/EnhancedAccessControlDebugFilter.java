package com.example.javase17learningproject.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerMapping;

import com.example.javase17learningproject.service.AccessControlService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * 強化されたアクセス制御デバッグフィルター
 * アクセス制御のデバッグに役立つ詳細情報を収集・ログ出力します
 */
public class EnhancedAccessControlDebugFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAccessControlDebugFilter.class);
    
    private final List<RequestMatcher> securedPaths = new ArrayList<>();
    private final AccessControlService accessControlService;
    
    public EnhancedAccessControlDebugFilter(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
        
        // セキュリティ設定されたパスを登録
        securedPaths.add(new AntPathRequestMatcher("/users"));
        securedPaths.add(new AntPathRequestMatcher("/users/search"));
        securedPaths.add(new AntPathRequestMatcher("/users/new"));
        securedPaths.add(new AntPathRequestMatcher("/users/{id}/edit"));
        securedPaths.add(new AntPathRequestMatcher("/users/{id}/delete"));
        securedPaths.add(new AntPathRequestMatcher("/users/{id}/**"));
    }
    
    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestUri = httpRequest.getRequestURI();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        logger.debug("========== アクセス制御デバッグ情報 開始 ==========");
        logger.debug("リクエストURI: {}", requestUri);
        logger.debug("HTTPメソッド: {}", httpRequest.getMethod());
        logger.debug("リモートアドレス: {}", httpRequest.getRemoteAddr());
        
        // リクエストパラメータをログ出力
        logRequestParameters(httpRequest);
        
        // パス変数をログ出力
        logPathVariables(httpRequest);
        
        // 認証情報の詳細ログ
        logAuthenticationDetails(auth);
        
        // アクセス制御マトリックスの評価
        evaluateAccessControlMatrix(httpRequest, auth);
        
        // カスタムアクセス制御サービスのシミュレーション
        simulateAccessControlService(httpRequest, auth);
        
        // レスポンスを監視するためのラッパー
        ResponseStatusCaptureWrapper responseWrapper = new ResponseStatusCaptureWrapper(httpResponse);
        
        try {
            chain.doFilter(request, responseWrapper);
            
            // レスポンスのステータスを記録
            int status = responseWrapper.getStatus();
            logger.debug("レスポンスステータス: {}", status);
            
            if (status == 403) {
                logger.debug("アクセス拒否が発生しました。上記のアクセス制御評価を確認してください。");
            }
        } finally {
            logger.debug("========== アクセス制御デバッグ情報 終了 ==========");
        }
    }
    
    private void logRequestParameters(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();
        if (!paramMap.isEmpty()) {
            logger.debug("リクエストパラメータ:");
            paramMap.forEach((key, values) -> 
                logger.debug("  {} = {}", key, String.join(", ", values))
            );
        }
    }
    
    private void logPathVariables(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables = 
            (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        
        if (pathVariables != null && !pathVariables.isEmpty()) {
            logger.debug("パス変数:");
            pathVariables.forEach((key, value) -> 
                logger.debug("  {} = {}", key, value)
            );
            
            // ユーザーID関連のパス変数がある場合、その詳細情報を表示
            if (pathVariables.containsKey("id")) {
                String userId = pathVariables.get("id");
                logger.debug("ユーザーID '{}' に関連するアクセス制御チェック:", userId);
                try {
                    boolean canEdit = accessControlService.canEditUser(Long.valueOf(userId)).isGranted();
                    boolean canDelete = accessControlService.canDeleteUser(Long.valueOf(userId)).isGranted();
                    logger.debug("  - 編集権限: {}", canEdit);
                    logger.debug("  - 削除権限: {}", canDelete);
                } catch (NumberFormatException e) {
                    logger.debug("  - ユーザーIDの形式が無効です: {}", e.getMessage());
                } catch (RuntimeException e) {
                    logger.debug("  - アクセス制御チェック中に予期しないエラー: {}", e.getMessage());
                }
            }
        }
    }
    
    private void logAuthenticationDetails(Authentication auth) {
        if (auth == null) {
            logger.debug("認証情報: 未認証");
            return;
        }
        
        logger.debug("認証情報: {}", auth.getName());
        logger.debug("認証済み: {}", auth.isAuthenticated());
        
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && !authorities.isEmpty()) {
            logger.debug("保有権限リスト:");
            authorities.forEach(authority -> 
                logger.debug("  - {}", authority.getAuthority())
            );
        } else {
            logger.debug("保有権限: なし");
        }
        
        // 認証の詳細情報
        Object details = auth.getDetails();
        if (details != null) {
            logger.debug("認証詳細: {}", details);
        }
    }
    
    private void evaluateAccessControlMatrix(HttpServletRequest request, Authentication auth) {
        logger.debug("アクセス制御マトリックス評価:");
        
        if (auth == null || !auth.isAuthenticated()) {
            logger.debug("  未認証ユーザー - ほとんどのリソースにアクセスできません");
            return;
        }
        
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority authority : authorities) {
            roles.add(authority.getAuthority());
        }
        
        // 保護されたパスに対するアクセス評価
        securedPaths.forEach(matcher -> {
            boolean matches = matcher.matches(request);
            if (matches) {
                logger.debug("  パス '{}' のアクセス評価:", matcher.toString());
                
                // 各URLパターンに対するロールベースのアクセス制御を評価
                if (matcher.toString().contains("/users/new")) {
                    boolean hasAccess = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_MODERATOR");
                    logger.debug("    - 新規ユーザー作成権限: {} (ADMIN/MODERATORが必要)", hasAccess ? "あり" : "なし");
                }
                else if (matcher.toString().contains("/users/{id}/edit")) {
                    // パス変数からIDを取得してcanEditUserをチェック
                    @SuppressWarnings("unchecked")
                    Map<String, String> pathVars = 
                        (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                    
                    if (pathVars != null && pathVars.containsKey("id")) {
                        try {
                            Long userId = Long.valueOf(pathVars.get("id"));
                            boolean canEdit = accessControlService.canEditUser(userId).isGranted();
                            logger.debug("    - ユーザー編集権限: {} (ID: {})", canEdit ? "あり" : "なし", userId);
                        } catch (NumberFormatException e) {
                            logger.debug("    - ユーザーID変換エラー: {}", e.getMessage());
                        } catch (IllegalArgumentException e) {
                            logger.debug("    - ユーザー編集権限チェックエラー: {}", e.getMessage());
                        } catch (RuntimeException e) {
                            logger.debug("    - 実行時エラー: {}", e.getMessage());
                        }
                    }
                }
                else if (matcher.toString().contains("/users/search")) {
                    String roleParam = request.getParameter("role");
                    if (roleParam != null) {
                        boolean canView = accessControlService.canViewUsersByRole(roleParam);
                        logger.debug("    - 指定ロール('{}')のユーザー検索権限: {}", roleParam, canView ? "あり" : "なし");
                    }
                }
                else if (matcher.toString().equals("/users")) {
                    boolean hasAccess = roles.stream().anyMatch(r -> 
                        r.equals("ROLE_ADMIN") || r.equals("ROLE_MODERATOR") || r.equals("ROLE_USER"));
                    logger.debug("    - ユーザー一覧閲覧権限: {} (いずれかのロールが必要)", hasAccess ? "あり" : "なし");
                }
            }
        });
    }
    
    private void simulateAccessControlService(HttpServletRequest request, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        
        logger.debug("アクセス制御サービスシミュレーション:");
        
        // URLから特定のパターンを検出してアクセス制御サービスの呼び出しをシミュレート
        String uri = request.getRequestURI();
        
        // /users/search?role= のパターン
        if (uri.equals("/users/search")) {
            String role = request.getParameter("role");
            if (role != null) {
                boolean canView = accessControlService.canViewUsersByRole(role);
                logger.debug("  accessControlService.canViewUsersByRole('{}') = {}", role, canView);
            }
        }
        
        // /users/{id}/edit または /users/{id}/delete のパターン
        @SuppressWarnings("unchecked")
        Map<String, String> pathVars = 
            (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        
        if (pathVars != null && pathVars.containsKey("id")) {
            try {
                Long userId = Long.valueOf(pathVars.get("id"));
                
                if (uri.contains("/edit")) {
                    boolean canEdit = accessControlService.canEditUser(userId).isGranted();
                    logger.debug("  accessControlService.canEditUser({}) = {}", userId, canEdit);
                }
                
                if (uri.contains("/delete")) {
                    boolean canDelete = accessControlService.canDeleteUser(userId).isGranted();
                    logger.debug("  accessControlService.canDeleteUser({}) = {}", userId, canDelete);
                }
            } catch (NumberFormatException e) {
                logger.debug("  ユーザーID変換エラー: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void init(jakarta.servlet.FilterConfig filterConfig) throws ServletException {
        // 初期化処理は不要
    }
    
    @Override
    public void destroy() {
        // 破棄処理は不要
    }
    
    /**
     * レスポンスステータスを捕捉するためのラッパークラス
     */
    private static class ResponseStatusCaptureWrapper extends HttpServletResponseWrapper {
        private int status = 200;
        
        public ResponseStatusCaptureWrapper(HttpServletResponse response) {
            super(response);
        }
        
        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }
        
        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }
        
        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }
        
        @Override
        public int getStatus() {
            return this.status;
        }
    }
}