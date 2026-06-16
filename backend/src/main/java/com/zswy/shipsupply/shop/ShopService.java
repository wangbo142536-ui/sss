package com.zswy.shipsupply.shop;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.procurement.materials.MaterialParsedDocument;
import com.zswy.shipsupply.procurement.materials.MaterialQuoteRow;
import com.zswy.shipsupply.procurement.materials.XlsxMaterialQuoteParser;
import com.zswy.shipsupply.shop.ShopImportRecognitionService.ShopRecognitionResult;

@Service
public class ShopService {

    private static final Logger log = LoggerFactory.getLogger(ShopService.class);

    private static final String ACTIVE = "ACTIVE";
    private static final String PREVIEW_READY = "PREVIEW_READY";
    private static final String PENDING_EXCEPTION = "PENDING_EXCEPTION";
    private static final String CODE_MATCHED = "CODE_MATCHED";
    private static final String SPEC_MATCHED = "SPEC_MATCHED";

    private final CurrentUserService currentUserService;
    private final ShopRepository shopRepository;
    private final AuthRepository authRepository;
    private final XlsxMaterialQuoteParser xlsxParser;
    private final ShopImportRecognitionService recognitionService;

    public ShopService(CurrentUserService currentUserService, ShopRepository shopRepository, AuthRepository authRepository) {
        this(currentUserService, shopRepository, authRepository, null, null);
    }

    @Autowired
    public ShopService(
        CurrentUserService currentUserService,
        ShopRepository shopRepository,
        AuthRepository authRepository,
        XlsxMaterialQuoteParser xlsxParser,
        ShopImportRecognitionService recognitionService
    ) {
        this.currentUserService = currentUserService;
        this.shopRepository = shopRepository;
        this.authRepository = authRepository;
        this.xlsxParser = xlsxParser;
        this.recognitionService = recognitionService;
    }

