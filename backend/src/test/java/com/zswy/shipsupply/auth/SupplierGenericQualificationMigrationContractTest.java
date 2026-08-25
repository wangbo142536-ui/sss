package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SupplierGenericQualificationMigrationContractTest {

    @Test
    void migrationAddsOneIndependentGenericQualificationToEverySupplier() throws Exception {
        String migration = Files.readString(Path.of("../db/mysql/080_supplier_generic_registration_qualification.sql"));

        assertThat(migration)
            .contains("INIT-SUPPLIER-REG-CERT-")
            .contains("classpath:initialization/generic-enterprise-registration-qualification.png")
            .contains("company_type = 'SUPPLIER'")
            .contains("ENTERPRISE_REGISTRATION_QUALIFICATION")
            .contains("NOT EXISTS")
            .contains("仅用于系统展示，不作为政府签发证件或法律凭证");
        assertThat(Files.exists(Path.of(
            "src/main/resources/initialization/generic-enterprise-registration-qualification.png"
        ))).isTrue();
    }

    @Test
    void migrationDoesNotDeleteOrReplaceExistingEnterpriseQualifications() throws Exception {
        String migration = Files.readString(Path.of("../db/mysql/080_supplier_generic_registration_qualification.sql"));

        assertThat(migration).doesNotContain("DELETE FROM company_qualification");
        assertThat(migration).doesNotContain("UPDATE company_qualification SET file_id");
    }
}
