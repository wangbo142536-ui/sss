package com.zswy.shipsupply.auth;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OnboardingService {

    private static final String PROFILE_REQUIRED = "PROFILE_REQUIRED";
    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String REJECTED = "REJECTED";

    private final AuthRepository authRepository;
    private final TokenService tokenService;

    public OnboardingService(AuthRepository authRepository, TokenService tokenService) {
        this.authRepository = authRepository;
        this.tokenService = tokenService;
    }

    public CompanyProfileResponse currentProfile(String authorizationHeader) {
        AuthenticatedUser user = authRepository.getUserById(tokenService.requireUserId(authorizationHeader));
        return buildProfile(user);
    }

    @Transactional
    public CompanyProfileResponse submitProfile(String authorizationHeader, CompanyProfileRequest request) {
        AuthenticatedUser user = authRepository.getUserById(tokenService.requireUserId(authorizationHeader));
        if (!List.of(PROFILE_REQUIRED, REJECTED, PENDING_REVIEW).contains(user.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current account cannot resubmit onboarding profile");
        }
        String companyType = normalizeSelfServiceRole(request.companyType());
        String companyName = required(request.companyName(), "companyName");
        String unifiedSocialCreditCode = required(
            request.unifiedSocialCreditCode(),
            "unifiedSocialCreditCode",
            "UNIFIED_SOCIAL_CREDIT_CODE_REQUIRED: 统一社会信用代码必填"
        );
        String contactName = required(request.contactName(), "contactName");
        String contactPhone = required(request.contactPhone(), "contactPhone");
        String contactEmail = required(request.contactEmail(), "contactEmail");
        List<String> fileIds = request.qualificationFileIds() == null ? List.of() : request.qualificationFileIds();
        if (fileIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "qualificationFileIds is required");
        }

        authRepository.updateCompanyProfile(
            user.companyId(),
            companyType,
            companyName,
            unifiedSocialCreditCode,
            contactName,
            contactPhone,
            contactEmail,
            PENDING_REVIEW,
            null
        );
        authRepository.updateUserStatusAndType(user.id(), PENDING_REVIEW, companyType);
        authRepository.clearUserRoles(user.id());
        authRepository.replaceCompanyQualifications(user.companyId(), fileIds);
        authRepository.log(
            user.id(),
            "SUBMIT_ONBOARDING_PROFILE",
            "COMPANY",
            String.valueOf(user.companyId()),
            "/api/onboarding/company-profile",
            companyType
        );
        return buildProfile(authRepository.getUserById(user.id()));
    }

    private CompanyProfileResponse buildProfile(AuthenticatedUser user) {
        CompanyResponse company = visibleCompanyProfile(authRepository.getCompany(user.companyId()));
        String route = defaultRoute(user.status(), company.status());
        boolean onboardingRequired = !"ACTIVE".equals(user.status()) || !"ACTIVE".equals(company.status());
        return new CompanyProfileResponse(
            company,
            user.status(),
            company.status(),
            onboardingRequired,
            route,
            authRepository.qualificationsForCompany(company.id())
        );
    }

    private CompanyResponse visibleCompanyProfile(CompanyResponse company) {
        if (PROFILE_REQUIRED.equals(company.status()) && isPlaceholderCompanyName(company.companyName())) {
            return new CompanyResponse(
                company.id(),
                "",
                company.companyType(),
                company.unifiedSocialCreditCode(),
                company.contactName(),
                company.contactPhone(),
                company.contactEmail(),
                company.status()
            );
        }
        return company;
    }

    private boolean isPlaceholderCompanyName(String companyName) {
        return companyName != null && companyName.startsWith("待完善企业-");
    }

    private String normalizeSelfServiceRole(String rawValue) {
        String value = required(rawValue, "companyType");
        String upper = value.toUpperCase(Locale.ROOT).replace("-", "_");
        if (upper.contains("SUPPLIER") || value.contains("供货")) {
            return "SUPPLIER";
        }
        if (upper.contains("SHIP") || value.contains("船代")) {
            return "SHIP_AGENT";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only ship agent and supplier onboarding are open");
    }

    private String defaultRoute(String accountStatus, String companyStatus) {
        if (PENDING_REVIEW.equals(accountStatus) || PENDING_REVIEW.equals(companyStatus)) {
            return "/onboarding/review-status";
        }
        return "/onboarding/company-profile";
    }

    private String required(String value, String field) {
        return required(value, field, field + " is required");
    }

    private String required(String value, String field, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