    public ShopProfileResponse profile(String authorizationHeader) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return shopRepository.findProfile(currentUser.companyId())
            .orElseGet(() -> defaultProfile(currentUser.companyId()));
    }

    @Transactional
    public ShopProfileResponse saveProfile(String authorizationHeader, ShopProfileSaveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        ShopProfileSaveRequest safeRequest = validatedProfile(request);
        return shopRepository.saveProfile(currentUser.companyId(), currentUser.userId(), safeRequest);
    }

    public ShopSkuListResponse listSkus(
        String authorizationHeader,
        String productType,
        String codeStatus,
        String shelfStatus,
        String keyword,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return shopRepository.listSkus(
            currentUser.companyId(),
            optional(productType),
            optional(codeStatus),
            optional(shelfStatus),
            optional(keyword),
            page <= 0 ? 1 : page,
            size <= 0 ? 20 : Math.min(size, 100)
        );
    }

    @Transactional
    public ShopSkuResponse createSku(String authorizationHeader, ShopSkuRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        long shopId = shopRepository.ensureShop(currentUser.companyId(), currentUser.userId());
        try {
            return shopRepository.saveSku(currentUser.companyId(), shopId, currentUser.userId(), null, validatedSku(request));
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU_SUPPLIER_CODE_EXISTS: 供货商SKU编码已存在", ex);
        }
    }

    @Transactional
    public ShopSkuBatchUpsertResponse batchUpsertSkus(String authorizationHeader, ShopSkuBatchUpsertRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        List<ShopSkuUpsertItem> rows = list(request == null ? null : request.items());
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU_ITEMS_REQUIRED: 商品保存列表不能为空");
        }
        long shopId = shopRepository.ensureShop(currentUser.companyId(), currentUser.userId());
        ensureNoDuplicateSupplierSkuCodes(rows);
        List<ShopSkuResponse> saved = new ArrayList<>();
        List<ShopSkuBatchUpsertRowResult> rowResults = new ArrayList<>();
        int insertedCount = 0;
        int updatedCount = 0;
        for (ShopSkuUpsertItem row : rows) {
            if (row == null) {
                continue;
            }
            ShopSkuRequest skuRequest = mergedBatchSku(row, request.importBatchId());
            ShopSkuRequest safeSku;
            try {
                safeSku = validatedSku(skuRequest);
            } catch (ResponseStatusException ex) {
                throw batchRowError(row, skuRequest, "SKU_ROW_INVALID", ex.getReason(), ex);
            }
            Long skuId = row.skuId();
            ShopSkuResponse existingSku = null;
            String supplierSkuCode = optional(safeSku.supplierSkuCode());
            if (supplierSkuCode == null) {
                throw batchRowError(row, safeSku, "SUPPLIER_SKU_CODE_REQUIRED", "供货商SKU编码不能为空", null);
            }
            if (skuId != null) {
                existingSku = requireExistingSku(currentUser.companyId(), skuId);
            } else {
                existingSku = shopRepository.findSkuBySupplierSkuCode(currentUser.companyId(), shopId, supplierSkuCode)
                    .orElse(null);
                if (existingSku != null) {
                    skuId = existingSku.skuId();
                }
            }
            String status = skuId == null ? "INSERTED" : "UPDATED";
            safeSku = preservingExistingFieldsForUpdate(safeSku, existingSku);
            try {
                ShopSkuResponse savedSku = shopRepository.saveSku(
                    currentUser.companyId(),
                    shopId,
                    currentUser.userId(),
                    skuId,
                    safeSku
                );
                if (savedSku == null) {
                    throw batchRowError(row, safeSku, "SKU_NOT_FOUND", "商品不存在或无权限", null);
                }
                saved.add(savedSku);
                rowResults.add(new ShopSkuBatchUpsertRowResult(
                    safeSku.importRowNumber(),
                    safeSku.supplierSkuCode(),
                    savedSku.skuId(),
                    status,
                    null,
                    status.equals("INSERTED") ? "已新增" : "已更新"
                ));
                if ("INSERTED".equals(status)) {
                    insertedCount++;
                } else {
                    updatedCount++;
                }
            } catch (DuplicateKeyException ex) {
                throw batchRowError(HttpStatus.CONFLICT, row, safeSku, "SKU_SUPPLIER_CODE_EXISTS", "供货商SKU编码已存在", ex);
            }
        }
        return new ShopSkuBatchUpsertResponse(
            rows.size(),
            saved.size(),
            insertedCount,
            updatedCount,
            0,
            rowResults,
            saved
        );
    }

    @Transactional
    public ShopSkuResponse updateSku(String authorizationHeader, Long skuId, ShopSkuRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireSku(currentUser.companyId(), skuId);
        long shopId = shopRepository.ensureShop(currentUser.companyId(), currentUser.userId());
        ShopSkuResponse saved;
        try {
            saved = shopRepository.saveSku(currentUser.companyId(), shopId, currentUser.userId(), skuId, validatedSku(request));
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU_SUPPLIER_CODE_EXISTS: 供货商SKU编码已存在", ex);
        }
        if (saved == null) {
            throw notFound();
        }
        return saved;
    }

    @Transactional
    public void deleteSku(String authorizationHeader, Long skuId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!shopRepository.deleteSku(currentUser.companyId(), skuId)) {
            throw notFound();
        }
    }

    @Transactional
    public ShopSkuResponse updateShelfStatus(String authorizationHeader, Long skuId, ShopShelfStatusRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        String shelfStatus = validatedShelfStatus(request == null ? null : request.shelfStatus());
        return shopRepository.updateShelfStatus(currentUser.companyId(), skuId, shelfStatus)
            .orElseThrow(ShopService::notFound);
    }

    @Transactional
    public ShopShelfStatusBatchResponse updateShelfStatusBatch(String authorizationHeader, ShopShelfStatusBatchRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        String shelfStatus = validatedShelfStatus(request == null ? null : request.shelfStatus());
        List<ShopSkuResponse> items = shopRepository.updateShelfStatusBatch(
            currentUser.companyId(),
            list(request == null ? null : request.skuIds()),
            shelfStatus
        );
        return new ShopShelfStatusBatchResponse(shelfStatus, items.size(), items);
    }

    @Transactional
    public ShopImportPreviewResponse importPreview(String authorizationHeader, MultipartFile file) {
        long startedAt = System.nanoTime();
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMPORT_FILE_REQUIRED: 导入文件必填");
        }
        long authDoneAt = System.nanoTime();
        long shopId = shopRepository.ensureShop(currentUser.companyId(), currentUser.userId());
        long batchId = shopRepository.createBatch(currentUser.companyId(), shopId, currentUser.userId(), file.getOriginalFilename());
        long batchDoneAt = System.nanoTime();
        List<ShopImportPreviewItem> items = parsePreviewItems(file, batchId, currentUser.companyId(), shopId, currentUser.userId());
        long parseDoneAt = System.nanoTime();
        shopRepository.savePreviewRows(batchId, currentUser.companyId(), shopId, items);
        long saveDoneAt = System.nanoTime();
        int exceptionCount = (int) items.stream().filter(item -> PENDING_EXCEPTION.equals(item.codeStatus())).count();
        log.info(
            "SHOP_IMPORT_PREVIEW_TIMING batchId={} rows={} authMs={} batchMs={} parseAndRecognizeMs={} saveMs={} totalMs={}",
            batchId,
            items.size(),
            elapsedMs(startedAt, authDoneAt),
            elapsedMs(authDoneAt, batchDoneAt),
            elapsedMs(batchDoneAt, parseDoneAt),
            elapsedMs(parseDoneAt, saveDoneAt),
            elapsedMs(startedAt, saveDoneAt)
        );
        return new ShopImportPreviewResponse(
            batchId,
            exceptionCount > 0 ? "PENDING_EXCEPTION" : PREVIEW_READY,
            items.size(),
            items.size() - exceptionCount,
            exceptionCount,
            items
        );
    }

    @Transactional
    public ShopImportConfirmResponse importConfirm(String authorizationHeader, ShopImportConfirmRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        Long batchId = request == null ? null : request.batchId();
        if (batchId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMPORT_BATCH_REQUIRED: 导入批次必填");
        }
        shopRepository.batchCompany(currentUser.companyId(), batchId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IMPORT_BATCH_NOT_FOUND: 导入批次不存在"));
        shopRepository.applyConfirmSelections(currentUser.companyId(), batchId, list(request.items()));
        List<ShopSkuResponse> items = shopRepository.confirmBatch(currentUser.companyId(), currentUser.userId(), batchId);
        int exceptionCount = (int) items.stream().filter(item -> PENDING_EXCEPTION.equals(item.codeStatus())).count();
        return new ShopImportConfirmResponse(batchId, "CONFIRMED", items.size(), items.size() - exceptionCount, exceptionCount, items);
    }

    @Transactional
    public ShopSkuResponse resolveException(String authorizationHeader, Long skuId, ShopExceptionResolveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireSku(currentUser.companyId(), skuId);
        String action = required(request == null ? null : request.actionType(), "EXCEPTION_ACTION_REQUIRED").toUpperCase(Locale.ROOT);
        if (!List.of("SELECT_CANDIDATE", "MANUAL_CODE", "IGNORE", "REOPEN").contains(action)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_EXCEPTION_ACTION: 异常处理动作不支持");
        }
        return shopRepository.resolveException(currentUser.companyId(), currentUser.userId(), skuId, request);
    }

    private ShopProfileResponse defaultProfile(long companyId) {
        String companyName = shopRepository.companyName(companyId);
        return new ShopProfileResponse(
            null,
            companyId,
            companyName,
            companyName,
            null,
            null,
            null,
            List.of(),
            List.of(),
            List.of(),
            null,
            null,
            null,
            ACTIVE,
            null,
            null,
            false
        );
    }

    private ShopProfileSaveRequest validatedProfile(ShopProfileSaveRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SHOP_PROFILE_REQUIRED: 店铺资料必填");
        }
        String shopName = required(request.shopName(), "SHOP_NAME_REQUIRED: 店铺名称必填");
        return new ShopProfileSaveRequest(
            shopName,
            optional(request.logoFileId()),
            optional(request.logoUrl()),
            optional(request.description()),
            list(request.mainCategories()),
            list(request.servicePorts()),
            list(request.deliveryAreas()),
            optional(request.contactName()),
            optional(request.contactPhone()),
            optional(request.contactEmail()),
            optional(request.status()) == null ? ACTIVE : optional(request.status())
        );
    }

    private ShopSkuRequest validatedSku(ShopSkuRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU_REQUIRED: SKU资料必填");
        }
        String productName = required(request.productName(), "PRODUCT_NAME_REQUIRED: 商品名称必填");
        List<ShopSkuImageRequest> images = normalizedImages(request);
        return new ShopSkuRequest(
            optional(request.productType()) == null ? "MATERIAL" : optional(request.productType()),
            optional(request.categoryCode()),
            optional(request.categoryName()),
            optional(request.platformCode()),
            optional(request.impaCode()),
            optional(request.supplierSkuCode()),
            productName,
            list(request.specifications()),
            request.stockQty(),
            optional(request.stockUnit()),
            request.leadTimeDays(),
            optional(request.deliveryArea()),
            list(request.servicePorts()),
            request.monthlySales() == null ? 0 : request.monthlySales(),
            request.unitPrice(),
            normalizeCurrency(optional(request.currency())),
            optional(request.brand()),
            optional(request.unit()),
            optional(request.packageSpec()),
            optional(request.barcode()),
            optional(request.shelfStatus()) == null ? "OFF_SHELF" : validatedShelfStatus(request.shelfStatus()),
            optional(request.codeStatus()) == null ? PENDING_EXCEPTION : optional(request.codeStatus()),
            optional(request.exceptionReason()),
            images,
            optional(request.imageFileId()),
            optional(request.imageUrl()),
            optional(request.thumbnailUrl()),
            request.importBatchId(),
            request.importRowNumber(),
            request.rawRow()
        );
    }

    private ShopSkuRequest mergedBatchSku(ShopSkuUpsertItem row, Long requestImportBatchId) {
        if (row == null || row.sku() == null) {
            return null;
        }
        ShopSkuRequest sku = row.sku();
        Long importBatchId = row.importBatchId() != null
            ? row.importBatchId()
            : (sku.importBatchId() != null ? sku.importBatchId() : requestImportBatchId);
        Integer importRowNumber = row.importRowNumber() != null ? row.importRowNumber() : sku.importRowNumber();
        String supplierSkuCode = optional(row.supplierSkuCode()) != null
            ? optional(row.supplierSkuCode())
            : optional(sku.supplierSkuCode());
        return new ShopSkuRequest(
            sku.productType(),
            sku.categoryCode(),
            sku.categoryName(),
            sku.platformCode(),
            sku.impaCode(),
            supplierSkuCode,
            sku.productName(),
            sku.specifications(),
            sku.stockQty(),
            sku.stockUnit(),
            sku.leadTimeDays(),
            sku.deliveryArea(),
            sku.servicePorts(),
            sku.monthlySales(),
            sku.unitPrice(),
            sku.currency(),
            sku.brand(),
            sku.unit(),
            sku.packageSpec(),
            sku.barcode(),
            sku.shelfStatus(),
            sku.codeStatus(),
            sku.exceptionReason(),
            sku.images(),
            sku.imageFileId(),
            sku.imageUrl(),
            sku.thumbnailUrl(),
            importBatchId,
            importRowNumber,
            sku.rawRow()
        );
    }

    private void ensureNoDuplicateSupplierSkuCodes(List<ShopSkuUpsertItem> rows) {
        Map<String, ShopSkuUpsertItem> seen = new HashMap<>();
        for (ShopSkuUpsertItem row : rows) {
            String supplierSkuCode = supplierSkuCode(row);
            if (supplierSkuCode == null) {
                continue;
            }
            String key = supplierKey(supplierSkuCode);
            ShopSkuUpsertItem first = seen.putIfAbsent(key, row);
            if (first != null) {
                throw batchRowError(row, row.sku(), "DUPLICATE_SUPPLIER_SKU_CODE", "同一批次存在重复供货商SKU编码", null);
            }
        }
    }

    private String supplierSkuCode(ShopSkuUpsertItem row) {
        if (row == null) {
            return null;
        }
        String outer = optional(row.supplierSkuCode());
        if (outer != null) {
            return outer;
        }
        return row.sku() == null ? null : optional(row.sku().supplierSkuCode());
    }

    private String supplierKey(String supplierSkuCode) {
        return supplierSkuCode == null ? null : supplierSkuCode.trim().toUpperCase(Locale.ROOT);
    }

    private ShopSkuRequest preservingExistingFieldsForUpdate(ShopSkuRequest request, ShopSkuResponse existingSku) {
        if (existingSku == null) {
            return request;
        }
        List<ShopSkuImageRequest> images = list(request.images()).isEmpty()
            ? imageRequests(existingSku.images())
            : request.images();
        return new ShopSkuRequest(
            request.productType(),
            request.categoryCode(),
            request.categoryName(),
            request.platformCode(),
            request.impaCode(),
            request.supplierSkuCode(),
            request.productName(),
            request.specifications(),
            request.stockQty(),
            request.stockUnit(),
            request.leadTimeDays(),
            request.deliveryArea(),
            request.servicePorts(),
            existingSku.monthlySales(),
            request.unitPrice(),
            request.currency(),
            request.brand(),
            request.unit(),
            request.packageSpec(),
            request.barcode(),
            existingSku.shelfStatus(),
            request.codeStatus(),
            request.exceptionReason(),
            images,
            request.imageFileId(),
            request.imageUrl(),
            request.thumbnailUrl(),
            request.importBatchId(),
            request.importRowNumber(),
            request.rawRow()
        );
    }

    private List<ShopSkuImageRequest> imageRequests(List<ShopSkuImageResponse> images) {
        return list(images).stream()
            .map(image -> new ShopSkuImageRequest(
                image.fileId(),
                image.imageUrl(),
                image.thumbnailUrl(),
                image.primary(),
                image.sortOrder()
            ))
            .toList();
    }

    private List<ShopSkuImageRequest> normalizedImages(ShopSkuRequest request) {
        List<ShopSkuImageRequest> images = new ArrayList<>(list(request.images()));
        boolean hasTopLevelImage = optional(request.imageFileId()) != null
            || optional(request.imageUrl()) != null
            || optional(request.thumbnailUrl()) != null;
        if (!hasTopLevelImage) {
            return images;
        }
        boolean alreadyIncluded = images.stream().anyMatch(image ->
            Objects.equals(optional(image.fileId()), optional(request.imageFileId()))
                && Objects.equals(optional(image.imageUrl()), optional(request.imageUrl()))
        );
        if (!alreadyIncluded) {
            images.add(new ShopSkuImageRequest(
                optional(request.imageFileId()),
                optional(request.imageUrl()),
                optional(request.thumbnailUrl()),
                images.stream().noneMatch(image -> Boolean.TRUE.equals(image.primary())),
                images.size()
            ));
        }
        return normalizePrimaryImage(images);
    }

    private List<ShopSkuImageRequest> normalizePrimaryImage(List<ShopSkuImageRequest> images) {
        if (images.isEmpty() || images.stream().anyMatch(image -> Boolean.TRUE.equals(image.primary()))) {
            return images;
        }
        List<ShopSkuImageRequest> normalized = new ArrayList<>();
        for (int index = 0; index < images.size(); index++) {
            ShopSkuImageRequest image = images.get(index);
            normalized.add(new ShopSkuImageRequest(
                image.fileId(),
                image.imageUrl(),
                image.thumbnailUrl(),
                index == 0,
                image.sortOrder() == null ? index : image.sortOrder()
            ));
        }
        return normalized;
    }

    private ResponseStatusException batchRowError(
        ShopSkuUpsertItem row,
        ShopSkuRequest sku,
        String errorCode,
        String message,
        Throwable cause
    ) {
        return batchRowError(HttpStatus.BAD_REQUEST, row, sku, errorCode, message, cause);
    }

    private ResponseStatusException batchRowError(
        HttpStatus status,
        ShopSkuUpsertItem row,
        ShopSkuRequest sku,
        String errorCode,
        String message,
        Throwable cause
    ) {
        Integer importRowNumber = row != null && row.importRowNumber() != null
            ? row.importRowNumber()
            : (sku == null ? null : sku.importRowNumber());
        String supplierSkuCode = row != null && optional(row.supplierSkuCode()) != null
            ? optional(row.supplierSkuCode())
            : (sku == null ? null : sku.supplierSkuCode());
        String reason = "%s: row=%s supplierSkuCode=%s message=%s".formatted(
            errorCode,
            importRowNumber == null ? "" : importRowNumber,
            supplierSkuCode == null ? "" : supplierSkuCode,
            message == null ? "" : message
        );
        return new ResponseStatusException(status, reason, cause);
    }

    private List<ShopImportPreviewItem> parsePreviewItems(MultipartFile file, long batchId, long companyId, long shopId, long userId) {
        Path temp = null;
        long startedAt = System.nanoTime();
        try {
            temp = Files.createTempFile("shop-sku-import-", ".xlsx");
            file.transferTo(temp);
            long transferDoneAt = System.nanoTime();
            if (xlsxParser == null) {
                return List.of();
            }
            MaterialParsedDocument document = xlsxParser.parse(temp);
            long excelDoneAt = System.nanoTime();
            List<ShopImportPreviewItem> items = new ArrayList<>();
            for (MaterialQuoteRow row : document.rows()) {
                items.add(previewItem(row, batchId, companyId, shopId, userId, temp));
            }
            items = withPreviewActions(items, companyId, shopId);
            long previewDoneAt = System.nanoTime();
            log.info(
                "SHOP_IMPORT_PARSE_TIMING batchId={} fileName={} rows={} transferMs={} excelParseMs={} recognizeAndImageMs={} totalMs={}",
                batchId,
                file.getOriginalFilename(),
                items.size(),
                elapsedMs(startedAt, transferDoneAt),
                elapsedMs(transferDoneAt, excelDoneAt),
                elapsedMs(excelDoneAt, previewDoneAt),
                elapsedMs(startedAt, previewDoneAt)
            );
            return items;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMPORT_PARSE_FAILED: 导入文件解析失败", ex);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private long elapsedMs(long from, long to) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(to - from);
    }

    private ShopImportPreviewItem previewItem(MaterialQuoteRow row, long batchId, long companyId, long shopId, long userId, Path sourceFile) {
        String rawCode = optional(row.impaCode());
        String supplierSkuCode = optional(row.supplierItemNo());
        String rawName = truncate(first(optional(row.rawNameSpec()), optional(row.description()), supplierSkuCode, "?????"), 1000);
        String spec = truncate(first(optional(row.sizeModel()), optional(row.description())), 1000);
        BigDecimal unitPrice = decimal(priceValue(row));
        String currency = detectCurrency(row.rawColumns());
        BigDecimal stockQty = decimal(row.rawColumns().get("STOCK"));
        String packageSpec = optional(row.rawColumns().get("Packing"));
        String barcode = optional(row.rawColumns().get("Barcode"));
        ImageImportResult image = importEmbeddedImage(sourceFile, row, userId, batchId);
        ShopRecognitionResult recognition = recognition(rawName, spec, packageSpec);
        String productName = truncate(first(optional(recognition.cleanName()), rawName), 500);
        ShopImportRecommendation selected = selectedRecommendation(recognition);
        String impaCode = selected == null ? null : selected.impaCode();
        String categoryCode = selected == null ? null : selected.categoryCode();
        String categoryName = selected == null ? null : selected.categoryName();
        String codeStatus = codeStatus(recognition, rawCode);
        String exceptionReason = PENDING_EXCEPTION.equals(codeStatus) ? exceptionReason(recognition) : null;
        List<ShopSkuAttributeResponse> specifications = previewAttributes(recognition.parsedAttributes(), spec, packageSpec, barcode, row.rawColumns());
        String attributeSummary = attributeSummary(specifications);
        return new ShopImportPreviewItem(
            null,
            null,
            shopId,
            companyId,
            "MATERIAL",
            categoryCode,
            categoryName,
            impaCode,
            impaCode,
            supplierSkuCode,
            productName,
            specifications,
            attributeSummary,
            stockQty,
            null,
            null,
            null,
            image == null ? null : image.url(),
            image == null ? null : image.thumbnailUrl(),
            image == null ? null : image.fileId(),
            0,
            unitPrice,
            currency,
            currencySymbol(currency),
            null,
            null,
            packageSpec,
            barcode,
            "OFF_SHELF",
            codeStatus,
            exceptionReason,
            batchId,
            row.sourceRowNo(),
            row.sourceRowNo(),
            rawName,
            spec,
            recognition.cleanName(),
            recognition.parsedAttributes(),
            recognition.logicRecommendation(),
            recognition.categoryCandidates(),
            row.rawColumns(),
            null,
            "INSERT",
            null
        );
    }

    private List<ShopImportPreviewItem> withPreviewActions(List<ShopImportPreviewItem> items, long companyId, long shopId) {
        Map<String, Integer> counts = new HashMap<>();
        for (ShopImportPreviewItem item : items) {
            String supplierSkuCode = optional(item.supplierSkuCode());
            if (supplierSkuCode == null) {
                continue;
            }
            counts.merge(supplierKey(supplierSkuCode), 1, Integer::sum);
        }
        List<ShopImportPreviewItem> marked = new ArrayList<>();
        for (ShopImportPreviewItem item : items) {
            String supplierSkuCode = optional(item.supplierSkuCode());
            if (supplierSkuCode == null) {
                marked.add(withPreviewAction(item, null, "BLOCKED", null, PENDING_EXCEPTION, "SUPPLIER_SKU_CODE_REQUIRED"));
                continue;
            }
            if (counts.getOrDefault(supplierKey(supplierSkuCode), 0) > 1) {
                ShopSkuResponse existing = shopRepository.findSkuBySupplierSkuCode(companyId, shopId, supplierSkuCode).orElse(null);
                marked.add(withPreviewAction(
                    item,
                    existing == null ? null : existing.skuId(),
                    "DUPLICATE",
                    existing,
                    PENDING_EXCEPTION,
                    "DUPLICATE_SUPPLIER_SKU_CODE"
                ));
                continue;
            }
            ShopSkuResponse existing = shopRepository.findSkuBySupplierSkuCode(companyId, shopId, supplierSkuCode).orElse(null);
            marked.add(withPreviewAction(
                item,
                existing == null ? null : existing.skuId(),
                existing == null ? "INSERT" : "UPDATE",
                existing,
                item.codeStatus(),
                item.exceptionReason()
            ));
        }
        return marked;
    }

    private ShopImportPreviewItem withPreviewAction(
        ShopImportPreviewItem item,
        Long existingSkuId,
        String previewAction,
        ShopSkuResponse existingSnapshot,
        String codeStatus,
        String exceptionReason
    ) {
        return new ShopImportPreviewItem(
            item.previewRowId(),
            item.skuId(),
            item.shopId(),
            item.companyId(),
            item.productType(),
            item.categoryCode(),
            item.categoryName(),
            item.platformCode(),
            item.impaCode(),
            item.supplierSkuCode(),
            item.productName(),
            item.specifications(),
            item.attributeSummary(),
            item.stockQty(),
            item.stockUnit(),
            item.leadTimeDays(),
            item.deliveryArea(),
            item.imageUrl(),
            item.thumbnailUrl(),
            item.imageFileId(),
            item.monthlySales(),
            item.unitPrice(),
            item.currency(),
            item.currencySymbol(),
            item.brand(),
            item.unit(),
            item.packageSpec(),
            item.barcode(),
            item.shelfStatus(),
            codeStatus,
            exceptionReason,
            item.importBatchId(),
            item.importRowNumber(),
            item.rowNo(),
            item.rawName(),
            item.rawSpec(),
            item.cleanName(),
            item.parsedAttributes(),
            item.logicRecommendation(),
            item.categoryCandidates(),
            item.rawColumns(),
            existingSkuId,
            previewAction,
            existingSnapshot
        );
    }

    private ShopRecognitionResult recognition(String rawName, String rawSpec, String packing) {
        if (recognitionService == null) {
            return new ShopRecognitionResult(
                null,
                List.of(),
                new ShopImportRecommendation(false, "NO_MATCH", null, null, null, null, null, null, null, "RECOGNITION_SERVICE_UNAVAILABLE", "NONE"),
                List.of(),
                true,
                "UNMATCHED"
            );
        }
        return recognitionService.recognize(rawName, rawSpec, packing);
    }

    private ShopImportRecommendation selectedRecommendation(ShopRecognitionResult recognition) {
        if (Boolean.TRUE.equals(recognition.logicRecommendation().available())) {
            return recognition.logicRecommendation();
        }
        return null;
    }

    private String codeStatus(ShopRecognitionResult recognition, String rawCode) {
        if (isStandardImpa(rawCode)) {
            return CODE_MATCHED;
        }
        if (hasRecommendedCode(recognition.logicRecommendation())) {
            return SPEC_MATCHED;
        }
        return PENDING_EXCEPTION;
    }

    private String exceptionReason(ShopRecognitionResult recognition) {
        return switch (recognition.matchDecision()) {
            case "LOGIC_ONLY" -> "LOGIC_RECOMMENDATION_REVIEW_REQUIRED";
            case "CATEGORY_ONLY" -> "CATEGORY_ONLY_IMPA_REQUIRED";
            case "UNMATCHED" -> "LOGIC_UNMATCHED";
            default -> "IMPORT_REVIEW_REQUIRED";
        };
    }

    private boolean hasRecommendedCode(ShopImportRecommendation recommendation) {
        return recommendation != null
            && Boolean.TRUE.equals(recommendation.available())
            && recommendation.impaCode() != null
            && !recommendation.impaCode().isBlank();
    }

    private ImageImportResult importEmbeddedImage(Path sourceFile, MaterialQuoteRow row, long userId, long batchId) {
        String mediaPath = first(optional(row.imageMediaPath()), optional(row.rawColumns().get("_imageMediaPath")));
        if (mediaPath == null) {
            return null;
        }
        try (ZipFile zipFile = new ZipFile(sourceFile.toFile())) {
            ZipEntry entry = zipFile.getEntry(mediaPath);
            if (entry == null) {
                return null;
            }
            Path uploadDir = Path.of("uploads", "shop-sku-images").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            String originalName = "batch-" + batchId + "-row-" + row.sourceRowNo() + "-" + fileName(mediaPath);
            Path target = uploadDir.resolve(UUID.randomUUID() + "-" + originalName.replaceAll("[\\\\/:*?\"<>|]", "_"));
            try (var input = zipFile.getInputStream(entry)) {
                Files.copy(input, target);
            }
            String contentType = first(optional(row.imageContentType()), optional(row.rawColumns().get("_imageContentType")), "application/octet-stream");
            String fileId = authRepository.insertFile(userId, originalName, target.toString(), contentType, Files.size(target));
            String url = "/api/files/" + fileId;
            return new ImageImportResult(fileId, url, url);
        } catch (Exception ex) {
            return null;
        }
    }

    private String fileName(String mediaPath) {
        int index = mediaPath.lastIndexOf('/');
        return index < 0 ? mediaPath : mediaPath.substring(index + 1);
    }

    private List<ShopSkuAttributeResponse> previewAttributes(
        List<ShopParsedAttribute> parsedAttributes,
        String rawSpec,
        String packageSpec,
        String barcode,
        Map<String, String> rawColumns
    ) {
        List<ShopSkuAttributeResponse> attributes = new ArrayList<>();
        int sort = 0;
        for (ShopParsedAttribute parsedAttribute : list(parsedAttributes)) {
            if (parsedAttribute == null || parsedAttribute.value() == null || parsedAttribute.value().isBlank()) {
                continue;
            }
            attributes.add(new ShopSkuAttributeResponse(
                null,
                parsedAttribute.key(),
                parsedAttribute.name(),
                parsedAttribute.value(),
                parsedAttribute.unit(),
                sort++,
                first(parsedAttribute.rawText(), parsedAttribute.value())
            ));
        }
        if (rawSpec != null && attributes.stream().noneMatch(attribute -> rawSpec.equals(attribute.value()))) {
            attributes.add(new ShopSkuAttributeResponse(null, "specification", "Specification", rawSpec, null, sort++, rawSpec));
        }
        if (packageSpec != null) {
            attributes.add(new ShopSkuAttributeResponse(null, "packing", "Packing", packageSpec, null, sort++, packageSpec));
        }
        sort = appendSupplierQuoteSpecs(attributes, rawColumns, sort);
        if (barcode != null) {
            attributes.add(new ShopSkuAttributeResponse(null, "barcode", "Barcode", barcode, null, sort, barcode));
        }
        return attributes;
    }

    private int appendSupplierQuoteSpecs(List<ShopSkuAttributeResponse> attributes, Map<String, String> rawColumns, int sort) {
        if (rawColumns == null || rawColumns.isEmpty()) {
            return sort;
        }
        sort = addRawColumnAttribute(attributes, rawColumns, sort, "pcsInnerBox", "Pcs/Inner box", null, "Pcs/Inner box");
        sort = addRawColumnAttribute(attributes, rawColumns, sort, "pcsCtns", "Pcs/Ctns", null, "Pcs/Ctns");
        sort = addRawColumnAttribute(attributes, rawColumns, sort, "lengthCm", "Length", "cm", "L/cm");
        sort = addRawColumnAttribute(attributes, rawColumns, sort, "widthCm", "Width", "cm", "W/cm");
        sort = addRawColumnAttribute(attributes, rawColumns, sort, "heightCm", "Height", "cm", "H/cm");
        sort = addRawColumnAttribute(attributes, rawColumns, sort, "volumeM3", "Volume", "m3", "N/m³", "N/m3", "N/m?");
        sort = addRawColumnAttribute(attributes, rawColumns, sort, "grossWeightKg", "Gross Weight", "kg", "GW/kgs", "GW/kg");
        sort = addRawColumnAttribute(attributes, rawColumns, sort, "netWeightKg", "Net Weight", "kg", "NW/kgs", "NW/kg");
        return sort;
    }

    private int addRawColumnAttribute(
        List<ShopSkuAttributeResponse> attributes,
        Map<String, String> rawColumns,
        int sort,
        String key,
        String name,
        String unit,
        String... columnNames
    ) {
        for (String columnName : columnNames) {
            String value = optional(rawColumns.get(columnName));
            if (value == null || hasAttribute(attributes, key, value)) {
                continue;
            }
            attributes.add(new ShopSkuAttributeResponse(null, key, name, value, unit, sort++, columnName + ": " + value));
            break;
        }
        return sort;
    }

    private boolean hasAttribute(List<ShopSkuAttributeResponse> attributes, String key, String value) {
        return attributes.stream()
            .anyMatch(attribute -> key.equals(attribute.key()) && value.equals(attribute.value()));
    }

    private String attributeSummary(List<ShopSkuAttributeResponse> attributes) {
        return attributes.stream()
            .map(attribute -> first(attribute.value(), attribute.rawText()))
            .filter(value -> value != null && !value.isBlank())
            .collect(java.util.stream.Collectors.joining(" / "));
    }

    private BigDecimal decimal(String value) {
        String normalized = optional(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replaceAll("[^0-9.\\-]", "");
        if (normalized.isBlank() || "-".equals(normalized) || ".".equals(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String priceValue(MaterialQuoteRow row) {
        String price = optional(row.price());
        if (price != null) {
            return price;
        }
        if (row.rawColumns() == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : row.rawColumns().entrySet()) {
            String header = entry.getKey() == null ? "" : entry.getKey().toUpperCase(Locale.ROOT);
            if (header.contains("PRICE") || header.contains("FOB") || header.contains("WAREHOUSE")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String detectCurrency(Map<String, String> rawColumns) {
        if (rawColumns == null || rawColumns.isEmpty()) {
            return "CNY";
        }
        boolean cnyHit = false;
        for (Map.Entry<String, String> entry : rawColumns.entrySet()) {
            String text = (first(entry.getKey(), "") + " " + first(entry.getValue(), "")).toUpperCase(Locale.ROOT);
            if (text.contains("USD") || text.contains("US DOLLAR") || text.contains("$") || text.contains("美元")) {
                return "USD";
            }
            if (text.contains("CNY") || text.contains("RMB") || text.contains("人民币") || text.contains("¥")) {
                cnyHit = true;
            }
        }
        return cnyHit ? "CNY" : "CNY";
    }

    private String normalizeCurrency(String currency) {
        if (currency == null) {
            return "CNY";
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if ("USD".equals(normalized) || "US DOLLAR".equals(normalized) || "$".equals(currency.trim()) || "美元".equals(currency.trim())) {
            return "USD";
        }
        if ("CNY".equals(normalized) || "RMB".equals(normalized) || "¥".equals(currency.trim()) || "人民币".equals(currency.trim())) {
            return "CNY";
        }
        return normalized;
    }

    private String currencySymbol(String currency) {
        return "USD".equals(normalizeCurrency(currency)) ? "$" : "\u00A5";
    }

    private boolean isStandardImpa(String value) {
        return value != null && value.matches("\\d{6}");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void requireSku(long companyId, Long skuId) {
        requireExistingSku(companyId, skuId);
    }

    private ShopSkuResponse requireExistingSku(long companyId, Long skuId) {
        if (skuId == null) {
            throw notFound();
        }
        return shopRepository.findSku(companyId, skuId).orElseThrow(ShopService::notFound);
    }

    private String validatedShelfStatus(String value) {
        String shelfStatus = required(value, "SHELF_STATUS_REQUIRED: 上下架状态不能为空").toUpperCase(Locale.ROOT);
        if (!List.of("ON_SHELF", "OFF_SHELF").contains(shelfStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SHELF_STATUS: 上下架状态只支持 ON_SHELF/OFF_SHELF");
        }
        return shelfStatus;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU_NOT_FOUND: SKU不存在或无权访问");
    }

    private String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> List<T> list(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record ImageImportResult(String fileId, String url, String thumbnailUrl) {
    }
}
