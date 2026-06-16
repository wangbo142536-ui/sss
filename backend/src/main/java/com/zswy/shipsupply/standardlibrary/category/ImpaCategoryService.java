package com.zswy.shipsupply.standardlibrary.category;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImpaCategoryService {

    private final ImpaCategoryRepository impaCategoryRepository;
    private final int categoryTreeMaxLevel;

    public ImpaCategoryService(
        ImpaCategoryRepository impaCategoryRepository,
        @Value("${ship-supply.standard-library.category-tree-max-level:2}") int categoryTreeMaxLevel
    ) {
        this.impaCategoryRepository = impaCategoryRepository;
        this.categoryTreeMaxLevel = categoryTreeMaxLevel;
    }

    public List<ImpaCategoryResponse> listCategoryTree() {
        List<ImpaCategoryRow> rows = impaCategoryRepository.findEnabledCategories().stream()
            .filter(row -> row.level() <= categoryTreeMaxLevel)
            .toList();
        Map<String, MutableCategory> byCode = new LinkedHashMap<>();
        List<MutableCategory> roots = new ArrayList<>();

        for (ImpaCategoryRow row : rows) {
            MutableCategory category = byCode.computeIfAbsent(row.categoryCode(), code -> new MutableCategory(row));
            category.row = row;
        }

        for (MutableCategory category : byCode.values()) {
            String parentCode = category.row.parentCode();
            if (parentCode == null || parentCode.isBlank()) {
                roots.add(category);
                continue;
            }
            MutableCategory parent = byCode.get(parentCode);
            if (parent != null) {
                parent.children.add(category);
            } else {
                roots.add(category);
            }
        }

        return roots.stream().map(MutableCategory::toResponse).toList();
    }

    private static final class MutableCategory {
        private ImpaCategoryRow row;
        private final List<MutableCategory> children = new ArrayList<>();

        private MutableCategory(ImpaCategoryRow row) {
            this.row = row;
        }

        private ImpaCategoryResponse toResponse() {
            return new ImpaCategoryResponse(
                row.categoryCode(),
                row.categoryNameCn(),
                row.categoryNameEn(),
                row.parentCode(),
                row.level(),
                row.sortOrder(),
                row.itemCount(),
                children.stream().map(MutableCategory::toResponse).toList()
            );
        }
    }
}
