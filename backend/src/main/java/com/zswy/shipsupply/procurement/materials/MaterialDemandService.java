package com.zswy.shipsupply.procurement.materials;

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
        LocalDate inquiryDate = parseInquiryDate(safeRequest.inquiryDate());
        MaterialDemandStats stats = stats(safeRequest.items());
        Long targetDemandId = pathDemandId == null ? safeRequest.demandId() : pathDemandId;

        if (targetDemandId != null) {
            MaterialDemandSummaryResponse existing = requireDemandById(currentUser.companyId(), targetDemandId);
            materialDemandRepository.updateDemand(targetDemandId, currentUser.companyId(), currentUser.userId(), safeRequest, stats);
            materialDemandRepository.replaceItems(targetDemandId, currentUser.companyId(), safeRequest.items());
            return savedResponse(targetDemandId, existing.demandNo());
        }

        String requestDemandNo = optionalText(safeRequest.demandNo());
        if (requestDemandNo != null) {
            MaterialDemandSummaryResponse existing = materialDemandRepository.findSummaryByNo(currentUser.companyId(), requestDemandNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MATERIAL_DEMAND_NOT_FOUND"));
            materialDemandRepository.updateDemand(existing.demandId(), currentUser.companyId(), currentUser.userId(), safeRequest, stats);
            materialDemandRepository.replaceItems(existing.demandId(), currentUser.companyId(), safeRequest.items());
            return savedResponse(existing.demandId(), existing.demandNo());
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

    private MaterialDemandSaveRequest validated(MaterialDemandSaveRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request is required");
        }
        required(request.applicationNo(), "applicationNo");
        required(request.vesselName(), "vesselName");
        parseInquiryDate(request.inquiryDate());
        List<MaterialDemandItemRequest> items = request.items() == null ? List.of() : request.items();
        return new MaterialDemandSaveRequest(
            request.demandId(),
            request.demandNo(),
            request.applicationNo().trim(),
            request.vesselName().trim(),
            request.inquiryDate().trim(),
            optionalText(request.sourceFileName()),
            optionalText(request.documentType()),
            request.headerRowIndex() == null ? 0 : request.headerRowIndex(),
            items
        );
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
        return new MaterialDemandSaveResponse(
            demandId,
            demandNo,
            SAVED,
            DEFAULT_ROUTE,
            DEFAULT_ROUTE + "?demandId=" + demandId
        );
    }

    private LocalDate parseInquiryDate(String inquiryDate) {
        try {
            return LocalDate.parse(required(inquiryDate, "inquiryDate"));
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
