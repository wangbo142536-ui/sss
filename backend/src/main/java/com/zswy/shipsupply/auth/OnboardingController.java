package com.zswy.shipsupply.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding/company-profile")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public CompanyProfileResponse currentProfile(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return onboardingService.currentProfile(authorizationHeader);
    }

    @PostMapping
    public CompanyProfileResponse submitProfile(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody CompanyProfileRequest request
    ) {
        return onboardingService.submitProfile(authorizationHeader, request);
    }
}
