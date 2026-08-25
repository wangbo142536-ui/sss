package com.zswy.shipsupply.auth;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanyProfileService {

    static final String DEFAULT_QUALIFICATION_STATUS = "SUBMITTED";
    static final String DELETED_QUALIFICATION_STATUS = "DELETED";
    static final String DEFAULT_CONTACT_STATUS = "ACTIVE";
    static final String DELETED_CONTACT_STATUS = "DELETED";
    static final String DEFAULT_VESSEL_STATUS = "ACTIVE";
    static final String DELETED_VESSEL_STATUS = "DELETED";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

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
        ContactNamePolicy.rejectPathValue(request.contactName());
        String introduction = request.companyIntroduction() == null ? null : request.companyIntroduction().trim();
        if (introduction != null && introduction.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "COMPANY_INTRODUCTION_TOO_LONG");
        }
        return companyProfileRepository.saveProfile(currentUser.companyId(), new EnterpriseProfileSaveRequest(
            request.companyName(),
            request.unifiedSocialCreditCode(),
            request.logoFileId(),
            request.logoUrl(),
            introduction,
            request.contactName(),
            request.contactPhone(),
            request.contactEmail()
        ));
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

    public CompanyContactListResponse contacts(String authorizationHeader, String status) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return new CompanyContactListResponse(companyProfileRepository.listContacts(currentUser.companyId(), contactStatus(status)));
    }

    @Transactional
    public CompanyContactResponse createContact(String authorizationHeader, CompanyContactSaveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return companyProfileRepository.createContact(currentUser.companyId(), validateContact(request, true));
    }

    @Transactional
    public CompanyContactResponse updateContact(String authorizationHeader, Long contactId, CompanyContactSaveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (contactId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CONTACT_ID_REQUIRED: contact id required");
        }
        return companyProfileRepository.updateContact(currentUser.companyId(), contactId, validateContact(request, false))
            .orElseThrow(CompanyProfileService::contactNotFound);
    }

    @Transactional
    public void deleteContact(String authorizationHeader, Long contactId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (contactId == null || !companyProfileRepository.softDeleteContact(currentUser.companyId(), contactId)) {
            throw contactNotFound();
        }
    }

    public CompanyVesselListResponse vessels(String authorizationHeader, String status) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return new CompanyVesselListResponse(companyProfileRepository.listVessels(currentUser.companyId(), vesselStatus(status)));
    }

    public CompanyValueAddedServiceResponse valueAddedServices(String authorizationHeader, Long companyId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        long targetCompanyId = companyId == null ? currentUser.companyId() : companyId;
        return companyProfileRepository.findValueAddedServices(targetCompanyId);
    }

    @Transactional
    public CompanyValueAddedServiceResponse saveValueAddedServices(String authorizationHeader, CompanyValueAddedServiceSaveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return companyProfileRepository.saveValueAddedServices(currentUser.companyId(), normalizeValueAddedServices(request));
    }

    @Transactional
    public CompanyVesselResponse createVessel(String authorizationHeader, CompanyVesselSaveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        return companyProfileRepository.createVessel(currentUser.companyId(), validateVessel(request));
    }

    @Transactional
    public CompanyVesselResponse updateVessel(String authorizationHeader, Long vesselId, CompanyVesselSaveRequest request) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (vesselId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VESSEL_ID_REQUIRED: vessel id required");
        }
        return companyProfileRepository.updateVessel(currentUser.companyId(), vesselId, validateVessel(request))
            .orElseThrow(CompanyProfileService::vesselNotFound);
    }

    @Transactional
    public void deleteVessel(String authorizationHeader, Long vesselId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        if (vesselId == null || !companyProfileRepository.softDeleteVessel(currentUser.companyId(), vesselId)) {
            throw vesselNotFound();
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

    private CompanyContactSaveRequest validateContact(CompanyContactSaveRequest request, boolean requireValues) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CONTACT_REQUIRED: contact required");
        }
        String contactName = ContactNamePolicy.requireValid(request.contactName(), "contactName");
        if (blank(request.contactPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CONTACT_PHONE_REQUIRED: contact phone required");
        }
        if (!blank(request.contactEmail()) && !EMAIL_PATTERN.matcher(request.contactEmail().trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CONTACT_EMAIL_INVALID: contact email invalid");
        }
        return new CompanyContactSaveRequest(
            contactName,
            trim(request.contactPhone()),
            trim(request.contactEmail()),
            normalizeContactStatus(request.status())
        );
    }

    private String contactStatus(String status) {
        return blank(status) ? DEFAULT_CONTACT_STATUS : status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String normalizeContactStatus(String status) {
        return blank(status) ? null : status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private CompanyVesselSaveRequest validateVessel(CompanyVesselSaveRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VESSEL_REQUIRED: vessel required");
        }
        if (blank(request.vesselName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VESSEL_NAME_REQUIRED: vessel name required");
        }
        return new CompanyVesselSaveRequest(
            trim(request.vesselName()),
            trim(request.vesselType()),
            trim(request.buildDate()),
            trim(request.nextMaintenanceDate()),
            trim(request.capacity()),
            normalizeVesselStatus(request.status()),
            trim(request.remark())
        );
    }

    private CompanyValueAddedServiceSaveRequest normalizeValueAddedServices(CompanyValueAddedServiceSaveRequest request) {
        if (request == null) {
            return new CompanyValueAddedServiceSaveRequest(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, null);
        }
        return new CompanyValueAddedServiceSaveRequest(
            nonNegative(request.freightPrice()),
            nonNegative(request.customsPrice()),
            nonNegative(request.cranePrice()),
            trim(request.remark())
        );
    }

    private java.math.BigDecimal nonNegative(java.math.BigDecimal value) {
        if (value == null || value.compareTo(java.math.BigDecimal.ZERO) < 0) {
            return java.math.BigDecimal.ZERO;
        }
        return value;
    }

    private String vesselStatus(String status) {
        return blank(status) ? DEFAULT_VESSEL_STATUS : status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String normalizeVesselStatus(String status) {
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

    private static ResponseStatusException contactNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "CONTACT_NOT_FOUND: contact not found or access denied");
    }

    private static ResponseStatusException vesselNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "VESSEL_NOT_FOUND: vessel not found or access denied");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
