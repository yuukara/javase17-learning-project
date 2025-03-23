# 実装チェックリスト

このドキュメントは、設計書に記載された機能の実装状況を追跡し、解決すべき不整合を特定するためのものです。

## 現状の課題

### 設計との不整合
- UserRepositoryの`findByRoles`メソッドがString roleNameを受け取る形で実装されており、設計書で指定されたRole roleパラメータとの不一致がある
- 検索機能で`UserSearchCriteria`レコードを使用せず、String型パラメータを使用している

### コード品質の改善点
- パスワードエンコーディングがUserエンティティのsetPasswordメソッド内で行われており、責任の分離の観点から再考の余地あり
- instanceofの使用が従来型であり、Java 17のパターンマッチングを活用できる
- テキストブロックを使用できる箇所（特にSQLクエリ）での活用検討

### セキュリティ上の注意点
- H2コンソール用の設定（CSRF無効化、frame-options）の本番環境での取り扱い検討
- SecurityConfigTestのテストカバレッジの確認
- UserControllerTestでの認証・認可テストが失敗（403エラー）
- AccessControlServiceTestでUserRepositoryの依存性注入が正しく機能していない

### テスト実装の課題
- UserRepositoryTestでロールテーブルのプライマリキー違反が発生
- AccessControlServiceTestで複数のテストケースがNullPointerExceptionで失敗
- テストにおける依存性注入の問題を解決する必要あり

これらの課題のうち、特にテスト関連の問題は優先的に対応が必要です。
=======

## 1. ユーザーエンティティの実装

### 対応すべき課題:
- [x] `User`エンティティがSpring SecurityのUserDetailsインターフェースを実装していることを確認
- [x] すべてのフィールドが設計書と一致していることを確認:
  - [x] `id`: Long型
  - [x] `name`: バリデーション付きString（2～100文字、必須）
  - [x] `email`: メールバリデーション付きString（必須）
  - [x] `password`: バリデーション付きString（8文字以上、必須）
  - [x] `roles`: Set<Role>型
  - [x] `createdAt`: LocalDateTime型
  - [x] `updatedAt`: LocalDateTime型
  - [x] Spring Securityのフィールド:
    - [x] `accountNonExpired`: boolean型
    - [x] `accountNonLocked`: boolean型
    - [x] `credentialsNonExpired`: boolean型
    - [x] `enabled`: boolean型
- [x] 必須のUserDetailsメソッドが実装されていることを確認:
  - [x] `getAuthorities()`
  - [x] `getUsername()`
  - [x] `isAccountNonExpired()`
  - [x] `isAccountNonLocked()`
  - [x] `isCredentialsNonExpired()`
  - [x] `isEnabled()`

### 実施すべき作業:
1. ✓ 既存のUserエンティティと設計要件の比較
2. ✓ 未実装の場合は`UserDetails`インターフェースの実装
3. ✓ 不足フィールドとメソッドの追加
4. ✓ バリデーションアノテーションの確認と追加

## 2. リポジトリメソッドの実装

### 対応すべき課題:
- [x] `UserRepository`に設計書の全メソッドが含まれていることを確認:
  - [x] `findByRoles(Role role)` (String roleNameパラメータで実装)
  - [x] `findByNameContaining(String name)`
  - [x] `findByEmail(String email)`
  - [x] `searchUsers(String name, String email, String role)`
- [x] `searchUsers`メソッドのJPQLクエリ実装を確認
- [x] `RoleRepository`に`findByName(String name)`メソッドが含まれていることを確認

### 実施すべき作業:
1. ✓ 既存のリポジトリインターフェースの確認
2. ✓ 不足メソッドの追加
3. ✓ 適切なJPQLクエリの実装
4. △ `UserSearchCriteria`レコードの使用検討（現在はString型パラメータを使用）

## 3. Spring Securityの設定

