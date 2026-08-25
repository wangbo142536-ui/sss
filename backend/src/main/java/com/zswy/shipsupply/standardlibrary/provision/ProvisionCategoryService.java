package com.zswy.shipsupply.standardlibrary.provision;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ProvisionCategoryService {

    private final ProvisionCategoryRepository repository;

    public ProvisionCategoryService(ProvisionCategoryRepository repository) {
        this.repository = repository;
    }

    public List<ProvisionCategoryResponse> listCategories(String keyword) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (normalized != null && normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }
        return repository.findEnabledCategories(normalized);
    }
}
