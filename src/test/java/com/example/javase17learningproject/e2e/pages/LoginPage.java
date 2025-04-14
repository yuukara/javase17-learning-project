package com.example.javase17learningproject.e2e.pages;

import java.time.Duration;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ログインページのページオブジェクト。
 * Seleniumのページオブジェクトパターンを使用して
 * ログインページの要素とアクションをカプセル化します。
 */
public class LoginPage {

    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebDriver driver;
    private final WebDriverWait wait;

    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "loginButton")
    private WebElement loginButton;

    @FindBy(className = "alert-danger")
    private WebElement errorMessage;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, TIMEOUT);
        PageFactory.initElements(driver, this);
    }

    /**
     * ログインページに移動します。
     * @param baseUrl ベースURL
     * @throws TimeoutException ページの読み込みがタイムアウトした場合
     */
    public void navigateTo(String baseUrl) {
        log.debug("Navigating to login page");
        driver.get(baseUrl + "/login");
        waitForPageLoad();
    }

    /**
     * 指定された認証情報でログインを実行します。
     * @param username ユーザー名
     * @param password パスワード
     * @throws TimeoutException 要素の操作がタイムアウトした場合
     */
    public void login(String username, String password) {
        log.debug("Attempting login with username: {}", username);
        
        waitAndSendKeys(usernameInput, username);
        waitAndSendKeys(passwordInput, password);
        waitAndClick(loginButton);
        
        log.debug("Login attempt completed");
    }

    /**
     * ログインページが表示されているかを確認します。
     * @return ログインページが表示されている場合はtrue
     */
    public boolean isAt() {
        try {
            return wait.until(ExpectedConditions.urlContains("/login")) &&
                   isElementDisplayed(usernameInput) &&
                   isElementDisplayed(passwordInput) &&
                   isElementDisplayed(loginButton);
        } catch (TimeoutException e) {
            log.warn("Timeout while checking login page visibility", e);
            return false;
        }
    }

    /**
     * エラーメッセージが表示されているかを確認します。
     * @return エラーメッセージが表示されている場合はtrue
     */
    public boolean hasError() {
        try {
            return isElementDisplayed(errorMessage);
        } catch (NoSuchElementException | TimeoutException e) {
            return false;
        }
    }

    /**
     * エラーメッセージのテキストを取得します。
     * @return エラーメッセージのテキスト。メッセージが表示されていない場合は空文字
     */
    public String getErrorMessage() {
        if (hasError()) {
            return errorMessage.getText();
        }
        return "";
    }

    private void waitForPageLoad() {
        try {
            wait.until(ExpectedConditions.visibilityOf(usernameInput));
        } catch (TimeoutException e) {
            log.error("Timeout waiting for login page to load", e);
            throw e;
        }
    }

    private void waitAndSendKeys(WebElement element, String text) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
        element.clear();
        element.sendKeys(text);
    }

    private void waitAndClick(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element));
        element.click();
    }

    private boolean isElementDisplayed(WebElement element) {
        try {
            return wait.until(ExpectedConditions.visibilityOf(element))
                      .isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
}