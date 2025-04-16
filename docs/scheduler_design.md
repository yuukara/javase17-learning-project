# 監査ログスケジューラー 設計書

## 1. 概要

監査ログのアーカイブ処理を自動化するスケジューラー機能の設計書です。
定期的なアーカイブ作成、メンテナンス処理、エラー処理を管理します。

## 2. 要件定義

### 2.1. 機能要件

1. 定期実行
   - 日次アーカイブ作成（毎日午前1時）
   - 月次アーカイブ作成（毎月1日午前2時）
   - 古いアーカイブの削除（毎日午前3時）

2. リトライ処理
   - 最大リトライ回数: 3回
   - リトライ間隔: 指数バックオフ（1分、2分、4分）
   - エラー種別に応じたリトライ判断

3. エラー通知
   - メール通知
   - アプリケーションログへの記録
   - メトリクスへの記録

4. タスク管理
   - タスクの優先順位付け
   - 実行状況の監視
   - リソース使用量の制御

### 2.2. 非機能要件

1. パフォーマンス
   - 1回のアーカイブ処理は30分以内に完了
   - システムリソース使用率は50%未満

2. 信頼性
   - 処理の冪等性を確保
   - 中断時の状態復元
   - データ整合性の保証

3. 運用性
   - 実行スケジュールの動的変更
   - 手動実行の対応
   - 詳細なログ記録

## 3. アーキテクチャ設計

### 3.1. コンポーネント構成

```
SchedulerService
├── TaskManager
│   ├── TaskExecutor
│   └── TaskQueue
├── RetryManager
│   ├── RetryPolicy
│   └── BackoffStrategy
├── ErrorHandler
│   ├── NotificationService
│   └── ErrorLogger
└── MonitoringService
    ├── MetricsCollector
    └── HealthChecker
```

### 3.2. クラス設計

```java
@Service
public class AuditLogSchedulerService {
    private final TaskManager taskManager;
    private final RetryManager retryManager;
    private final ErrorHandler errorHandler;
    private final MonitoringService monitoringService;
    
    // スケジュールされたタスクの定義
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduleDailyArchive() { ... }
    
    @Scheduled(cron = "0 0 2 1 * ?")
    public void scheduleMonthlyArchive() { ... }
    
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduleCleanup() { ... }
}

public class TaskManager {
    private final PriorityQueue<ScheduledTask> taskQueue;
    private final TaskExecutor taskExecutor;
    
    public void scheduleTask(ScheduledTask task) { ... }
    public void executeNext() { ... }
}

public class RetryManager {
    private final RetryPolicy retryPolicy;
    private final BackoffStrategy backoffStrategy;
    
    public boolean shouldRetry(Exception e, int attempts) { ... }
    public Duration getNextDelay(int attempts) { ... }
}
```

### 3.3. データモデル

```java
public record ScheduledTask(
    String taskId,
    TaskType type,
    TaskPriority priority,
    LocalDateTime scheduledTime,
    Map<String, Object> parameters
) {}

public record TaskResult(
    String taskId,
    TaskStatus status,
    LocalDateTime completedTime,
    String errorMessage
) {}
```

## 4. 処理フロー

### 4.1. タスク実行フロー

1. スケジュール時刻になると該当のタスクを起動
2. タスクの前提条件をチェック
3. タスクを実行キューに登録
4. 実行とリトライ処理
5. 結果の記録と通知

### 4.2. エラー処理フロー

1. 例外の種別を判定
2. リトライ可能か判断
3. リトライ可能な場合はバックオフ後に再実行
4. リトライ不可または上限に達した場合は管理者に通知

### 4.3. 監視フロー

1. タスクの実行状況を監視
2. リソース使用量を監視
3. 異常を検知した場合は通知
4. メトリクスの収集と記録

## 5. 設定項目

```yaml
scheduler:
  # タスク設定
  tasks:
    daily-archive:
      cron: "0 0 1 * * ?"
      timeout: 1800  # 30分
      priority: HIGH
    monthly-archive:
      cron: "0 0 2 1 * ?"
      timeout: 3600  # 60分
      priority: HIGH
    cleanup:
      cron: "0 0 3 * * ?"
      timeout: 1800
      priority: MEDIUM

  # リトライ設定
  retry:
    max-attempts: 3
    initial-delay: 60
    multiplier: 2
    max-delay: 300

  # 通知設定
  notification:
    email:
      enabled: true
      recipients: admin@example.com
    slack:
      enabled: false
      webhook-url: ""

  # モニタリング設定
  monitoring:
    metrics-enabled: true
    health-check-interval: 60
```

## 6. テスト計画

### 6.1. 単体テスト
- スケジューラーの基本機能
- リトライロジック
- エラーハンドリング
- タスク優先順位付け

### 6.2. 統合テスト
- 実際のタスク実行
- 他のコンポーネントとの連携
- 設定の読み込み
- 通知機能

### 6.3. 負荷テスト
- 複数タスクの同時実行
- リソース使用量の測定
- タイムアウト処理
- 長期実行時の安定性

## 7. 運用設計

### 7.1. 監視項目
- タスク実行状況
- エラー発生率
- リソース使用率
- 処理時間

### 7.2. アラート条件
- タスク失敗
- リトライ上限到達
- タイムアウト発生
- リソース枯渇

### 7.3. バックアップ計画
- スケジュール設定のバックアップ
- タスク実行履歴の保存
- 状態復元手順の整備