package com.zswy.shipsupply.procurement.food.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandItemPayload;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSaveRequest;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonSettings;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.InquirySummary;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.SupplierSummary;
import com.zswy.shipsupply.procurement.food.domain.FoodItemNormalizer;

@Repository
public class FoodDemandRepository {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String ACTIVE_SUPPLIER_ACCOUNT_EXISTS = """
        EXISTS (
          SELECT 1
          FROM sys_user supplier_account
          WHERE supplier_account.company_id = %s
            AND supplier_account.status = 'ACTIVE'
        )
        """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final FoodItemNormalizer normalizer;

    public FoodDemandRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, FoodItemNormalizer normalizer) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.normalizer = normalizer;
    }

    public Long insertDemand(long companyId, long userId, DemandSaveRequest request, int matchedCount, int pendingCount) {
        LocalDate today = LocalDate.now();
        int sequence = nextDailySequence(companyId, today);
        String suffix = String.format(Locale.ROOT, "%03d", sequence);
        String demandNo = "FREQ-" + DAY.format(today) + "-" + suffix;
        String inquiryNo = blankToNull(request.inquiryNo());
        if (inquiryNo == null) {
            inquiryNo = "FOOD" + DAY.format(today) + suffix;
        }
        String resolvedInquiryNo = inquiryNo;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO food_demand
                  (buyer_company_id, demand_no, inquiry_no, vessel_name, supply_port, vessel_eta, currency,
                   status, source_file_name, source_sheet_name, item_count, matched_count, pending_count,
                   created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, companyId);
            statement.setString(2, demandNo);
            statement.setString(3, resolvedInquiryNo);
            statement.setString(4, request.vesselName().trim());
            statement.setString(5, request.supplyPort().trim());
            statement.setTimestamp(6, Timestamp.valueOf(request.vesselEta()));
            statement.setString(7, currency(request.currency()));
            statement.setString(8, blankToNull(request.sourceFileName()));
            statement.setString(9, blankToNull(request.sourceSheetName()));
            statement.setInt(10, request.items().size());
            statement.setInt(11, matchedCount);
            statement.setInt(12, pendingCount);
            statement.setLong(13, userId);
            statement.setLong(14, userId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateDemand(long demandId, long companyId, long userId, DemandSaveRequest request, int matchedCount, int pendingCount) {
        String inquiryNo = blankToNull(request.inquiryNo());
        if (inquiryNo == null) {
            inquiryNo = jdbcTemplate.queryForObject(
                "SELECT inquiry_no FROM food_demand WHERE id = ? AND buyer_company_id = ?",
                String.class,
                demandId,
                companyId
            );
        }
        int updated = jdbcTemplate.update(
            """
            UPDATE food_demand
            SET inquiry_no = ?, vessel_name = ?, supply_port = ?, vessel_eta = ?, currency = ?,
                source_file_name = ?, source_sheet_name = ?, item_count = ?, matched_count = ?, pending_count = ?,
                updated_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND buyer_company_id = ? AND status = 'DRAFT'
            """,
            inquiryNo,
            request.vesselName().trim(),
            request.supplyPort().trim(),
            Timestamp.valueOf(request.vesselEta()),
            currency(request.currency()),
            blankToNull(request.sourceFileName()),
            blankToNull(request.sourceSheetName()),
            request.items().size(),
            matchedCount,
            pendingCount,
            userId,
            demandId,
            companyId
        );
        if (updated == 0) {
            throw new IllegalStateException("FOOD_DEMAND_NOT_EDITABLE");
        }
        jdbcTemplate.update("DELETE FROM food_demand_item WHERE demand_id = ?", demandId);
    }

    public void insertItems(long demandId, List<DemandItemPayload> items) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO food_demand_item
              (demand_id, sequence_no, name_en, name_zh, normalized_name_en, normalized_name_zh,
               remark, specification, normalized_specification, unit, normalized_unit,
               requested_quantity, match_status, match_reason, raw_row_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            items,
            Math.min(items.size(), 500),
            (statement, item) -> {
                statement.setLong(1, demandId);
                statement.setInt(2, item.sequenceNo());
                statement.setString(3, blankToNull(item.nameEn()));
                statement.setString(4, blankToNull(item.nameZh()));
                statement.setString(5, normalizer.name(item.nameEn()));
                statement.setString(6, normalizer.name(item.nameZh()));
                statement.setString(7, blankToNull(item.remark()));
                statement.setString(8, blankToNull(item.specification()));
                statement.setString(9, normalizer.specification(item.specification()));
                statement.setString(10, item.unit().trim());
                statement.setString(11, normalizer.unit(item.unit()));
                statement.setBigDecimal(12, item.requestedQuantity());
                statement.setString(13, item.matchStatus());
                statement.setString(14, blankToNull(item.matchReason()));
                statement.setString(15, json(item.rawColumns()));
            }
        );
    }

    public List<DemandSummary> listDemands(long companyId, String keyword, String status) {
        String like = "%" + safe(keyword).toLowerCase(Locale.ROOT) + "%";
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        String activeInquirySupplier = ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted("fis.supplier_company_id");
        String activeQuoteSupplier = ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted("fq.supplier_company_id");
        return jdbcTemplate.query(
            """
            SELECT d.*,
                   (SELECT COUNT(*) FROM food_inquiry_supplier fis WHERE fis.demand_id = d.id AND %s) supplier_count,
                   (SELECT COUNT(*) FROM food_supplier_quote fq WHERE fq.demand_id = d.id AND fq.status = 'SUBMITTED' AND %s) submitted_quote_count,
                   (SELECT MIN(fis.sent_at) FROM food_inquiry_supplier fis WHERE fis.demand_id = d.id AND %s) inquiry_sent_at,
                   (SELECT MAX(fis.quote_deadline_at) FROM food_inquiry_supplier fis WHERE fis.demand_id = d.id AND %s) quote_deadline_at
            FROM food_demand d
            WHERE d.buyer_company_id = ?
              AND (? = '' OR LOWER(CONCAT_WS(' ', d.demand_no, d.inquiry_no, d.vessel_name, d.supply_port)) LIKE ?)
              AND (? = '' OR d.status = ?)
            ORDER BY d.updated_at DESC, d.id DESC
            """.formatted(activeInquirySupplier, activeQuoteSupplier, activeInquirySupplier, activeInquirySupplier),
            (rs, rowNum) -> demandSummary(rs),
            companyId,
            safe(keyword),
            like,
            normalizedStatus,
            normalizedStatus
        );
    }

    public DemandDetail getDemand(long demandId, long companyId) {
        String activeInquirySupplier = ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted("fis.supplier_company_id");
        String activeQuoteSupplier = ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted("fq.supplier_company_id");
        DemandSummary summary = jdbcTemplate.query(
            """
            SELECT d.*,
                   (SELECT COUNT(*) FROM food_inquiry_supplier fis WHERE fis.demand_id = d.id AND %s) supplier_count,
                   (SELECT COUNT(*) FROM food_supplier_quote fq WHERE fq.demand_id = d.id AND fq.status = 'SUBMITTED' AND %s) submitted_quote_count,
                   (SELECT MIN(fis.sent_at) FROM food_inquiry_supplier fis WHERE fis.demand_id = d.id AND %s) inquiry_sent_at,
                   (SELECT MAX(fis.quote_deadline_at) FROM food_inquiry_supplier fis WHERE fis.demand_id = d.id AND %s) quote_deadline_at
            FROM food_demand d
            WHERE d.id = ? AND d.buyer_company_id = ?
            """.formatted(activeInquirySupplier, activeQuoteSupplier, activeInquirySupplier, activeInquirySupplier),
            (rs, rowNum) -> demandSummary(rs),
            demandId,
            companyId
        ).stream().findFirst().orElse(null);
        if (summary == null) {
            return null;
        }
        List<DemandItem> items = demandItems(demandId);
        List<String> source = jdbcTemplate.query(
            "SELECT CONCAT_WS('||', COALESCE(source_file_name, ''), COALESCE(source_sheet_name, '')) FROM food_demand WHERE id = ?",
            (rs, rowNum) -> rs.getString(1),
            demandId
        );
        String[] sourceParts = source.isEmpty() ? new String[] { "", "" } : source.get(0).split("\\|\\|", -1);
        return new DemandDetail(summary, sourceParts[0], sourceParts.length > 1 ? sourceParts[1] : "", items);
    }

    public ComparisonSettings getComparisonSettings(long demandId, long companyId) {
        return jdbcTemplate.query(
            """
            SELECT quote_markup_percent, fixed_freight_fee, fixed_customs_fee, fixed_crane_fee,
                   fixed_other_fee, supply_mode, fixed_provider_type, fixed_provider_id,
                   fixed_provider_name, traffic_service_json, comparison_selected_demand_item_ids_json
            FROM food_demand
            WHERE id = ? AND buyer_company_id = ?
            """,
            (rs, rowNum) -> new ComparisonSettings(
                rs.getBigDecimal("quote_markup_percent"), rs.getBigDecimal("fixed_freight_fee"),
                rs.getBigDecimal("fixed_customs_fee"), rs.getBigDecimal("fixed_crane_fee"),
                rs.getBigDecimal("fixed_other_fee"), rs.getString("supply_mode"),
                rs.getString("fixed_provider_type"), rs.getString("fixed_provider_id"),
                rs.getString("fixed_provider_name"), rs.getString("traffic_service_json"),
                selectedDemandItemIds(rs.getString("comparison_selected_demand_item_ids_json"))
            ), demandId, companyId
        ).stream().findFirst().orElse(null);
    }

    public int updateComparisonSettings(long demandId, long companyId, long userId, ComparisonSettings settings) {
        return jdbcTemplate.update(
            """
            UPDATE food_demand
            SET quote_markup_percent = ?, fixed_freight_fee = ?, fixed_customs_fee = ?,
                fixed_crane_fee = ?, fixed_other_fee = ?, supply_mode = ?, fixed_provider_type = ?,
                fixed_provider_id = ?, fixed_provider_name = ?, traffic_service_json = ?,
                comparison_selected_demand_item_ids_json = ?,
                updated_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND buyer_company_id = ?
            """,
            settings.markupPercent(), settings.fixedFreightFee(), settings.fixedCustomsFee(),
            settings.fixedCraneFee(), settings.fixedOtherFee(), settings.supplyMode(),
            settings.fixedProviderType(), settings.fixedProviderId(), settings.fixedProviderName(),
            settings.trafficServiceJson(), selectedDemandItemIdsJson(settings.selectedDemandItemIds()),
            userId, demandId, companyId
        );
    }

    private List<Long> selectedDemandItemIds(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("FOOD_COMPARISON_SELECTION_INVALID", exception);
        }
    }

    private String selectedDemandItemIdsJson(List<Long> demandItemIds) {
        if (demandItemIds == null) return null;
        try {
            return objectMapper.writeValueAsString(demandItemIds);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("FOOD_COMPARISON_SELECTION_INVALID", exception);
        }
    }

    public List<DemandItem> demandItems(long demandId) {
        return jdbcTemplate.query(
            """
            SELECT id, sequence_no, name_en, name_zh, remark, specification, unit,
                   requested_quantity, match_status, match_reason
            FROM food_demand_item
            WHERE demand_id = ?
            ORDER BY sequence_no, id
            """,
            (rs, rowNum) -> new DemandItem(
                rs.getLong("id"),
                rs.getInt("sequence_no"),
                rs.getString("name_en"),
                rs.getString("name_zh"),
                rs.getString("remark"),
                rs.getString("specification"),
                rs.getString("unit"),
                rs.getBigDecimal("requested_quantity"),
                rs.getString("match_status"),
                rs.getString("match_reason")
            ),
            demandId
        );
    }

    public List<SupplierSummary> listSuppliers(long currentCompanyId) {
        String sql = """
            SELECT supplier_company.id, supplier_company.company_name, supplier_company.company_type
            FROM company supplier_company
            WHERE supplier_company.status = 'ACTIVE' AND supplier_company.id <> ?
              AND (UPPER(supplier_company.company_type) LIKE '%SUPPLIER%' OR UPPER(supplier_company.company_type) LIKE '%FOOD%')
              AND
            """ + ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted("supplier_company.id") + """
            ORDER BY supplier_company.company_name, supplier_company.id
            """;
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new SupplierSummary(rs.getLong("id"), rs.getString("company_name"), rs.getString("company_type")),
            currentCompanyId
        );
    }

    public int insertInquirySuppliers(long demandId, long buyerCompanyId, long userId, List<Long> supplierIds, int quoteValidityDays) {
        int[][] counts = jdbcTemplate.batchUpdate(
            """
            INSERT IGNORE INTO food_inquiry_supplier
              (demand_id, buyer_company_id, supplier_company_id, status, sent_at, quote_deadline_at, created_by)
            VALUES (?, ?, ?, 'SENT', CURRENT_TIMESTAMP, TIMESTAMPADD(DAY, ?, CURRENT_TIMESTAMP), ?)
            """,
            supplierIds,
            supplierIds.size(),
            (statement, supplierId) -> {
                statement.setLong(1, demandId);
                statement.setLong(2, buyerCompanyId);
                statement.setLong(3, supplierId);
                statement.setInt(4, quoteValidityDays);
                statement.setLong(5, userId);
            }
        );
        jdbcTemplate.batchUpdate(
            "UPDATE food_inquiry_supplier SET quote_deadline_at = TIMESTAMPADD(DAY, ?, sent_at) WHERE demand_id = ? AND supplier_company_id = ?",
            supplierIds,
            supplierIds.size(),
            (statement, supplierId) -> {
                statement.setInt(1, quoteValidityDays);
                statement.setLong(2, demandId);
                statement.setLong(3, supplierId);
            }
        );
        int inserted = 0;
        for (int[] batch : counts) {
            for (int count : batch) {
                if (count > 0) inserted += count;
            }
        }
        jdbcTemplate.update(
            "UPDATE food_demand SET status = 'INQUIRY_SENT', updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND buyer_company_id = ?",
            userId,
            demandId,
            buyerCompanyId
        );
        return inserted;
    }

    public int insertInquirySuppliers(long demandId, long buyerCompanyId, long userId, List<Long> supplierIds, LocalDateTime quoteDeadlineAt) {
        int[][] counts = jdbcTemplate.batchUpdate(
            """
            INSERT IGNORE INTO food_inquiry_supplier
              (demand_id, buyer_company_id, supplier_company_id, status, sent_at, quote_deadline_at, created_by)
            VALUES (?, ?, ?, 'SENT', CURRENT_TIMESTAMP, ?, ?)
            """,
            supplierIds,
            supplierIds.size(),
            (statement, supplierId) -> {
                statement.setLong(1, demandId);
                statement.setLong(2, buyerCompanyId);
                statement.setLong(3, supplierId);
                statement.setTimestamp(4, Timestamp.valueOf(quoteDeadlineAt));
                statement.setLong(5, userId);
            }
        );
        jdbcTemplate.batchUpdate(
            "UPDATE food_inquiry_supplier SET quote_deadline_at = ? WHERE demand_id = ? AND supplier_company_id = ?",
            supplierIds,
            supplierIds.size(),
            (statement, supplierId) -> {
                statement.setTimestamp(1, Timestamp.valueOf(quoteDeadlineAt));
                statement.setLong(2, demandId);
                statement.setLong(3, supplierId);
            }
        );
        int inserted = 0;
        for (int[] batch : counts) {
            for (int count : batch) {
                if (count > 0) inserted += count;
            }
        }
        jdbcTemplate.update(
            "UPDATE food_demand SET status = 'INQUIRY_SENT', updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND buyer_company_id = ?",
            userId,
            demandId,
            buyerCompanyId
        );
        return inserted;
    }

    public List<InquirySummary> listBuyerInquiries(long buyerCompanyId, String keyword, String status) {
        return listInquiries("fis.buyer_company_id", buyerCompanyId, keyword, status);
    }

    public List<InquirySummary> listSupplierInquiries(long supplierCompanyId, String keyword, String status) {
        return listInquiries("fis.supplier_company_id", supplierCompanyId, keyword, status);
    }

    private List<InquirySummary> listInquiries(String companyColumn, long companyId, String keyword, String status) {
        String sql = """
            SELECT fis.id inquiry_supplier_id, d.id demand_id, q.id quote_id, d.demand_no, d.inquiry_no,
                   d.vessel_name, d.supply_port, d.vessel_eta, buyer.company_name buyer_name,
                   supplier.company_name supplier_name, fis.status, COALESCE(q.status, 'NOT_STARTED') quote_status,
                   d.item_count, COALESCE(q.quoted_item_count, 0) quoted_item_count,
                   COALESCE(q.missing_item_count, d.item_count) missing_item_count,
                   COALESCE(q.total_amount, 0) total_amount, fis.sent_at, fis.quote_deadline_at,
                   GREATEST(fis.updated_at, COALESCE(q.updated_at, fis.updated_at)) updated_at
            FROM food_inquiry_supplier fis
            JOIN food_demand d ON d.id = fis.demand_id
            JOIN company buyer ON buyer.id = fis.buyer_company_id
            JOIN company supplier ON supplier.id = fis.supplier_company_id
            LEFT JOIN food_supplier_quote q ON q.inquiry_supplier_id = fis.id
            WHERE %s = ?
              AND %s
              AND (? = '' OR LOWER(CONCAT_WS(' ', d.demand_no, d.inquiry_no, d.vessel_name, buyer.company_name, supplier.company_name)) LIKE ?)
              AND (? = '' OR COALESCE(q.status, fis.status) = ?)
            ORDER BY updated_at DESC, fis.id DESC
            """.formatted(companyColumn, ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted("supplier.id"));
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new InquirySummary(
                rs.getLong("inquiry_supplier_id"),
                rs.getLong("demand_id"),
                nullableLong(rs.getObject("quote_id")),
                rs.getString("demand_no"),
                rs.getString("inquiry_no"),
                rs.getString("vessel_name"),
                rs.getString("supply_port"),
                localDateTime(rs.getTimestamp("vessel_eta")),
                rs.getString("buyer_name"),
                rs.getString("supplier_name"),
                rs.getString("status"),
                rs.getString("quote_status"),
                rs.getInt("item_count"),
                rs.getInt("quoted_item_count"),
                rs.getInt("missing_item_count"),
                rs.getBigDecimal("total_amount"),
                localDateTime(rs.getTimestamp("sent_at")),
                localDateTime(rs.getTimestamp("quote_deadline_at")),
                localDateTime(rs.getTimestamp("updated_at"))
            ),
            companyId,
            safe(keyword),
            "%" + safe(keyword).toLowerCase(Locale.ROOT) + "%",
            normalizedStatus,
            normalizedStatus
        );
    }

    public boolean ownsDemand(long demandId, long companyId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM food_demand WHERE id = ? AND buyer_company_id = ?",
            Integer.class,
            demandId,
            companyId
        );
        return count != null && count > 0;
    }

    private int nextDailySequence(long companyId, LocalDate date) {
        jdbcTemplate.update(
            """
            INSERT INTO food_inquiry_daily_sequence (buyer_company_id, inquiry_date, last_sequence)
            VALUES (?, ?, LAST_INSERT_ID(1))
            ON DUPLICATE KEY UPDATE last_sequence = LAST_INSERT_ID(last_sequence + 1)
            """,
            companyId,
            Date.valueOf(date)
        );
        Integer sequence = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        if (sequence == null || sequence < 1) {
            throw new IllegalStateException("FOOD_INQUIRY_SEQUENCE_FAILED");
        }
        return sequence;
    }

    private DemandSummary demandSummary(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DemandSummary(
            rs.getLong("id"),
            rs.getString("demand_no"),
            rs.getString("inquiry_no"),
            rs.getString("vessel_name"),
            rs.getString("supply_port"),
            localDateTime(rs.getTimestamp("vessel_eta")),
            rs.getString("currency"),
            rs.getString("status"),
            rs.getInt("item_count"),
            rs.getInt("matched_count"),
            rs.getInt("pending_count"),
            rs.getInt("supplier_count"),
            rs.getInt("submitted_quote_count"),
            localDateTime(rs.getTimestamp("inquiry_sent_at")),
            localDateTime(rs.getTimestamp("quote_deadline_at")),
            localDateTime(rs.getTimestamp("updated_at"))
        );
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? java.util.Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("FOOD_RAW_ROW_INVALID", exception);
        }
    }

    private String currency(String value) {
        return safe(value).isBlank() ? "USD" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        return safe(value).isBlank() ? null : value.trim();
    }

    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
