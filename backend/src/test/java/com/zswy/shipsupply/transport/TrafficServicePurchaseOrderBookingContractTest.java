package com.zswy.shipsupply.transport;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrafficServicePurchaseOrderBookingContractTest {

    @Test
    void purchaseOrderResponseExposesRealShuttleBookingFields() {
        assertThat(componentNames(TrafficServiceOrderResponse.class)).contains(
            "bookingId",
            "shuttleServiceId",
            "shuttleNo",
            "shuttleDeparturePoint",
            "shuttleDestinationPoint",
            "shuttleStartTime",
            "shuttleReturnTime",
            "shuttleServiceNodes",
            "bookingNodeIndex",
            "bookingNodeName",
            "bookingNodeTime",
            "bookingAmount",
            "bookingFreightFee",
            "bookingCustomsFee",
            "bookingCraneFee"
        );
    }

    @Test
    void bookingPayloadSupportsExplicitDemandAndPurchaseOrderLinks() {
        assertThat(componentNames(TrafficShuttleBookingPayload.class))
            .contains("demandId", "purchaseOrderId");
    }

    @Test
    void purchaseOrderLookupUsesExplicitBookingLinksAndSafeDemandFallback() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/zswy/shipsupply/transport/TrafficServiceRepository.java"
        ));

        assertThat(source)
            .contains("$.bookingId")
            .contains("$.trafficServiceOrderId")
            .contains("booking.request_id = purchase.demand_id")
            .contains("other_purchase.demand_id = purchase.demand_id")
            .contains("JOIN traffic_service_order service_order ON service_order.id = booking.traffic_service_order_id")
            .doesNotContain("booking.request_no LIKE");
    }

    @Test
    void bookingUpdateDoesNotReuseTrafficOrderFromAnotherPurchaseSource() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/zswy/shipsupply/transport/TrafficServiceRepository.java"
        ));

        assertThat(source)
            .contains("trafficOrderMatchesSource(requesterCompanyId, existingOrderId, source)")
            .contains("syncTrafficOrderSource(requesterCompanyId, orderId, source)")
            .contains("createOrderFromShuttle(requesterCompanyId, userId, service, payload, source)")
            .contains("purchase_order_id = ? OR purchase_order_id IS NULL")
            .contains("demand_id = ? OR demand_id IS NULL");
    }

    @Test
    void mapsBookingAndShuttleColumnsFromTheRealBookingRow() throws Exception {
        TrafficServiceRepository repository = new TrafficServiceRepository(mock(JdbcTemplate.class));
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(901L);
        when(rs.getObject("resolved_booking_id")).thenReturn(801L);
        when(rs.getLong("resolved_booking_id")).thenReturn(801L);
        when(rs.getObject("resolved_shuttle_service_id")).thenReturn(701L);
        when(rs.getLong("resolved_shuttle_service_id")).thenReturn(701L);
        when(rs.getString("resolved_shuttle_no")).thenReturn("SH-701");
        when(rs.getString("resolved_shuttle_departure_point")).thenReturn("West Pier");
        when(rs.getString("resolved_shuttle_destination_point")).thenReturn("A1 Anchorage");
        when(rs.getTimestamp("resolved_shuttle_start_time")).thenReturn(Timestamp.valueOf("2026-07-12 08:00:00"));
        when(rs.getTimestamp("resolved_shuttle_return_time")).thenReturn(Timestamp.valueOf("2026-07-12 18:00:00"));
        when(rs.getString("resolved_shuttle_service_nodes_json"))
            .thenReturn("[{\"nodeName\":\"Node A\",\"startTime\":\"08:00\",\"endTime\":\"10:00\"}]");
        when(rs.getObject("resolved_booking_node_index")).thenReturn(2);
        when(rs.getString("resolved_booking_node_name")).thenReturn("Node A");
        when(rs.getString("resolved_booking_node_time")).thenReturn("08:00-10:00");
        when(rs.getBigDecimal("resolved_booking_amount")).thenReturn(new BigDecimal("1800.00"));
        when(rs.getBigDecimal("resolved_booking_freight_fee")).thenReturn(new BigDecimal("1200.00"));
        when(rs.getBigDecimal("resolved_booking_customs_fee")).thenReturn(new BigDecimal("300.00"));
        when(rs.getBigDecimal("resolved_booking_crane_fee")).thenReturn(new BigDecimal("300.00"));

        Method mapper = TrafficServiceRepository.class.getDeclaredMethod(
            "order", ResultSet.class, boolean.class, Long.class, boolean.class
        );
        mapper.setAccessible(true);
        TrafficServiceOrderResponse response = (TrafficServiceOrderResponse) mapper.invoke(
            repository, rs, false, 501L, true
        );

        assertThat(response.purchaseOrderId()).isEqualTo(501L);
        assertThat(response.bookingId()).isEqualTo(801L);
        assertThat(response.shuttleServiceId()).isEqualTo(701L);
        assertThat(response.shuttleNo()).isEqualTo("SH-701");
        assertThat(response.shuttleDeparturePoint()).isEqualTo("West Pier");
        assertThat(response.shuttleDestinationPoint()).isEqualTo("A1 Anchorage");
        assertThat(response.shuttleServiceNodes()).extracting(TrafficShuttleNodePayload::nodeName)
            .containsExactly("Node A");
        assertThat(response.bookingNodeIndex()).isEqualTo(2);
        assertThat(response.bookingAmount()).isEqualByComparingTo("1800.00");
        assertThat(response.bookingFreightFee()).isEqualByComparingTo("1200.00");
        assertThat(response.bookingCustomsFee()).isEqualByComparingTo("300.00");
        assertThat(response.bookingCraneFee()).isEqualByComparingTo("300.00");
    }

    private String[] componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
            .map(RecordComponent::getName)
            .toArray(String[]::new);
    }
}
