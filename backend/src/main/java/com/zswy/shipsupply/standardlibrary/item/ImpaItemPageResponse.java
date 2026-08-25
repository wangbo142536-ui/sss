package com.zswy.shipsupply.standardlibrary.item;

import java.util.List;

public record ImpaItemPageResponse(
    List<ImpaItemResponse> items,
    long total,
    int page,
    int pageSize,
    int totalPages
) {
}
