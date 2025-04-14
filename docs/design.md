### 11.2. 監査ログ機能の実装方針

1. **2段階での実装**:
   * Phase 1: recordを使用したインメモリでの監査ログ（Java 17機能学習） ← 完了
     - [x] AuditLogレコードクラス: 実装完了
       * 不変性の確保
       * イベントタイプと重要度の定義
       * コンパクトコンストラクタによるバリデーション
     - [x] インメモリでのログ保持: ConcurrentHashMapで実装完了
       * スレッドセーフな操作
       * AtomicLongによるID生成
       * 最新ログ・イベントタイプによる検索
     - [x] AOPによる自動ログ記録: 実装完了
       * @Auditedアノテーション
       * AuditAspectによる処理
       * Spring Securityとの統合
     - [x] テスト: 実装完了
       * インメモリストレージ（5テストケース）
       * アスペクト（3テストケース）
       * 並行アクセス
   * Phase 2: データベースへの永続化（将来の拡張）
     - [ ] エンティティ設計
     - [ ] JPA実装
     - [ ] 変換層実装

2. **実装の詳細**:
   * インメモリストレージの特徴:
     - ConcurrentHashMapによる高速アクセス
     - スレッドセーフな実装
     - メモリ使用量の制御（今後の課題）
   * AOPの特徴:
     - アノテーションベースの設定
     - メソッドレベルの監査
     - 柔軟なイベントタイプ定義

3. **今後の課題**:
   * Phase 1の改善:
     - キャッシュサイズの制限実装
     - ログローテーション方式の確立
     - パフォーマンス最適化
   * Phase 2への準備:
     - データベーススキーマの詳細設計
     - 移行戦略の策定
    *   `canViewUsersByRole(String)`: 指定された役割のユーザー一覧を表示できるか判断
    *   `canCreateUserWithRole(String)`: 指定された役割でユーザーを作成できるか判断
    *   アクセス制御ルール:
        * 管理者（ROLE_ADMIN）: 全ての操作が可能
        * 管理補助者（ROLE_MODERATOR）: 一般ユーザーの編集・削除・作成が可能
        * 一般ユーザー（ROLE_USER）: 自身の情報の編集のみ可能

*   `UserController`: ユーザーコントローラー。APIエンドポイントを提供。
    *   `GET /users`: 全てのユーザーを取得
    *   `GET /users/{id}`: 指定されたIDのユーザーを取得
    *   `GET /users/{id}/edit`: ユーザー編集画面を表示
    *   `POST /users`: 新しいユーザーを作成（一時パスワードを設定）
    *   `POST /users/{id}`: 指定されたIDのユーザーを更新（役割の変更を含む）
    *   `GET /users/{id}/delete`: ユーザー削除確認画面を表示
    *   `POST /users/{id}/delete`: 指定されたIDのユーザーを削除
    *   `GET /users/new`: 新規ユーザー作成画面を表示
    *   `GET /users/search`: ユーザーを検索（名前、メール、役割で検索可能）

## 4. 技術スタック

*   Java 17
*   Spring Boot
*   Spring Security (認証・認可)
*   Spring Data JPA
*   H2 Database (インメモリ)
*   Thymeleaf
*   Maven
*   Lombok (ボイラープレートコード削減)
*   Spring Boot Validation (入力値検証)

### 4.1. Spring Security

*   `UserDetails`インターフェースを実装した`User`エンティティ
*   パスワードのBCryptハッシュ化
*   ロール（ROLE_USER、ROLE_ADMIN、ROLE_MODERATOR）ベースの認可
*   アカウント状態管理（有効期限、ロック、認証情報有効期限）

### 4.2. Java SE 17の機能活用

*   **レコードクラス**
    *   `UserSearchCriteria`: ユーザー検索条件を表す不変のレコードクラス
    *   `UserDto`: API応答用のデータ転送オブジェクト
    *   `AuditLog`: 監査ログエントリを表す不変のレコード

*   **instanceof パターンマッチング**
    *   認証オブジェクトからユーザー情報を抽出する処理の簡略化
    *   例外処理での例外タイプの判定と情報抽出

