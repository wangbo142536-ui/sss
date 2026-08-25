package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class XlsxMappedShopImportParserTest {

    @Test
    void appliesValidatedStructureMappingToEveryBusinessRowWithoutDroppingRows() throws Exception {
        Path file = Files.createTempFile("mapped-shop-import", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("随机报价");
            sheet.createRow(0).createCell(0).setCellValue("某供应商报价单");
            var header = sheet.createRow(2);
            header.createCell(2).setCellValue("奇怪品名列");
            header.createCell(4).setCellValue("内部代码");
            header.createCell(5).setCellValue("参数文本");
            var first = sheet.createRow(3);
            first.createCell(2).setCellValue("Diagonal Cutting Plier");
            first.createCell(4).setCellValue("100108");
            first.createCell(5).setCellValue("180mm Cr-V");
            var second = sheet.createRow(4);
            second.createCell(2).setCellValue("Fluorescent Pen");
            second.createCell(4).setCellValue("470673");
            second.createCell(5).setCellValue("orange");
            try (OutputStream output = Files.newOutputStream(file)) { workbook.write(output); }
        }

        XlsxMaterialQuoteParser.GenericSheetDocument parsed = new XlsxMaterialQuoteParser().parseMappedGeneric(
            file, "随机报价", 3, 4,
            Map.of("productName", "C", "standardCode", "E", "specification", "F")
        );

        assertThat(parsed.rows()).hasSize(2);
        assertThat(parsed.rows()).extracting(XlsxMaterialQuoteParser.GenericProductRow::sourceItemId)
            .containsExactly("随机报价!R4", "随机报价!R5");
        assertThat(parsed.rows().get(0).productName()).isEqualTo("Diagonal Cutting Plier");
        assertThat(parsed.rows().get(0).rawStandardCode()).isEqualTo("100108");
        assertThat(parsed.rows().get(0).specification()).isEqualTo("180mm Cr-V");
        assertThat(parsed.rows().get(0).rawColumns()).containsEntry("_structureMapping", "MODEL_VALIDATED");
        Files.deleteIfExists(file);
    }
}
