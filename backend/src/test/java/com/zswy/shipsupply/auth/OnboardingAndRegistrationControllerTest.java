package com.zswy.shipsupply.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

@WebMvcTest({OnboardingController.class, AdminRegistrationController.class})
class OnboardingAndRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OnboardingService onboardingService;

    @MockBean
    private AdminRegistrationService adminRegistrationService;

    @Test
    void submitsAndReadsCompanyProfile() throws Exception {
        var profile = new CompanyProfileResponse(
            new CompanyResponse(1L, "舟山测试船代", "SHIP_AGENT", "91330000TEST000001", "张三", "13800138000", "test@example.com", "PENDING_REVIEW"),
            "PENDING_REVIEW",
            "PENDING_REVIEW",
            true,
            "/onboarding/review-status",
            List.of(new QualificationFileResponse("FILE-1", "BUSINESS_LICENSE", "license.pdf", "SUBMITTED", "/api/files/FILE-1"))
        );
        when(onboardingService.submitProfile(any(), any(CompanyProfileRequest.class))).thenReturn(profile);
        when(onboardingService.currentProfile("Bearer dev-token")).thenReturn(profile);

        mockMvc.perform(post("/api/onboarding/company-profile")
                .header("Authorization", "Bearer dev-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "companyType": "SHIP_AGENT",
                      "companyName": "舟山测试船代",
                      "unifiedSocialCreditCode": "91330000TEST000001",
                      "contactName": "张三",
                      "contactPhone": "13800138000",
                      "contactEmail": "test@example.com",
                      "qualificationFileIds": ["FILE-1"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyStatus").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.defaultRoute").value("/onboarding/review-status"));

        mockMvc.perform(get("/api/onboarding/company-profile").header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.qualifications[0].fileId").value("FILE-1"));
    }

    @Test
    void platformAdminReviewsRegistrations() throws Exception {
        when(adminRegistrationService.registrations("Bearer admin-token", "PENDING_REVIEW", "舟山", 1, 20)).thenReturn(List.of(
            new RegistrationReviewResponse(
                2L,
                2L,
                2L,
                "13800138000",
                "13800138000",
                "舟山测试船代",
                "SHIP_AGENT",
                "张三",
                "13800138000",
                "test@example.com",
                "PENDING_REVIEW",
                "PENDING_REVIEW",
                "PENDING_REVIEW",
                "PENDING_REVIEW",
                "2026-06-04T15:30:00",
                null,
                null,
                null,
                List.of()
            )
        ));
        when(adminRegistrationService.approve("Bearer admin-token", 2L)).thenReturn(new AuthResponse(
            null,
            new UserProfileResponse(2L, "13800138000", "13800138000", "SHIP_AGENT", "ACTIVE"),
            new CompanyResponse(2L, "舟山测试船代", "SHIP_AGENT", "91330000TEST000001", "张三", "13800138000", "test@example.com", "ACTIVE"),
            List.of(new RoleResponse("SHIP_AGENT", "船代", "SHIP_AGENT")),
            List.of(new PermissionResponse("DASHBOARD_VIEW", "Dashboard查看", "MENU", "DASHBOARD", "VIEW")),
            "/dashboard",
            "ACTIVE",
            "ACTIVE",
            "ACTIVE",
            false,
            false,
            "/dashboard",
            "/dashboard"
        ));

        mockMvc.perform(get("/api/admin/registrations")
                .header("Authorization", "Bearer admin-token")
                .param("status", "PENDING_REVIEW")
                .param("keyword", "舟山")
                .param("page", "1")
                .param("pageSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].companyId").value(2))
            .andExpect(jsonPath("$[0].status").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$[0].companyStatus").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$[0].profileStatus").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$[0].submittedAt").value("2026-06-04T15:30:00"));
        verify(adminRegistrationService).registrations(eq("Bearer admin-token"), eq("PENDING_REVIEW"), eq("舟山"), eq(1), eq(20));
        mockMvc.perform(post("/api/admin/registrations/2/approve").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
        mockMvc.perform(post("/api/admin/registrations/2/reject")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"资料不完整\"}"))
            .andExpect(status().isNoContent());
    }
}
