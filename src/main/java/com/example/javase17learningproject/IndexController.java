package com.example.javase17learningproject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * インデックスコントローラー。
 * アプリケーションのルートURLへのアクセスを処理します。
 */
@Controller
public class IndexController {

    /**
     * ルートURLへのアクセスを処理し、インデックスページを表示します。
     *
     * @return インデックスページのテンプレート名
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}