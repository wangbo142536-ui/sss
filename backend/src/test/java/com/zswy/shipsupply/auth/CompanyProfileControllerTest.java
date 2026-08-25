package com.zswy.shipsupply.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompanyProfileController.class)
class CompanyProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyProfileService companyProfileService;

    @Test
    void exposesCompanyProfileEndpoints() throws Exception {
        when(companyProfileService.profile("Bearer token")).thenReturn(profile());
        when(companyProfileService.saveProfile(eq("Bearer token"), any(EnterpriseProfileSaveRequest.class))).thenReturn(profile());

        mockMvc.perform(get("/api/company/profile").header("Authorization", "Bearer token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").value("Zhoushan Supplier"))
            .andExpect(jsonPath("$.unifiedSocialCreditCode").value("91330000TEST"))
            .andExpect(jsonPath("$.logoUrl").value("/api/files/LOGO-1"));

        mockMvc.perform(put("/api/company/profile")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "companyName": "Zhoushan Supplier",
                      "unifiedSocialCreditCode": "91330000TEST",
                      "logoFileId": "LOGO-1",
                      "logoUrl": "/api/files/LOGO-1",
                      "contactName": "Wang Bo",
                      "contactPhone": "13800000000",
                      "contactEmail": "wb@example.com"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contactName").value("Wang Bo"));
    }

    @Test
    void exposesCompanyQualificationCrudEndpoints() throws Exception {
        CompanyQualificationResponse qualification = qualification(9L);
        when(companyProfileService.qualifications("Bearer token", null)).thenReturn(new CompanyQualificationListResponse(List.of(qualification)));
        when(companyProfileService.createQualification(eq("Bearer token"), any(CompanyQualificationSaveRequest.class))).thenReturn(qualification);
        when(companyProfileService.updateQualification(eq("Bearer token"), eq(9L), any(CompanyQualificationSaveRequest.class))).thenReturn(qualification);

        mockMvc.perform(get("/api/company/qualifications").header("Authorization", "Bearer token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].qualificationId").value(9))
            .andExpect(jsonPath("$.items[0].fileUrl").value("/api/files/FILE-1"));

        mockMvc.perform(post("/api/company/qualifications")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fileId": "FILE-1",
                      "fileName": "business-license.pdf",
                      "qualificationType": "BUSINESS_LICENSE",
                      "title": "Business License",
                      "description": "Latest version",
                      "status": "ACTIVE"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Business License"));

        mockMvc.perform(put("/api/company/qualifications/9")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Business License\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/company/qualifications/9").header("Authorization", "Bearer token"))
            .andExpect(status().isNoContent());

        verify(companyProfileService).deleteQualification("Bearer token", 9L);
    }

    private EnterpriseProfileResponse profile() {
        return new EnterpriseProfileResponse(
            22L,
            "Zhoushan Supplier",
            "91330000TEST",
            "LOGO-1",
            "/api/files/LOGO-1",
            "Professional marine supply enterprise.",
            "Wang Bo",
            "13800000000",
            "wb@example.com",
            "SUPPLIER",
            "ACTIVE"
        );
    }

    private CompanyQualificationResponse qualification(Long id) {
        return new CompanyQualificationResponse(
            id,
            22L,
            "FILE-1",
            "business-license.pdf",
            "/api/files/FILE-1",
            "BUSINESS_LICENSE",
            "Business License",
            "Latest version",
            "ACTIVE",
            "2026-06-10T09:00:00",
            "2026-06-10T09:05:00"
        );
    }
}