*   **シールドクラス**
    *   `UserRole`: ユーザーロールを表すシールドクラス階層
    *   `AuditEvent`: 監査イベントタイプを限定するシールドクラス

*   **テキストブロック**
    *   複雑なSQLクエリの可読性向上
    *   Thymeleafテンプレート内のJavaScriptコードの記述

*   **switch式**
    *   ユーザーロールに基づく権限チェック
    *   HTTP応答ステータスコードの決定
    *   ログレベルの判定

## 5. データベース

*   H2 Database (インメモリ)
*   テーブル名: `users`
    *   `id`: BIGINT (主キー、自動生成)
    *   `name`: VARCHAR(100) NOT NULL
    *   `email`: VARCHAR(255) NOT NULL UNIQUE
    *   `password`: VARCHAR(255) NOT NULL
    *   `account_non_expired`: BOOLEAN NOT NULL DEFAULT TRUE
    *   `account_non_locked`: BOOLEAN NOT NULL DEFAULT TRUE
    *   `credentials_non_expired`: BOOLEAN NOT NULL DEFAULT TRUE
    *   `enabled`: BOOLEAN NOT NULL DEFAULT TRUE
    *   `created_at`: TIMESTAMP NOT NULL
    *   `updated_at`: TIMESTAMP NOT NULL
    *   インデックス:
        * `idx_user_name` (name) - 検索性能向上用
        * `idx_user_email` (email) - 一意性確保用
*   テーブル名: `roles`
    *   `id`: BIGINT (主キー、自動生成)
    *   `name`: VARCHAR(50) NOT NULL UNIQUE
    *   インデックス:
        * `idx_role_name` (name) - 一意性確保用
*   テーブル名: `user_roles` (多対多関連のための中間テーブル)
    *   `user_id`: BIGINT NOT NULL (外部キー、`users`テーブルを参照)
    *   `role_id`: BIGINT NOT NULL (外部キー、`roles`テーブルを参照)
    *   主キー: (`user_id`, `role_id`)
    *   インデックス:
        * `idx_user_roles_user` (user_id) - 結合性能向上用
        * `idx_user_roles_role` (role_id) - 結合性能向上用
*   テーブル名: `audit_logs` (監査ログ)
    *   `id`: BIGINT (主キー、自動生成)
    *   `event_type`: VARCHAR(50) NOT NULL
    *   `user_id`: BIGINT
    *   `target_id`: BIGINT
    *   `description`: TEXT
    *   `created_at`: TIMESTAMP NOT NULL
    *   インデックス:
        * `idx_audit_event_type` (event_type)
        * `idx_audit_user_id` (user_id)
        * `idx_audit_created_at` (created_at)

## 6. UI

*   `users.html`: ユーザー一覧画面
*   `user_detail.html`: ユーザー詳細画面
*   `user_edit.html`: ユーザー編集画面
*   `user_create.html`: 新規ユーザー作成画面
*   `user_delete.html`: ユーザー削除確認画面

## 7. ユーザー検索機能の詳細設計

### 7.1. 要件定義

*   ユーザーは、ユーザー名、メールアドレス、役割などの条件でユーザーを検索できること。
*   検索条件は複数指定できること。
*   検索結果は、一覧画面に表示すること。
*   検索結果がない場合は、その旨を表示すること。

### 7.2. 設計

*   **概略**
    *   ユーザー検索機能は、UserController、UserRepositoryで実装する。
    *   Java 17のレコードクラスを使用して検索条件を表現する。
    *   Thymeleafテンプレートを使用して検索フォームと検索結果を表示する。
*   **機能**
    *   UserController:
        *   `/users/search`エンドポイントで検索リクエストを受け付ける。
        *   検索条件をUserSearchCriteriaレコードにまとめる。
        *   検索条件をUserRepositoryに渡し、検索結果を受け取る。
        *   検索結果をThymeleafテンプレートに渡し、表示する。
    *   UserRepository:
        *   JPQLクエリを使用して検索条件に合致するユーザーを取得する。
        *   nullの検索条件は無視し、指定された条件のみで検索を行う。
