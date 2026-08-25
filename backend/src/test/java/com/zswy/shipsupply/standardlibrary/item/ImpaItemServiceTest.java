package com.zswy.shipsupply.standardlibrary.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImpaItemServiceTest {

    @Mock
    private ImpaItemRepository impaItemRepository;

    @Test
    void trimsKeywordAndCapsLimit() {
        var expected = List.of(
            new ImpaItemResponse("613435", "61", "电气物料", "6134", "防水插头", "WATERTIGHT PLUG", "10A", "只")
        );
        when(impaItemRepository.findItems("61", null, "613435", 200)).thenReturn(expected);
        var service = new ImpaItemService(impaItemRepository);

        List<ImpaItemResponse> items = service.listItems("61", null, " 613435 ", 500);

        assertThat(items).isSameAs(expected);
    }

    @Test
    void returnsRequestedPageWithTotalMetadata() {
        var expected = List.of(
            new ImpaItemResponse("615001", "61", "工具", "6150", "第二页物料", "PAGE TWO ITEM", "50MM", "件")
        );
        when(impaItemRepository.countItems("61", null, null)).thenReturn(125L);
        when(impaItemRepository.findItems("61", null, null, 50, 50)).thenReturn(expected);
        var service = new ImpaItemService(impaItemRepository);

        ImpaItemPageResponse result = service.listItemPage("61", null, null, 2, 50);

        assertThat(result.items()).isSameAs(expected);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.pageSize()).isEqualTo(50);
        assertThat(result.total()).isEqualTo(125);
        assertThat(result.totalPages()).isEqualTo(3);
        verify(impaItemRepository).findItems("61", null, null, 50, 50);
    }
}
