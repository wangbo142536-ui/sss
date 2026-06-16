package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@Service
public class PurchaseOrderService {

    static final String PENDING_SUPPLIER_CONFIRM = "PENDING_SUPPLIER_CONFIRM";
    static final String PREPARING = "PREPARING";
    static final String REJECTED = "REJECTED";
    static final String LOWEST_MIXED = "LOWEST_MIXED";
    static final String SINGLE_SUPPLIER = "SINGLE_SUPPLIER";
    private static final String CNY = "CNY";

    private final CurrentUserService currentUserService;
    private final MaterialDemandComparisonService comparisonService;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public PurchaseOrderService(
        CurrentUserService currentUserService,
        MaterialDemandComparisonService comparisonService,
        PurchaseOrderRepository purchaseOrderRepository
    ) {
        this.currentUserService = currentUserService;
        this.comparisonService = comparisonService;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Transactional
    public PurchaseOrderCreateResponse createFromDemand(
        String authorizationHeader,
        Long pathDemandId,
        PurchaseOrderCreateRequest request
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        Long demandId = pathDemandId == null ? request == null ? null : request.demandId() : pathDemandId;
        if (demandId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DEMAND_ID_REQUIRED");
        }
        String strategyType = normalizeStrategy(request == null ? null : request.strategyType());
        return purchaseOrderRepository.findActiveByDemandAndStrategy(currentUser.companyId(), demandId, strategyType)
            .map(this::existingResponse)
            .orElseGet(() -> createNewOrder(authorizationHeader, currentUser, demandId, strategyType, request));
    }

    public PurchaseOrderListResponse listBuyer(
        String authorizationHeader,
        String keyword,
        String status,
        String supplier,
        String createdFrom,
        String createdTo,
        String deliveryFrom,
        String deliveryTo,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return purchaseOrderRepository.listBuyer(
            currentUser.companyId(),
            optionalText(keyword),
            optionalText(status),
            optionalText(supplier),
            optionalText(createdFrom),
            optionalText(createdTo),
            optionalText(deliveryFrom),
            optionalText(deliveryTo),
            safePage(page),
            safeSize(size)
        );
    }

    public PurchaseOrderDetailResponse buyerDetail(String authorizationHeader, Long orderId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return purchaseOrderRepository.findBuyerDetail(currentUser.companyId(), orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND"));
    }

    public PurchaseOrderListResponse listSupplier(
        String authorizationHeader,
        String keyword,
        String status,
        String createdFrom,
        String createdTo,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return purchaseOrderRepository.listSupplier(
            currentUser.companyId(),
            optionalText(keyword),
            optionalText(status),
            optionalText(createdFrom),
            optionalText(createdTo),
            safePage(page),
            safeSize(size)
        );
    }

    @Transactional
    public PurchaseOrderDetailResponse confirmSupplierOrder(
        String authorizationHeader,
        Long orderId,
        Long supplierOrderId,
        PurchaseSupplierConfirmRequest request
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (request == null || request.expectedReadyAt() == null || request.expectedReadyAt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EXPECTED_READY_AT_REQUIRED");
        }
        return purchaseOrderRepository.confirmSupplierOrder(currentUser.companyId(), currentUser.userId(), orderId, supplierOrderId, request)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SUPPLIER_ORDER_NOT_FOUND"));
    }

    @Transactional
    public PurchaseOrderDetailResponse rejectSupplierOrder(
        String authorizationHeader,
        Long orderId,
        Long supplierOrderId,
        PurchaseSupplierRejectRequest request
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (request == null || request.rejectReason() == null || request.rejectReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "REJECT_REASON_REQUIRED");
        }
        return purchaseOrderRepository.rejectSupplierOrder(currentUser.companyId(), currentUser.userId(), orderId, supplierOrderId, request)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SUPPLIER_ORDER_NOT_FOUND"));
    }

    private PurchaseOrderCreateResponse createNewOrder(
        String authorizationHeader,
        CurrentUserContext currentUser,
        Long demandId,
        String strategyType,
        PurchaseOrderCreateRequest request
    ) {
        MaterialDemandComparisonResponse comparison = comparisonService.comparison(authorizationHeader, demandId);
        List<OrderableLine> lines = orderableLines(comparison, strategyType);
        if (lines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PURCHASE_ORDER_NO_ORDERABLE_ITEMS");
        }

        String orderNo = purchaseOrderRepository.nextOrderNo(currentUser.companyId(), LocalDate.now());
        String buyerCompanyName = purchaseOrderRepository.companyName(currentUser.companyId());
        Map<Long, PurchaseSupplierOrderDraft> supplierOrders = new LinkedHashMap<>();
        List<PurchaseOrderItemDraft> items = new ArrayList<>();

        Map<Long, List<OrderableLine>> grouped = new LinkedHashMap<>();
        for (OrderableLine line : lines) {
            grouped.computeIfAbsent(line.candidate().companyId(), ignored -> new ArrayList<>()).add(line);
        }
        int supplierIndex = 1;
        for (Map.Entry<Long, List<OrderableLine>> entry : grouped.entrySet()) {
            List<OrderableLine> supplierLines = entry.getValue();
            MaterialSupplierCandidate first = supplierLines.get(0).candidate();
            String supplierOrderNo = orderNo + "-S" + String.format("%02d", supplierIndex++);
            BigDecimal subtotal = amount(supplierLines);
            supplierOrders.put(first.companyId(), new PurchaseSupplierOrderDraft(
                supplierOrderNo,
                first.companyId(),
                first.supplierName(),
                PENDING_SUPPLIER_CONFIRM,
                supplierLines.size(),
                subtotal,
                subtotal,
                CNY,
                optionalText(request == null ? null : request.defaultPackagingMethod())
            ));
            for (OrderableLine line : supplierLines) {
                items.add(itemDraft(supplierOrderNo, line));
            }
        }

        PurchaseOrderDraft draft = new PurchaseOrderDraft(
            orderNo,
            demandId,
            comparison.demand().demandNo(),
            comparison.demand().applicationNo(),
            currentUser.companyId(),
            buyerCompanyName,
            comparison.demand().vesselName(),
            optionalText(request == null ? null : request.supplyPort()),
            optionalText(request == null ? null : request.vesselEta()),
            optionalText(request == null ? null : request.requiredDeliveryTime()),
            strategyType,
            strategyName(strategyType),
            supplierOrders.size(),
            items.size(),
            amount(lines),
            CNY,
            PENDING_SUPPLIER_CONFIRM,
            optionalText(request == null ? null : request.buyerRemark()),
            currentUser.userId(),
            supplierOrders.values().stream().toList(),
            items
        );
        PurchaseOrderDetailResponse saved = purchaseOrderRepository.insertOrder(draft);
        return new PurchaseOrderCreateResponse(
            saved.order().purchaseOrderId(),
            saved.order().purchaseOrderNo(),
            PENDING_SUPPLIER_CONFIRM,
            draft.supplierCount(),
            draft.itemCount(),
            draft.totalAmount(),
            CNY,
            "/orders/" + saved.order().purchaseOrderId(),
            false
        );
    }

    private List<OrderableLine> orderableLines(MaterialDemandComparisonResponse comparison, String strategyType) {
        List<OrderableLine> lines = new ArrayList<>();
        for (MaterialDemandComparisonItem item : comparison.items()) {
            MaterialSupplierCandidate candidate = LOWEST_MIXED.equals(strategyType)
                ? item.lowestCandidate()
                : item.singleSupplierCandidate();
            if (candidate == null || candidate.unitPrice() == null) {
                continue;
            }
            if (!"ON_SHELF".equalsIgnoreCase(candidate.shelfStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "PURCHASE_ORDER_SKU_CONFLICT");
            }
            if (candidate.skuId() == null || candidate.companyId() == null || candidate.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "PURCHASE_ORDER_SKU_CONFLICT");
            }
            lines.add(new OrderableLine(item, candidate));
        }
        return lines;
    }

