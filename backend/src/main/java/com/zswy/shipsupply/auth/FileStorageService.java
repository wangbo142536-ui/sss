package com.zswy.shipsupply.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileStorageService {

    private final AuthRepository authRepository;
    private final TokenService tokenService;

    public FileStorageService(AuthRepository authRepository, TokenService tokenService) {
        this.authRepository = authRepository;
        this.tokenService = tokenService;
    }

    public FileUploadResponse upload(String authorizationHeader, MultipartFile file) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        return storeForUser(userId, file);
    }

    FileUploadResponse storeForUser(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        Path target = null;
        try {
            Path uploadDir = Path.of("uploads", "qualifications").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            String originalName = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
            String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
            target = Files.createTempFile(uploadDir, "qualification-", "-" + safeName);
            file.transferTo(target);
            String fileId = authRepository.insertFile(
                userId,
                originalName,
                target.toString(),
                file.getContentType(),
                file.getSize()
            );
            registerRollbackCleanup(target);
            return new FileUploadResponse(fileId, originalName, file.getContentType(), file.getSize(), "/api/files/" + fileId);
        } catch (IOException exception) {
            deletePhysicalFile(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed", exception);
        } catch (RuntimeException exception) {
            deletePhysicalFile(target);
            throw exception;
        }
    }

    private void registerRollbackCleanup(Path target) {
        if (target == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deletePhysicalFile(target);
                }
            }
        });
    }

    private void deletePhysicalFile(Path target) {
        if (target == null) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Database rollback remains authoritative; an inaccessible orphan can be cleaned operationally.
        }
    }

    public FileDownloadResponse download(String authorizationHeader, String fileId) {
        Long userId = tokenService.requireUserId(authorizationHeader);
        AuthenticatedUser user = authRepository.getUserById(userId);
        StoredFileResponse file = authRepository.findStoredFile(fileId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (!canAccess(user, file)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File access denied");
        }
        Path path = Path.of(file.storagePath()).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File content not found");
        }
        String contentType = file.contentType() == null || file.contentType().isBlank()
            ? "application/octet-stream"
            : file.contentType();
        return new FileDownloadResponse(
            new FileSystemResource(path),
            file.fileName(),
            contentType,
            file.fileSize()
        );
    }

    private boolean canAccess(AuthenticatedUser user, StoredFileResponse file) {
        var roles = authRepository.rolesForUser(user.id());
        boolean isAdmin = roles.stream().anyMatch(role -> "PLATFORM_ADMIN".equals(role.roleCode()));
        if (isAdmin) {
            return true;
        }
        if (Objects.equals(file.uploaderUserId(), user.id())) {
            return true;
        }
        if (file.qualificationCompanyId() != null && Objects.equals(file.qualificationCompanyId(), user.companyId())) {
            return true;
        }
        boolean regulatory = roles.stream().anyMatch(role ->
            role.roleCode() != null && role.roleCode().toUpperCase().contains("REGULATORY"));
        return authRepository.hasBusinessFileAccess(file.fileId(), user.companyId(), regulatory);
    }
}
