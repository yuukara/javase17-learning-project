package com.example.javase17learningproject.util;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;

class JsonArchiveUtilsTest {

    private JsonArchiveUtils jsonArchiveUtils;
    private List<AuditLog> testLogs;
    private LocalDateTime baseTime;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jsonArchiveUtils = new JsonArchiveUtils();
        baseTime = LocalDateTime.now();
        testLogs = createTestLogs();
    }

    @Test
    void testSaveAndLoadArchive() throws IOException {
        // Given
        Path archivePath = tempDir.resolve("test_archive.json.gz");

        // When
        jsonArchiveUtils.saveToGzipJson(testLogs, archivePath);
        List<AuditLog> loadedLogs = jsonArchiveUtils.loadFromGzipJson(archivePath);

        // Then
        assertThat(loadedLogs).hasSize(testLogs.size());
        assertThat(loadedLogs.get(0).eventType())
            .isEqualTo(testLogs.get(0).eventType());
        assertThat(loadedLogs.get(0).createdAt())
            .isEqualToIgnoringNanos(testLogs.get(0).createdAt());
    }

    @Test
    void testReadMetadata() throws IOException {
        // Given
        Path archivePath = tempDir.resolve("test_archive.json.gz");
        jsonArchiveUtils.saveToGzipJson(testLogs, archivePath);

        // When
        JsonArchiveUtils.ArchiveMetadata metadata = 
            jsonArchiveUtils.readMetadata(archivePath);

        // Then
        assertThat(metadata.recordCount()).isEqualTo(testLogs.size());
        assertThat(metadata.version()).isEqualTo("1.0");
        assertThat(metadata.date()).isEqualTo(archivePath.getFileName().toString());
    }

    @Test
    void testInvalidArchiveFile() {
        // Given
        Path invalidPath = tempDir.resolve("invalid.json.gz");

        // When/Then
        assertThatThrownBy(() -> 
            jsonArchiveUtils.loadFromGzipJson(invalidPath)
        ).isInstanceOf(IOException.class);
    }

    @Test
    void testEmptyLogList() throws IOException {
        // Given
        Path archivePath = tempDir.resolve("empty_archive.json.gz");
        List<AuditLog> emptyLogs = List.of();

        // When
        jsonArchiveUtils.saveToGzipJson(emptyLogs, archivePath);
        List<AuditLog> loadedLogs = jsonArchiveUtils.loadFromGzipJson(archivePath);

        // Then
        assertThat(loadedLogs).isEmpty();
    }

    private List<AuditLog> createTestLogs() {
        return List.of(
            new AuditLog(
                1L,
                "USER_LOGIN",
                Severity.LOW,
                1L,
                null,
                "User logged in",
                baseTime
            ),
            new AuditLog(
                2L,
                "USER_UPDATE",
                Severity.MEDIUM,
                1L,
                2L,
                "User profile updated",
                baseTime.plusHours(1)
            ),
            new AuditLog(
                3L,
                "USER_DELETE",
                Severity.HIGH,
                1L,
                3L,
                "User deleted",
                baseTime.plusHours(2)
            )
        );
    }
}