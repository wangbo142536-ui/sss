package com.zswy.shipsupply.procurement.food.infrastructure.spreadsheet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.procurement.food.domain.FoodItemNormalizer;
import com.zswy.shipsupply.procurement.food.domain.FoodSpreadsheetDocument;
import com.zswy.shipsupply.procurement.food.domain.FoodSpreadsheetDocument.FoodSheetOption;
import com.zswy.shipsupply.procurement.food.domain.FoodSpreadsheetRow;

@Component
public class FoodSpreadsheetParser {

    private static final int HEADER_SCAN_LIMIT = 80;
    private static final Set<String> NAME_EN_HEADERS = Set.of("英文名字", "英文名称", "ENGLISHNAME", "DESCRIPTIONEN");
    private static final Set<String> NAME_ZH_HEADERS = Set.of("中文名字", "中文名称", "品名", "商品名称", "CHINESENAME", "DESCRIPTIONCN");
    private static final Set<String> SPEC_HEADERS = Set.of("规格", "包装规格", "SPEC", "SPECIFICATION", "PACKING");
    private static final Set<String> UNIT_HEADERS = Set.of("单位", "UNIT", "UOM");
    private static final Set<String> QUANTITY_HEADERS = Set.of("数量", "QTY", "QUANTITY");
    private static final Set<String> REQUESTED_QUANTITY_HEADERS = Set.of("需求数量", "REQUESTEDQTY", "REQUESTEDQUANTITY");
    private static final Set<String> QUOTED_QUANTITY_HEADERS = Set.of("报价数量", "QUOTEDQTY", "QUOTEDQUANTITY");
    private static final Set<String> PRICE_HEADERS = Set.of("单价", "单价USD", "UNITPRICE", "PRICE");
    private static final Set<String> AMOUNT_HEADERS = Set.of("总价", "金额", "TOTAL", "AMOUNT");
    private static final Set<String> REMARK_HEADERS = Set.of("备注", "REMARK", "REMARKS", "DESCRIPTION");
    private static final Set<String> SEQUENCE_HEADERS = Set.of("序号", "NO", "NO.", "序列");

    private final FoodItemNormalizer normalizer;

