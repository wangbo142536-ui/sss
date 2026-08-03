package com.zswy.shipsupply.procurement.food.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class FoodProcurementDtos {

    private FoodProcurementDtos() {
    }

    public record SheetOption(String name, int headerRow, int validRows) {
    }

    public record PreviewItem(
        int sourceRow,
        int sequenceNo,
        String nameEn,
        String nameZh,
        String remark,
        String specification,
        String unit,
        BigDecimal requestedQuantity,
        String matchStatus,
        String matchReason,
        String matchKey,
        Map<String, String> rawColumns
    ) {
    }

    public record MatchPreviewResponse(
        String fileName,
        String selectedSheet,
        int headerRow,
        int totalRows,
        int matchedCount,
        int pendingCount,
        List<SheetOption> sheets,
        List<PreviewItem> items
    ) {
    }

    public record DemandItemPayload(
        int sourceRow,
        int sequenceNo,
        String nameEn,
        String nameZh,
        String remark,
        String specification,
        String unit,
        BigDecimal requestedQuantity,
        String matchStatus,
        String matchReason,
        Map<String, String> rawColumns
    ) {
    }

    public record DemandSaveRequest(
        Long demandId,
        String inquiryNo,
        String vesselName,
        String supplyPort,
        LocalDateTime vesselEta,
        LocalDateTime quoteDeadlineAt,
        String currency,
        String sourceFileName,
        String sourceSheetName,
        List<DemandItemPayload> items
    ) {
    }

    public record DemandSaveResponse(Long demandId, String demandNo, String inquiryNo, String status, int supplierCount) {
    }

    public record DemandSummary(
        Long demandId,
        String demandNo,
        String inquiryNo,
        String vesselName,
        String supplyPort,
        LocalDateTime vesselEta,
        String currency,
        String status,
        int itemCount,
        int matchedCount,
        int pendingCount,
        int supplierCount,
        int submittedQuoteCount,
        LocalDateTime inquirySentAt,
        LocalDateTime quoteDeadlineAt,
        LocalDateTime updatedAt
    ) {
    }

    public record DemandItem(
        Long itemId,
        int sequenceNo,
        String nameEn,
        String nameZh,
        String remark,
        String specification,
        String unit,
        BigDecimal requestedQuantity,
        String matchStatus,
        String matchReason
    ) {
    }

    public record DemandDetail(DemandSummary demand, String sourceFileName, String sourceSheetName, List<DemandItem> items) {
    }

    public record SupplierSummary(Long supplierCompanyId, String supplierName, String companyType) {
    }

    public record SendInquiryRequest(List<Long> supplierCompanyIds, Integer quoteValidityDays) {
    }

    public record SendInquiryResponse(Long demandId, int supplierCount, String status) {
    }

    public record InquirySummary(
        Long inquirySupplierId,
        Long demandId,
        Long quoteId,
        String demandNo,
        String inquiryNo,
        String vesselName,
        String supplyPort,
        LocalDateTime vesselEta,
        String buyerName,
        String supplierName,
        String status,
        String quoteStatus,
        int itemCount,
        int quotedItemCount,
        int missingItemCount,
        BigDecimal totalAmount,
        LocalDateTime sentAt,
        LocalDateTime quoteDeadlineAt,
        LocalDateTime updatedAt
    ) {
    }

    public record QuoteItem(
        Long quoteItemId,
        Long demandItemId,
        int sequenceNo,
        String nameEn,
        String nameZh,
        String remark,
        String specification,
        String unit,
        BigDecimal requestedQuantity,
        BigDecimal quotedQuantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        String availability,
        String priceSource,
        String matchStatus,
        String supplierRemark
    ) {
    }

    public record QuoteDetail(
        Long quoteId,
        Long demandId,
        String quoteNo,
        String demandNo,
        String inquiryNo,
        String vesselName,
        String supplyPort,
        LocalDateTime vesselEta,
        String buyerName,
        String supplierName,
        String currency,
        String status,
        int versionNo,
        BigDecimal totalAmount,
        int quotedItemCount,
        int missingItemCount,
        int quantityDifferenceCount,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        List<QuoteItem> items
    ) {
    }

    public record QuoteItemUpdate(
        Long quoteItemId,
        BigDecimal quotedQuantity,
        BigDecimal unitPrice,
        String availability,
        String supplierRemark,
        String priceSource
    ) {
    }

    public record QuoteSaveRequest(List<QuoteItemUpdate> items) {
    }

    public record VirtualFillRequest(boolean overwriteExisting) {
    }

    public record VirtualFillResponse(Long quoteId, int filledCount, int preservedCount, String priceSource) {
    }

    public record ImportIssue(
        int sourceRow,
        String nameEn,
        String nameZh,
        String specification,
        String unit,
        String issueType,
        String message
    ) {
    }

    public record ImportUpdate(
        int sourceRow,
        Long quoteItemId,
        Long demandItemId,
        BigDecimal quotedQuantity,
        BigDecimal unitPrice,
        String matchStatus
    ) {
    }

    public record QuoteImportPreviewResponse(
        Long batchId,
        String fileName,
        String selectedSheet,
        int matchedCount,
        int issueCount,
        List<ImportUpdate> updates,
        List<ImportIssue> issues
    ) {
    }

    public record QuoteImportCommitResponse(Long batchId, Long quoteId, int updatedCount, int versionNo) {
    }

    public record ComparisonQuoteOption(
        Long demandItemId,
        Long quoteItemId,
        Long quoteId,
        Long supplierCompanyId,
        String supplierName,
        BigDecimal requestedQuantity,
        BigDecimal quotedQuantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        String availability,
        String priceSource,
        boolean quantitySatisfied,
        boolean lowestPrice
    ) {
    }

    public record ComparisonItem(
        Long demandItemId,
        int sequenceNo,
        String nameEn,
        String nameZh,
        String specification,
        String unit,
        BigDecimal requestedQuantity,
        List<ComparisonQuoteOption> quotes
    ) {
    }

    public record ComparisonStrategy(
        String strategyType,
        String label,
        boolean enabled,
        int coveredItemCount,
        int totalItemCount,
        BigDecimal totalAmount,
        Long supplierCompanyId,
        String supplierName
    ) {
    }

    public record ComparisonSettings(
        BigDecimal markupPercent,
        BigDecimal fixedFreightFee,
        BigDecimal fixedCustomsFee,
        BigDecimal fixedCraneFee,
        BigDecimal fixedOtherFee,
        String supplyMode,
        String fixedProviderType,
        String fixedProviderId,
        String fixedProviderName,
        String trafficServiceJson,
        List<Long> selectedDemandItemIds
    ) {
    }

    public record ComparisonResponse(
        DemandSummary demand,
        int submittedSupplierCount,
        List<ComparisonStrategy> strategies,
        List<ComparisonItem> items,
        ComparisonSettings settings
    ) {
    }

    public record SelectedQuoteItem(Long demandItemId, Long quoteItemId) {
    }

    public record OrderCreateRequest(
        String strategyType,
        boolean allowPartial,
        Long selectedSupplierCompanyId,
        List<SelectedQuoteItem> selectedItems,
        LocalDateTime requiredDeliveryTime,
        String deliveryAddress,
        String deliveryContactName,
        String deliveryContactPhone,
        String deliveryContactEmail,
        String defaultPackagingMethod,
        String buyerRemark
    ) {
        public OrderCreateRequest(
            String strategyType, boolean allowPartial, Long selectedSupplierCompanyId,
            List<SelectedQuoteItem> selectedItems
        ) {
            this(strategyType, allowPartial, selectedSupplierCompanyId, selectedItems, null, null, null, null, null, null, null);
        }

        public OrderCreateRequest(
            String strategyType, boolean allowPartial, Long selectedSupplierCompanyId,
            List<SelectedQuoteItem> selectedItems, LocalDateTime requiredDeliveryTime,
            String deliveryContactName, String deliveryContactPhone, String deliveryContactEmail,
            String defaultPackagingMethod, String buyerRemark
        ) {
            this(
                strategyType, allowPartial, selectedSupplierCompanyId, selectedItems, requiredDeliveryTime, null,
                deliveryContactName, deliveryContactPhone, deliveryContactEmail, defaultPackagingMethod, buyerRemark
            );
        }
    }

    public record OrderCreateResponse(Long orderId, String orderNo, String status, BigDecimal totalAmount) {
    }

    public record OrderSummary(
        Long orderId,
        String orderNo,
        Long demandId,
        String demandNo,
        String vesselName,
        String supplyPort,
        LocalDateTime vesselEta,
        String currency,
        String status,
        BigDecimal totalAmount,
        int supplierCount,
        int itemCount,
        LocalDateTime updatedAt,
        LocalDateTime requiredDeliveryTime,
        String deliveryAddress,
        String deliveryContactName,
        String deliveryContactPhone,
        String deliveryContactEmail,
        String defaultPackagingMethod,
        String buyerRemark,
        BigDecimal fixedFreightFee,
        BigDecimal fixedCustomsFee,
        BigDecimal fixedCraneFee,
        BigDecimal fixedOtherFee,
        String supplyMode,
        String fixedProviderType,
        String fixedProviderId,
        String fixedProviderName,
        String trafficServiceJson
    ) {
    }

    public record SupplierOrderSummary(
        Long orderId,
        String orderNo,
        Long supplierOrderId,
        Long supplierCompanyId,
        String supplierName,
        String demandNo,
        String vesselName,
        String supplyPort,
        LocalDateTime vesselEta,
        String currency,
        String status,
        BigDecimal totalAmount,
        int supplierCount,
        int itemCount,
        LocalDateTime expectedReadyAt,
        LocalDateTime updatedAt
    ) {
    }

    public record SupplierOrder(
        Long supplierOrderId,
        Long supplierCompanyId,
        String supplierName,
        String status,
        BigDecimal subtotalAmount,
        LocalDateTime expectedReadyAt,
        String rejectReason,
        String shipmentRemark
    ) {
    }

    public record OrderItem(
        Long orderItemId,
        Long supplierOrderId,
        Long demandItemId,
        Long quoteItemId,
        String supplierName,
        String nameEn,
        String nameZh,
        String specification,
        String unit,
        BigDecimal requestedQuantity,
        BigDecimal orderedQuantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        BigDecimal quotedUnitPrice,
        BigDecimal quotedAmount
    ) {
    }

    public record OrderDetail(OrderSummary order, List<SupplierOrder> suppliers, List<OrderItem> items) {
    }

    public record SupplierOrderActionRequest(
        String targetStatus,
        LocalDateTime expectedReadyAt,
        String rejectReason,
        String shipmentRemark
    ) {
    }

    public record AttachmentPayload(String fileId, String fileName, String fileUrl) {
    }

    public record SettlementSummary(
        Long settlementId,
        Long orderId,
        String orderNo,
        Long supplierOrderId,
        String supplierName,
        String buyerCompanyName,
        String vesselName,
        String currency,
        String status,
        BigDecimal amount,
        BigDecimal actualAmount,
        String invoiceNo,
        String invoiceFileId,
        List<AttachmentPayload> invoiceAttachments,
        LocalDateTime updatedAt
    ) {
    }

    public record SettlementActionRequest(
        String targetStatus,
        String invoiceNo,
        String invoiceFileId,
        BigDecimal actualAmount,
        List<AttachmentPayload> invoiceAttachments
    ) {
        public SettlementActionRequest(String targetStatus, String invoiceNo, String invoiceFileId) {
            this(targetStatus, invoiceNo, invoiceFileId, null, null);
        }
    }

    public record EvaluationSummary(
        Long evaluationId,
        Long orderId,
        String orderNo,
        String supplierName,
        String status,
        Integer qualityRating,
        Integer logisticsRating,
        String comment,
        List<AttachmentPayload> attachments,
        String reviewRemark,
        LocalDateTime updatedAt
    ) {
    }

    public record EvaluationSubmitRequest(
        int qualityRating,
        int logisticsRating,
        String comment,
        List<AttachmentPayload> attachments
    ) {
        public EvaluationSubmitRequest(int qualityRating, int logisticsRating, String comment) {
            this(qualityRating, logisticsRating, comment, null);
        }
    }

    public record EvaluationReviewRequest(String targetStatus, String reviewRemark) {
    }
}
