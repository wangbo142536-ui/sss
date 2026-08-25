package com.zswy.shipsupply.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zswy.shipsupply.shop.ShopImportRecognitionService.ShopRecognitionResult;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemRepository;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemResponse;

@ExtendWith(MockitoExtension.class)
class ShopImportRecognitionServiceTest {

    @Mock
    private ImpaItemRepository impaItemRepository;

    private ShopImportRecognitionService service;

    @BeforeEach
    void setUp() {
        service = new ShopImportRecognitionService(impaItemRepository);
    }

    @Test
    void parsesSizeAndCleanNameBeforeMatching() {
        ShopImportRecognitionService.ParsedName parsed = service.parse("6\"/160mm Flat Nose Plier", null, "12PCS/CTN");

        assertThat(parsed.cleanName()).isEqualTo("Flat Nose Plier");
        assertThat(parsed.attributes()).extracting(ShopParsedAttribute::key)
            .contains("size", "packing");
        assertThat(parsed.attributes().get(0).value()).contains("6\"", "160mm");
    }

    @Test
    void stripsRealQuoteParametersFromCleanName() {
        List<ShopImportRecognitionService.ParsedName> parsedItems = List.of(
            service.parse("""
                7''/180Mm Diagonal Cutting Plier
                Material:Cr-V
                Drop-Forged Hardened
                Nickle-Plated Finish
                Double Color Pvc Handle
                """, null, "PP CARD HANGER"),
            service.parse("5L Red Oxide Primer Paint", null, "4TIN/CTN"),
            service.parse("20mm x 100m Manila Rope", null, "COIL"),
            service.parse("8PCS Stainless Steel Kitchen Knife Set", null, "BOX"),
            service.parse("500ml Plastic Measuring Cup", null, "48PCS/CTN")
        );

        assertThat(parsedItems).extracting(ShopImportRecognitionService.ParsedName::cleanName)
            .containsExactly(
                "Diagonal Cutting Plier",
                "Red Oxide Primer Paint",
                "Manila Rope",
                "Stainless Steel Kitchen Knife Set",
                "Plastic Measuring Cup"
            );
        assertThat(parsedItems.get(0).attributes()).extracting(ShopParsedAttribute::key)
            .contains("size", "material", "process", "finish", "handle", "packing");
        assertThat(parsedItems.get(1).attributes()).extracting(ShopParsedAttribute::key)
            .contains("size", "packing");
        assertThat(parsedItems.get(2).attributes()).extracting(ShopParsedAttribute::key)
            .contains("size", "packing");
        assertThat(parsedItems.get(3).attributes()).extracting(ShopParsedAttribute::key)
            .contains("size", "packing");
        assertThat(parsedItems.get(4).attributes()).extracting(ShopParsedAttribute::key)
            .contains("size", "packing");
    }

    @Test
    void returnsLogicRecommendationAndModelNotConfigured() {
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(
                new ImpaItemResponse("613001", "61", "工具", "6130", "尖嘴钳", "FLAT NOSE PLIER", "160MM", "PCS"),
                new ImpaItemResponse("110101", "11", "甲板物料", "1101", "棉纱", "COTTON RAG", null, "KG")
            ));

        ShopRecognitionResult result = service.recognize("6\"/160mm Flat Nose Plier", null, null);

