package com.zswy.shipsupply.procurement.food.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.AttachmentPayload;

@ExtendWith(MockitoExtension.class)
class FoodOrderRepositoryTest {

    @Mock private JdbcTemplate jdbcTemplate;

    private FoodOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FoodOrderRepository(jdbcTemplate, new ObjectMapper());
    }

    @Test
    void historicalOrInvalidEvaluationAttachmentDataReturnsEmptyList() {
        List<AttachmentPayload> missing = ReflectionTestUtils.invokeMethod(
            repository, "evaluationAttachments", (Object) null
        );
        List<AttachmentPayload> invalid = ReflectionTestUtils.invokeMethod(
            repository, "evaluationAttachments", "not-json"
        );

        assertThat(missing).isEmpty();
        assertThat(invalid).isEmpty();
    }

    @Test
    void submitEvaluationPersistsAttachmentJson() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        repository.submitEvaluation(
            61L, 11L, 5, 4, "Good service",
            List.of(new AttachmentPayload("file-1", "evidence.jpg", "/api/files/file-1"))
        );

        verify(jdbcTemplate).update(
            argThat(sql -> sql.contains("attachments_json = ?")),
            eq(5), eq(4), eq("Good service"),
            argThat(json -> json instanceof String value
                && value.contains("\"fileId\":\"file-1\"")
                && value.contains("\"fileName\":\"evidence.jpg\"")),
            eq(61L), eq(11L)
        );
    }
}
