package com.zswy.shipsupply.customs.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationItem;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclarationRecord;
import com.zswy.shipsupply.customs.api.CustomsDeclarationDtos.DeclareRequest;
import com.zswy.shipsupply.customs.infrastructure.CustomsDeclarationRepository;
import com.zswy.shipsupply.customs.infrastructure.CustomsDeclarationRepository.SourceOrder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CustomsDeclarationServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private CustomsDeclarationRepository repository;
    private CustomsDeclarationService service;

    @BeforeEach
    void setUp() {
        service = new CustomsDeclarationService(currentUserService, repository, "");
    }

    @Test
    void responsibleCompanyDeclaresOnceThroughRepositoryDeduplication() {
        SourceOrder source = source(20L);
        DeclarationRecord saved = record();
        when(currentUserService.requireActiveCompanyUser("token"))
            .thenReturn(new CurrentUserContext(9L, 20L, "ACTIVE", "ACTIVE"));
        when(repository.requireSource("MATERIAL", 7L)).thenReturn(source);
        when(repository.isPlatformAdmin(9L)).thenReturn(false);
        when(repository.find("MATERIAL", 7L)).thenReturn(Optional.of(saved));

        DeclarationRecord result = service.declare("token", new DeclareRequest("material", 7L));

        assertEquals(31L, result.id());
        verify(repository).insertIfAbsent(source, 9L);
    }

    @Test
    void unrelatedSupplierCannotDeclare() {
        SourceOrder source = source(20L);
        when(currentUserService.requireActiveCompanyUser("token"))
            .thenReturn(new CurrentUserContext(9L, 99L, "ACTIVE", "ACTIVE"));
        when(repository.requireSource("MATERIAL", 7L)).thenReturn(source);
        when(repository.isPlatformAdmin(9L)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.declare("token", new DeclareRequest("MATERIAL", 7L)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).insertIfAbsent(source, 9L);
    }

    @Test
    void responsibleSupplierContextContainsAllOrderItemsAndExistingStatus() {
        SourceOrder source = source(20L);
        DeclarationRecord saved = record();
        List<DeclarationItem> items = List.of(
            new DeclarationItem(1L, "150101", "床单", "1.37x2.3m", "200", "条"),
            new DeclarationItem(2L, "170103", "餐匙", "203mm", "30", "把")
        );
        when(currentUserService.requireActiveCompanyUser("token"))
            .thenReturn(new CurrentUserContext(9L, 20L, "ACTIVE", "ACTIVE"));
        when(repository.requireSource("MATERIAL", 7L)).thenReturn(source);
        when(repository.isPlatformAdmin(9L)).thenReturn(false);
        when(repository.find("MATERIAL", 7L)).thenReturn(Optional.of(saved));
        when(repository.listItems("MATERIAL", 7L)).thenReturn(items);

        var result = service.context("token", "MATERIAL", 7L);

        assertEquals(31L, result.declarationId());
        assertEquals("DECLARED", result.status());
        assertEquals(2, result.items().size());
    }

    private SourceOrder source(Long responsibleCompanyId) {
        return new SourceOrder("MATERIAL", 7L, "PO-007", 10L, responsibleCompanyId, "SUPPLIER",
            "凯珀供应", "NEW AMBER", "船代企业", "物料", "外贸", "船代企业",
            "张三", "13800000000", "2026-08-04 09:00", "2026-08-04 12:00", "马峙锚地",
            "交通艇A", "李四 / 13900000000", "王五", "13700000000", LocalDate.of(2026, 8, 4),
            new BigDecimal("200.00"));
    }

    private DeclarationRecord record() {
        return new DeclarationRecord(31L, "MATERIAL", 7L, "PO-007", 10L, 20L, "SUPPLIER",
            "凯珀供应", "DECLARED", "NEW AMBER", "船代企业", "物料", "外贸", "凯珀供应",
            "张三", "13800000000", "2026-08-04 09:00", "2026-08-04 12:00", "马峙锚地",
            "交通艇A", "李四 / 13900000000", "王五", "13700000000", LocalDate.of(2026, 8, 4),
            new BigDecimal("200.00"), LocalDateTime.of(2026, 8, 4, 9, 30));
    }
}
