package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.zswy.shipsupply.auth.TokenService;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemRepository;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemResponse;

class MaterialMatchPreviewServiceTest {

    @Test
    void supplierItemNoCodeHitIsSpecCandidateInsteadOfCodeMatch() {
        MaterialMatchPreviewService service = serviceWithItems(List.of(item(
            "110203",
            "11",
            "Deck",
            "1102",
            "Cotton coat",
            "COTTON COAT WITH HOOD",
            "",
            "PCS"
        )));

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            1,
            2,
            Map.of("Item No.", "110203", "Name of Commodity & Specification", "BENT NOSE PLIERS"),
            "",
            "",
            "",
            "",
            "",
            "",
            "110203",
            "BENT NOSE PLIERS",
            "12.50",
            "BOX",
            "100",
            false,
            null,
            null,
            null,
            null
        )));

        assertThat(response.exactCount()).isZero();
        assertThat(response.unmatchedCount()).isEqualTo(1);
        assertThat(response.items().get(0).matchResult()).isEqualTo("UNMATCHED");
        assertThat(response.items().get(0).candidates().get(0).candidateMatchType()).isEqualTo("SPEC_MATCH");
        assertThat(response.items().get(0).reason()).isEqualTo("NAME_SPEC_MISMATCH");
    }

    @Test
    void nameSpecCandidatePrioritizesHighlyConsistentNameOverPartialTokenHit() {
        MaterialMatchPreviewService service = serviceWithItems(List.of(
            item("110203", "11", "Deck", "1102", "Coat", "LONG SLEEVE COTTON COAT", "", "PCS"),
            item("613041", "61", "Tools", "6130", "Pliers", "LONG NOSE PLIERS", "", "PCS")
        ));

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "SUPPLIER_QUOTATION",
            "SUPPLIER_QUOTATION",
            1,
            1,
            2,
            Map.of("Item No.", "SKU-001", "Name of Commodity & Specification", "LONG NOSE PLIERS"),
            "",
            "",
            "",
            "",
            "",
            "",
            "SKU-001",
            "LONG NOSE PLIERS",
            "12.50",
            "BOX",
            "100",
            false,
            null,
            null,
            null,
            null
        )));

        MaterialMatchCandidate best = response.items().get(0).candidates().get(0);
        assertThat(response.similarCount()).isEqualTo(1);
        assertThat(response.items().get(0).candidateImpaCode()).isEqualTo("613041");
        assertThat(best.categoryCode()).isEqualTo("61");
        assertThat(best.categoryName()).isEqualTo("Tools");
        assertThat(best.candidateMatchType()).isEqualTo("SPEC_MATCH");
        assertThat(response.items().get(0).reason()).isEqualTo("NAME_SPEC_MATCH");
    }

    @Test
    void demandNonStandardCodeUsesDescriptionCandidatesOnly() {
        MaterialMatchPreviewService service = serviceWithItems(List.of(item(
            "330101",
            "33",
            "Safety",
            "3301",
            "SCBA backboard",
            "SCBA BACKBOARD",
            "",
            "PCS"
        )));

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "STANDARD_FQ",
            27,
            1,
            28,
            Map.of("IMPA", "SCBA backboard", "DESCRIPTION", "SCBA backboard", "Size/Model", ""),
            "SCBA backboard",
            "SCBA backboard",
            "",
            "1",
            "PCS",
            "",
            "",
            "",
            "",
            "",
            "",
            false,
            null,
            null,
            null,
            null
        )));

        assertThat(response.exactCount()).isZero();
        assertThat(response.similarCount()).isEqualTo(1);
        assertThat(response.items().get(0).candidateImpaCode()).isEqualTo("330101");
        assertThat(response.items().get(0).candidates().get(0).candidateMatchType()).isEqualTo("SPEC_MATCH");
        assertThat(response.items().get(0).reason()).isEqualTo("NAME_SPEC_MATCH");
    }

    @Test
    void demandOfficialCodeHitReturnsExactEvenWhenNameNeedsReview() {
        MaterialMatchPreviewService service = serviceWithItems(List.of(item(
            "110203",
            "11",
            "Deck",
            "1102",
            "Cotton coat",
            "COTTON COAT WITH HOOD",
            "",
            "PCS"
        )));

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "STANDARD_FQ",
            27,
            1,
            28,
            Map.of("IMPA", "110203", "DESCRIPTION", "BENT NOSE PLIERS", "Size/Model", ""),
            "110203",
            "BENT NOSE PLIERS",
            "",
            "1",
            "PCS",
            "",
            "",
            "",
            "",
            "",
            "",
            false,
            null,
            null,
            null,
            null
        )));

        assertThat(response.items().get(0).matchResult()).isEqualTo("EXACT");
        assertThat(response.items().get(0).reason()).isEqualTo("CODE_MATCH");
    }

    @Test
    void demandOfficialCodeHitTreatsPluralAndColorAsExactCodeMatch() {
        MaterialMatchPreviewService service = serviceWithItems(List.of(item(
            "150601",
            "15",
            "Cabin Stores",
            "1506",
            "Bath towel",
            "Towel (Bath)",
            "",
            "PCS"
        )));

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "STANDARD_FQ",
            27,
            1,
            28,
            Map.of("IMPA", "150601", "DESCRIPTION", "Towels WHITE", "Size/Model", ""),
            "150601",
            "Towels WHITE",
            "",
            "1",
            "PCS",
            "",
            "",
            "",
            "",
            "",
            "",
            false,
            null,
            null,
            null,
            null
        )));

        MaterialMatchPreviewItem matched = response.items().get(0);
        assertThat(response.exactCount()).isEqualTo(1);
        assertThat(matched.matchResult()).isEqualTo("EXACT");
        assertThat(matched.reason()).isEqualTo("CODE_MATCH");
        assertThat(matched.candidateImpaCode()).isEqualTo("150601");
    }

    @Test
    void demandLongDescriptionUsesCleanNameAndSpecTokensForMatching() {
        MaterialMatchPreviewService service = serviceWithItems(List.of(
            item("611705", "61", "Tools", "6117", "Flat Nose Plier", "FLAT NOSE PLIER", "160MM", "PCS"),
            item("231401", "23", "Rigging", "2314", "Wire Rope Clip", "DROP FORGED WIRE ROPE CLIP", "180MM", "PCS")
        ));

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "SKU_RFQ",
            1,
            1,
            3,
            Map.of("Supplier SKU Ref", "100115", "Name of Commodity & Specification", """
                6"/160mm Flat Nose Plier
                Material:Cr-V
                Drop-Forged Hardened
                Nickle-Plated Finish
                Double Color Pvc Handle
                """),
            "",
            """
                6"/160mm Flat Nose Plier
                Material:Cr-V
                Drop-Forged Hardened
                Nickle-Plated Finish
                Double Color Pvc Handle
                """,
            "PP CARD HANGER",
            "5",
            "PCS",
            "",
            "100115",
            "",
            "",
            "PP CARD HANGER",
            "",
            false,
            null,
            null,
            null,
            null
        )));

        MaterialMatchPreviewItem item = response.items().get(0);
        assertThat(item.cleanName()).isEqualTo("Flat Nose Plier");
        assertThat(item.candidateImpaCode()).isEqualTo("611705");
        assertThat(item.reason()).isEqualTo("NAME_SPEC_MATCH");
        assertThat(item.matchResult()).isEqualTo("SIMILAR");
    }

    @Test
    void demandSupplierSkuCollisionWithMismatchedOfficialCodeIsDowngraded() {
        MaterialMatchPreviewService service = serviceWithItems(List.of(
            item("110203", "11", "Deck", "1102", "Cotton coat", "COTTON COAT WITH HOOD", "", "PCS"),
            item("611641", "61", "Tools", "6116", "Locking Plier", "VISE GRIP COMBINATION PLIER", "250MM", "PCS")
        ));

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "SKU_RFQ",
            1,
            1,
            9,
            Map.of("Supplier SKU Ref", "110203", "IMPA/Platform Code", "110203", "Name of Commodity & Specification", """
                10''/250Mm Curved Jaw Locking Plier
                Material:Cr-V
                Drop-Forged Hardened
                Nickle-Plated Finish
                With Wire Cutter
                """),
            "110203",
            """
                10''/250Mm Curved Jaw Locking Plier
                Material:Cr-V
                Drop-Forged Hardened
                Nickle-Plated Finish
                With Wire Cutter
                """,
            "PP CARD HANGER",
            "2",
            "PCS",
            "",
            "110203",
            "",
            "",
            "PP CARD HANGER",
            "",
            false,
            null,
            null,
            null,
            null
        )));

        MaterialMatchPreviewItem item = response.items().get(0);
        assertThat(item.reason()).isNotEqualTo("CODE_MATCH");
        assertThat(item.matchResult()).isNotEqualTo("EXACT");
        assertThat(item.candidateImpaCode()).isEqualTo("611641");
        assertThat(item.riskFlags()).contains("SUPPLIER_CODE_COLLISION");
    }

    @Test
    void kitOrSetDemandIsDowngradedForManualReview() {
        MaterialMatchPreviewService service = serviceWithItems(List.of(
            item("615233", "61", "Tools", "6152", "Hand Riveter Kit", "HAND RIVETER KIT", "WITH RIVETS", "SET"),
            item("610101", "61", "Tools", "6101", "Socket Wrench Set", "SOCKET WRENCH SET", "12PCS 1/2IN", "SET")
        ));

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "SKU_RFQ",
            1,
            1,
            20,
            Map.of("Supplier SKU Ref", "201901", "Name of Commodity & Specification", "12Pcs 1/2\" Dr. Socket Set With Extension Bar And Ratchet"),
            "",
            "12Pcs 1/2\" Dr. Socket Set With Extension Bar And Ratchet",
            "",
            "1",
            "SET",
            "",
            "201901",
            "",
            "",
            "",
            "",
            false,
            null,
            null,
            null,
            null
        )));

        MaterialMatchPreviewItem item = response.items().get(0);
        assertThat(item.cleanName()).contains("Socket Set");
        assertThat(item.riskFlags()).contains("KIT_OR_SET");
        assertThat(item.reason()).isEqualTo("NAME_MATCH_SPEC_CHECK");
        assertThat(item.matchResult()).isEqualTo("SIMILAR");
    }

    @Test
    void nameMatchWithDifferentSpecificationReturnsSpecificReason() {
        MaterialMatchPreviewService service = serviceWithItems(List.of(item(
            "613041",
            "61",
            "Tools",
            "6130",
            "Pliers",
            "LONG NOSE PLIERS",
            "150MM",
            "PCS"
        )));

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "STANDARD_FQ",
            27,
            1,
            28,
            Map.of("DESCRIPTION", "LONG NOSE PLIERS", "Size/Model", "300MM"),
            "",
            "LONG NOSE PLIERS",
            "300MM",
            "1",
            "PCS",
            "",
            "",
            "",
            "",
            "",
            "",
            false,
            null,
            null,
            null,
            null
        )));

        assertThat(response.items().get(0).matchResult()).isEqualTo("SIMILAR");
        assertThat(response.items().get(0).reason()).isEqualTo("NAME_MATCH_SPEC_CHECK");
        assertThat(response.items().get(0).candidates().get(0).reason()).isEqualTo("NAME_MATCH_SPEC_CHECK");
    }

    @Test
    void demandPreviewDoesNotReturnSupplierCandidatesOrTopLevelPrice() {
        MaterialMatchPreviewService service = serviceWithItemsAndSupplierSkus(
            List.of(),
            List.of(
                supplierSku(1L, "Supplier A", "100108", "Diagonal Cutting Plier", "100108", null, "11", "General Tools", "8.91", "ON_SHELF"),
                supplierSku(2L, "Supplier B", "100108", "Diagonal Cutting Plier", "100108", null, "11", "General Tools", "10.27", "ON_SHELF"),
                supplierSku(3L, "Supplier C", "100108", "Diagonal Cutting Plier", "100108", null, "11", "General Tools", "11.43", "ON_SHELF"),
                supplierSku(4L, "Supplier Hidden", "100108", "Diagonal Cutting Plier", "100108", null, "11", "General Tools", "1.00", "OFF_SHELF")
            )
        );

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "SKU_RFQ",
            1,
            1,
            2,
            Map.of(
                "Supplier SKU Ref", "100108",
                "Name of Commodity & Specification", "7''/180Mm Diagonal Cutting Plier",
                "Request Qty", "5",
                "Unit", "PCS"
            ),
            "",
            "7''/180Mm Diagonal Cutting Plier",
            "PP CARD HANGER",
            "5",
            "PCS",
            "",
                "100108",
                "7''/180Mm Diagonal Cutting Plier",
                "8.91",
                "PP CARD HANGER",
                "",
                false,
            null,
            null,
            null,
            null
        )));

        assertThat(response.items().get(0).supplierCandidates()).isEmpty();
        assertThat(response.items().get(0).price()).isNull();
        assertThat(response.items().get(0).rawColumns()).containsEntry("Supplier SKU Ref", "100108");
    }

    @Test
    void standardDemandPreviewDoesNotReturnSupplierCandidates() {
        List<MaterialSupplierCandidate> supplierSkus = java.util.stream.IntStream.rangeClosed(1, 12)
            .mapToObj(index -> supplierSku(
                (long) index,
                "Supplier " + index,
                "SKU-" + index,
                "Flat Nose Plier",
                null,
                "613041",
                "61",
                "Tools",
                String.valueOf(index),
                "ON_SHELF"
            ))
            .toList();
        MaterialMatchPreviewService service = serviceWithItemsAndSupplierSkus(List.of(item(
            "613041",
            "61",
            "Tools",
            "6130",
            "Pliers",
            "FLAT NOSE PLIER",
            "",
            "PCS"
        )), supplierSkus);

        MaterialMatchPreviewResponse response = service.matchRows(List.of(new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "STANDARD_FQ",
            27,
            1,
            28,
            Map.of("IMPA", "613041", "DESCRIPTION", "Flat Nose Plier", "Inquiry Qty.", "1", "UNIT", "PCS"),
            "613041",
            "Flat Nose Plier",
            "",
            "1",
            "PCS",
            "",
            "",
            "",
            "",
            "",
            "",
            false,
            null,
            null,
            null,
            null
        )));

        assertThat(response.items().get(0).supplierCandidates()).isEmpty();
        assertThat(response.items().get(0).matchResult()).isEqualTo("EXACT");
    }

    @Test
    void supplierCandidatePoolIsNotLoadedForDemandPreview() {
        TokenService tokenService = mock(TokenService.class);
        ImpaItemRepository impaItemRepository = mock(ImpaItemRepository.class);
        XlsxMaterialQuoteParser parser = mock(XlsxMaterialQuoteParser.class);
        MaterialSupplierCandidateProvider supplierCandidateProvider = mock(MaterialSupplierCandidateProvider.class);
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000))).thenReturn(List.of());
        when(supplierCandidateProvider.findOnShelfCandidates()).thenReturn(List.of(
            supplierSku(1L, "Supplier A", "SKU-1", "Flat Nose Plier", null, null, "61", "Tools", "8.91", "ON_SHELF")
        ));
        MaterialMatchPreviewService service = new MaterialMatchPreviewService(
            tokenService,
            impaItemRepository,
            parser,
            supplierCandidateProvider
        );

        service.matchRows(List.of(
            demandRow("SKU-1", "Flat Nose Plier"),
            demandRow("SKU-2", "Diagonal Cutting Plier")
        ));

        verify(supplierCandidateProvider, never()).findOnShelfCandidates();
    }

    private MaterialMatchPreviewService serviceWithItems(List<ImpaItemResponse> items) {
        return serviceWithItemsAndSupplierSkus(items, List.of());
    }

    private MaterialMatchPreviewService serviceWithItemsAndSupplierSkus(
        List<ImpaItemResponse> items,
        List<MaterialSupplierCandidate> supplierSkus
    ) {
        TokenService tokenService = mock(TokenService.class);
        ImpaItemRepository impaItemRepository = mock(ImpaItemRepository.class);
        XlsxMaterialQuoteParser parser = mock(XlsxMaterialQuoteParser.class);
        MaterialSupplierCandidateProvider supplierCandidateProvider = mock(MaterialSupplierCandidateProvider.class);
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000))).thenReturn(items);
        when(supplierCandidateProvider.findOnShelfCandidates()).thenReturn(supplierSkus);
        return new MaterialMatchPreviewService(tokenService, impaItemRepository, parser, supplierCandidateProvider);
    }

    private ImpaItemResponse item(
        String impaCode,
        String categoryCode,
        String categoryName,
        String segmentCode,
        String nameCn,
        String nameEn,
        String specification,
        String unit
    ) {
        return new ImpaItemResponse(
            impaCode,
            categoryCode,
            categoryName,
            segmentCode,
            nameCn,
            nameEn,
            specification,
            unit
        );
    }

    private MaterialSupplierCandidate supplierSku(
        Long skuId,
        String supplierName,
        String supplierSkuCode,
        String productName,
        String impaCode,
        String platformCode,
        String categoryCode,
        String categoryName,
        String unitPrice,
        String shelfStatus
    ) {
        return new MaterialSupplierCandidate(
            skuId,
            100L + skuId,
            supplierName,
            supplierSkuCode,
            productName,
            impaCode,
            platformCode,
            categoryCode,
            categoryName,
            List.of(new MaterialSupplierSkuAttribute("specification", "Specification", productName, null, 0, productName)),
            productName,
            new BigDecimal(unitPrice),
            "CNY",
            "\u00A5",
            new BigDecimal("99"),
            "PCS",
            "BOX",
            "/files/" + skuId + ".png",
            "/files/" + skuId + "-thumb.png",
            shelfStatus,
            "SPEC_MATCHED",
            null,
            null
        );
    }

    private MaterialQuoteRow demandRow(String supplierSkuCode, String description) {
        return new MaterialQuoteRow(
            "DEMAND_INQUIRY",
            "SKU_RFQ",
            1,
            1,
            2,
            Map.of("Supplier SKU Ref", supplierSkuCode, "Name of Commodity & Specification", description),
            "",
            description,
            "",
            "1",
            "PCS",
            "",
            supplierSkuCode,
            description,
            "",
            "",
            "",
            false,
            null,
            null,
            null,
            null
        );
    }
}

