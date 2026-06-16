package com.zswy.shipsupply.procurement.materials;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zswy.shipsupply.auth.TokenService;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemRepository;

@SpringBootTest
class MaterialMatchPreviewSampleSmokeTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ImpaItemRepository impaItemRepository;

    @Autowired
    private XlsxMaterialQuoteParser parser;

    @Test
    void parsesSkuRfqSampleAsDemandInquiry() throws Exception {
        MaterialParsedDocument document = parser.parse(Path.of(
            "..",
            "tmp",
            "end_to_end_sku_pool",
            "wangbo_material_rfq_131.xlsx"
        ));

        assertThat(document.documentType()).isEqualTo("DEMAND_INQUIRY");
        assertThat(document.sourceFormat()).isEqualTo("SKU_RFQ");
        assertThat(document.headerRowIndex()).isEqualTo(1);
        assertThat(document.rows()).hasSize(131);

        MaterialQuoteRow first = document.rows().get(0);
        assertThat(first.supplierItemNo()).isEqualTo("100108");
        assertThat(first.description()).contains("Diagonal Cutting Plier");
        assertThat(first.quantity()).isEqualTo("5");
        assertThat(first.unit()).isEqualTo("PCS");
        assertThat(first.remarks()).contains("E2E RFQ");
        assertThat(first.rawColumns()).containsKeys("Application No.", "Vessel Name", "Inquiry Date");

        MaterialMatchPreviewService service = new MaterialMatchPreviewService(
            tokenService,
            impaItemRepository,
            parser,
            () -> {
                throw new AssertionError("Demand import preview must not load supplier SKU candidates");
            }
        );
        MaterialMatchPreviewResponse response = service.matchDocument(document);
        assertThat(response.documentType()).isEqualTo("DEMAND_INQUIRY");
        assertThat(response.sourceFormat()).isEqualTo("SKU_RFQ");
        assertThat(response.totalRows()).isEqualTo(131);
        assertThat(response.items()).allSatisfy(item -> assertThat(item.supplierCandidates()).isEmpty());
        assertThat(response.items().get(0).cleanName()).isEqualTo("Diagonal Cutting Plier");
        assertThat(response.items().get(0).parsedAttributes()).extracting("key")
            .contains("size", "material", "process", "finish", "handle", "packing");
        assertThat(response.items().get(0).price()).isNull();
    }

    @Test
    void parsesAndMatchesRealSamples() throws Exception {
        MaterialMatchPreviewService service = new MaterialMatchPreviewService(
            tokenService,
            impaItemRepository,
            parser,
            () -> List.of()
        );
        List<Path> samples = List.of(
            Path.of("..", "tmp", "provision_analysis", "FQ", "STCL26SD003-R01.xlsx"),
            Path.of("..", "tmp", "provision_analysis", "FQ", "STCL26SD004-R01.xlsx"),
            Path.of("..", "tmp", "provision_analysis", "FQ", "STCL26SG001-R01.xlsx"),
            Path.of("..", "tmp", "provision_analysis", "FQ", "STCL26SW001-R01.xlsx"),
            Path.of("..", "tmp", "provision_analysis", "material_quote.xlsx")
        );

        for (Path sample : samples) {
            MaterialMatchPreviewResponse response = service.matchDocument(parser.parse(sample));
            System.out.printf(
                "SAMPLE_STATS file=%s type=%s header=%d total=%d exact=%d similar=%d unmatched=%d%n",
                sample.getFileName(),
                response.documentType(),
                response.headerRowIndex(),
                response.totalRows(),
                response.exactCount(),
                response.similarCount(),
                response.unmatchedCount()
            );
            assertThat(response.totalRows()).isGreaterThan(0);
            assertThat(response.totalRows()).isEqualTo(
                response.exactCount() + response.similarCount() + response.unmatchedCount()
            );
        }
    }
}
