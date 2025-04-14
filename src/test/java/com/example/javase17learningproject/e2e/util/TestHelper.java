package com.example.javase17learningproject.e2e.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * E2Eテストのヘルパークラス。
 * スクリーンショットの取得やテスト関連のユーティリティ機能を提供します。
 */
@Component
public class TestHelper {

    private static final Logger log = LoggerFactory.getLogger(TestHelper.class);
    private static final String SCREENSHOT_DIR = "test-output/screenshots";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * スクリーンショットを取得し、ファイルに保存します。
     * 
     * @param driver WebDriverインスタンス
     * @param testName テスト名
     * @return 保存されたファイルのパス
     */
    public String takeScreenshot(WebDriver driver, String testName) {
        try {
            createScreenshotDirectory();
            
            String fileName = generateFileName(testName);
            Path destination = Paths.get(SCREENSHOT_DIR, fileName);

            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), destination);

            log.info("Screenshot saved: {}", destination);
            return destination.toString();
        } catch (IOException e) {
            log.error("Failed to take screenshot", e);
            return null;
        }
    }

    /**
     * テスト失敗時にスクリーンショットを取得します。
     * 
     * @param driver WebDriverインスタンス
     * @param testName テスト名
     * @param e 発生した例外
     */
    public void handleTestFailure(WebDriver driver, String testName, Throwable e) {
        log.error("Test failed: {}", testName, e);
        takeScreenshot(driver, "failure_" + testName);
    }

    /**
     * 指定された要素が表示されるまで待機します。
     * 
     * @param driver WebDriverインスタンス
     * @param timeoutSeconds タイムアウト時間（秒）
     */
    public void waitForPageLoad(WebDriver driver, int timeoutSeconds) {
        try {
            Thread.sleep(timeoutSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Wait interrupted", e);
        }
    }

    private void createScreenshotDirectory() throws IOException {
        Path screenshotDir = Paths.get(SCREENSHOT_DIR);
        if (!Files.exists(screenshotDir)) {
            Files.createDirectories(screenshotDir);
        }
    }

    private String generateFileName(String testName) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        return String.format("%s_%s.png", testName, timestamp);
    }
}