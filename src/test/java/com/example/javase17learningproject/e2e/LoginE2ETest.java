package com.example.javase17learningproject.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.javase17learningproject.e2e.annotation.E2ETest;
import com.example.javase17learningproject.e2e.pages.LoginPage;
import com.example.javase17learningproject.e2e.util.TestHelper;

/**
 * ログイン機能のE2Eテスト。
 * 実際のブラウザ操作をシミュレートしてテストを行います。
 */
@E2ETest
public class LoginE2ETest {

    @Autowired
    private WebDriver driver;

    @Autowired
    private TestHelper testHelper;

    private LoginPage loginPage;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        loginPage = new LoginPage(driver);
        baseUrl = "http://localhost:" + System.getProperty("test.server.port", "8080");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // ブラウザの終了に失敗しても続行
            }
        }
    }

    @Test
    @DisplayName("正常なログインフロー：正しい認証情報でログインできること")
    void testSuccessfulLogin() {
        // ログインページに移動
        loginPage.navigateTo(baseUrl);

        // ログイン実行
        loginPage.login("admin@example.com", "admin123");

        // ログイン成功を確認
        assertThat(driver.getCurrentUrl()).isEqualTo(baseUrl + "/");
    }

    @Test
    @DisplayName("異常なログインフロー：誤った認証情報でログインできないこと")
    void testFailedLogin() {
        // ログインページに移動
        loginPage.navigateTo(baseUrl);

        // 誤った認証情報でログイン
        loginPage.login("wrong@example.com", "wrongpass");

        // エラーメッセージを確認
        assertThat(loginPage.hasError()).isTrue();
        assertThat(loginPage.getErrorMessage()).contains("Invalid credentials");

        // スクリーンショットを取得
        testHelper.takeScreenshot(driver, "login_error");
    }

    @Test
    @DisplayName("ログインページのアクセシビリティ：ページが正しく表示されること")
    void testLoginPageAccessibility() {
        // ログインページに移動
        loginPage.navigateTo(baseUrl);

        // ログインページの要素が表示されていることを確認
        assertThat(loginPage.isAt()).isTrue();
    }
}