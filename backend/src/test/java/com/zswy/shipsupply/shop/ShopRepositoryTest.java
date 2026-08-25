package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShopRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listSkusOrdersSavedProductsByImportRowNumberAscending() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        ShopRepository repository = new ShopRepository(jdbcTemplate, new ObjectMapper());
        repository.listSkus(22L, null, null, null, null, null, 1, 20);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        assertThat(sqlCaptor.getValue()).contains(
            "CASE WHEN import_row_no IS NULL THEN 1 ELSE 0 END ASC",
            "import_row_no ASC",
            "import_batch_id DESC",
            "id ASC"
        );
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void supplierDetailFilterKeepsSqlBoundaryBeforeOrderBy() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        ShopRepository repository = new ShopRepository(jdbcTemplate, new ObjectMapper());
        repository.listSuppliers(24L, null, null, null, null, 1, 1);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        assertThat(sqlCaptor.getValue()).contains("AND c.id = ?\nORDER BY");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void supplierDirectoryOrdersCompaniesByCreationTimeDescending() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        ShopRepository repository = new ShopRepository(jdbcTemplate, new ObjectMapper());
        repository.listSuppliers(null, null, null, null, null, 1, 50);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        assertThat(sqlCaptor.getValue()).contains("ORDER BY\n  c.created_at DESC,\n  c.id DESC");
    }
}
