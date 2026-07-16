package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SettlementOrderRepositoryContractTest {

    @Test
    void keepsSettlementSourcesIdempotentAndProviderScopesIsolatedByType() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/zswy/shipsupply/procurement/materials/SettlementOrderRepository.java"
        ));

        assertThat(source).contains("UNIQUE KEY uk_settlement_source (purchase_order_id, settlement_type, source_key)");
        assertThat(source).contains("list(\"settlement.provider_company_id = ?\", companyId, \"SUPPLIER\"");
        assertThat(source).contains("list(\"settlement.provider_company_id = ?\", companyId, \"BARGE\"");
        assertThat(source).contains("purchase.vessel_name");
    }

    @Test
    void repairsBargeSettlementSourceThroughRealBookingPurchaseLink() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/zswy/shipsupply/procurement/materials/SettlementOrderRepository.java"
        ));

        assertThat(source)
            .contains("OR booking.purchase_order_id = ?")
            .contains("SET traffic.demand_id = COALESCE(?, traffic.demand_id)")
            .contains("traffic.purchase_order_id = ?")
            .contains("AND (traffic.purchase_order_id IS NULL OR booking.purchase_order_id = ?)")
            .contains("NULLIF(COALESCE(booking.freight_fee, 0) + COALESCE(booking.customs_fee, 0) + COALESCE(booking.crane_fee, 0), 0)");
    }
}
