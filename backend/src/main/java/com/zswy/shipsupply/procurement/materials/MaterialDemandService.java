package com.zswy.shipsupply.procurement.materials;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@Service
public class MaterialDemandService {

    private static final String SAVED = "SAVED";
    private static final String DEFAULT_ROUTE = "/procurement/materials";

    private final CurrentUserService currentUserService;
    private final MaterialDemandRepository materialDemandRepository;

    public MaterialDemandService(CurrentUserService currentUserService, MaterialDemandRepository materialDemandRepository) {
        this.currentUserService = currentUserService;
        this.materialDemandRepository = materialDemandRepository;
    }

    @Transactional
    public MaterialDemandSaveResponse save(String authorizationHeader, MaterialDemandSaveRequest request, Long pathDemandId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        MaterialDemandSaveRequest safeRequest = validated(request);
        LocalDate inquiryDate = LocalDate.parse(safeRequest.inquiryDate());
        MaterialDemandStats stats = stats(safeRequest.items());
        Long targetDemandId = pathDemandId == null ? safeRequest.demandId() : pathDemandId;

        if (targetDemandId != null) {
            MaterialDemandSummaryResponse existing = requireDemandById(currentUser.companyId(), targetDemandId);
            ensureEditable(existing);
            materialDemandRepository.updateDemand(targetDemandId, currentUser.companyId(), currentUser.userId(), safeRequest, stats);
            materialDemandRepository.replaceItems(targetDemandId, currentUser.companyId(), safeRequest.items());
            return savedResponse(targetDemandId, existing.demandNo(), existing.status());
        }

        String requestDemandNo = optionalText(safeRequest.demandNo());
        if (requestDemandNo != null) {
            MaterialDemandSummaryResponse existing = materialDemandRepository.findSummaryByNo(currentUser.companyId(), requestDemandNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MATERIAL_DEMAND_NOT_FOUND"));
            ensureEditable(existing);
            materialDemandRepository.updateDemand(existing.demandId(), currentUser.companyId(), currentUser.userId(), safeRequest, stats);
            materialDemandRepository.replaceItems(existing.demandId(), currentUser.companyId(), safeRequest.items());
            return savedResponse(existing.demandId(), existing.demandNo(), existing.status());
        }

        String demandNo = materialDemandRepository.nextDemandNo(currentUser.companyId(), inquiryDate);
        long demandId = materialDemandRepository.insertDemand(currentUser.companyId(), currentUser.userId(), demandNo, safeRequest, stats);
        materialDemandRepository.replaceItems(demandId, currentUser.companyId(), safeRequest.items());
        return savedResponse(demandId, demandNo);
    }

    public MaterialDemandListResponse list(
        String authorizationHeader,
        String keyword,
        String status,
        LocalDate dateFrom,
        LocalDate dateTo,
        String stage,
        int page,
        int size
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        int safePage = page <= 0 ? 1 : page;
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        return materialDemandRepository.list(
            currentUser.companyId(),
            optionalText(keyword),
            optionalText(status),
            dateFrom,
            dateTo,
            optionalText(stage),
            safePage,
            safeSize
        );
    }

    public MaterialDemandDetailResponse detail(String authorizationHeader, Long demandId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        MaterialDemandSummaryResponse demand = requireDemandById(currentUser.companyId(), demandId);
        return new MaterialDemandDetailResponse(
            demand,
            materialDemandRepository.items(currentUser.companyId(), demandId)
        );
    }

    private MaterialDemandSummaryResponse requireDemandById(Long companyId, Long demandId) {
        if (demandId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "demandId is required");
        }
        return materialDemandRepository.findSummaryById(companyId, demandId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MATERIAL_DEMAND_NOT_FOUND"));
    }

    private void ensureEditable(MaterialDemandSummaryResponse existing) {
        if ("ORDERED".equalsIgnoreCase(existing.status()) || "DISCARDED".equalsIgnoreCase(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MATERIAL_DEMAND_LOCKED");
        }
    }

    private MaterialDemandSaveRequest validated(MaterialDemandSaveRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request is required");
        }
        required(request.vesselName(), "vesselName");
        LocalDate inquiryDate = compatibleInquiryDate(request.vesselEta(), request.inquiryDate());
        List<MaterialDemandItemRequest> items = request.items() == null ? List.of() : request.items().stream().map(this::validatedItem).toList();
        SupplyPortValue supplyPort = normalizeSupplyPort(request.supplyPortCode(), request.supplyPortName());
        return new MaterialDemandSaveRequest(
            request.demandId(),
            request.demandNo(),
            optionalText(request.applicationNo()),
            optionalText(request.inquiryNo()),
            optionalText(request.materialType()),
            optionalText(request.currency()),
            optionalText(request.recipientCompany()),
            optionalText(request.handlerName()),
            optionalText(request.handlerEmail()),
            request.vesselName().trim(),
            supplyPort.code(),
            supplyPort.name(),
            optionalText(request.vesselEta()),
            inquiryDate.toString(),
            optionalText(request.sourceFileName()),
            optionalText(request.sourceFileId()),
            optionalText(request.documentType()),
            request.headerRowIndex() == null ? 0 : request.headerRowIndex(),
            request.trafficService(),
            items
        );
    }

    private SupplyPortValue normalizeSupplyPort(String code, String name) {
        String safeCode = optionalText(code);
        String safeName = optionalText(name);
        if (isSulanghuPort(safeCode) || isSulanghuPort(safeName)) {
            return new SupplyPortValue("SULANGHU", "衢山港区/SULANGHU");
        }
        return new SupplyPortValue(safeCode, safeName);
    }

    private boolean isSulanghuPort(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5]", "").toUpperCase();
        return normalized.equals("SULANGHU")
            || normalized.equals("SHULANGHU")
            || normalized.contains("鼠浪湖");
    }

