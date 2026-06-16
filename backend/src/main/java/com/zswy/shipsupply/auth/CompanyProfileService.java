package com.zswy.shipsupply.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanyProfileService {

    static final String DEFAULT_QUALIFICATION_STATUS = "SUBMITTED";
    static final String DELETED_QUALIFICATION_STATUS = "DELETED";

    private final CurrentUserService currentUserService;
    private final CompanyProfileRepository companyProfileRepository;

    public CompanyProfileService(CurrentUserService currentUserService, CompanyProfileRepository companyProfileRepository) {
        this.currentUserService = currentUserService;
        this.companyProfileRepository = companyProfileRepository;
    }

    public EnterpriseProfileResponse profile(String authorizationHeader) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return companyProfileRepository.findProfile(currentUser.companyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND: company not found"));
    }

    @Transactional
    public EnterpriseProfileResponse saveProfile(String authorizationHeader, EnterpriseProfileSaveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "COMPANY_PROFILE_REQUIRED: company profile required");
        }
        return companyProfileRepository.saveProfile(currentUser.companyId(), request);
    }

    public CompanyQualificationListResponse qualifications(String authorizationHeader, String status) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return new CompanyQualificationListResponse(companyProfileRepository.listQualifications(currentUser.companyId(), qualificationStatus(status)));
    }

    @Transactional
    public CompanyQualificationResponse createQualification(String authorizationHeader, CompanyQualificationSaveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        CompanyQualificationSaveRequest safeRequest = validateQualification(request, true);
        return companyProfileRepository.createQualification(currentUser.companyId(), safeRequest);
    }

    @Transactional
    public CompanyQualificationResponse updateQualification(String authorizationHeader, Long qualificationId, CompanyQualificationSaveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (qualificationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QUALIFICATION_ID_REQUIRED: qualification id required");
        }
        CompanyQualificationSaveRequest safeRequest = validateQualification(request, false);
        return companyProfileRepository.updateQualification(currentUser.companyId(), qualificationId, safeRequest)
            .orElseThrow(CompanyProfileService::qualificationNotFound);
    }

    @Transactional
    public void deleteQualification(String authorizationHeader, Long qualificationId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (qualificationId == null || !companyProfileRepository.softDeleteQualification(currentUser.companyId(), qualificationId)) {
            throw qualificationNotFound();
        }
    }

    private CompanyQualificationSaveRequest validateQualification(CompanyQualificationSaveRequest request, boolean requireFile) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QUALIFICATION_REQUIRED: qualification required");
        }
        String fileId = valueFromFileUrl(request.fileId(), request.fileUrl());
        if (requireFile && blank(fileId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QUALIFICATION_FILE_REQUIRED: qualification file required");
        }
        return new CompanyQualificationSaveRequest(
            trim(fileId),
            trim(request.fileName()),
            trim(request.fileUrl()),
            normalizeCode(request.qualificationType()),
            trim(request.title()),
            trim(request.description()),
            normalizeQualificationStatus(request.status())
        );
    }

    private String qualificationStatus(String status) {
        return blank(status) ? DEFAULT_QUALIFICATION_STATUS : status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String normalizeQualificationStatus(String status) {
        return blank(status) ? null : status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return blank(value) ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String valueFromFileUrl(String fileId, String fileUrl) {
        if (!blank(fileId)) {
            return fileId;
        }
        String prefix = "/api/files/";
        if (fileUrl != null && fileUrl.startsWith(prefix) && fileUrl.length() > prefix.length()) {
            return fileUrl.substring(prefix.length()).trim();
        }
        return fileId;
    }

    private static ResponseStatusException qualificationNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "QUALIFICATION_NOT_FOUND: qualification not found or access denied");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
