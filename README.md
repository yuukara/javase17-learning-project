# Java SE 17 Learning Project

Spring Boot と Java SE 17 を使用したユーザー管理システムのサンプルプロジェクトです。

## 開発環境

- Java 17
- Spring Boot 3.2.3
- Maven
- H2 Database
- Thymeleaf
- Spring Security
- Selenium WebDriver

## ビルドとテスト

### プロジェクトのビルド
```bash
mvn clean install
```

### テストの実行

#### 単体テスト
```bash
mvn test
```

#### E2Eテストの実行
```bash
# テスト用プロファイルを有効化
export SPRING_PROFILES_ACTIVE=test

# E2Eテストの実行
mvn test -Dtest=*E2ETest

# 特定のE2Eテストの実行
mvn test -Dtest=LoginE2ETest
```

### テスト結果の確認

#### テストレポート
テスト実行後、以下のディレクトリでレポートを確認できます：
```bash
target/surefire-reports/
```

#### E2Eテストのログ
E2Eテスト実行時のログは以下のファイルで確認できます：
```bash
logs/e2e-test.log    # 基本的なE2Eテストログ
logs/test-detail.log # 詳細なデバッグ情報
```

#### スクリーンショット
テスト失敗時のスクリーンショットは以下のディレクトリに保存されます：
```bash
test-output/screenshots/
```

## トラブルシューティング

### E2Eテスト実行時の注意点

1. ブラウザのバージョン確認
```bash
# Chromeのバージョン確認
google-chrome --version
```

2. WebDriverの更新
```bash
# テスト実行時にWebDriverが自動更新されます
# 手動更新が必要な場合は以下を実行
mvn clean test -Dtest=LoginE2ETest -Dwdm.chromeDriverVersion=latest
```

3. テストデータのリセット
```bash
# H2データベースのクリーンアップ
rm -rf ~/.h2/testdb*
```

### よくある問題と解決方法

1. テストが遅い場合
- application-test.propertiesでタイムアウト設定を調整
- test.selenium.implicit-wait の値を調整

2. 要素が見つからない場合
- ページの読み込みを待機
- WebDriverWaitの使用を検討

3. ヘッドレスモードでの問題
- application-test.propertiesで test.selenium.headless=false に設定
- ブラウザウィンドウサイズの調整

## 開発ガイドライン

### テストコード作成時の注意点

1. Page Objectパターンの使用
- 各画面の操作は専用のPageクラスに実装
- 要素のセレクタは一箇所で管理

2. テストデータの管理
- TestDataFactoryを使用
- テストデータはtest-data.sqlで管理

3. テストの分類
- @E2ETest: E2Eテスト用
- @WithTestUser: 認証が必要なテスト用

4. ログ出力
- テスト失敗時の原因特定のため、適切なログを出力
- スクリーンショットの活用