    public FoodSpreadsheetParser(FoodItemNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public FoodSpreadsheetDocument parse(MultipartFile file, String requestedSheet) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FOOD_FILE_REQUIRED");
        }
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<SheetProfile> profiles = profileSheets(workbook, formatter, evaluator);
            SheetProfile selected = selectProfile(profiles, requestedSheet);
            List<FoodSpreadsheetRow> rows = parseRows(selected, formatter, evaluator);
            if (rows.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FOOD_FILE_NO_ITEMS");
            }
            List<FoodSheetOption> options = profiles.stream()
                .filter(profile -> profile.headerRow >= 0)
                .map(profile -> new FoodSheetOption(profile.sheet.getSheetName(), profile.headerRow + 1, profile.validRows))
                .toList();
            return new FoodSpreadsheetDocument(file.getOriginalFilename(), selected.sheet.getSheetName(), selected.headerRow + 1, options, rows);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FOOD_FILE_PARSE_FAILED", exception);
        }
    }

    private List<SheetProfile> profileSheets(Workbook workbook, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<SheetProfile> profiles = new ArrayList<>();
        for (Sheet sheet : workbook) {
            SheetProfile profile = findHeader(sheet, formatter, evaluator);
            if (profile.headerRow >= 0) {
                profile.namedRows = countNamedRows(profile, formatter, evaluator);
                profile.validRows = countValidRows(profile, formatter, evaluator);
            }
            profiles.add(profile);
        }
        return profiles;
    }

    private SheetProfile selectProfile(List<SheetProfile> profiles, String requestedSheet) {
        if (requestedSheet != null && !requestedSheet.isBlank()) {
            return profiles.stream()
                .filter(profile -> profile.sheet.getSheetName().equals(requestedSheet) && profile.headerRow >= 0)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "FOOD_SHEET_NOT_FOUND"));
        }
        return profiles.stream()
            .filter(profile -> profile.headerRow >= 0 && profile.validRows > 0)
            .max((left, right) -> {
                int ratio = Double.compare(left.completeness(), right.completeness());
                return ratio != 0 ? ratio : Integer.compare(left.validRows, right.validRows);
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "FOOD_HEADER_NOT_FOUND"));
    }

    private SheetProfile findHeader(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        int maxRow = Math.min(sheet.getLastRowNum(), HEADER_SCAN_LIMIT);
        for (int rowIndex = 0; rowIndex <= maxRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> columns = mapHeader(row, formatter, evaluator);
            boolean hasName = columns.containsKey("nameEn") || columns.containsKey("nameZh");
            if (hasName && columns.containsKey("unit") && quantityColumn(columns) != null) {
                return new SheetProfile(sheet, rowIndex, columns, 0);
            }
        }
        return new SheetProfile(sheet, -1, Map.of(), 0);
    }

    private Map<String, Integer> mapHeader(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (Cell cell : row) {
            String header = headerKey(formatter.formatCellValue(cell, evaluator));
            putHeader(columns, "sequence", cell.getColumnIndex(), header, SEQUENCE_HEADERS);
            putHeader(columns, "nameEn", cell.getColumnIndex(), header, NAME_EN_HEADERS);
            putHeader(columns, "nameZh", cell.getColumnIndex(), header, NAME_ZH_HEADERS);
            putHeader(columns, "remark", cell.getColumnIndex(), header, REMARK_HEADERS);
            putHeader(columns, "specification", cell.getColumnIndex(), header, SPEC_HEADERS);
            putHeader(columns, "unit", cell.getColumnIndex(), header, UNIT_HEADERS);
            putHeader(columns, "quantity", cell.getColumnIndex(), header, QUANTITY_HEADERS);
            putHeader(columns, "requestedQuantity", cell.getColumnIndex(), header, REQUESTED_QUANTITY_HEADERS);
            putHeader(columns, "quotedQuantity", cell.getColumnIndex(), header, QUOTED_QUANTITY_HEADERS);
            putHeader(columns, "unitPrice", cell.getColumnIndex(), header, PRICE_HEADERS);
            putHeader(columns, "amount", cell.getColumnIndex(), header, AMOUNT_HEADERS);
        }
        return columns;
    }

    private void putHeader(Map<String, Integer> columns, String key, int index, String header, Set<String> aliases) {
        if (!columns.containsKey(key) && aliases.stream().map(this::headerKey).anyMatch(header::contains)) {
            columns.put(key, index);
        }
    }

    private Integer quantityColumn(Map<String, Integer> columns) {
        if (columns.containsKey("quotedQuantity")) return columns.get("quotedQuantity");
        if (columns.containsKey("quantity")) return columns.get("quantity");
        return columns.get("requestedQuantity");
    }

    private int countValidRows(SheetProfile profile, DataFormatter formatter, FormulaEvaluator evaluator) {
        int count = 0;
        for (int index = profile.headerRow + 1; index <= profile.sheet.getLastRowNum(); index++) {
            Row row = profile.sheet.getRow(index);
            String unit = row == null ? "" : value(row, profile.columns.get("unit"), formatter, evaluator);
            BigDecimal quantity = row == null ? null : decimal(value(row, quantityColumn(profile.columns), formatter, evaluator));
            if (row != null && hasName(row, profile.columns, formatter, evaluator)
                && !unit.isBlank() && quantity != null && quantity.signum() > 0) {
                count++;
            }
        }
        return count;
    }

    private int countNamedRows(SheetProfile profile, DataFormatter formatter, FormulaEvaluator evaluator) {
        int count = 0;
        for (int index = profile.headerRow + 1; index <= profile.sheet.getLastRowNum(); index++) {
            Row row = profile.sheet.getRow(index);
            if (row != null && hasName(row, profile.columns, formatter, evaluator)) count++;
        }
        return count;
    }

    private List<FoodSpreadsheetRow> parseRows(SheetProfile profile, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<FoodSpreadsheetRow> rows = new ArrayList<>();
        int fallbackSequence = 1;
        for (int index = profile.headerRow + 1; index <= profile.sheet.getLastRowNum(); index++) {
            Row row = profile.sheet.getRow(index);
            if (row == null || !hasName(row, profile.columns, formatter, evaluator)) {
                continue;
            }
            String nameEn = value(row, profile.columns.get("nameEn"), formatter, evaluator);
            String nameZh = value(row, profile.columns.get("nameZh"), formatter, evaluator);
            String remark = value(row, profile.columns.get("remark"), formatter, evaluator);
            String specification = value(row, profile.columns.get("specification"), formatter, evaluator);
            String unit = value(row, profile.columns.get("unit"), formatter, evaluator);
            BigDecimal quantity = decimal(value(row, quantityColumn(profile.columns), formatter, evaluator));
            BigDecimal unitPrice = decimal(value(row, profile.columns.get("unitPrice"), formatter, evaluator));
            BigDecimal amount = decimal(value(row, profile.columns.get("amount"), formatter, evaluator));
            int sequence = integer(value(row, profile.columns.get("sequence"), formatter, evaluator), fallbackSequence);
            String status = validationStatus(nameEn, nameZh, unit, quantity);
            String reason = validationReason(status);
            Map<String, String> raw = rawColumns(row, formatter, evaluator);
            rows.add(new FoodSpreadsheetRow(
                index + 1,
                sequence,
                nameEn,
                nameZh,
                remark,
                specification,
                unit,
                quantity,
                unitPrice,
                amount,
                status,
                reason,
                normalizer.matchKey(nameZh, nameEn, specification, unit),
                raw
            ));
            fallbackSequence++;
        }
        return rows;
    }

    private boolean hasName(Row row, Map<String, Integer> columns, DataFormatter formatter, FormulaEvaluator evaluator) {
        return !value(row, columns.get("nameEn"), formatter, evaluator).isBlank()
            || !value(row, columns.get("nameZh"), formatter, evaluator).isBlank();
    }

    private String validationStatus(String nameEn, String nameZh, String unit, BigDecimal quantity) {
        if ((nameEn == null || nameEn.isBlank()) && (nameZh == null || nameZh.isBlank())) {
            return "MISSING_NAME";
        }
        if (unit == null || unit.isBlank()) {
            return "MISSING_UNIT";
        }
        if (quantity == null || quantity.signum() <= 0) {
            return "INVALID_QUANTITY";
        }
        return "MATCHED";
    }

    private String validationReason(String status) {
        return switch (status) {
            case "MISSING_NAME" -> "伙食名称缺失";
            case "MISSING_UNIT" -> "单位缺失";
            case "INVALID_QUANTITY" -> "数量必须大于零";
            default -> "名称、规格和单位已生成匹配键";
        };
    }

    private Map<String, String> rawColumns(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<String, String> raw = new LinkedHashMap<>();
        for (Cell cell : row) {
            raw.put(columnName(cell.getColumnIndex()), formatter.formatCellValue(cell, evaluator));
        }
        return raw;
    }

    private String value(Row row, Integer column, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (column == null) {
            return "";
        }
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String normalized = value.replace(",", "").replace("$", "").replace("¥", "").trim();
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int integer(String value, int fallback) {
        BigDecimal decimal = decimal(value);
        return decimal == null ? fallback : decimal.intValue();
    }

    private String headerKey(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[\\s()（）_/\\-.]+", "");
    }

    private String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int current = index;
        do {
            name.insert(0, (char) ('A' + current % 26));
            current = current / 26 - 1;
        } while (current >= 0);
        return name.toString();
    }

    private static final class SheetProfile {
        private final Sheet sheet;
        private final int headerRow;
        private final Map<String, Integer> columns;
        private int namedRows;
        private int validRows;

        private SheetProfile(Sheet sheet, int headerRow, Map<String, Integer> columns, int validRows) {
            this.sheet = sheet;
            this.headerRow = headerRow;
            this.columns = columns;
            this.validRows = validRows;
        }

        private double completeness() {
            return namedRows == 0 ? 0 : (double) validRows / namedRows;
        }
    }
}
