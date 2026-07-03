package com.zswy.shipsupply.procurement.materials;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@Service
public class MaterialQuoteExportService {

    private final CurrentUserService currentUserService;
    private final MaterialDemandRepository materialDemandRepository;
    private final JdbcTemplate jdbcTemplate;

    public MaterialQuoteExportService(
        CurrentUserService currentUserService,
        MaterialDemandRepository materialDemandRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.currentUserService = currentUserService;
        this.materialDemandRepository = materialDemandRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public MaterialQuoteExportFile exportQuote(String authorizationHeader, Long demandId) {
        CurrentUserContext currentUser = currentUserService.requireActiveCompanyUser(authorizationHeader);
        MaterialDemandSummaryResponse demand = materialDemandRepository.findSummaryById(currentUser.companyId(), demandId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MATERIAL_DEMAND_NOT_FOUND"));
        if (demand.sourceFileId() == null || demand.sourceFileId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SOURCE_TEMPLATE_NOT_FOUND");
        }
        StoredTemplate template = template(demand.sourceFileId());
        Path sourcePath = Path.of(template.storagePath()).toAbsolutePath().normalize();
        if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SOURCE_TEMPLATE_FILE_NOT_FOUND");
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
