package com.zswy.shipsupply.procurement.food.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FoodItemNormalizerTest {

    private final FoodItemNormalizer normalizer = new FoodItemNormalizer();

    @Test
    void normalizesNameSpecificationAndUnitForFoodMatching() {
        assertThat(normalizer.name("  Fresh, Apple  ")).isEqualTo("FRESH APPLE");
        assertThat(normalizer.specification("10 x 500 g")).isEqualTo("10*500G");
        assertThat(normalizer.unit("KGS")).isEqualTo("KG");
        assertThat(normalizer.unit("pieces")).isEqualTo("PC");
    }

    @Test
    void prefersChineseNameAndBuildsStableCompositeKey() {
        assertThat(normalizer.matchKey("\u82f9\u679c", "Apple", "10 x 500 g", "KGS"))
            .isEqualTo("\u82f9\u679c|10*500G|KG");
        assertThat(normalizer.matchKey("", "Fresh Apple", "10x500g", "kg"))
            .isEqualTo("FRESH APPLE|10*500G|KG");
    }
}
