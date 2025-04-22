# テストクラスの比較分析

## AuditLogServiceTestとAuditLogArchiveServiceTestの違い

### 1. テスト対象の責務
#### AuditLogServiceTest
- 監査ログの基本的なCRUD操作のテスト
- キャッシュ管理機能のテスト
- ログの集計機能のテスト
- インメモリキャッシュと永続化層の連携テスト

#### AuditLogArchiveServiceTest
- 日次アーカイブファイルの作成・検証テスト
- 月次アーカイブファイルの作成テスト
- アーカイブの検索機能テスト
- 古いアーカイブの削除テスト
- アーカイブの統計情報取得テスト

### 2. 依存コンポーネント
#### AuditLogServiceTest
- `AuditLogRepository`：データベース操作
- `AuditLogInMemoryStorage`：インメモリキャッシュ操作

#### AuditLogArchiveServiceTest
- `AuditLogRepository`：データベース操作
- `AuditLogArchiveStorageImpl`：アーカイブストレージ操作
- `@TempDir`：一時ディレクトリ（ファイルシステム操作のテスト用）

### 3. テストケースの特徴
#### AuditLogServiceTest
- キャッシュとデータベース間の整合性確認
- ログデータの検索・集計機能の正確性検証
- メモリキャッシュの更新・移行処理の検証
- 監査ログの即時性と永続化の両立を確認

#### AuditLogArchiveServiceTest
- アーカイブファイルの作成と圧縮の検証
- アーカイブデータの整合性チェック
- 長期保存データの検索機能の検証
- アーカイブの保持期間管理の検証
- アーカイブに関する統計情報の正確性確認

### 4. ファイルシステムの関与
#### AuditLogServiceTest
- ファイルシステムを直接使用しない
- すべてのデータはメモリまたはデータベースで管理

#### AuditLogArchiveServiceTest
- `@TempDir`を使用して実際のファイルシステム操作をテスト
- アーカイブファイルの物理的な作成・削除を検証
- ファイルシステムを使用した長期保存機能の検証

### 5. 設計上の意図
このような分離には以下の利点があります：

1. **責務の明確な分離**
   - AuditLogService: リアルタイムのログ処理と短期保存
   - AuditLogArchiveService: 長期保存とアーカイブ管理

2. **パフォーマンスの最適化**
   - リアルタイム処理（AuditLogService）とバッチ処理（AuditLogArchiveService）の分離
   - それぞれに適した保存方式の採用

3. **テストの効率化**
   - 各コンポーネントの責務に焦点を当てたテストケース
   - テストの依存関係の明確化

4. **保守性の向上**
   - 機能変更の影響範囲を局所化
   - 各コンポーネントの独立した進化が可能