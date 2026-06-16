package com.zswy.shipsupply.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registrations")
public class AdminRegistrationController {

    private final AdminRegistrationService adminRegistrationService;

    public AdminRegistrationController(AdminRegistrationService adminRegistrationService) {
        this.adminRegistrationService = adminRegistrationService;
    }

    @GetMapping
    public List<RegistrationReviewResponse> registrations(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize
    ) {
        return adminRegistrationService.registrations(authorizationHeader, status, keyword, page, pageSize);
    }

    @PostMapping("/{id}/approve")
    public AuthResponse approve(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ) {
        return adminRegistrationService.approve(authorizationHeader, id);
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id,
        @RequestBody(required = false) RejectRegistrationRequest request
    ) {
        adminRegistrationService.reject(authorizationHeader, id, request);
    }
}
