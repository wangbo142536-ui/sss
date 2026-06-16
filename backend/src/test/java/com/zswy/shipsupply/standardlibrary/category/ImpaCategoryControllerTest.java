package com.zswy.shipsupply.standardlibrary.category;

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

@WebMvcTest(ImpaCategoryController.class)
class ImpaCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImpaCategoryService impaCategoryService;

    @Test
    void listsImpaCategoryTreeForMaterialEntry() throws Exception {
        var child = new ImpaCategoryResponse(
            "1101",
            "1101 码段",
            "1101 Code Segment",
            "11",
            2,
            1101,
            33,
            List.of()
        );
        var category = new ImpaCategoryResponse(
            "11",
            "船员后勤、娱乐用品",
            "Welfare Items",
            null,
            1,
            11,
            236,
            List.of(child)
        );
        when(impaCategoryService.listCategoryTree()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/standard-library/impa/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("11"))
            .andExpect(jsonPath("$[0].nameCn").value("船员后勤、娱乐用品"))
            .andExpect(jsonPath("$[0].nameEn").value("Welfare Items"))
            .andExpect(jsonPath("$[0].parentCode").value(nullValue()))
            .andExpect(jsonPath("$[0].level").value(1))
            .andExpect(jsonPath("$[0].sortOrder").value(11))
            .andExpect(jsonPath("$[0].itemCount").value(236))
            .andExpect(jsonPath("$[0].children").isArray())
            .andExpect(jsonPath("$[0].children.length()").value(1))
            .andExpect(jsonPath("$[0].children[0].code").value("1101"))
            .andExpect(jsonPath("$[0].children[0].nameCn").value("1101 码段"))
            .andExpect(jsonPath("$[0].children[0].parentCode").value("11"))
            .andExpect(jsonPath("$[0].children[0].level").value(2))
            .andExpect(jsonPath("$[0].children[0].itemCount").value(33));
    }
}
