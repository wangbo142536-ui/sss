package com.zswy.shipsupply.standardlibrary.category;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ImpaCategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public ImpaCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ImpaCategoryRow> findEnabledCategories() {
        return jdbcTemplate.query(
            """
            SELECT category_code, category_name_cn, category_name_en, parent_code,
                   level, sort_order, item_count
            FROM (
                SELECT category_code, category_name_cn, category_name_en, parent_code,
                       level, sort_order, item_count
                FROM impa_category
                WHERE enabled = 1 AND level = 1

                UNION ALL

                SELECT code_segment AS category_code,
                       CONCAT(code_segment, ' 码段') AS category_name_cn,
                       CONCAT(code_segment, ' Code Segment') AS category_name_en,
                       category_code AS parent_code,
                       2 AS level,
                       CAST(code_segment AS UNSIGNED) AS sort_order,
                       COUNT(DISTINCT impa_code) AS item_count
                FROM (
                    SELECT i18n.category_code, i18n.impa_code, LEFT(i18n.impa_code, 4) AS code_segment
                    FROM impa_item_i18n i18n
                    JOIN impa_item item ON item.impa_code = i18n.impa_code
                    WHERE i18n.language = 'zh-CN'
                      AND item.enabled = 1
                ) item_name_segments
                GROUP BY category_code, code_segment
            ) category_tree
            ORDER BY level ASC, sort_order ASC, category_code ASC
            """,
            this::mapRow
        );
    }

    private ImpaCategoryRow mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new ImpaCategoryRow(
            resultSet.getString("category_code"),
            resultSet.getString("category_name_cn"),
            resultSet.getString("category_name_en"),
            resultSet.getString("parent_code"),
            resultSet.getInt("level"),
            resultSet.getInt("sort_order"),
            resultSet.getInt("item_count")
        );
    }
}
