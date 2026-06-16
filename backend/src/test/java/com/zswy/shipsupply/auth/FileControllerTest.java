package com.zswy.shipsupply.auth;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    void downloadsQualificationFileWithHeaders() throws Exception {
        when(fileStorageService.download("Bearer admin-token", "FILE-1")).thenReturn(new FileDownloadResponse(
            new ByteArrayResource("hello".getBytes(StandardCharsets.UTF_8)),
            "license.pdf",
            "application/pdf",
            5L
        ));

        mockMvc.perform(get("/api/files/FILE-1").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("license.pdf")));
    }
}
