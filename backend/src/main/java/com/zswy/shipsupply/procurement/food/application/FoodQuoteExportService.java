package com.zswy.shipsupply.procurement.food.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.QuoteItem;

@Service
public class FoodQuoteExportService {

    private final FoodProcurementApplicationService applicationService;

    public FoodQuoteExportService(FoodProcurementApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public ExportedQuote export(String authorizationHeader, long quoteId) {
        QuoteDetail quote = applicationService.getQuote(authorizationHeader, quoteId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Food Quotation");
            createTitle(sheet, quote, titleStyle(workbook));
            createHeader(sheet, headerStyle(workbook));
            createItems(sheet, quote, bodyStyle(workbook), amountStyle(workbook));
            configureColumns(sheet);
            workbook.write(output);
            String fileName = "food-quote-" + safeFileName(quote.quoteNo()) + ".xlsx";
            return new ExportedQuote(fileName, output.toByteArray());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FOOD_QUOTE_EXPORT_FAILED", exception);
        }
    }

    private void createTitle(Sheet sheet, QuoteDetail quote, CellStyle style) {
        Row title = sheet.createRow(0);
        Cell cell = title.createCell(0);
        cell.setCellValue("PROVISION QUOTATION / \u4f19\u98df\u62a5\u4ef7\u5355");
        cell.setCellStyle(style);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 10));
        Row info = sheet.createRow(1);
        info.createCell(0).setCellValue("Quote No.");
        info.createCell(1).setCellValue(quote.quoteNo());
        info.createCell(3).setCellValue("Vessel");
        info.createCell(4).setCellValue(quote.vesselName());
        info.createCell(6).setCellValue("Port");
        info.createCell(7).setCellValue(quote.supplyPort());
        info.createCell(9).setCellValue("Currency");
        info.createCell(10).setCellValue(quote.currency());
    }

    private void createHeader(Sheet sheet, CellStyle style) {
        String[] headers = {
            "No.", "English Name", "Chinese Name", "Remark", "Specification", "Unit",
            "Requested Qty", "Quoted Qty", "Unit Price", "Amount", "Platform Item ID"
        };
        Row row = sheet.createRow(3);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(style);
        }
    }

    private void createItems(Sheet sheet, QuoteDetail quote, CellStyle body, CellStyle amount) {
        int rowIndex = 4;
        for (QuoteItem item : quote.items()) {
            Row row = sheet.createRow(rowIndex++);
            text(row, 0, Integer.toString(item.sequenceNo()), body);
            text(row, 1, item.nameEn(), body);
            text(row, 2, item.nameZh(), body);
            text(row, 3, item.remark(), body);
            text(row, 4, item.specification(), body);
            text(row, 5, item.unit(), body);
            number(row, 6, item.requestedQuantity(), amount);
            number(row, 7, item.quotedQuantity(), amount);
            number(row, 8, item.unitPrice(), amount);
            Cell amountCell = row.createCell(9);
            amountCell.setCellFormula("IF(OR(H" + rowIndex + "=\"\",I" + rowIndex + "=\"\"),\"\",H" + rowIndex + "*I" + rowIndex + ")");
            amountCell.setCellStyle(amount);
            number(row, 10, java.math.BigDecimal.valueOf(item.quoteItemId()), amount);
        }
    }

    private void configureColumns(Sheet sheet) {
        int[] widths = {8, 28, 22, 24, 20, 10, 15, 15, 15, 16, 18};
        for (int index = 0; index < widths.length; index++) sheet.setColumnWidth(index, widths[index] * 256);
        sheet.createFreezePane(0, 4);
        sheet.setColumnHidden(10, true);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(3, Math.max(3, sheet.getLastRowNum()), 0, 9));
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = baseStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle bodyStyle(Workbook workbook) {
        CellStyle style = baseStyle(workbook);
        style.setWrapText(true);
        return style;
    }

    private CellStyle amountStyle(Workbook workbook) {
        CellStyle style = baseStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.0000"));
        return style;
    }

    private CellStyle baseStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void number(Row row, int column, java.math.BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private String safeFileName(String value) {
        return value == null ? "draft" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public record ExportedQuote(String fileName, byte[] content) {
    }
}
