package com.zswy.shipsupply.shop;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/profile")
    public ShopProfileResponse profile(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return shopService.profile(authorizationHeader);
    }

    @PutMapping("/profile")
    public ShopProfileResponse saveProfile(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody ShopProfileSaveRequest request
    ) {
        return shopService.saveProfile(authorizationHeader, request);
    }

    @GetMapping("/skus")
    public ShopSkuListResponse skus(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "productType", required = false) String productType,
        @RequestParam(value = "codeStatus", required = false) String codeStatus,
        @RequestParam(value = "shelfStatus", required = false) String shelfStatus,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return shopService.listSkus(authorizationHeader, productType, codeStatus, shelfStatus, keyword, page, size);
    }

    @PostMapping("/skus")
    public ShopSkuResponse createSku(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody ShopSkuRequest request
    ) {
        return shopService.createSku(authorizationHeader, request);
    }

    @PostMapping("/skus/batch-upsert")
    public ShopSkuBatchUpsertResponse batchUpsertSkus(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody ShopSkuBatchUpsertRequest request
    ) {
        return shopService.batchUpsertSkus(authorizationHeader, request);
    }

    @PutMapping("/skus/{skuId}")
    public ShopSkuResponse updateSku(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long skuId,
        @RequestBody ShopSkuRequest request
    ) {
        return shopService.updateSku(authorizationHeader, skuId, request);
    }

    @DeleteMapping("/skus/{skuId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSku(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long skuId
    ) {
        shopService.deleteSku(authorizationHeader, skuId);
    }

    @PatchMapping("/skus/{skuId}/shelf-status")
    public ShopSkuResponse updateShelfStatus(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long skuId,
        @RequestBody ShopShelfStatusRequest request
    ) {
        return shopService.updateShelfStatus(authorizationHeader, skuId, request);
    }

    @PatchMapping("/skus/shelf-status")
    public ShopShelfStatusBatchResponse updateShelfStatusBatch(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody ShopShelfStatusBatchRequest request
    ) {
        return shopService.updateShelfStatusBatch(authorizationHeader, request);
    }

    @PostMapping("/skus/import-preview")
    public ShopImportPreviewResponse importPreview(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam("file") MultipartFile file
    ) {
        return shopService.importPreview(authorizationHeader, file);
    }

    @PostMapping("/skus/import-confirm")
    public ShopImportConfirmResponse importConfirm(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody ShopImportConfirmRequest request
    ) {
        return shopService.importConfirm(authorizationHeader, request);
    }

    @PostMapping("/skus/{skuId}/exceptions/resolve")
    public ShopSkuResponse resolveException(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long skuId,
        @RequestBody ShopExceptionResolveRequest request
    ) {
        return shopService.resolveException(authorizationHeader, skuId, request);
    }
}