    private record SupplyPortValue(String code, String name) {
    }

    private MaterialDemandItemRequest validatedItem(MaterialDemandItemRequest item) {
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MATERIAL_DEMAND_ITEM_REQUIRED");
        }
        String quantity = positiveQuantity(item.quantity());
        String unit = optionalText(item.unit());
        return new MaterialDemandItemRequest(
            item.documentType(),
            item.headerRowIndex(),
            item.sequence(),
            item.sourceRowNo(),
            item.sourceRowNumber(),
            item.rawColumns(),
            item.impaCode(),
            item.description(),
            item.sizeModel(),
            quantity,
            unit == null ? "\u4e2a" : unit,
            item.remarks(),
            item.supplierItemNo(),
            item.rawNameSpec(),
            item.price(),
            item.packing(),
            item.stock(),
            item.selectedImpaCode(),
            item.candidateImpaCode(),
            item.candidateNameCn(),
            item.candidateNameEn(),
            item.candidateSpec(),
            item.matchResult(),
            item.matchResultName(),
            item.reason(),
            validationStatus(item),
            validationReason(item),
            item.hasImage(),
            item.imageIndex(),
            item.imageAnchor(),
            item.actualQuotePrice(),
            optionalText(item.actualQuoteCurrency()),
            item.quoteMarkupPercent(),
            item.quoteSupplierSkuId(),
            optionalText(item.quoteSelectedUnit()),
            item.quoteUnitPrice(),
            item.quoteUnitPriceUsd(),
            optionalText(item.quoteStrategyType()),
            item.candidateSnapshot(),
            item.candidates()
        );
    }

    private String validationStatus(MaterialDemandItemRequest item) {
        String explicit = optionalText(item.validationStatus());
        if (explicit != null) {
            return explicit;
        }
        return "EXACT".equals(item.matchResult()) && "CODE_MATCH".equals(item.reason()) ? "MATCHED" : "ABNORMAL";
    }

    private String validationReason(MaterialDemandItemRequest item) {
        String explicit = optionalText(item.validationReason());
        if (explicit != null) {
            return explicit;
        }
        if ("EXACT".equals(item.matchResult()) && "CODE_MATCH".equals(item.reason())) {
            return "CODE_MATCH";
        }
        String code = optionalText(item.impaCode());
        if (code == null) {
            return "CODE_MISSING";
        }
        return "CODE_ABNORMAL";
    }

    @Transactional
    public MaterialComparisonQuoteSaveResponse saveComparisonQuotes(
        String authorizationHeader,
        Long demandId,
        MaterialComparisonQuoteSaveRequest request
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        MaterialDemandSummaryResponse demand = requireDemandById(currentUser.companyId(), demandId);
        ensureEditable(demand);
        int savedCount = materialDemandRepository.updateComparisonQuotes(currentUser.companyId(), demandId, request);
        return new MaterialComparisonQuoteSaveResponse(demandId, savedCount);
    }

    private String positiveQuantity(String value) {
        String text = optionalText(value);
        if (text == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MATERIAL_DEMAND_ITEM_QUANTITY_INVALID");
        }
        try {
            BigDecimal quantity = new BigDecimal(text.replace(",", ""));
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                return text;
            }
        } catch (NumberFormatException ex) {
            // handled below
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MATERIAL_DEMAND_ITEM_QUANTITY_INVALID");
    }

    private MaterialDemandStats stats(List<MaterialDemandItemRequest> items) {
        int exactCount = 0;
        int similarCount = 0;
        int unmatchedCount = 0;
        for (MaterialDemandItemRequest item : items) {
            if ("EXACT".equals(item.matchResult())) {
                exactCount++;
            } else if ("SIMILAR".equals(item.matchResult())) {
                similarCount++;
            } else if ("UNMATCHED".equals(item.matchResult())) {
                unmatchedCount++;
            }
        }
        return new MaterialDemandStats(items.size(), exactCount, similarCount, unmatchedCount);
    }

    private MaterialDemandSaveResponse savedResponse(Long demandId, String demandNo) {
        return savedResponse(demandId, demandNo, SAVED);
    }

    private MaterialDemandSaveResponse savedResponse(Long demandId, String demandNo, String status) {
        return new MaterialDemandSaveResponse(
            demandId,
            demandNo,
            "COMPARING".equalsIgnoreCase(status) ? "COMPARING" : SAVED,
            DEFAULT_ROUTE,
            DEFAULT_ROUTE + "?demandId=" + demandId
        );
    }

    private LocalDate compatibleInquiryDate(String vesselEta, String inquiryDate) {
        LocalDate etaDate = parseOptionalDate(vesselEta);
        if (etaDate != null) {
            return etaDate;
        }
        LocalDate requestDate = parseOptionalDate(inquiryDate);
        return requestDate == null ? LocalDate.now() : requestDate;
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        String datePart = text.length() >= 10 ? text.substring(0, 10) : text;
        try {
            return LocalDate.parse(datePart);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_INQUIRY_DATE");
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
