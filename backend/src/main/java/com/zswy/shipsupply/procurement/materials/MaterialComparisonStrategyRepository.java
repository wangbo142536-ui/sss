package com.zswy.shipsupply.procurement.materials;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class MaterialComparisonStrategyRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MaterialComparisonStrategyRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<MaterialComparisonStrategySettings> find(Long companyId, Long demandId) {
        return jdbcTemplate.query(
            """
            SELECT mixed_supplier_count, price_enabled, price_level, quality_enabled, quality_level,
                   core_item_ids_json, strategy_version
            FROM material_comparison_strategy_setting
            WHERE company_id = ? AND demand_id = ?
            """,
            (rs, rowNum) -> new MaterialComparisonStrategySettings(
                rs.getInt("mixed_supplier_count"),
                rs.getBoolean("price_enabled"),
                rs.getInt("price_level"),
                rs.getBoolean("quality_enabled"),
                rs.getInt("quality_level"),
                readIds(rs.getString("core_item_ids_json")),
                rs.getInt("strategy_version")
            ),
            companyId,
            demandId
        ).stream().findFirst();
    }

    public MaterialComparisonStrategySettings save(
        Long companyId,
        Long demandId,
        Long userId,
        MaterialComparisonStrategySettings settings
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO material_comparison_strategy_setting
              (company_id, demand_id, mixed_supplier_count, price_enabled, price_level,
               quality_enabled, quality_level, core_item_ids_json, strategy_version, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON), 1, ?)
            ON DUPLICATE KEY UPDATE
              mixed_supplier_count = VALUES(mixed_supplier_count),
              price_enabled = VALUES(price_enabled),
              price_level = VALUES(price_level),
              quality_enabled = VALUES(quality_enabled),
              quality_level = VALUES(quality_level),
              core_item_ids_json = VALUES(core_item_ids_json),
              strategy_version = strategy_version + 1,
              updated_by = VALUES(updated_by),
              updated_at = CURRENT_TIMESTAMP
            """,
            companyId,
            demandId,
            settings.mixedSupplierCount(),
            settings.priceEnabled(),
            settings.priceLevel(),
            settings.qualityEnabled(),
            settings.qualityLevel(),
            writeIds(settings.coreDemandItemIds()),
            userId
        );
        return find(companyId, demandId).orElse(settings);
    }

    private List<Long> readIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeIds(List<Long> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception error) {
            throw new IllegalStateException("MATERIAL_COMPARISON_STRATEGY_SERIALIZE_FAILED", error);
        }
    }
}
