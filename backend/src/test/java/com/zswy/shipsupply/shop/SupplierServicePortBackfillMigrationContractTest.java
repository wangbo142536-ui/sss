package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SupplierServicePortBackfillMigrationContractTest {

    @Test
    void migrationBackfillsEverySupplierStoreToZhoushanWithoutDestroyingBusinessData() throws Exception {
        String script = Files.readString(Path.of("..", "db", "mysql", "076_supplier_service_port_zhoushan_backfill.sql"));

        assertThat(script).contains("INSERT INTO shop_store");
        assertThat(script).contains("FROM company c");
        assertThat(script).contains("c.company_type = 'SUPPLIER'");
        assertThat(script).contains("'舟山港'");
        assertThat(script).contains("ON DUPLICATE KEY UPDATE");
        assertThat(script).contains("UPDATE shop_store s");
        assertThat(script).contains("JOIN company c ON c.id = s.company_id");
        assertThat(script).contains("SET s.service_ports = '舟山港'");
        assertThat(script).doesNotContain("DROP TABLE");
        assertThat(script).doesNotContain("TRUNCATE");
        assertThat(script).doesNotContain("DELETE FROM");
    }
}
