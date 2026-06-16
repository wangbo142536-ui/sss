package com.zswy.shipsupply.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    public CompanyProfileController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    @GetMapping("/profile")
    public EnterpriseProfileResponse profile(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return companyProfileService.profile(authorizationHeader);
    }

    @PutMapping("/profile")
    public EnterpriseProfileResponse saveProfile(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody EnterpriseProfileSaveRequest request
    ) {
        return companyProfileService.saveProfile(authorizationHeader, request);
    }

    @GetMapping("/qualifications")
    public CompanyQualificationListResponse qualifications(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(value = "status", required = false) String status
    ) {
        return companyProfileService.qualifications(authorizationHeader, status);
    }

    @PostMapping("/qualifications")
    public CompanyQualificationResponse createQualification(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody CompanyQualificationSaveRequest request
    ) {
        return companyProfileService.createQualification(authorizationHeader, request);
    }

    @PutMapping("/qualifications/{qualificationId}")
    public CompanyQualificationResponse updateQualification(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long qualificationId,
        @RequestBody CompanyQualificationSaveRequest request
    ) {
        return companyProfileService.updateQualification(authorizationHeader, qualificationId, request);
    }

    @DeleteMapping("/qualifications/{qualificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQualification(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long qualificationId
    ) {
        companyProfileService.deleteQualification(authorizationHeader, qualificationId);
    }
}

