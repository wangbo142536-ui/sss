package com.zswy.shipsupply.procurement.materials;

import java.util.Map;

public record MaterialHeaderContext(
    String inquiryNo,
    String requestNo,
    String vesselName,
    String materialType,
    String currency,
    String suggestedPort,
    String eta,
    String recipientCompany,
    String handlerName,
    String handlerEmail,
    Map<String, String> rawHeaderFields
) {
    public static MaterialHeaderContext empty() {
        return new MaterialHeaderContext(null, null, null, null, null, null, null, null, null, null, Map.of());
    }
}
