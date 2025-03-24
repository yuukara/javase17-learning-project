package com.example.javase17learningproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVCの設定クラス
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // ログインページのViewControllerを登録
        registry.addViewController("/login").setViewName("login");
        // ルートページのViewControllerを登録
        registry.addViewController("/").setViewName("index");
    }
}