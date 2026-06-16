package com.zswy.shipsupply.standardlibrary.category;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/standard-library/impa/categories")
public class ImpaCategoryController {

    private final ImpaCategoryService impaCategoryService;

    public ImpaCategoryController(ImpaCategoryService impaCategoryService) {
        this.impaCategoryService = impaCategoryService;
    }

    @GetMapping
    public List<ImpaCategoryResponse> listCategoryTree() {
        return impaCategoryService.listCategoryTree();
    }
}
