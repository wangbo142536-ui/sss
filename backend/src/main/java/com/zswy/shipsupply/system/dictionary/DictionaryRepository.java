package com.zswy.shipsupply.system.dictionary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DictionaryRepository {

    private final JdbcTemplate jdbcTemplate;

    public DictionaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DictionaryTypeResponse> listTypes(String keyword, Boolean enabled) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            """
            SELECT id, type_code, type_name, description, sort_order, enabled
            FROM sys_dictionary_type
            WHERE 1 = 1
            """
        );
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + escapeLike(keyword.trim()) + "%";
            sql.append(" AND (type_code LIKE ? ESCAPE '\\\\' OR type_name LIKE ? ESCAPE '\\\\' OR description LIKE ? ESCAPE '\\\\')");
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (enabled != null) {
            sql.append(" AND enabled = ?");
            args.add(enabled ? 1 : 0);
        }
        sql.append(" ORDER BY sort_order ASC, type_code ASC");
        return jdbcTemplate.query(sql.toString(), this::mapType, args.toArray());
    }

    public List<DictionaryItemResponse> listItems(String typeCode, String keyword, Boolean enabled) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            """
            SELECT id, type_code, item_code, item_name, item_value, item_name_en,
                   description, sort_order, enabled, built_in
            FROM sys_dictionary_item
            WHERE 1 = 1
            """
        );
        if (typeCode != null && !typeCode.isBlank()) {
            sql.append(" AND type_code = ?");
            args.add(normalizeCode(typeCode));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + escapeLike(keyword.trim()) + "%";
            sql.append(" AND (item_code LIKE ? ESCAPE '\\\\' OR item_name LIKE ? ESCAPE '\\\\' OR item_value LIKE ? ESCAPE '\\\\' OR item_name_en LIKE ? ESCAPE '\\\\')");
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
        if (enabled != null) {
            sql.append(" AND enabled = ?");
            args.add(enabled ? 1 : 0);
        }
        sql.append(" ORDER BY type_code ASC, sort_order ASC, item_code ASC");
        return jdbcTemplate.query(sql.toString(), this::mapItem, args.toArray());
    }

    public boolean typeExists(String typeCode) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_dictionary_type WHERE type_code = ?",
            Integer.class,
            normalizeCode(typeCode)
        );
        return count != null && count > 0;
    }

    public boolean itemCodeExists(String typeCode, String itemCode, Long excludedId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_dictionary_item WHERE type_code = ? AND item_code = ?");
        args.add(normalizeCode(typeCode));
        args.add(normalizeCode(itemCode));
        if (excludedId != null) {
            sql.append(" AND id <> ?");
            args.add(excludedId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return count != null && count > 0;
    }

    public DictionaryTypeResponse createType(DictionaryTypeSaveRequest request) {
        jdbcTemplate.update(
            """
            INSERT INTO sys_dictionary_type (type_code, type_name, description, sort_order, enabled)
            VALUES (?, ?, ?, ?, ?)
            """,
            normalizeCode(request.typeCode()),
            trim(request.typeName()),
            trimToNull(request.description()),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            request.enabled() == null || request.enabled() ? 1 : 0
        );
        return getType(normalizeCode(request.typeCode()));
    }

    public DictionaryTypeResponse updateType(String typeCode, DictionaryTypeSaveRequest request) {
        int updated = jdbcTemplate.update(
            """
            UPDATE sys_dictionary_type
            SET type_name = ?, description = ?, sort_order = ?, enabled = ?
            WHERE type_code = ?
            """,
            trim(request.typeName()),
            trimToNull(request.description()),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            request.enabled() == null || request.enabled() ? 1 : 0,
            normalizeCode(typeCode)
        );
        return updated == 0 ? null : getType(normalizeCode(typeCode));
    }

    public void disableType(String typeCode) {
        String normalized = normalizeCode(typeCode);
        jdbcTemplate.update("UPDATE sys_dictionary_type SET enabled = 0 WHERE type_code = ?", normalized);
        jdbcTemplate.update("UPDATE sys_dictionary_item SET enabled = 0 WHERE type_code = ?", normalized);
    }

    public DictionaryItemResponse createItem(DictionaryItemSaveRequest request) {
        jdbcTemplate.update(
            """
            INSERT INTO sys_dictionary_item (
                type_code, item_code, item_name, item_value, item_name_en,
                description, sort_order, enabled, built_in
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
            """,
            normalizeCode(request.typeCode()),
            normalizeCode(request.itemCode()),
            trim(request.itemName()),
            trimToNull(request.itemValue()),
            trimToNull(request.itemNameEn()),
            trimToNull(request.description()),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            request.enabled() == null || request.enabled() ? 1 : 0
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return getItem(id);
    }

    public DictionaryItemResponse updateItem(Long itemId, DictionaryItemSaveRequest request) {
        int updated = jdbcTemplate.update(
            """
            UPDATE sys_dictionary_item
            SET type_code = ?, item_code = ?, item_name = ?, item_value = ?, item_name_en = ?,
                description = ?, sort_order = ?, enabled = ?
            WHERE id = ?
            """,
            normalizeCode(request.typeCode()),
            normalizeCode(request.itemCode()),
            trim(request.itemName()),
            trimToNull(request.itemValue()),
            trimToNull(request.itemNameEn()),
            trimToNull(request.description()),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            request.enabled() == null || request.enabled() ? 1 : 0,
            itemId
        );
        return updated == 0 ? null : getItem(itemId);
    }

    public void disableItem(Long itemId) {
        jdbcTemplate.update("UPDATE sys_dictionary_item SET enabled = 0 WHERE id = ?", itemId);
    }

    private DictionaryTypeResponse getType(String typeCode) {
        return jdbcTemplate.queryForObject(
            """
            SELECT id, type_code, type_name, description, sort_order, enabled
            FROM sys_dictionary_type
            WHERE type_code = ?
            """,
            this::mapType,
            normalizeCode(typeCode)
        );
    }

    private DictionaryItemResponse getItem(Long itemId) {
        return jdbcTemplate.queryForObject(
            """
            SELECT id, type_code, item_code, item_name, item_value, item_name_en,
                   description, sort_order, enabled, built_in
            FROM sys_dictionary_item
            WHERE id = ?
            """,
            this::mapItem,
            itemId
        );
    }

    private DictionaryTypeResponse mapType(ResultSet resultSet, int rowNum) throws SQLException {
        return new DictionaryTypeResponse(
            resultSet.getLong("id"),
            resultSet.getString("type_code"),
            resultSet.getString("type_name"),
            resultSet.getString("description"),
            resultSet.getInt("sort_order"),
            resultSet.getBoolean("enabled")
        );
    }

    private DictionaryItemResponse mapItem(ResultSet resultSet, int rowNum) throws SQLException {
        return new DictionaryItemResponse(
            resultSet.getLong("id"),
            resultSet.getString("type_code"),
            resultSet.getString("item_code"),
            resultSet.getString("item_name"),
            resultSet.getString("item_value"),
            resultSet.getString("item_name_en"),
            resultSet.getString("description"),
            resultSet.getInt("sort_order"),
            resultSet.getBoolean("enabled"),
            resultSet.getBoolean("built_in")
        );
    }

    private String normalizeCode(String value) {
        return trim(value).toUpperCase();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeLike(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
