package com.zswy.shipsupply.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

@WebMvcTest(CompanyMemberController.class)
class CompanyMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyMemberService companyMemberService;

    @Test
    void exposesCompanyMemberManagementEndpoints() throws Exception {
        when(companyMemberService.members("Bearer owner-token", null, null)).thenReturn(
            new CompanyMembersResponse(List.of(member(30L, "ops-01")))
        );
        when(companyMemberService.createMember(eq("Bearer owner-token"), any(CompanyMemberCreateRequest.class)))
            .thenReturn(member(31L, "ops-02"));
        when(companyMemberService.updateMemberStatus(eq("Bearer owner-token"), eq(31L), any(CompanyMemberStatusRequest.class)))
            .thenReturn(member(31L, "ops-02"));
        when(companyMemberService.availableRoles("Bearer owner-token")).thenReturn(
            new CompanyRolesResponse(List.of(new RoleResponse("SHIP_AGENT", "船代", "SHIP_AGENT")))
        );

        mockMvc.perform(get("/api/company/members").header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].username").value("ops-01"));

        mockMvc.perform(post("/api/company/members")
                .header("Authorization", "Bearer owner-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "ops-02",
                      "phone": "13800000002",
                      "password": "Temp@123456",
                      "roles": ["SHIP_AGENT"]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(31));

        mockMvc.perform(patch("/api/company/members/31/status")
                .header("Authorization", "Bearer owner-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DISABLED\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/company/members/31/reset-password")
                .header("Authorization", "Bearer owner-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"New@123456\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/company/members/31/roles")
                .header("Authorization", "Bearer owner-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"SHIP_AGENT\"]}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/company/roles").header("Authorization", "Bearer owner-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].roleCode").value("SHIP_AGENT"));

        verify(companyMemberService).resetMemberPassword(eq("Bearer owner-token"), eq(31L), any(CompanyMemberResetPasswordRequest.class));
        verify(companyMemberService).updateMemberRoles(eq("Bearer owner-token"), eq(31L), any(CompanyMemberRolesRequest.class));
    }

    private CompanyMemberResponse member(Long id, String username) {
        return new CompanyMemberResponse(id, username, "13800000001", "SHIP_AGENT", "ACTIVE", false, List.of("SHIP_AGENT"));
    }
}
