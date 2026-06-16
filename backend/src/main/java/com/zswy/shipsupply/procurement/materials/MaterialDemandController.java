package com.zswy.shipsupply.procurement.materials;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procurement/material-demands")
public class MaterialDemandController {

    private final MaterialDemandService materialDemandService;
    private final MaterialDemandComparisonService materialDemandComparisonService;

    public MaterialDemandController(
        MaterialDemandService materialDemandService,
        MaterialDemandComparisonService materialDemandComparisonService
    ) {
        this.materialDemandService = materialDemandService;
        this.materialDemandComparisonService = materialDemandComparisonService;
    }

    @PostMapping
    public MaterialDemandSaveResponse save(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody MaterialDemandSaveRequest request
    ) {
        return materialDemandService.save(authorizationHeader, request, null);
    }

    @PutMapping("/{id}")
    public MaterialDemandSaveResponse update(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id,
        @RequestBody MaterialDemandSaveRequest request
    ) {
        return materialDemandService.save(authorizationHeader, request, id);
    }

    @GetMapping
    public MaterialDemandListResponse list(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(value = "inquiryDateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inquiryDateFrom,
        @RequestParam(value = "inquiryDateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inquiryDateTo,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        LocalDate resolvedDateFrom = inquiryDateFrom == null ? dateFrom : inquiryDateFrom;
        LocalDate resolvedDateTo = inquiryDateTo == null ? dateTo : inquiryDateTo;
        return materialDemandService.list(authorizationHeader, keyword, status, resolvedDateFrom, resolvedDateTo, page, size);
    }

    @GetMapping("/{id}")
    public MaterialDemandDetailResponse detail(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ) {
        return materialDemandService.detail(authorizationHeader, id);
    }

    @GetMapping("/{id}/comparison")
    public MaterialDemandComparisonResponse comparison(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ) {
        return materialDemandComparisonService.comparison(authorizationHeader, id);
    }
}
