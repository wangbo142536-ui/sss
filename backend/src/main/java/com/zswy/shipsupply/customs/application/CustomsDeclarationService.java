package com.zswy.shipsupply.customs.application;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationContext;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationPage;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationRecord;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclareRequest;
import com.zswy.shipsupply.customs.infrastructure.CustomsDeclarationRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomsDeclarationService {
    private final CurrentUserService currentUserService;
    private final CustomsDeclarationRepository repository;
    private final String attachmentPath;

    public CustomsDeclarationService(
        CurrentUserService currentUserService,
        CustomsDeclarationRepository repository,
        @Value("${ship-supply.customs.default-attachment-path:}") String attachmentPath
    ) {
        this.currentUserService = currentUserService;
        this.repository = repository;
        this.attachmentPath = attachmentPath == null ? "" : attachmentPath.trim();
    }

    public DeclarationContext context(String authorization, String businessType, Long purchaseOrderId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorization);
        String type = normalizeType(businessType);
        var source = repository.requireSource(type, purchaseOrderId);
        boolean platformAdmin = repository.isPlatformAdmin(user.userId());
        if (!platformAdmin && !source.canView(user.companyId())) forbidden();
        var existing = repository.find(type, purchaseOrderId).orElse(null);
        return new DeclarationContext(
            type, purchaseOrderId, source.purchaseOrderNo(), source.responsibleType(),
            source.responsibleCompanyId(), source.responsibleCompanyName(),
            platformAdmin || source.responsibleCompanyId().equals(user.companyId()),
            existing == null ? null : existing.id(), existing == null ? "" : existing.status(),
            source.customsFee(), repository.listItems(type, purchaseOrderId)
        );
    }

    @Transactional
    public DeclarationRecord declare(String authorization, DeclareRequest request) {
        if (request == null || request.purchaseOrderId() == null) badRequest("PURCHASE_ORDER_ID_REQUIRED");
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorization);
        String type = normalizeType(request.businessType());
        var source = repository.requireSource(type, request.purchaseOrderId());
        boolean platformAdmin = repository.isPlatformAdmin(user.userId());
        if (!platformAdmin && !source.responsibleCompanyId().equals(user.companyId())) forbidden();
        repository.insertIfAbsent(source, user.userId());
        return repository.find(type, request.purchaseOrderId()).orElseThrow();
    }

    public DeclarationPage list(
        String authorization,
        String keyword,
        String status,
        LocalDate deliveryDate,
        int page,
        int size
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorization);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        return repository.list(
            user.companyId(), repository.isPlatformAdmin(user.userId()), keyword, normalizeStatus(status),
            deliveryDate, safePage, safeSize
        );
    }

    public DownloadFile download(String authorization, Long id) {
        if (id == null) badRequest("DECLARATION_ID_REQUIRED");
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorization);
        var row = repository.findById(id).orElseThrow(() -> notFound("CUSTOMS_DECLARATION_NOT_FOUND"));
        if (!repository.isPlatformAdmin(user.userId())
            && !row.buyerCompanyId().equals(user.companyId())
            && !row.responsibleCompanyId().equals(user.companyId())) {
            forbidden();
        }
        Path path = attachmentPath.isBlank() ? null : Path.of(attachmentPath).toAbsolutePath().normalize();
        if (path == null || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CUSTOMS_ATTACHMENT_NOT_CONFIGURED");
        }
        return new DownloadFile(new FileSystemResource(path), "附件一-凯珀物资上船通知单.pdf");
    }

    private String normalizeType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase();
        if (!"MATERIAL".equals(type) && !"FOOD".equals(type)) badRequest("BUSINESS_TYPE_INVALID");
        return type;
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) return "";
        String status = value.trim().toUpperCase();
        if (!"DECLARING".equals(status) && !"DECLARED".equals(status)) badRequest("CUSTOMS_STATUS_INVALID");
        return status;
    }

    private void badRequest(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private void forbidden() { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CUSTOMS_DECLARATION_FORBIDDEN"); }

    public record DownloadFile(Resource resource, String fileName) {}
}
