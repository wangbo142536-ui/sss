package com.zswy.shipsupply.shop.intelligent;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/shop/skus/intelligent-imports")
public class IntelligentImportController {

    private final IntelligentImportService service;

    public IntelligentImportController(IntelligentImportService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IntelligentImportStartResponse start(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam("file") MultipartFile file
    ) {
        return service.start(authorization, file);
    }

    @GetMapping("/{jobId}")
    public IntelligentImportJobResponse status(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable String jobId
    ) {
        return service.status(authorization, jobId);
    }

    @PostMapping("/{jobId}/execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IntelligentImportStartResponse execute(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable String jobId,
        @RequestBody(required = false) IntelligentImportExecutionRequest request
    ) {
        return service.execute(authorization, jobId, request);
    }
}
