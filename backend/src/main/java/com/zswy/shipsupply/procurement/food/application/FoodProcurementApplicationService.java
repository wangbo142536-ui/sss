package com.zswy.shipsupply.procurement.food.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonQuoteOption;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonSettings;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonStrategy;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandItemPayload;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSaveRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSaveResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationReviewRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSubmitRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.AttachmentPayload;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ImportIssue;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ImportUpdate;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.InquirySummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.MatchPreviewResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderCreateRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderCreateResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.PreviewItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteImportCommitResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteImportPreviewResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteItemUpdate;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteSaveRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SelectedQuoteItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SendInquiryRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SendInquiryResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementActionRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SheetOption;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierOrder;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierOrderActionRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierOrderSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.VirtualFillResponse;
import com.zswy.shipsupply.procurement.food.domain.FoodItemNormalizer;
import com.zswy.shipsupply.procurement.food.domain.FoodOrderStatus;
import com.zswy.shipsupply.procurement.food.domain.FoodSpreadsheetDocument;
import com.zswy.shipsupply.procurement.food.domain.FoodSpreadsheetRow;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodDemandRepository;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodOrderRepository;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodOrderRepository.OrderableQuoteItem;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodQuoteRepository;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodQuoteRepository.ImportBatch;
import com.zswy.shipsupply.procurement.food.infrastructure.spreadsheet.FoodSpreadsheetParser;

@Service
public class FoodProcurementApplicationService {

    private static final int MAX_COMPARISON_SUPPLIER_LIMIT = 10;

    private final CurrentUserService currentUserService;
    private final AuthRepository authRepository;
    private final FoodSpreadsheetParser spreadsheetParser;
    private final FoodItemNormalizer normalizer;
    private final FoodDemandRepository demandRepository;
    private final FoodQuoteRepository quoteRepository;
    private final FoodOrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final boolean developmentOpenSupplierScope;

    public FoodProcurementApplicationService(
        CurrentUserService currentUserService,
        AuthRepository authRepository,
        FoodSpreadsheetParser spreadsheetParser,
        FoodItemNormalizer normalizer,
        FoodDemandRepository demandRepository,
        FoodQuoteRepository quoteRepository,
        FoodOrderRepository orderRepository,
        ObjectMapper objectMapper,
        @Value("${ship-supply.procurement.food.development-open-supplier-scope:false}") boolean developmentOpenSupplierScope
    ) {
        this.currentUserService = currentUserService;
        this.authRepository = authRepository;
        this.spreadsheetParser = spreadsheetParser;
        this.normalizer = normalizer;
        this.demandRepository = demandRepository;
        this.quoteRepository = quoteRepository;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.developmentOpenSupplierScope = developmentOpenSupplierScope;
    }

    public MatchPreviewResponse preview(String authorizationHeader, MultipartFile file, String sheetName) {
        currentUserService.requireActiveCompanyUser(authorizationHeader);
        FoodSpreadsheetDocument document = spreadsheetParser.parse(file, sheetName);
        List<PreviewItem> items = document.rows().stream().map(this::previewItem).toList();
        int matched = (int) items.stream().filter(item -> "MATCHED".equals(item.matchStatus())).count();
        return new MatchPreviewResponse(
            document.fileName(), document.sheetName(), document.headerRow(), items.size(), matched, items.size() - matched,
            document.sheets().stream().map(sheet -> new SheetOption(sheet.name(), sheet.headerRow(), sheet.validRows())).toList(), items
        );
    }

    @Transactional
    public DemandSaveResponse saveDemand(String authorizationHeader, DemandSaveRequest request) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        validateDemand(request);
        List<Long> supplierIds = demandRepository.listSuppliers(user.companyId()).stream()
            .map(SupplierSummary::supplierCompanyId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
        if (supplierIds.isEmpty()) {
            throw badRequest("FOOD_SUPPLIER_REQUIRED");
        }
        int matched = (int) request.items().stream().filter(item -> "MATCHED".equals(item.matchStatus())).count();
        int pending = request.items().size() - matched;
        long demandId = translateConflict(() -> {
            if (request.demandId() == null) {
                return demandRepository.insertDemand(user.companyId(), user.userId(), request, matched, pending);
            }
            demandRepository.updateDemand(request.demandId(), user.companyId(), user.userId(), request, matched, pending);
            return request.demandId();
        });
        demandRepository.insertItems(demandId, request.items());
        translateConflict(() -> demandRepository.insertInquirySuppliers(
            demandId, user.companyId(), user.userId(), supplierIds, request.quoteDeadlineAt()
        ));
        quoteRepository.ensureQuotesForDemand(demandId, user.userId());
        DemandDetail detail = requireDemand(demandId, user.companyId());
        return new DemandSaveResponse(
            demandId,
            detail.demand().demandNo(),
            detail.demand().inquiryNo(),
            detail.demand().status(),
            supplierIds.size()
        );
    }

