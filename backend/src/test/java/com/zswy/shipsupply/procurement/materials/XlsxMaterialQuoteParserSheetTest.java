package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XlsxMaterialQuoteParserSheetTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesMaterialAndFoodByTheirFixedSheetNames() throws Exception {
        Path workbookPath = tempDir.resolve("dual-sheet.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            writeSheet(workbook, "物料", "MAT-001", "Cotton Rag");
            writeSheet(workbook, "伙食", "FOOD-001", "Fresh Apple");
            try (OutputStream output = Files.newOutputStream(workbookPath)) {
                workbook.write(output);
            }
        }

        XlsxMaterialQuoteParser parser = new XlsxMaterialQuoteParser();

        assertThat(parser.sheetNames(workbookPath)).containsExactly("物料", "伙食");
        assertThat(parser.parse(workbookPath, "物料").rows())
            .extracting(MaterialQuoteRow::supplierItemNo)
            .containsExactly("MAT-001");
        assertThat(parser.parse(workbookPath, "伙食").rows())
            .extracting(MaterialQuoteRow::supplierItemNo)
            .containsExactly("FOOD-001");
    }

    private void writeSheet(XSSFWorkbook workbook, String name, String supplierSku, String productName) {
        var sheet = workbook.createSheet(name);
        var header = sheet.createRow(0);
        header.createCell(0).setCellValue("Product Name");
        header.createCell(1).setCellValue("Supplier SKU Code");
        header.createCell(2).setCellValue("Qty");
        header.createCell(3).setCellValue("Unit");
        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(productName);
        row.createCell(1).setCellValue(supplierSku);
        row.createCell(2).setCellValue(10);
        row.createCell(3).setCellValue("PCS");
    }
}
