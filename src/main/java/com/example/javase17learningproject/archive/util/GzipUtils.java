package com.example.javase17learningproject.archive.util;

import com.example.javase17learningproject.archive.ArchiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP圧縮ユーティリティクラス.
 */
public class GzipUtils {
    private static final Logger logger = LoggerFactory.getLogger(GzipUtils.class);
    private static final int BUFFER_SIZE = 8192;

    private GzipUtils() {
        // ユーティリティクラスのため、インスタンス化を防止
    }

    /**
     * 文字列をGZIP形式で圧縮します.
     *
     * @param content 圧縮する文字列
     * @return 圧縮されたバイト配列
     * @throws ArchiveException 圧縮処理に失敗した場合
     */
    public static byte[] compress(String content) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            
            gzos.write(content.getBytes(StandardCharsets.UTF_8));
            gzos.finish();
            return baos.toByteArray();
            
        } catch (IOException e) {
            logger.error("Failed to compress content", e);
            throw new ArchiveException("Failed to compress content", e);
        }
    }

    /**
     * GZIP形式で圧縮されたバイト配列を展開します.
     *
     * @param compressed 圧縮されたバイト配列
     * @return 展開された文字列
     * @throws ArchiveException 展開処理に失敗した場合
     */
    public static String decompress(byte[] compressed) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gzis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
            
        } catch (IOException e) {
            logger.error("Failed to decompress content", e);
            throw new ArchiveException("Failed to decompress content", e);
        }
    }

    /**
     * ファイルをGZIP形式で圧縮します.
     *
     * @param sourceFile 圧縮元のファイル
     * @param targetFile 圧縮先のファイル
     * @throws ArchiveException 圧縮処理に失敗した場合
     */
    public static void compressFile(File sourceFile, File targetFile) {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(targetFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
            }
            gzos.finish();
            
        } catch (IOException e) {
            logger.error("Failed to compress file: {}", sourceFile.getName(), e);
            throw new ArchiveException("Failed to compress file: " + sourceFile.getName(), e);
        }
    }

    /**
     * GZIP形式で圧縮されたファイルを展開します.
     *
     * @param sourceFile 圧縮されたファイル
     * @param targetFile 展開先のファイル
     * @throws ArchiveException 展開処理に失敗した場合
     */
    public static void decompressFile(File sourceFile, File targetFile) {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            
        } catch (IOException e) {
            logger.error("Failed to decompress file: {}", sourceFile.getName(), e);
            throw new ArchiveException("Failed to decompress file: " + sourceFile.getName(), e);
        }
    }
}