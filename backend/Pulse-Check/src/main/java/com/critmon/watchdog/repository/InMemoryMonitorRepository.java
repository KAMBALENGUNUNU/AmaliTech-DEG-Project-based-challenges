package com.critmon.watchdog.repository;

import com.critmon.watchdog.model.Monitor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for Monitor records.
 */
@Repository
public class InMemoryMonitorRepository {

    private final Map<String, Monitor> store = new ConcurrentHashMap<>();

    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    public void save(Monitor monitor) {
        store.put(monitor.getId(), monitor);
    }

    public Optional<Monitor> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Monitor> findAll() {
        return List.copyOf(store.values());
    }

    public void deleteById(String id) {
        store.remove(id);
    }
}