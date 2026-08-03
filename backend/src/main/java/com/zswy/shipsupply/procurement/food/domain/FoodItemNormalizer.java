package com.zswy.shipsupply.procurement.food.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class FoodItemNormalizer {

    private static final Map<String, String> UNIT_ALIASES = Map.ofEntries(
        Map.entry("KGS", "KG"),
        Map.entry("KILOGRAM", "KG"),
        Map.entry("KILOGRAMS", "KG"),
        Map.entry("PCS", "PC"),
        Map.entry("PIECE", "PC"),
        Map.entry("PIECES", "PC"),
        Map.entry("PC/个", "PC"),
        Map.entry("PACK", "PKT"),
        Map.entry("PACKET", "PKT"),
        Map.entry("PACKETS", "PKT"),
        Map.entry("BOTTLE", "BTL"),
        Map.entry("BOTTLES", "BTL"),
        Map.entry("CARTON", "BOX"),
        Map.entry("CARTONS", "BOX")
    );

    public String name(String value) {
        return compact(value)
            .toUpperCase(Locale.ROOT)
            .replaceAll("[\\p{Punct}，。；：、（）【】《》]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    public String specification(String value) {
        return compact(value)
            .toUpperCase(Locale.ROOT)
            .replace('×', '*')
            .replace('Ｘ', '*')
            .replaceAll("\\s*[Xx*]\\s*", "*")
            .replaceAll("\\s+", "")
            .trim();
    }

    public String unit(String value) {
        String normalized = compact(value).toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        return UNIT_ALIASES.getOrDefault(normalized, normalized);
    }

    public String matchKey(String nameZh, String nameEn, String specification, String unit) {
        String preferredName = name(nameZh);
        if (preferredName.isBlank()) {
            preferredName = name(nameEn);
        }
        return preferredName + "|" + specification(specification) + "|" + unit(unit);
    }

    private String compact(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
    }
}

