package com.zswy.shipsupply.procurement.food.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.ComparisonQuoteOption;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteItem;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteItemUpdate;

@Repository
public class FoodQuoteRepository {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JdbcTemplate jdbcTemplate;

    public FoodQuoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int ensureQuotesForDemand(long demandId, long userId) {
        List<InquirySeed> seeds = jdbcTemplate.query(
            """
            SELECT fis.id, fis.buyer_company_id, fis.supplier_company_id, d.currency
            FROM food_inquiry_supplier fis
            JOIN food_demand d ON d.id = fis.demand_id
            LEFT JOIN food_supplier_quote q ON q.inquiry_supplier_id = fis.id
            WHERE fis.demand_id = ? AND q.id IS NULL
              AND EXISTS (
                SELECT 1
                FROM sys_user supplier_account
                WHERE supplier_account.company_id = fis.supplier_company_id
                  AND supplier_account.status = 'ACTIVE'
              )
            """,
            (rs, rowNum) -> new InquirySeed(
                rs.getLong("id"),
                rs.getLong("buyer_company_id"),
                rs.getLong("supplier_company_id"),
                rs.getString("currency")
            ),
            demandId
        );
        for (InquirySeed seed : seeds) {
            long quoteId = insertQuote(demandId, seed, userId);
            insertQuoteItems(quoteId, demandId, userId);
            recalculate(quoteId);
        }
        return seeds.size();
    }

    public QuoteDetail getQuote(long quoteId, long companyId) {
        QuoteHead head = jdbcTemplate.query(
            """
            SELECT q.*, d.demand_no, d.inquiry_no, d.vessel_name, d.supply_port, d.vessel_eta,
                   buyer.company_name buyer_name, supplier.company_name supplier_name
            FROM food_supplier_quote q
            JOIN food_demand d ON d.id = q.demand_id
            JOIN company buyer ON buyer.id = q.buyer_company_id
            JOIN company supplier ON supplier.id = q.supplier_company_id
            WHERE q.id = ? AND (q.supplier_company_id = ? OR q.buyer_company_id = ?)
            """,
            (rs, rowNum) -> new QuoteHead(
                rs.getLong("id"),
                rs.getLong("demand_id"),
                rs.getLong("buyer_company_id"),
                rs.getLong("supplier_company_id"),
                rs.getString("quote_no"),
                rs.getString("demand_no"),
                rs.getString("inquiry_no"),
                rs.getString("vessel_name"),
                rs.getString("supply_port"),
                localDateTime(rs.getTimestamp("vessel_eta")),
                rs.getString("buyer_name"),
                rs.getString("supplier_name"),
                rs.getString("currency"),
                rs.getString("status"),
                rs.getInt("version_no"),
                rs.getBigDecimal("total_amount"),
                rs.getInt("quoted_item_count"),
                rs.getInt("missing_item_count"),
                rs.getInt("quantity_difference_count"),
                localDateTime(rs.getTimestamp("submitted_at")),
                localDateTime(rs.getTimestamp("updated_at"))
            ),
            quoteId,
            companyId,
            companyId
        ).stream().findFirst().orElse(null);
        if (head == null) {
            return null;
        }
        return toDetail(head, quoteItems(quoteId));
    }

    public QuoteDetail getQuoteForSupplierInquiry(long inquirySupplierId, long supplierCompanyId) {
        Long quoteId = jdbcTemplate.query(
            "SELECT id FROM food_supplier_quote WHERE inquiry_supplier_id = ? AND supplier_company_id = ?",
            (rs, rowNum) -> rs.getLong("id"),
            inquirySupplierId,
            supplierCompanyId
        ).stream().findFirst().orElse(null);
        return quoteId == null ? null : getQuote(quoteId, supplierCompanyId);
    }

