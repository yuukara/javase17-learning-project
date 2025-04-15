# 監査ログキャッシュ戦略 設計書

## 1. 概要

監査ログのパフォーマンスを最適化するため、多層的なキャッシュ戦略を実装します。

### 1.1. キャッシュレイヤー
1. インメモリキャッシュ（L1）
   - 最新のログエントリ
   - 高速アクセス用

2. Hibernateセカンドレベルキャッシュ（L2）
   - 頻繁にアクセスされる過去ログ
   - クエリキャッシュ

### 1.2. 非機能要件
- レスポンスタイム: キャッシュヒット時 10ms以内
- メモリ使用量: 最大1GB
- 整合性: 更新から5分以内の反映
- スレッドセーフ性の確保

## 2. インメモリキャッシュの設計

### 2.1. キャッシュ設定
```java
@Configuration
public class AuditLogCacheConfig {
    @Bean
    public Cache<Long, AuditLog> auditLogCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .recordStats()
            .build();
    }
}
```

### 2.2. キャッシュ更新戦略
- Write-Through: 書き込み時に同時更新
- Read-Ahead: アクセスパターンの予測
- 定期的な一括更新（15分間隔）

### 2.3. エビクション方針
- LRU (Least Recently Used)
- サイズベース (最大1000エントリ)
- 時間ベース (15分)

## 3. Hibernateセカンドレベルキャッシュ

### 3.1. 設定
```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          region.factory_class: org.hibernate.cache.ehcache.EhCacheRegionFactory
          use_query_cache: true
```

### 3.2. エンティティキャッシュ設定
```java
@Entity
@Table(name = "audit_logs")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AuditLogEntity {
    // ... エンティティの実装
}
```

### 3.3. クエリキャッシュ
```java
@QueryHints({
    @QueryHint(name = "org.hibernate.cacheable", value = "true"),
    @QueryHint(name = "org.hibernate.cacheRegion", value = "audit.log.query")
})
List<AuditLogEntity> findByEventTypeAndCreatedAtBetween(
    String eventType,
    LocalDateTime start,
    LocalDateTime end
);
```

## 4. キャッシュ整合性管理

### 4.1. 更新トリガー
- エンティティの変更時
- スケジュールされた更新
- 手動キャッシュクリア

### 4.2. 整合性チェック
```java
@Service
public class AuditLogCacheManager {
    // キャッシュの整合性チェック
    public void verifyCache() {
        // 最新データとの比較
        // 不整合があれば更新
    }

    // キャッシュの強制更新
    public void forceRefresh() {
        // 全キャッシュのクリア
        // 最新データの再読み込み
    }
}
```

## 5. 監視と運用

### 5.1. キャッシュ統計
- ヒット率
- ミス率
- 読み込み時間
- メモリ使用量

### 5.2. メトリクス収集
```java
@Configuration
public class CacheMetricsConfig {
    @Bean
    public CacheMetricsCollector cacheMetrics(
        Cache<Long, AuditLog> auditLogCache
    ) {
        return new CacheMetricsCollector(auditLogCache);
    }
}
```

### 5.3. アラート条件
- ヒット率が80%未満
- メモリ使用量が90%超過
- 更新遅延が5分超過

## 6. パフォーマンスチューニング

### 6.1. キャッシュサイズの最適化
- アクセスパターンの分析
- メモリ使用量の監視
- 動的なサイズ調整

### 6.2. プリフェッチ戦略
- アクセス頻度の高いデータの特定
- バックグラウンドでの先読み
- キャッシュウォーミング

### 6.3. 負荷テスト
- 同時アクセス時の性能
- メモリリーク検出
- 長期実行時の安定性

## 7. テスト計画

### 7.1. 単体テスト
- キャッシュ操作の基本機能
- エビクションポリシー
- 整合性チェック

### 7.2. 統合テスト
- 複数キャッシュレイヤーの連携
- トランザクション管理との整合性
- 並行アクセス時の動作

### 7.3. 性能テスト
- キャッシュヒット率の測定
- レスポンスタイムの計測
- メモリ使用量の推移