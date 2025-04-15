package com.example.javase17learningproject.scheduler.model;

/**
 * タスクの実行状態を表す列挙型。
 */
public enum TaskStatus {
    PENDING("待機中"),
    RUNNING("実行中"),
    SUCCEEDED("成功"),
    FAILED("失敗"),
    RETRYING("リトライ中"),
    CANCELLED("キャンセル済"),
    TIMEOUT("タイムアウト");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * タスクが最終状態（これ以上状態遷移しない）かどうかを判定します。
     */
    public boolean isFinal() {
        return this == SUCCEEDED || this == FAILED || 
               this == CANCELLED || this == TIMEOUT;
    }

    /**
     * タスクが失敗状態かどうかを判定します。
     */
    public boolean isFailure() {
        return this == FAILED || this == TIMEOUT;
    }

    /**
     * タスクがリトライ可能な状態かどうかを判定します。
     */
    public boolean canRetry() {
        return this == FAILED || this == TIMEOUT;
    }

    /**
     * タスクがアクティブ（実行中または一時停止中）かどうかを判定します。
     */
    public boolean isActive() {
        return this == RUNNING || this == RETRYING;
    }
}