package com.zswy.shipsupply.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminRegistrationService {

    private static final String ACTIVE = "ACTIVE";
    private static final String REJECTED = "REJECTED";

    private final AuthRepository authRepository;
    private final TokenService tokenService;

    public AdminRegistrationService(AuthRepository authRepository, TokenService tokenService) {
        this.authRepository = authRepository;
        this.tokenService = tokenService;
    }

    public List<RegistrationReviewResponse> registrations(
        String authorizationHeader,
        String status,
        String keyword,
        Integer page,
        Integer pageSize
    ) {
        requirePlatformAdmin(authorizationHeader);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 50 : Math.min(pageSize, 100);
        return authRepository.registrationsForReview(status, keyword, safePage, safePageSize);
    }

    @Transactional
    public AuthResponse approve(String authorizationHeader, Long userId) {
        Long operatorUserId = requirePlatformAdmin(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        CompanyResponse company = authRepository.getCompany(user.companyId());
        if (!List.of("SHIP_AGENT", "SUPPLIER", "BARGE_AGENT").contains(company.companyType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported registration company type");
        }
        if ("SUPPLIER".equals(company.companyType()) && company.supplierServiceTypes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPPLIER_SERVICE_TYPE_REQUIRED");
        }
        authRepository.updateCompanyStatus(company.id(), ACTIVE, null);
        authRepository.updateUserStatusAndType(user.id(), ACTIVE, company.companyType());
        authRepository.clearUserRoles(user.id());
        String companyAdminRoleCode = authRepository.ensureCompanyAdminRole(company.id());
        authRepository.assignCompanyRole(user.id(), company.id(), companyAdminRoleCode);
        authRepository.log(
            operatorUserId,
            "APPROVE_REGISTRATION",
            "USER",
            String.valueOf(userId),
            "/api/admin/registrations/" + userId + "/approve",
            companyAdminRoleCode
        );
        return new AuthResponse(
            null,
            new UserProfileResponse(user.id(), user.username(), user.phone(), company.companyType(), ACTIVE),
            authRepository.getCompany(company.id()),
            authRepository.rolesForUser(user.id()),
            authRepository.permissionsForUser(user.id()),
            "/dashboard",
            ACTIVE,
            ACTIVE,
            ACTIVE,
            false,
            false,
            "/dashboard",
            "/dashboard"
        );
    }

    @Transactional
    public void reject(String authorizationHeader, Long userId, RejectRegistrationRequest request) {
        Long operatorUserId = requirePlatformAdmin(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        String reason = request == null || request.reason() == null ? "" : request.reason();
        authRepository.updateCompanyStatus(user.companyId(), REJECTED, reason);
        authRepository.updateUserStatusAndType(user.id(), REJECTED, user.userType());
        authRepository.clearUserRoles(user.id());
        authRepository.log(
            operatorUserId,
            "REJECT_REGISTRATION",
            "USER",
            String.valueOf(userId),
            "/api/admin/registrations/" + userId + "/reject",
            reason
        );
    }

    private Long requirePlatformAdmin(String authorizationHeader) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        boolean isAdmin = authRepository.rolesForUser(userId).stream()
            .anyMatch(role -> "PLATFORM_ADMIN".equals(role.roleCode()));
        if (!ACTIVE.equals(user.status()) || !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform admin role is required");
        }
        return userId;
    }
}
