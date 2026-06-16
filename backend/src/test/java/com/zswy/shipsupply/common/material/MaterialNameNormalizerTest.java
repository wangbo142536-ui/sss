package com.zswy.shipsupply.common.material;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaterialNameNormalizerTest {

    private final MaterialNameNormalizer normalizer = new MaterialNameNormalizer();

    @Test
    void extractsCleanNameAndAttributesFromLongToolDescription() {
        NormalizedMaterialName normalized = normalizer.normalize(
            """
                7''/180Mm Diagonal Cutting Plier
                Material:Cr-V
                Drop-Forged Hardened
                Nickle-Plated Finish
                Double Color Pvc Handle
                """,
            "",
            "PP CARD HANGER"
        );

        assertThat(normalized.cleanName()).isEqualTo("Diagonal Cutting Plier");
        assertThat(normalized.attributes()).extracting(MaterialNameAttribute::key)
            .contains("size", "material", "process", "finish", "handle", "packing");
        assertThat(normalized.nameTokens()).containsExactly("diagonal", "cutting", "plier");
        assertThat(normalized.specTokens()).contains("180mm", "cr", "forged", "finish", "handle", "card", "hanger");
    }

    @Test
    void marksToolKitsAsManualReviewRisk() {
        NormalizedMaterialName normalized = normalizer.normalize(
            "Leather Belt Hole Punch+ Eyelet Plier +Snap Button Grommet Setter Tool Kit",
            "",
            ""
        );

        assertThat(normalized.cleanName()).contains("Hole Punch");
        assertThat(normalized.riskFlags()).contains("MULTI_PRODUCT_FAMILY", "KIT_OR_SET");
    }
}
