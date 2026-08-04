package com.zswy.shipsupply.auth;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "SHIP_AGENT";
    private static final String PROFILE_REQUIRED = "PROFILE_REQUIRED";
    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String ACTIVE = "ACTIVE";
    private static final String REJECTED = "REJECTED";

    private final AuthRepository authRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public AuthService(AuthRepository authRepository, PasswordHasher passwordHasher, TokenService tokenService) {
        this.authRepository = authRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = required(firstText(request.account(), request.username()), "account");
        String rawPassword = required(request.password(), "password");
        if (request.confirmPassword() != null && !rawPassword.equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "confirmPassword does not match password");
        }
        String requestedCompanyType = firstText(request.companyType(), request.userType());
        String companyType = requestedCompanyType == null ? "UNSPECIFIED" : normalizeSelfServiceRole(requestedCompanyType);
        String userType = companyType;
        List<String> supplierServiceTypes = normalizeSupplierServiceTypes(companyType, request.supplierServiceTypes(), requestedCompanyType != null);
        String companyName = "";
        String contactName = "";
        String phone = firstText(request.phone(), request.contactPhone());
        String contactPhone = phone == null ? "" : phone;

        if (authRepository.userExists(username, phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        long companyId = authRepository.insertCompany(companyName, companyType, contactName, contactPhone, null, PROFILE_REQUIRED);
        long userId = authRepository.insertUser(
            companyId,
            username,
            phone,
            passwordHasher.hash(rawPassword),
            userType,
            PROFILE_REQUIRED
        );
        authRepository.markCompanyOwner(userId);
        authRepository.replaceCompanySupplierServiceTypes(companyId, supplierServiceTypes);
        authRepository.log(
            userId,
            "REGISTER",
            "USER",
            String.valueOf(userId),
            "/api/auth/register",
            "companyType=" + companyType + ";supplierServiceTypes=" + String.join(",", supplierServiceTypes)
        );
        return buildAuthResponse(userId, tokenService.issue(userId));
    }

    public AuthResponse login(LoginRequest request) {
        String account = required(firstText(request.account(), request.username(), request.phone()), "account");
        String password = required(request.password(), "password");
        AuthenticatedUser user = authRepository.findUserByAccount(account)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account or password"));
        if ("DISABLED".equals(user.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "USER_DISABLED");
        }
        if (!passwordHasher.matches(password, user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account or password");
        }
        authRepository.log(user.id(), "LOGIN", "USER", String.valueOf(user.id()), "/api/auth/login", "login");
        return buildAuthResponse(user.id(), tokenService.issue(user.id()));
    }

    public AuthResponse currentUser(String authorizationHeader) {
        return buildAuthResponse(tokenService.requireUserId(authorizationHeader), null);
    }

    public List<MenuResponse> menus(String authorizationHeader) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        CompanyResponse company = authRepository.getCompany(user.companyId());
        if (!ACTIVE.equals(user.status()) || !ACTIVE.equals(company.status())) {
            return List.of(onboardingMenu(defaultRoute(user.status(), company.status())));
        }
        return authRepository.buildTree(authRepository.menusForUser(userId));
    }

    public List<PermissionResponse> permissions(String authorizationHeader) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        CompanyResponse company = authRepository.getCompany(user.companyId());
        if (!ACTIVE.equals(user.status()) || !ACTIVE.equals(company.status())) {
            return List.of();
        }
        return authRepository.permissionsForUser(userId);
    }

    public RegisterOptionsResponse registerOptions() {
        List<OptionResponse> options = List.of(
            new OptionResponse("SHIP_AGENT", "船舶代理"),
            new OptionResponse("SUPPLIER", "供应服务商"),
            new OptionResponse("BARGE_AGENT", "驳船服务商")
        );
        List<OptionResponse> qualificationTypes = List.of(
            new OptionResponse("BUSINESS_LICENSE", "营业执照"),
            new OptionResponse("SERVICE_QUALIFICATION", "服务资质"),
            new OptionResponse("OTHER", "其他资质")
        );
        List<OptionResponse> supplierServiceTypes = List.of(
            new OptionResponse("MATERIAL", "物料供应服务"),
            new OptionResponse("FOOD", "伙食供货服务")
        );
        return new RegisterOptionsResponse(options, options, qualificationTypes, supplierServiceTypes);
    }

    AuthResponse buildAuthResponse(Long userId, String token) {
        AuthenticatedUser user = authRepository.getUserById(userId);
        CompanyResponse company = authRepository.getCompany(user.companyId());
        boolean active = ACTIVE.equals(user.status()) && ACTIVE.equals(company.status());
        List<PermissionResponse> permissions = active ? authRepository.permissionsForUser(user.id()) : List.of();
        List<MenuResponse> menus = active ? authRepository.menusForUser(userId) : List.of();
        String defaultRoute = active
            ? (menus.isEmpty() ? "/dashboard" : menus.get(0).routePath())
            : defaultRoute(user.status(), company.status());
        return new AuthResponse(
            token,
            new UserProfileResponse(user.id(), user.username(), user.phone(), user.name(), user.email(), user.userType(), user.status()),
            company,
            authRepository.rolesForUser(user.id()),
            permissions,
            defaultRoute,
            user.status(),
            user.status(),
            company.status(),
            !active,
            !active,
            defaultRoute,
            defaultRoute
        );
    }

    private String normalizeRole(String rawValue) {
        String value = firstText(rawValue, DEFAULT_ROLE).trim();
        String upper = value.toUpperCase(Locale.ROOT).replace("-", "_");
        if (upper.contains("SUPPLIER") || value.contains("供货")) {
            return "SUPPLIER";
        }
        if (upper.contains("BARGE") || value.contains("驳船")) {
            return "BARGE_AGENT";
        }
        if (upper.contains("ADMIN") || value.contains("平台")) {
            return "PLATFORM_ADMIN";
        }
        return "SHIP_AGENT";
    }

    String normalizeSelfServiceRole(String rawValue) {
        String role = normalizeRole(rawValue);
        if (!"SHIP_AGENT".equals(role) && !"SUPPLIER".equals(role) && !"BARGE_AGENT".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported self-service role");
        }
        return role;
    }

    List<String> normalizeSupplierServiceTypes(String companyType, List<String> values, boolean roleSelected) {
        List<String> normalized = values == null ? List.of() : values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(value -> value.trim().toUpperCase(Locale.ROOT))
            .distinct()
            .toList();
        if (!"SUPPLIER".equals(companyType)) {
            if (!normalized.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPPLIER_SERVICE_TYPES_NOT_ALLOWED");
            }
            return List.of();
        }
        if (roleSelected && normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPPLIER_SERVICE_TYPE_REQUIRED");
        }
        for (String value : normalized) {
            if (!"MATERIAL".equals(value) && !"FOOD".equals(value)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_SUPPLIER_SERVICE_TYPE");
            }
        }
        return normalized;
    }

    private String defaultRoute(String accountStatus, String companyStatus) {
        if (PENDING_REVIEW.equals(accountStatus) || PENDING_REVIEW.equals(companyStatus)) {
            return "/onboarding/review-status";
        }
        if (REJECTED.equals(accountStatus) || REJECTED.equals(companyStatus)) {
            return "/onboarding/company-profile";
        }
        if (PROFILE_REQUIRED.equals(accountStatus) || PROFILE_REQUIRED.equals(companyStatus)) {
            return "/onboarding/company-profile";
        }
        return "/dashboard";
    }

    private MenuResponse onboardingMenu(String route) {
        if ("/onboarding/review-status".equals(route)) {
            return new MenuResponse(
                "ONBOARDING_REVIEW_STATUS",
                "入驻审核状态",
                route,
                "Hourglass",
                null,
                810,
                "ONBOARDING_REVIEW_STATUS_VIEW",
                "SHIP_AGENT,SUPPLIER,BARGE_AGENT",
                List.of()
            );
        }
        return new MenuResponse(
            "ONBOARDING_PROFILE",
            "企业入驻资料",
            route,
            "ClipboardList",
            null,
            800,
            "ONBOARDING_PROFILE_VIEW",
            "SHIP_AGENT,SUPPLIER,BARGE_AGENT",
            List.of()
        );
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }
}
