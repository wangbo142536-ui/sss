package com.zswy.shipsupply.procurement.food.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.AttachmentPayload;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSummary;
import com.zswy.shipsupply.procurement.food.application.FoodProcurementApplicationService;

@WebMvcTest(FoodRegulatoryEvaluationController.class)
class FoodRegulatoryEvaluationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FoodProcurementApplicationService service;

    @Test
    void regulatoryListReturnsEvaluationAttachments() throws Exception {
        EvaluationSummary summary = new EvaluationSummary(
            61L, 91L, "FPO-91", "Supplier A", "PENDING_REVIEW", 5, 4, "Good service",
            List.of(new AttachmentPayload("file-1", "evidence.jpg", "/api/files/file-1")),
            null, LocalDateTime.of(2026, 7, 21, 10, 0)
        );
        when(service.listRegulatoryEvaluations(anyString(), any(), any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/regulatory/food/evaluations").header("Authorization", "token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].attachments[0].fileId").value("file-1"))
            .andExpect(jsonPath("$[0].attachments[0].fileName").value("evidence.jpg"))
            .andExpect(jsonPath("$[0].attachments[0].fileUrl").value("/api/files/file-1"));
    }
}
