package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.procurement.materials.XlsxMaterialQuoteParser;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShopOfficialTemplateTest {

    @Mock CurrentUserService currentUserService;
    @Mock ShopRepository shopRepository;
    @Mock AuthRepository authRepository;

    @Test
    void officialTemplateIsVersionedAndExampleRowsAreNeverParsedAsProducts() throws Exception {
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(1L, 2L, "ACTIVE", "ACTIVE"));
        ShopService service = new ShopService(currentUserService, shopRepository, authRepository, null, null);

        byte[] bytes = service.importTemplate("Bearer token");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            for (String sheetName : new String[]{"物料", "伙食"}) {
                var sheet = workbook.getSheet(sheetName);
                assertNotNull(sheet);
                assertNotNull(sheet.getRow(0));
                assertNotNull(sheet.getRow(1));
                int imageColumn = -1;
                for (var cell : sheet.getRow(0)) {
                    if (cell.getStringCellValue().contains("图片")) imageColumn = cell.getColumnIndex();
                }
                assertThat(imageColumn).isGreaterThanOrEqualTo(0);
                assertThat(sheet.getRow(0).getCell(imageColumn).getCellComment()).isNotNull();
            }
        }

        Path file = Files.createTempFile("official-shop-template", ".xlsx");
        try {
            Files.write(file, bytes);
            XlsxMaterialQuoteParser parser = new XlsxMaterialQuoteParser();
            assertThat(parser.isOfficialShopTemplate(file)).isTrue();
            assertThat(parser.parseOfficialShopTemplate(file, "物料").rows()).isEmpty();
            assertThat(parser.parseOfficialShopTemplate(file, "伙食").rows()).isEmpty();
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
