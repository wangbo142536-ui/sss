package com.zswy.shipsupply.procurement.materials;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(MaterialDemandController.class)
class MaterialDemandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MaterialDemandService materialDemandService;

    @MockBean
    private MaterialDemandComparisonService materialDemandComparisonService;

    @Test
    void savesPreviewAsMaterialDemand() throws Exception {
        MaterialDemandSaveRequest request = saveRequest(null, null);
        when(materialDemandService.save(eq("Bearer company-token"), eq(request), eq(null)))
            .thenReturn(new MaterialDemandSaveResponse(
                101L,
                "REQ-20260605-001",
                "SAVED",
                "/procurement/materials",
                "/procurement/materials?demandId=101"
            ));

        mockMvc.perform(post("/api/procurement/material-demands")
                .header("Authorization", "Bearer company-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.demandId").value(101))
            .andExpect(jsonPath("$.demandNo").value("REQ-20260605-001"))
            .andExpect(jsonPath("$.status").value("SAVED"))
            .andExpect(jsonPath("$.redirectTo").value("/procurement/materials?demandId=101"));
    }

    @Test
    void updatesDemandByPathId() throws Exception {
        MaterialDemandSaveRequest request = saveRequest(null, null);
        when(materialDemandService.save(eq("Bearer company-token"), eq(request), eq(101L)))
            .thenReturn(new MaterialDemandSaveResponse(
                101L,
                "REQ-20260605-001",
                "SAVED",
                "/procurement/materials",
                "/procurement/materials?demandId=101"
            ));

        mockMvc.perform(put("/api/procurement/material-demands/101")
                .header("Authorization", "Bearer company-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.demandId").value(101))
            .andExpect(jsonPath("$.demandNo").value("REQ-20260605-001"));
    }

    @Test
    void listsMaterialDemandsWithFilters() throws Exception {
        MaterialDemandSummaryResponse summary = summary(101L);
        when(materialDemandService.list(
            "Bearer company-token",
            "STCL26",
            "SAVED",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "COMPARISON",
            1,
            20
        )).thenReturn(new MaterialDemandListResponse(List.of(summary), 1, 20, 1L));

        mockMvc.perform(get("/api/procurement/material-demands")
                .header("Authorization", "Bearer company-token")
                .param("keyword", "STCL26")
                .param("status", "SAVED")
                .param("inquiryDateFrom", "2026-06-01")
                .param("inquiryDateTo", "2026-06-30")
                .param("stage", "COMPARISON")
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].demandNo").value("REQ-20260605-001"))
            .andExpect(jsonPath("$.items[0].applicationNo").value("APP-001"))
            .andExpect(jsonPath("$.items[0].vesselName").value("MV BLUE"))
            .andExpect(jsonPath("$.items[0].sourceFileName").value("STCL26SD003-R01.xlsx"))
            .andExpect(jsonPath("$.items[0].skuCount").value(1))
            .andExpect(jsonPath("$.items[0].itemCount").value(1))
            .andExpect(jsonPath("$.items[0].exactMatchCount").value(1))
            .andExpect(jsonPath("$.items[0].similarMatchCount").value(0))
            .andExpect(jsonPath("$.items[0].unmatchedMatchCount").value(0))
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void returnsDemandDetailWithItems() throws Exception {
        MaterialDemandSummaryResponse summary = summary(101L);
        MaterialDemandItemResponse item = itemResponse(201L);
        when(materialDemandService.detail("Bearer company-token", 101L))
            .thenReturn(new MaterialDemandDetailResponse(summary, List.of(item)));

        mockMvc.perform(get("/api/procurement/material-demands/101")
                .header("Authorization", "Bearer company-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.demand.demandId").value(101))
            .andExpect(jsonPath("$.items[0].sequence").value(1))
            .andExpect(jsonPath("$.items[0].rowNo").value(28))
            .andExpect(jsonPath("$.items[0].platformCode").value("110101"))
            .andExpect(jsonPath("$.items[0].productName").value("COTTON RAG"))
            .andExpect(jsonPath("$.items[0].specification").value("WHITE"))
            .andExpect(jsonPath("$.items[0].rawColumns.IMPA").value("110101"))
            .andExpect(jsonPath("$.items[0].selectedImpaCode").value("110101"))
            .andExpect(jsonPath("$.items[0].candidateNameEn").value("COTTON RAG"))
            .andExpect(jsonPath("$.items[0].candidateSnapshot[0].impaCode").value("110101"));
    }

    @Test
    void returnsDemandComparison() throws Exception {
        MaterialDemandSummaryResponse summary = summary(101L);
        MaterialDemandComparisonResponse response = new MaterialDemandComparisonResponse(
            summary,
            new MaterialDemandSupplyInfo("MV BLUE", "舟山港", "ZHOUSHAN", "舟山港", "2026-06-05T09:30", "2026-06-05", "天气待接入", "STATIC_PLACEHOLDER"),
            List.of(new MaterialDemandComparisonStrategy(
                "LOWEST_MIXED",
                "最低混供",
                0,
                1,
                1,
                0,
                java.math.BigDecimal.ZERO,
                "CNY",
                List.of(),
                false,
                "ONLY_ONE_SUPPLIER"
            )),
            List.of(new MaterialDemandComparisonItem(
                201L,
                28,
                "110101",
                "110101",
                "COTTON RAG",
                "COTTON RAG",
                "WHITE",
                "2",
                new java.math.BigDecimal("2"),
                null,
                "PCS",
                "110101",
                "COTTON RAG",
                null,
                null,
                List.of(),
                "NO_SUPPLIER_CANDIDATE"
            )),
            false,
            false,
            null
        );
        when(materialDemandComparisonService.comparison("Bearer company-token", 101L)).thenReturn(response);

        mockMvc.perform(get("/api/procurement/material-demands/101/comparison")
                .header("Authorization", "Bearer company-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.demand.demandId").value(101))
            .andExpect(jsonPath("$.supplyInfo.supplyVessel").value("MV BLUE"))
            .andExpect(jsonPath("$.supplyInfo.weatherSource").value("STATIC_PLACEHOLDER"))
            .andExpect(jsonPath("$.strategies[0].strategyType").value("LOWEST_MIXED"))
            .andExpect(jsonPath("$.items[0].demandItemId").value(201))
            .andExpect(jsonPath("$.items[0].emptyReason").value("NO_SUPPLIER_CANDIDATE"));
    }

    private MaterialDemandSaveRequest saveRequest(Long demandId, String demandNo) {
        return new MaterialDemandSaveRequest(
            demandId,
            demandNo,
            "APP-001",
            "MV BLUE",
            "ZHOUSHAN",
            "舟山港",
            "2026-06-05T09:30",
            "2026-06-05",
            "STCL26SD003-R01.xlsx",
            "DEMAND_INQUIRY",
            27,
            List.of(new MaterialDemandItemRequest(
                "DEMAND_INQUIRY",
                27,
                1,
                28,
                28,
                Map.of("IMPA", "110101", "DESCRIPTION", "COTTON RAG"),
                "110101",
                "COTTON RAG",
                "WHITE",
                "2",
                "PCS",
                "urgent",
                null,
                null,
                null,
                null,
                null,
                "110101",
                "110101",
                "棉布",
                "COTTON RAG",
                "WHITE",
                "EXACT",
                "Exact Match",
                "CODE_MATCH",
                false,
                null,
                null,
                List.of(new MaterialMatchCandidate(
                    "110101",
                    "11",
                    "Clothing",
                    "妫夊竷",
                    "COTTON RAG",
                    "WHITE",
                    "PCS",
                    "CODE_MATCH",
                    "CODE_MATCH"
                )),
                List.of(new MaterialMatchCandidate(
                    "110101",
                    "11",
                    "Clothing",
                    "棉布",
                    "COTTON RAG",
                    "WHITE",
                    "PCS",
                    "CODE_MATCH",
                    "CODE_MATCH"
                ))
            ))
        );
    }

    private MaterialDemandSummaryResponse summary(Long demandId) {
        return new MaterialDemandSummaryResponse(
            demandId,
            "REQ-20260605-001",
            "APP-001",
            "MV BLUE",
            "ZHOUSHAN",
            "舟山港",
            "2026-06-05T09:30",
            "2026-06-05",
            "STCL26SD003-R01.xlsx",
            "DEMAND_INQUIRY",
            27,
            1,
            1,
            0,
            0,
            "SAVED",
            "2026-06-05T10:00:00",
            "2026-06-05T10:05:00"
        );
    }

    private MaterialDemandItemResponse itemResponse(Long itemId) {
        MaterialDemandItemRequest request = saveRequest(null, null).items().get(0);
        return new MaterialDemandItemResponse(
            itemId,
            request.documentType(),
            request.headerRowIndex(),
            request.sequence(),
            request.sourceRowNo(),
            request.sourceRowNumber(),
            request.rawColumns(),
            request.impaCode(),
            request.description(),
            request.sizeModel(),
            request.quantity(),
            request.unit(),
            request.remarks(),
            request.supplierItemNo(),
            request.rawNameSpec(),
            request.price(),
            request.packing(),
            request.stock(),
            request.selectedImpaCode(),
            request.candidateImpaCode(),
            request.candidateNameCn(),
            request.candidateNameEn(),
            request.candidateSpec(),
            request.matchResult(),
            request.matchResultName(),
            request.reason(),
            request.hasImage(),
            request.imageIndex(),
            request.imageAnchor(),
            request.candidateSnapshot(),
            request.candidates()
        );
    }
}
