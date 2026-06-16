package com.zswy.shipsupply.common.material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class MaterialNameNormalizer {

    private static final Pattern SIZE_PATTERN = Pattern.compile(
        "(?i)(\\d+(?:\\.\\d+)?\\s*(?:[\"']{1,2}|inch|in)\\s*/\\s*\\d+(?:\\.\\d+)?\\s*(?:mm|cm|m)\\b|"
            + "\\d+(?:\\.\\d+)?\\s*(?:/\\s*\\d+(?:\\.\\d+)?)?\\s*(?:[\"']{1,2}|inch|in|mm|cm|m|ml|l|kg|g|pcs|ctn|box)\\b|"
            + "\\d+(?:\\.\\d+)?\\s*[xX]\\s*\\d+(?:\\.\\d+)?(?:\\s*[xX]\\s*\\d+(?:\\.\\d+)?)?\\s*(?:mm|cm|m|ml|l|kg|g|pcs|ctn|box|in|inch)?\\b)"
    );
    private static final Pattern MATERIAL_PATTERN = Pattern.compile(
        "(?i)\\b(?:material|\\u6750\\u8d28)\\s*(?::|\\uFF1A)\\s*([^,;\\uFF0C\\uFF1B\\r\\n]+)"
    );
    private static final Pattern ATTRIBUTE_TAIL_PATTERN = Pattern.compile(
        "(?i)\\b(?:material|finish|handle|colour|color|packing|package|barcode|brand|model)\\s*(?::|\\uFF1A).*$"
    );
    private static final Pattern WITH_ACCESSORY_PATTERN = Pattern.compile("(?i)\\bwith\\b\\s+.+$");
    private static final Set<String> STOP_WORDS = Set.of(
        "of", "and", "for", "with", "without", "the", "a", "an", "type", "model",
        "commodity", "specification", "size", "name"
    );
    private static final Set<String> FAMILY_WORDS = Set.of(
        "plier", "pliers", "punch", "setter", "rivet", "riveter", "socket", "spanner", "wrench",
        "knife", "cup", "rope", "paint", "belt"
    );

    public NormalizedMaterialName normalize(String rawName, String rawSpec, String packing) {
        String source = first(rawName, rawSpec);
        List<MaterialNameAttribute> attributes = new ArrayList<>();
        List<String> lines = lines(source);
        String clean = lines.isEmpty() ? "" : lines.get(0);

        Matcher sizeMatcher = SIZE_PATTERN.matcher(source == null ? "" : source);
        List<String> sizes = new ArrayList<>();
        while (sizeMatcher.find()) {
            sizes.add(normalizeSize(sizeMatcher.group()));
        }
        if (!sizes.isEmpty()) {
            String value = String.join(" / ", sizes);
            attributes.add(new MaterialNameAttribute("size", "Size", value, null, value));
            clean = SIZE_PATTERN.matcher(clean).replaceAll(" ");
        }

        Matcher materialMatcher = MATERIAL_PATTERN.matcher(source == null ? "" : source);
        if (materialMatcher.find()) {
            String material = materialMatcher.group(1).trim();
            attributes.add(new MaterialNameAttribute("material", "Material", material, null, materialMatcher.group()));
            clean = MATERIAL_PATTERN.matcher(clean).replaceAll(" ");
        }
        addDetailLineAttributes(lines.size() > 1 ? lines.subList(1, lines.size()) : List.of(), attributes);

        Matcher accessoryMatcher = WITH_ACCESSORY_PATTERN.matcher(clean);
        if (accessoryMatcher.find()) {
            addAttributeIfAbsent(attributes, "detail", "Detail", accessoryMatcher.group().trim(), accessoryMatcher.group().trim());
            clean = accessoryMatcher.replaceAll(" ");
        }

        if (rawSpec != null && !rawSpec.isBlank() && !Objects.equals(rawSpec.trim(), rawName == null ? null : rawName.trim())) {
            attributes.add(new MaterialNameAttribute("specification", "Specification", rawSpec.trim(), null, rawSpec.trim()));
        }
        if (packing != null && !packing.isBlank()) {
            attributes.add(new MaterialNameAttribute("packing", "Packing", packing.trim(), null, packing.trim()));
        }

        clean = clean
            .replaceAll(ATTRIBUTE_TAIL_PATTERN.pattern(), " ")
            .replaceAll("(?i)\\b(?:Name\\s+of\\s+Commodity|Specification)\\b", " ")
            .replaceAll("[,;\\uFF0C\\uFF1B()\\uFF08\\uFF09\\[\\]]+", " ")
            .replaceAll("(?i)\\b[x]\\b", " ")
            .replaceAll("\\s*/\\s*$", " ")
            .replaceAll("\\s+", " ")
            .trim();

        List<String> riskFlags = riskFlags(source, clean);
        List<String> nameTokens = tokens(clean);
        List<String> specTokens = attributes.stream()
            .flatMap(attribute -> tokens(attribute.value()).stream())
            .distinct()
            .toList();
        String cleanName = clean.isBlank() ? null : clean;
        return new NormalizedMaterialName(cleanName, cleanName, List.copyOf(attributes), nameTokens, specTokens, riskFlags);
    }

    private List<String> lines(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        return Arrays.stream(source.split("\\R+"))
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();
    }

    private String normalizeSize(String value) {
        return value
            .replace("''", "\"")
            .replace("'", "\"")
            .replaceAll("(?i)mm\\b", "mm")
            .replaceAll("(?i)cm\\b", "cm")
            .replaceAll("(?i)ml\\b", "ml")
            .replaceAll("(?i)l\\b", "L")
            .replaceAll("(?i)kg\\b", "kg")
            .replaceAll("(?i)g\\b", "g")
            .replaceAll("(?i)pcs\\b", "PCS")
            .replaceAll("(?i)ctn\\b", "CTN")
            .replaceAll("(?i)box\\b", "BOX")
            .replaceAll("(?i)m\\b", "m")
            .replaceAll("\\s*/\\s*", " / ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private void addDetailLineAttributes(List<String> detailLines, List<MaterialNameAttribute> attributes) {
        for (String line : detailLines) {
            String value = line.trim();
            if (value.isBlank() || hasRawText(attributes, value)) {
                continue;
            }
            String lower = value.toLowerCase(Locale.ROOT);
            Matcher materialMatcher = MATERIAL_PATTERN.matcher(value);
            if (materialMatcher.find()) {
                addAttributeIfAbsent(attributes, "material", "Material", materialMatcher.group(1).trim(), value);
            } else if (lower.contains("finish") || lower.contains("plated") || lower.contains("painted")) {
                addAttributeIfAbsent(attributes, "finish", "Finish", value, value);
            } else if (lower.contains("handle")) {
                addAttributeIfAbsent(attributes, "handle", "Handle", value, value);
            } else if (lower.contains("color") || lower.contains("colour")) {
                addAttributeIfAbsent(attributes, "color", "Color", value, value);
            } else if (lower.contains("forged") || lower.contains("hardened")) {
                addAttributeIfAbsent(attributes, "process", "Process", value, value);
            } else {
                addAttributeIfAbsent(attributes, "detail", "Detail", value, value);
            }
        }
    }

    private void addAttributeIfAbsent(List<MaterialNameAttribute> attributes, String key, String name, String value, String rawText) {
        if (value == null || value.isBlank()) {
            return;
        }
        boolean exists = attributes.stream()
            .anyMatch(attribute -> key.equals(attribute.key()) && Objects.equals(value, attribute.value()));
        if (!exists) {
            attributes.add(new MaterialNameAttribute(key, name, value, null, rawText));
        }
    }

    private boolean hasRawText(List<MaterialNameAttribute> attributes, String rawText) {
        return attributes.stream().anyMatch(attribute -> Objects.equals(rawText, attribute.rawText()));
    }

    private List<String> riskFlags(String source, String cleanName) {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        Set<String> flags = new LinkedHashSet<>();
        String lower = source.toLowerCase(Locale.ROOT);
        if (lower.contains(" kit") || lower.endsWith("kit") || lower.contains(" set") || lower.endsWith("set")
            || lower.contains("with accessories") || lower.contains("with rivet") || lower.contains("with extension")) {
            flags.add("KIT_OR_SET");
        }
        long familyCount = FAMILY_WORDS.stream().filter(lower::contains).count();
        if (familyCount >= 3 || cleanName.contains("+")) {
            flags.add("MULTI_PRODUCT_FAMILY");
        }
        long sizeGroups = SIZE_PATTERN.matcher(source).results().count();
        if (sizeGroups >= 3) {
            flags.add("MULTIPLE_SIZE_GROUPS");
        }
        return List.copyOf(flags);
    }

    public List<String> tokens(String value) {
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

    private String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
