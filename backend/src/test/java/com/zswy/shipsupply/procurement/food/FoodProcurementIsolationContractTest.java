package com.zswy.shipsupply.procurement.food;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class FoodProcurementIsolationContractTest {

    @Test
    void migrationCreatesFoodTablesWithoutChangingMaterialTables() throws Exception {
        String migration = Files.readString(Path.of("..", "db", "mysql", "056_create_food_procurement.sql")).toLowerCase();
        assertThat(migration).contains("create table if not exists food_demand");
        assertThat(migration).contains("create table if not exists food_supplier_quote");
        assertThat(migration).contains("create table if not exists food_purchase_order");
        assertThat(migration).doesNotContain("material_demand");
        assertThat(migration).doesNotContain("material_purchase_order");
        assertThat(migration).doesNotContain("update purchase_order");
    }

    @Test
    void foodJavaModuleDoesNotImportMaterialProcurementClasses() throws Exception {
        Path root = Path.of("src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food");
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file).toLowerCase();
                assertThat(source).as(file.toString()).doesNotContain("procurement.materials");
                assertThat(source).as(file.toString()).doesNotContain("material_demand");
                assertThat(source).as(file.toString()).doesNotContain(" from purchase_order");
            }
        }
    }

    @Test
    void inquiryNumbersAndComparisonUseGuardedDatabaseContracts() throws Exception {
        String sequenceMigration = Files.readString(Path.of("..", "db", "mysql", "058_food_inquiry_atomic_sequence.sql"));
        String quoteRepository = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "infrastructure", "persistence", "FoodQuoteRepository.java"
        ));
        String orderRepository = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "infrastructure", "persistence", "FoodOrderRepository.java"
        ));

        assertThat(sequenceMigration).contains("PRIMARY KEY (buyer_company_id, inquiry_date)");
        assertThat(sequenceMigration).contains("uk_food_demand_company_inquiry_no");
        assertThat(quoteRepository).contains("qi.availability IN ('AVAILABLE', 'PARTIAL')");
        assertThat(quoteRepository).contains("qi.match_status = 'MATCHED'");
        assertThat(orderRepository).contains("qi.match_status = 'MATCHED'");
    }

    @Test
    void foodInquiryQueriesRequireAnActiveSupplierAccount() throws Exception {
        String demandRepository = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "infrastructure", "persistence", "FoodDemandRepository.java"
        ));
        String quoteRepository = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "infrastructure", "persistence", "FoodQuoteRepository.java"
        ));

        assertThat(demandRepository).contains("ACTIVE_SUPPLIER_ACCOUNT_EXISTS");
        assertThat(demandRepository).contains("ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted(\"supplier_company.id\")");
        assertThat(demandRepository).contains("ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted(\"fis.supplier_company_id\")");
        assertThat(demandRepository).contains("ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted(\"fq.supplier_company_id\")");
        assertThat(demandRepository).contains("ACTIVE_SUPPLIER_ACCOUNT_EXISTS.formatted(\"supplier.id\")");
        assertThat(quoteRepository).contains("supplier_account.company_id = fis.supplier_company_id");
        assertThat(quoteRepository).contains("supplier_account.status = 'ACTIVE'");
    }

    @Test
    void quoteWritesAllowOnlyAssociatedBuyerOrSupplierCompanies() throws Exception {
        String quoteRepository = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "infrastructure", "persistence", "FoodQuoteRepository.java"
        ));

        assertThat(quoteRepository).contains("(q.supplier_company_id = ? OR q.buyer_company_id = ?)");
        assertThat(quoteRepository).contains("(supplier_company_id = ? OR buyer_company_id = ?)");
        assertThat(quoteRepository).contains("AND status = 'DRAFT'");
    }

    @Test
    void comparisonMigrationPersistsFeesTransportAndProfitSnapshots() throws Exception {
        String migration = Files.readString(Path.of("..", "db", "mysql", "059_food_comparison_settings.sql"))
            .toLowerCase();

        assertThat(migration).contains("alter table food_demand");
        assertThat(migration).contains("quote_markup_percent");
        assertThat(migration).contains("fixed_freight_fee");
        assertThat(migration).contains("supply_mode");
        assertThat(migration).contains("traffic_service_json");
        assertThat(migration).contains("alter table food_purchase_order");
        assertThat(migration).contains("cost_amount");
        assertThat(migration).contains("quoted_amount");
        assertThat(migration).contains("profit_amount");
        assertThat(migration).doesNotContain("material_demand");
        assertThat(migration).doesNotContain("material_purchase_order");
    }

    @Test
    void orderDetailReturnsThePersistedTransportAndFeeSnapshot() throws Exception {
        String dtoSource = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "api", "FoodProcurementDtos.java"
        ));
        String orderRepository = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "infrastructure", "persistence", "FoodOrderRepository.java"
        ));

        assertThat(dtoSource).contains("BigDecimal fixedFreightFee");
        assertThat(dtoSource).contains("String supplyMode");
        assertThat(dtoSource).contains("String fixedProviderType");
        assertThat(dtoSource).contains("String fixedProviderName");
        assertThat(dtoSource).contains("String trafficServiceJson");
        assertThat(orderRepository).contains("rs.getBigDecimal(\"fixed_freight_fee\")");
        assertThat(orderRepository).contains("rs.getString(\"traffic_service_json\")");
    }

    @Test
    void comparisonSelectionMigrationAddsOnlyTheFoodDemandSelectionSnapshot() throws Exception {
        String migration = Files.readString(Path.of("..", "db", "mysql", "062_food_comparison_selection.sql"))
            .toLowerCase();

        assertThat(migration).contains("alter table food_demand");
        assertThat(migration).contains("comparison_selected_demand_item_ids_json");
        assertThat(migration).doesNotContain("material_demand");
        assertThat(migration).doesNotContain("food_purchase_order");
    }

    @Test
    void orderLineMigrationKeepsCostAndQuotedAmountsTogether() throws Exception {
        String migration = Files.readString(Path.of("..", "db", "mysql", "061_food_order_line_pricing.sql"))
            .toLowerCase();
        String orderRepository = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "infrastructure", "persistence", "FoodOrderRepository.java"
        )).toLowerCase();

        assertThat(migration).contains("alter table food_purchase_order_item");
        assertThat(migration).contains("quoted_unit_price");
        assertThat(migration).contains("quoted_amount");
        assertThat(migration).doesNotContain("material_demand");
        assertThat(orderRepository).contains("unit_price, quoted_unit_price");
        assertThat(orderRepository).contains("amount, quoted_amount");
    }

    @Test
    void settlementAndDeliveryMigrationBackfillsCompletedSupplierOrders() throws Exception {
        String migration = Files.readString(Path.of("..", "db", "mysql", "064_food_settlement_invoice_and_delivery_address.sql"))
            .toLowerCase();
        String orderRepository = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "infrastructure", "persistence", "FoodOrderRepository.java"
        )).toLowerCase();

        assertThat(migration).contains("delivery_address");
        assertThat(migration).contains("actual_amount");
        assertThat(migration).contains("invoice_attachments_json");
        assertThat(migration).contains("insert ignore into food_settlement");
        assertThat(migration).contains("os.status = 'supplied'");
        assertThat(migration).doesNotContain("set actual_amount = amount");
        assertThat(migration).doesNotContain("status, amount, actual_amount");
        assertThat(orderRepository).contains("delivery_address = ?");
        assertThat(orderRepository).contains("invoice_attachments_json");
        assertThat(orderRepository).contains(
            "(order_id, supplier_order_id, buyer_company_id, supplier_company_id, status, amount)"
        );
        assertThat(orderRepository).doesNotContain(
            "(order_id, supplier_order_id, buyer_company_id, supplier_company_id, status, amount, actual_amount)"
        );
    }

    @Test
    void evaluationAttachmentsUseIdempotentFoodOnlyPersistenceContract() throws Exception {
        String migration = Files.readString(Path.of("..", "db", "mysql", "065_food_evaluation_attachments.sql"))
            .toLowerCase();
        String orderRepository = Files.readString(Path.of(
            "src", "main", "java", "com", "zswy", "shipsupply", "procurement", "food",
            "infrastructure", "persistence", "FoodOrderRepository.java"
        ));

        assertThat(migration).contains("information_schema.columns");
        assertThat(migration).contains("table_name = 'food_evaluation'");
        assertThat(migration).contains("column_name = 'attachments_json'");
        assertThat(migration).contains("alter table food_evaluation add column attachments_json json null");
        assertThat(migration).doesNotContain("service_evaluation");
        assertThat(orderRepository).contains("attachments_json = ?");
        assertThat(orderRepository).contains("evaluationAttachments(rs.getString(\"attachments_json\"))");
    }
}
