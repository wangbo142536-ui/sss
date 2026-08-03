package com.zswy.shipsupply.procurement.food.domain;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class FoodOrderStatus {

    public static final String PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String PREPARING = "PREPARING";
    public static final String READY_TO_SHIP = "READY_TO_SHIP";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String WAITING_SUPPLY = "WAITING_SUPPLY";
    public static final String SUPPLYING = "SUPPLYING";
    public static final String SUPPLIED = "SUPPLIED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    private static final List<String> PROGRESS = List.of(
        PENDING_CONFIRMATION,
        CONFIRMED,
        PREPARING,
        READY_TO_SHIP,
        IN_TRANSIT,
        WAITING_SUPPLY,
        SUPPLYING,
        SUPPLIED
    );

    private FoodOrderStatus() {
    }

    public static void requireTransition(String current, String target) {
        if (REJECTED.equals(target) && PENDING_CONFIRMATION.equals(current)) {
            return;
        }
        if (CANCELLED.equals(target) && !SUPPLIED.equals(current)) {
            return;
        }
        if (PENDING_CONFIRMATION.equals(current) && PREPARING.equals(target)) {
            return;
        }
        if (CONFIRMED.equals(current) && READY_TO_SHIP.equals(target)) {
            return;
        }
        int currentIndex = PROGRESS.indexOf(current);
        int targetIndex = PROGRESS.indexOf(target);
        if (currentIndex < 0 || targetIndex != currentIndex + 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "FOOD_ORDER_INVALID_STATUS_TRANSITION");
        }
    }

    public static String aggregate(List<String> statuses) {
        if (statuses.isEmpty()) {
            return PENDING_CONFIRMATION;
        }
        int max = statuses.stream().mapToInt(PROGRESS::indexOf).max().orElse(0);
        if (max >= 0) {
            return PROGRESS.get(max);
        }
        return statuses.stream().allMatch(REJECTED::equals) ? REJECTED : PENDING_CONFIRMATION;
    }
}
