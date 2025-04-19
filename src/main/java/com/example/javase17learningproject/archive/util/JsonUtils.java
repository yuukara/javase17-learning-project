package com.example.javase17learningproject.archive.util;

import com.example.javase17learningproject.archive.ArchiveException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JSON変換ユーティリティクラス.
 */
public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private JsonUtils() {
        // ユーティリティクラスのため、インスタンス化を防止
    }

    /**
     * オブジェクトをJSON文字列に変換します.
     *
     * @param object 変換対象のオブジェクト
     * @return JSON文字列
     * @throws ArchiveException JSON変換に失敗した場合
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            logger.error("Failed to convert object to JSON", e);
            throw new ArchiveException("Failed to convert object to JSON", e);
        }
    }

    /**
     * JSON文字列を指定された型のオブジェクトに変換します.
     *
     * @param json JSON文字列
     * @param clazz 変換先のクラス
     * @param <T> 変換先の型
     * @return 変換されたオブジェクト
     * @throws ArchiveException JSON変換に失敗した場合
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            logger.error("Failed to convert JSON to object", e);
            throw new ArchiveException("Failed to convert JSON to object", e);
        }
    }

    /**
     * JSON文字列を指定された型のオブジェクトに変換します（ジェネリック型用）.
     *
     * @param json JSON文字列
     * @param typeReference 変換先の型情報
     * @param <T> 変換先の型
     * @return 変換されたオブジェクト
     * @throws ArchiveException JSON変換に失敗した場合
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            logger.error("Failed to convert JSON to object", e);
            throw new ArchiveException("Failed to convert JSON to object", e);
        }
    }

    /**
     * ObjectMapperのインスタンスを取得します.
     * 
     * @return ObjectMapperインスタンス
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}