    private PurchaseOrderItemDraft itemDraft(String supplierOrderNo, OrderableLine line) {
        MaterialDemandComparisonItem item = line.item();
        MaterialSupplierCandidate candidate = line.candidate();
        BigDecimal amount = candidate.unitPrice().multiply(item.pricingQuantity());
        return new PurchaseOrderItemDraft(
            supplierOrderNo,
            item.demandItemId(),
            candidate.skuId(),
            candidate.supplierSkuCode(),
            candidate.platformCode(),
            candidate.impaCode(),
            candidate.productName(),
            item.specification(),
            item.quantity(),
            item.unit(),
            item.pricingQuantity(),
            candidate.unitPrice(),
            amount,
            CNY,
            unitMismatch(item.unit(), candidate.stockUnit()),
            item.pricingQuantityNote() != null,
            candidate.matchType(),
            candidate.reason()
        );
    }

    private PurchaseOrderCreateResponse existingResponse(PurchaseOrderSummaryResponse existing) {
        return new PurchaseOrderCreateResponse(
            existing.purchaseOrderId(),
            existing.purchaseOrderNo(),
            existing.status(),
            existing.supplierCount(),
            existing.itemCount(),
            existing.totalAmount(),
            existing.currency(),
            "/orders/" + existing.purchaseOrderId(),
            true
        );
    }

    private BigDecimal amount(List<OrderableLine> lines) {
        return lines.stream()
            .map(line -> line.candidate().unitPrice().multiply(line.item().pricingQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean unitMismatch(String demandUnit, String stockUnit) {
        if (demandUnit == null || demandUnit.isBlank() || stockUnit == null || stockUnit.isBlank()) {
            return false;
        }
        return !demandUnit.trim().equalsIgnoreCase(stockUnit.trim());
    }

    private String normalizeStrategy(String value) {
        String strategy = value == null || value.isBlank() ? LOWEST_MIXED : value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!LOWEST_MIXED.equals(strategy) && !SINGLE_SUPPLIER.equals(strategy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_STRATEGY_TYPE");
        }
        return strategy;
    }

    private String strategyName(String strategyType) {
        return SINGLE_SUPPLIER.equals(strategyType) ? "\u96c6\u4e2d\u91c7\u8d2d" : "\u6700\u4f4e\u6df7\u4f9b";
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int safePage(int page) {
        return page <= 0 ? 1 : page;
    }

    private int safeSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 100);
    }

    private record OrderableLine(
        MaterialDemandComparisonItem item,
        MaterialSupplierCandidate candidate
    ) {
    }
}
