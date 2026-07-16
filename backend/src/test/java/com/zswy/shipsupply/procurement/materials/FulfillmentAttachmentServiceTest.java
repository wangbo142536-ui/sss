package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class FulfillmentAttachmentServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private FulfillmentAttachmentRepository repository;

    @Test
    void buyerReadsAllOrderAttachments() {
        FulfillmentAttachmentService service = new FulfillmentAttachmentService(currentUserService, repository);
        FulfillmentAttachmentResponse attachment = org.mockito.Mockito.mock(FulfillmentAttachmentResponse.class);
        when(currentUserService.requireActiveCompanyUser("Bearer buyer"))
            .thenReturn(new CurrentUserContext(10L, 22L, "ACTIVE", "ACTIVE"));
        when(repository.buyerOwnsOrder(22L, 501L)).thenReturn(true);
        when(repository.listForPurchaseOrder(501L)).thenReturn(List.of(attachment));

        assertThat(service.listForOrder("Bearer buyer", 501L).items()).containsExactly(attachment);
        verify(repository).listForPurchaseOrder(501L);
    }

    @Test
    void supplierReadsOnlyOwnProviderAttachments() {
        FulfillmentAttachmentService service = new FulfillmentAttachmentService(currentUserService, repository);
        FulfillmentAttachmentResponse attachment = org.mockito.Mockito.mock(FulfillmentAttachmentResponse.class);
        when(currentUserService.requireActiveCompanyUser("Bearer supplier"))
            .thenReturn(new CurrentUserContext(10L, 24L, "ACTIVE", "ACTIVE"));
        when(repository.supplierHasOrder(24L, 501L)).thenReturn(true);
        when(repository.listProviderForPurchaseOrder(24L, 501L)).thenReturn(List.of(attachment));

        assertThat(service.listForOrder("Bearer supplier", 501L).items()).containsExactly(attachment);
        verify(repository).listProviderForPurchaseOrder(24L, 501L);
    }

    @Test
    void bargeReadsOnlyAttachmentsForItsLinkedOrder() {
        FulfillmentAttachmentService service = new FulfillmentAttachmentService(currentUserService, repository);
        FulfillmentAttachmentResponse attachment = org.mockito.Mockito.mock(FulfillmentAttachmentResponse.class);
        when(currentUserService.requireActiveCompanyUser("Bearer barge"))
            .thenReturn(new CurrentUserContext(10L, 35L, "ACTIVE", "ACTIVE"));
        when(repository.bargeHasOrder(35L, 501L)).thenReturn(true);
        when(repository.listProviderForPurchaseOrder(35L, 501L)).thenReturn(List.of(attachment));

        assertThat(service.listForOrder("Bearer barge", 501L).items()).containsExactly(attachment);
        verify(repository).listProviderForPurchaseOrder(35L, 501L);
    }

    @Test
    void unrelatedCompanyCannotReadOrderAttachments() {
        FulfillmentAttachmentService service = new FulfillmentAttachmentService(currentUserService, repository);
        when(currentUserService.requireActiveCompanyUser("Bearer stranger"))
            .thenReturn(new CurrentUserContext(10L, 99L, "ACTIVE", "ACTIVE"));

        assertThatThrownBy(() -> service.listForOrder("Bearer stranger", 501L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("PURCHASE_ORDER_NOT_FOUND");
    }

    @Test
    void savesMultipleSupplierAttachmentsAndReturnsAllOrderAttachments() {
        FulfillmentAttachmentService service = new FulfillmentAttachmentService(currentUserService, repository);
        when(currentUserService.requireActiveCompanyUser("Bearer supplier"))
            .thenReturn(new CurrentUserContext(10L, 24L, "ACTIVE", "ACTIVE"));
        when(repository.supplierOwnsOrder(24L, 501L, 601L)).thenReturn(true);
        FileSnapshot first = new FileSnapshot("F1", "one.jpg", "/api/files/F1");
        FileSnapshot second = new FileSnapshot("F2", "two.jpg", "/api/files/F2");
        FulfillmentAttachmentResponse saved = org.mockito.Mockito.mock(FulfillmentAttachmentResponse.class);
        when(repository.listForPurchaseOrder(501L)).thenReturn(List.of(saved));

        FulfillmentAttachmentListResponse response = service.saveSupplier(
            "Bearer supplier", 501L, 601L,
            new FulfillmentAttachmentCreateRequest(null, null, null, null, null, null, List.of(first, second))
        );

        assertThat(response.items()).containsExactly(saved);
        verify(repository).saveSupplier(24L, 10L, 501L, 601L, first);
        verify(repository).saveSupplier(24L, 10L, 501L, 601L, second);
        verify(repository).listForPurchaseOrder(501L);
    }

    @Test
    void savesMultipleNodeAttachmentsForRequestedTrafficOrder() {
        FulfillmentAttachmentService service = new FulfillmentAttachmentService(currentUserService, repository);
        when(currentUserService.requireActiveCompanyUser("Bearer barge"))
            .thenReturn(new CurrentUserContext(10L, 35L, "ACTIVE", "ACTIVE"));
        when(repository.shuttleOwnedBy(35L, 701L)).thenReturn(true);
        FileSnapshot first = new FileSnapshot("F1", "one.jpg", "/api/files/F1");
        FileSnapshot second = new FileSnapshot("F2", "two.jpg", "/api/files/F2");
        when(repository.saveShuttle(35L, 10L, 701L, 2, "Anchorage", 801L, 901L, first)).thenReturn(List.of());
        when(repository.saveShuttle(35L, 10L, 701L, 2, "Anchorage", 801L, 901L, second)).thenReturn(List.of());

        FulfillmentAttachmentListResponse response = service.saveShuttleNode(
            "Bearer barge", 701L, 2,
            new FulfillmentAttachmentCreateRequest(null, null, null, "Anchorage", 801L, 901L, List.of(first, second))
        );

        assertThat(response.items()).isEmpty();
        verify(repository, times(1)).saveShuttle(35L, 10L, 701L, 2, "Anchorage", 801L, 901L, first);
        verify(repository, times(1)).saveShuttle(35L, 10L, 701L, 2, "Anchorage", 801L, 901L, second);
    }
}
