package com.zswy.shipsupply.standardlibrary.item;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ImpaItemService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final ImpaItemRepository impaItemRepository;

    public ImpaItemService(ImpaItemRepository impaItemRepository) {
        this.impaItemRepository = impaItemRepository;
    }

    public List<ImpaItemResponse> listItems(String categoryCode, String segmentCode, String keyword, int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        String normalizedKeyword = normalize(keyword);
        return impaItemRepository.findItems(categoryCode, segmentCode, normalizedKeyword, safeLimit);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
