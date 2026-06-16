package com.zswy.shipsupply.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company")
public class CompanyMemberController {

    private final CompanyMemberService companyMemberService;

    public CompanyMemberController(CompanyMemberService companyMemberService) {
        this.companyMemberService = companyMemberService;
    }

    @GetMapping("/members")
    public CompanyMembersResponse members(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return companyMemberService.members(authorizationHeader, status, keyword);
    }

    @PutMapping("/members/{userId}")
    public CompanyMemberResponse updateMember(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long userId,
        @RequestBody CompanyMemberUpdateRequest request
    ) {
        return companyMemberService.updateMember(authorizationHeader, userId, request);
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyMemberResponse createMember(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody CompanyMemberCreateRequest request
    ) {
        return companyMemberService.createMember(authorizationHeader, request);
    }

    @PatchMapping("/members/{userId}/status")
    public CompanyMemberResponse updateMemberStatus(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long userId,
        @RequestBody CompanyMemberStatusRequest request
    ) {
        return companyMemberService.updateMemberStatus(authorizationHeader, userId, request);
    }

    @PostMapping("/members/{userId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetMemberPassword(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long userId,
        @RequestBody CompanyMemberResetPasswordRequest request
    ) {
        companyMemberService.resetMemberPassword(authorizationHeader, userId, request);
    }

    @PutMapping("/members/{userId}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMemberRoles(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long userId,
        @RequestBody CompanyMemberRolesRequest request
    ) {
        companyMemberService.updateMemberRoles(authorizationHeader, userId, request);
    }

    @GetMapping("/roles")
    public CompanyRolesResponse availableRoles(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return companyMemberService.availableRoles(authorizationHeader);
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse createRole(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody CompanyRoleUpsertRequest request
    ) {
        return companyMemberService.createRole(authorizationHeader, request);
    }

    @PutMapping("/roles/{roleCode}")
    public RoleResponse updateRole(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable String roleCode,
        @RequestBody CompanyRoleUpsertRequest request
    ) {
        return companyMemberService.updateRole(authorizationHeader, roleCode, request);
    }

    @DeleteMapping("/roles/{roleCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable String roleCode
    ) {
        companyMemberService.deleteRole(authorizationHeader, roleCode);
    }

    @GetMapping("/menus/permissions")
    public List<MenuResponse> menuPermissions(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return companyMemberService.menuPermissions(authorizationHeader);
    }

    @GetMapping("/members/{userId}/menu-permissions")
    public MenuPermissionKeysRequest memberMenuPermissions(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long userId
    ) {
        return companyMemberService.memberMenuPermissions(authorizationHeader, userId);
    }

    @PutMapping("/members/{userId}/menu-permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMemberMenuPermissions(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long userId,
        @RequestBody MenuPermissionKeysRequest request
    ) {
        companyMemberService.updateMemberMenuPermissions(authorizationHeader, userId, request);
    }
}