*   **クラス構成**
    *   UserSearchCriteria (レコード):
        *   `String name`
        *   `String email`
        *   `String role`
    *   UserController:
        *   `searchUsers(UserSearchCriteria criteria, Model model)`
    *   UserRepository:
        *   `@Query`で実装された`List<User> searchUsers(UserSearchCriteria criteria)`メソッド

## 8. セキュリティと監査

### 8.1. セキュリティ要件

*   パスワードは必ずハッシュ化して保存する
    * BCryptPasswordEncoderを使用
    * 既にハッシュ化されているパスワードは再エンコードしない
*   セッション管理を実装する
    * Spring Securityのデフォルトセッション管理を使用
*   CSRF対策を実装する
    * Spring SecurityのCSRFトークンを使用
*   適切なエラーハンドリングを実装する
    * アクセス制御違反は適切にログ記録
    * ユーザーフレンドリーなエラーメッセージを表示

### 8.2. アクセス制御実装

*   AccessControlServiceによる詳細なアクセス制御
    * ユーザー編集権限
        - 管理者: 全てのユーザーを編集可能
        - 管理補助者: 一般ユーザーのみ編集可能
        - 一般ユーザー: 自身のみ編集可能
    * ユーザー削除権限
        - 管理者: 全てのユーザーを削除可能
        - 管理補助者: 一般ユーザーのみ削除可能
        - 一般ユーザー: 削除権限なし
    * ユーザー作成権限
        - 管理者: 全ての役割のユーザーを作成可能
        - 管理補助者: 一般ユーザーのみ作成可能
        - 一般ユーザー: 作成権限なし
    * ユーザー一覧表示権限
        - 管理者: 全ての役割のユーザーを表示可能
        - 管理補助者: 一般ユーザーのみ表示可能
        - 一般ユーザー: 一般ユーザーのみ表示可能

### 8.3. 監査要件

*   全てのエンティティに作成日時・更新日時を記録する
*   重要な操作（ユーザー作成、役割変更、削除）はログに記録する
*   監査ログには以下の情報を含める：
    *   操作日時
    *   操作者
    *   操作内容
    *   対象データ
*   AuditLogレコードクラスを使用して監査情報を表現
*   AuditEventシールドクラスで監査イベントタイプを定義

## 9. ロギング要件

### 9.1. ログレベル

*   ERROR: システムエラー、例外発生時
*   WARN: 業務エラー、警告が必要な状態
*   INFO: 主要な処理の開始・終了
*   DEBUG: 詳細なデバッグ情報

### 9.2. ログ出力項目

*   タイムスタンプ
*   ログレベル
*   スレッドID
*   クラス名
*   メソッド名
*   メッセージ
*   例外スタックトレース（該当する場合）

### 9.3. ログローテーション

*   日次でログファイルを切り替え
*   30日分のログを保持
*   圧縮保存で容量を節約

## 10. Java SE 17機能実装例

### 10.1. レコードクラスの実装例

```java
// ユーザー検索条件を表すレコード
public record UserSearchCriteria(String name, String email, String role) {
    // コンパクトなコンストラクタで値の検証が可能
    public UserSearchCriteria {
        // nameとemailがnull出ない場合はtrimする
        if (name != null) name = name.trim();
        if (email != null) email = email.trim();
    }
}

// 監査ログを表すレコード
public record AuditLog(
    Long id,
    String eventType,
    Long userId,
    Long targetId,
    String description,
    LocalDateTime createdAt
) {}
```

### 10.2. パターンマッチングの実装例

```java
// 認証情報からユーザー情報を取得するメソッド
public User getCurrentUser(Authentication authentication) {
    if (authentication == null) {
        return null;
    }
    
    // パターンマッチングを使用して型チェックと変数代入を同時に行う
    if (authentication.getPrincipal() instanceof User user) {
        return user;
    }
    
    return null;
}

// 例外処理でのパターンマッチング
public ResponseEntity<String> handleException(Exception ex) {
    return switch (ex) {
        case UserNotFoundException e -> ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("ユーザーが見つかりません: " + e.getMessage());
        case AccessDeniedException e -> ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body("アクセスが拒否されました: " + e.getMessage());
        default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("エラーが発生しました: " + ex.getMessage());
    };
}
```

