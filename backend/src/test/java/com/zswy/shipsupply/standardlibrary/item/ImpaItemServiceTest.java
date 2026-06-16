package com.zswy.shipsupply.standardlibrary.item;

import static org.assertj.core.api.Assertions.assertThat;
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
}
