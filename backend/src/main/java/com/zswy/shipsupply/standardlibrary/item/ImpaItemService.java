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

    public ImpaItemPageResponse listItemPage(
        String categoryCode,
        String segmentCode,
        String keyword,
        int page,
        int pageSize
    ) {
        int safePageSize = pageSize <= 0 ? DEFAULT_LIMIT : Math.min(pageSize, MAX_LIMIT);
        String normalizedKeyword = normalize(keyword);
        long total = impaItemRepository.countItems(categoryCode, segmentCode, normalizedKeyword);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safePageSize);
        int requestedPage = Math.max(page, 1);
        int safePage = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);
        int offset = (safePage - 1) * safePageSize;
        List<ImpaItemResponse> items = total == 0
            ? List.of()
            : impaItemRepository.findItems(categoryCode, segmentCode, normalizedKeyword, safePageSize, offset);
        return new ImpaItemPageResponse(items, total, safePage, safePageSize, totalPages);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
