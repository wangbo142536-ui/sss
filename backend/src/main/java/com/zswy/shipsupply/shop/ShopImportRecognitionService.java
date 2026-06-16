package com.zswy.shipsupply.shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zswy.shipsupply.common.material.MaterialNameAttribute;
import com.zswy.shipsupply.common.material.MaterialNameNormalizer;
import com.zswy.shipsupply.common.material.NormalizedMaterialName;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemRepository;
import com.zswy.shipsupply.standardlibrary.item.ImpaItemResponse;

@Service
public class ShopImportRecognitionService {

    private static final Set<String> STOP_WORDS = Set.of(
        "of", "and", "for", "with", "the", "a", "an", "type", "model", "commodity", "specification"
    );

    private final ImpaItemRepository impaItemRepository;
    private final MaterialNameNormalizer normalizer;
    private volatile List<ImpaItemResponse> cachedItems;
    private volatile List<ItemProfile> cachedProfiles;

    @Autowired
    public ShopImportRecognitionService(ImpaItemRepository impaItemRepository, MaterialNameNormalizer normalizer) {
        this.impaItemRepository = impaItemRepository;
        this.normalizer = normalizer;
    }

    ShopImportRecognitionService(ImpaItemRepository impaItemRepository) {
        this(impaItemRepository, new MaterialNameNormalizer());
    }

    public ShopRecognitionResult recognize(String rawName, String rawSpec, String packing) {
        ParsedName parsed = parse(rawName, rawSpec, packing);
        ShopImportRecommendation logic = logicRecommendation(parsed);
        List<ShopCategoryCandidate> categoryCandidates = categoryCandidates(parsed);
        if (!Boolean.TRUE.equals(logic.available()) && !categoryCandidates.isEmpty()) {
            logic = categoryOnlyRecommendation(categoryCandidates.get(0));
        }
        Decision decision = decide(logic);
        return new ShopRecognitionResult(parsed.cleanName(), parsed.attributes(), logic, categoryCandidates, decision.reviewRequired(), decision.matchDecision());
    }

    ParsedName parse(String rawName, String rawSpec, String packing) {
        NormalizedMaterialName normalized = normalizer.normalize(rawName, rawSpec, packing);
        List<ShopParsedAttribute> attributes = normalized.attributes().stream()
            .map(this::toShopAttribute)
            .toList();
        return new ParsedName(normalized.cleanName(), attributes);
    }

    private ShopParsedAttribute toShopAttribute(MaterialNameAttribute attribute) {
        return new ShopParsedAttribute(attribute.key(), attribute.name(), attribute.value(), attribute.unit(), attribute.rawText());
    }

    private ShopImportRecommendation logicRecommendation(ParsedName parsed) {
        if (parsed.cleanName() == null) {
            return noMatch("EMPTY_CLEAN_NAME");
        }
        List<String> queryTokens = tokens(parsed.cleanName());
        if (queryTokens.isEmpty()) {
            return noMatch("EMPTY_CLEAN_NAME");
        }
        return allItemProfiles().stream()
            .map(profile -> score(profile, queryTokens, parsed.attributes()))
            .filter(score -> score.nameScore() >= 0.55)
            .max(Comparator
                .comparingDouble(CandidateScore::totalScore)
                .thenComparing(score -> score.item().impaCode(), Comparator.reverseOrder()))
            .map(this::recommendation)
            .orElseGet(() -> noMatch("CLEAN_NAME_NO_FAMILY_MATCH"));
    }

    private List<ImpaItemResponse> allItems() {
        List<ImpaItemResponse> items = cachedItems;
        if (items == null) {
            synchronized (this) {
                items = cachedItems;
                if (items == null) {
                    items = impaItemRepository.findItems(null, null, null, 60000);
                    cachedItems = items;
                }
            }
        }
        return items;
    }

    private List<ItemProfile> allItemProfiles() {
        List<ItemProfile> profiles = cachedProfiles;
        if (profiles == null) {
            synchronized (this) {
                profiles = cachedProfiles;
                if (profiles == null) {
                    profiles = allItems().stream()
                        .map(item -> {
                            Set<String> itemTokens = new LinkedHashSet<>();
                            itemTokens.addAll(tokens(item.nameEn()));
                            itemTokens.addAll(tokens(item.nameCn()));
                            itemTokens.addAll(tokens(item.specification()));
                            return new ItemProfile(item, itemTokens);
                        })
                        .toList();
                    cachedProfiles = profiles;
                }
            }
        }
        return profiles;
    }

    private CandidateScore score(ItemProfile profile, List<String> queryTokens, List<ShopParsedAttribute> attributes) {
        ImpaItemResponse item = profile.item();
        long hitCount = queryTokens.stream().filter(profile.tokens()::contains).count();
        double nameScore = queryTokens.isEmpty() ? 0 : (double) hitCount / queryTokens.size();
        double specScore = specScore(item.specification(), attributes);
        return new CandidateScore(item, nameScore, specScore, nameScore * 0.8 + specScore * 0.2);
    }

    private double specScore(String specification, List<ShopParsedAttribute> attributes) {
        if (specification == null || specification.isBlank() || attributes.isEmpty()) {
            return 0;
        }
        String normalizedSpec = normalize(specification);
        long hits = attributes.stream()
            .map(ShopParsedAttribute::value)
            .filter(Objects::nonNull)
            .map(this::normalize)
            .filter(value -> !value.isBlank())
            .filter(value -> normalizedSpec.contains(value) || value.contains(normalizedSpec))
            .count();
        return hits > 0 ? 1 : 0;
    }

