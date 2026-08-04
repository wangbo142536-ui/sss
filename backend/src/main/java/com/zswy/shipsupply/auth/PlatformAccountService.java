package com.zswy.shipsupply.auth;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlatformAccountService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";

    private final AuthRepository authRepository;
    private final TokenService tokenService;

    public PlatformAccountService(AuthRepository authRepository, TokenService tokenService) {
        this.authRepository = authRepository;
        this.tokenService = tokenService;
    }

    public PlatformCompanyAccountPageResponse accounts(
        String authorizationHeader,
        String companyType,
        String supplierServiceType,
        String accountSource,
        String roleCode,
        String status,
        String keyword,
        Integer page,
        Integer pageSize
    ) {
        requirePlatformAdmin(authorizationHeader);
        String normalizedCompanyType = upperOrNull(companyType);
        String normalizedServiceType = upperOrNull(supplierServiceType);
        String normalizedSource = upperOrNull(accountSource);
        String normalizedRole = upperOrNull(roleCode);
        String normalizedStatus = upperOrNull(status);
        String normalizedKeyword = textOrNull(keyword);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);

        List<PlatformCompanyAccountsResponse> matched = authRepository.platformCompanyAccounts().stream()
            .filter(company -> normalizedCompanyType == null || normalizedCompanyType.equals(company.companyType()))
            .filter(company -> normalizedServiceType == null || company.supplierServiceTypes().contains(normalizedServiceType))
            .map(company -> filterCompanyAccounts(
                company,
                normalizedSource,
                normalizedRole,
                normalizedStatus,
                normalizedKeyword
            ))
            .filter(company -> !company.accounts().isEmpty())
            .toList();
        long totalAccounts = matched.stream().mapToLong(company -> company.accounts().size()).sum();
        int fromIndex = Math.min((safePage - 1) * safePageSize, matched.size());
        int toIndex = Math.min(fromIndex + safePageSize, matched.size());
        return new PlatformCompanyAccountPageResponse(
            matched.subList(fromIndex, toIndex),
            safePage,
            safePageSize,
            matched.size(),
            totalAccounts
        );
    }

    @Transactional
    public PlatformAccountResponse updateStatus(
        String authorizationHeader,
        Long userId,
        PlatformAccountStatusRequest request
    ) {
        Long operatorUserId = requirePlatformAdmin(authorizationHeader);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (operatorUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLATFORM_ADMIN_CANNOT_DISABLE_SELF");
        }
        PlatformAccountResponse target = authRepository.findPlatformAccount(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PLATFORM_ACCOUNT_NOT_FOUND"));
        if (target.roleCodes().contains("PLATFORM_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PLATFORM_ADMIN_ACCOUNT_CANNOT_BE_MANAGED_HERE");
        }
        if (!List.of(ACTIVE, DISABLED).contains(target.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ACCOUNT_ONBOARDING_STATUS_CANNOT_BE_CHANGED_HERE");
        }
        String nextStatus = normalizeStatus(request == null ? null : request.status());
        String reason = required(request == null ? null : request.reason(), "reason");
        authRepository.updateMemberStatus(userId, nextStatus);
        if (DISABLED.equals(nextStatus)) {
            tokenService.invalidateUser(userId);
        }
        authRepository.log(
            operatorUserId,
            "UPDATE_PLATFORM_ACCOUNT_STATUS",
            "USER",
            String.valueOf(userId),
            "/api/platform/accounts/" + userId + "/status",
            "status=" + target.status() + "->" + nextStatus + ";reason=" + reason
        );
        return authRepository.findPlatformAccount(userId).orElseThrow();
    }

    private PlatformCompanyAccountsResponse filterCompanyAccounts(
        PlatformCompanyAccountsResponse company,
        String accountSource,
        String roleCode,
        String status,
        String keyword
    ) {
        boolean companyKeywordMatched = keyword == null
            || containsIgnoreCase(company.companyName(), keyword)
            || containsIgnoreCase(company.companyType(), keyword);
        List<PlatformAccountResponse> accounts = company.accounts().stream()
            .filter(account -> accountSource == null || accountSource.equals(account.accountSource()))
            .filter(account -> roleCode == null || account.roleCodes().contains(roleCode))
            .filter(account -> status == null || status.equals(account.status()))
            .filter(account -> companyKeywordMatched || accountMatchesKeyword(account, keyword))
            .toList();
        int activeAccountCount = (int) accounts.stream().filter(account -> ACTIVE.equals(account.status())).count();
        return new PlatformCompanyAccountsResponse(
            company.companyId(),
            company.companyName(),
            company.companyType(),
            company.supplierServiceTypes(),
            company.companyStatus(),
            accounts.size(),
            activeAccountCount,
            accounts
        );
    }

    private boolean accountMatchesKeyword(PlatformAccountResponse account, String keyword) {
        return containsIgnoreCase(account.username(), keyword)
            || containsIgnoreCase(account.name(), keyword)
            || containsIgnoreCase(account.phone(), keyword)
            || containsIgnoreCase(account.email(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && keyword != null
            && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private Long requirePlatformAdmin(String authorizationHeader) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        boolean platformAdmin = authRepository.rolesForUser(userId).stream()
            .anyMatch(role -> "PLATFORM_ADMIN".equals(role.roleCode()));
        if (!ACTIVE.equals(user.status()) || !platformAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform admin role is required");
        }
        return userId;
    }

    private String normalizeStatus(String value) {
        String status = required(value, "status").toUpperCase(Locale.ROOT);
        if (!ACTIVE.equals(status) && !DISABLED.equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_STATUS");
        }
        return status;
    }

    private String upperOrNull(String value) {
        String normalized = textOrNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }
}
