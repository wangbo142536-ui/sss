package com.zswy.shipsupply.procurement.materials;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@Service
public class MaterialQuoteExportService {

    private final CurrentUserService currentUserService;
    private final MaterialDemandRepository materialDemandRepository;
    private final JdbcTemplate jdbcTemplate;
    private final XlsxMaterialQuoteParser xlsxMaterialQuoteParser;

    public MaterialQuoteExportService(
        CurrentUserService currentUserService,
        MaterialDemandRepository materialDemandRepository,
        JdbcTemplate jdbcTemplate,
        XlsxMaterialQuoteParser xlsxMaterialQuoteParser
    ) {
        this.currentUserService = currentUserService;
        this.materialDemandRepository = materialDemandRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.xlsxMaterialQuoteParser = xlsxMaterialQuoteParser;
    }

    public MaterialQuoteExportFile exportQuote(String authorizationHeader, Long demandId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        MaterialDemandSummaryResponse demand = materialDemandRepository.findSummaryById(currentUser.companyId(), demandId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MATERIAL_DEMAND_NOT_FOUND"));
        if (demand.sourceFileId() == null || demand.sourceFileId().isBlank()) {
            return exportFallbackQuote(currentUser.companyId(), demandId, demand);
        }
        StoredTemplate template = template(demand.sourceFileId());
        Path sourcePath = Path.of(template.storagePath()).toAbsolutePath().normalize();
        if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            return exportFallbackQuote(currentUser.companyId(), demandId, demand);
        }
        try (InputStream inputStream = Files.newInputStream(sourcePath);
             Workbook workbook = WorkbookFactory.create(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            int quoteColumnIndex = findQuotationPriceColumn(sheet, demand.headerRowIndex());
            for (MaterialDemandItemResponse item : materialDemandRepository.items(currentUser.companyId(), demandId)) {
                Integer sourceRowNumber = item.sourceRowNumber() == null ? item.sourceRowNo() : item.sourceRowNumber();
                if (item.actualQuotePrice() == null || sourceRowNumber == null) {
                    continue;
                }
                Row row = sheet.getRow(Math.max(sourceRowNumber - 1, 0));
                if (row == null) {
                    row = sheet.createRow(Math.max(sourceRowNumber - 1, 0));
                }
                Cell cell = row.getCell(quoteColumnIndex);
                if (cell == null) {
                    cell = row.createCell(quoteColumnIndex, CellType.NUMERIC);
                }
                setQuoteCell(cell, item.actualQuotePrice());
            }
            workbook.setForceFormulaRecalculation(true);
            workbook.write(outputStream);
            byte[] bytes = outputStream.toByteArray();
            String fileName = safeFileName(demand.demandNo()) + "-quotation.xlsx";
            return new MaterialQuoteExportFile(
                new ByteArrayResource(bytes),
                fileName,
                bytes.length
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "QUOTE_EXPORT_FAILED", exception);
        }
    }

    private MaterialQuoteExportFile exportFallbackQuote(Long companyId, Long demandId, MaterialDemandSummaryResponse demand) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Quotation");
            String[] headers = {
                "No.",
                "IMPA/CN Code",
                "Description",
                "Specification",
                "Quantity",
                "Unit",
                "Candidate Code",
                "Candidate Name",
                "Candidate Spec",
                "Candidate Unit",
                "Unit Price",
                "Quotation Price"
            };
            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index, CellType.STRING).setCellValue(headers[index]);
            }
            List<MaterialDemandItemResponse> items = materialDemandRepository.items(companyId, demandId);
            for (int index = 0; index < items.size(); index++) {
                MaterialDemandItemResponse item = items.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0, CellType.NUMERIC).setCellValue(index + 1);
                row.createCell(1, CellType.STRING).setCellValue(defaultText(item.impaCode(), item.selectedImpaCode(), item.candidateImpaCode()));
                row.createCell(2, CellType.STRING).setCellValue(defaultText(item.description(), item.rawNameSpec(), item.candidateNameEn(), item.candidateNameCn()));
                row.createCell(3, CellType.STRING).setCellValue(defaultText(item.sizeModel(), item.candidateSpec()));
                row.createCell(4, CellType.STRING).setCellValue(defaultText(item.quantity()));
                row.createCell(5, CellType.STRING).setCellValue(defaultText(item.unit()));
                row.createCell(6, CellType.STRING).setCellValue(defaultText(item.candidateImpaCode(), item.selectedImpaCode()));
                row.createCell(7, CellType.STRING).setCellValue(defaultText(item.candidateNameCn(), item.candidateNameEn()));
                row.createCell(8, CellType.STRING).setCellValue(defaultText(item.candidateSpec()));
                row.createCell(9, CellType.STRING).setCellValue(defaultText(item.quoteSelectedUnit(), item.unit()));
                setOptionalDecimal(row.createCell(10, CellType.NUMERIC), item.quoteUnitPrice());
                setOptionalDecimal(row.createCell(11, CellType.NUMERIC), item.actualQuotePrice());
            }
            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(outputStream);
            byte[] bytes = outputStream.toByteArray();
            String fileName = safeFileName(demand.demandNo()) + "-quotation.xlsx";
            return new MaterialQuoteExportFile(new ByteArrayResource(bytes), fileName, bytes.length);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "QUOTE_EXPORT_FAILED", exception);
        }
    }

    public MaterialComparisonQuoteImportResponse importQuote(String authorizationHeader, Long demandId, MultipartFile file) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        materialDemandRepository.findSummaryById(currentUser.companyId(), demandId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MATERIAL_DEMAND_NOT_FOUND"));
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QUOTE_IMPORT_FILE_REQUIRED");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!originalName.endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ONLY_XLSX_SUPPORTED");
        }
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("material-quote-import-", ".xlsx");
            try (InputStream inputStream = file.getInputStream(); OutputStream outputStream = Files.newOutputStream(tempFile)) {
                inputStream.transferTo(outputStream);
            }
            MaterialParsedDocument document = xlsxMaterialQuoteParser.parse(tempFile);
            Map<Integer, String> quantityBySourceRow = new HashMap<>();
            for (MaterialQuoteRow row : document.rows()) {
                if (row.sourceRowNo() != 0) {
                    quantityBySourceRow.put(row.sourceRowNo(), row.quantity());
                }
            }
            List<MaterialComparisonQuoteImportItem> items = materialDemandRepository.items(currentUser.companyId(), demandId).stream()
                .map(item -> {
                    Integer sourceRowNumber = item.sourceRowNumber() == null ? item.sourceRowNo() : item.sourceRowNumber();
                    String quantity = sourceRowNumber == null ? null : quantityBySourceRow.get(sourceRowNumber);
                    if (quantity == null) {
                        return null;
                    }
                    return new MaterialComparisonQuoteImportItem(item.itemId(), sourceRowNumber, quantity);
                })
                .filter(item -> item != null)
                .toList();
            return new MaterialComparisonQuoteImportResponse(demandId, items.size(), items);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "QUOTE_IMPORT_FAILED", exception);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                    // Temporary file cleanup failure should not mask the import result.
                }
            }
        }
    }

    private StoredTemplate template(String fileId) {
        return jdbcTemplate.query(
            """
            SELECT file_id, original_name, storage_path
            FROM sys_file
            WHERE file_id = ? AND status = 'ACTIVE'
            LIMIT 1
            """,
            (rs, rowNum) -> new StoredTemplate(
                rs.getString("file_id"),
                rs.getString("original_name"),
                rs.getString("storage_path")
            ),
            fileId
        ).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOURCE_TEMPLATE_NOT_FOUND"));
    }

    private int findQuotationPriceColumn(Sheet sheet, Integer headerRowIndex) {
        int headerIndex = headerRowIndex == null || headerRowIndex <= 0 ? 0 : headerRowIndex - 1;
        int maxRow = Math.min(sheet.getLastRowNum(), headerIndex + 60);
        for (int rowIndex = headerIndex; rowIndex <= maxRow; rowIndex++) {
            Row headerRow = sheet.getRow(rowIndex);
            if (headerRow == null) {
                continue;
            }
            for (Cell cell : headerRow) {
                String normalized = normalize(cell.toString());
                if ("QUOTATIONPRICE".equals(normalized) || "QUOTEPRICE".equals(normalized)) {
                    return cell.getColumnIndex();
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "QUOTE_PRICE_COLUMN_NOT_FOUND");
    }

    private void setQuoteCell(Cell cell, BigDecimal value) {
        try {
            cell.setCellValue(value.doubleValue());
        } catch (RuntimeException exception) {
            cell.setCellValue(value.toPlainString());
        }
    }

    private void setOptionalDecimal(Cell cell, BigDecimal value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        setQuoteCell(cell, value);
    }

    private String defaultText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]+", "").toUpperCase(Locale.ROOT);
    }

    private String safeFileName(String value) {
        String text = value == null || value.isBlank() ? "material-quote" : value;
        return text.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private record StoredTemplate(String fileId, String originalName, String storagePath) {
    }
}

record MaterialQuoteExportFile(ByteArrayResource resource, String fileName, long fileSize) {
}
