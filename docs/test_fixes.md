# テストパッケージの修正方針

## 1. 問題の分析

### 1.1 キャッシュ関連の問題
- Hibernateの2次キャッシュとアプリケーションキャッシュ（Caffeine）の混在
- JCacheRegionFactory実装の欠如
- テスト時のキャッシュ設定の矛盾

### 1.2 設定の重複
- セキュリティ設定の重複（USER/ADMIN）
- Selenium設定の重複
- データベース設定の分散

### 1.3 テストコンテキストの問題
- E2Eテストとユニットテストのコンテキストが混在
- Spring Boot設定の過剰な読み込み

## 2. 修正方針

### 2.1 テスト設定の分離
1. テスト種別ごとの設定ファイル作成
   - `application-test-unit.properties`
   - `application-test-integration.properties`
   - `application-test-e2e.properties`

2. 共通設定の集約
   - `application-test-common.properties`

### 2.2 キャッシュ設定の整理
1. テスト環境でのキャッシュ無効化
   ```properties
   spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.internal.NoCachingRegionFactory
   spring.cache.type=none
   ```

2. 本番環境用のキャッシュ設定追加
   - Hibernateキャッシュ依存関係の追加
   - キャッシュ設定の明確化

### 2.3 セキュリティ設定の統合
- 重複を排除
- テスト用アカウントの一元管理
- パスワードエンコーディング設定の明確化

### 2.4 テストコンテキストの最適化
1. 各テストタイプに必要最小限の設定
   - エンティティテスト：JPAのみ
   - サービステスト：JPA + キャッシュ
   - E2Eテスト：フルコンテキスト

2. テストクラスのアノテーション見直し
   - `@DataJpaTest`
   - `@SpringBootTest`
   - カスタムアノテーションの作成

## 3. 優先順位

1. 高優先度
   - キャッシュ設定の修正
   - テスト設定ファイルの分離

2. 中優先度
   - セキュリティ設定の統合
   - テストコンテキストの最適化

3. 低優先度
   - カスタムアノテーションの作成
   - テストコードのリファクタリング

## 4. 想定される効果

1. テストの安定性向上
   - キャッシュ関連エラーの解消
   - コンテキスト起動の信頼性向上

2. 保守性の改善
   - 設定の重複排除
   - 責務の明確な分離

3. テスト実行時間の短縮
   - 必要最小限のコンテキスト
   - 効率的なリソース使用