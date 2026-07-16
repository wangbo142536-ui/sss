package com.zswy.shipsupply.transport;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

class TrafficServiceMarketplaceControllerContractTest {

    @Test
    void exposesTrafficMarketplaceEndpoints() {
        assertThat(Arrays.stream(TrafficServiceController.class.getDeclaredMethods()).flatMap(this::paths))
            .containsAll(List.of(
                "/api/traffic/service-requests",
                "/api/traffic/service-requests/{requestId}",
                "/api/traffic/service-requests/{requestId}/publish",
                "/api/traffic/service-requests/{requestId}/cancel",
                "/api/traffic/service-requests/{requestId}/select-quote",
                "/api/supplier/traffic-service-requests",
                "/api/supplier/traffic-service-requests/{requestId}/quotes",
                "/api/supplier/traffic-service-quotes/{quoteId}/withdraw",
                "/api/supplier/traffic-shuttles",
                "/api/supplier/traffic-shuttles/{shuttleId}",
                "/api/supplier/traffic-shuttles/{shuttleId}/close",
                "/api/supplier/traffic-shuttles/{shuttleId}/bookings/{bookingId}/execution",
                "/api/traffic/shuttles",
                "/api/traffic/shuttles/{shuttleId}",
                "/api/traffic/shuttles/{shuttleId}/book"
            ));
    }

    private Stream<String> paths(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return Arrays.stream(method.getAnnotation(GetMapping.class).value());
        if (method.isAnnotationPresent(PostMapping.class)) return Arrays.stream(method.getAnnotation(PostMapping.class).value());
        if (method.isAnnotationPresent(PutMapping.class)) return Arrays.stream(method.getAnnotation(PutMapping.class).value());
        if (method.isAnnotationPresent(DeleteMapping.class)) return Arrays.stream(method.getAnnotation(DeleteMapping.class).value());
        return Stream.empty();
    }
}
