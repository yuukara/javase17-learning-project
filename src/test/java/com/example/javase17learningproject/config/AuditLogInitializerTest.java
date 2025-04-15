package com.example.javase17learningproject.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.YearMonth;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.boot.context.event.ApplicationReadyEvent;

class AuditLogInitializerTest {

    @TempDir
    Path tempDir;

    private ArchiveConfig archiveConfig;
    private AuditLogInitializer initializer;
    private ApplicationReadyEvent event;

    @BeforeEach
    void setUp() {
        archiveConfig = mock(ArchiveConfig.class);
        when(archiveConfig.archiveBasePath()).thenReturn(tempDir);
        when(archiveConfig.dailyArchivePath()).thenReturn(tempDir.resolve("daily"));
        when(archiveConfig.monthlyArchivePath()).thenReturn(tempDir.resolve("monthly"));

        initializer = new AuditLogInitializer(archiveConfig);
        event = mock(ApplicationReadyEvent.class);
    }

    @Test
    void testInitializeDirectories() {
        // When
        initializer.onApplicationEvent(event);

        // Then
        assertThat(Files.exists(tempDir.resolve("daily"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("monthly"))).isTrue();

        // 現在の年月のディレクトリが作成されていることを確認
        YearMonth current = YearMonth.now();
        Path yearMonthPath = tempDir.resolve("daily")
            .resolve(String.valueOf(current.getYear()))
            .resolve(String.format("%02d", current.getMonthValue()));
        assertThat(Files.exists(yearMonthPath)).isTrue();
    }

    @Test
    void testInitializeWithExistingDirectories() throws IOException {
        // Given
        Files.createDirectories(tempDir.resolve("daily"));
        Files.createDirectories(tempDir.resolve("monthly"));

        // When
        initializer.onApplicationEvent(event);

        // Then
        assertThat(Files.exists(tempDir.resolve("daily"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("monthly"))).isTrue();
    }

    @Test
    void testInitializeWithReadOnlyDirectory() throws IOException {
        // Given: 読み取り専用のディレクトリを作成
        Path readOnlyDir = createReadOnlyDirectory();
        when(archiveConfig.dailyArchivePath()).thenReturn(readOnlyDir);

        // When/Then
        assertThatThrownBy(() -> initializer.onApplicationEvent(event))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to initialize audit log environment");
    }

    @Test
    void testVerifyEnvironment() {
        // Given
        initializer.onApplicationEvent(event);

        // When/Then: 書き込みテストが成功することを確認
        Path testFile = tempDir.resolve("daily/.write-test");
        assertThat(Files.exists(testFile)).isFalse(); // テストファイルは自動的に削除されているはず
    }

    @Test
    void testInitializeWithInvalidPath() {
        // Given: 存在しないパスを設定
        when(archiveConfig.archiveBasePath()).thenReturn(Path.of("/invalid/path"));

        // When/Then
        assertThatThrownBy(() -> initializer.onApplicationEvent(event))
            .isInstanceOf(RuntimeException.class);
    }

    /**
     * 読み取り専用のディレクトリを作成します。
     * POSIXシステムの場合はPOSIX権限を、そうでない場合は通常の読み取り専用属性を設定します。
     */
    private Path createReadOnlyDirectory() throws IOException {
        Path readOnlyDir = tempDir.resolve("readonly");
        if (isPosixSystem()) {
            FileAttribute<Set<PosixFilePermission>> attrs = 
                PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("r-xr-xr-x")
                );
            return Files.createDirectories(readOnlyDir, attrs);
        } else {
            Path dir = Files.createDirectories(readOnlyDir);
            dir.toFile().setReadOnly();
            return dir;
        }
    }

    private boolean isPosixSystem() {
        return !System.getProperty("os.name").toLowerCase().contains("windows");
    }
}