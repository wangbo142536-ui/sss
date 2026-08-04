package com.zswy.shipsupply.auth;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RegistrationSubmissionService registrationSubmissionService;

    public AuthController(AuthService authService, RegistrationSubmissionService registrationSubmissionService) {
        this.authService = authService;
        this.registrationSubmissionService = registrationSubmissionService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping(value = "/register-and-submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AuthResponse registerAndSubmit(
        @RequestPart("request") RegistrationSubmissionRequest request,
        @RequestPart("files") List<MultipartFile> files
    ) {
        return registrationSubmissionService.registerAndSubmit(request, files);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthResponse me(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.currentUser(authorizationHeader);
    }

    @GetMapping("/menus")
    public List<MenuResponse> menus(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.menus(authorizationHeader);
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> permissions(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return authService.permissions(authorizationHeader);
    }

    @GetMapping("/register/options")
    public RegisterOptionsResponse registerOptions() {
        return authService.registerOptions();
    }
}
