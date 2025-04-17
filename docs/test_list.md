# テスト実行コマンドリスト

## 概要
このドキュメントには、各テストクラスを個別に実行するためのMavenコマンドを記載しています。

## 既知の問題点

### 1. リポジトリテストの失敗
現在、以下のテストが失敗しています：
1. AuditLogRepositoryTest
2. RoleRepositoryTest
3. UserRepositoryTest

主な原因：
- AuditLogクラスがJPAエンティティとして正しく設定されていない
- エラーメッセージ：`Not a managed type: class com.example.javase17learningproject.model.AuditLog`

推奨される修正手順：
1. AuditLogクラスに@Entityアノテーションを追加
2. 必要なJPA関連のアノテーションを確認（@Id、@Column等）
3. AuditLogクラスの修正後、各リポジトリテストを再実行

### 2. ✅ パスワード暗号化テストの修正完了
UserTestの`testPasswordEncryption`メソッドが成功しました：
- 修正内容：Userクラスの`setPassword`メソッドにBCryptPasswordEncoderによる暗号化を実装
- 修正結果：全12テストが成功
- 対応済の問題：~~パスワードの暗号化処理が実装されていないか、正しく動作していない~~

### 3. ✅ 監査ログ機能の基本実装（Phase 1）完了
インメモリでの監査ログ機能の実装が完了しました：
- AuditLogInMemoryStorageImplTest：全5テストが成功
- AuditAspectTest：全3テストが成功

### 4. ✅ Java 23環境でのMockitoテスト実行
Java 23環境でMockitoのテストを実行する場合、ByteBuddy実験的機能の有効化が必要です：

対応方法：
1. 環境変数による設定：
```powershell
# PowerShell
$env:MAVEN_OPTS="-Dnet.bytebuddy.experimental=true"; mvn test

# Bash
export MAVEN_OPTS="-Dnet.bytebuddy.experimental=true" && mvn test
```

2. Maven Surefire Plugin設定による対応：
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Dnet.bytebuddy.experimental=true</argLine>
    </configuration>
</plugin>
```

## テストクラス実行コマンド

### 1. モデルテスト

#### 1.1. UserTest（ユーザーモデル）
```bash
mvn -Dtest=UserTest test
```

主なテストケース：
- testPasswordEncryption（パスワード暗号化）✅ 修正済み・成功
- その他11個のテストケース（全て成功）

#### 1.2. AuditLogTest（監査ログモデル）
```bash
mvn -Dtest=AuditLogTest test
```

利用可能なテストメソッド：
- testCreateAuditLog（監査ログの作成）
- testCreateAuditLogWithNullEventType（イベントタイプnullの場合）
- testCreateAuditLogWithBlankEventType（イベントタイプが空の場合）
- testCreateAuditLogWithNullSeverity（重要度nullの場合）
- testCreateAuditLogWithNullCreatedAt（作成日時nullの場合）
- testFactoryMethodWithEvent（イベントを使用したファクトリメソッド）
- testFactoryMethodWithEventType（イベントタイプを使用したファクトリメソッド）

### 2. リポジトリテスト

#### 2.1. RoleRepositoryTest（ロールリポジトリ）
```bash
mvn -Dtest=RoleRepositoryTest test
```

利用可能なテストメソッド：
- testSaveRole（ロールの保存）
- testFindByName（名前による検索）
- testFindByNameNotFound（存在しない名前での検索）
- testFindAll（全件取得）
- testDeleteRole（ロールの削除）

#### 2.2. UserRepositoryTest（ユーザーリポジトリ）
```bash
mvn -Dtest=UserRepositoryTest test
```

利用可能なテストメソッド：
- testSaveUser（ユーザーの保存）
- testFindByEmail（メールアドレスによる検索）
- testFindByNameContaining（名前による部分一致検索）
- testFindByRoles（ロールによる検索）
- testSearchUsers（複合条件による検索）

#### 2.3. AuditLogInMemoryStorageTest（監査ログインメモリストレージ）✅
```bash
mvn -Dtest=AuditLogInMemoryStorageImplTest test
```

利用可能なテストメソッド：
- testSaveAndFindById（保存と取得）
- testFindLatestLogs（最新ログの取得）
- testFindByEventType（イベントタイプによる検索）
- testFindAll（全件取得）
- testConcurrentAccess（並行アクセス）

### 3. アスペクトテスト

#### 3.1. AuditAspectTest（監査ログアスペクト）✅
```bash
mvn -Dtest=AuditAspectTest test
```

利用可能なテストメソッド：
- testAuditedMethodExecution（基本的な監査ログ記録）
- testAuditedMethodWithCustomDescription（カスタム説明付きの監査ログ）
- testAuditedMethodWithHighSeverity（重要度HIGH指定の監査ログ）

### 4. メトリクステスト

#### 4.1. AuditLogMetricsTest（監査ログメトリクス）✅
```bash
mvn -Dtest=AuditLogMetricsTest test
```

利用可能なテストメソッド：
- testDailyArchiveSuccessCount（日次アーカイブ成功カウント）
- testDailyArchiveFailureCount（日次アーカイブ失敗カウント）
- testMonthlyArchiveSuccessCount（月次アーカイブ成功カウント）
- testMonthlyArchiveFailureCount（月次アーカイブ失敗カウント）
- testDailyArchiveDuration（日次アーカイブ処理時間）
- testMonthlyArchiveDuration（月次アーカイブ処理時間）
- testCombinedMetrics（複合メトリクス）

#### 4.2. AuditLogSchedulerServiceTest（スケジューラーサービス）✅
```bash
mvn -Dtest=AuditLogSchedulerServiceTest test
```

利用可能なテストメソッド：
- testScheduleDailyArchive（日次アーカイブのスケジュール）
- testScheduleMonthlyArchive（月次アーカイブのスケジュール）
- testScheduleCleanup（クリーンアップのスケジュール）
- testHandleDailyArchiveSuccess（日次アーカイブ成功時の処理）
- testHandleMonthlyArchiveSuccess（月次アーカイブ成功時の処理）
- testHandleCleanupSuccess（クリーンアップ成功時の処理）
- testHandleDailyArchiveFailure（日次アーカイブ失敗時の処理）

複数のテストを同時に実行する場合：
```bash
mvn test -Dtest="AuditLogMetricsTest,AuditLogSchedulerServiceTest"
```

## 注意事項
- 特定のテストメソッドのみを実行する場合は、以下のように`#メソッド名`を追加してください：
  ```bash
  mvn -Dtest=テストクラス名#テストメソッド名 test
  ```
  例：
  ```bash
  mvn -Dtest=UserControllerTest#testCreateUser test
  ```

- テストの実行は、プロジェクトのルートディレクトリで行ってください。

- テスト実行時にエラーが発生する場合は、実装の問題である可能性があります。
  その場合は実装を確認してください。

- Java 23環境でテストを実行する場合は、ByteBuddy実験的機能の有効化が必要です。
  上記の「Java 23環境でのMockitoテスト実行」セクションを参照してください。