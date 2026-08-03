package com.zswy.shipsupply.procurement.food.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.EvaluationSubmitRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.OrderCreateRequest;

@WebMvcTest(BuyerFoodProcurementController.class)
class BuyerFoodProcurementControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FoodProcurementApplicationService service;

    @Test
    void exposesEveryBuyerFoodProcurementAction() throws Exception {
        when(service.listDemands(anyString(), any(), any())).thenReturn(List.of());
        when(service.listSuppliers(anyString())).thenReturn(List.of());
        when(service.listBuyerInquiries(anyString(), any(), any())).thenReturn(List.of());
        when(service.listBuyerOrders(anyString(), any(), any())).thenReturn(List.of());
        when(service.listBuyerSettlements(anyString(), any())).thenReturn(List.of());
        when(service.listEvaluations(anyString(), any())).thenReturn(List.of());
        MockMultipartFile file = new MockMultipartFile("file", "food.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[] {1});

        mockMvc.perform(multipart("/api/procurement/food/demands/match-preview").file(file).header("Authorization", "token"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/procurement/food/demands").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk());
        mockMvc.perform(put("/api/procurement/food/demands/1").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/demands").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/demands/1").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/suppliers").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(post("/api/procurement/food/demands/1/send-inquiry").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{\"supplierCompanyIds\":[2]}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/inquiries").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/quotes/1").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/demands/1/comparison").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(post("/api/procurement/food/demands/1/purchase-orders").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{\"strategyType\":\"LOWEST_ITEM\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/purchase-orders").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/purchase-orders/1").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/settlements").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(post("/api/procurement/food/settlements/1/action").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{\"targetStatus\":\"SETTLED\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/procurement/food/evaluations").header("Authorization", "token")).andExpect(status().isOk());
        mockMvc.perform(post("/api/procurement/food/evaluations/1/submit").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{\"qualityRating\":5,\"logisticsRating\":5}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/procurement/food/evaluations/1/review").header("Authorization", "token").contentType(MediaType.APPLICATION_JSON).content("{\"targetStatus\":\"APPROVED\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void passesIndependentDeliveryAddressIntoOrderConfirmation() throws Exception {
        mockMvc.perform(post("/api/procurement/food/demands/1/purchase-orders")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "strategyType":"LOWEST_ITEM",
                      "requiredDeliveryTime":"2026-07-27T09:00:00",
                      "deliveryAddress":"Xi Gang Terminal, Berth 3",
                      "deliveryContactName":"Buyer",
                      "deliveryContactPhone":"13800000000",
                      "defaultPackagingMethod":"UNIFIED_PACKAGING"
                    }
                    """))
            .andExpect(status().isOk());

        ArgumentCaptor<OrderCreateRequest> request = ArgumentCaptor.forClass(OrderCreateRequest.class);
        verify(service).createOrder(anyString(), anyLong(), request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().deliveryAddress())
            .isEqualTo("Xi Gang Terminal, Berth 3");
    }

    @Test
    void passesEvaluationAttachmentsIntoSubmission() throws Exception {
        mockMvc.perform(post("/api/procurement/food/evaluations/61/submit")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "qualityRating":5,
                      "logisticsRating":4,
                      "comment":"Good service",
                      "attachments":[{
                        "fileId":"file-1",
                        "fileName":"evidence.jpg",
                        "fileUrl":"/api/files/file-1"
                      }]
                    }
                    """))
            .andExpect(status().isOk());

        ArgumentCaptor<EvaluationSubmitRequest> request = ArgumentCaptor.forClass(EvaluationSubmitRequest.class);
        verify(service).submitEvaluation(anyString(), anyLong(), request.capture());
        org.assertj.core.api.Assertions.assertThat(request.getValue().attachments()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(request.getValue().attachments().get(0).fileId())
            .isEqualTo("file-1");
    }
}
