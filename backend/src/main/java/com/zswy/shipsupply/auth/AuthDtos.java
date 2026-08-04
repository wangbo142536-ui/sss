package com.zswy.shipsupply.auth;

import java.util.List;

import org.springframework.core.io.Resource;

record RegisterRequest(
    String account,
    String companyName,
    String company,
    String contactName,
    String contact,
    String phone,
    String contactPhone,
    String companyType,
    String userType,
    String username,
    String password,
    String confirmPassword,
    List<String> supplierServiceTypes
) {
    RegisterRequest(
        String account,
        String companyName,
        String company,
        String contactName,
        String contact,
        String phone,
        String contactPhone,
        String companyType,
        String userType,
        String username,
        String password,
        String confirmPassword
    ) {
        this(account, companyName, company, contactName, contact, phone, contactPhone, companyType, userType, username,
            password, confirmPassword, List.of());
    }
}

record RegistrationSubmissionRequest(
    String account,
    String password,
    String confirmPassword,
    String companyType,
    List<String> supplierServiceTypes,
    String companyName,
    String unifiedSocialCreditCode,
    String contactName,
    String contactPhone,
    String contactEmail
) {
}

record LoginRequest(
    String account,
    String username,
    String phone,
    String password
) {
}

record AuthResponse(
    String token,
    UserProfileResponse user,
    CompanyResponse company,
    List<RoleResponse> roles,
    List<PermissionResponse> permissions,
    String defaultHome,
    String accountStatus,
    String profileStatus,
    String companyStatus,
    boolean onboardingRequired,
    boolean requiresOnboarding,
    String defaultRoute,
    String redirectTo
) {
}

record UserProfileResponse(
    Long id,
    String username,
    String phone,
    String name,
    String email,
    String userType,
    String status
) {
    UserProfileResponse(Long id, String username, String phone, String userType, String status) {
        this(id, username, phone, null, null, userType, status);
    }
}

record CompanyResponse(
    Long id,
    String companyName,
    String companyType,
    String unifiedSocialCreditCode,
    String contactName,
    String contactPhone,
    String contactEmail,
    String status,
    List<String> supplierServiceTypes
) {
    CompanyResponse(
        Long id,
        String companyName,
        String companyType,
        String unifiedSocialCreditCode,
        String contactName,
        String contactPhone,
        String contactEmail,
        String status
    ) {
        this(id, companyName, companyType, unifiedSocialCreditCode, contactName, contactPhone, contactEmail, status, List.of());
    }
}

record RoleResponse(
    String roleCode,
    String roleName,
    String roleType,
    List<String> menuPermissionKeys
) {
    RoleResponse(String roleCode, String roleName, String roleType) {
        this(roleCode, roleName, roleType, List.of());
    }
}

record PermissionResponse(
    String permissionCode,
    String permissionName,
    String permissionType,
    String resourceCode,
    String actionCode
) {
}

record MenuResponse(
    String menuCode,
    String menuName,
    String routePath,
    String icon,
    String parentCode,
    int sortOrder,
    String requiredPermission,
    String visibleRoles,
    List<MenuResponse> children
) {
}

record MenuPermissionResponse(
    Long menuId,
    String menuCode,
    String menuName,
    String parentCode,
    int sortOrder,
    String requiredPermission
) {
}

record AdminMenuResponse(
    Long menuId,
    String menuCode,
    String menuName,
    String routePath,
    String icon,
    String parentCode,
    int sortOrder,
    String requiredPermission,
    String visibleRoles,
    boolean enabled
) {
}

record AdminMenuTreeResponse(
    Long menuId,
    String menuCode,
    String menuName,
    String routePath,
    String icon,
    String parentCode,
    int sortOrder,
    String requiredPermission,
    String visibleRoles,
    boolean enabled,
    List<AdminMenuTreeResponse> children
) {
}

record MenuSortOrderUpdateRequest(
    List<MenuSortOrderItem> items
) {
}

record MenuSortOrderItem(
    String menuCode,
    Integer sortOrder
) {
}

record MenuStructureUpdateRequest(
    List<MenuStructureItem> items
) {
}

record MenuStructureItem(
    String menuCode,
    String parentCode,
    Integer sortOrder,
    Boolean enabled
) {
}

record OptionResponse(
    String code,
    String name
) {
}

record RegisterOptionsResponse(
    List<OptionResponse> companyTypes,
    List<OptionResponse> userTypes,
    List<OptionResponse> qualificationTypes,
    List<OptionResponse> supplierServiceTypes
) {
    RegisterOptionsResponse(
        List<OptionResponse> companyTypes,
        List<OptionResponse> userTypes,
        List<OptionResponse> qualificationTypes
    ) {
        this(companyTypes, userTypes, qualificationTypes, List.of());
    }
}

record RolePermissionUpdateRequest(
    List<String> permissionCodes
) {
}

record UserRoleUpdateRequest(
    List<String> roleCodes
) {
}

record CompanyMembersResponse(
    List<CompanyMemberResponse> items
) {
}

