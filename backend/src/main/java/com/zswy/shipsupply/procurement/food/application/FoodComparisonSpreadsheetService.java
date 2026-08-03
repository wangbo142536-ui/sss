package com.zswy.shipsupply.procurement.food.application;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandDetail;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodComparisonQuoteRepository;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodComparisonQuoteRepository.ComparisonQuoteRow;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodComparisonQuoteRepository.ComparisonQuoteUpdate;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodDemandRepository;

@Service
public class FoodComparisonSpreadsheetService {

    private static final String SHEET_NAME = "Food Comparison";
    private static final String[] HEADERS = {
        "Quote Item ID", "Demand Item ID", "No.", "English Name", "Chinese Name", "Remark",
        "Specification", "Unit", "Requested Quantity", "Quoted Quantity", "Unit Price", "Amount"
    };

    private final CurrentUserService currentUserService;
    private final FoodDemandRepository demandRepository;
    private final FoodComparisonQuoteRepository comparisonQuoteRepository;

    public FoodComparisonSpreadsheetService(
        CurrentUserService currentUserService,
        FoodDemandRepository demandRepository,
        FoodComparisonQuoteRepository comparisonQuoteRepository
    ) {
        this.currentUserService = currentUserService;
        this.demandRepository = demandRepository;
        this.comparisonQuoteRepository = comparisonQuoteRepository;
    }

