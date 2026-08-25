package com.zswy.shipsupply.procurement.materials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class XlsxGenericShopImportParserTest {

    @Test
    void parsesArbitrarySheetHeadersAndKeepsEverySourceRowIndependent() throws Exception {
        Path workbookPath = Files.createTempFile("cs-generic-shop-import-", ".xlsx");
        try {
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("文具及工具");
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("物料编码");
                header.createCell(1).setCellValue("品名");
                header.createCell(2).setCellValue("规格型号");
                header.createCell(3).setCellValue("集采价格");
                Row first = sheet.createRow(1);
                first.createCell(0).setCellValue("471234");
                first.createCell(1).setCellValue("荧光笔");
                first.createCell(2).setCellValue("粉色 2mm");
                first.createCell(3).setCellValue(1.5);
                Row second = sheet.createRow(2);
                second.createCell(0).setCellValue("471234");
                second.createCell(1).setCellValue("荧光笔");
                second.createCell(2).setCellValue("粉色 2mm");
                second.createCell(3).setCellValue(2.0);
                try (var output = Files.newOutputStream(workbookPath)) {
                    workbook.write(output);
                }
            }

            XlsxMaterialQuoteParser.GenericSheetDocument document = new XlsxMaterialQuoteParser()
                .parseGeneric(workbookPath, "文具及工具");

            assertEquals(2, document.rows().size());
            assertEquals("文具及工具!R2", document.rows().get(0).sourceItemId());
            assertEquals("文具及工具!R3", document.rows().get(1).sourceItemId());
            assertEquals("荧光笔", document.rows().get(0).productName());
            assertEquals("471234", document.rows().get(0).rawStandardCode());
            assertEquals("1.5", document.rows().get(0).price());
            assertEquals("2.0", document.rows().get(1).price());
            assertTrue(document.rows().stream().allMatch(row -> row.rawColumns().containsKey("规格型号")));
        } finally {
            Files.deleteIfExists(workbookPath);
        }
    }
}
