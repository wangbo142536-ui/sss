package com.zswy.shipsupply.standardlibrary.item;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImpaItemController.class)
class ImpaItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImpaItemService impaItemService;

    @Test
    void searchesItemsByKeywordAndCategory() throws Exception {
        when(impaItemService.listItems("11", null, "独轮车", 50)).thenReturn(List.of(
            new ImpaItemResponse("110105", "11", "甲板物料", "1101", "独轮车", "WHEEL BARROW", null, "套")
        ));

        mockMvc.perform(get("/api/standard-library/impa/items")
                .param("categoryCode", "11")
                .param("keyword", "独轮车"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].impaCode").value("110105"))
            .andExpect(jsonPath("$[0].categoryCode").value("11"))
            .andExpect(jsonPath("$[0].segmentCode").value("1101"))
            .andExpect(jsonPath("$[0].nameCn").value("独轮车"))
            .andExpect(jsonPath("$[0].nameEn").value("WHEEL BARROW"))
            .andExpect(jsonPath("$[0].specification").value(nullValue()))
            .andExpect(jsonPath("$[0].unit").value("套"));
    }

    @Test
    void acceptsQAsKeywordAlias() throws Exception {
        when(impaItemService.listItems(null, null, "613435", 10)).thenReturn(List.of(
            new ImpaItemResponse("613435", "61", "电气物料", "6134", "防水插头", "WATERTIGHT PLUG", "10A", "只")
        ));

        mockMvc.perform(get("/api/standard-library/impa/items")
                .param("q", "613435")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].impaCode").value("613435"))
            .andExpect(jsonPath("$[0].nameCn").value("防水插头"))
            .andExpect(jsonPath("$[0].nameEn").value("WATERTIGHT PLUG"))
            .andExpect(jsonPath("$[0].specification").value("10A"));
    }

    @Test
    void returnsPagedItemsWhenPageIsRequested() throws Exception {
        when(impaItemService.listItemPage("11", null, "独轮车", 2, 50)).thenReturn(
            new ImpaItemPageResponse(
                List.of(new ImpaItemResponse("110105", "11", "甲板物料", "1101", "独轮车", "WHEEL BARROW", null, "套")),
                51,
                2,
                50,
                2
            )
        );

        mockMvc.perform(get("/api/standard-library/impa/items")
                .param("categoryCode", "11")
                .param("keyword", "独轮车")
                .param("page", "2")
                .param("pageSize", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].impaCode").value("110105"))
            .andExpect(jsonPath("$.total").value(51))
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.pageSize").value(50))
            .andExpect(jsonPath("$.totalPages").value(2));
    }
}
