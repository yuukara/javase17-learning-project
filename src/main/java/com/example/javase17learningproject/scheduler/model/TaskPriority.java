package com.example.javase17learningproject.scheduler.model;

/**
 * タスクの優先順位を表す列挙型。
 * 数値が小さいほど優先順位が高くなります。
 */
public enum TaskPriority {
    HIGH(0, "高"),
    MEDIUM(5, "中"),
    LOW(10, "低");

    private final int value;
    private final String description;

    TaskPriority(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 優先順位を比較します。
     * @param other 比較対象の優先順位
     * @return 優先順位の差分（負の値の場合はこのインスタンスの方が優先順位が高い）
     */
    public int compareByValue(TaskPriority other) {
        return this.value - other.value;
    }

    /**
     * 指定された優先順位よりも高いかどうかを判定します。
     * @param other 比較対象の優先順位
     * @return このインスタンスの方が優先順位が高い場合はtrue
     */
    public boolean isHigherThan(TaskPriority other) {
        return this.value < other.value;
    }
}