package com.zswy.shipsupply.transport;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class TrafficServiceMarketplaceRepositoryContractTest {

    @Test
    void exposesTrafficMarketplaceRepositoryOperations() throws Exception {
        assertThat(Arrays.stream(TrafficServiceRepository.class.getDeclaredMethods()).map(Method::getName))
            .contains(
                "createRequest",
                "listRequests",
                "listSupplierRequests",
                "getRequestDetail",
                "publishRequest",
                "cancelRequest",
                "submitQuote",
                "withdrawQuote",
                "selectQuote",
                "createShuttle",
                "listShuttles",
                "getShuttle",
                "bookShuttle"
                , "updateBookingExecution"
            );
        String source = Files.readString(Path.of(
            "src/main/java/com/zswy/shipsupply/transport/TrafficServiceRepository.java"
        ));
        assertThat(source).contains("booking.shuttle_service_id = ?");
        assertThat(source).contains("shuttle.supplier_company_id = ?");
    }
}
