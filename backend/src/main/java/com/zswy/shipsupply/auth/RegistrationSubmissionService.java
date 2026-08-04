package com.zswy.shipsupply.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationSubmissionService {

    private static final String PENDING_REVIEW = "PENDING_REVIEW";

    private final AuthRepository authRepository;
    private final AuthService authService;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final FileStorageService fileStorageService;

    public RegistrationSubmissionService(
        AuthRepository authRepository,
        AuthService authService,
        PasswordHasher passwordHasher,
        TokenService tokenService,
        FileStorageService fileStorageService
    ) {
        this.authRepository = authRepository;
        this.authService = authService;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public AuthResponse registerAndSubmit(RegistrationSubmissionRequest request, List<MultipartFile> files) {
        String account = required(request.account(), "account");
        String password = required(request.password(), "password");
        if (!password.equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "confirmPassword does not match password");
        }
        String companyType = authService.normalizeSelfServiceRole(required(request.companyType(), "companyType"));
        List<String> supplierServiceTypes = authService.normalizeSupplierServiceTypes(
            companyType,
            request.supplierServiceTypes(),
            true
        );
        String companyName = required(request.companyName(), "companyName");
        String creditCode = required(request.unifiedSocialCreditCode(), "unifiedSocialCreditCode");
        String contactName = required(request.contactName(), "contactName");
        String contactPhone = required(request.contactPhone(), "contactPhone");
        String contactEmail = required(request.contactEmail(), "contactEmail");
        if (files == null || files.isEmpty() || files.stream().anyMatch(file -> file == null || file.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "qualification files are required");
        }
        if (authRepository.userExists(account, contactPhone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        long companyId = authRepository.insertCompany(companyName, companyType, contactName, contactPhone, contactEmail, PENDING_REVIEW);
        authRepository.updateCompanyProfile(
            companyId,
            companyType,
            companyName,
            creditCode,
            contactName,
            contactPhone,
            contactEmail,
            PENDING_REVIEW,
            null
        );
        long userId = authRepository.insertUser(
            companyId,
            account,
            contactPhone,
            passwordHasher.hash(password),
            companyType,
            PENDING_REVIEW
        );
        authRepository.markCompanyOwner(userId);
        authRepository.replaceCompanySupplierServiceTypes(companyId, supplierServiceTypes);
        List<String> fileIds = files.stream()
            .map(file -> fileStorageService.storeForUser(userId, file).fileId())
            .toList();
        authRepository.replaceCompanyQualifications(companyId, fileIds);
        authRepository.log(
            userId,
            "REGISTER_AND_SUBMIT_ONBOARDING",
            "COMPANY",
            String.valueOf(companyId),
            "/api/auth/register-and-submit",
            "companyType=" + companyType + ";supplierServiceTypes=" + String.join(",", supplierServiceTypes)
        );
        return authService.buildAuthResponse(userId, tokenService.issue(userId));
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }
}
