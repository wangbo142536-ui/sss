package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SupplierPublicQualificationContractTest {

    @Test
    void qualificationListIsSupplierScopedActiveAndReadOnly() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/zswy/shipsupply/shop/ShopController.java"));
        String repository = Files.readString(Path.of("src/main/java/com/zswy/shipsupply/shop/ShopRepository.java"));
        assertThat(controller).contains("@GetMapping(\"/suppliers/{companyId}/qualifications\")");
        assertThat(controller).doesNotContain("@PutMapping(\"/suppliers/{companyId}/qualifications");
        assertThat(repository).contains("company_qualification", "qualification.status IN", "c.company_type = 'SUPPLIER'");
    }

    @Test
    void activeSupplierQualificationFilesAreDownloadableByAuthenticatedUsersOnly() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/com/zswy/shipsupply/auth/SupplierQualificationFileController.java"));
        String repository = Files.readString(Path.of("src/main/java/com/zswy/shipsupply/auth/AuthRepository.java"));
        assertThat(controller).contains("/api/shop/suppliers/{companyId}/qualifications/{qualificationId}/file");
        assertThat(repository).contains("findPublicSupplierQualificationFile", "qualification.status IN", "company.company_type = 'SUPPLIER'");
    }
}
