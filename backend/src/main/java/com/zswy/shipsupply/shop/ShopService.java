package com.zswy.shipsupply.shop;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    private static final String MATERIAL = "MATERIAL";
    private static final String FOOD = "FOOD";
    private static final String MATERIAL_SHEET = "物料";
    private static final String FOOD_SHEET = "伙食";

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
        Long requestedCompanyId,
        String productType,
        String categoryName,
        String codeStatus,
        String shelfStatus,
        String keyword,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        long targetCompanyId = requestedCompanyId == null ? currentUser.companyId() : requestedCompanyId;
        if (targetCompanyId != currentUser.companyId() && !shopRepository.isSupplierCompany(targetCompanyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND: supplier not found");
        }
        return shopRepository.listSkus(
            targetCompanyId,
            optional(productType),
            optional(categoryName),
            optional(codeStatus),
            optional(shelfStatus),
            optional(keyword),
            page <= 0 ? 1 : page,
            size <= 0 ? 20 : Math.min(size, 100)
        );
    }

    public ShopSkuListResponse listSkus(
        String authorizationHeader,
        String productType,
        String categoryName,
        String codeStatus,
        String shelfStatus,
        String keyword,
        int page,
        int size
    ) {
        return listSkus(authorizationHeader, null, productType, categoryName, codeStatus, shelfStatus, keyword, page, size);
    }

    public SupplierListResponse listSuppliers(
        String authorizationHeader,
        Long companyId,
        String keyword,
        String port,
        String category,
        String status,
        int page,
        int size
    ) {
        currentUserService.requireActiveCompanyUser(authorizationHeader);
        return shopRepository.listSuppliers(
            companyId,
            optional(keyword),
            optional(port),
            optional(category),
            optional(status),
            page <= 0 ? 1 : page,
            size <= 0 ? 50 : Math.min(size, 200)
        );
    }

    public SupplierListResponse listSuppliers(
        String authorizationHeader,
        String keyword,
        String port,
        String category,
        String status,
        int page,
        int size
    ) {
        return listSuppliers(authorizationHeader, null, keyword, port, category, status, page, size);
    }

    public SupplierQualificationListResponse listSupplierQualifications(String authorizationHeader, Long companyId) {
        currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (companyId == null || !shopRepository.isActiveSupplierCompany(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND: supplier not found");
        }
        return new SupplierQualificationListResponse(shopRepository.listSupplierQualifications(companyId));
    }

    @Transactional
    public SupplierStatusUpdateResponse updateSupplierStatus(
        String authorizationHeader,
        Long companyId,
        SupplierStatusUpdateRequest request
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!authRepository.hasRole(currentUser.userId(), "PLATFORM_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PLATFORM_ADMIN_REQUIRED: platform admin role is required");
        }
        if (companyId == null || request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPPLIER_STATUS_REQUIRED: supplier and status are required");
        }
        String targetStatus = optional(request.status());
        targetStatus = targetStatus == null ? "" : targetStatus.toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "DISABLED").contains(targetStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPPLIER_STATUS_INVALID: status must be ACTIVE or DISABLED");
        }
        if (!shopRepository.updateSupplierStatus(companyId, targetStatus)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND: supplier not found");
        }
        shopRepository.logSupplierStatusChange(currentUser.userId(), companyId, targetStatus);
        return new SupplierStatusUpdateResponse(companyId, targetStatus);
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
            if (skuId != null) {
                existingSku = requireExistingSku(currentUser.companyId(), skuId);
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

    public byte[] importTemplate(String authorizationHeader) {
        currentUserService.requireActiveCompanyUser(authorizationHeader);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.getProperties().getCustomProperties().addProperty(
                "ShipSupplyTemplateVersion",
                XlsxMaterialQuoteParser.OFFICIAL_SHOP_TEMPLATE_VERSION
            );
            workbook.getProperties().getCustomProperties().addProperty(
                "ShipSupplyTemplateFingerprint",
                XlsxMaterialQuoteParser.OFFICIAL_SHOP_TEMPLATE_FINGERPRINT
            );
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            writeTemplateSheet(
                workbook,
                MATERIAL_SHEET,
                headerStyle,
                List.of(
                    "Product Name", "Supplier SKU Code", "Qty", "Unit", "IMPA", "Specification",
                    "Unit Price", "Stock", "Packing", "Barcode", "Category", "图片（可粘贴）"
                ),
                List.of(
                    "示例：船用工作手套", XlsxMaterialQuoteParser.OFFICIAL_TEMPLATE_EXAMPLE_PREFIX + "MATERIAL__",
                    "100", "双", "", "耐油 / L码", "12.80", "500", "10双/包", "", "", ""
                )
            );
            writeTemplateSheet(
                workbook,
                FOOD_SHEET,
                headerStyle,
                List.of(
                    "Product Name", "Supplier SKU Code", "Qty", "Unit", "Specification",
                    "Unit Price", "Stock", "Packing", "Barcode", "Category", "图片（可粘贴）"
                ),
                List.of(
                    "示例：东北大米", XlsxMaterialQuoteParser.OFFICIAL_TEMPLATE_EXAMPLE_PREFIX + "FOOD__",
                    "20", "袋", "25kg/袋", "138.00", "100", "编织袋", "", "粮油米面", ""
                )
            );
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "IMPORT_TEMPLATE_FAILED: 导入模板生成失败", ex);
        }
    }

    private void writeTemplateSheet(
        XSSFWorkbook workbook,
        String sheetName,
        CellStyle headerStyle,
        List<String> headers,
        List<String> example
    ) {
        Sheet sheet = workbook.createSheet(sheetName);
        var headerRow = sheet.createRow(0);
        for (int index = 0; index < headers.size(); index++) {
            var cell = headerRow.createCell(index);
            cell.setCellValue(headers.get(index));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(index, Math.min(50, Math.max(14, headers.get(index).length() + 4)) * 256);
            if (headers.get(index).contains("图片")) {
                var helper = workbook.getCreationHelper();
                var anchor = helper.createClientAnchor();
                anchor.setCol1(index);
                anchor.setCol2(index + 3);
                anchor.setRow1(0);
                anchor.setRow2(4);
                var comment = sheet.createDrawingPatriarch().createCellComment(anchor);
                comment.setAuthor("CS");
                comment.setString(helper.createRichTextString("可直接粘贴或插入商品图片；每个商品一行，图片锚定到该商品所在行。示例行仅作说明，系统不会导入。"));
                cell.setCellComment(comment);
            }
        }
        var exampleRow = sheet.createRow(1);
        for (int index = 0; index < example.size(); index++) exampleRow.createCell(index).setCellValue(example.get(index));
        sheet.createFreezePane(0, 1);
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
        String productType = optional(request.productType()) == null ? null : validatedProductType(request.productType());
        List<ShopSkuResponse> items = shopRepository.confirmBatch(
            currentUser.companyId(),
            currentUser.userId(),
            batchId,
            productType
        );
        int exceptionCount = (int) items.stream().filter(item -> PENDING_EXCEPTION.equals(item.codeStatus())).count();
        return new ShopImportConfirmResponse(
            batchId,
            "CONFIRMED",
            items.size(),
            items.size() - exceptionCount,
            exceptionCount,
            items,
            productType
        );
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
            validatedProductType(request.productType()),
            optional(request.categoryCode()),
            optional(request.categoryName()),
            optional(request.platformCode()),
            optional(request.impaCode()),
            optional(request.supplierSkuCode()),
            productName,
            truncate(optional(request.productDescription()), 4000),
            normalizedProductTags(request.productTags()),
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
            sku.productDescription(),
            sku.productTags(),
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

    private String validatedProductType(String productType) {
        String normalized = optional(productType) == null ? MATERIAL : productType.trim().toUpperCase(Locale.ROOT);
        if (!List.of(MATERIAL, FOOD).contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_TYPE: 商品类型只支持 MATERIAL 或 FOOD");
        }
        return normalized;
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
            request.productDescription(),
            request.productTags(),
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

    private List<String> normalizedProductTags(List<String> productTags) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : list(productTags)) {
            String tag = optional(value);
            if (tag == null) {
                continue;
            }
            if (tag.length() > 12) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCT_TAG_TOO_LONG: 单个商品标签最多12个字符");
            }
            normalized.add(tag);
            if (normalized.size() > 6) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOO_MANY_PRODUCT_TAGS: 每个商品最多6个标签");
            }
        }
        return List.copyOf(normalized);
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
            String fileName = optional(file.getOriginalFilename());
            if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMPORT_XLSX_REQUIRED: 仅支持固定双 Sheet XLSX 模板");
            }
            List<String> sheetNames = xlsxParser.sheetNames(temp);
            boolean legacyParserStub = sheetNames == null || sheetNames.isEmpty();
            if (!legacyParserStub && (sheetNames.size() < 2
                || !MATERIAL_SHEET.equals(sheetNames.get(0))
                || !FOOD_SHEET.equals(sheetNames.get(1)))) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "IMPORT_SHEETS_REQUIRED: XLSX 前两个 Sheet 必须依次为“物料”和“伙食”"
                );
            }
            MaterialParsedDocument materialDocument = legacyParserStub
                ? xlsxParser.parse(temp)
                : xlsxParser.parse(temp, MATERIAL_SHEET);
            MaterialParsedDocument foodDocument = legacyParserStub
                ? new MaterialParsedDocument("FOOD", "SHOP_TEMPLATE", 0, List.of())
                : xlsxParser.parse(temp, FOOD_SHEET);
            if (!legacyParserStub) {
                validateSheetHeader(temp, MATERIAL_SHEET, materialDocument);
                validateSheetHeader(temp, FOOD_SHEET, foodDocument);
            }
            long excelDoneAt = System.nanoTime();
            List<ShopImportPreviewItem> items = new ArrayList<>();
            for (MaterialQuoteRow row : materialDocument.rows()) {
                if (isTemplateExample(row)) continue;
                items.add(previewItem(row, batchId, companyId, shopId, userId, temp, MATERIAL));
            }
            for (MaterialQuoteRow row : foodDocument.rows()) {
                if (isTemplateExample(row)) continue;
                items.add(previewItem(row, batchId, companyId, shopId, userId, temp, FOOD));
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

    private boolean isTemplateExample(MaterialQuoteRow row) {
        return row != null && row.supplierItemNo() != null
            && row.supplierItemNo().startsWith(XlsxMaterialQuoteParser.OFFICIAL_TEMPLATE_EXAMPLE_PREFIX);
    }

    private void validateSheetHeader(Path sourceFile, String sheetName, MaterialParsedDocument document) throws IOException {
        if (document != null && document.headerRowIndex() == 0 && xlsxParser.hasNonBlankCells(sourceFile, sheetName)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "IMPORT_SHEET_HEADER_INVALID: “" + sheetName + "”Sheet 表头不符合模板"
            );
        }
    }

    private long elapsedMs(long from, long to) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(to - from);
    }

    private ShopImportPreviewItem previewItem(
        MaterialQuoteRow row,
        long batchId,
        long companyId,
        long shopId,
        long userId,
        Path sourceFile,
        String productType
    ) {
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
        boolean material = MATERIAL.equals(productType);
        ShopRecognitionResult recognition = material ? recognition(rawName, spec, packageSpec) : null;
        String cleanName = recognition == null ? rawName : recognition.cleanName();
        String productName = truncate(first(optional(cleanName), rawName), 500);
        ShopImportRecommendation exactCode = material && recognitionService != null
            ? recognitionService.resolveStandardCode(rawCode)
            : null;
        ShopImportRecommendation selected = exactCode != null
            ? exactCode
            : recognition == null ? null : selectedRecommendation(recognition);
        String impaCode = selected == null ? null : selected.impaCode();
        String categoryCode = selected == null ? optional(row.rawColumns().get("Category Code")) : selected.categoryCode();
        String categoryName = selected == null ? optional(row.rawColumns().get("Category")) : selected.categoryName();
        String codeStatus = material ? codeStatus(recognition, exactCode) : "CONFIRMED";
        String exceptionReason = material && PENDING_EXCEPTION.equals(codeStatus) ? exceptionReason(recognition) : null;
        List<ShopParsedAttribute> parsedAttributes = recognition == null ? List.of() : recognition.parsedAttributes();
        List<ShopSkuAttributeResponse> specifications = previewAttributes(parsedAttributes, spec, packageSpec, barcode, row.rawColumns());
        String attributeSummary = attributeSummary(specifications);
        Map<String, String> rawColumns = new LinkedHashMap<>(row.rawColumns());
        rawColumns.put("_sourceSheet", material ? MATERIAL_SHEET : FOOD_SHEET);
        return new ShopImportPreviewItem(
            null,
            null,
            shopId,
            companyId,
            productType,
            categoryCode,
            categoryName,
            impaCode,
            impaCode,
            supplierSkuCode,
            productName,
            specifications,
            attributeSummary,
            stockQty,
            optional(row.unit()),
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
            optional(row.unit()),
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
            cleanName,
            parsedAttributes,
            recognition == null ? null : recognition.logicRecommendation(),
            recognition == null ? List.of() : recognition.categoryCandidates(),
            rawColumns,
            null,
            "INSERT",
            null
        );
    }

    private List<ShopImportPreviewItem> withPreviewActions(List<ShopImportPreviewItem> items, long companyId, long shopId) {
        List<ShopImportPreviewItem> marked = new ArrayList<>();
        for (ShopImportPreviewItem item : items) {
            marked.add(withPreviewAction(
                item,
                null,
                "INSERT",
                null,
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

    private String codeStatus(ShopRecognitionResult recognition, ShopImportRecommendation exactCode) {
        if (exactCode != null && hasRecommendedCode(exactCode)) {
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
