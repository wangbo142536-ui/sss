package com.zswy.shipsupply.procurement.materials;

import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@Service
public class ServiceEvaluationService {

    private final CurrentUserService currentUserService;
    private final AuthRepository authRepository;
    private final ServiceEvaluationRepository repository;
    private final ObjectMapper objectMapper;

    public ServiceEvaluationService(
        CurrentUserService currentUserService,
        AuthRepository authRepository,
        ServiceEvaluationRepository repository,
        ObjectMapper objectMapper
    ) {
        this.currentUserService = currentUserService;
        this.authRepository = authRepository;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ServiceEvaluationListResponse list(
        String authorizationHeader, String scope, String keyword, String status, int page, int size
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        String normalizedScope = text(scope) == null ? "BUYER" : scope.trim().toUpperCase(Locale.ROOT);
        String normalizedKeyword = text(keyword);
        String normalizedStatus = status(status);
        if ("BUYER".equals(normalizedScope)) {
            return repository.listBuyer(user.companyId(), normalizedKeyword, normalizedStatus, safePage(page), safeSize(size));
        }
        if ("REGULATORY".equals(normalizedScope)) {
            requireRegulatory(user.userId());
            return repository.listRegulatory(normalizedKeyword, normalizedStatus, safePage(page), safeSize(size));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_EVALUATION_SCOPE");
    }

    @Transactional
    public ServiceEvaluationResponse submit(
        String authorizationHeader, Long id, ServiceEvaluationSubmitRequest request
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (request == null || request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RATING_MUST_BE_1_TO_5");
        }
        int logisticsRating = request.logisticsRating() == null ? request.rating() : request.logisticsRating();
        if (logisticsRating < 1 || logisticsRating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "LOGISTICS_RATING_MUST_BE_1_TO_5");
        }
        return repository.submit(
            user.companyId(), id, request.rating(), logisticsRating, text(request.content()), json(validAttachments(request.attachments()))
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "EVALUATION_NOT_SUBMITTABLE"));
    }

    @Transactional
    public ServiceEvaluationResponse approve(
        String authorizationHeader, Long id, ServiceEvaluationReviewRequest request
    ) {
        return review(authorizationHeader, id, "APPROVED", request);
    }

    @Transactional
    public ServiceEvaluationResponse reject(
        String authorizationHeader, Long id, ServiceEvaluationReviewRequest request
    ) {
        if (request == null || text(request.remark()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "REVIEW_REMARK_REQUIRED");
        }
        return review(authorizationHeader, id, "REJECTED", request);
    }

    private ServiceEvaluationResponse review(
        String authorizationHeader, Long id, String status, ServiceEvaluationReviewRequest request
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireRegulatory(user.userId());
        return repository.review(user.userId(), id, status, text(request == null ? null : request.remark()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "EVALUATION_NOT_REVIEWABLE"));
    }

    private void requireRegulatory(Long userId) {
        boolean allowed = authRepository.roleCodesForUser(userId).stream()
            .map(code -> code == null ? "" : code.toUpperCase(Locale.ROOT))
            .anyMatch(code ->
                "PLATFORM_ADMIN".equals(code)
                    || code.contains("REGULATORY")
                    || code.startsWith("COMPANY_ADMIN_")
                    || "SHIP_AGENT".equals(code)
            );
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "REGULATORY_ROLE_REQUIRED");
    }

    private List<FileSnapshot> validAttachments(List<FileSnapshot> attachments) {
        if (attachments == null) return List.of();
        return attachments.stream().map(file -> {
            if (file == null || text(file.fileName()) == null || (text(file.fileUrl()) == null && text(file.fileId()) == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_EVALUATION_ATTACHMENT");
            }
            String url = text(file.fileUrl()) == null ? "/api/files/" + text(file.fileId()) : text(file.fileUrl());
            return new FileSnapshot(text(file.fileId()), text(file.fileName()), url);
        }).toList();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ATTACHMENT_DATA_INVALID");
        }
    }

    private String status(String value) {
        String normalized = text(value) == null ? null : value.trim().toUpperCase(Locale.ROOT);
        if (normalized != null && !List.of("PENDING_EVALUATION", "PENDING_REVIEW", "APPROVED", "REJECTED").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_EVALUATION_STATUS");
        }
        return normalized;
    }

    private int safePage(int value) { return Math.max(value, 1); }
    private int safeSize(int value) { return Math.min(Math.max(value, 1), 100); }
    private String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