### 10.3. シールドクラスの実装例

```java
// ユーザーロールを表すシールドクラス階層
public sealed interface UserRole permits AdminRole, ModeratorRole, UserRole {
    String getName();
    List<String> getPermissions();
}

public final class AdminRole implements UserRole {
    public String getName() { return "ADMIN"; }
    public List<String> getPermissions() { 
        return List.of("CREATE", "READ", "UPDATE", "DELETE"); 
    }
}

public final class ModeratorRole implements UserRole {
    public String getName() { return "MODERATOR"; }
    public List<String> getPermissions() { 
        return List.of("READ", "UPDATE"); 
    }
}

public final class UserRole implements UserRole {
    public String getName() { return "USER"; }
    public List<String> getPermissions() { 
        return List.of("READ"); 
    }
}
```

### 10.4. テキストブロックの実装例

```java
// 複雑なSQLクエリ
@Query("""
    SELECT u FROM User u
    LEFT JOIN u.roles r
    WHERE (:name IS NULL OR u.name LIKE %:name%)
    AND (:email IS NULL OR u.email = :email)
    AND (:role IS NULL OR r.name = :role)
    ORDER BY u.name ASC
    """)
List<User> searchUsers(@Param("name") String name, 
                       @Param("email") String email, 
                       @Param("role") String role);

// JavaScriptコード
String javaScript = """
    function confirmDelete(userId) {
        if (confirm('このユーザーを削除してもよろしいですか？')) {
            document.getElementById('deleteForm' + userId).submit();
        }
    }
    
    function resetSearchForm() {
        document.getElementById('name').value = '';
        document.getElementById('email').value = '';
        document.getElementById('role').value = '';
        document.getElementById('searchForm').submit();
    }
    """;
```

### 10.5. Switch式の実装例

```java
// ユーザーロールに基づく権限チェック
public boolean hasPermission(User user, String operation) {
    return switch (user.getHighestRole()) {
        case "ADMIN" -> true;
        case "MODERATOR" -> !operation.equals("DELETE");
        case "USER" -> operation.equals("READ");
        default -> false;
    };
}

// HTTPステータスコードの決定
public HttpStatus determineHttpStatus(OperationResult result) {
    return switch (result) {
        case SUCCESS -> HttpStatus.OK;
        case CREATED -> HttpStatus.CREATED;
        case NOT_FOUND -> HttpStatus.NOT_FOUND;
        case FORBIDDEN -> HttpStatus.FORBIDDEN;
        case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
}
```

## 11. テスト実装の現状と計画

以下にテストの実装状況と今後の計画を示します：

### 11.1. 実装済みのテスト

* **UserTest**: ユーザーエンティティの単体テスト
    * バリデーション（名前、メール、パスワード）
    * パスワードのハッシュ化
    * ロール設定と検証
    * equals/hashCode動作
    * toString（パスワード非表示の確認）

* **AccessControlServiceTest**: アクセス制御サービスのテスト
    * 各ロール（管理者、管理補助者、一般ユーザー）の権限チェック
    * ユーザー編集権限の検証
    * ユーザー削除権限の検証
    * ユーザー作成権限の検証
    * ユーザー一覧表示権限の検証

* **UserControllerTest**: コントローラーのテスト（4つのクラスに分割）
    * **UserControllerTest**: 基本的なCRUD操作の検証
        - ユーザー作成（testCreateUser）
        - ユーザー一覧表示（testListUsers）
        - ユーザー詳細表示（testShowUserDetail）
        - ユーザー更新（testUpdateUser）
        - ユーザー削除（testDeleteUser）
        - ユーザー編集画面表示（testEditUser）
    * **UserControllerValidationTest**: バリデーションの検証
        - 入力値の検証
        - 重複データのチェック
        - 無効な入力値の処理
    * **UserControllerSecurityTest**: セキュリティ機能の検証
        - 権限のないユーザーのアクセス制御
        - CSRF保護の確認
        - 認証・認可の検証
    * **UserControllerSearchTest**: 検索機能の検証
        - 名前による検索
        - メールアドレスによる検索
        - ロールによる検索
        - 複合条件による検索

