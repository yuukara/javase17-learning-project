package com.example.javase17learningproject.e2e.config;

import java.time.Duration;
import java.util.logging.Level;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Seleniumテストの設定クラス。
 * WebDriverの設定と初期化を管理します。
 */
@Configuration
@Profile("test")
public class SeleniumConfig {

    private static final Logger log = LoggerFactory.getLogger(SeleniumConfig.class);

    @Value("${test.selenium.headless:true}")
    private boolean headless;

    @Value("${test.selenium.implicit-wait:10}")
    private int implicitWait;

    @Value("${test.selenium.page-load-timeout:30}")
    private int pageLoadTimeout;

    private WebDriver webDriver;

    @PostConstruct
    void setupClass() {
        // Seleniumのログレベルを設定
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.WARNING);
        
        // ChromeDriverのセットアップ
        WebDriverManager.chromedriver().setup();
        log.info("ChromeDriver setup completed");
    }

    @Bean
    public WebDriver webDriver() {
        ChromeOptions options = new ChromeOptions();
        
        // ヘッドレスモードの設定
        if (headless) {
            options.addArguments("--headless");
            log.info("Chrome running in headless mode");
        }

        // その他の必要なオプション
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        // WebDriverの作成
        webDriver = new ChromeDriver(options);
        
        // タイムアウトの設定
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        webDriver.manage().window().maximize();

        log.info("WebDriver initialized with implicit wait: {}s, page load timeout: {}s",
                implicitWait, pageLoadTimeout);

        return webDriver;
    }

    @PreDestroy
    void tearDown() {
        if (webDriver != null) {
            try {
                webDriver.quit();
                log.info("WebDriver shutdown completed");
            } catch (Exception e) {
                log.error("Error during WebDriver shutdown", e);
            }
        }
    }
}