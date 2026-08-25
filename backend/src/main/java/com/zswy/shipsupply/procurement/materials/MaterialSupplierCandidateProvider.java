package com.zswy.shipsupply.procurement.materials;

import java.util.List;
import java.util.Set;

interface MaterialSupplierCandidateProvider {

    List<MaterialSupplierCandidate> findOnShelfCandidates();

    default boolean supportsTargetedSearch() {
        return false;
    }

    default List<MaterialSupplierCandidate> findCandidates(MaterialSupplierCandidateQuery query) {
        return findOnShelfCandidates();
    }

    default List<MaterialSupplierCandidate> enrichCandidates(Set<Long> skuIds) {
        return List.of();
    }
}