* **SecurityConfigTest**: セキュリティ設定のテスト
    * 認証処理の検証
    * 認可処理の検証
    * CSRF保護の確認
    * セッション管理の検証

### 11.2. 監査ログ機能の実装方針

1. **2段階での実装**:
   * Phase 1: recordを使用したインメモリでの監査ログ（Java 17機能学習）
     - AuditLogレコードクラスの活用
     - インメモリでの一時的なログ保持
     - ファクトリメソッドパターンの実践
   * Phase 2: データベースへの永続化（将来の拡張）
     - 永続化用のエンティティクラスの新規作成
     - JPAリポジトリの実装
     - 変換層の追加

2. **現在の実装状況**:
   * AuditLogレコードクラス: 実装済み
   * インメモリでのログ管理: 実装中
   * データベース永続化: 未実装（Phase 2で対応予定）

3. **今後の課題**:
   * インメモリログのローテーション方針
   * データベース移行時の変換戦略
   * 過去ログの保持期間の設定

### 11.3. 改善計画

1. テストカバレッジの維持（現在85%以上を維持）
   - 新機能追加時の確実なテスト実装
   - エッジケースのカバレッジ向上

2. テストコードの品質向上
   - テストメソッドの命名規則の統一
   - テストデータセットアップの共通化
   - アサーションの強化

3. 統合テストの強化
   - E2Eテストシナリオの作成
   - 実環境に近いテストデータの使用
   - 性能測定の自動化

4. CI/CDパイプラインの改善
   - テスト実行の高速化
   - テストレポートの自動生成
   - コードカバレッジレポートの可視化

実装は段階的に進め、各段階でテストを追加・実行して品質を確保します。

## 12. 実装整合性チェックリスト

設計とコードの整合性を確保するため、以下の点について確認と対応が必要です：

### 12.1. User エンティティ実装の確認

- [ ] UserエンティティがUserDetailsを実装しているか確認する
- [ ] 設計書に記載された全てのフィールドが実装されているか確認する
- [ ] Spring Security関連のメソッド（getAuthorities()など）が正しく実装されているか確認する
- [ ] バリデーション（@NotNull、@Size、@Email等）が適切に設定されているか確認する

### 12.2. Repository 実装の確認

- [ ] UserRepositoryの定義が設計書と一致しているか確認する
- [ ] 特にsearchUsersメソッドの実装が完了しているか確認する
- [ ] JPQLクエリが正しく動作するか確認する
- [ ] ネイティブクエリを使用している場合、適切に定義されているか確認する

### 12.3. セキュリティ設定の確認

- [ ] Spring Securityの設定クラスが作成されているか確認する
- [ ] パスワードエンコーダー（BCrypt）が設定されているか確認する
- [ ] 認証プロバイダが適切に設定されているか確認する
- [ ] ユーザー詳細サービスが実装されているか確認する

### 12.4. 監査ログ機能の確認

- [ ] AuditLogレコードが実装されているか確認する
- [ ] AuditEventシールドクラスが実装されているか確認する
- [ ] 監査ログを保存するためのリポジトリが実装されているか確認する
- [ ] 重要な操作時に監査ログが記録される仕組みが実装されているか確認する

### 12.5. Java 17 機能の活用確認

- [ ] レコードクラスが適切に使用されているか確認する
- [ ] パターンマッチングが活用されているか確認する
- [ ] シールドクラスが実装されているか確認する
- [ ] テキストブロックがSQLクエリなどで使用されているか確認する
- [ ] switch式が条件分岐に活用されているか確認する

実装と設計の整合性を確認した後、必要に応じてコードを修正し、テストを更新することが重要です。
