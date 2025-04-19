package com.example.javase17learningproject.archive.util;

import com.example.javase17learningproject.archive.ArchiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * チェックサム計算ユーティリティクラス.
 */
public class ChecksumUtils {
    private static final Logger logger = LoggerFactory.getLogger(ChecksumUtils.class);
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    private ChecksumUtils() {
        // ユーティリティクラスのため、インスタンス化を防止
    }

    /**
     * 文字列のSHA-256チェックサムを計算します.
     *
     * @param content チェックサムを計算する文字列
     * @return 16進数文字列形式のチェックサム
     * @throws ArchiveException チェックサム計算に失敗した場合
     */
    public static String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to calculate checksum: algorithm not found", e);
            throw new ArchiveException("Failed to calculate checksum: algorithm not found", e);
        }
    }

    /**
     * ファイルのSHA-256チェックサムを計算します.
     *
     * @param file チェックサムを計算するファイル
     * @return 16進数文字列形式のチェックサム
     * @throws ArchiveException チェックサム計算に失敗した場合
     */
    public static String calculateFileChecksum(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to calculate file checksum: algorithm not found", e);
            throw new ArchiveException("Failed to calculate file checksum: algorithm not found", e);
        } catch (IOException e) {
            logger.error("Failed to calculate file checksum: IO error", e);
            throw new ArchiveException("Failed to calculate file checksum: IO error", e);
        }
    }

    /**
     * バイト配列を16進数文字列に変換します.
     *
     * @param bytes 変換するバイト配列
     * @return 16進数文字列
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * チェックサムを検証します.
     *
     * @param content 検証する内容
     * @param expectedChecksum 期待されるチェックサム
     * @return チェックサムが一致する場合はtrue
     */
    public static boolean verifyChecksum(String content, String expectedChecksum) {
        String actualChecksum = calculateChecksum(content);
        return actualChecksum.equals(expectedChecksum);
    }

    /**
     * ファイルのチェックサムを検証します.
     *
     * @param file 検証するファイル
     * @param expectedChecksum 期待されるチェックサム
     * @return チェックサムが一致する場合はtrue
     */
    public static boolean verifyFileChecksum(File file, String expectedChecksum) {
        String actualChecksum = calculateFileChecksum(file);
        return actualChecksum.equals(expectedChecksum);
    }
}