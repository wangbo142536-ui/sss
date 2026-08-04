package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.procurement.materials.MaterialParsedDocument;
import com.zswy.shipsupply.procurement.materials.MaterialQuoteRow;
import com.zswy.shipsupply.procurement.materials.XlsxMaterialQuoteParser;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemRepository;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemResponse;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private XlsxMaterialQuoteParser xlsxParser;

    @Mock
    private ImpaItemRepository impaItemRepository;

    private ShopService service;

    @BeforeEach
    void setUp() {
        service = new ShopService(
            currentUserService,
            shopRepository,
            authRepository,
            xlsxParser,
            new ShopImportRecognitionService(impaItemRepository)
        );
    }

    @Test
    void returnsDefaultProfileWithoutWritingWhenShopDoesNotExist() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.companyName(22L)).thenReturn("A供货商");
        when(shopRepository.findProfile(22L)).thenReturn(Optional.empty());

        ShopProfileResponse response = service.profile("Bearer token");

        assertThat(response.companyId()).isEqualTo(22L);
        assertThat(response.companyName()).isEqualTo("A供货商");
        assertThat(response.shopName()).isEqualTo("A供货商");
        assertThat(response.dataReady()).isFalse();
        verify(shopRepository, never()).saveProfile(eq(22L), eq(10L), any());
    }

    @Test
    void downloadsFixedMaterialAndFoodImportTemplate() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));

        byte[] bytes = service.importTemplate("Bearer token");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("物料");
            assertThat(workbook.getSheetName(1)).isEqualTo("伙食");
            assertThat(workbook.getSheet("物料").getRow(0).getCell(0).getStringCellValue())
                .isEqualTo("Product Name");
            assertThat(workbook.getSheet("伙食").getRow(0).getCell(1).getStringCellValue())
                .isEqualTo("Supplier SKU Code");
        }
    }

    @Test
    void savesProfileInCurrentCompanyScope() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        ShopProfileResponse saved = profile(5L, "Ocean Store", true);
        when(shopRepository.saveProfile(eq(22L), eq(10L), any())).thenReturn(saved);

        ShopProfileResponse response = service.saveProfile("Bearer token", new ShopProfileSaveRequest(
            "Ocean Store",
            "LOGO-1",
            "/api/files/LOGO-1",
            "Marine supplies",
            List.of("11", "33"),
            List.of("Zhoushan"),
            List.of("Outer Anchorage"),
            "Wang",
            "13800000000",
            "shop@example.com",
            "ACTIVE"
        ));

        assertThat(response.shopId()).isEqualTo(5L);
        assertThat(response.shopName()).isEqualTo("Ocean Store");
    }

    @Test
    void createsSkuAndListsCurrentCompanyData() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.saveSku(eq(22L), eq(5L), eq(10L), eq(null), any())).thenReturn(sku(100L));
        when(shopRepository.listSkus(22L, "MATERIAL", "PENDING_EXCEPTION", "OFF_SHELF", "rag", 1, 20))
            .thenReturn(new ShopSkuListResponse(List.of(sku(100L)), 1L, 1, 20));

        ShopSkuResponse created = service.createSku("Bearer token", skuRequest());
        ShopSkuListResponse list = service.listSkus("Bearer token", "MATERIAL", "PENDING_EXCEPTION", "OFF_SHELF", "rag", 1, 20);

        assertThat(created.skuId()).isEqualTo(100L);
        assertThat(list.total()).isEqualTo(1L);
    }

    @Test
    void batchUpsertInsertsRowsWithoutSkuIdAndUpdatesRowsWithSkuId() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.findSku(22L, 100L)).thenReturn(Optional.of(sku(100L)));
        when(shopRepository.findSkuBySupplierSkuCode(22L, 5L, "MATERIAL", "SKU-001")).thenReturn(Optional.empty());
        when(shopRepository.saveSku(eq(22L), eq(5L), eq(10L), eq(null), any())).thenReturn(sku(101L));
        when(shopRepository.saveSku(eq(22L), eq(5L), eq(10L), eq(100L), any())).thenReturn(sku(100L));

        ShopSkuBatchUpsertResponse response = service.batchUpsertSkus("Bearer token", new ShopSkuBatchUpsertRequest(List.of(
            new ShopSkuUpsertItem(null, skuRequest()),
            new ShopSkuUpsertItem(100L, skuRequest("SKU-002", "Cotton Rag", List.of(), null, null, null, 8L, 3))
        )));

        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.successCount()).isEqualTo(2);
        assertThat(response.insertedCount()).isEqualTo(1);
        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        assertThat(response.rowResults()).extracting(ShopSkuBatchUpsertRowResult::status).containsExactly("INSERTED", "UPDATED");
        assertThat(response.items()).extracting(ShopSkuResponse::skuId).containsExactly(101L, 100L);
        verify(shopRepository).saveSku(eq(22L), eq(5L), eq(10L), eq(null), any());
        verify(shopRepository).saveSku(eq(22L), eq(5L), eq(10L), eq(100L), any());
    }

    @Test
    void batchUpsertUpdatesExistingSupplierSkuWhenSkuIdIsMissing() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.findSkuBySupplierSkuCode(22L, 5L, "MATERIAL", "SKU-001")).thenReturn(Optional.of(sku(100L, "ON_SHELF")));
        when(shopRepository.saveSku(eq(22L), eq(5L), eq(10L), eq(100L), any())).thenReturn(sku(100L));

        ShopSkuBatchUpsertResponse response = service.batchUpsertSkus("Bearer token", new ShopSkuBatchUpsertRequest(
            48L,
            List.of(new ShopSkuUpsertItem(null, null, 7, "SKU-001", skuRequest(
                "SKU-001",
                "Updated Cotton Rag",
                List.of(),
                null,
                null,
                null,
                null,
                null
            )))
        ));

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.insertedCount()).isZero();
        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(response.rowResults().get(0).status()).isEqualTo("UPDATED");
        assertThat(response.rowResults().get(0).importRowNumber()).isEqualTo(7);
        assertThat(response.rowResults().get(0).supplierSkuCode()).isEqualTo("SKU-001");
        ArgumentCaptor<ShopSkuRequest> requestCaptor = ArgumentCaptor.forClass(ShopSkuRequest.class);
        verify(shopRepository).saveSku(eq(22L), eq(5L), eq(10L), eq(100L), requestCaptor.capture());
        ShopSkuRequest savedRequest = requestCaptor.getValue();
        assertThat(savedRequest.shelfStatus()).isEqualTo("ON_SHELF");
        assertThat(savedRequest.images()).hasSize(1);
        assertThat(savedRequest.images().get(0).fileId()).isEqualTo("FILE-1");
    }

    @Test
    void batchUpsertPreservesImportSourceAndAcceptsTopLevelImageFields() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.findSkuBySupplierSkuCode(22L, 5L, "MATERIAL", "SKU-TOP-IMG")).thenReturn(Optional.empty());
        when(shopRepository.saveSku(eq(22L), eq(5L), eq(10L), eq(null), any())).thenReturn(sku(101L));

        ShopSkuRequest request = skuRequest(
            "SKU-TOP-IMG",
            "Cotton Rag",
            List.of(),
            "FILE-TOP",
            "/api/files/FILE-TOP",
            "/api/files/FILE-TOP/thumb",
            null,
            null
        );
        service.batchUpsertSkus("Bearer token", new ShopSkuBatchUpsertRequest(
            88L,
            List.of(new ShopSkuUpsertItem(null, null, 12, null, request))
        ));

        ArgumentCaptor<ShopSkuRequest> requestCaptor = ArgumentCaptor.forClass(ShopSkuRequest.class);
        verify(shopRepository).saveSku(eq(22L), eq(5L), eq(10L), eq(null), requestCaptor.capture());
        ShopSkuRequest savedRequest = requestCaptor.getValue();
        assertThat(savedRequest.importBatchId()).isEqualTo(88L);
        assertThat(savedRequest.importRowNumber()).isEqualTo(12);
        assertThat(savedRequest.images()).hasSize(1);
        assertThat(savedRequest.images().get(0).fileId()).isEqualTo("FILE-TOP");
        assertThat(savedRequest.images().get(0).imageUrl()).isEqualTo("/api/files/FILE-TOP");
        assertThat(savedRequest.images().get(0).thumbnailUrl()).isEqualTo("/api/files/FILE-TOP/thumb");
    }

    @Test
    void batchUpsertReportsRowContextAndRollsBackOnValidationError() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);

        ShopSkuRequest invalid = skuRequest(
            "SKU-BAD",
            " ",
            List.of(),
            null,
            null,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> service.batchUpsertSkus("Bearer token", new ShopSkuBatchUpsertRequest(
            88L,
            List.of(new ShopSkuUpsertItem(null, null, 3, "SKU-BAD", invalid))
        )))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("SKU_ROW_INVALID")
            .hasMessageContaining("row=3")
            .hasMessageContaining("supplierSkuCode=SKU-BAD");

        verify(shopRepository, never()).saveSku(any(Long.class), any(Long.class), any(Long.class), any(), any());
    }

    @Test
    void batchUpsertRejectsDuplicateSupplierSkuCodesInSameRequest() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);

        assertThatThrownBy(() -> service.batchUpsertSkus("Bearer token", new ShopSkuBatchUpsertRequest(
            88L,
            List.of(
                new ShopSkuUpsertItem(null, null, 3, "SKU-DUP", skuRequest("SKU-DUP", "Cotton Rag", List.of(), null, null, null, null, null)),
                new ShopSkuUpsertItem(null, null, 4, "SKU-DUP", skuRequest("SKU-DUP", "Cotton Rag", List.of(), null, null, null, null, null))
            )
        )))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("DUPLICATE_SUPPLIER_SKU_CODE")
            .hasMessageContaining("row=4")
            .hasMessageContaining("supplierSkuCode=SKU-DUP");

        verify(shopRepository, never()).saveSku(any(Long.class), any(Long.class), any(Long.class), any(), any());
    }

    @Test
    void batchShelfStatusUpdatesCurrentCompanySavedSkus() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.updateShelfStatusBatch(22L, List.of(100L, 101L), "ON_SHELF"))
            .thenReturn(List.of(sku(100L, "ON_SHELF"), sku(101L, "ON_SHELF")));

        ShopShelfStatusBatchResponse response = service.updateShelfStatusBatch(
            "Bearer token",
            new ShopShelfStatusBatchRequest(List.of(100L, 101L), "ON_SHELF")
        );

        assertThat(response.shelfStatus()).isEqualTo("ON_SHELF");
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.items()).extracting(ShopSkuResponse::shelfStatus).containsOnly("ON_SHELF");
    }

    @Test
    void rejectsCrossCompanySkuUpdate() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.findSku(22L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSku("Bearer token", 999L, skuRequest()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("SKU_NOT_FOUND");
    }

    @Test
    void resolvesExceptionAndWritesLog() {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.findSku(22L, 100L)).thenReturn(Optional.of(sku(100L)));
        when(shopRepository.resolveException(eq(22L), eq(10L), eq(100L), any())).thenReturn(sku(100L));

        ShopSkuResponse response = service.resolveException("Bearer token", 100L, new ShopExceptionResolveRequest(
            "MANUAL_CODE",
            "110101",
            "110101",
            null,
            "人工录入IMPA"
        ));

        assertThat(response.skuId()).isEqualTo(100L);
        verify(shopRepository).resolveException(eq(22L), eq(10L), eq(100L), any());
    }

    @Test
    void importPreviewUsesCleanProductNameAndKeepsParametersAsAttributes() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.createBatch(22L, 5L, 10L, "quote.xlsx")).thenReturn(8L);
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(new ImpaItemResponse("613001", "61", "Tools", "6130", "Flat Nose Plier", "FLAT NOSE PLIER", "160MM", "PCS")));
        when(xlsxParser.parse(any(Path.class))).thenReturn(new MaterialParsedDocument(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            List.of(materialQuoteRow("6\"/160mm Flat Nose Plier"))
        ));

        ShopImportPreviewResponse response = service.importPreview(
            "Bearer token",
            new MockMultipartFile("file", "quote.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3})
        );

        ShopImportPreviewItem item = response.items().get(0);
        assertThat(item.productName()).isEqualTo("Flat Nose Plier");
        assertThat(item.supplierSkuCode()).isEqualTo("SKU-PLIER-001");
        assertThat(item.rawName()).isEqualTo("6\"/160mm Flat Nose Plier");
        assertThat(item.cleanName()).isEqualTo("Flat Nose Plier");
        assertThat(item.parsedAttributes()).extracting(ShopParsedAttribute::key).contains("size", "packing");
        assertThat(item.specifications()).extracting(ShopSkuAttributeResponse::value).contains("6\" / 160mm", "12PCS/CTN");
        assertThat(item.attributeSummary()).contains("6\" / 160mm");
        assertThat(item.currency()).isEqualTo("CNY");
        assertThat(item.currencySymbol()).isEqualTo("\u00A5");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ShopImportPreviewItem>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(shopRepository).savePreviewRows(eq(8L), eq(22L), eq(5L), rowsCaptor.capture());
        ShopImportPreviewItem savedPreviewRow = rowsCaptor.getValue().get(0);
        assertThat(savedPreviewRow.productName()).isEqualTo("Flat Nose Plier");
        assertThat(savedPreviewRow.rawName()).isEqualTo("6\"/160mm Flat Nose Plier");
    }

    @Test
    void importPreviewMarksExistingSupplierSkuAsUpdateAndReturnsSnapshot() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.createBatch(22L, 5L, 10L, "quote.xlsx")).thenReturn(12L);
        when(shopRepository.findSkuBySupplierSkuCode(22L, 5L, "MATERIAL", "SKU-PLIER-001")).thenReturn(Optional.of(sku(100L, "ON_SHELF")));
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(new ImpaItemResponse("613001", "61", "Tools", "6130", "Flat Nose Plier", "FLAT NOSE PLIER", "160MM", "PCS")));
        when(xlsxParser.parse(any(Path.class))).thenReturn(new MaterialParsedDocument(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            List.of(materialQuoteRow("SKU-PLIER-001", "6\"/160mm Flat Nose Plier"))
        ));

        ShopImportPreviewResponse response = service.importPreview(
            "Bearer token",
            new MockMultipartFile("file", "quote.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3})
        );

        ShopImportPreviewItem item = response.items().get(0);
        assertThat(item.previewAction()).isEqualTo("UPDATE");
        assertThat(item.existingSkuId()).isEqualTo(100L);
        assertThat(item.existingSnapshot()).isNotNull();
        assertThat(item.existingSnapshot().shelfStatus()).isEqualTo("ON_SHELF");
        assertThat(item.productName()).isEqualTo("Flat Nose Plier");
    }

    @Test
    void importPreviewMarksMissingSupplierSkuAsBlocked() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.createBatch(22L, 5L, 10L, "quote.xlsx")).thenReturn(13L);
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of());
        when(xlsxParser.parse(any(Path.class))).thenReturn(new MaterialParsedDocument(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            List.of(materialQuoteRow("", "No Code Paint Brush"))
        ));

        ShopImportPreviewResponse response = service.importPreview(
            "Bearer token",
            new MockMultipartFile("file", "quote.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3})
        );

        ShopImportPreviewItem item = response.items().get(0);
        assertThat(item.previewAction()).isEqualTo("BLOCKED");
        assertThat(item.existingSkuId()).isNull();
        assertThat(item.codeStatus()).isEqualTo("PENDING_EXCEPTION");
        assertThat(item.exceptionReason()).isEqualTo("SUPPLIER_SKU_CODE_REQUIRED");
        assertThat(response.successCount()).isZero();
        assertThat(response.exceptionCount()).isEqualTo(1);
    }

    @Test
    void importPreviewMarksDuplicateSupplierSkuCodesInSameFile() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.createBatch(22L, 5L, 10L, "quote.xlsx")).thenReturn(14L);
        when(shopRepository.findSkuBySupplierSkuCode(22L, 5L, "MATERIAL", "SKU-DUP")).thenReturn(Optional.empty());
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of());
        when(xlsxParser.parse(any(Path.class))).thenReturn(new MaterialParsedDocument(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            List.of(
                materialQuoteRow("SKU-DUP", "First Paint Brush"),
                materialQuoteRow("SKU-DUP", "Second Paint Brush")
            )
        ));

        ShopImportPreviewResponse response = service.importPreview(
            "Bearer token",
            new MockMultipartFile("file", "quote.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3})
        );

        assertThat(response.items()).extracting(ShopImportPreviewItem::previewAction).containsExactly("DUPLICATE", "DUPLICATE");
        assertThat(response.items()).extracting(ShopImportPreviewItem::exceptionReason).containsOnly("DUPLICATE_SUPPLIER_SKU_CODE");
        assertThat(response.successCount()).isZero();
        assertThat(response.exceptionCount()).isEqualTo(2);
    }

    @Test
    void importPreviewKeepsSameSupplierSkuSeparatedBySheetProductType() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.createBatch(22L, 5L, 10L, "dual-sheet.xlsx")).thenReturn(15L);
        when(xlsxParser.sheetNames(any(Path.class))).thenReturn(List.of("物料", "伙食"));
        when(xlsxParser.parse(any(Path.class), eq("物料"))).thenReturn(new MaterialParsedDocument(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            List.of(materialQuoteRow("SKU-SAME", "Paint Brush"))
        ));
        when(xlsxParser.parse(any(Path.class), eq("伙食"))).thenReturn(new MaterialParsedDocument(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            List.of(materialQuoteRow("SKU-SAME", "Fresh Apple"))
        ));
        when(shopRepository.findSkuBySupplierSkuCode(22L, 5L, "MATERIAL", "SKU-SAME"))
            .thenReturn(Optional.empty());
        when(shopRepository.findSkuBySupplierSkuCode(22L, 5L, "FOOD", "SKU-SAME"))
            .thenReturn(Optional.empty());
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000))).thenReturn(List.of());

        ShopImportPreviewResponse response = service.importPreview(
            "Bearer token",
            new MockMultipartFile(
                "file",
                "dual-sheet.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3}
            )
        );

        assertThat(response.items()).extracting(ShopImportPreviewItem::productType)
            .containsExactly("MATERIAL", "FOOD");
        assertThat(response.items()).extracting(ShopImportPreviewItem::previewAction)
            .containsOnly("INSERT");
        ShopImportPreviewItem food = response.items().get(1);
        assertThat(food.impaCode()).isNull();
        assertThat(food.logicRecommendation()).isNull();
        assertThat(food.codeStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void importPreviewReturnsCleanProductNameForMultilineSupplierDescription() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.createBatch(22L, 5L, 10L, "quote.xlsx")).thenReturn(9L);
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(new ImpaItemResponse("613002", "61", "Tools", "6130", "Diagonal Cutting Plier", "DIAGONAL CUTTING PLIER", "180MM", "PCS")));
        String rawName = """
            7''/180Mm Diagonal Cutting Plier
            Material:Cr-V
            Drop-Forged Hardened
            Nickle-Plated Finish
            Double Color Pvc Handle
            """;
        when(xlsxParser.parse(any(Path.class))).thenReturn(new MaterialParsedDocument(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            List.of(materialQuoteRow("SKU-PLIER-002", rawName))
        ));

        ShopImportPreviewResponse response = service.importPreview(
            "Bearer token",
            new MockMultipartFile("file", "quote.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3})
        );

        ShopImportPreviewItem item = response.items().get(0);
        assertThat(item.supplierSkuCode()).isEqualTo("SKU-PLIER-002");
        assertThat(item.rawName()).contains("Material:Cr-V");
        assertThat(item.cleanName()).isEqualTo("Diagonal Cutting Plier");
        assertThat(item.productName()).isEqualTo("Diagonal Cutting Plier");
        assertThat(item.parsedAttributes()).extracting(ShopParsedAttribute::key)
            .contains("size", "material", "process", "finish", "handle", "packing");
        assertThat(item.specifications()).extracting(ShopSkuAttributeResponse::key)
            .contains("size", "lengthCm", "widthCm", "heightCm", "volumeM3", "grossWeightKg", "netWeightKg", "pcsInnerBox", "pcsCtns");
        assertThat(item.attributeSummary()).contains("7\" / 180mm", "Cr-V", "PP CARD HANGER");
        assertThat(item.attributeSummary()).contains("33", "12", "8", "0.012", "13.5", "12.8", "6", "48");
    }

    @Test
    void importPreviewDetectsUsdCurrencyFromPriceHeaderAndValue() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.createBatch(22L, 5L, 10L, "quote.xlsx")).thenReturn(10L);
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of());
        when(xlsxParser.parse(any(Path.class))).thenReturn(new MaterialParsedDocument(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            List.of(materialQuoteRowWithPrice("SKU-USD-001", "8PCS Paint Brush", "FOB WAREHOUSE(USD)", "$3.50"))
        ));

        ShopImportPreviewResponse response = service.importPreview(
            "Bearer token",
            new MockMultipartFile("file", "quote.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3})
        );

        ShopImportPreviewItem item = response.items().get(0);
        assertThat(item.unitPrice()).isEqualByComparingTo("3.50");
        assertThat(item.currency()).isEqualTo("USD");
        assertThat(item.currencySymbol()).isEqualTo("$");
    }

    @Test
    void importPreviewReturnsCategoryCandidatesForPlierWithoutSelectingCode() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(shopRepository.ensureShop(22L, 10L)).thenReturn(5L);
        when(shopRepository.createBatch(22L, 5L, 10L, "quote.xlsx")).thenReturn(11L);
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(
                new ImpaItemResponse("611801", "61", "Tools", "6118", "External Ring Plier", "EXTERNAL RING PLIER", "180MM", "PCS"),
                new ImpaItemResponse("611802", "61", "Tools", "6118", "Internal Ring Plier", "INTERNAL RING PLIER", "180MM", "PCS"),
                new ImpaItemResponse("611601", "61", "Tools", "6116", "Combination Plier", "COMBINATION PLIER", "180MM", "PCS"),
                new ImpaItemResponse("172601", "17", "Cabin Stores", "1726", "Can Opener", "HAND CAN OPENER PLIER TYPE", null, "PCS")
            ));
        when(xlsxParser.parse(any(Path.class))).thenReturn(new MaterialParsedDocument(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            List.of(materialQuoteRow("SKU-PLIER-003", "10''/250Mm Curved Jaw Locking Plier"))
        ));

        ShopImportPreviewResponse response = service.importPreview(
            "Bearer token",
            new MockMultipartFile("file", "quote.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3})
        );

        ShopImportPreviewItem item = response.items().get(0);
        assertThat(item.productName()).isEqualTo("Curved Jaw Locking Plier");
        assertThat(item.categoryCode()).isEqualTo("61");
        assertThat(item.categoryName()).isEqualTo("Tools");
        assertThat(item.impaCode()).isNull();
        assertThat(item.platformCode()).isNull();
        assertThat(item.codeStatus()).isEqualTo("PENDING_EXCEPTION");
        assertThat(item.exceptionReason()).isEqualTo("CATEGORY_ONLY_IMPA_REQUIRED");
        assertThat(item.categoryCandidates()).extracting(ShopCategoryCandidate::segmentCode).contains("6118");
        assertThat(item.logicRecommendation().status()).isEqualTo("CATEGORY_ONLY");
    }

    private ShopProfileResponse profile(Long shopId, String shopName, boolean dataReady) {
        return new ShopProfileResponse(
            shopId,
            22L,
            "A供货商",
            shopName,
            null,
            null,
            "Marine supplies",
            List.of("11"),
            List.of("Zhoushan"),
            List.of("Outer Anchorage"),
            "Wang",
            "13800000000",
            "shop@example.com",
            "ACTIVE",
            "2026-06-09T10:00:00",
            "2026-06-09T10:05:00",
            dataReady
        );
    }

    private ShopSkuRequest skuRequest() {
        return skuRequest(
            "SKU-001",
            "Cotton Rag",
            List.of(new ShopSkuImageRequest("FILE-1", "/api/files/FILE-1", "/api/files/FILE-1", true, 0)),
            null,
            null,
            null,
            8L,
            3
        );
    }

    private ShopSkuRequest skuRequest(
        String supplierSkuCode,
        String productName,
        List<ShopSkuImageRequest> images,
        String imageFileId,
        String imageUrl,
        String thumbnailUrl,
        Long importBatchId,
        Integer importRowNumber
    ) {
        return new ShopSkuRequest(
            "MATERIAL",
            "11",
            "Deck stores",
            "110101",
            "110101",
            supplierSkuCode,
            productName,
            List.of(new ShopSkuAttributeRequest("specification", "Specification", "white", null, 0, "white")),
            new BigDecimal("20.0000"),
            "PCS",
            3,
            "Zhoushan",
            List.of("Zhoushan"),
            10,
            new BigDecimal("12.50"),
            "CNY",
            "Brand A",
            "PCS",
            "Carton",
            "690000000001",
            "OFF_SHELF",
            "PENDING_EXCEPTION",
            "未找到编码",
            images,
            imageFileId,
            imageUrl,
            thumbnailUrl,
            importBatchId,
            importRowNumber,
            null
        );
    }

    private ShopSkuResponse sku(Long id) {
        return sku(id, "OFF_SHELF");
    }

    private ShopSkuResponse sku(Long id, String shelfStatus) {
        return new ShopSkuResponse(
            id,
            5L,
            22L,
            "MATERIAL",
            "11",
            "Deck stores",
            "110101",
            "110101",
            "SKU-001",
            "Cotton Rag",
            List.of(new ShopSkuAttributeResponse(1L, "specification", "Specification", "white", null, 0, "white")),
            "Specification: white",
            new BigDecimal("20.0000"),
            "PCS",
            3,
            "Zhoushan",
            "/api/files/FILE-1",
            "/api/files/FILE-1",
            "FILE-1",
            10,
            new BigDecimal("12.50"),
            "CNY",
            "\u00A5",
            "Brand A",
            "PCS",
            "Carton",
            "690000000001",
            shelfStatus,
            "PENDING_EXCEPTION",
            "未找到编码",
            8L,
            3,
            "2026-06-09T10:00:00",
            "2026-06-09T10:05:00",
            List.of(new ShopSkuImageResponse(1L, "FILE-1", "/api/files/FILE-1", "/api/files/FILE-1", true, 0))
        );
    }

    private MaterialQuoteRow materialQuoteRow(String rawNameSpec) {
        return materialQuoteRow("SKU-PLIER-001", rawNameSpec);
    }

    private MaterialQuoteRow materialQuoteRow(String supplierSkuCode, String rawNameSpec) {
        return materialQuoteRowWithPrice(supplierSkuCode, rawNameSpec, "FOB WAREHOUSE(RMB)", "18.50");
    }

    private MaterialQuoteRow materialQuoteRowWithPrice(String supplierSkuCode, String rawNameSpec, String priceHeader, String priceValue) {
        return new MaterialQuoteRow(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            1,
            2,
            Map.ofEntries(
                Map.entry("Name of Commodity & Specification", rawNameSpec),
                Map.entry("Item No.", supplierSkuCode),
                Map.entry("Packing", supplierSkuCode.endsWith("002") ? "PP CARD HANGER" : "12PCS/CTN"),
                Map.entry(priceHeader, priceValue),
                Map.entry("STOCK", "30"),
                Map.entry("Pcs/Inner box", "6"),
                Map.entry("Pcs/Ctns", "48"),
                Map.entry("L/cm", "33"),
                Map.entry("W/cm", "12"),
                Map.entry("H/cm", "8"),
                Map.entry("N/m³", "0.012"),
                Map.entry("GW/kgs", "13.5"),
                Map.entry("NW/kgs", "12.8")
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            supplierSkuCode,
            rawNameSpec,
            priceValue,
            supplierSkuCode.endsWith("002") ? "PP CARD HANGER" : "12PCS/CTN",
            "30",
            false,
            null,
            null,
            null,
            null
        );
    }
}