record CompanyMemberResponse(
    Long userId,
    String username,
    String name,
    String phone,
    String email,
    String userType,
    String status,
    boolean isCompanyOwner,
    List<String> roles,
    List<String> menuPermissionKeys
) {
    CompanyMemberResponse(Long userId, String username, String phone, String userType, String status, boolean isCompanyOwner, List<String> roles) {
        this(userId, username, null, phone, null, userType, status, isCompanyOwner, roles, List.of());
    }
}

record CompanyMemberCreateRequest(
    String username,
    String name,
    String phone,
    String email,
    String password,
    List<String> roles,
    List<String> roleCodes,
    List<String> menuPermissionKeys
) {
}

record CompanyMemberUpdateRequest(
    String name,
    String phone,
    String email,
    List<String> roles,
    List<String> roleCodes
) {
}

record CompanyMemberStatusRequest(
    String status
) {
}

record CompanyMemberResetPasswordRequest(
    String newPassword
) {
}

record CompanyMemberRolesRequest(
    List<String> roles
) {
}

record CompanyRolesResponse(
    List<RoleResponse> items
) {
}

record CompanyRoleUpsertRequest(
    String roleCode,
    String roleName,
    String roleType,
    List<String> menuPermissionKeys
) {
}

record MenuPermissionKeysRequest(
    List<String> menuPermissionKeys
) {
}

record AdminUserResponse(
    Long id,
    String username,
    String phone,
    String userType,
    String status,
    String companyName,
    List<String> roleCodes
) {
}

record CompanyProfileRequest(
    String companyType,
    String companyName,
    String unifiedSocialCreditCode,
    String contactName,
    String contactPhone,
    String contactEmail,
    List<String> qualificationFileIds,
    List<String> supplierServiceTypes
) {
    CompanyProfileRequest(
        String companyType,
        String companyName,
        String unifiedSocialCreditCode,
        String contactName,
        String contactPhone,
        String contactEmail,
        List<String> qualificationFileIds
    ) {
        this(companyType, companyName, unifiedSocialCreditCode, contactName, contactPhone, contactEmail,
            qualificationFileIds, List.of());
    }
}

record PlatformAccountStatusRequest(
    String status,
    String reason
) {
}

record PlatformAccountResponse(
    Long userId,
    String username,
    String name,
    String phone,
    String email,
    String userType,
    String accountSource,
    boolean isCompanyOwner,
    List<String> roleCodes,
    String status,
    String createdAt,
    String lastLoginAt
) {
}

record PlatformCompanyAccountsResponse(
    Long companyId,
    String companyName,
    String companyType,
    List<String> supplierServiceTypes,
    String companyStatus,
    int accountCount,
    int activeAccountCount,
    List<PlatformAccountResponse> accounts
) {
}

record PlatformCompanyAccountPageResponse(
    List<PlatformCompanyAccountsResponse> items,
    int page,
    int pageSize,
    long totalCompanies,
    long totalAccounts
) {
}

record QualificationFileResponse(
    String fileId,
    String fileType,
    String fileName,
    String status,
    String url
) {
}

record CompanyProfileResponse(
    CompanyResponse company,
    String accountStatus,
    String companyStatus,
    boolean onboardingRequired,
    String defaultRoute,
    List<QualificationFileResponse> qualifications
) {
}

record FileUploadResponse(
    String fileId,
    String fileName,
    String contentType,
    long fileSize,
    String url
) {
}

record FileDownloadResponse(
    Resource resource,
    String fileName,
    String contentType,
    long fileSize
) {
}

record StoredFileResponse(
    String fileId,
    String fileName,
    String contentType,
    long fileSize,
    String storagePath,
    Long uploaderUserId,
    Long qualificationCompanyId
) {
}

record RegistrationReviewResponse(
    Long id,
    Long userId,
    Long companyId,
    String username,
    String phone,
    String companyName,
    String companyType,
    String contactName,
    String contactPhone,
    String contactEmail,
    String status,
    String accountStatus,
    String companyStatus,
    String profileStatus,
    String submittedAt,
    String reviewedAt,
    Long reviewerUserId,
    String reviewReason,
    List<String> supplierServiceTypes,
    List<QualificationFileResponse> qualifications
) {
    RegistrationReviewResponse(
        Long id,
        Long userId,
        Long companyId,
        String username,
        String phone,
        String companyName,
        String companyType,
        String contactName,
        String contactPhone,
        String contactEmail,
        String status,
        String accountStatus,
        String companyStatus,
        String profileStatus,
        String submittedAt,
        String reviewedAt,
        Long reviewerUserId,
        String reviewReason,
        List<QualificationFileResponse> qualifications
    ) {
        this(id, userId, companyId, username, phone, companyName, companyType, contactName, contactPhone, contactEmail,
            status, accountStatus, companyStatus, profileStatus, submittedAt, reviewedAt, reviewerUserId, reviewReason,
            List.of(), qualifications);
    }
}

record RejectRegistrationRequest(
    String reason
) {
}

record AuthenticatedUser(
    Long id,
    String username,
    String phone,
    String name,
    String email,
    String passwordHash,
    String userType,
    String status,
    Long companyId
) {
    AuthenticatedUser(Long id, String username, String phone, String passwordHash, String userType, String status, Long companyId) {
        this(id, username, phone, null, null, passwordHash, userType, status, companyId);
    }
}