    public List<DemandSummary> listDemands(String authorizationHeader, String keyword, String status) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return demandRepository.listDemands(user.companyId(), keyword, status);
    }

    public DemandDetail getDemand(String authorizationHeader, long demandId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return requireDemand(demandId, user.companyId());
    }

    public List<SupplierSummary> listSuppliers(String authorizationHeader) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return demandRepository.listSuppliers(user.companyId());
    }

    @Transactional
    public SendInquiryResponse sendInquiry(String authorizationHeader, long demandId, SendInquiryRequest request) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        DemandDetail demand = requireDemand(demandId, user.companyId());
        List<Long> requestedSupplierIds = request == null || request.supplierCompanyIds() == null
            ? List.of() : request.supplierCompanyIds().stream().filter(id -> id != null && id > 0).distinct().toList();
        Set<Long> quotableSupplierIds = demandRepository.listSuppliers(user.companyId()).stream()
            .map(SupplierSummary::supplierCompanyId)
            .collect(Collectors.toSet());
        List<Long> supplierIds = requestedSupplierIds.stream().filter(quotableSupplierIds::contains).toList();
        if (supplierIds.isEmpty()) {
            throw badRequest("FOOD_SUPPLIER_REQUIRED");
        }
        int quoteValidityDays = request.quoteValidityDays() == null ? 3 : request.quoteValidityDays();
        if (quoteValidityDays < 1 || quoteValidityDays > 30) {
            throw badRequest("FOOD_QUOTE_VALIDITY_DAYS_INVALID");
        }
        if (!List.of("DRAFT", "INQUIRY_SENT").contains(demand.demand().status())) {
            throw conflict("FOOD_DEMAND_CANNOT_SEND_INQUIRY");
        }
        translateConflict(() -> demandRepository.insertInquirySuppliers(
            demandId, user.companyId(), user.userId(), supplierIds, quoteValidityDays
        ));
        quoteRepository.ensureQuotesForDemand(demandId, user.userId());
        return new SendInquiryResponse(demandId, supplierIds.size(), "INQUIRY_SENT");
    }

    public List<InquirySummary> listBuyerInquiries(String authorizationHeader, String keyword, String status) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return demandRepository.listBuyerInquiries(user.companyId(), keyword, status);
    }

    public List<InquirySummary> listSupplierInquiries(String authorizationHeader, String keyword, String status) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return demandRepository.listSupplierInquiries(user.companyId(), keyword, status);
    }

    public QuoteDetail getQuote(String authorizationHeader, long quoteId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        QuoteDetail quote = quoteRepository.getQuote(quoteId, user.companyId());
        if (quote == null) throw notFound("FOOD_QUOTE_NOT_FOUND");
        return quote;
    }

    @Transactional
    public QuoteDetail saveQuote(String authorizationHeader, long quoteId, QuoteSaveRequest request) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireParticipantQuote(quoteId, user.companyId());
        List<QuoteItemUpdate> updates = request == null || request.items() == null ? List.of() : request.items();
        validateQuoteUpdates(updates);
        translateConflict(() -> quoteRepository.saveQuoteItems(quoteId, user.companyId(), user.userId(), updates));
        return requireParticipantQuote(quoteId, user.companyId());
    }

    @Transactional
    public VirtualFillResponse virtualFill(String authorizationHeader, long quoteId, boolean overwriteExisting) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        QuoteDetail before = requireParticipantQuote(quoteId, user.companyId());
        long existing = before.items().stream().filter(item -> item.unitPrice() != null).count();
        translateConflict(() -> quoteRepository.virtualFill(quoteId, user.companyId(), user.userId(), overwriteExisting));
        QuoteDetail after = requireParticipantQuote(quoteId, user.companyId());
        long filled = after.items().stream().filter(item -> "VIRTUAL".equals(item.priceSource())).count();
        return new VirtualFillResponse(quoteId, Math.toIntExact(filled), overwriteExisting ? 0 : Math.toIntExact(existing), "VIRTUAL");
    }

    @Transactional
    public QuoteDetail submitQuote(String authorizationHeader, long quoteId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireParticipantQuote(quoteId, user.companyId());
        translateConflict(() -> quoteRepository.submitQuote(quoteId, user.companyId()));
        return requireParticipantQuote(quoteId, user.companyId());
    }

    @Transactional
    public QuoteImportPreviewResponse previewQuoteImport(
        String authorizationHeader, long quoteId, MultipartFile file, String sheetName
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        QuoteDetail quote = requireParticipantQuote(quoteId, user.companyId());
        if (!"DRAFT".equals(quote.status())) throw conflict("FOOD_QUOTE_NOT_EDITABLE");
        FoodSpreadsheetDocument document = spreadsheetParser.parse(file, sheetName);
        Map<String, List<QuoteItem>> candidates = quote.items().stream().collect(Collectors.groupingBy(
            item -> normalizer.matchKey(item.nameZh(), item.nameEn(), item.specification(), item.unit()),
            LinkedHashMap::new, Collectors.toList()
        ));
        Set<Long> used = new HashSet<>();
        List<ImportUpdate> updates = new ArrayList<>();
        List<ImportIssue> issues = new ArrayList<>();
        for (FoodSpreadsheetRow row : document.rows()) {
            List<QuoteItem> matches = candidates.getOrDefault(row.matchKey(), List.of()).stream()
                .filter(item -> !used.contains(item.quoteItemId())).toList();
            if (!"MATCHED".equals(row.matchStatus()) || row.unitPrice() == null) {
                issues.add(importIssue(row, "INVALID_ROW", row.unitPrice() == null ? "FOOD_IMPORT_PRICE_REQUIRED" : row.matchReason()));
            } else if (matches.isEmpty()) {
                issues.add(importIssue(row, "UNMATCHED", "FOOD_IMPORT_NAME_SPEC_UNIT_NOT_FOUND"));
            } else if (matches.size() > 1) {
                issues.add(importIssue(row, "AMBIGUOUS", "FOOD_IMPORT_MULTIPLE_MATCHES"));
            } else {
                QuoteItem match = matches.get(0);
                used.add(match.quoteItemId());
                updates.add(new ImportUpdate(row.sourceRow(), match.quoteItemId(), match.demandItemId(), row.quantity(), row.unitPrice(), "EXACT"));
            }
        }
        QuoteImportPreviewResponse stored = new QuoteImportPreviewResponse(
            null, document.fileName(), document.sheetName(), updates.size(), issues.size(), updates, issues
        );
        long batchId = quoteRepository.saveImportBatch(
            quoteId, document.fileName(), toJson(stored), updates.size(), issues.size(), user.userId()
        );
        return new QuoteImportPreviewResponse(
            batchId, stored.fileName(), stored.selectedSheet(), stored.matchedCount(), stored.issueCount(), updates, issues
        );
    }

    @Transactional
    public QuoteImportCommitResponse commitQuoteImport(String authorizationHeader, long quoteId, long batchId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        QuoteDetail quote = requireParticipantQuote(quoteId, user.companyId());
        ImportBatch batch = quoteRepository.getImportBatch(batchId, quoteId);
        if (batch == null) throw notFound("FOOD_QUOTE_IMPORT_NOT_FOUND");
        QuoteImportPreviewResponse preview = fromJson(batch.previewJson(), QuoteImportPreviewResponse.class);
        Map<Long, BigDecimal> requestedQuantities = quote.items().stream().collect(Collectors.toMap(
            QuoteItem::quoteItemId, QuoteItem::requestedQuantity
        ));
        List<QuoteItemUpdate> updates = preview.updates().stream().map(update -> new QuoteItemUpdate(
            update.quoteItemId(), requestedQuantities.get(update.quoteItemId()), update.unitPrice(), "AVAILABLE", null, "IMPORTED"
        )).toList();
        int updated = translateConflict(() -> quoteRepository.commitImport(
            batchId, quoteId, user.companyId(), user.userId(), updates
        ));
        return new QuoteImportCommitResponse(batchId, quoteId, updated, quoteRepository.quoteVersion(quoteId));
    }

    public ComparisonResponse comparison(String authorizationHeader, long demandId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        DemandDetail demand = requireDemand(demandId, user.companyId());
        ComparisonSettings comparisonSettings = normalizedComparisonSettings(
            demandRepository.getComparisonSettings(demandId, user.companyId())
        );
        List<ComparisonQuoteOption> allOptions = quoteRepository.comparisonOptions(demandId);
        Set<Long> candidateSupplierIds = topComparisonSupplierIds(allOptions, comparisonSettings.mixedSupplierCount());
        Map<Long, List<ComparisonQuoteOption>> byItem = allOptions.stream()
            .filter(option -> candidateSupplierIds.contains(option.supplierCompanyId()))
            .collect(Collectors.groupingBy(ComparisonQuoteOption::demandItemId, LinkedHashMap::new, Collectors.toList()));
        List<ComparisonItem> items = demand.items().stream().map(item -> comparisonItem(item, byItem.getOrDefault(item.itemId(), List.of()))).toList();
        return new ComparisonResponse(
            demand.demand(), quoteRepository.submittedSupplierCount(demandId), comparisonStrategies(items), items,
            comparisonSettings
        );
    }

    @Transactional
    public ComparisonSettings saveComparisonSettings(
        String authorizationHeader, long demandId, ComparisonSettings request
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        DemandDetail demand = requireDemand(demandId, user.companyId());
        if ("ORDERED".equalsIgnoreCase(safe(demand.demand().status()))) {
            throw conflict("FOOD_COMPARISON_LOCKED");
        }
        ComparisonSettings settings = normalizedComparisonSettings(request);
        Set<Long> validDemandItemIds = demand.items().stream().map(DemandItem::itemId).collect(Collectors.toSet());
        if (settings.selectedDemandItemIds() != null) {
            if (!validDemandItemIds.containsAll(settings.selectedDemandItemIds())) {
                throw badRequest("FOOD_COMPARISON_SELECTION_INVALID");
            }
        }
        if (!validDemandItemIds.containsAll(settings.coreDemandItemIds())) {
            throw badRequest("FOOD_COMPARISON_CORE_ITEM_INVALID");
        }
        if (demandRepository.updateComparisonSettings(demandId, user.companyId(), user.userId(), settings) != 1) {
            throw notFound("FOOD_DEMAND_NOT_FOUND");
        }
        return settings;
    }

    @Transactional
    public OrderCreateResponse createOrder(String authorizationHeader, long demandId, OrderCreateRequest request) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        DemandDetail demand = requireDemand(demandId, user.companyId());
        if (request == null) throw badRequest("FOOD_ORDER_REQUEST_REQUIRED");
        if (request.requiredDeliveryTime() == null) throw badRequest("FOOD_REQUIRED_DELIVERY_TIME_REQUIRED");
        if (safe(request.deliveryContactName()).isBlank()) throw badRequest("FOOD_DELIVERY_CONTACT_NAME_REQUIRED");
        if (safe(request.deliveryContactPhone()).isBlank()) throw badRequest("FOOD_DELIVERY_CONTACT_PHONE_REQUIRED");
        if (safe(request.defaultPackagingMethod()).isBlank()) throw badRequest("FOOD_PACKAGING_METHOD_REQUIRED");
        if (orderRepository.orderExistsForDemand(demandId)) throw conflict("FOOD_ORDER_ALREADY_EXISTS");
        List<OrderableQuoteItem> available = orderRepository.orderableItems(demandId);
        if (available.isEmpty()) throw conflict("FOOD_ORDER_NO_SUBMITTED_QUOTES");
        Set<Long> candidateSupplierIds = topOrderSupplierIds(available);
        available = available.stream().filter(item -> candidateSupplierIds.contains(item.supplierCompanyId())).toList();
        available = selectedOrderableItems(available, request.selectedItems());
        if (available.isEmpty()) throw conflict("FOOD_ORDER_SELECTED_ITEMS_EMPTY");
        String strategy = safe(request.strategyType()).toUpperCase(Locale.ROOT);
        List<OrderableQuoteItem> selected = switch (strategy) {
            case "LOWEST_ITEM" -> selectLowest(available, request.allowPartial());
            case "SINGLE_SUPPLIER" -> selectSingleSupplier(
                available, request.allowPartial(), request.selectedSupplierCompanyId()
            );
            default -> throw badRequest("FOOD_ORDER_STRATEGY_INVALID");
        };
        int demandItemCount = demand.items().size();
        int selectedItemCount = (int) selected.stream().map(OrderableQuoteItem::demandItemId).distinct().count();
        if (!request.allowPartial() && selectedItemCount != demandItemCount) throw conflict("FOOD_ORDER_ITEMS_INCOMPLETE");
        ComparisonSettings settings = normalizedComparisonSettings(
            demandRepository.getComparisonSettings(demandId, user.companyId())
        );
        List<OrderableQuoteItem> normalized = selected.stream()
            .map(this::normalizeOrderedItem)
            .map(item -> applyMarkup(item, settings.markupPercent()))
            .toList();
        BigDecimal cost = normalized.stream().map(OrderableQuoteItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal quoted = normalized.stream().map(OrderableQuoteItem::quotedAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);
        BigDecimal fixedFees = settings.fixedFreightFee().add(settings.fixedCustomsFee())
            .add(settings.fixedCraneFee()).add(settings.fixedOtherFee());
        BigDecimal profit = quoted.subtract(cost).setScale(4, RoundingMode.HALF_UP);
        BigDecimal total = quoted.add(fixedFees).setScale(4, RoundingMode.HALF_UP);
        long orderId = orderRepository.insertOrder(
            demandId, user.companyId(), user.userId(), strategy, demand.demand().currency(),
            total, cost, quoted, profit, settings
        );
        orderRepository.updateOrderDeliveryInfo(orderId, request);
        Map<Long, Long> supplierOrderIds = orderRepository.insertSupplierOrders(orderId, normalized);
        orderRepository.insertOrderItems(orderId, supplierOrderIds, normalized);
        orderRepository.finalizeDemandAfterOrder(demandId, user.userId());
        return new OrderCreateResponse(orderId, orderRepository.orderNo(orderId), FoodOrderStatus.PENDING_CONFIRMATION, total);
    }

    public List<OrderSummary> listBuyerOrders(String authorizationHeader, String keyword, String status) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return orderRepository.listBuyerOrders(user.companyId(), keyword, status);
    }

    public List<SupplierOrderSummary> listSupplierOrders(String authorizationHeader, String keyword, String status) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (developmentOpenSupplierScope) return orderRepository.listAllSupplierOrders(keyword, status);
        return orderRepository.listSupplierOrders(user.companyId(), keyword, status);
    }

    public OrderDetail getOrder(String authorizationHeader, long orderId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        OrderDetail order = orderRepository.getOrder(orderId, user.companyId());
        if (order == null) throw notFound("FOOD_ORDER_NOT_FOUND");
        return order;
    }

    public OrderDetail getSupplierOrder(String authorizationHeader, long orderId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        OrderDetail order = developmentOpenSupplierScope
            ? orderRepository.getOrderForSupplierManagement(orderId)
            : orderRepository.getOrder(orderId, user.companyId());
        if (order == null) throw notFound("FOOD_ORDER_NOT_FOUND");
        return order;
    }

    @Transactional
    public OrderDetail actionSupplierOrder(
        String authorizationHeader, long orderId, long supplierOrderId, SupplierOrderActionRequest request
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        SupplierOrder supplierOrder = developmentOpenSupplierScope
            ? orderRepository.findSupplierOrderForManagement(orderId, supplierOrderId)
            : orderRepository.findSupplierOrder(orderId, supplierOrderId, user.companyId());
        if (supplierOrder == null) throw notFound("FOOD_SUPPLIER_ORDER_NOT_FOUND");
        String target = safe(request == null ? null : request.targetStatus()).toUpperCase(Locale.ROOT);
        FoodOrderStatus.requireTransition(supplierOrder.status(), target);
        if (List.of(FoodOrderStatus.CONFIRMED, FoodOrderStatus.PREPARING).contains(target)
            && request.expectedReadyAt() == null) {
            throw badRequest("FOOD_EXPECTED_READY_AT_REQUIRED");
        }
        if (FoodOrderStatus.REJECTED.equals(target) && safe(request.rejectReason()).isBlank()) {
            throw badRequest("FOOD_REJECT_REASON_REQUIRED");
        }
        if (developmentOpenSupplierScope) {
            translateConflict(() -> orderRepository.updateSupplierOrderStatusForManagement(
                orderId, supplierOrderId, user.userId(), supplierOrder.status(), target,
                request.expectedReadyAt(), request.rejectReason(), request.shipmentRemark()
            ));
        } else {
            orderRepository.updateSupplierOrderStatus(
                orderId, supplierOrderId, user.companyId(), user.userId(), supplierOrder.status(), target,
                request.expectedReadyAt(), request.rejectReason(), request.shipmentRemark()
            );
        }
        return getSupplierOrder(authorizationHeader, orderId);
    }

    public List<SettlementSummary> listBuyerSettlements(String authorizationHeader, String status) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return orderRepository.listBuyerSettlements(user.companyId(), status);
    }

    public List<SettlementSummary> listSupplierSettlements(String authorizationHeader, String status) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        if (developmentOpenSupplierScope) return orderRepository.listAllSupplierSettlements(normalizedStatus);
        return orderRepository.listSupplierSettlements(user.companyId(), normalizedStatus);
    }

    @Transactional
    public SettlementSummary updateSettlement(String authorizationHeader, long id, SettlementActionRequest request) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return updateSettlement(user, id, request, false);
    }

    @Transactional
    public SettlementSummary updateSupplierSettlement(String authorizationHeader, long id, SettlementActionRequest request) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return updateSettlement(user, id, request, true);
    }

    private SettlementSummary updateSettlement(
        CurrentUserContext user, long id, SettlementActionRequest request, boolean supplierAction
    ) {
        if (request == null || safe(request.targetStatus()).isBlank()) throw badRequest("FOOD_SETTLEMENT_STATUS_REQUIRED");
        String targetStatus = request.targetStatus().toUpperCase(Locale.ROOT);
        if (supplierAction && !"INVOICED".equals(targetStatus)) {
            throw badRequest("FOOD_SUPPLIER_SETTLEMENT_STATUS_INVALID");
        }
        if (!supplierAction && !List.of("SETTLED", "PAID").contains(targetStatus)) {
            throw badRequest("FOOD_BUYER_SETTLEMENT_STATUS_INVALID");
        }
        BigDecimal actualAmount = request.actualAmount();
        if (actualAmount != null && actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("FOOD_SETTLEMENT_ACTUAL_AMOUNT_INVALID");
        }
        List<AttachmentPayload> attachments = normalizedSettlementAttachments(request);
        if ("INVOICED".equals(targetStatus)) {
            if (actualAmount == null) throw badRequest("FOOD_SETTLEMENT_ACTUAL_AMOUNT_REQUIRED");
            if (attachments == null || attachments.isEmpty()) throw badRequest("FOOD_SETTLEMENT_INVOICE_ATTACHMENT_REQUIRED");
        }
        String invoiceFileId = safe(request.invoiceFileId());
        if (invoiceFileId.isBlank() && attachments != null && !attachments.isEmpty()) {
            invoiceFileId = safe(attachments.get(0).fileId());
        }
        String compatibleInvoiceFileId = invoiceFileId.isBlank() ? null : invoiceFileId;
        SettlementSummary result = translateConflict(() -> developmentOpenSupplierScope && supplierAction
            ? orderRepository.updateSettlementForManagement(
                id, targetStatus, request.invoiceNo(), compatibleInvoiceFileId, actualAmount, attachments
            )
            : supplierAction
                ? orderRepository.updateSupplierSettlement(
                    id, user.companyId(), targetStatus, request.invoiceNo(), compatibleInvoiceFileId, actualAmount, attachments
                )
                : orderRepository.updateSettlement(
                    id, user.companyId(), targetStatus, request.invoiceNo(), compatibleInvoiceFileId, actualAmount, attachments
                ));
        if (result == null) throw notFound("FOOD_SETTLEMENT_NOT_FOUND");
        return result;
    }

    public List<EvaluationSummary> listEvaluations(String authorizationHeader, String status) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return orderRepository.listEvaluations(user.companyId(), status);
    }

    public List<EvaluationSummary> listRegulatoryEvaluations(
        String authorizationHeader, String keyword, String status
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireRegulatory(user.userId());
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        if (!normalizedStatus.isBlank()
            && !List.of("PENDING_REVIEW", "APPROVED", "REJECTED").contains(normalizedStatus)) {
            throw badRequest("FOOD_EVALUATION_REGULATORY_STATUS_INVALID");
        }
        return orderRepository.listRegulatoryEvaluations(keyword, normalizedStatus);
    }

    @Transactional
    public EvaluationSummary submitEvaluation(String authorizationHeader, long id, EvaluationSubmitRequest request) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (request == null || request.qualityRating() < 1 || request.qualityRating() > 5
            || request.logisticsRating() < 1 || request.logisticsRating() > 5) {
            throw badRequest("FOOD_EVALUATION_RATING_INVALID");
        }
        List<AttachmentPayload> attachments = normalizedEvaluationAttachments(request.attachments());
        EvaluationSummary result = orderRepository.submitEvaluation(
            id, user.companyId(), request.qualityRating(), request.logisticsRating(), request.comment(), attachments
        );
        if (result == null) throw notFound("FOOD_EVALUATION_NOT_FOUND");
        return result;
    }

    @Transactional
    public EvaluationSummary reviewEvaluation(String authorizationHeader, long id, EvaluationReviewRequest request) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (request == null) throw badRequest("FOOD_EVALUATION_REVIEW_REQUIRED");
        EvaluationSummary result = translateConflict(() -> orderRepository.reviewEvaluation(
            id, user.companyId(), safe(request.targetStatus()).toUpperCase(Locale.ROOT), request.reviewRemark()
        ));
        if (result == null) throw notFound("FOOD_EVALUATION_NOT_FOUND");
        return result;
    }

    @Transactional
    public EvaluationSummary reviewRegulatoryEvaluation(
        String authorizationHeader, long id, String targetStatus, String reviewRemark
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireRegulatory(user.userId());
        String normalizedStatus = safe(targetStatus).toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED").contains(normalizedStatus)) {
            throw badRequest("FOOD_EVALUATION_INVALID_REVIEW_STATUS");
        }
        if ("REJECTED".equals(normalizedStatus) && safe(reviewRemark).isBlank()) {
            throw badRequest("FOOD_EVALUATION_REVIEW_REMARK_REQUIRED");
        }
        EvaluationSummary result = orderRepository.reviewRegulatoryEvaluation(
            id, user.userId(), normalizedStatus, reviewRemark
        );
        if (result == null) throw conflict("FOOD_EVALUATION_NOT_REVIEWABLE");
        return result;
    }

    private PreviewItem previewItem(FoodSpreadsheetRow row) {
        return new PreviewItem(
            row.sourceRow(), row.sequenceNo(), row.nameEn(), row.nameZh(), row.remark(), row.specification(), row.unit(),
            row.quantity(), row.matchStatus(), row.matchReason(), row.matchKey(), row.rawColumns()
        );
    }

    private void validateDemand(DemandSaveRequest request) {
        if (request == null) throw badRequest("FOOD_DEMAND_REQUIRED");
        if (safe(request.vesselName()).isBlank()) throw badRequest("FOOD_VESSEL_REQUIRED");
        if (safe(request.supplyPort()).isBlank()) throw badRequest("FOOD_SUPPLY_PORT_REQUIRED");
        if (request.vesselEta() == null) throw badRequest("FOOD_VESSEL_ETA_REQUIRED");
        if (request.quoteDeadlineAt() == null) throw badRequest("FOOD_QUOTE_DEADLINE_REQUIRED");
        if (request.items() == null || request.items().isEmpty()) throw badRequest("FOOD_DEMAND_ITEMS_REQUIRED");
        Set<Integer> sequences = new HashSet<>();
        for (DemandItemPayload item : request.items()) {
            if ((!sequences.add(item.sequenceNo())) || (safe(item.nameEn()).isBlank() && safe(item.nameZh()).isBlank())
                || safe(item.unit()).isBlank() || item.requestedQuantity() == null || item.requestedQuantity().signum() <= 0) {
                throw badRequest("FOOD_DEMAND_ITEM_INVALID");
            }
        }
    }

    private void validateQuoteUpdates(List<QuoteItemUpdate> updates) {
        if (updates.isEmpty()) throw badRequest("FOOD_QUOTE_ITEMS_REQUIRED");
        Set<Long> ids = new HashSet<>();
        for (QuoteItemUpdate item : updates) {
            if (item.quoteItemId() == null || !ids.add(item.quoteItemId())) throw badRequest("FOOD_QUOTE_ITEM_DUPLICATE");
            String availability = safe(item.availability()).toUpperCase(Locale.ROOT);
            if (!List.of("AVAILABLE", "PARTIAL", "UNAVAILABLE").contains(availability)) throw badRequest("FOOD_QUOTE_AVAILABILITY_INVALID");
            if (!"UNAVAILABLE".equals(availability)
                && (item.quotedQuantity() == null || item.quotedQuantity().signum() <= 0 || item.unitPrice() == null || item.unitPrice().signum() < 0)) {
                throw badRequest("FOOD_QUOTE_VALUE_INVALID");
            }
        }
    }

    private DemandDetail requireDemand(long demandId, long companyId) {
        DemandDetail demand = demandRepository.getDemand(demandId, companyId);
        if (demand == null) throw notFound("FOOD_DEMAND_NOT_FOUND");
        return demand;
    }

    private void requireRegulatory(long userId) {
        boolean allowed = authRepository.roleCodesForUser(userId).stream()
            .map(code -> code == null ? "" : code.toUpperCase(Locale.ROOT))
            .anyMatch(code ->
                "PLATFORM_ADMIN".equals(code)
                    || code.contains("REGULATORY")
                    || code.startsWith("COMPANY_ADMIN_")
                    || "SHIP_AGENT".equals(code)
            );
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "REGULATORY_ROLE_REQUIRED");
    }

    private QuoteDetail requireParticipantQuote(long quoteId, long companyId) {
        if (!quoteRepository.quoteBelongsToCompany(quoteId, companyId)) throw notFound("FOOD_QUOTE_NOT_FOUND");
        QuoteDetail quote = quoteRepository.getQuote(quoteId, companyId);
        if (quote == null) throw notFound("FOOD_QUOTE_NOT_FOUND");
        return quote;
    }

    private ImportIssue importIssue(FoodSpreadsheetRow row, String type, String message) {
        return new ImportIssue(row.sourceRow(), row.nameEn(), row.nameZh(), row.specification(), row.unit(), type, message);
    }

    private ComparisonItem comparisonItem(DemandItem item, List<ComparisonQuoteOption> source) {
        BigDecimal lowest = source.stream().filter(ComparisonQuoteOption::quantitySatisfied)
            .map(ComparisonQuoteOption::unitPrice).min(BigDecimal::compareTo).orElse(null);
        List<ComparisonQuoteOption> options = source.stream().map(option -> new ComparisonQuoteOption(
            option.demandItemId(), option.quoteItemId(), option.quoteId(), option.supplierCompanyId(), option.supplierName(),
            option.requestedQuantity(), option.quotedQuantity(), option.unitPrice(), option.amount(), option.availability(),
            option.priceSource(), option.quantitySatisfied(), lowest != null && lowest.compareTo(option.unitPrice()) == 0,
            option.productTags()
        )).toList();
        return new ComparisonItem(
            item.itemId(), item.sequenceNo(), item.nameEn(), item.nameZh(), item.specification(), item.unit(),
            item.requestedQuantity(), options
        );
    }

    private List<ComparisonStrategy> comparisonStrategies(List<ComparisonItem> items) {
        List<ComparisonQuoteOption> lowest = items.stream().map(item -> item.quotes().stream()
            .filter(ComparisonQuoteOption::quantitySatisfied).min(Comparator.comparing(ComparisonQuoteOption::unitPrice)).orElse(null))
            .filter(java.util.Objects::nonNull).toList();
        BigDecimal lowestTotal = lowest.stream().map(this::orderedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Long, List<ComparisonQuoteOption>> suppliers = items.stream().flatMap(item -> item.quotes().stream())
            .filter(ComparisonQuoteOption::quantitySatisfied)
            .collect(Collectors.groupingBy(ComparisonQuoteOption::supplierCompanyId));
        List<ComparisonQuoteOption> single = suppliers.values().stream()
            .filter(options -> options.stream().map(ComparisonQuoteOption::demandItemId).distinct().count() == items.size())
            .min(Comparator.comparing(options -> options.stream().map(this::orderedAmount).reduce(BigDecimal.ZERO, BigDecimal::add)))
            .orElse(List.of());
        BigDecimal singleTotal = single.stream().map(this::orderedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        ComparisonQuoteOption first = single.isEmpty() ? null : single.get(0);
        return List.of(
            new ComparisonStrategy("LOWEST_ITEM", "最低混供", lowest.size() == items.size(), lowest.size(), items.size(), lowestTotal, null, null),
            new ComparisonStrategy("SINGLE_SUPPLIER", "集中采购", !single.isEmpty(), single.size(), items.size(), singleTotal,
                first == null ? null : first.supplierCompanyId(), first == null ? null : first.supplierName())
        );
    }

    private Set<Long> topComparisonSupplierIds(List<ComparisonQuoteOption> options, int limit) {
        return options.stream().filter(ComparisonQuoteOption::quantitySatisfied)
            .collect(Collectors.groupingBy(ComparisonQuoteOption::supplierCompanyId))
            .entrySet().stream()
            .map(entry -> {
                Map<Long, ComparisonQuoteOption> cheapestByItem = entry.getValue().stream().collect(Collectors.toMap(
                    ComparisonQuoteOption::demandItemId,
                    option -> option,
                    (left, right) -> left.unitPrice().compareTo(right.unitPrice()) <= 0 ? left : right
                ));
                BigDecimal total = cheapestByItem.values().stream().map(this::orderedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                String name = entry.getValue().isEmpty() ? "" : entry.getValue().get(0).supplierName();
                return new SupplierRank(entry.getKey(), name, cheapestByItem.size(), total);
            })
            .sorted(Comparator.comparingInt(SupplierRank::coveredItemCount).reversed()
                .thenComparing(SupplierRank::totalAmount)
                .thenComparing(SupplierRank::supplierName))
            .limit(Math.max(3, Math.min(MAX_COMPARISON_SUPPLIER_LIMIT, limit)))
            .map(SupplierRank::supplierCompanyId)
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Set<Long> topOrderSupplierIds(List<OrderableQuoteItem> options) {
        return options.stream()
            .filter(item -> item.quotedQuantity().compareTo(item.requestedQuantity()) >= 0)
            .collect(Collectors.groupingBy(OrderableQuoteItem::supplierCompanyId))
            .entrySet().stream()
            .map(entry -> {
                Map<Long, OrderableQuoteItem> cheapestByItem = entry.getValue().stream().collect(Collectors.toMap(
                    OrderableQuoteItem::demandItemId,
                    item -> item,
                    (left, right) -> left.unitPrice().compareTo(right.unitPrice()) <= 0 ? left : right
                ));
                BigDecimal total = cheapestByItem.values().stream().map(this::orderedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                String name = entry.getValue().isEmpty() ? "" : entry.getValue().get(0).supplierName();
                return new SupplierRank(entry.getKey(), name, cheapestByItem.size(), total);
            })
            .sorted(Comparator.comparingInt(SupplierRank::coveredItemCount).reversed()
                .thenComparing(SupplierRank::totalAmount)
                .thenComparing(SupplierRank::supplierName))
            .limit(MAX_COMPARISON_SUPPLIER_LIMIT)
            .map(SupplierRank::supplierCompanyId)
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private List<OrderableQuoteItem> selectLowest(List<OrderableQuoteItem> available, boolean allowPartial) {
        return available.stream().collect(Collectors.groupingBy(OrderableQuoteItem::demandItemId, LinkedHashMap::new, Collectors.toList()))
            .values().stream().map(options -> options.stream()
                .filter(item -> allowPartial || item.quotedQuantity().compareTo(item.requestedQuantity()) >= 0)
                .min(Comparator.comparing(OrderableQuoteItem::unitPrice)).orElse(null))
            .filter(java.util.Objects::nonNull).toList();
    }

    private List<OrderableQuoteItem> selectedOrderableItems(
        List<OrderableQuoteItem> available,
        List<SelectedQuoteItem> requestedItems
    ) {
        if (requestedItems == null || requestedItems.isEmpty()) return available;
        Map<Long, Long> requestedByDemandItem = new LinkedHashMap<>();
        for (SelectedQuoteItem item : requestedItems) {
            if (item == null || item.demandItemId() == null || item.demandItemId() <= 0
                || item.quoteItemId() == null || item.quoteItemId() <= 0) {
                throw badRequest("FOOD_ORDER_SELECTED_ITEM_INVALID");
            }
            Long previous = requestedByDemandItem.putIfAbsent(item.demandItemId(), item.quoteItemId());
            if (previous != null && !previous.equals(item.quoteItemId())) {
                throw badRequest("FOOD_ORDER_SELECTED_ITEM_DUPLICATE");
            }
        }
        List<OrderableQuoteItem> selected = available.stream()
            .filter(item -> java.util.Objects.equals(
                requestedByDemandItem.get(item.demandItemId()), item.quoteItemId()
            ))
            .toList();
        if (selected.size() != requestedByDemandItem.size()) {
            throw badRequest("FOOD_ORDER_SELECTED_ITEM_INVALID");
        }
        return selected;
    }

    private List<OrderableQuoteItem> selectSingleSupplier(
        List<OrderableQuoteItem> available,
        boolean allowPartial,
        Long selectedSupplierCompanyId
    ) {
        Map<Long, List<OrderableQuoteItem>> suppliers = available.stream().collect(Collectors.groupingBy(OrderableQuoteItem::supplierCompanyId));
        int allItemCount = (int) available.stream().map(OrderableQuoteItem::demandItemId).distinct().count();
        return suppliers.entrySet().stream()
            .filter(entry -> selectedSupplierCompanyId == null || selectedSupplierCompanyId <= 0 || entry.getKey().equals(selectedSupplierCompanyId))
            .map(Map.Entry::getValue)
            .map(items -> items.stream()
                .filter(item -> allowPartial || item.quotedQuantity().compareTo(item.requestedQuantity()) >= 0)
                .collect(Collectors.toMap(OrderableQuoteItem::demandItemId, item -> item,
                    (left, right) -> left.unitPrice().compareTo(right.unitPrice()) <= 0 ? left : right, LinkedHashMap::new))
                .values().stream().toList())
            .filter(items -> allowPartial ? !items.isEmpty() : items.size() == allItemCount)
            .min(Comparator.comparing(items -> items.stream().map(this::orderedAmount).reduce(BigDecimal.ZERO, BigDecimal::add)))
            .orElseThrow(() -> conflict("FOOD_ORDER_SINGLE_SUPPLIER_UNAVAILABLE"));
    }

    private OrderableQuoteItem normalizeOrderedItem(OrderableQuoteItem item) {
        BigDecimal quantity = item.quotedQuantity().min(item.requestedQuantity());
        BigDecimal amount = quantity.multiply(item.unitPrice()).setScale(4, RoundingMode.HALF_UP);
        return new OrderableQuoteItem(
            item.quoteItemId(), item.demandItemId(), item.quoteId(), item.supplierCompanyId(), item.supplierName(),
            item.nameEn(), item.nameZh(), item.specification(), item.unit(), item.requestedQuantity(), quantity,
            item.unitPrice(), amount, null, null
        );
    }

    private OrderableQuoteItem applyMarkup(OrderableQuoteItem item, BigDecimal markupPercent) {
        BigDecimal rate = BigDecimal.ONE.add(markupPercent.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
        BigDecimal quotedUnitPrice = item.unitPrice().multiply(rate).setScale(2, RoundingMode.CEILING);
        BigDecimal quotedAmount = item.quotedQuantity().multiply(quotedUnitPrice).setScale(2, RoundingMode.CEILING);
        return new OrderableQuoteItem(
            item.quoteItemId(), item.demandItemId(), item.quoteId(), item.supplierCompanyId(), item.supplierName(),
            item.nameEn(), item.nameZh(), item.specification(), item.unit(), item.requestedQuantity(), item.quotedQuantity(),
            item.unitPrice(), item.amount(), quotedUnitPrice, quotedAmount
        );
    }

    private BigDecimal orderedAmount(ComparisonQuoteOption option) {
        if (option.quotedQuantity() == null || option.unitPrice() == null) return BigDecimal.ZERO;
        return option.quotedQuantity().min(option.requestedQuantity()).multiply(option.unitPrice());
    }

    private BigDecimal orderedAmount(OrderableQuoteItem item) {
        return item.quotedQuantity().min(item.requestedQuantity()).multiply(item.unitPrice());
    }

    private ComparisonSettings normalizedComparisonSettings(ComparisonSettings value) {
        ComparisonSettings source = value == null
            ? new ComparisonSettings(null, null, null, null, null, null, null, null, null, null, null)
            : value;
        BigDecimal markup = nonNegative(source.markupPercent(), BigDecimal.TEN);
        if (markup.compareTo(BigDecimal.valueOf(1000)) > 0) throw badRequest("FOOD_MARKUP_PERCENT_INVALID");
        String mode = safe(source.supplyMode()).toUpperCase(Locale.ROOT);
        if (mode.isBlank()) mode = "SEA";
        if (!Set.of("SEA", "LAND").contains(mode)) throw badRequest("FOOD_SUPPLY_MODE_INVALID");
        String providerType = safe(source.fixedProviderType()).toUpperCase(Locale.ROOT);
        if (providerType.isBlank()) providerType = "SEA".equals(mode) ? "BARGE" : "SUPPLIER";
        String trafficServiceJson = safe(source.trafficServiceJson());
        if (trafficServiceJson.isBlank()) {
            trafficServiceJson = null;
        } else {
            try {
                objectMapper.readTree(trafficServiceJson);
            } catch (JsonProcessingException exception) {
                throw badRequest("FOOD_TRAFFIC_SERVICE_INVALID");
            }
        }
        List<Long> selectedDemandItemIds = source.selectedDemandItemIds() == null ? null : source.selectedDemandItemIds().stream()
            .map(itemId -> {
                if (itemId == null || itemId <= 0) throw badRequest("FOOD_COMPARISON_SELECTION_INVALID");
                return itemId;
            })
            .distinct()
            .toList();
        int mixedSupplierCount = source.mixedSupplierCount() == null ? 3 : source.mixedSupplierCount();
        if (mixedSupplierCount < 2 || mixedSupplierCount > 10) throw badRequest("FOOD_MIXED_SUPPLIER_COUNT_INVALID");
        boolean priceEnabled = source.priceEnabled() == null || source.priceEnabled();
        boolean qualityEnabled = source.qualityEnabled() == null || source.qualityEnabled();
        if (!priceEnabled && !qualityEnabled) throw badRequest("FOOD_COMPARISON_STRATEGY_REQUIRED");
        int priceLevel = source.priceLevel() == null ? 5 : source.priceLevel();
        int qualityLevel = source.qualityLevel() == null ? 3 : source.qualityLevel();
        if (priceLevel < 1 || priceLevel > 5 || qualityLevel < 1 || qualityLevel > 5) {
            throw badRequest("FOOD_COMPARISON_STRATEGY_LEVEL_INVALID");
        }
        List<Long> coreDemandItemIds = source.coreDemandItemIds() == null ? List.of() : source.coreDemandItemIds().stream()
            .map(itemId -> {
                if (itemId == null || itemId <= 0) throw badRequest("FOOD_COMPARISON_CORE_ITEM_INVALID");
                return itemId;
            })
            .distinct()
            .toList();
        return new ComparisonSettings(
            markup, nonNegative(source.fixedFreightFee(), BigDecimal.ZERO),
            nonNegative(source.fixedCustomsFee(), BigDecimal.ZERO),
            nonNegative(source.fixedCraneFee(), BigDecimal.ZERO),
            nonNegative(source.fixedOtherFee(), BigDecimal.ZERO), mode, providerType,
            safe(source.fixedProviderId()), safe(source.fixedProviderName()), trafficServiceJson,
            selectedDemandItemIds, mixedSupplierCount, priceEnabled, priceLevel, qualityEnabled, qualityLevel,
            coreDemandItemIds
        );
    }

    private BigDecimal nonNegative(BigDecimal value, BigDecimal fallback) {
        BigDecimal normalized = value == null ? fallback : value;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) throw badRequest("FOOD_COMPARISON_AMOUNT_INVALID");
        return normalized.setScale(4, RoundingMode.HALF_UP);
    }

    private List<AttachmentPayload> normalizedSettlementAttachments(SettlementActionRequest request) {
        List<AttachmentPayload> source = request.invoiceAttachments();
        if ((source == null || source.isEmpty()) && !safe(request.invoiceFileId()).isBlank()) {
            String fileId = safe(request.invoiceFileId());
            String fileName = safe(request.invoiceNo()).isBlank() ? fileId : safe(request.invoiceNo());
            source = List.of(new AttachmentPayload(fileId, fileName, "/api/files/" + fileId));
        }
        if (source == null) return null;
        return source.stream().map(file -> {
            if (file == null || safe(file.fileName()).isBlank()
                || (safe(file.fileId()).isBlank() && safe(file.fileUrl()).isBlank())) {
                throw badRequest("FOOD_SETTLEMENT_INVOICE_ATTACHMENT_INVALID");
            }
            String fileId = safe(file.fileId());
            String fileUrl = safe(file.fileUrl());
            if (fileUrl.isBlank()) fileUrl = "/api/files/" + fileId;
            return new AttachmentPayload(fileId.isBlank() ? null : fileId, safe(file.fileName()), fileUrl);
        }).toList();
    }

    private List<AttachmentPayload> normalizedEvaluationAttachments(List<AttachmentPayload> attachments) {
        if (attachments == null) return List.of();
        return attachments.stream().map(file -> {
            if (file == null || safe(file.fileName()).isBlank()
                || (safe(file.fileId()).isBlank() && safe(file.fileUrl()).isBlank())) {
                throw badRequest("INVALID_EVALUATION_ATTACHMENT");
            }
            String fileId = safe(file.fileId());
            String fileUrl = safe(file.fileUrl());
            if (fileUrl.isBlank()) fileUrl = "/api/files/" + fileId;
            return new AttachmentPayload(fileId.isBlank() ? null : fileId, safe(file.fileName()), fileUrl);
        }).toList();
    }

    private record SupplierRank(long supplierCompanyId, String supplierName, int coveredItemCount, BigDecimal totalAmount) {
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("FOOD_JSON_SERIALIZE_FAILED", exception);
        }
    }

    private <T> T fromJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw conflict("FOOD_IMPORT_PREVIEW_INVALID");
        }
    }

    private <T> T translateConflict(Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalStateException exception) {
            throw conflict(exception.getMessage());
        }
    }

    private void translateConflict(Runnable action) {
        translateConflict(() -> {
            action.run();
            return null;
        });
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason == null ? "FOOD_OPERATION_CONFLICT" : reason);
    }
}
