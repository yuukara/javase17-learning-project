package com.example.javase17learningproject.scheduler.model;

/**
 * スケジュールされたタスクの種別を表す列挙型。
 */
public enum TaskType {
    DAILY_ARCHIVE("日次アーカイブ作成"),
    MONTHLY_ARCHIVE("月次アーカイブ作成"),
    CLEANUP("古いアーカイブの削除"),
    CACHE_REFRESH("キャッシュの更新");

    private final String description;

    TaskType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}