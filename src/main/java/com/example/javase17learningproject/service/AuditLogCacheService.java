package com.example.javase17learningproject.service;

import com.example.javase17learningproject.cache.AuditLogCacheManager;
import com.example.javase17learningproject.entity.AuditLogEntity;
import com.example.javase17learningproject.model.AuditLog;
import com.example.javase17learningproject.model.audit.AuditEvent.Severity;
import com.example.javase17learningproject.repository.AuditLogRepository;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 監査ログのキャッシュサービスクラス.
 */
@Service
public class AuditLogCacheService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogCacheService.class);

    private final Cache<Long, AuditLog> cache;
    private final AuditLogRepository repository;
    private final AuditLogCacheManager cacheManager;

    @Autowired
    public AuditLogCacheService(
            Cache<Long, AuditLog> auditLogCache,
            AuditLogRepository repository,
            AuditLogCacheManager cacheManager) {
        this.cache = auditLogCache;
        this.repository = repository;
        this.cacheManager = cacheManager;
    }

    /**
     * IDで監査ログを取得します.
     * キャッシュにない場合はデータベースから取得してキャッシュに格納します.
     *
     * @param id 監査ログID
     * @return 監査ログ（存在しない場合はEmpty）
     */
    @Transactional(readOnly = true)
    public Optional<AuditLog> findById(Long id) {
        AuditLog cached = cache.getIfPresent(id);
        if (cached != null) {
            logger.debug("Cache hit for ID: {}", id);
            return Optional.of(cached);
        }

        logger.debug("Cache miss for ID: {}", id);
        return repository.findById(id)
                .map(entity -> {
                    AuditLog log = entity.toRecord();
                    cache.put(id, log);
                    return log;
                });
    }

    /**
     * 指定された期間とイベントタイプに一致する監査ログを取得します.
     *
     * @param startDate 開始日時
     * @param endDate 終了日時
     * @param eventType イベントタイプ
     * @param severity 重要度
     * @return 該当する監査ログのリスト
     */
    @Transactional(readOnly = true)
    public List<AuditLog> findByDateRangeAndType(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String eventType,
            Severity severity) {
        
        // リポジトリから検索
        List<AuditLogEntity> entities = repository.findBySeverityAndCreatedAtBetween(
            severity, startDate, endDate);

        // 結果をキャッシュに格納しながら変換
        return entities.stream()
                .map(entity -> {
                    AuditLog log = entity.toRecord();
                    cache.put(log.id(), log);
                    return log;
                })
                .filter(log -> eventType == null || log.eventType().equals(eventType))
                .collect(Collectors.toList());
    }

    /**
     * 監査ログを保存し、キャッシュを更新します.
     *
     * @param auditLog 保存する監査ログ
     * @return 保存された監査ログ
     */
    @Transactional
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = AuditLogEntity.fromRecord(auditLog);
        entity = repository.save(entity);
        
        AuditLog saved = entity.toRecord();
        cache.put(saved.id(), saved);
        
        return saved;
    }

    /**
     * 監査ログを削除し、キャッシュから削除します.
     *
     * @param id 削除する監査ログのID
     */
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        cacheManager.invalidate(id);
    }

    /**
     * キャッシュの整合性チェックを実行します.
     */
    public void verifyCache() {
        cacheManager.verifyCache();
    }

    /**
     * キャッシュを強制的に更新します.
     */
    public void refreshCache() {
        cacheManager.forceRefresh();
    }

    /**
     * 現在のキャッシュサイズを取得します.
     *
     * @return キャッシュに格納されているエントリ数
     */
    public long getCacheSize() {
        return cache.estimatedSize();
    }

    /**
     * 現在のキャッシュヒット率を取得します.
     *
     * @return キャッシュヒット率
     */
    public double getHitRate() {
        return cache.stats().hitRate();
    }
}