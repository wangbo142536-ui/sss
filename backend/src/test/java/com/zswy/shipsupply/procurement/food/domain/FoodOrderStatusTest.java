package com.zswy.shipsupply.procurement.food.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FoodOrderStatusTest {

    @Test
    void acceptsEverySequentialExecutionTransition() {
        List<String> statuses = List.of(
            FoodOrderStatus.PENDING_CONFIRMATION,
            FoodOrderStatus.CONFIRMED,
            FoodOrderStatus.PREPARING,
            FoodOrderStatus.READY_TO_SHIP,
            FoodOrderStatus.IN_TRANSIT,
            FoodOrderStatus.WAITING_SUPPLY,
            FoodOrderStatus.SUPPLYING,
            FoodOrderStatus.SUPPLIED
        );
        for (int index = 0; index < statuses.size() - 1; index++) {
            String current = statuses.get(index);
            String target = statuses.get(index + 1);
            assertThatCode(() -> FoodOrderStatus.requireTransition(current, target)).doesNotThrowAnyException();
        }
    }

    @Test
    void acceptsVisualStageTransitionsUsedBySupplierActions() {
        assertThatCode(() -> FoodOrderStatus.requireTransition("PENDING_CONFIRMATION", "PREPARING"))
            .doesNotThrowAnyException();
        assertThatCode(() -> FoodOrderStatus.requireTransition("CONFIRMED", "READY_TO_SHIP"))
            .doesNotThrowAnyException();
    }

    @Test
    void onlyAllowsRejectionBeforeConfirmationAndRejectsUnrelatedStatusSkipping() {
        assertThatCode(() -> FoodOrderStatus.requireTransition("PENDING_CONFIRMATION", "REJECTED"))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> FoodOrderStatus.requireTransition("PREPARING", "IN_TRANSIT"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("FOOD_ORDER_INVALID_STATUS_TRANSITION");
        assertThatThrownBy(() -> FoodOrderStatus.requireTransition("PREPARING", "REJECTED"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void aggregateStatusFollowsFarthestSupplierProgress() {
        assertThat(FoodOrderStatus.aggregate(List.of("CONFIRMED", "IN_TRANSIT", "PREPARING")))
            .isEqualTo("IN_TRANSIT");
        assertThat(FoodOrderStatus.aggregate(List.of("REJECTED", "REJECTED"))).isEqualTo("REJECTED");
        assertThat(FoodOrderStatus.aggregate(List.of())).isEqualTo("PENDING_CONFIRMATION");
    }
}
