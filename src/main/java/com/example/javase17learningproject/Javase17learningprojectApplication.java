package com.example.javase17learningproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * アプリケーションのメインクラス。
 * アプリケーションの起動とSpring Bootの設定を行います。
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class Javase17learningprojectApplication {

    public static void main(String[] args) {
        SpringApplication.run(Javase17learningprojectApplication.class, args);
    }
}
