package com.zswy.shipsupply.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse upload(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestPart("file") MultipartFile file
    ) {
        return fileStorageService.upload(authorizationHeader, file);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable String fileId
    ) {
        FileDownloadResponse file = fileStorageService.download(authorizationHeader, fileId);
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
