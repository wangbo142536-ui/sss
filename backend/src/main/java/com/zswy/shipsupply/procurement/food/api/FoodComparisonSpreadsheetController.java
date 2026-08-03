package com.zswy.shipsupply.procurement.food.api;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zswy.shipsupply.procurement.food.application.FoodComparisonSpreadsheetService;
import com.zswy.shipsupply.procurement.food.application.FoodComparisonSpreadsheetService.ComparisonImportResponse;
import com.zswy.shipsupply.procurement.food.application.FoodComparisonSpreadsheetService.ComparisonItemsRequest;

@RestController
@RequestMapping("/api/procurement/food/demands/{demandId}")
public class FoodComparisonSpreadsheetController {

    private final FoodComparisonSpreadsheetService service;

    public FoodComparisonSpreadsheetController(FoodComparisonSpreadsheetService service) {
        this.service = service;
    }

    @GetMapping("/comparison-export")
    public ResponseEntity<?> exportComparison(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long demandId,
        @RequestParam("quoteItemIds") List<Long> quoteItemIds
    ) {
        var file = service.exportComparison(authorizationHeader, demandId, quoteItemIds);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .contentLength(file.fileSize())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
            .body(file.resource());
    }

    @PostMapping(value = "/comparison-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComparisonImportResponse importComparison(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long demandId,
        @RequestParam("file") MultipartFile file
    ) {
        return service.importComparison(authorizationHeader, demandId, file);
    }

    @PutMapping("/comparison-items")
    public ComparisonImportResponse saveComparisonItems(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long demandId,
        @RequestBody ComparisonItemsRequest request
    ) {
        return service.saveComparisonItems(authorizationHeader, demandId, request);
    }
}
