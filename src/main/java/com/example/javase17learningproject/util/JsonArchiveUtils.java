package com.example.javase17learningproject.util;

import com.example.javase17learningproject.model.AuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 監査ログのJSON形式アーカイブを処理するユーティリティクラス。
 */
@Component
public class JsonArchiveUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonArchiveUtils.class);
    private final ObjectMapper objectMapper;

    public JsonArchiveUtils() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 監査ログアーカイブのメタデータ構造
     */
    public record ArchiveMetadata(
        String date,
        int recordCount,
        String checksum,
        String version,
        LocalDateTime createdAt
    ) {}

    /**
     * アーカイブファイルの構造
     */
    public record ArchiveFile(
        ArchiveMetadata metadata,
        List<AuditLog> logs
    ) {}

    /**
     * 監査ログリストをGZIP圧縮されたJSONファイルとして保存します。
     */
    public void saveToGzipJson(List<AuditLog> logs, Path filePath) throws IOException {
        // 親ディレクトリが存在しない場合は作成
        Files.createDirectories(filePath.getParent());

        // メタデータの作成
        ArchiveMetadata metadata = new ArchiveMetadata(
            filePath.getFileName().toString(),
            logs.size(),
            calculateChecksum(logs),
            "1.0",
            LocalDateTime.now()
        );

        // アーカイブファイルの作成
        ArchiveFile archiveFile = new ArchiveFile(metadata, logs);

        try (OutputStream fos = Files.newOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             Writer writer = new OutputStreamWriter(gzos)) {
            
            objectMapper.writeValue(writer, archiveFile);
            logger.info("アーカイブファイルを作成: {}", filePath);
        }
    }

    /**
     * GZIP圧縮されたJSONファイルから監査ログを読み込みます。
     */
    public List<AuditLog> loadFromGzipJson(Path filePath) throws IOException {
        try (InputStream fis = Files.newInputStream(filePath);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             Reader reader = new InputStreamReader(gzis)) {
            
            ArchiveFile archiveFile = objectMapper.readValue(
                reader,
                new TypeReference<ArchiveFile>() {}
            );

            // チェックサムの検証
            String actualChecksum = calculateChecksum(archiveFile.logs);
            if (!actualChecksum.equals(archiveFile.metadata.checksum)) {
                throw new IOException("チェックサムが一致しません");
            }

            logger.info("アーカイブファイルを読み込み: {} ({} レコード)",
                filePath, archiveFile.logs.size());
            return archiveFile.logs;
        }
    }

    /**
     * 監査ログリストのチェックサムを計算します。
     */
    private String calculateChecksum(List<AuditLog> logs) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(logs);
        return String.valueOf(json.hashCode());
    }

    /**
     * 指定されたパスのアーカイブファイルのメタデータのみを読み込みます。
     */
    public ArchiveMetadata readMetadata(Path filePath) throws IOException {
        try (InputStream fis = Files.newInputStream(filePath);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             Reader reader = new InputStreamReader(gzis)) {
            
            ArchiveFile archiveFile = objectMapper.readValue(
                reader,
                new TypeReference<ArchiveFile>() {}
            );
            return archiveFile.metadata;
        }
    }
}