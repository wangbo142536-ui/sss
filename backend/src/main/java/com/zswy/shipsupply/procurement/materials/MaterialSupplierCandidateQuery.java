package com.zswy.shipsupply.procurement.materials;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

record MaterialSupplierCandidateQuery(
    Set<String> standardCodes,
    Set<String> supplierSkuCodes,
    Set<String> categoryCodes,
    List<String> keywords
) {

    MaterialSupplierCandidateQuery {
        standardCodes = copy(standardCodes);
        supplierSkuCodes = copy(supplierSkuCodes);
        categoryCodes = copy(categoryCodes);
        keywords = keywords == null ? List.of() : keywords.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .distinct()
            .limit(48)
            .toList();
    }

    static MaterialSupplierCandidateQuery empty() {
        return new MaterialSupplierCandidateQuery(Set.of(), Set.of(), Set.of(), List.of());
    }

    boolean hasEvidence() {
        return !standardCodes.isEmpty() || !supplierSkuCodes.isEmpty() || !categoryCodes.isEmpty() || !keywords.isEmpty();
    }

    private static Set<String> copy(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(new LinkedHashSet<>(source));
    }
}
