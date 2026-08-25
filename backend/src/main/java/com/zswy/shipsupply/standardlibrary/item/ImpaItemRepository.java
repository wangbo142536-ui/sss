package com.zswy.shipsupply.standardlibrary.item;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ImpaItemRepository {

    private final JdbcTemplate jdbcTemplate;

    public ImpaItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ImpaItemResponse> findItems(String categoryCode, String segmentCode, String keyword, int limit) {
        return findItems(categoryCode, segmentCode, keyword, limit, 0);
    }

    public List<ImpaItemResponse> findItems(String categoryCode, String segmentCode, String keyword, int limit, int offset) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            """
            SELECT zh.impa_code,
                   item.cn_code,
                   zh.category_code,
                   category.category_name_cn,
                   LEFT(zh.impa_code, 4) AS segment_code,
                   zh.description AS name_cn,
                   en.description AS name_en,
                   zh.specification,
                   zh.unit
            FROM impa_item_i18n zh
            JOIN impa_item item ON item.impa_code = zh.impa_code
            LEFT JOIN impa_category category ON category.category_code = zh.category_code
            LEFT JOIN impa_item_i18n en
              ON en.impa_code = zh.impa_code
             AND en.language = 'en-US'
            WHERE zh.language = 'zh-CN'
              AND item.enabled = 1
            """
        );

        appendFilters(sql, args, categoryCode, segmentCode, keyword);

        sql.append(" ORDER BY zh.impa_code ASC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(Math.max(offset, 0));

        return jdbcTemplate.query(sql.toString(), this::mapRow, args.toArray());
    }

    public long countItems(String categoryCode, String segmentCode, String keyword) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            """
            SELECT COUNT(*)
            FROM impa_item_i18n zh
            JOIN impa_item item ON item.impa_code = zh.impa_code
            LEFT JOIN impa_item_i18n en
              ON en.impa_code = zh.impa_code
             AND en.language = 'en-US'
            WHERE zh.language = 'zh-CN'
              AND item.enabled = 1
            """
        );
        appendFilters(sql, args, categoryCode, segmentCode, keyword);
        Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    private void appendFilters(
        StringBuilder sql,
        List<Object> args,
        String categoryCode,
        String segmentCode,
        String keyword
    ) {
        if (categoryCode != null && !categoryCode.isBlank()) {
            sql.append(" AND zh.category_code = ?");
            args.add(categoryCode.trim());
        }
        if (segmentCode != null && !segmentCode.isBlank()) {
            sql.append(" AND LEFT(zh.impa_code, 4) = ?");
            args.add(segmentCode.trim());
        }
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        String exactKeyword = keyword.trim();
        String escapedKeyword = escapeLike(exactKeyword);
        String prefixKeyword = escapedKeyword + "%";
        String fuzzyKeyword = "%" + escapedKeyword + "%";
        sql.append(
            """
             AND (
               zh.impa_code = ?
               OR zh.impa_code LIKE ? ESCAPE '\\\\'
               OR zh.impa_code LIKE ? ESCAPE '\\\\'
               OR zh.description LIKE ? ESCAPE '\\\\'
               OR en.description LIKE ? ESCAPE '\\\\'
             )
            """
        );
        args.add(exactKeyword);
        args.add(prefixKeyword);
        args.add(fuzzyKeyword);
        args.add(fuzzyKeyword);
        args.add(fuzzyKeyword);
    }

    private String escapeLike(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    private ImpaItemResponse mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new ImpaItemResponse(
            resultSet.getString("impa_code"),
            resultSet.getString("cn_code"),
            resultSet.getString("category_code"),
            resultSet.getString("category_name_cn"),
            resultSet.getString("segment_code"),
            resultSet.getString("name_cn"),
            resultSet.getString("name_en"),
            resultSet.getString("specification"),
            resultSet.getString("unit")
        );
    }
}
