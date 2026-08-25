package com.zswy.shipsupply.standardlibrary.provision;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProvisionCategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProvisionCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProvisionCategoryResponse> findEnabledCategories(String keyword) {
        return jdbcTemplate.query(
            """
            SELECT category_code, category_name_cn, category_name_en,
                   parent_code,
                   CASE
                     WHEN parent_code IS NULL OR TRIM(parent_code) = '' THEN 1
                     ELSE 2
                   END AS category_level,
                   sort_order
            FROM provision_category
            WHERE enabled = 1
              AND (? IS NULL
                   OR category_code LIKE CONCAT('%', ?, '%')
                   OR category_name_cn LIKE CONCAT('%', ?, '%')
                   OR category_name_en LIKE CONCAT('%', ?, '%'))
            ORDER BY category_level ASC, sort_order ASC, category_code ASC
            """,
            (resultSet, rowNum) -> new ProvisionCategoryResponse(
                resultSet.getString("category_code"),
                resultSet.getString("category_name_cn"),
                resultSet.getString("category_name_en"),
                resultSet.getString("parent_code"),
                resultSet.getInt("category_level"),
                resultSet.getInt("sort_order")
            ),
            keyword,
            keyword,
            keyword,
            keyword
        );
    }
}