    public List<QuoteItem> quoteItems(long quoteId) {
        return jdbcTemplate.query(
            """
            SELECT qi.id quote_item_id, qi.demand_item_id, di.sequence_no, di.name_en, di.name_zh,
                   di.remark, di.specification, di.unit, qi.requested_quantity, qi.quoted_quantity,
                   qi.unit_price, qi.amount, qi.availability, qi.price_source, qi.match_status, qi.supplier_remark
            FROM food_supplier_quote_item qi
            JOIN food_demand_item di ON di.id = qi.demand_item_id
            WHERE qi.quote_id = ?
            ORDER BY di.sequence_no, di.id
            """,
            (rs, rowNum) -> new QuoteItem(
                rs.getLong("quote_item_id"),
                rs.getLong("demand_item_id"),
                rs.getInt("sequence_no"),
                rs.getString("name_en"),
                rs.getString("name_zh"),
                rs.getString("remark"),
                rs.getString("specification"),
                rs.getString("unit"),
                rs.getBigDecimal("requested_quantity"),
                rs.getBigDecimal("quoted_quantity"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("amount"),
                rs.getString("availability"),
                rs.getString("price_source"),
                rs.getString("match_status"),
                rs.getString("supplier_remark")
            ),
            quoteId
        );
    }

    public void saveQuoteItems(long quoteId, long companyId, long userId, List<QuoteItemUpdate> updates) {
        requireDraftQuote(quoteId, companyId);
        jdbcTemplate.batchUpdate(
            """
            UPDATE food_supplier_quote_item qi
            JOIN food_supplier_quote q ON q.id = qi.quote_id
            SET qi.quoted_quantity = ?, qi.unit_price = ?,
                qi.amount = CASE WHEN ? IS NULL OR ? IS NULL THEN NULL ELSE ? * ? END,
                qi.availability = ?, qi.supplier_remark = ?, qi.price_source = ?, qi.updated_by = ?, qi.updated_at = CURRENT_TIMESTAMP
            WHERE qi.id = ? AND qi.quote_id = ?
              AND (q.supplier_company_id = ? OR q.buyer_company_id = ?)
              AND q.status = 'DRAFT'
            """,
            updates,
            Math.min(updates.size(), 500),
            (statement, update) -> {
                BigDecimal quantity = update.quotedQuantity();
                BigDecimal price = update.unitPrice();
                statement.setBigDecimal(1, quantity);
                statement.setBigDecimal(2, price);
                statement.setBigDecimal(3, quantity);
                statement.setBigDecimal(4, price);
                statement.setBigDecimal(5, quantity);
                statement.setBigDecimal(6, price);
                statement.setString(7, availability(update.availability()));
                statement.setString(8, blankToNull(update.supplierRemark()));
                statement.setString(9, priceSource(update.priceSource()));
                statement.setLong(10, userId);
                statement.setLong(11, update.quoteItemId());
                statement.setLong(12, quoteId);
                statement.setLong(13, companyId);
                statement.setLong(14, companyId);
            }
        );
        recalculate(quoteId);
    }

    public void virtualFill(long quoteId, long companyId, long userId, boolean overwriteExisting) {
        requireDraftQuote(quoteId, companyId);
        String overwriteClause = overwriteExisting ? "" : " AND qi.unit_price IS NULL";
        jdbcTemplate.update(
            """
            UPDATE food_supplier_quote_item qi
            JOIN food_supplier_quote q ON q.id = qi.quote_id
            JOIN food_demand_item di ON di.id = qi.demand_item_id
            SET qi.quoted_quantity = COALESCE(qi.quoted_quantity, qi.requested_quantity),
                qi.unit_price = ROUND(0.50 + MOD(ABS(CRC32(CONCAT(q.supplier_company_id, ':', di.normalized_name_zh, ':', di.normalized_name_en, ':', di.normalized_specification, ':', di.normalized_unit))), 4951) / 100, 2),
                qi.amount = ROUND(COALESCE(qi.quoted_quantity, qi.requested_quantity) *
                    (0.50 + MOD(ABS(CRC32(CONCAT(q.supplier_company_id, ':', di.normalized_name_zh, ':', di.normalized_name_en, ':', di.normalized_specification, ':', di.normalized_unit))), 4951) / 100), 2),
                qi.price_source = 'VIRTUAL', qi.availability = 'AVAILABLE', qi.updated_by = ?, qi.updated_at = CURRENT_TIMESTAMP
            WHERE qi.quote_id = ?
              AND (q.supplier_company_id = ? OR q.buyer_company_id = ?)
              AND q.status = 'DRAFT'
            """ + overwriteClause,
            userId,
            quoteId,
            companyId,
            companyId
        );
        recalculate(quoteId);
    }

    public void submitQuote(long quoteId, long companyId) {
        requireDraftQuote(quoteId, companyId);
        Integer missing = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM food_supplier_quote_item WHERE quote_id = ? AND availability = 'AVAILABLE' AND (unit_price IS NULL OR quoted_quantity IS NULL OR quoted_quantity <= 0)",
            Integer.class,
            quoteId
        );
        if (missing != null && missing > 0) {
            throw new IllegalStateException("FOOD_QUOTE_INCOMPLETE");
        }
        jdbcTemplate.update(
            "UPDATE food_supplier_quote SET status = 'SUBMITTED', submitted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            quoteId
        );
        jdbcTemplate.update(
            """
            UPDATE food_inquiry_supplier fis
            JOIN food_supplier_quote q ON q.inquiry_supplier_id = fis.id
            SET fis.status = 'SUBMITTED', fis.submitted_at = CURRENT_TIMESTAMP, fis.updated_at = CURRENT_TIMESTAMP
            WHERE q.id = ?
            """,
            quoteId
        );
        jdbcTemplate.update(
            """
            UPDATE food_demand d
            JOIN food_supplier_quote q ON q.demand_id = d.id
            SET d.status = 'QUOTED', d.updated_at = CURRENT_TIMESTAMP
            WHERE q.id = ? AND d.status IN ('INQUIRY_SENT', 'QUOTED')
            """,
            quoteId
        );
    }

