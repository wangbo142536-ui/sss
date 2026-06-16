package com.zswy.shipsupply.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void registersWithAccountPasswordOnly() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "account": "dock-agent-01",
                      "password": "secret123",
                      "confirmPassword": "secret123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("dev-token"))
            .andExpect(jsonPath("$.user.username").value("dock-agent-01"))
            .andExpect(jsonPath("$.company.companyName").value("pending-company-dock-agent-01"))
            .andExpect(jsonPath("$.roles.length()").value(0))
            .andExpect(jsonPath("$.permissions.length()").value(0))
            .andExpect(jsonPath("$.accountStatus").value("PROFILE_REQUIRED"))
            .andExpect(jsonPath("$.profileStatus").value("PROFILE_REQUIRED"))
            .andExpect(jsonPath("$.onboardingRequired").value(true))
            .andExpect(jsonPath("$.requiresOnboarding").value(true))
            .andExpect(jsonPath("$.redirectTo").value("/onboarding/company-profile"))
            .andExpect(jsonPath("$.defaultRoute").value("/onboarding/company-profile"));
    }

    @Test
    void logsInAndReturnsPermissionEnvelope() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "account": "13800138000",
                      "password": "123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("dev-token"))
            .andExpect(jsonPath("$.user.username").value("dock-agent-01"))
            .andExpect(jsonPath("$.roles.length()").value(0))
            .andExpect(jsonPath("$.permissions.length()").value(0))
            .andExpect(jsonPath("$.defaultRoute").value("/onboarding/company-profile"));
    }

    @Test
    void returnsCurrentUserMenusPermissionsAndRegisterOptions() throws Exception {
        when(authService.currentUser("Bearer dev-token")).thenReturn(sampleAuthResponse());
        when(authService.menus("Bearer dev-token")).thenReturn(List.of(
            new MenuResponse("ONBOARDING_PROFILE", "企业入驻资料", "/onboarding/company-profile", "ClipboardList", null, 10, "ONBOARDING_PROFILE_VIEW", "SHIP_AGENT,SUPPLIER", List.of())
        ));
        when(authService.permissions("Bearer dev-token")).thenReturn(List.of());
        when(authService.registerOptions()).thenReturn(new RegisterOptionsResponse(
            List.of(new OptionResponse("SHIP_AGENT", "船代")),
            List.of(new OptionResponse("SHIP_AGENT", "船代")),
            List.of(new OptionResponse("BUSINESS_LICENSE", "营业执照"))
        ));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.username").value("dock-agent-01"));
        mockMvc.perform(get("/api/auth/menus").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].routePath").value("/onboarding/company-profile"));
        mockMvc.perform(get("/api/auth/permissions").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/auth/register/options"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyTypes[0].code").value("SHIP_AGENT"));
    }

    private AuthResponse sampleAuthResponse() {
        return new AuthResponse(
            "dev-token",
            new UserProfileResponse(1L, "dock-agent-01", null, "UNSPECIFIED", "PROFILE_REQUIRED"),
            new CompanyResponse(1L, "pending-company-dock-agent-01", "UNSPECIFIED", null, "dock-agent-01", "", null, "PROFILE_REQUIRED"),
            List.of(),
            List.of(),
            "/onboarding/company-profile",
            "PROFILE_REQUIRED",
            "PROFILE_REQUIRED",
            "PROFILE_REQUIRED",
            true,
            true,
            "/onboarding/company-profile",
            "/onboarding/company-profile"
        );
    }
}
