package com.zswy.shipsupply.procurement.food.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandDetail;
import com.zswy.shipsupply.procurement.food.api.FoodProcurementDtos.DemandSummary;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodComparisonQuoteRepository;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodComparisonQuoteRepository.ComparisonQuoteRow;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodComparisonQuoteRepository.ComparisonQuoteUpdate;
import com.zswy.shipsupply.procurement.food.infrastructure.persistence.FoodDemandRepository;

@ExtendWith(MockitoExtension.class)
class FoodComparisonSpreadsheetServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private FoodDemandRepository demandRepository;
    @Mock private FoodComparisonQuoteRepository comparisonQuoteRepository;

    private FoodComparisonSpreadsheetService service;

    @BeforeEach
    void setUp() {
        service = new FoodComparisonSpreadsheetService(currentUserService, demandRepository, comparisonQuoteRepository);
        when(currentUserService.requireActiveCompanyUser("Bearer token"))
            .thenReturn(new CurrentUserContext(7L, 11L, "ACTIVE", "ACTIVE"));
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demand());
    }

    @Test
    void exportsSelectedComparisonRowsAsReusableWorkbook() throws Exception {
        when(comparisonQuoteRepository.rows(55L, 11L, List.of(91L))).thenReturn(List.of(row()));

        var file = service.exportComparison("Bearer token", 55L, List.of(91L));

        assertThat(file.fileName()).isEqualTo("FOOD20260718001-comparison.xlsx");
        assertThat(file.fileSize()).isPositive();
        try (var workbook = WorkbookFactory.create(file.resource().getInputStream())) {
            var sheet = workbook.getSheet("Food Comparison");
            assertThat(sheet).isNotNull();
            assertThat(sheet.isColumnHidden(0)).isTrue();
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("土豆");
            assertThat(sheet.getRow(1).getCell(10).getNumericCellValue()).isEqualTo(3.25d);
            assertThat(sheet.getRow(1).getCell(11).getCellFormula()).isEqualTo("I2*K2");
        }
    }

    @Test
    void importsQuotedQuantityPriceAndRemarkAsComparisonOverride() throws Exception {
        MockMultipartFile file = workbookFile();
        when(comparisonQuoteRepository.rows(55L, 11L, List.of(91L))).thenReturn(List.of(row()));

        var response = service.importComparison("Bearer token", 55L, file);

        assertThat(response.updatedCount()).isEqualTo(1);
        ArgumentCaptor<List<ComparisonQuoteUpdate>> updates = ArgumentCaptor.forClass(List.class);
        verify(comparisonQuoteRepository).saveOverrides(eq(55L), eq(7L), updates.capture());
        assertThat(updates.getValue()).hasSize(1);
        ComparisonQuoteUpdate update = updates.getValue().get(0);
        assertThat(update.quoteItemId()).isEqualTo(91L);
        assertThat(update.demandItemId()).isEqualTo(501L);
        assertThat(update.requestedQuantity()).isEqualByComparingTo("120");
        assertThat(update.quotedQuantity()).isEqualByComparingTo("100");
        assertThat(update.unitPrice()).isEqualByComparingTo("4.50");
        assertThat(update.remark()).isEqualTo("人工校价");
    }

    @Test
    void importsTheWorkbookProducedByComparisonExport() throws Exception {
        when(comparisonQuoteRepository.rows(55L, 11L, List.of(91L))).thenReturn(List.of(row()));
        var exported = service.exportComparison("Bearer token", 55L, List.of(91L));
        var uploaded = new MockMultipartFile(
            "file", exported.fileName(),
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            exported.resource().getInputStream()
        );

        var response = service.importComparison("Bearer token", 55L, uploaded);

        assertThat(response.updatedCount()).isEqualTo(1);
        ArgumentCaptor<List<ComparisonQuoteUpdate>> updates = ArgumentCaptor.forClass(List.class);
        verify(comparisonQuoteRepository).saveOverrides(eq(55L), eq(7L), updates.capture());
        assertThat(updates.getValue()).singleElement().satisfies(update -> {
            assertThat(update.requestedQuantity()).isEqualByComparingTo("100");
            assertThat(update.quotedQuantity()).isEqualByComparingTo("100");
            assertThat(update.unitPrice()).isEqualByComparingTo("3.25");
        });
    }

    @Test
    void rejectsComparisonImportAfterOrderCreation() throws Exception {
        when(demandRepository.getDemand(55L, 11L)).thenReturn(demand("ORDERED"));

        assertThatThrownBy(() -> service.importComparison("Bearer token", 55L, workbookFile()))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .hasMessageContaining("FOOD_COMPARISON_LOCKED");
    }

    private MockMultipartFile workbookFile() throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Food Comparison");
            var header = sheet.createRow(0);
            String[] headers = { "Quote Item ID", "Demand Item ID", "No.", "English Name", "Chinese Name", "Remark", "Specification", "Unit", "Requested Quantity", "Quoted Quantity", "Unit Price", "Amount" };
            for (int index = 0; index < headers.length; index++) header.createCell(index, CellType.STRING).setCellValue(headers[index]);
            var row = sheet.createRow(1);
            row.createCell(0, CellType.NUMERIC).setCellValue(91);
            row.createCell(1, CellType.NUMERIC).setCellValue(501);
            row.createCell(5, CellType.STRING).setCellValue("人工校价");
            row.createCell(8, CellType.NUMERIC).setCellValue(120);
            row.createCell(9, CellType.NUMERIC).setCellValue(100);
            row.createCell(10, CellType.NUMERIC).setCellValue(4.50);
            workbook.write(output);
            return new MockMultipartFile("file", "comparison.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private ComparisonQuoteRow row() {
        return new ComparisonQuoteRow(
            91L, 501L, 1, "土豆", "POTATO", "L", "KG", new BigDecimal("100"), "供货商A",
            new BigDecimal("100"), new BigDecimal("3.25"), null
        );
    }

    private DemandDetail demand() {
        return demand("QUOTED");
    }

    private DemandDetail demand(String status) {
        return new DemandDetail(
            new DemandSummary(
                55L, "FD-55", "FOOD20260718001", "NEW AMBER", "舟山", LocalDateTime.now(), "USD",
                status, 1, 1, 0, 1, 1, LocalDateTime.now(), LocalDateTime.now().plusDays(1), LocalDateTime.now()
            ),
            "food.xlsx", "Quotation", List.of()
        );
    }
}