    private ShopImportRecommendation recommendation(CandidateScore score) {
        String reason = score.specScore() > 0 ? "CLEAN_NAME_SPEC_MATCH" : "CLEAN_NAME_FAMILY_MATCH";
        String confidence = score.nameScore() >= 0.8 && score.specScore() > 0 ? "HIGH" : score.nameScore() >= 0.7 ? "MEDIUM" : "LOW";
        ImpaItemResponse item = score.item();
        return new ShopImportRecommendation(
            true,
            "READY",
            item.impaCode(),
            item.nameCn(),
            item.nameEn(),
            item.specification(),
            item.categoryCode(),
            item.categoryName(),
            item.unit(),
            reason,
            confidence
        );
    }

    private ShopImportRecommendation categoryOnlyRecommendation(ShopCategoryCandidate candidate) {
        return new ShopImportRecommendation(
            true,
            "CATEGORY_ONLY",
            null,
            null,
            null,
            null,
            candidate.categoryCode(),
            candidate.categoryName(),
            null,
            "CATEGORY_KEYWORD_MATCH",
            "LOW"
        );
    }

    private ShopImportRecommendation noMatch(String reason) {
        return new ShopImportRecommendation(false, "NO_MATCH", null, null, null, null, null, null, null, reason, "NONE");
    }

    private Decision decide(ShopImportRecommendation logic) {
        if (Boolean.TRUE.equals(logic.available())) {
            if (logic.impaCode() == null) {
                return new Decision(true, "CATEGORY_ONLY");
            }
            boolean reviewRequired = !"HIGH".equals(logic.confidenceLevel());
            return new Decision(reviewRequired, "LOGIC_ONLY");
        }
        return new Decision(true, "UNMATCHED");
    }

    private List<ShopCategoryCandidate> categoryCandidates(ParsedName parsed) {
        if (parsed.cleanName() == null) {
            return List.of();
        }
        List<String> queryTokens = tokens(parsed.cleanName());
        List<String> categoryKeywords = queryTokens.stream()
            .map(this::categoryKeyword)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (categoryKeywords.isEmpty()) {
            return List.of();
        }
        List<ItemProfile> profiles = allItemProfiles();
        List<ShopCategoryCandidate> candidates = new ArrayList<>();
        for (String keyword : categoryKeywords) {
            List<ImpaItemResponse> keywordItems = profiles.stream()
                .filter(profile -> profile.tokens().contains(keyword))
                .map(ItemProfile::item)
                .toList();
            if (keywordItems.isEmpty()) {
                continue;
            }
            Map<String, List<ImpaItemResponse>> byCategory = keywordItems.stream()
                .filter(item -> item.categoryCode() != null && !item.categoryCode().isBlank())
                .collect(Collectors.groupingBy(
                    ImpaItemResponse::categoryCode,
                    Collectors.toList()
                ));
            ImpaItemResponse preferredCategoryItem = byCategory.values().stream()
                .max(Comparator.comparingInt(List::size))
                .flatMap(group -> group.stream().findFirst())
                .orElse(null);
            if (preferredCategoryItem == null) {
                continue;
            }
            String preferredCategoryCode = preferredCategoryItem.categoryCode();
            List<ShopCategoryCandidate> segmentCandidates = keywordItems.stream()
                .filter(item -> preferredCategoryCode.equals(item.categoryCode()))
                .filter(item -> item.segmentCode() != null && !item.segmentCode().isBlank())
                .collect(Collectors.groupingBy(
                    ImpaItemResponse::segmentCode,
                    Collectors.collectingAndThen(Collectors.toList(), Function.identity())
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(8)
                .map(entry -> {
                    ImpaItemResponse firstItem = entry.getValue().get(0);
                    return new ShopCategoryCandidate(
                        firstItem.categoryCode(),
                        firstItem.categoryName(),
                        firstItem.segmentCode(),
                        firstItem.segmentCode() + " code segment",
                        keyword,
                        "CATEGORY_KEYWORD_MATCH",
                        entry.getValue().size()
                    );
                })
                .toList();
            candidates.addAll(segmentCandidates);
        }
        return candidates.stream()
            .collect(Collectors.toMap(
                candidate -> candidate.categoryCode() + ":" + candidate.segmentCode() + ":" + candidate.matchedKeyword(),
                Function.identity(),
                (left, right) -> left
            ))
            .values()
            .stream()
            .sorted(Comparator
                .comparing(ShopCategoryCandidate::categoryCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(ShopCategoryCandidate::segmentCode, Comparator.nullsLast(String::compareTo)))
            .toList();
    }

    private String categoryKeyword(String token) {
        if ("plier".equals(token) || "pliers".equals(token)) {
            return "plier";
        }
        return null;
    }

    private List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^a-z0-9\\u4e00-\\u9fa5]+"))
            .map(String::trim)
            .filter(token -> token.length() > 1)
            .filter(token -> !STOP_WORDS.contains(token))
            .distinct()
            .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\p{Punct}\\s]+", "").toUpperCase(Locale.ROOT);
    }

    private String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    record ShopRecognitionResult(
        String cleanName,
        List<ShopParsedAttribute> parsedAttributes,
        ShopImportRecommendation logicRecommendation,
        List<ShopCategoryCandidate> categoryCandidates,
        boolean reviewRequired,
        String matchDecision
    ) {
    }

    record ParsedName(String cleanName, List<ShopParsedAttribute> attributes) {
    }

    private record CandidateScore(ImpaItemResponse item, double nameScore, double specScore, double totalScore) {
    }

    private record ItemProfile(ImpaItemResponse item, Set<String> tokens) {
    }

    private record Decision(boolean reviewRequired, String matchDecision) {
    }
}
