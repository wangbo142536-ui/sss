package com.zswy.shipsupply.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/suppliers/{companyId}/qualifications/{qualificationId}/file")
public class SupplierQualificationFileController {

    private final FileStorageService fileStorageService;

    public SupplierQualificationFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<Resource> download(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable long companyId,
        @PathVariable long qualificationId
    ) {
        FileDownloadResponse file = fileStorageService.downloadPublicSupplierQualification(
            authorizationHeader,
            companyId,
            qualificationId
        );
        String encodedName = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.contentType()))
            .contentLength(file.fileSize())
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + file.fileName().replace("\"", "") + "\"; filename*=UTF-8''" + encodedName
            )
            .body(file.resource());
    }
}
