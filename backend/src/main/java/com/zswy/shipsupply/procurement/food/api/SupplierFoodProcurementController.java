package com.zswy.shipsupply.procurement.food.api;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.InquirySummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierOrderSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteImportCommitResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteImportPreviewResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteSaveRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementActionRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierOrderActionRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.VirtualFillRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.VirtualFillResponse;
import com.zswy.shipsupply.procurement.food.application.FoodProcurementApplicationService;
import com.zswy.shipsupply.procurement.food.application.FoodQuoteExportService;
import com.zswy.shipsupply.procurement.food.application.FoodQuoteExportService.ExportedQuote;

@RestController
@RequestMapping("/api/supplier/food")
public class SupplierFoodProcurementController {

    private static final MediaType XLSX = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final FoodProcurementApplicationService service;
    private final FoodQuoteExportService exportService;

    public SupplierFoodProcurementController(FoodProcurementApplicationService service, FoodQuoteExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @GetMapping("/inquiries")
    public List<InquirySummary> inquiries(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status
    ) {
        return service.listSupplierInquiries(authorizationHeader, keyword, status);
    }

    @GetMapping("/quotes/{id}")
    public QuoteDetail quote(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id
    ) {
        return service.getQuote(authorizationHeader, id);
    }

    @PutMapping("/quotes/{id}/draft")
    public QuoteDetail saveQuote(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody QuoteSaveRequest request
    ) {
        return service.saveQuote(authorizationHeader, id, request);
    }

    @PostMapping("/quotes/{id}/virtual-fill")
    public VirtualFillResponse virtualFill(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody(required = false) VirtualFillRequest request
    ) {
        return service.virtualFill(authorizationHeader, id, request != null && request.overwriteExisting());
    }

    @PostMapping("/quotes/{id}/submit")
    public QuoteDetail submitQuote(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id
    ) {
        return service.submitQuote(authorizationHeader, id);
    }

    @GetMapping("/quotes/{id}/export")
    public ResponseEntity<byte[]> exportQuote(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id
    ) {
        ExportedQuote file = exportService.export(authorizationHeader, id);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(file.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
            .contentType(XLSX)
            .contentLength(file.content().length)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(file.content());
    }

    @PostMapping(value = "/quotes/{id}/import-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public QuoteImportPreviewResponse importPreview(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "sheetName", required = false) String sheetName
    ) {
        return service.previewQuoteImport(authorizationHeader, id, file, sheetName);
    }

    @PostMapping("/quotes/{id}/imports/{batchId}/commit")
    public QuoteImportCommitResponse commitImport(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @PathVariable long batchId
    ) {
        return service.commitQuoteImport(authorizationHeader, id, batchId);
    }

    @GetMapping("/purchase-orders")
    public List<SupplierOrderSummary> orders(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status
    ) {
        return service.listSupplierOrders(authorizationHeader, keyword, status);
    }

    @GetMapping("/purchase-orders/{id}")
    public OrderDetail order(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id
    ) {
        return service.getSupplierOrder(authorizationHeader, id);
    }

    @PostMapping("/purchase-orders/{id}/supplier-orders/{supplierOrderId}/action")
    public OrderDetail actionOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @PathVariable long supplierOrderId,
        @RequestBody SupplierOrderActionRequest request
    ) {
        return service.actionSupplierOrder(authorizationHeader, id, supplierOrderId, request);
    }

    @GetMapping("/settlements")
    public List<SettlementSummary> settlements(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "status", required = false) String status
    ) {
        return service.listSupplierSettlements(authorizationHeader, status);
    }

    @PostMapping("/settlements/{id}/action")
    public SettlementSummary settlementAction(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody SettlementActionRequest request
    ) {
        return service.updateSupplierSettlement(authorizationHeader, id, request);
    }
}
