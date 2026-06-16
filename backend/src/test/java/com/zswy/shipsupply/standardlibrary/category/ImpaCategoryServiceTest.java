package com.zswy.shipsupply.standardlibrary.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImpaCategoryServiceTest {

    @Mock
    private ImpaCategoryRepository impaCategoryRepository;

    @Test
    void attachesSyntheticLevel2CodeSegmentsUnderParentCategories() {
        when(impaCategoryRepository.findEnabledCategories()).thenReturn(List.of(
            new ImpaCategoryRow("11", "船员后勤、娱乐用品", "Welfare Items", null, 1, 11, 236),
            new ImpaCategoryRow("1101", "1101 码段", "1101 Code Segment", "11", 2, 1101, 33)
        ));
        var service = new ImpaCategoryService(impaCategoryRepository, 2);

        List<ImpaCategoryResponse> categories = service.listCategoryTree();

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).code()).isEqualTo("11");
        assertThat(categories.get(0).children()).hasSize(1);
        assertThat(categories.get(0).children().get(0).code()).isEqualTo("1101");
        assertThat(categories.get(0).children().get(0).nameCn()).isEqualTo("1101 码段");
        assertThat(categories.get(0).children().get(0).itemCount()).isEqualTo(33);
    }
}
