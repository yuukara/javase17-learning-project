package com.example.javase17learningproject.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.example.javase17learningproject.model.AuditLog;

/**
 * 監査ログのインメモリストレージ実装。
 * スレッドセーフな実装を提供します。
 */
@Repository
public class AuditLogInMemoryStorageImpl implements AuditLogInMemoryStorage {

    private final Map<Long, AuditLog> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public AuditLog save(AuditLog log) {
        if (log == null) {
            throw new IllegalArgumentException("log must not be null");
        }

        Long id = log.id() != null ? log.id() : idGenerator.incrementAndGet();
        AuditLog logWithId = new AuditLog(
            id,
            log.eventType(),
            log.severity(),
            log.userId(),
            log.targetId(),
            log.description(),
            log.createdAt()
        );

        storage.put(id, logWithId);
        return logWithId;
    }

    @Override
    public Optional<AuditLog> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<AuditLog> findByEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be null or blank");
        }

        return storage.values().stream()
            .filter(log -> eventType.equals(log.eventType()))
            .toList();
    }

    @Override
    public List<AuditLog> findLatestLogs(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }

        return storage.values().stream()
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
            .limit(limit)
            .toList();
    }

    @Override
    public List<AuditLog> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void clear() {
        storage.clear();
        idGenerator.set(0);
    }
}