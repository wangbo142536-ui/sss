package com.zswy.shipsupply.procurement.materials;

import java.util.List;

@FunctionalInterface
interface MaterialSupplierCandidateProvider {

    List<MaterialSupplierCandidate> findOnShelfCandidates();
}
