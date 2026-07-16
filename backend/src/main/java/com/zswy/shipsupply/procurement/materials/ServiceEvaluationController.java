package com.zswy.shipsupply.procurement.materials;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceEvaluationController {

    private final ServiceEvaluationService service;

    public ServiceEvaluationController(ServiceEvaluationService service) {
        this.service = service;
    }

    @GetMapping("/api/evaluations")
    public ServiceEvaluationListResponse list(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "scope", defaultValue = "BUYER") String scope,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return service.list(authorizationHeader, scope, keyword, status, page, size);
    }

    @PostMapping("/api/evaluations/{id}/submit")
    public ServiceEvaluationResponse submit(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id,
        @RequestBody ServiceEvaluationSubmitRequest request
    ) {
        return service.submit(authorizationHeader, id, request);
    }

    @PostMapping("/api/regulatory/evaluations/{id}/approve")
    public ServiceEvaluationResponse approve(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id,
        @RequestBody(required = false) ServiceEvaluationReviewRequest request
    ) {
        return service.approve(authorizationHeader, id, request);
    }

    @PostMapping("/api/regulatory/evaluations/{id}/reject")
    public ServiceEvaluationResponse reject(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id,
        @RequestBody ServiceEvaluationReviewRequest request
    ) {
        return service.reject(authorizationHeader, id, request);
    }
}
