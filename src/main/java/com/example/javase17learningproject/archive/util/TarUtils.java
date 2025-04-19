package com.example.javase17learningproject.archive.util;

import com.example.javase17learningproject.archive.ArchiveException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * TAR形式のアーカイブを操作するユーティリティクラス.
 */
public class TarUtils {
    private static final Logger logger = LoggerFactory.getLogger(TarUtils.class);
    private static final int BUFFER_SIZE = 8192;

    private TarUtils() {
        // ユーティリティクラスのため、インスタンス化を防止
    }

    /**
     * 指定されたファイルリストからTARアーカイブを作成します.
     *
     * @param sourceFiles アーカイブに含めるファイルのリスト
     * @param targetFile 作成するTARファイル
     * @throws ArchiveException アーカイブ作成に失敗した場合
     */
    public static void createTarArchive(List<File> sourceFiles, File targetFile) {
        try (FileOutputStream fos = new FileOutputStream(targetFile);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(fos)) {
            
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            for (File file : sourceFiles) {
                if (!file.exists()) {
                    logger.warn("File does not exist: {}", file.getName());
                    continue;
                }

                // TARエントリの作成
                TarArchiveEntry entry = new TarArchiveEntry(file, file.getName());
                taos.putArchiveEntry(entry);

                // ファイルの内容を書き込み
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        taos.write(buffer, 0, len);
                    }
                }

                taos.closeArchiveEntry();
            }

            taos.finish();
            logger.info("Created TAR archive: {}", targetFile.getName());

        } catch (IOException e) {
            logger.error("Failed to create TAR archive", e);
            throw new ArchiveException("Failed to create TAR archive", e);
        }
    }

    /**
     * TARアーカイブからファイルを展開します.
     *
     * @param sourceFile TARアーカイブファイル
     * @param targetDir 展開先ディレクトリ
     * @return 展開されたファイルのリスト
     * @throws ArchiveException アーカイブの展開に失敗した場合
     */
    public static List<File> extractTarArchive(File sourceFile, File targetDir) {
        List<File> extractedFiles = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(sourceFile);
             TarArchiveInputStream tais = new TarArchiveInputStream(fis)) {
            
            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                File outputFile = new File(targetDir, entry.getName());
                Files.createDirectories(outputFile.getParentFile().toPath());

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = tais.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }

                extractedFiles.add(outputFile);
            }

            logger.info("Extracted {} files from TAR archive", extractedFiles.size());
            return extractedFiles;

        } catch (IOException e) {
            logger.error("Failed to extract TAR archive", e);
            throw new ArchiveException("Failed to extract TAR archive", e);
        }
    }

    /**
     * TARアーカイブ内のファイルを検証します.
     *
     * @param archiveFile TARアーカイブファイル
     * @return アーカイブが有効な場合はtrue
     * @throws ArchiveException 検証に失敗した場合
     */
    public static boolean verifyTarArchive(File archiveFile) {
        try (FileInputStream fis = new FileInputStream(archiveFile);
             TarArchiveInputStream tais = new TarArchiveInputStream(fis)) {
            
            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                // エントリの存在を確認するだけで十分
                if (entry.getSize() < 0) {
                    return false;
                }
            }
            return true;

        } catch (IOException e) {
            logger.error("Failed to verify TAR archive", e);
            throw new ArchiveException("Failed to verify TAR archive", e);
        }
    }
}