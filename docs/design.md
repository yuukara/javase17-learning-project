# 設計書

## 1. プロジェクト概要

このプロジェクトは、Java 17とSpring Bootを使用して構築された、ユーザー管理アプリケーションです。Java SE 17の新機能を活用しながら、ユーザーのCRUD操作（作成、読み取り、更新、削除）をAPI経由で提供し、Thymeleafテンプレートを使用してユーザー一覧を表示します。Spring Securityを使用して認証・認可機能を実装しています。

## 2. 機能

*   ユーザー一覧の表示
*   ユーザーの追加
*   ユーザーの詳細表示
*   ユーザーの編集
*   ユーザーの削除
*   **ユーザー役割管理**
    *   ユーザーに対して役割を設定・変更
    *   役割に応じた操作の可否をチェック
*   **ユーザー検索**
    *   ユーザー名、メールアドレス、役割などの条件でユーザーを検索できる。
    *   検索条件は複数指定できる。
    *   検索結果は、一覧画面に表示する。
    *   検索結果がない場合は、その旨を表示する。

## 3. クラス構成

*   `Javase17learningprojectApplication`: アプリケーションのエントリーポイント。
*   `User`: ユーザーエンティティ。`UserDetails`インターフェースを実装。
    *   `id`: ユーザーID (Long)
    *   `name`: ユーザー名 (String)
        * バリデーション: 2〜100文字、必須
    *   `email`: メールアドレス (String)
        * バリデーション: メールアドレス形式、必須
    *   `password`: パスワード (String)
        * バリデーション: 8文字以上、必須
        * BCryptPasswordEncoderでハッシュ化して保存
    *   `roles`: ユーザーの役割セット (Set<Role>)
        * バリデーション: 必須
    *   `createdAt`: 作成日時 (LocalDateTime)
    *   `updatedAt`: 更新日時 (LocalDateTime)
    *   Spring Security関連フィールド:
        * `accountNonExpired`: アカウントの有効期限
        * `accountNonLocked`: アカウントのロック状態
        * `credentialsNonExpired`: 認証情報の有効期限
        * `enabled`: アカウントの有効状態
*   `Role`: 役割エンティティ。
    *   `id`: 役割ID (Long)
    *   `name`: 役割名 (String) (`USER`, `ADMIN`, `MODERATOR`)
        * unique制約
*   `UserRepository`: ユーザーリポジトリ。`JpaRepository<User, Long>`を継承。
    *   `findByRoles`: 指定された役割を持つユーザーを検索
    *   `findByNameContaining`: 指定された名前を含むユーザーを検索
    *   `findByEmail`: 指定されたメールアドレスを持つユーザーを検索
    *   `searchUsers`: ユーザー名、メールアドレス、役割で検索（nullの条件は無視）
*   `RoleRepository`: 役割リポジトリ。`JpaRepository<Role, Long>`を継承。
    *   `findByName`: 指定された名前の役割を検索
*   `UserController`: ユーザーコントローラー。APIエンドポイントを提供。
    *   `GET /users`: 全てのユーザーを取得
    *   `GET /users/{id}`: 指定されたIDのユーザーを取得
    *   `GET /users/{id}/edit`: ユーザー編集画面を表示
    *   `POST /users`: 新しいユーザーを作成
    *   `POST /users/{id}`: 指定されたIDのユーザーを更新（役割の変更を含む）
    *   `GET /users/{id}/delete`: ユーザー削除確認画面を表示
    *   `POST /users/{id}/delete`: 指定されたIDのユーザーを削除
    *   `GET /users/new`: 新規ユーザー作成画面を表示
    *   `GET /users/search`: ユーザーを検索

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
*   セッション管理を実装する
*   CSRF対策を実装する
*   適切なエラーハンドリングを実装する

### 8.2. 監査要件

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

現在テストが失敗している箇所があります。以下に主な課題と対応計画を示します：

### 11.1. テスト失敗の主な原因

* **エンティティとリポジトリの不整合**: UserエンティティとUserRepositoryの間にインターフェース定義と実装の不一致があります。
* **セキュリティ設定の不完全**: Spring Securityの設定が一部未実装または不適切な設定があります。
* **検索機能の実装不足**: ユーザー検索機能で設計とコードの間に乖離があります。

### 11.2. テスト計画

* **単体テスト**: すべてのエンティティ、リポジトリ、サービスに対する単体テストを実装予定
* **統合テスト**: コントローラーとリポジトリ、サービスを統合したテストを実装予定
* **エンドツーエンドテスト**: ユーザーインターフェースからデータベースまでの一連の処理をテスト予定

### 11.3. 次の対応

1. 設計書と実装コードを同期させる（特にUserエンティティとUserRepository）
2. テストコードを修正し、テストが成功することを確認
3. 不足している機能（監査ログなど）を順次実装
4. テストカバレッジを80%以上にする

当面は現状の実装をGitに登録し、履歴を残した上で改善を進めていく予定です。

`