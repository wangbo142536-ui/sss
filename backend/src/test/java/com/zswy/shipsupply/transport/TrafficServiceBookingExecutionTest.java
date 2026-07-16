package com.zswy.shipsupply.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class TrafficServiceBookingExecutionTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private TrafficServiceRepository repository;

    private TrafficServiceService service;

    @BeforeEach
    void setUp() {
        service = new TrafficServiceService(currentUserService, repository);
        when(currentUserService.requireActiveCompanyUser("Bearer barge"))
            .thenReturn(new CurrentUserContext(10L, 35L, "ACTIVE", "ACTIVE"));
    }

    @Test
    void updatesBookingExecutionForOwningBargeCompany() {
        TrafficShuttleBookingExecutionPayload payload = payload();
        TrafficShuttleBookingResponse response = mock(TrafficShuttleBookingResponse.class);
        when(repository.updateBookingExecution(35L, 701L, 801L, payload)).thenReturn(Optional.of(response));

        TrafficShuttleBookingResponse actual = service.updateBookingExecution(
            "Bearer barge", 701L, 801L, payload
        );

        assertThat(actual).isSameAs(response);
        verify(repository).updateBookingExecution(35L, 701L, 801L, payload);
    }

    @Test
    void hidesBookingWhenBargeCompanyDoesNotOwnShuttle() {
        TrafficShuttleBookingExecutionPayload payload = payload();
        when(repository.updateBookingExecution(35L, 701L, 801L, payload)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBookingExecution("Bearer barge", 701L, 801L, payload))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("TRAFFIC_SHUTTLE_BOOKING_NOT_FOUND");
    }

    private TrafficShuttleBookingExecutionPayload payload() {
        return new TrafficShuttleBookingExecutionPayload(
            2, "Anchorage", "2026-07-11T10:00", "Barge 1", "IMO-1", "2026-07-11T10:00", "30.1,122.2",
            new BigDecimal("122.2000000"), new BigDecimal("30.1000000"), "12",
            2, "Stores", new BigDecimal("1200.00"), new BigDecimal("8.500"),
            true, true, true, 2, "Operator", "13800000000", "Arrived"
        );
    }
}
