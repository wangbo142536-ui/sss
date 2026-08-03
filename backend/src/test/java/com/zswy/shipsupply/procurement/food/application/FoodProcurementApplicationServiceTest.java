package com.zswy.shipsupply.procurement.food.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.AttachmentPayload;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandItemPayload;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSaveRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonQuoteOption;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonSettings;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSubmitRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ImportUpdate;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderCreateRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderCreateResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteImportPreviewResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteItemUpdate;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SelectedQuoteItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementActionRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SendInquiryRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierOrder;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierOrderActionRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierSummary;
import com.zswy.shipsupply.procurement.food.domain.FoodItemNormalizer;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodDemandRepository;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodOrderRepository;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodOrderRepository.OrderableQuoteItem;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodQuoteRepository;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodQuoteRepository.ImportBatch;
import com.zswy.shipsupply.procurement.food.infrastructure.spreadsheet.FoodSpreadsheetParser;

@ExtendWith(MockitoExtension.class)
class FoodProcurementApplicationServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private AuthRepository authRepository;
    @Mock private FoodSpreadsheetParser spreadsheetParser;
    @Mock private FoodDemandRepository demandRepository;
    @Mock private FoodQuoteRepository quoteRepository;
    @Mock private FoodOrderRepository orderRepository;

    private FoodProcurementApplicationService service;

    @BeforeEach
    void setUp() {
        service = new FoodProcurementApplicationService(
            currentUserService, authRepository, spreadsheetParser, new FoodItemNormalizer(), demandRepository,
            quoteRepository, orderRepository, new ObjectMapper().findAndRegisterModules(), true
        );
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(7L, 11L, "ACTIVE", "ACTIVE"));
    }

    @Test
    void regulatoryEvaluationListRequiresRegulatoryRole() {
        when(authRepository.roleCodesForUser(7L)).thenReturn(List.of("REGULATORY"));
        when(orderRepository.listRegulatoryEvaluations("amber", "PENDING_REVIEW")).thenReturn(List.of());

        assertThat(service.listRegulatoryEvaluations("Bearer token", "amber", "pending_review")).isEmpty();

        verify(orderRepository).listRegulatoryEvaluations("amber", "PENDING_REVIEW");
    }

    @Test
    void regulatoryEvaluationListRejectsOrdinaryUser() {
        when(authRepository.roleCodesForUser(7L)).thenReturn(List.of("PURCHASER"));

        assertThatThrownBy(() -> service.listRegulatoryEvaluations("Bearer token", "", ""))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("REGULATORY_ROLE_REQUIRED");

        verify(orderRepository, never()).listRegulatoryEvaluations(any(), any());
    }

    @Test
    void buyerEvaluationSubmissionNormalizesAndStoresAttachments() {
        AttachmentPayload attachment = new AttachmentPayload(" file-1 ", " evidence.jpg ", null);
        EvaluationSummary updated = mock(EvaluationSummary.class);
        when(orderRepository.submitEvaluation(
            eq(61L), eq(11L), eq(5), eq(4), eq("Good service"), anyList()
        )).thenReturn(updated);

        EvaluationSummary result = service.submitEvaluation(
            "Bearer token", 61L,
            new EvaluationSubmitRequest(5, 4, "Good service", List.of(attachment))
        );

        assertThat(result).isSameAs(updated);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttachmentPayload>> attachments = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).submitEvaluation(
            eq(61L), eq(11L), eq(5), eq(4), eq("Good service"), attachments.capture()
        );
        assertThat(attachments.getValue()).containsExactly(
            new AttachmentPayload("file-1", "evidence.jpg", "/api/files/file-1")
        );
    }

    @Test
    void buyerEvaluationSubmissionRejectsInvalidAttachment() {
        EvaluationSubmitRequest request = new EvaluationSubmitRequest(
            5, 5, "Good service", List.of(new AttachmentPayload(null, "evidence.jpg", null))
        );

        assertThatThrownBy(() -> service.submitEvaluation("Bearer token", 61L, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("INVALID_EVALUATION_ATTACHMENT");

        verify(orderRepository, never()).submitEvaluation(
            any(Long.class), any(Long.class), any(Integer.class), any(Integer.class), any(), anyList()
        );
    }

    @Test
    void rejectsInvalidDemandBeforeAnyFoodTableWrite() {
        DemandSaveRequest request = new DemandSaveRequest(
            null, "RFQ-1", "", "ZHOUSHAN", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3),
            "USD", "food.xlsx", "Quotation", List.of()
        );

        assertThatThrownBy(() -> service.saveDemand("Bearer token", request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("FOOD_VESSEL_REQUIRED");
        verify(demandRepository, never()).insertDemand(any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void rejectsDemandWithoutQuoteDeadline() {
        DemandItemPayload item = new DemandItemPayload(
            1, 1, "Fresh Apple", "苹果", null, "10*500G", "KG",
            new BigDecimal("10"), "MATCHED", null, Map.of()
        );
        DemandSaveRequest request = new DemandSaveRequest(
            null, null, "MV AMBER", "ZHOUSHAN", LocalDateTime.now().plusDays(1), null,
            "USD", "food.xlsx", "Quotation", List.of(item)
        );

        assertThatThrownBy(() -> service.saveDemand("Bearer token", request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("FOOD_QUOTE_DEADLINE_REQUIRED");
        verify(demandRepository, never()).insertDemand(any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void saveDemandAtomicallyCreatesDraftQuotesForAllSuppliers() {
        DemandItemPayload item = new DemandItemPayload(
            1, 1, "Fresh Apple", "苹果", null, "10*500G", "KG",
            new BigDecimal("10"), "MATCHED", null, Map.of()
        );
        LocalDateTime quoteDeadline = LocalDateTime.of(2026, 7, 21, 23, 59, 59);
        DemandSaveRequest request = new DemandSaveRequest(
            null, null, "MV AMBER", "ZHOUSHAN", LocalDateTime.now().plusDays(1),
            quoteDeadline, "USD", "food.xlsx", "Quotation", List.of(item)
        );
        when(demandRepository.listSuppliers(11L)).thenReturn(List.of(
            new SupplierSummary(21L, "Supplier A", "SUPPLIER"),
            new SupplierSummary(22L, "Supplier B", "FOOD_SUPPLIER")
        ));
        when(demandRepository.insertDemand(11L, 7L, request, 1, 0)).thenReturn(55L);
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("INQUIRY_SENT"));

        var response = service.saveDemand("Bearer token", request);

        assertThat(response.status()).isEqualTo("INQUIRY_SENT");
        assertThat(response.supplierCount()).isEqualTo(2);
        verify(demandRepository).insertInquirySuppliers(55L, 11L, 7L, List.of(21L, 22L), quoteDeadline);
        verify(quoteRepository).ensureQuotesForDemand(55L, 7L);
    }

    @Test
    void sendsInquiryOnlyForOwnedDemandAndCreatesIndependentSupplierQuotes() {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("DRAFT"));
        when(demandRepository.listSuppliers(11L)).thenReturn(List.of(
            new SupplierSummary(21L, "Supplier A", "SUPPLIER"),
            new SupplierSummary(22L, "Supplier B", "FOOD_SUPPLIER")
        ));
        when(demandRepository.insertInquirySuppliers(55L, 11L, 7L, List.of(21L, 22L), 5)).thenReturn(2);
        when(quoteRepository.ensureQuotesForDemand(55L, 7L)).thenReturn(2);

        var response = service.sendInquiry("Bearer token", 55L, new SendInquiryRequest(List.of(21L, 22L, 21L), 5));

        assertThat(response.supplierCount()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("INQUIRY_SENT");
        verify(quoteRepository).ensureQuotesForDemand(55L, 7L);
    }

    @Test
    void excludesCompaniesWithoutActiveAccountsWhenSendingInquiry() {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("DRAFT"));
        when(demandRepository.listSuppliers(11L)).thenReturn(List.of(
            new SupplierSummary(21L, "Supplier A", "SUPPLIER")
        ));
        when(demandRepository.insertInquirySuppliers(55L, 11L, 7L, List.of(21L), 3)).thenReturn(1);
        when(quoteRepository.ensureQuotesForDemand(55L, 7L)).thenReturn(1);

        var response = service.sendInquiry(
            "Bearer token",
            55L,
            new SendInquiryRequest(List.of(21L, 29L, 21L), 3)
        );

        assertThat(response.supplierCount()).isEqualTo(1);
        verify(demandRepository).insertInquirySuppliers(55L, 11L, 7L, List.of(21L), 3);
    }

    @Test
    void unrelatedCompanyCannotUseAnyFoodQuoteMutation() {
        when(quoteRepository.quoteBelongsToCompany(88L, 11L)).thenReturn(false);
        QuoteItemUpdate update = new QuoteItemUpdate(
            901L, new BigDecimal("10"), new BigDecimal("3.00"), "AVAILABLE", null, "MANUAL"
        );

        assertThatThrownBy(() -> service.saveQuote(
            "Bearer token", 88L,
            new com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteSaveRequest(List.of(update))
        )).isInstanceOf(ResponseStatusException.class).hasMessageContaining("FOOD_QUOTE_NOT_FOUND");
        assertThatThrownBy(() -> service.virtualFill("Bearer token", 88L, false))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("FOOD_QUOTE_NOT_FOUND");
        assertThatThrownBy(() -> service.submitQuote("Bearer token", 88L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("FOOD_QUOTE_NOT_FOUND");
        assertThatThrownBy(() -> service.previewQuoteImport("Bearer token", 88L, null, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("FOOD_QUOTE_NOT_FOUND");
        assertThatThrownBy(() -> service.commitQuoteImport("Bearer token", 88L, 40L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("FOOD_QUOTE_NOT_FOUND");
        verify(quoteRepository, never()).saveQuoteItems(any(Long.class), any(Long.class), any(Long.class), anyList());
        verify(quoteRepository, never()).virtualFill(any(Long.class), any(Long.class), any(Long.class), any(Boolean.class));
        verify(quoteRepository, never()).submitQuote(any(Long.class), any(Long.class));
        verify(quoteRepository, never()).saveImportBatch(any(Long.class), any(), any(), any(Integer.class), any(Integer.class), any(Long.class));
        verify(quoteRepository, never()).commitImport(any(Long.class), any(Long.class), any(Long.class), any(Long.class), anyList());
    }

    @Test
    void buyerCompanyCanSaveVirtualFillAndSubmitDraftQuote() {
        QuoteItem item = new QuoteItem(
            901L, 501L, 1, "Fresh Apple", "Apple", null, "10*500G", "KG",
            new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("3.00"), new BigDecimal("30.00"),
            "AVAILABLE", "MANUAL", "MATCHED", "buyer entered"
        );
        QuoteDetail buyerQuote = new QuoteDetail(
            88L, 55L, "FQ-1", "FREQ-1", "FOOD-1", "MV AMBER", "ZHOUSHAN",
            LocalDateTime.of(2026, 7, 20, 9, 0), "Current Buyer", "Supplier", "USD", "DRAFT", 1,
            new BigDecimal("30.00"), 1, 0, 0, null, LocalDateTime.now(), List.of(item)
        );
        QuoteItemUpdate update = new QuoteItemUpdate(
            901L, new BigDecimal("10"), new BigDecimal("3.00"), "AVAILABLE", "buyer entered", "MANUAL"
        );
        when(quoteRepository.quoteBelongsToCompany(88L, 11L)).thenReturn(true);
        when(quoteRepository.getQuote(88L, 11L)).thenReturn(buyerQuote);

        service.saveQuote("Bearer token", 88L, new com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteSaveRequest(List.of(update)));
        service.virtualFill("Bearer token", 88L, true);
        service.submitQuote("Bearer token", 88L);

        verify(quoteRepository).saveQuoteItems(88L, 11L, 7L, List.of(update));
        verify(quoteRepository).virtualFill(88L, 11L, 7L, true);
        verify(quoteRepository).submitQuote(88L, 11L);
    }

    @Test
    void quoteImportCommitUsesCurrentRequestedQuantityInsteadOfSpreadsheetQuotedQuantity() throws Exception {
        QuoteItem item = new QuoteItem(
            901L, 501L, 1, "Fresh Apple", "苹果", null, "10*500G", "KG",
            new BigDecimal("10"), new BigDecimal("2"), null, null, "AVAILABLE", null, "MATCHED", null
        );
        QuoteDetail quote = new QuoteDetail(
            88L, 55L, "FQ-1", "FREQ-1", "FOOD-1", "MV AMBER", "ZHOUSHAN",
            LocalDateTime.of(2026, 7, 20, 9, 0), "Buyer", "Supplier", "USD", "DRAFT", 1,
            BigDecimal.ZERO, 0, 1, 1, null, LocalDateTime.now(), List.of(item)
        );
        QuoteImportPreviewResponse preview = new QuoteImportPreviewResponse(
            null, "quote.xlsx", "Food Quotation", 1, 0,
            List.of(new ImportUpdate(5, 901L, 501L, new BigDecimal("999"), new BigDecimal("3.50"), "EXACT")),
            List.of()
        );
        when(quoteRepository.quoteBelongsToCompany(88L, 11L)).thenReturn(true);
        when(quoteRepository.getQuote(88L, 11L)).thenReturn(quote);
        when(quoteRepository.getImportBatch(40L, 88L)).thenReturn(new ImportBatch(
            40L, 88L, "PREVIEW", new ObjectMapper().findAndRegisterModules().writeValueAsString(preview)
        ));
        when(quoteRepository.commitImport(eq(40L), eq(88L), eq(11L), eq(7L), anyList())).thenReturn(1);
        when(quoteRepository.quoteVersion(88L)).thenReturn(2);

        service.commitQuoteImport("Bearer token", 88L, 40L);

        ArgumentCaptor<List<QuoteItemUpdate>> updates = ArgumentCaptor.forClass(List.class);
        verify(quoteRepository).commitImport(eq(40L), eq(88L), eq(11L), eq(7L), updates.capture());
        assertThat(updates.getValue()).singleElement().satisfies(update -> {
            assertThat(update.quotedQuantity()).isEqualByComparingTo("10");
            assertThat(update.unitPrice()).isEqualByComparingTo("3.50");
            assertThat(update.availability()).isEqualTo("AVAILABLE");
            assertThat(update.priceSource()).isEqualTo("IMPORTED");
        });
    }

    @Test
    void lowestItemOrderSelectsCheapestCompleteQuoteAndUsesRequestedQuantity() {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("QUOTED"));
        when(orderRepository.orderExistsForDemand(55L)).thenReturn(false);
        when(orderRepository.orderableItems(55L)).thenReturn(List.of(
            orderable(101L, 21L, "Supplier A", "12", "3.00"),
            orderable(102L, 22L, "Supplier B", "12", "2.00")
        ));
        when(orderRepository.insertOrder(
            eq(55L), eq(11L), eq(7L), eq("LOWEST_ITEM"), eq("USD"),
            eq(new BigDecimal("22.0000")), eq(new BigDecimal("20.0000")),
            eq(new BigDecimal("22.0000")), eq(new BigDecimal("2.0000")), any()
        ))
            .thenReturn(900L);
        when(orderRepository.insertSupplierOrders(eq(900L), anyList())).thenReturn(Map.of(22L, 901L));
        when(orderRepository.orderNo(900L)).thenReturn("FPO-20260716-001");

        OrderCreateResponse response = service.createOrder(
            "Bearer token", 55L, orderRequest("LOWEST_ITEM", false, null, List.of())
        );

        assertThat(response.totalAmount()).isEqualByComparingTo("22.0000");
        ArgumentCaptor<List<OrderableQuoteItem>> selected = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).insertOrderItems(eq(900L), anyMap(), selected.capture());
        assertThat(selected.getValue()).singleElement().satisfies(item -> {
            assertThat(item.quoteItemId()).isEqualTo(102L);
            assertThat(item.quotedQuantity()).isEqualByComparingTo("10");
            assertThat(item.unitPrice()).isEqualByComparingTo("2.00");
            assertThat(item.amount()).isEqualByComparingTo("20.0000");
            assertThat(item.quotedUnitPrice()).isEqualByComparingTo("2.2000");
            assertThat(item.quotedAmount()).isEqualByComparingTo("22.0000");
        });
    }

    @Test
    void selectedComparisonItemControlsTheCreatedOrder() {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("QUOTED"));
        when(orderRepository.orderExistsForDemand(55L)).thenReturn(false);
        when(orderRepository.orderableItems(55L)).thenReturn(List.of(
            orderable(101L, 21L, "Supplier A", "10", "3.00"),
            orderable(102L, 22L, "Supplier B", "10", "2.00")
        ));
        when(orderRepository.insertOrder(
            eq(55L), eq(11L), eq(7L), eq("LOWEST_ITEM"), eq("USD"),
            eq(new BigDecimal("33.0000")), eq(new BigDecimal("30.0000")),
            eq(new BigDecimal("33.0000")), eq(new BigDecimal("3.0000")), any()
        )).thenReturn(900L);
        when(orderRepository.insertSupplierOrders(eq(900L), anyList())).thenReturn(Map.of(21L, 901L));
        when(orderRepository.orderNo(900L)).thenReturn("FPO-SELECTED");

        service.createOrder(
            "Bearer token", 55L,
            orderRequest("LOWEST_ITEM", true, null, List.of(new SelectedQuoteItem(501L, 101L)))
        );

        ArgumentCaptor<List<OrderableQuoteItem>> selected = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).insertOrderItems(eq(900L), anyMap(), selected.capture());
        assertThat(selected.getValue()).singleElement().satisfies(item -> {
            assertThat(item.quoteItemId()).isEqualTo(101L);
            assertThat(item.supplierCompanyId()).isEqualTo(21L);
        });
    }

    @Test
    void selectedComparisonItemsIgnoreUnselectedDemandItemsWithoutNullUnboxing() {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("QUOTED"));
        when(orderRepository.orderExistsForDemand(55L)).thenReturn(false);
        OrderableQuoteItem selectedItem = orderable(101L, 21L, "Supplier A", "10", "3.00");
        OrderableQuoteItem unselectedItem = new OrderableQuoteItem(
            201L, 502L, 1201L, 21L, "Supplier A", "Fresh Orange", "橙子", "10*500G", "KG",
            new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("4.00"), new BigDecimal("40.00"), null, null
        );
        when(orderRepository.orderableItems(55L)).thenReturn(List.of(selectedItem, unselectedItem));
        when(orderRepository.insertOrder(
            eq(55L), eq(11L), eq(7L), eq("LOWEST_ITEM"), eq("USD"),
            any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any()
        )).thenReturn(900L);
        when(orderRepository.insertSupplierOrders(eq(900L), anyList())).thenReturn(Map.of(21L, 901L));
        when(orderRepository.orderNo(900L)).thenReturn("FPO-PARTIAL-SELECTION");

        service.createOrder(
            "Bearer token", 55L,
            orderRequest("LOWEST_ITEM", true, null, List.of(new SelectedQuoteItem(501L, 101L)))
        );

        ArgumentCaptor<List<OrderableQuoteItem>> selected = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).insertOrderItems(eq(900L), anyMap(), selected.capture());
        assertThat(selected.getValue()).extracting(OrderableQuoteItem::demandItemId).containsExactly(501L);
    }

    @Test
    void saveComparisonSettingsStoresBlankTrafficServiceAsNull() {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("QUOTED"));
        when(demandRepository.updateComparisonSettings(eq(55L), eq(11L), eq(7L), any())).thenReturn(1);

        ComparisonSettings saved = service.saveComparisonSettings(
            "Bearer token", 55L,
            new ComparisonSettings(
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "SEA", "BARGE", "", "", "", List.of(501L)
            )
        );

        assertThat(saved.trafficServiceJson()).isNull();
        assertThat(saved.selectedDemandItemIds()).containsExactly(501L);
        ArgumentCaptor<ComparisonSettings> settings = ArgumentCaptor.forClass(ComparisonSettings.class);
        verify(demandRepository).updateComparisonSettings(eq(55L), eq(11L), eq(7L), settings.capture());
        assertThat(settings.getValue().trafficServiceJson()).isNull();
        assertThat(settings.getValue().selectedDemandItemIds()).containsExactly(501L);
    }

    @Test
    void saveComparisonSettingsRejectsAnOrderedDemand() {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("ORDERED"));

        assertThatThrownBy(() -> service.saveComparisonSettings(
            "Bearer token", 55L,
            new ComparisonSettings(
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "SEA", "BARGE", "", "", "", List.of(501L)
            )
        )).isInstanceOf(ResponseStatusException.class).hasMessageContaining("FOOD_COMPARISON_LOCKED");

        verify(demandRepository, never()).updateComparisonSettings(any(Long.class), any(Long.class), any(Long.class), any());
    }

    @Test
    void comparisonReturnsThreeSupplierQuotesAndOnlyTwoStrategies() {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("QUOTED"));
        when(quoteRepository.comparisonOptions(55L)).thenReturn(List.of(
            comparisonOption(101L, 21L, "Supplier A", "3.00"),
            comparisonOption(102L, 22L, "Supplier B", "2.00"),
            comparisonOption(103L, 23L, "Supplier C", "2.50"),
            comparisonOption(104L, 24L, "Supplier D", "4.00")
        ));
        when(quoteRepository.submittedSupplierCount(55L)).thenReturn(4);

        var response = service.comparison("Bearer token", 55L);

        assertThat(response.submittedSupplierCount()).isEqualTo(4);
        assertThat(response.strategies()).extracting(strategy -> strategy.strategyType())
            .containsExactly("LOWEST_ITEM", "SINGLE_SUPPLIER");
        assertThat(response.strategies()).extracting(strategy -> strategy.label())
            .containsExactly("最低混供", "集中采购");
        assertThat(response.strategies().get(1).supplierCompanyId()).isEqualTo(22L);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.quotes()).hasSize(3);
            assertThat(item.quotes()).extracting(option -> option.supplierCompanyId()).doesNotContain(24L);
            assertThat(item.quotes()).filteredOn(option -> option.lowestPrice())
                .singleElement().extracting(option -> option.supplierCompanyId()).isEqualTo(22L);
        });
    }

    @Test
    void singleSupplierOrderHonorsSelectedSupplierInsteadOfAutoChoosingCheapest() {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demandDetail("QUOTED"));
        when(orderRepository.orderExistsForDemand(55L)).thenReturn(false);
        when(orderRepository.orderableItems(55L)).thenReturn(List.of(
            orderable(101L, 21L, "Supplier A", "10", "3.00"),
            orderable(102L, 22L, "Supplier B", "10", "2.00")
        ));
        when(orderRepository.insertOrder(
            eq(55L), eq(11L), eq(7L), eq("SINGLE_SUPPLIER"), eq("USD"),
            eq(new BigDecimal("33.0000")), eq(new BigDecimal("30.0000")),
            eq(new BigDecimal("33.0000")), eq(new BigDecimal("3.0000")), any()
        ))
            .thenReturn(900L);
        when(orderRepository.insertSupplierOrders(eq(900L), anyList())).thenReturn(Map.of(21L, 901L));
        when(orderRepository.orderNo(900L)).thenReturn("FPO-20260716-002");

        service.createOrder(
            "Bearer token", 55L, orderRequest("SINGLE_SUPPLIER", false, 21L, List.of())
        );

        ArgumentCaptor<List<OrderableQuoteItem>> selected = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).insertOrderItems(eq(900L), anyMap(), selected.capture());
        assertThat(selected.getValue()).singleElement().satisfies(item -> {
            assertThat(item.supplierCompanyId()).isEqualTo(21L);
            assertThat(item.quoteItemId()).isEqualTo(101L);
        });
    }

    @Test
    void supplierOrderCannotSkipExecutionStates() {
        when(orderRepository.findSupplierOrderForManagement(900L, 901L)).thenReturn(
            new SupplierOrder(901L, 11L, "Supplier", "CONFIRMED", new BigDecimal("20"), LocalDateTime.now(), null, null)
        );

        assertThatThrownBy(() -> service.actionSupplierOrder(
            "Bearer token", 900L, 901L, new SupplierOrderActionRequest("IN_TRANSIT", null, null, null)
        )).isInstanceOf(ResponseStatusException.class).hasMessageContaining("FOOD_ORDER_INVALID_STATUS_TRANSITION");
        verify(orderRepository, never()).updateSupplierOrderStatusForManagement(
            any(Long.class), any(Long.class), any(Long.class), any(), any(), any(), any(), any()
        );
    }

    @Test
    void developmentSupplierManagementListsOrdersAcrossAllSuppliers() {
        when(orderRepository.listAllSupplierOrders("amber", "in_transit")).thenReturn(List.of());

        assertThat(service.listSupplierOrders("Bearer token", "amber", "in_transit")).isEmpty();

        verify(orderRepository).listAllSupplierOrders("amber", "in_transit");
        verify(orderRepository, never()).listSupplierOrders(any(Long.class), any(), any());
    }

    @Test
    void developmentSupplierManagementCanOperateAnotherSuppliersOrder() {
        LocalDateTime expectedReadyAt = LocalDateTime.of(2026, 7, 22, 9, 0);
        SupplierOrder supplierOrder = new SupplierOrder(
            901L, 26L, "Supplier B", "PENDING_CONFIRMATION", new BigDecimal("20"), null, null, null
        );
        OrderDetail detail = mock(OrderDetail.class);
        when(orderRepository.findSupplierOrderForManagement(900L, 901L)).thenReturn(supplierOrder);
        when(orderRepository.getOrderForSupplierManagement(900L)).thenReturn(detail);

        OrderDetail result = service.actionSupplierOrder(
            "Bearer token", 900L, 901L,
            new SupplierOrderActionRequest("PREPARING", expectedReadyAt, null, null)
        );

        assertThat(result).isSameAs(detail);
        verify(orderRepository).updateSupplierOrderStatusForManagement(
            900L, 901L, 7L, "PENDING_CONFIRMATION", "PREPARING", expectedReadyAt, null, null
        );
    }

    @Test
    void productionSupplierManagementKeepsCompanyScopeForListAndAction() {
        FoodProcurementApplicationService productionService = new FoodProcurementApplicationService(
            currentUserService, authRepository, spreadsheetParser, new FoodItemNormalizer(), demandRepository,
            quoteRepository, orderRepository, new ObjectMapper().findAndRegisterModules(), false
        );
        when(orderRepository.listSupplierOrders(11L, "amber", "in_transit")).thenReturn(List.of());
        LocalDateTime expectedReadyAt = LocalDateTime.of(2026, 7, 22, 9, 0);
        SupplierOrder supplierOrder = new SupplierOrder(
            901L, 11L, "Supplier A", "PENDING_CONFIRMATION", new BigDecimal("20"), null, null, null
        );
        OrderDetail detail = mock(OrderDetail.class);
        when(orderRepository.findSupplierOrder(900L, 901L, 11L)).thenReturn(supplierOrder);
        when(orderRepository.getOrder(900L, 11L)).thenReturn(detail);

        assertThat(productionService.listSupplierOrders("Bearer token", "amber", "in_transit")).isEmpty();
        assertThat(productionService.actionSupplierOrder(
            "Bearer token", 900L, 901L,
            new SupplierOrderActionRequest("PREPARING", expectedReadyAt, null, null)
        )).isSameAs(detail);

        verify(orderRepository).listSupplierOrders(11L, "amber", "in_transit");
        verify(orderRepository).updateSupplierOrderStatus(
            900L, 901L, 11L, 7L, "PENDING_CONFIRMATION", "PREPARING", expectedReadyAt, null, null
        );
        verify(orderRepository, never()).listAllSupplierOrders(any(), any());
        verify(orderRepository, never()).updateSupplierOrderStatusForManagement(
            any(Long.class), any(Long.class), any(Long.class), any(), any(), any(), any(), any()
        );
    }

    @Test
    void developmentSupplierSettlementListUsesGlobalScope() {
        when(orderRepository.listAllSupplierSettlements("PENDING_INVOICE")).thenReturn(List.of());

        assertThat(service.listSupplierSettlements("Bearer token", "pending_invoice")).isEmpty();

        verify(orderRepository).listAllSupplierSettlements("PENDING_INVOICE");
        verify(orderRepository, never()).listSupplierSettlements(any(Long.class), any());
    }

    @Test
    void developmentSupplierSettlementActionCanOperateAnotherSupplierAndStoresInvoiceData() {
        BigDecimal actualAmount = new BigDecimal("128.50");
        AttachmentPayload attachment = new AttachmentPayload("file-1", "invoice.pdf", null);
        SettlementSummary updated = new SettlementSummary(
            51L, 900L, "FPO-1", 901L, "Supplier B", "Buyer A", "MV AMBER", "USD", "INVOICED",
            new BigDecimal("130.00"), actualAmount, "INV-1", "file-1",
            List.of(new AttachmentPayload("file-1", "invoice.pdf", "/api/files/file-1")), LocalDateTime.now()
        );
        when(orderRepository.updateSettlementForManagement(
            eq(51L), eq("INVOICED"), eq("INV-1"), eq("file-1"), eq(actualAmount), anyList()
        )).thenReturn(updated);

        SettlementSummary result = service.updateSupplierSettlement(
            "Bearer token", 51L,
            new SettlementActionRequest("INVOICED", "INV-1", null, actualAmount, List.of(attachment))
        );

        assertThat(result).isSameAs(updated);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttachmentPayload>> attachments = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).updateSettlementForManagement(
            eq(51L), eq("INVOICED"), eq("INV-1"), eq("file-1"), eq(actualAmount), attachments.capture()
        );
        assertThat(attachments.getValue()).containsExactly(
            new AttachmentPayload("file-1", "invoice.pdf", "/api/files/file-1")
        );
    }

    @Test
    void supplierInvoiceSubmissionRequiresActualAmountAndAttachment() {
        assertThatThrownBy(() -> service.updateSupplierSettlement(
            "Bearer token", 51L,
            new SettlementActionRequest("INVOICED", "INV-1", null, null, List.of())
        )).isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("FOOD_SETTLEMENT_ACTUAL_AMOUNT_REQUIRED");

        verify(orderRepository, never()).updateSettlementForManagement(
            any(Long.class), any(), any(), any(), any(), anyList()
        );
    }

    @Test
    void productionSupplierSettlementActionKeepsSupplierCompanyScope() {
        FoodProcurementApplicationService productionService = new FoodProcurementApplicationService(
            currentUserService, authRepository, spreadsheetParser, new FoodItemNormalizer(), demandRepository,
            quoteRepository, orderRepository, new ObjectMapper().findAndRegisterModules(), false
        );
        BigDecimal actualAmount = new BigDecimal("88.00");
        List<AttachmentPayload> attachments = List.of(
            new AttachmentPayload("file-2", "invoice-2.pdf", "/api/files/file-2")
        );
        SettlementSummary updated = mock(SettlementSummary.class);
        when(orderRepository.updateSupplierSettlement(
            52L, 11L, "INVOICED", "INV-2", "file-2", actualAmount, attachments
        )).thenReturn(updated);

        assertThat(productionService.updateSupplierSettlement(
            "Bearer token", 52L,
            new SettlementActionRequest("INVOICED", "INV-2", null, actualAmount, attachments)
        )).isSameAs(updated);

        verify(orderRepository).updateSupplierSettlement(
            52L, 11L, "INVOICED", "INV-2", "file-2", actualAmount, attachments
        );
        verify(orderRepository, never()).updateSettlementForManagement(
            any(Long.class), any(), any(), any(), any(), anyList()
        );
    }

    private DemandDetail demandDetail(String status) {
        DemandSummary summary = new DemandSummary(
            55L, "FREQ-20260716-001", "RFQ-1", "MV AMBER", "ZHOUSHAN",
            LocalDateTime.of(2026, 7, 20, 9, 0), "USD", status, 1, 1, 0, 2, 2,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(4), LocalDateTime.now()
        );
        DemandItem item = new DemandItem(501L, 1, "Fresh Apple", "\u82f9\u679c", null, "10*500G", "KG", new BigDecimal("10"), "MATCHED", null);
        return new DemandDetail(summary, "food.xlsx", "Quotation", List.of(item));
    }

    private OrderCreateRequest orderRequest(
        String strategyType, boolean allowPartial, Long supplierCompanyId, List<SelectedQuoteItem> selectedItems
    ) {
        return new OrderCreateRequest(
            strategyType, allowPartial, supplierCompanyId, selectedItems,
            LocalDateTime.of(2026, 7, 20, 9, 0), "Buyer", "13800000000", "buyer@example.com",
            "UNIFIED_PACKAGING", "Deliver to port"
        );
    }

    private OrderableQuoteItem orderable(long quoteItemId, long supplierId, String supplier, String quantity, String price) {
        BigDecimal quoted = new BigDecimal(quantity);
        BigDecimal unitPrice = new BigDecimal(price);
        return new OrderableQuoteItem(
            quoteItemId, 501L, quoteItemId + 1000, supplierId, supplier, "Fresh Apple", "\u82f9\u679c", "10*500G", "KG",
            new BigDecimal("10"), quoted, unitPrice, quoted.multiply(unitPrice), null, null
        );
    }

    private ComparisonQuoteOption comparisonOption(long quoteItemId, long supplierId, String supplier, String price) {
        BigDecimal unitPrice = new BigDecimal(price);
        return new ComparisonQuoteOption(
            501L, quoteItemId, quoteItemId + 1000, supplierId, supplier, new BigDecimal("10"), new BigDecimal("10"),
            unitPrice, unitPrice.multiply(new BigDecimal("10")), "AVAILABLE", "MANUAL", true, false
        );
    }
}
