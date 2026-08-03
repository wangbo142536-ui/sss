package com.zswy.shipsupply.procurement.food.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationReviewRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSummary;
import com.zswy.shipsupply.procurement.food.application.FoodProcurementApplicationService;

@RestController
@RequestMapping("/api/regulatory/food/evaluations")
public class FoodRegulatoryEvaluationController {

    private final FoodProcurementApplicationService service;

    public FoodRegulatoryEvaluationController(FoodProcurementApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<EvaluationSummary> list(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status
    ) {
        return service.listRegulatoryEvaluations(authorizationHeader, keyword, status);
    }

    @PostMapping("/{id}/approve")
    public EvaluationSummary approve(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody(required = false) EvaluationReviewRequest request
    ) {
        return service.reviewRegulatoryEvaluation(
            authorizationHeader, id, "APPROVED", request == null ? null : request.reviewRemark()
        );
    }

    @PostMapping("/{id}/reject")
    public EvaluationSummary reject(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long id,
        @RequestBody EvaluationReviewRequest request
    ) {
        return service.reviewRegulatoryEvaluation(
            authorizationHeader, id, "REJECTED", request == null ? null : request.reviewRemark()
        );
    }
}
