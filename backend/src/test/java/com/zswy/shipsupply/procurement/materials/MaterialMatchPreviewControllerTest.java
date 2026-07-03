package com.zswy.shipsupply.procurement.materials;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.zswy.shipsupply.common.material.MaterialNameAttribute;

@WebMvcTest(MaterialMatchPreviewController.class)
class MaterialMatchPreviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialMatchPreviewService materialMatchPreviewService;

    @Test
    void previewsUploadedMaterialQuoteFile() throws Exception {
        MaterialMatchPreviewResponse response = new MaterialMatchPreviewResponse(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            1,
            0,
            1,
            0,
            List.of(new MaterialMatchPreviewItem(
                "SUPPLIER_QUOTATION",
                "SUPPLIER_QUOTATION",
                1,
                1,
                2,
                2,
                Map.of("Item No.", "110203", "Name of Commodity & Specification", "BENT NOSE PLIERS"),
                "BENT NOSE PLIERS",
                "BENT NOSE PLIERS",
                List.of(new MaterialNameAttribute("size", "Size", "150MM", null, "150MM")),
                List.of(),
                "",
                "",
                "",
                "",
                "",
                "",
                "110203",
                "BENT NOSE PLIERS",
                "12.50",
                "BOX",
                "100",
                "110203",
                "棉衣",
                "COTTON COAT",
                "",
                "SIMILAR",
                "Similar Match",
                "NAME_SPEC_MATCH",
                "ABNORMAL",
                "CODE_MISSING",
                true,
                1,
                "sheet1!R2C1",
                List.of(new MaterialMatchCandidate(
                    "613041",
                    "61",
                    "Tools",
                    "Pliers",
                    "LONG NOSE PLIERS",
                    "",
                    "PCS",
                    "SPEC_MATCH",
                    "NAME_SPEC_MATCH"
                )),
                List.of(new MaterialSupplierCandidate(
                    10L,
                    20L,
                    "Supplier A",
                    "110203",
                    "BENT NOSE PLIERS",
                    "613041",
                    null,
                    "61",
                    "Tools",
                    List.of(),
                    "150MM",
                    new java.math.BigDecimal("12.50"),
                    "CNY",
                    "\u00A5",
                    new java.math.BigDecimal("100"),
                    "PCS",
                    "BOX",
                    "/api/files/IMG-1",
                    "/api/files/IMG-1",
                    "ON_SHELF",
                    "SPEC_MATCHED",
                    "SUPPLIER_SKU_MATCH",
                    "SUPPLIER_SKU_MATCH"
                ))
            ))
        );
        when(materialMatchPreviewService.matchPreview(eq("Bearer dev-token"), any(MockMultipartFile.class)))
            .thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "quote.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/api/procurement/materials/match-preview")
                .file(file)
                .header("Authorization", "Bearer dev-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(1))
            .andExpect(jsonPath("$.documentType").value("SUPPLIER_QUOTATION"))
            .andExpect(jsonPath("$.headerRowIndex").value(1))
            .andExpect(jsonPath("$.similarCount").value(1))
            .andExpect(jsonPath("$.items[0].sequence").value(1))
            .andExpect(jsonPath("$.items[0].sourceRowNo").value(2))
            .andExpect(jsonPath("$.items[0].supplierItemNo").value("110203"))
            .andExpect(jsonPath("$.items[0].rawNameSpec").value("BENT NOSE PLIERS"))
            .andExpect(jsonPath("$.items[0].matchResult").value("SIMILAR"))
            .andExpect(jsonPath("$.items[0].reason").value("NAME_SPEC_MATCH"))
            .andExpect(jsonPath("$.items[0].hasImage").value(true))
            .andExpect(jsonPath("$.items[0].candidates[0].impaCode").value("613041"))
            .andExpect(jsonPath("$.items[0].candidates[0].categoryCode").value("61"))
            .andExpect(jsonPath("$.items[0].candidates[0].categoryName").value("Tools"))
            .andExpect(jsonPath("$.items[0].candidates[0].candidateMatchType").value("SPEC_MATCH"))
            .andExpect(jsonPath("$.items[0].supplierCandidates[0].supplierName").value("Supplier A"))
            .andExpect(jsonPath("$.items[0].supplierCandidates[0].unitPrice").value(12.50))
            .andExpect(jsonPath("$.items[0].candidates[0].matchScore").doesNotExist());
    }
}
