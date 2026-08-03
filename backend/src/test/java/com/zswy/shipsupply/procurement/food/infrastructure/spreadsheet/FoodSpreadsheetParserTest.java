package com.zswy.shipsupply.procurement.food.infrastructure.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.zswy.shipsupply.procurement.food.domain.FoodItemNormalizer;
import com.zswy.shipsupply.procurement.food.domain.FoodSpreadsheetDocument;

class FoodSpreadsheetParserTest {

    private final FoodSpreadsheetParser parser = new FoodSpreadsheetParser(new FoodItemNormalizer());

    @Test
    void selectsSheetWithMostItemsAndReadsRealQuotationColumns() throws Exception {
        MockMultipartFile file = workbookFile();

        FoodSpreadsheetDocument document = parser.parse(file, null);

        assertThat(document.sheetName()).isEqualTo("Quotation");
        assertThat(document.rows()).hasSize(2);
        assertThat(document.sheets()).extracting(FoodSpreadsheetDocument.FoodSheetOption::name)
            .containsExactly("History", "Quotation");
        assertThat(document.rows().get(0).nameEn()).isEqualTo("Fresh Apple");
        assertThat(document.rows().get(0).quantity()).isEqualByComparingTo("12.5");
        assertThat(document.rows().get(0).unitPrice()).isEqualByComparingTo("3.2");
        assertThat(document.rows().get(0).matchKey()).isEqualTo("\u82f9\u679c|10*500G|KG");
    }

    @Test
    void honorsRequestedSheetForImportPreview() throws Exception {
        FoodSpreadsheetDocument document = parser.parse(workbookFile(), "History");
        assertThat(document.sheetName()).isEqualTo("History");
        assertThat(document.rows()).hasSize(1);
    }

    @Test
    void readsQuotedQuantityFromExportedQuoteTemplate() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Food Quotation");
            Row header = sheet.createRow(3);
            String[] values = {
                "No.", "English Name", "Chinese Name", "Remark", "Specification", "Unit",
                "Requested Qty", "Quoted Qty", "Unit Price", "Amount", "Platform Item ID"
            };
            for (int index = 0; index < values.length; index++) header.createCell(index).setCellValue(values[index]);
            Row row = sheet.createRow(4);
            row.createCell(0).setCellValue(1);
            row.createCell(1).setCellValue("Fresh Apple");
            row.createCell(2).setCellValue("苹果");
            row.createCell(4).setCellValue("10*500G");
            row.createCell(5).setCellValue("KG");
            row.createCell(6).setCellValue(10);
            row.createCell(7).setCellValue(8);
            row.createCell(8).setCellValue(3.2);
            workbook.write(output);
            MockMultipartFile file = new MockMultipartFile(
                "file", "food-quote.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray()
            );

            FoodSpreadsheetDocument document = parser.parse(file, null);

            assertThat(document.rows()).singleElement().satisfies(parsed -> {
                assertThat(parsed.nameZh()).isEqualTo("苹果");
                assertThat(parsed.quantity()).isEqualByComparingTo("8");
                assertThat(parsed.unitPrice()).isEqualByComparingTo("3.2");
            });
        }
    }

    private MockMultipartFile workbookFile() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            createSheet(workbook.createSheet("History"), 1);
            createSheet(workbook.createSheet("Quotation"), 2);
            workbook.write(output);
            return new MockMultipartFile(
                "file", "provision.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray()
            );
        }
    }

    private void createSheet(Sheet sheet, int itemCount) {
        Row title = sheet.createRow(0);
        title.createCell(0).setCellValue("PROVISION QUOTATION");
        Row header = sheet.createRow(2);
        String[] values = {"No.", "English Name", "\u4e2d\u6587\u540d\u79f0", "Specification", "Unit", "Quantity", "Unit Price"};
        for (int index = 0; index < values.length; index++) header.createCell(index).setCellValue(values[index]);
        for (int index = 0; index < itemCount; index++) {
            Row row = sheet.createRow(index + 3);
            row.createCell(0).setCellValue(index + 1);
            row.createCell(1).setCellValue(index == 0 ? "Fresh Apple" : "Fresh Milk");
            row.createCell(2).setCellValue(index == 0 ? "\u82f9\u679c" : "\u725b\u5976");
            row.createCell(3).setCellValue(index == 0 ? "10 x 500 g" : "1 L");
            row.createCell(4).setCellValue(index == 0 ? "KGS" : "BTL");
            row.createCell(5).setCellValue(index == 0 ? 12.5 : 20);
            row.createCell(6).setCellValue(index == 0 ? 3.2 : 1.8);
        }
    }
}
