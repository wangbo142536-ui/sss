package com.zswy.shipsupply.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    private static final String ACTIVE = "ACTIVE";

    private final TokenService tokenService;
    private final AuthRepository authRepository;

    public CurrentUserService(TokenService tokenService, AuthRepository authRepository) {
        this.tokenService = tokenService;
        this.authRepository = authRepository;
    }

    public CurrentUserContext requireActiveCompanyUser(String authorizationHeader) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        CompanyResponse company = authRepository.getCompany(user.companyId());
        if (!ACTIVE.equals(user.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "USER_DISABLED");
        }
        if (!ACTIVE.equals(company.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "COMPANY_NOT_ACTIVE");
        }
        return new CurrentUserContext(user.id(), user.companyId(), user.status(), company.status());
    }
}
