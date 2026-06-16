package com.zswy.shipsupply.auth;

public record CurrentUserContext(
    Long userId,
    Long companyId,
    String userStatus,
    String companyStatus
) {
}