### 対応すべき課題:
- [x] セキュリティ設定の存在確認
- [x] パスワードエンコーダー設定（BCrypt）の確認
- [x] ロールベースの認可設定の確認
- [x] 認証済みユーザー詳細のコントローラー利用可能性確認
- [x] CSRF保護の確認
- [x] セッション管理の確認

### 実施すべき作業:
1. ✓ `SecurityConfig`クラスの作成または更新
2. ✓ 適切なパスワードエンコーディングの実装
3. ✓ URL単位のセキュリティルール設定
4. ✓ 適切なユーザー詳細サービスの設定
5. ✓ 認証・認可機能の動作確認

## 4. 監査ログの実装

### 対応すべき課題:
- [ ] `AuditLog`レコードの存在確認 → 未実装
- [ ] `AuditEvent`シールドクラスの実装確認 → 未実装
- [ ] audit_logsテーブルのデータベーススキーマ確認 → 未実装
- [ ] 以下の操作に対する監査ログ機能の実装確認:
  - [ ] ユーザー作成 → 未実装
  - [ ] ロール変更 → 未実装
  - [ ] ユーザー削除 → 未実装
  - [ ] その他の重要な操作 → 未実装

### 実施すべき作業:
1. `AuditLog`レコードの作成
2. `AuditEvent`シールドクラス階層の実装
3. audit_logsテーブルのスキーマ定義
4. 重要操作へのサービスメソッドにおける監査ログ追加
5. 横断的監査処理のアスペクト使用検討

## 5. Java 17機能の使用

### 対応すべき課題:
- [ ] レコードの使用確認:
  - [ ] `UserSearchCriteria` → 未実装
  - [ ] `UserDto` → 未実装
  - [ ] `AuditLog` → 未実装
- [ ] instanceofパターンマッチングの使用確認 → 従来の方式で実装済み
- [ ] シールドクラスの確認:
  - [ ] `UserRole`階層 → 未実装
  - [ ] `AuditEvent`階層 → 未実装
- [ ] SQLクエリやテンプレートでのテキストブロック使用確認 → 未使用
- [ ] ロールベースのロジックでのswitch式使用確認 → 未使用

### 実施すべき作業:
1. `UserSearchCriteria`、`UserDto`、`AuditLog`のレコード実装
2. instanceofパターンマッチングへのリファクタリング
3. `UserRole`と`AuditEvent`のシールドクラス階層作成
4. SQLクエリでのテキストブロック使用
5. 適切な箇所でのswitch式の使用

## 6. 全般的な実装状況

### ユーザー管理機能:
- [ ] ユーザー一覧 → 実装要確認
- [ ] ユーザー作成 → 実装要確認
- [ ] ユーザー詳細表示 → 実装要確認
- [ ] ユーザー編集 → 実装要確認
- [ ] ユーザー削除 → 実装要確認
- [ ] ユーザーロール管理 → 実装要確認
- [ ] ユーザー検索機能 → 実装要確認

### UI実装:
- [ ] `users.html` → 実装要確認
- [ ] `user_detail.html` → 実装要確認
- [ ] `user_edit.html` → 実装要確認
- [ ] `user_create.html` → 実装要確認
- [ ] `user_delete.html` → 実装要確認

### テスト実装:
- [x] エンティティテスト ※UserTestは全テストケース（12件）成功
- [△] リポジトリテスト ※UserRepositoryTestでロールテーブルのプライマリキー違反発生
- [△] サービステスト ※AccessControlServiceTestで依存性注入の問題あり
- [△] コントローラーテスト ※UserControllerTestで認証・認可関連の失敗（403エラー）あり
- [ ] 統合テスト
- [△] セキュリティテスト ※SecurityConfigTestの一部テストケースが失敗

## 次のステップ

1. 各チェックリスト項目の確認と現状把握
2. 未実装項目と不整合の優先順位付け
3. 未実装機能ごとのチケットまたはタスク作成
4. 優先順位に基づく未実装機能の実装
5. 実装に合わせたテストの更新
6. コードと設計文書の整合性確認

---

この実装チェックリストは、実装の進行に伴って更新されます。
