package com.zswy.shipsupply.shop.quality.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zswy.shipsupply.shop.quality.application.ShopSkuQualitySelectionService;

@RestController
@RequestMapping("/api/shop/suppliers/{companyId}")
public class ShopSkuQualitySelectionController {

    private final ShopSkuQualitySelectionService service;

    public ShopSkuQualitySelectionController(ShopSkuQualitySelectionService service) {
        this.service = service;
    }

    @GetMapping("/quality-selections")
    public QualitySelectionListResponse list(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long companyId
    ) {
        return service.list(authorizationHeader, companyId);
    }

    @PutMapping("/skus/{skuId}/quality-selection")
    public QualitySelectionResponse save(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long companyId,
        @PathVariable long skuId,
        @RequestBody QualitySelectionSaveRequest request
    ) {
        return service.save(authorizationHeader, companyId, skuId, request);
    }
}
