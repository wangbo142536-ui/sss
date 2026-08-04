package com.zswy.shipsupply.customs.api;

import com.zswy.shipsupply.customs.application.CustomsDeclarationService;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationContext;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationPage;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationRecord;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclareRequest;
import java.time.LocalDate;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customs/declarations")
public class CustomsDeclarationController {
    private final CustomsDeclarationService service;

    public CustomsDeclarationController(CustomsDeclarationService service) {
        this.service = service;
    }

    @GetMapping("/context")
    public DeclarationContext context(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam String businessType,
        @RequestParam Long purchaseOrderId
    ) {
        return service.context(authorization, businessType, purchaseOrderId);
    }

    @PostMapping
    public DeclarationRecord declare(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody DeclareRequest request
    ) {
        return service.declare(authorization, request);
    }

    @GetMapping
    public DeclarationPage list(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) LocalDate deliveryDate,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(authorization, keyword, status, deliveryDate, page, size);
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> download(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        var file = service.download(authorization, id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(file.fileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
            .body(file.resource());
    }
}