    public long saveImportBatch(long quoteId, String fileName, String previewJson, int matchedCount, int issueCount, long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO food_quote_import_batch
                  (quote_id, file_name, status, preview_json, matched_count, issue_count, created_by)
                VALUES (?, ?, 'PREVIEW', ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, quoteId);
            statement.setString(2, fileName);
            statement.setString(3, previewJson);
            statement.setInt(4, matchedCount);
            statement.setInt(5, issueCount);
            statement.setLong(6, userId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public ImportBatch getImportBatch(long batchId, long quoteId) {
        return jdbcTemplate.query(
            "SELECT id, quote_id, status, preview_json FROM food_quote_import_batch WHERE id = ? AND quote_id = ?",
            (rs, rowNum) -> new ImportBatch(rs.getLong("id"), rs.getLong("quote_id"), rs.getString("status"), rs.getString("preview_json")),
            batchId,
            quoteId
        ).stream().findFirst().orElse(null);
    }

    public int commitImport(long batchId, long quoteId, long companyId, long userId, List<QuoteItemUpdate> updates) {
        requireDraftQuote(quoteId, companyId);
        ImportBatch batch = getImportBatch(batchId, quoteId);
        if (batch == null || !"PREVIEW".equals(batch.status())) {
            throw new IllegalStateException("FOOD_QUOTE_IMPORT_NOT_COMMITTABLE");
        }
        saveQuoteItems(quoteId, companyId, userId, updates);
        jdbcTemplate.update(
            "UPDATE food_quote_import_batch SET status = 'COMMITTED', committed_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PREVIEW'",
            batchId
        );
        jdbcTemplate.update("UPDATE food_supplier_quote SET version_no = version_no + 1 WHERE id = ?", quoteId);
        return updates.size();
    }

    public List<ComparisonQuoteOption> comparisonOptions(long demandId) {
        return jdbcTemplate.query(
            """
            SELECT qi.id quote_item_id, q.id quote_id, q.supplier_company_id, supplier.company_name supplier_name,
                   qi.demand_item_id, qi.requested_quantity,
                   COALESCE(override_price.quoted_quantity, qi.quoted_quantity) quoted_quantity,
                   COALESCE(override_price.unit_price, qi.unit_price) unit_price,
                   ROUND(qi.requested_quantity
                     * COALESCE(override_price.unit_price, qi.unit_price), 4) amount,
                   qi.availability,
                   CASE WHEN override_price.id IS NULL THEN qi.price_source ELSE 'COMPARISON_IMPORTED' END price_source,
                   CAST(matched_sku.product_tags AS CHAR) product_tags
            FROM food_supplier_quote_item qi
            JOIN food_supplier_quote q ON q.id = qi.quote_id AND q.status = 'SUBMITTED'
            JOIN company supplier ON supplier.id = q.supplier_company_id
            JOIN food_demand_item demand_item ON demand_item.id = qi.demand_item_id
            LEFT JOIN shop_sku matched_sku ON matched_sku.id = (
              SELECT sku.id
              FROM shop_sku sku
              WHERE sku.company_id = q.supplier_company_id
                AND sku.product_type = 'FOOD'
                AND sku.shelf_status = 'ON_SHELF'
                AND (
                  LOWER(REPLACE(TRIM(sku.product_name), ' ', '')) IN (
                    LOWER(REPLACE(TRIM(COALESCE(demand_item.name_zh, '')), ' ', '')),
                    LOWER(REPLACE(TRIM(COALESCE(demand_item.name_en, '')), ' ', ''))
                  )
                  OR LOWER(REPLACE(TRIM(COALESCE(sku.normalized_name, '')), ' ', '')) IN (
                    LOWER(REPLACE(TRIM(COALESCE(demand_item.name_zh, '')), ' ', '')),
                    LOWER(REPLACE(TRIM(COALESCE(demand_item.name_en, '')), ' ', ''))
                  )
                )
              ORDER BY
                CASE WHEN LOWER(REPLACE(TRIM(COALESCE(sku.specification_summary, '')), ' ', ''))
                  = LOWER(REPLACE(TRIM(COALESCE(demand_item.specification, '')), ' ', '')) THEN 0 ELSE 1 END,
                sku.updated_at DESC,
                sku.id DESC
              LIMIT 1
            )
            LEFT JOIN food_comparison_quote_override override_price
              ON override_price.demand_id = q.demand_id AND override_price.quote_item_id = qi.id
            WHERE q.demand_id = ?
              AND qi.availability IN ('AVAILABLE', 'PARTIAL')
              AND qi.match_status = 'MATCHED'
              AND COALESCE(override_price.unit_price, qi.unit_price) IS NOT NULL
              AND COALESCE(override_price.quoted_quantity, qi.quoted_quantity) IS NOT NULL
              AND COALESCE(override_price.quoted_quantity, qi.quoted_quantity) > 0
            ORDER BY qi.demand_item_id, COALESCE(override_price.unit_price, qi.unit_price), q.supplier_company_id
            """,
            (rs, rowNum) -> new ComparisonQuoteOption(
                rs.getLong("demand_item_id"),
                rs.getLong("quote_item_id"),
                rs.getLong("quote_id"),
                rs.getLong("supplier_company_id"),
                rs.getString("supplier_name"),
                rs.getBigDecimal("requested_quantity"),
                rs.getBigDecimal("quoted_quantity"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("amount"),
                rs.getString("availability"),
                rs.getString("price_source"),
                quantitySatisfied(rs.getBigDecimal("requested_quantity"), rs.getBigDecimal("quoted_quantity")),
                false,
                strategyTags(rs.getString("product_tags"))
            ),
            demandId
        );
    }

    private List<String> strategyTags(String json) {
        if (json == null || json.isBlank()) return List.of();
        List<String> tags = new ArrayList<>();
        if (json.contains("价格低")) tags.add("价格低");
        if (json.contains("质量高")) tags.add("质量高");
        return List.copyOf(tags);
    }

    public int submittedSupplierCount(long demandId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM food_supplier_quote WHERE demand_id = ? AND status = 'SUBMITTED'",
            Integer.class,
            demandId
        );
        return count == null ? 0 : count;
    }

    public boolean quoteBelongsToCompany(long quoteId, long companyId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM food_supplier_quote WHERE id = ? AND (supplier_company_id = ? OR buyer_company_id = ?)",
            Integer.class,
            quoteId,
            companyId,
            companyId
        );
        return count != null && count > 0;
    }

    public int quoteVersion(long quoteId) {
        Integer version = jdbcTemplate.queryForObject("SELECT version_no FROM food_supplier_quote WHERE id = ?", Integer.class, quoteId);
        return version == null ? 1 : version;
    }

    private long insertQuote(long demandId, InquirySeed seed, long userId) {
        String quoteNo = "FQ-" + DAY.format(LocalDateTime.now()) + "-" + demandId + "-" + seed.supplierCompanyId();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO food_supplier_quote
                  (demand_id, inquiry_supplier_id, buyer_company_id, supplier_company_id, quote_no,
                   currency, status, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, demandId);
            statement.setLong(2, seed.inquirySupplierId());
            statement.setLong(3, seed.buyerCompanyId());
            statement.setLong(4, seed.supplierCompanyId());
            statement.setString(5, quoteNo);
            statement.setString(6, seed.currency());
            statement.setLong(7, userId);
            statement.setLong(8, userId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void insertQuoteItems(long quoteId, long demandId, long userId) {
        jdbcTemplate.update(
            """
            INSERT INTO food_supplier_quote_item
              (quote_id, demand_item_id, requested_quantity, quoted_quantity, availability, match_status, updated_by)
            SELECT ?, di.id, di.requested_quantity, di.requested_quantity, 'AVAILABLE', di.match_status, ?
            FROM food_demand_item di
            WHERE di.demand_id = ?
            ORDER BY di.sequence_no, di.id
            """,
            quoteId,
            userId,
            demandId
        );
    }

    private void recalculate(long quoteId) {
        jdbcTemplate.update(
            """
            UPDATE food_supplier_quote q
            JOIN (
              SELECT quote_id,
                     COALESCE(SUM(COALESCE(amount, 0)), 0) total_amount,
                     SUM(CASE WHEN availability <> 'AVAILABLE' OR unit_price IS NOT NULL THEN 1 ELSE 0 END) quoted_count,
                     SUM(CASE WHEN availability = 'AVAILABLE' AND unit_price IS NULL THEN 1 ELSE 0 END) missing_count,
                     SUM(CASE WHEN quoted_quantity IS NOT NULL AND quoted_quantity <> requested_quantity THEN 1 ELSE 0 END) quantity_difference_count
              FROM food_supplier_quote_item
              WHERE quote_id = ?
              GROUP BY quote_id
            ) totals ON totals.quote_id = q.id
            SET q.total_amount = totals.total_amount,
                q.quoted_item_count = totals.quoted_count,
                q.missing_item_count = totals.missing_count,
                q.quantity_difference_count = totals.quantity_difference_count,
                q.updated_at = CURRENT_TIMESTAMP
            WHERE q.id = ?
            """,
            quoteId,
            quoteId
        );
    }

    private void requireDraftQuote(long quoteId, long companyId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM food_supplier_quote WHERE id = ? AND (supplier_company_id = ? OR buyer_company_id = ?) AND status = 'DRAFT'",
            Integer.class,
            quoteId,
            companyId,
            companyId
        );
        if (count == null || count == 0) {
            throw new IllegalStateException("FOOD_QUOTE_NOT_EDITABLE");
        }
    }

    private QuoteDetail toDetail(QuoteHead head, List<QuoteItem> items) {
        return new QuoteDetail(
            head.quoteId(), head.demandId(), head.quoteNo(), head.demandNo(), head.inquiryNo(), head.vesselName(),
            head.supplyPort(), head.vesselEta(), head.buyerName(), head.supplierName(), head.currency(), head.status(),
            head.versionNo(), head.totalAmount(), head.quotedItemCount(), head.missingItemCount(),
            head.quantityDifferenceCount(), head.submittedAt(), head.updatedAt(), items
        );
    }

    private String availability(String value) {
        String normalized = value == null ? "AVAILABLE" : value.trim().toUpperCase(Locale.ROOT);
        return List.of("AVAILABLE", "UNAVAILABLE", "PARTIAL").contains(normalized) ? normalized : "AVAILABLE";
    }

    private String priceSource(String value) {
        String normalized = value == null ? "MANUAL" : value.trim().toUpperCase(Locale.ROOT);
        return List.of("MANUAL", "IMPORTED", "VIRTUAL", "CATALOG").contains(normalized) ? normalized : "MANUAL";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean quantitySatisfied(BigDecimal requested, BigDecimal quoted) {
        return requested != null && quoted != null && quoted.compareTo(requested) >= 0;
    }

    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private record InquirySeed(long inquirySupplierId, long buyerCompanyId, long supplierCompanyId, String currency) {
    }

    public record ImportBatch(long batchId, long quoteId, String status, String previewJson) {
    }

    private record QuoteHead(
        long quoteId,
        long demandId,
        long buyerCompanyId,
        long supplierCompanyId,
        String quoteNo,
        String demandNo,
        String inquiryNo,
        String vesselName,
        String supplyPort,
        LocalDateTime vesselEta,
        String buyerName,
        String supplierName,
        String currency,
        String status,
        int versionNo,
        BigDecimal totalAmount,
        int quotedItemCount,
        int missingItemCount,
        int quantityDifferenceCount,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt
    ) {
    }
}