        assertThat(result.cleanName()).isEqualTo("Flat Nose Plier");
        assertThat(result.logicRecommendation().available()).isTrue();
        assertThat(result.logicRecommendation().impaCode()).isEqualTo("613001");
        assertThat(result.logicRecommendation().categoryCode()).isEqualTo("61");
        assertThat(result.matchDecision()).isEqualTo("LOGIC_ONLY");
    }

    @Test
    void marksUnmatchedWhenLogicAndModelHaveNoResult() {
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(new ImpaItemResponse("110101", "11", "甲板物料", "1101", "棉纱", "COTTON RAG", null, "KG")));

        ShopRecognitionResult result = service.recognize("Special Unknown Gear", null, null);

        assertThat(result.logicRecommendation().available()).isFalse();
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.matchDecision()).isEqualTo("UNMATCHED");
    }
    @Test
    void returnsCategoryCandidatesForCommonPlierKeywordWithoutChoosingCode() {
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(
                new ImpaItemResponse("611801", "61", "Tools", "6118", "External Ring Plier", "EXTERNAL RING PLIER", "180MM", "PCS"),
                new ImpaItemResponse("611802", "61", "Tools", "6118", "Internal Ring Plier", "INTERNAL RING PLIER", "180MM", "PCS"),
                new ImpaItemResponse("611601", "61", "Tools", "6116", "Combination Plier", "COMBINATION PLIER", "180MM", "PCS"),
                new ImpaItemResponse("172601", "17", "Cabin Stores", "1726", "Can Opener", "HAND CAN OPENER PLIER TYPE", null, "PCS")
            ));

        ShopRecognitionResult result = service.recognize("7\"/180Mm Circlip Plier", null, null);

        assertThat(result.cleanName()).isEqualTo("Circlip Plier");
        assertThat(result.logicRecommendation().available()).isTrue();
        assertThat(result.logicRecommendation().impaCode()).isNull();
        assertThat(result.logicRecommendation().categoryCode()).isEqualTo("61");
        assertThat(result.logicRecommendation().matchReason()).isEqualTo("CATEGORY_KEYWORD_MATCH");
        assertThat(result.categoryCandidates()).extracting(ShopCategoryCandidate::categoryCode).contains("61");
        assertThat(result.categoryCandidates()).extracting(ShopCategoryCandidate::segmentCode).contains("6118");
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.matchDecision()).isEqualTo("CATEGORY_ONLY");
    }

    @Test
    void importRecognitionUsesRuleOnly() {
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(new ImpaItemResponse("613001", "61", "Tools", "6130", "Pliers", "FLAT NOSE PLIER", "160MM", "PCS")));

        ShopRecognitionResult result = service.recognize("6\"/160mm Flat Nose Plier", null, null);

        assertThat(result.cleanName()).isEqualTo("Flat Nose Plier");
        assertThat(result.logicRecommendation().available()).isTrue();
        assertThat(result.logicRecommendation().impaCode()).isEqualTo("613001");
    }

    @Test
    void keepsManualReviewWhenRuleCannotMatch() {
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(new ImpaItemResponse("613001", "61", "Tools", "6130", "Pliers", "FLAT NOSE PLIER", "160MM", "PCS")));

        ShopRecognitionResult result = service.recognize("Unknown Special Gear", null, null);

        assertThat(result.cleanName()).isEqualTo("Unknown Special Gear");
        assertThat(result.logicRecommendation().available()).isFalse();
        assertThat(result.reviewRequired()).isTrue();
    }

    @Test
    void exposesCleanNameAndStableCategoryWithoutForcingLowConfidenceImpa() {
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(
                new ImpaItemResponse("611801", "61", "一般作业工具类", "6118", "外卡簧钳", "EXTERNAL RING PLIER", "180MM", "PCS"),
                new ImpaItemResponse("611802", "61", "一般作业工具类", "6118", "内卡簧钳", "INTERNAL RING PLIER", "180MM", "PCS"),
                new ImpaItemResponse("172619", "17", "厨房用品", "1726", "手开罐头器", "HAND CAN OPENER PLIER TYPE", null, "PCS")
            ));

        ShopIntelligentImportAnalysis analysis = service.analyzeForIntelligentImport(
            "7\"/180Mm Circlip Plier\nExternal Straight\nMaterial:Cr-V",
            null,
            "PP CARD HANGER"
        );

        assertThat(analysis.cleanName()).isEqualTo("Circlip Plier");
        assertThat(analysis.specification()).contains("180mm", "Cr-V", "PP CARD HANGER");
        assertThat(analysis.categoryCode()).isEqualTo("61");
        assertThat(analysis.categoryName()).isEqualTo("一般作业工具类");
        assertThat(analysis.recommendedImpaCode()).isNull();
        assertThat(analysis.reviewRequired()).isTrue();
    }

    @Test
    void usesStrongToolFamilyForCategoryButDoesNotForceAnExactImpaItem() {
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(
                new ImpaItemResponse("611101", "61", "一般作业工具类", "6111", "梅花扳手", "RING SPANNER", "10MM", "PCS"),
                new ImpaItemResponse("611102", "61", "一般作业工具类", "6111", "梅花扳手", "RING SPANNER", "12MM", "PCS"),
                new ImpaItemResponse("175001", "17", "厨房用品", "1750", "厨房扳手", "KITCHEN WRENCH", null, "PCS")
            ));

        ShopIntelligentImportAnalysis analysis = service.analyzeForIntelligentImport(
            "Double Offset Ring Spanner\nChrome Vanadium Steel",
            null,
            "PP CARD"
        );

        assertThat(analysis.cleanName()).isEqualTo("Double Offset Ring Spanner");
        assertThat(analysis.categoryCode()).isEqualTo("61");
        assertThat(analysis.recommendedImpaCode()).isNull();
        assertThat(analysis.reviewRequired()).isTrue();
    }

    @Test
    void treatsSourceCodePrefixAsBroadCategoryEvidenceWithoutInventingExactImpa() {
        when(impaItemRepository.findItems(isNull(), isNull(), isNull(), eq(60000)))
            .thenReturn(List.of(
                new ImpaItemResponse("250101", "25", "船舶油漆", "2501", "防锈底漆", "ANTICORROSIVE PRIMER", null, "LTR"),
                new ImpaItemResponse("250201", "25", "船舶油漆", "2502", "船壳面漆", "TOPSIDE PAINT", null, "LTR"),
                new ImpaItemResponse("750101", "75", "阀、旋塞类", "7501", "截止阀", "GLOBE VALVE", null, "PCS")
            ));

        ShopIntelligentImportAnalysis analysis = service.analyzeForIntelligentImport(
            "保养油漆品牌：PPG，类型：醇酸面漆 7238 SIGMARINE",
            "48 标志红 3149",
            "20L/桶",
            "25D-7238-3149"
        );

        assertThat(analysis.categoryCode()).isEqualTo("25");
        assertThat(analysis.categoryName()).isEqualTo("船舶油漆");
        assertThat(analysis.candidateCategoryCodes()).containsExactly("25");
        assertThat(analysis.recommendedImpaCode()).isNull();
        assertThat(analysis.reviewRequired()).isTrue();
    }
}
