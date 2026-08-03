package com.zswy.shipsupply.procurement.food.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.zswy.shipsupply.procurement.food.application.FoodProcurementApplicationService;
import com.zswy.shipsupply.procurement.food.application.FoodQuoteExportService;
import com.zswy.shipsupply.procurement.food.application.FoodQuoteExportService.ExportedQuote;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.AttachmentPayload;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementActionRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SettlementSummary;

@WebMvcTest(SupplierFoodProcurementController.class)
class SupplierFoodProcurementControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FoodProcurementApplicationService service;
    @MockBean private FoodQuoteExportService exportService;

    @Test
    void exposesEverySupplierQuoteOrderAndSettlementAction() throws Exception {
        when(service.listSupplierInquiries(anyString(), any(), any())).thenReturn(List.of());
        when(service.listSupplierOrders(anyString(), any(), any())).thenReturn(List.of());
        when(service.listSupplierSettlements(anyString(), any())).thenReturn(List.of());
        when(exportService.export(anyString(), any(Long.class))).thenReturn(new ExportedQuote("food-quote.xlsx", new byte[] {1, 2, 3}));
        MockMultipartFile file = new MockMultipartFile("file", "quote.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[] {1});

        mockMvc.perform(get("/api/supplier/food/inquiries").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/supplier/food/quotes/1").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(put("/api/supplier/food/quotes/1/draft").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/supplier/food/quotes/1/virtual-fill").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{\"overwriteExisting\":false}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/supplier/food/quotes/1/submit").header("Authorization", "token"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/supplier/food/quotes/1/export").header("Authorization", "token"))
            .andExpect(status().isOk()).andExpect(content().bytes(new byte[] {1, 2, 3}));
        mockMvc.perform(multipart("/api/supplier/food/quotes/1/import-preview").file(file).header("Authorization", "token"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/supplier/food/quotes/1/imports/2/commit").header("Authorization", "token"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/supplier/food/purchase-orders").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/supplier/food/purchase-orders/1").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(post("/api/supplier/food/purchase-orders/1/supplier-orders/2/action").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{\"targetStatus\":\"CONFIRMED\",\"expectedReadyAt\":\"2026-07-17T09:00:00\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/supplier/food/settlements").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(post("/api/supplier/food/settlements/1/action").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{\"targetStatus\":\"INVOICED\",\"invoiceNo\":\"INV-1\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void settlementContractIncludesContextActualAmountAndInvoiceAttachments() throws Exception {
        SettlementSummary summary = new SettlementSummary(
            51L, 9L, "FPO-9", 91L, "Supplier A", "Buyer A", "MV AMBER", "USD", "INVOICED",
            new BigDecimal("130.00"), new BigDecimal("128.50"), "INV-1", "file-1",
            List.of(new AttachmentPayload("file-1", "invoice.pdf", "/api/files/file-1")),
            LocalDateTime.of(2026, 7, 21, 11, 0)
        );
        when(service.listSupplierSettlements(anyString(), any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/supplier/food/settlements").header("Authorization", "token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].buyerCompanyName").value("Buyer A"))
            .andExpect(jsonPath("$[0].vesselName").value("MV AMBER"))
            .andExpect(jsonPath("$[0].currency").value("USD"))
            .andExpect(jsonPath("$[0].actualAmount").value(128.50))
            .andExpect(jsonPath("$[0].invoiceAttachments[0].fileId").value("file-1"))
            .andExpect(jsonPath("$[0].invoiceAttachments[0].fileName").value("invoice.pdf"))
            .andExpect(jsonPath("$[0].invoiceAttachments[0].fileUrl").value("/api/files/file-1"));

        mockMvc.perform(post("/api/supplier/food/settlements/51/action")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "targetStatus":"INVOICED",
                      "invoiceNo":"INV-1",
                      "actualAmount":128.50,
                      "invoiceAttachments":[{
                        "fileId":"file-1",
                        "fileName":"invoice.pdf",
                        "fileUrl":"/api/files/file-1"
                      }]
                    }
                    """))
            .andExpect(status().isOk());

        ArgumentCaptor<SettlementActionRequest> request = ArgumentCaptor.forClass(SettlementActionRequest.class);
        verify(service).updateSupplierSettlement(anyString(), any(Long.class), request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().actualAmount()).isEqualByComparingTo("128.50");
        org.assertj.core.api.Assertions.assertThat(request.getValue().invoiceAttachments()).singleElement()
            .satisfies(file -> org.assertj.core.api.Assertions.assertThat(file.fileName()).isEqualTo("invoice.pdf"));
    }
}