    public ComparisonExportFile exportComparison(String authorizationHeader, long demandId, List<Long> quoteItemIds) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        DemandDetail demand = requireDemand(demandId, user.companyId());
        List<Long> normalizedIds = distinctIds(quoteItemIds);
        if (normalizedIds.isEmpty()) throw badRequest("FOOD_COMPARISON_EXPORT_ROWS_REQUIRED");
        List<ComparisonQuoteRow> rows = comparisonQuoteRepository.rows(demandId, user.companyId(), normalizedIds);
        if (rows.size() != normalizedIds.size()) throw badRequest("FOOD_COMPARISON_EXPORT_ROWS_INVALID");
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Row header = sheet.createRow(0);
            for (int index = 0; index < HEADERS.length; index++) {
                header.createCell(index, CellType.STRING).setCellValue(HEADERS[index]);
            }
            for (int index = 0; index < rows.size(); index++) {
                ComparisonQuoteRow item = rows.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0, CellType.NUMERIC).setCellValue(item.quoteItemId());
                row.createCell(1, CellType.NUMERIC).setCellValue(item.demandItemId());
                row.createCell(2, CellType.NUMERIC).setCellValue(item.sequenceNo());
                row.createCell(3, CellType.STRING).setCellValue(text(item.nameEn()));
                row.createCell(4, CellType.STRING).setCellValue(text(item.nameZh()));
                row.createCell(5, CellType.STRING).setCellValue(text(item.remark()));
                row.createCell(6, CellType.STRING).setCellValue(text(item.specification()));
                row.createCell(7, CellType.STRING).setCellValue(text(item.unit()));
                decimal(row.createCell(8, CellType.NUMERIC), item.requestedQuantity());
                decimal(row.createCell(9, CellType.NUMERIC), item.quotedQuantity());
                decimal(row.createCell(10, CellType.NUMERIC), item.unitPrice());
                row.createCell(11, CellType.FORMULA).setCellFormula("I" + (index + 2) + "*K" + (index + 2));
            }
            sheet.setColumnHidden(0, true);
            sheet.setColumnHidden(1, true);
            for (int index = 2; index < HEADERS.length; index++) sheet.autoSizeColumn(index);
            workbook.write(output);
            byte[] bytes = output.toByteArray();
            String demandNo = demand.demand().inquiryNo() == null ? demand.demand().demandNo() : demand.demand().inquiryNo();
            String fileName = safeFileName(demandNo) + "-comparison.xlsx";
            return new ComparisonExportFile(new ByteArrayResource(bytes), fileName, bytes.length);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FOOD_COMPARISON_EXPORT_FAILED", exception);
        }
    }

    @Transactional
    public ComparisonImportResponse importComparison(
        String authorizationHeader, long demandId, MultipartFile file
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireComparisonEditable(requireDemand(demandId, user.companyId()));
        validateFile(file);
        List<ComparisonQuoteUpdate> updates = parse(file);
        List<Long> quoteItemIds = updates.stream().map(ComparisonQuoteUpdate::quoteItemId).toList();
        List<ComparisonQuoteRow> rows = comparisonQuoteRepository.rows(demandId, user.companyId(), quoteItemIds);
        if (rows.size() != quoteItemIds.size()) throw badRequest("FOOD_COMPARISON_IMPORT_ROWS_INVALID");
        var rowsByQuoteItemId = rows.stream().collect(java.util.stream.Collectors.toMap(ComparisonQuoteRow::quoteItemId, row -> row));
        if (updates.stream().anyMatch(update -> rowsByQuoteItemId.get(update.quoteItemId()).demandItemId() != update.demandItemId())) {
            throw badRequest("FOOD_COMPARISON_IMPORT_ROWS_INVALID");
        }
        comparisonQuoteRepository.saveOverrides(demandId, user.userId(), updates);
        return new ComparisonImportResponse(demandId, updates.size());
    }

    @Transactional
    public ComparisonImportResponse saveComparisonItems(
        String authorizationHeader, long demandId, ComparisonItemsRequest request
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorizationHeader);
        requireComparisonEditable(requireDemand(demandId, user.companyId()));
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw badRequest("FOOD_COMPARISON_ITEMS_REQUIRED");
        }
        List<Long> quoteItemIds = request.items().stream().map(ComparisonItemRequest::quoteItemId).distinct().toList();
        if (quoteItemIds.size() != request.items().size()) throw badRequest("FOOD_COMPARISON_ITEM_DUPLICATE");
        List<ComparisonQuoteRow> rows = comparisonQuoteRepository.rows(demandId, user.companyId(), quoteItemIds);
        var rowsByQuoteItemId = rows.stream().collect(java.util.stream.Collectors.toMap(ComparisonQuoteRow::quoteItemId, row -> row));
        List<ComparisonQuoteUpdate> updates = request.items().stream().map(item -> {
            ComparisonQuoteRow row = rowsByQuoteItemId.get(item.quoteItemId());
            if (row == null || row.demandItemId() != item.demandItemId()) throw badRequest("FOOD_COMPARISON_ITEM_INVALID");
            validateValues(item.requestedQuantity(), item.quotedQuantity(), item.unitPrice());
            return new ComparisonQuoteUpdate(
                item.quoteItemId(), item.demandItemId(), item.requestedQuantity(), item.quotedQuantity(), item.unitPrice(), item.remark()
            );
        }).toList();
        comparisonQuoteRepository.saveOverrides(demandId, user.userId(), updates);
        return new ComparisonImportResponse(demandId, updates.size());
    }

    private List<ComparisonQuoteUpdate> parse(MultipartFile file) {
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null || !"QUOTEITEMID".equals(normalize(cellText(header.getCell(0))))) {
                throw badRequest("FOOD_COMPARISON_IMPORT_TEMPLATE_INVALID");
            }
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Set<Long> seen = new HashSet<>();
            List<ComparisonQuoteUpdate> updates = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || formatter.formatCellValue(row.getCell(0)).isBlank()) continue;
                long quoteItemId = decimal(formatter.formatCellValue(row.getCell(0)), "FOOD_COMPARISON_IMPORT_ID_INVALID").longValueExact();
                if (!seen.add(quoteItemId)) throw badRequest("FOOD_COMPARISON_IMPORT_ROW_DUPLICATE");
                long demandItemId = decimal(formatter.formatCellValue(row.getCell(1)), "FOOD_COMPARISON_IMPORT_DEMAND_ITEM_INVALID").longValueExact();
                BigDecimal requestedQuantity = decimal(formatter.formatCellValue(row.getCell(8)), "FOOD_COMPARISON_IMPORT_REQUESTED_QUANTITY_INVALID");
                BigDecimal quotedQuantity = decimal(formatter.formatCellValue(row.getCell(9)), "FOOD_COMPARISON_IMPORT_QUANTITY_INVALID");
                BigDecimal price = decimal(formatter.formatCellValue(row.getCell(10)), "FOOD_COMPARISON_IMPORT_PRICE_INVALID");
                validateValues(requestedQuantity, quotedQuantity, price);
                updates.add(new ComparisonQuoteUpdate(
                    quoteItemId, demandItemId, requestedQuantity, quotedQuantity, price, nullable(formatter.formatCellValue(row.getCell(5)))
                ));
            }
            if (updates.isEmpty()) throw badRequest("FOOD_COMPARISON_IMPORT_ROWS_REQUIRED");
            return updates;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FOOD_COMPARISON_IMPORT_FAILED", exception);
        }
    }

    private DemandDetail requireDemand(long demandId, long companyId) {
        DemandDetail demand = demandRepository.getDemand(demandId, companyId);
        if (demand == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FOOD_DEMAND_NOT_FOUND");
        return demand;
    }

    private void requireComparisonEditable(DemandDetail demand) {
        if ("ORDERED".equalsIgnoreCase(text(demand.demand().status()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FOOD_COMPARISON_LOCKED");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw badRequest("FOOD_COMPARISON_IMPORT_FILE_REQUIRED");
        String name = text(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx")) throw badRequest("ONLY_XLSX_SUPPORTED");
    }

    private List<Long> distinctIds(List<Long> values) {
        if (values == null) return List.of();
        return values.stream().filter(value -> value != null && value > 0).distinct().toList();
    }

    private BigDecimal decimal(String value, String message) {
        try {
            return new BigDecimal(text(value));
        } catch (Exception exception) {
            throw badRequest(message);
        }
    }

    private void decimal(Cell cell, BigDecimal value) {
        if (value == null) cell.setBlank(); else cell.setCellValue(value.doubleValue());
    }

    private void validateValues(BigDecimal requestedQuantity, BigDecimal quotedQuantity, BigDecimal unitPrice) {
        if (requestedQuantity == null || requestedQuantity.signum() <= 0
            || quotedQuantity == null || quotedQuantity.signum() <= 0
            || unitPrice == null || unitPrice.signum() < 0) {
            throw badRequest("FOOD_COMPARISON_IMPORT_VALUE_INVALID");
        }
    }

    private String cellText(Cell cell) {
        return cell == null ? "" : new DataFormatter(Locale.ROOT).formatCellValue(cell);
    }

    private String normalize(String value) {
        return text(value).replaceAll("[^A-Za-z0-9]+", "").toUpperCase(Locale.ROOT);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        String normalized = text(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String safeFileName(String value) {
        String normalized = text(value).isBlank() ? "food-comparison" : text(value);
        return URLEncoder.encode(normalized.replaceAll("[\\\\/:*?\"<>|]", "_"), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record ComparisonExportFile(ByteArrayResource resource, String fileName, long fileSize) {
    }

    public record ComparisonImportResponse(long demandId, int updatedCount) {
    }

    public record ComparisonItemsRequest(List<ComparisonItemRequest> items) {
    }

    public record ComparisonItemRequest(
        long quoteItemId,
        long demandItemId,
        BigDecimal requestedQuantity,
        BigDecimal quotedQuantity,
        BigDecimal unitPrice,
        String remark
    ) {
    }
}
