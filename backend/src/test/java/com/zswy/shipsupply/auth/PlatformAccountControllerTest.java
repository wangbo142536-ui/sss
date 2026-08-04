package com.zswy.shipsupply.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlatformAccountController.class)
class PlatformAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatformAccountService platformAccountService;

    @Test
    void listsGroupedCompanyAccountsWithFilters() throws Exception {
        when(platformAccountService.accounts(
            "Bearer admin-token",
            "SUPPLIER",
            "FOOD",
            null,
            null,
            "ACTIVE",
            "舟山",
            1,
            20
        )).thenReturn(new PlatformCompanyAccountPageResponse(List.of(), 1, 20, 0, 0));

        mockMvc.perform(get("/api/platform/companies/accounts")
                .header("Authorization", "Bearer admin-token")
                .param("companyType", "SUPPLIER")
                .param("supplierServiceType", "FOOD")
                .param("status", "ACTIVE")
                .param("keyword", "舟山")
                .param("page", "1")
                .param("pageSize", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void updatesAccountStatusWithReason() throws Exception {
        when(platformAccountService.updateStatus(
            eq("Bearer admin-token"),
            eq(10L),
            any(PlatformAccountStatusRequest.class)
        )).thenReturn(new PlatformAccountResponse(
            10L, "owner", "企业管理员", null, null, "SUPPLIER", "REGISTERED_ADMIN", true,
            List.of("COMPANY_ADMIN_2"), "DISABLED", "2026-08-04T10:00:00", null
        ));

        mockMvc.perform(patch("/api/platform/accounts/10/status")
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"DISABLED","reason":"企业申请暂停账号"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISABLED"));

        verify(platformAccountService).updateStatus(
            eq("Bearer admin-token"),
            eq(10L),
            any(PlatformAccountStatusRequest.class)
        );
    }
}
