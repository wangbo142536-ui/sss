package com.zswy.shipsupply.procurement.materials;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zswy.shipsupply.standardlibrary.item.ImpaItemRepository;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemResponse;

@Component
class MaterialImpaSearchIndex {

    private static final int MAX_STANDARD_ITEMS = 60_000;

    private final ImpaItemRepository repository;
    private final long ttlNanos;
    private volatile Snapshot cached;

    @Autowired
    MaterialImpaSearchIndex(
        ImpaItemRepository repository,
        @Value("${ship-supply.procurement.materials.impa-index-ttl-seconds:300}") long ttlSeconds
    ) {
        this.repository = repository;
        this.ttlNanos = Duration.ofSeconds(Math.max(1, ttlSeconds)).toNanos();
    }

    MaterialImpaSearchIndex(ImpaItemRepository repository) {
        this(repository, 300);
    }

    Snapshot snapshot() {
        Snapshot current = cached;
        long now = System.nanoTime();
        if (current != null && now - current.loadedAtNanos() < ttlNanos) {
            return current;
        }
        synchronized (this) {
            current = cached;
            now = System.nanoTime();
            if (current != null && now - current.loadedAtNanos() < ttlNanos) {
                return current;
            }
            List<IndexedItem> items = repository.findItems(null, null, null, MAX_STANDARD_ITEMS).stream()
                .map(item -> new IndexedItem(item, haystack(item)))
                .toList();
            Map<String, ImpaItemResponse> byCode = new LinkedHashMap<>();
            for (IndexedItem item : items) {
                byCode.putIfAbsent(normalizeCode(item.item().impaCode()), item.item());
            }
            current = new Snapshot(now, items, Map.copyOf(byCode));
            cached = current;
            return current;
        }
    }

    void invalidate() {
        cached = null;
    }

    private String haystack(ImpaItemResponse item) {
        return String.join(" ", value(item.impaCode()), value(item.nameCn()), value(item.nameEn()), value(item.specification()))
            .toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value(value).replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    record Snapshot(long loadedAtNanos, List<IndexedItem> items, Map<String, ImpaItemResponse> byCode) {
    }

    record IndexedItem(ImpaItemResponse item, String haystack) {
    }
}
