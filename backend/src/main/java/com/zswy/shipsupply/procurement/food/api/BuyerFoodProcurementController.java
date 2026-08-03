package com.zswy.shipsupply.procurement.food.api;

import java.util.List;

import org.springframework.http.MediaType;
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

import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonSettings;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSaveRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSaveResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationReviewRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSubmitRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.InquirySummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.MatchPreviewResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderCreateRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderCreateResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SendInquiryRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SendInquiryResponse;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementActionRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierSummary;
import com.zswy.shipsupply.procurement.food.application.FoodProcurementApplicationService;

@RestController
@RequestMapping("/api/procurement/food")
public class BuyerFoodProcurementController {

    private final FoodProcurementApplicationService service;

    public BuyerFoodProcurementController(FoodProcurementApplicationService service) {
        this.service = service;
    }

    @PostMapping(value = "/demands/match-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MatchPreviewResponse preview(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "sheetName", required = false) String sheetName
    ) {
        return service.preview(authorizationHeader, file, sheetName);
    }

    @PostMapping("/demands")
    public DemandSaveResponse saveDemand(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody DemandSaveRequest request
    ) {
        return service.saveDemand(authorizationHeader, request);
    }

    @PutMapping("/demands/{id}")
    public DemandSaveResponse updateDemand(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody DemandSaveRequest request
    ) {
        DemandSaveRequest resolved = new DemandSaveRequest(
            id, request.inquiryNo(), request.vesselName(), request.supplyPort(), request.vesselEta(), request.quoteDeadlineAt(), request.currency(),
            request.sourceFileName(), request.sourceSheetName(), request.items()
        );
        return service.saveDemand(authorizationHeader, resolved);
    }

    @GetMapping("/demands")
    public List<DemandSummary> demands(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status
    ) {
        return service.listDemands(authorizationHeader, keyword, status);
    }

    @GetMapping("/demands/{id}")
    public DemandDetail demand(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id
    ) {
        return service.getDemand(authorizationHeader, id);
    }

    @GetMapping("/suppliers")
    public List<SupplierSummary> suppliers(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return service.listSuppliers(authorizationHeader);
    }

    @PostMapping("/demands/{id}/send-inquiry")
    public SendInquiryResponse sendInquiry(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody SendInquiryRequest request
    ) {
        return service.sendInquiry(authorizationHeader, id, request);
    }

    @GetMapping("/inquiries")
    public List<InquirySummary> inquiries(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status
    ) {
        return service.listBuyerInquiries(authorizationHeader, keyword, status);
    }

    @GetMapping("/quotes/{id}")
    public QuoteDetail quote(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id
    ) {
        return service.getQuote(authorizationHeader, id);
    }

    @GetMapping("/demands/{id}/comparison")
    public ComparisonResponse comparison(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id
    ) {
        return service.comparison(authorizationHeader, id);
    }

    @PutMapping("/demands/{id}/comparison-settings")
    public ComparisonSettings saveComparisonSettings(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody ComparisonSettings request
    ) {
        return service.saveComparisonSettings(authorizationHeader, id, request);
    }

    @PostMapping("/demands/{id}/purchase-orders")
    public OrderCreateResponse createOrder(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody OrderCreateRequest request
    ) {
        return service.createOrder(authorizationHeader, id, request);
    }

    @GetMapping("/purchase-orders")
    public List<OrderSummary> orders(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status
    ) {
        return service.listBuyerOrders(authorizationHeader, keyword, status);
    }

    @GetMapping("/purchase-orders/{id}")
    public OrderDetail order(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id
    ) {
        return service.getOrder(authorizationHeader, id);
    }

    @GetMapping("/settlements")
    public List<SettlementSummary> settlements(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "status", required = false) String status
    ) {
        return service.listBuyerSettlements(authorizationHeader, status);
    }

    @PostMapping("/settlements/{id}/action")
    public SettlementSummary settlementAction(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody SettlementActionRequest request
    ) {
        return service.updateSettlement(authorizationHeader, id, request);
    }

    @GetMapping("/evaluations")
    public List<EvaluationSummary> evaluations(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "status", required = false) String status
    ) {
        return service.listEvaluations(authorizationHeader, status);
    }

    @PostMapping("/evaluations/{id}/submit")
    public EvaluationSummary submitEvaluation(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody EvaluationSubmitRequest request
    ) {
        return service.submitEvaluation(authorizationHeader, id, request);
    }

    @PostMapping("/evaluations/{id}/review")
    public EvaluationSummary reviewEvaluation(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody EvaluationReviewRequest request
    ) {
        return service.reviewEvaluation(authorizationHeader, id, request);
    }
}
