package com.zswy.shipsupply.standardlibrary.provision;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/standard-library/provision/categories")
public class ProvisionCategoryController {

    private final ProvisionCategoryService service;

    public ProvisionCategoryController(ProvisionCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProvisionCategoryResponse> listCategories(@RequestParam(required = false) String keyword) {
        return service.listCategories(keyword);
    }
}
