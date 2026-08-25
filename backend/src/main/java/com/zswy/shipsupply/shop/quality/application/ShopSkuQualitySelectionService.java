package com.zswy.shipsupply.shop.quality.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.shop.quality.api.QualitySelectionListResponse;
import com.zswy.shipsupply.shop.quality.api.QualitySelectionResponse;
import com.zswy.shipsupply.shop.quality.api.QualitySelectionSaveRequest;
import com.zswy.shipsupply.shop.quality.domain.ShopSkuQualitySelection;
import com.zswy.shipsupply.shop.quality.infrastructure.ShopSkuQualitySelectionRepository;

@Service
public class ShopSkuQualitySelectionService {

    private final CurrentUserService currentUserService;
    private final AuthRepository authRepository;
    private final ShopSkuQualitySelectionRepository repository;

    public ShopSkuQualitySelectionService(
        CurrentUserService currentUserService,
        AuthRepository authRepository,
        ShopSkuQualitySelectionRepository repository
    ) {
        this.currentUserService = currentUserService;
        this.authRepository = authRepository;
        this.repository = repository;
    }

    public QualitySelectionListResponse list(String authorizationHeader, long companyId) {
        currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!repository.isSupplierCompany(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND");
        }
        return new QualitySelectionListResponse(repository.listByCompany(companyId).stream().map(this::response).toList());
    }

    @Transactional
    public QualitySelectionResponse save(
        String authorizationHeader,
        long companyId,
        long skuId,
        QualitySelectionSaveRequest request
    ) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (!authRepository.hasRole(currentUser.userId(), "PLATFORM_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PLATFORM_ADMIN_REQUIRED");
        }
        if (!repository.isSupplierSku(companyId, skuId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SUPPLIER_SKU_NOT_FOUND");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QUALITY_SELECTION_REQUIRED");
        }
        LocalDateTime inspectionTime = dateTime(required(request.inspectionTime(), "inspectionTime"));
        String inspectionContent = limited(required(request.inspectionContent(), "inspectionContent"), 4000, "inspectionContent");
        String inspectionProcess = limited(optional(request.inspectionProcess()), 8000, "inspectionProcess");
        String conclusion = limited(required(request.inspectionConclusion(), "inspectionConclusion"), 1000, "inspectionConclusion");
        String reportFileId = optional(request.reportFileId());
        if (reportFileId != null && !repository.activeFileExists(reportFileId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QUALITY_REPORT_FILE_NOT_FOUND");
        }
        String reportFileName = reportFileId == null
            ? null
            : limited(value(optional(request.reportFileName()), repository.fileName(reportFileId)), 255, "reportFileName");
        ShopSkuQualitySelection saved = repository.save(
            companyId,
            skuId,
            currentUser.userId(),
            inspectionTime,
            inspectionContent,
            inspectionProcess,
            reportFileId,
            reportFileName,
            conclusion
        );
        authRepository.log(
            currentUser.userId(),
            "SHOP_SKU_QUALITY_SELECTION_SAVE",
            "SHOP_SKU",
            String.valueOf(skuId),
            "/api/shop/suppliers/" + companyId + "/skus/" + skuId + "/quality-selection",
            "qualitySelectionId=" + saved.qualitySelectionId()
        );
        return response(saved);
    }

    private QualitySelectionResponse response(ShopSkuQualitySelection item) {
        String reportUrl = item.reportFileId() == null ? null : "/api/files/" + item.reportFileId();
        return new QualitySelectionResponse(
            item.qualitySelectionId(),
            item.companyId(),
            item.skuId(),
            item.inspectionTime().toString(),
            item.inspectionContent(),
            item.inspectionProcess(),
            item.reportFileId(),
            item.reportFileName(),
            reportUrl,
            item.inspectionConclusion(),
            item.auditTrail(),
            item.status(),
            item.updatedAt().toString()
        );
    }

    private LocalDateTime dateTime(String value) {
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QUALITY_INSPECTION_TIME_INVALID", exception);
        }
    }

    private String required(String value, String field) {
        String normalized = optional(value);
        if (normalized == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        return normalized;
    }

    private String optional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String limited(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " exceeds " + max + " characters");
        }
        return value;
    }

    private String value(String first, String fallback) {
        return first == null ? fallback : first;
    }
}
