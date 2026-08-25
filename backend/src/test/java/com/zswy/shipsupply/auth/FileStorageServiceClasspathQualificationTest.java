package com.zswy.shipsupply.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class FileStorageServiceClasspathQualificationTest {

    @Test
    void publicSupplierQualificationCanBeReadFromPackagedClasspathResource() throws Exception {
        AuthRepository authRepository = mock(AuthRepository.class);
        TokenService tokenService = mock(TokenService.class);
        FileStorageService service = new FileStorageService(authRepository, tokenService);

        when(tokenService.requireUserId("Bearer active")).thenReturn(7L);
        when(authRepository.getUserById(7L)).thenReturn(new AuthenticatedUser(
            7L, "viewer", null, null, null, null, "SHIP_AGENT", "ACTIVE", 22L
        ));
        when(authRepository.findPublicSupplierQualificationFile(35L, 17L)).thenReturn(Optional.of(
            new StoredFileResponse(
                "INIT-SUPPLIER-REG-CERT-000035",
                "企业注册资质通用示例.png",
                "image/png",
                1_651_641L,
                "classpath:initialization/generic-enterprise-registration-qualification.png",
                null,
                35L
            )
        ));

        FileDownloadResponse response = service.downloadPublicSupplierQualification("Bearer active", 35L, 17L);

        assertThat(response.resource().exists()).isTrue();
        assertThat(response.resource().contentLength()).isEqualTo(1_651_641L);
        assertThat(response.contentType()).isEqualTo("image/png");
    }
}
