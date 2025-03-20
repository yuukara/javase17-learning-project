# 設計書

## 1. プロジェクト概要

このプロジェクトは、Spring Bootを使用して構築された、ユーザー管理アプリケーションです。ユーザーのCRUD操作（作成、読み取り、更新、削除）をAPI経由で提供し、Thymeleafテンプレートを使用してユーザー一覧を表示します。

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
*   `User`: ユーザーエンティティ。
    *   `id`: ユーザーID (Long)
    *   `name`: ユーザー名 (String)
        * バリデーション: 2〜100文字、必須
    *   `email`: メールアドレス (String)
        * バリデーション: メールアドレス形式、必須
    *   `password`: パスワード (String)
        * バリデーション: 8文字以上、必須
    *   `role`: ユーザーの役割 (Role)
        * バリデーション: 必須
    *   `createdAt`: 作成日時 (LocalDateTime)
    *   `updatedAt`: 更新日時 (LocalDateTime)
*   `Role`: 役割エンティティ。
    *   `id`: 役割ID (Long)
    *   `name`: 役割名 (String) (`USER`, `ADMIN`, `MODERATOR`)
        * unique制約
*   `UserRepository`: ユーザーリポジトリ。JPAによるデータベース操作を提供。
*   `RoleRepository`: 役割リポジトリ。JPAによるデータベース操作を提供。
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
*   `RoleService`: 役割管理サービス。役割の作成、取得、更新、削除を提供。
*   `AccessControlService`: アクセス制御サービス。役割に基づいて操作の可否をチェック。

## 4. 技術スタック

*   Java 17
*   Spring Boot
*   Spring Data JPA
*   H2 Database (インメモリ)
*   Thymeleaf
*   Maven
*   Lombok (ボイラープレートコード削減)
*   Spring Boot Validation (入力値検証)

## 5. データベース

*   H2 Database (インメモリ)
*   テーブル名: `users`
    *   `id`: BIGINT (主キー、自動生成)
    *   `name`: VARCHAR(100) NOT NULL
    *   `email`: VARCHAR(255) NOT NULL UNIQUE
    *   `password`: VARCHAR(255) NOT NULL
    *   `role_id`: BIGINT NOT NULL (外部キー、`roles`テーブルを参照)
    *   `created_at`: TIMESTAMP NOT NULL
    *   `updated_at`: TIMESTAMP NOT NULL
    *   インデックス:
        * `idx_user_name` (name) - 検索性能向上用
        * `idx_user_email` (email) - 一意性確保用
        * `idx_user_role` (role_id) - 結合性能向上用
*   テーブル名: `roles`
    *   `id`: BIGINT (主キー、自動生成)
    *   `name`: VARCHAR(50) NOT NULL UNIQUE
    *   インデックス:
        * `idx_role_name` (name) - 一意性確保用

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
    *   Thymeleafテンプレートを使用して検索フォームと検索結果を表示する。
*   **機能**
    *   UserController:
        *   `/users/search`エンドポイントで検索リクエストを受け付ける。
        *   検索条件をUserRepositoryに渡し、検索結果を受け取る。
        *   検索結果をThymeleafテンプレートに渡し、表示する。
    *   UserRepository:
        *   JPQLクエリを使用して検索条件に合致するユーザーを取得する。
        *   nullの検索条件は無視し、指定された条件のみで検索を行う。
*   **クラス構成**
    *   UserController:
        *   `searchUsers(@RequestParam(required = false) String name, @RequestParam(required = false) String email, @RequestParam(required = false) String role, Model model)`
    *   UserRepository:
        *   `@Query`で実装された`List<User> searchUsers(String name, String email, String role)`メソッドで、検索条件（ユーザー名、メールアドレス、役割）に部分一致するユーザーを取得する。

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
