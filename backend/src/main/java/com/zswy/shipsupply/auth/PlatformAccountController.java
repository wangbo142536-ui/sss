package com.zswy.shipsupply.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class PlatformAccountController {

    private final PlatformAccountService platformAccountService;

    public PlatformAccountController(PlatformAccountService platformAccountService) {
        this.platformAccountService = platformAccountService;
    }

    @GetMapping("/companies/accounts")
    public PlatformCompanyAccountPageResponse accounts(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(required = false) String companyType,
        @RequestParam(required = false) String supplierServiceType,
        @RequestParam(required = false) String accountSource,
        @RequestParam(required = false) String roleCode,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer pageSize
    ) {
        return platformAccountService.accounts(
            authorizationHeader,
            companyType,
            supplierServiceType,
            accountSource,
            roleCode,
            status,
            keyword,
            page,
            pageSize
        );
    }

    @PatchMapping("/accounts/{userId}/status")
    public PlatformAccountResponse updateStatus(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long userId,
        @RequestBody PlatformAccountStatusRequest request
    ) {
        return platformAccountService.updateStatus(authorizationHeader, userId, request);
    }
}
