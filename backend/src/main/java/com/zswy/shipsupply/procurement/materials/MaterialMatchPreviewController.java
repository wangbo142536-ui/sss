package com.zswy.shipsupply.procurement.materials;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/procurement/materials")
public class MaterialMatchPreviewController {

    private final MaterialMatchPreviewService materialMatchPreviewService;

    public MaterialMatchPreviewController(MaterialMatchPreviewService materialMatchPreviewService) {
        this.materialMatchPreviewService = materialMatchPreviewService;
    }

    @PostMapping(value = "/match-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MaterialMatchPreviewResponse matchPreview(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestPart("file") MultipartFile file
    ) {
        return materialMatchPreviewService.matchPreview(authorizationHeader, file);
    }
}
