package com.example.javase17learningproject.archive;

/**
 * アーカイブ処理に関する例外クラス.
 */
public class ArchiveException extends RuntimeException {

    /**
     * デフォルトコンストラクタ.
     */
    public ArchiveException() {
        super();
    }

    /**
     * 指定されたメッセージで例外を作成します.
     *
     * @param message エラーメッセージ
     */
    public ArchiveException(String message) {
        super(message);
    }

    /**
     * 指定されたメッセージと原因となった例外で例外を作成します.
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public ArchiveException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 指定された原因となった例外で例外を作成します.
     *
     * @param cause 原因となった例外
     */
    public ArchiveException(Throwable cause) {
        super(cause);
    }
}