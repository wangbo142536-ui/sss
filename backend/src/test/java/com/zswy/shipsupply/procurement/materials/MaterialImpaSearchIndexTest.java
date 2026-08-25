package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zswy.shipsupply.standardlibrary.item.ImpaItemRepository;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemResponse;

@ExtendWith(MockitoExtension.class)
class MaterialImpaSearchIndexTest {

    @Mock
    private ImpaItemRepository repository;

    @Test
    void reusesOneProcessSnapshotUntilExplicitlyInvalidated() {
        when(repository.findItems(null, null, null, 60_000)).thenReturn(List.of(item("611705")));
        MaterialImpaSearchIndex index = new MaterialImpaSearchIndex(repository, 300);

        assertThat(index.snapshot().byCode()).containsKey("611705");
        assertThat(index.snapshot().items()).hasSize(1);
        verify(repository, times(1)).findItems(null, null, null, 60_000);

        index.invalidate();
        assertThat(index.snapshot().items()).hasSize(1);
        verify(repository, times(2)).findItems(null, null, null, 60_000);
    }

    private ImpaItemResponse item(String code) {
        return new ImpaItemResponse(code, null, "61", "一般工具", "6117", "尖嘴钳", "FLAT NOSE PLIER", "160MM", "PCS");
    }
}
