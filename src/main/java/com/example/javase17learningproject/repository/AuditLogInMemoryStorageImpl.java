package com.example.javase17learningproject.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.example.javase17learningproject.model.AuditLog;

/**
 * 監査ログのインメモリストレージの実装クラス。
 * スレッドセーフな実装を提供します。
 */
@Component
public class AuditLogInMemoryStorageImpl implements AuditLogInMemoryStorage {

    private final ConcurrentHashMap<Long, AuditLog> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public AuditLog save(AuditLog log) {
        Long id = log.id() != null ? log.id() : idGenerator.getAndIncrement();
        AuditLog savedLog = new AuditLog(
            id,
            log.eventType(),
            log.severity(),
            log.userId(),
            log.targetId(),
            log.description(),
            log.createdAt()
        );
        storage.put(id, savedLog);
        return savedLog;
    }

    @Override
    public List<AuditLog> findLatestLogs(int limit) {
        return storage.values().stream()
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
            .limit(limit)
            .toList();
    }

    @Override
    public List<AuditLog> findByEventType(String eventType) {
        if (eventType == null) {
            return Collections.emptyList();
        }
        return storage.values().stream()
            .filter(log -> eventType.equals(log.eventType()))
            .toList();
    }

    @Override
    public Optional<AuditLog> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<AuditLog> findAll() {
        return new ArrayList<>(storage.values());
    }
}