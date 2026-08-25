package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MaterialSupplierCandidateRepositoryContractTest {

    @Test
    void usesAllActiveMaterialSuppliersAndOnlyEnrichesShortlistedSkus() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/zswy/shipsupply/procurement/materials/MaterialSupplierSkuRepository.java"
        ));

        assertThat(source).doesNotContain("supplier_a_e2e", "supplier_e_demo", "supplier_f_demo");
        assertThat(source).contains("s.product_type = 'MATERIAL'", "s.shelf_status = 'ON_SHELF'", "c.status = 'ACTIVE'");
        assertThat(source).contains("findCandidates(MaterialSupplierCandidateQuery query)");
        assertThat(source).contains("enrichCandidates(java.util.Set<Long> requestedSkuIds)");
        assertThat(source).contains("WHERE sku_id IN (%s)");
    }
}
