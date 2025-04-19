package com.example.javase17learningproject.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent;
import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.repository.AuditLogInMemoryStorage;
import com.example.javase17learningproject.repository.AuditLogRepository;

/**
 * 監査ログサービスの実装クラス。
 * インメモリストレージとデータベースの両方を管理します。
 */
@Service
@EnableScheduling
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private static final int CACHE_SIZE = 1000;

    private final AuditLogRepository repository;
    private final AuditLogInMemoryStorage memoryStorage;

    public AuditLogServiceImpl(
        AuditLogRepository repository,
        AuditLogInMemoryStorage memoryStorage
    ) {
        this.repository = repository;
        this.memoryStorage = memoryStorage;
    }

    @Override
    @Transactional
    public AuditLog save(AuditLog log) {
        logger.debug("監査ログを保存: {}", log);

        // メモリストレージに保存
        AuditLog savedInMemory = memoryStorage.save(log);

        // データベースにも保存
        AuditLogEntity entity = AuditLogEntity.fromRecord(log);
        AuditLogEntity savedEntity = repository.save(entity);

        return savedEntity.toRecord();
    }

    @Override
    public AuditLog findById(Long id) {
        logger.debug("監査ログを検索: id = {}", id);

        // まずメモリキャッシュを確認
        Optional<AuditLog> fromMemory = memoryStorage.findById(id);
        if (fromMemory.isPresent()) {
            return fromMemory.get();
        }

        // データベースから検索
        return repository.findById(id)
            .map(AuditLogEntity::toRecord)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> searchLogs(
        String eventType,
        AuditEvent.Severity severity,
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    ) {
        logger.debug("監査ログを検索: eventType={}, severity={}, userId={}", 
            eventType, severity, userId);

        return repository.searchLogs(
            eventType,
            severity,
            userId,
            startDate,
            endDate,
            pageable
        ).map(AuditLogEntity::toRecord);
    }

    @Override
    public List<AuditLog> findLatestLogs(int limit) {
        logger.debug("最新の監査ログを取得: limit = {}", limit);
        return memoryStorage.findLatestLogs(limit);
    }

    @Override
    @Transactional
    public void archiveOldLogs(LocalDateTime before) {
        logger.info("アーカイブ処理を開始: {}", before);
        
        // アーカイブ対象のログ数を確認
        long count = repository.countByCreatedAtBefore(before);
        if (count == 0) {
            logger.info("アーカイブ対象のログなし");
            return;
        }

        try {
            // TODO: アーカイブファイルへの出力処理を実装

            // 古いログを削除
            repository.deleteByCreatedAtBefore(before);
            logger.info("{}件のログをアーカイブしました", count);

        } catch (Exception e) {
            logger.error("アーカイブ処理でエラーが発生: {}", e.getMessage(), e);
            throw new RuntimeException("アーカイブ処理に失敗しました", e);
        }
    }

    @Override
    @Transactional
    public void migrateToDatabase() {
        logger.info("インメモリログの移行を開始");
        List<AuditLog> logs = memoryStorage.findAll();

        try {
            // バッチ処理でデータベースに保存
            List<AuditLogEntity> entities = logs.stream()
                .map(AuditLogEntity::fromRecord)
                .collect(Collectors.toList());

            repository.saveAll(entities);
            logger.info("{}件のログを移行しました", logs.size());

            // メモリストレージをクリア
            memoryStorage.clear();

        } catch (Exception e) {
            logger.error("データベース移行でエラーが発生: {}", e.getMessage(), e);
            throw new RuntimeException("データベース移行に失敗しました", e);
        }
    }

    @Override
    @Scheduled(fixedRate = 900000) // 15分ごと
    public void refreshCache() {
        logger.info("キャッシュの更新を開始");
        
        try {
            // メモリキャッシュをクリア
            memoryStorage.clear();

            // 最新のログをキャッシュに読み込み
            Page<AuditLogEntity> latest = repository.findAll(
                PageRequest.of(0, CACHE_SIZE)
            );

            latest.getContent().stream()
                .map(AuditLogEntity::toRecord)
                .forEach(memoryStorage::save);

            logger.info("{}件のログをキャッシュに読み込みました", latest.getNumberOfElements());

        } catch (Exception e) {
            logger.error("キャッシュ更新でエラーが発生: {}", e.getMessage(), e);
            throw new RuntimeException("キャッシュ更新に失敗しました", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> aggregateByEventType(LocalDateTime start, LocalDateTime end) {
        return repository.findByCreatedAtBetween(start, end).stream()
            .collect(Collectors.groupingBy(
                AuditLogEntity::getEventType,
                Collectors.counting()
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<AuditEvent.Severity, Long> countBySeverity(LocalDateTime start, LocalDateTime end) {
        return repository.findByCreatedAtBetween(start, end).stream()
            .collect(Collectors.groupingBy(
                AuditLogEntity::getSeverity,
                Collectors.counting()
            ));
    }
}