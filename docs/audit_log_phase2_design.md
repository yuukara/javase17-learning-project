# 監査ログ機能 Phase 2 設計書

## 1. データベース永続化

### 1.1. エンティティ設計

```java
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // インデックス
    @Index(name = "idx_audit_event_type")
    @Index(name = "idx_audit_user_id")
    @Index(name = "idx_audit_created_at")
}
```

### 1.2. リポジトリ設計

```java
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    // 基本的なCRUD操作は JpaRepository から継承

    // カスタムクエリ
    List<AuditLogEntity> findByEventType(String eventType);
    List<AuditLogEntity> findByUserId(Long userId);
    List<AuditLogEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<AuditLogEntity> findBySeverityAndCreatedAtBetween(
        Severity severity, LocalDateTime start, LocalDateTime end);
}
```

### 1.3. 変換層の設計

```java
@Component
public class AuditLogConverter {
    // Record -> Entity
    public AuditLogEntity toEntity(AuditLog record) {
        // 変換ロジック
    }

    // Entity -> Record
    public AuditLog toRecord(AuditLogEntity entity) {
        // 変換ロジック
    }
}
```

## 2. ログローテーション設計

### 2.1. 保持期間とクリーンアップ

- デフォルト保持期間: 90日
- クリーンアップバッチ: 毎日深夜3時に実行
- アーカイブ対象: 90日以上経過したログ

### 2.2. アーカイブ戦略

1. 日次アーカイブ
   - 1日分のログをJSONファイルに出力
   - アーカイブファイルの命名規則: `audit_log_YYYYMMDD.json`
   - 圧縮形式: GZIP

2. 月次アーカイブ
   - 1ヶ月分のアーカイブを統合
   - アーカイブファイルの命名規則: `audit_log_YYYYMM.tar.gz`

### 2.3. クリーンアップジョブ

```java
@Scheduled(cron = "0 0 3 * * ?")  // 毎日午前3時に実行
public void cleanupOldLogs() {
    // 90日以上経過したログの処理
    LocalDateTime threshold = LocalDateTime.now().minusDays(90);
    
    // アーカイブ処理
    archiveOldLogs(threshold);
    
    // データベースからの削除
    deleteOldLogs(threshold);
}
```

## 3. パフォーマンス最適化

### 3.1. インデックス戦略

1. 主要インデックス
   - event_type: 検索性能向上
   - user_id: ユーザー別ログ取得の高速化
   - created_at: 期間検索の効率化

2. 複合インデックス
   - (severity, created_at): 重要度と期間での検索
   - (user_id, created_at): ユーザー別の期間検索

### 3.2. キャッシュ戦略

1. インメモリキャッシュ
   - 最新1000件のログをメモリに保持
   - LRUキャッシュポリシー
   - キャッシュの自動更新（15分間隔）

2. 二次キャッシュ
   - Hibernateの二次キャッシュを使用
   - 頻繁にアクセスされる過去ログをキャッシュ

### 3.3. バッチ処理最適化

1. バルク操作
   - 一括インサート：100件単位
   - 一括アーカイブ：1日単位

2. 非同期処理
   - ログ書き込みの非同期化
   - アーカイブ処理の非同期実行

## 4. 移行計画

### 4.1. データ移行手順

1. 準備フェーズ
   - バックアップの作成
   - 移行スクリプトのテスト

2. 移行フェーズ
   - インメモリデータのエクスポート
   - データベースへのインポート
   - 整合性チェック

3. 検証フェーズ
   - データ件数の確認
   - サンプルデータの検証
   - パフォーマンステスト

### 4.2. ロールバック計画

1. トリガー条件
   - データ不整合の検出
   - 重大なパフォーマンス問題
   - アプリケーションエラーの発生

2. ロールバック手順
   - 新システムの停止
   - バックアップからの復旧
   - インメモリシステムの再開

## 5. テスト計画

### 5.1. 単体テスト

1. エンティティテスト
   - バリデーション
   - マッピング
   - インデックス

2. リポジトリテスト
   - CRUD操作
   - カスタムクエリ
   - 性能測定

### 5.2. 統合テスト

1. 移行テスト
   - データ整合性
   - パフォーマンス
   - ロールバック

2. システムテスト
   - 負荷テスト
   - 長期実行テスト
   - 障害復旧テスト