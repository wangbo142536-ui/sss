package com.zswy.shipsupply.procurement.materials;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public class XlsxMaterialQuoteParser {

    static final String SOURCE_FORMAT_STANDARD_FQ = "STANDARD_FQ";
    static final String SOURCE_FORMAT_SKU_RFQ = "SKU_RFQ";
    static final String SOURCE_FORMAT_SUPPLIER_QUOTATION = "SUPPLIER_QUOTATION";
    static final String SOURCE_FORMAT_UNKNOWN = "UNKNOWN";

    private static final Set<String> DEMAND_HEADERS = Set.of(
        "IMPA", "DESCRIPTION", "SIZEMODEL", "INQUIRYQTY", "UNIT"
    );
    private static final Set<String> SUPPLIER_HEADERS = Set.of(
        "PICTURES", "NAMEOFCOMMODITYSPECIFICATION", "ITEMNO"
    );
    private static final Set<String> SKU_RFQ_DESCRIPTION_HEADERS = Set.of(
        "NAMEOFCOMMODITYSPECIFICATION", "PRODUCTNAME", "DESCRIPTION"
    );
    private static final Set<String> SKU_RFQ_QUANTITY_HEADERS = Set.of(
        "REQUESTQTY", "QTY", "QUANTITY"
    );
    private static final Set<String> SKU_RFQ_UNIT_HEADERS = Set.of("UNIT");

    private static final List<String> IMPA_ALIASES = List.of(
        "IMPA", "IMPA/Platform Code", "Platform Code", "平台编码", "IMPA编码"
    );
    private static final List<String> DESCRIPTION_ALIASES = List.of(
        "DESCRIPTION", "Name of Commodity & Specification", "商品描述", "商品名称", "Product Name", "Description"
    );
    private static final List<String> SIZE_MODEL_ALIASES = List.of(
        "Size/Model", "Specification", "规格型号"
    );
    private static final List<String> QUANTITY_ALIASES = List.of(
        "Inquiry Qty.", "Request Qty", "Qty", "Quantity", "数量"
    );
    private static final List<String> UNIT_ALIASES = List.of("UNIT", "Unit", "单位");
    private static final List<String> SUPPLIER_ITEM_ALIASES = List.of(
        "Supplier SKU Ref", "Supplier SKU Code", "Item No.", "供货商SKU", "供应商编码"
    );
    private static final List<String> RAW_NAME_SPEC_ALIASES = List.of(
        "Name of Commodity & Specification", "DESCRIPTION", "商品描述", "商品名称", "Product Name", "Description"
    );
    private static final List<String> PRICE_ALIASES = List.of(
        "FOB WAREHOUSE(RMB)", "FOB WAREHOUSE", "Price", "Unit Price", "单价"
    );
    private static final List<String> PACKING_ALIASES = List.of("Packing", "包装");
    private static final List<String> STOCK_ALIASES = List.of("STOCK", "Stock", "库存");
    private static final List<String> REMARK_ALIASES = List.of("Remarks", "Remark", "备注");

    public MaterialParsedDocument parse(Path xlsxFile) throws IOException {
        try (ZipFile zipFile = new ZipFile(xlsxFile.toFile())) {
            List<String> sharedStrings = readSharedStrings(zipFile);
            Map<Integer, ImageAnchor> imageAnchorsByRow = readImageAnchors(zipFile);
            Document sheet = readXml(zipFile, "xl/worksheets/sheet1.xml");
            return readRows(sheet, sharedStrings, imageAnchorsByRow);
        }
    }

    private List<String> readSharedStrings(ZipFile zipFile) throws IOException {
        ZipEntry entry = zipFile.getEntry("xl/sharedStrings.xml");
        if (entry == null) {
            return List.of();
        }
        Document document = readXml(zipFile, entry);
        NodeList stringItems = document.getElementsByTagNameNS("*", "si");
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < stringItems.getLength(); i++) {
            strings.add(textFromDescendants((Element) stringItems.item(i), "t"));
        }
        return strings;
    }

    private Map<Integer, ImageAnchor> readImageAnchors(ZipFile zipFile) throws IOException {
        ZipEntry entry = zipFile.getEntry("xl/drawings/drawing1.xml");
        if (entry == null) {
            return Map.of();
        }
        Document document = readXml(zipFile, entry);
        Map<String, String> mediaPathByRelId = readDrawingRelationships(zipFile);
        Map<Integer, ImageAnchor> anchors = new HashMap<>();
        int imageIndex = 1;
        NodeList allNodes = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < allNodes.getLength(); i++) {
            Node node = allNodes.item(i);
            if (!(node instanceof Element element)) {
                continue;
            }
            String localName = element.getLocalName();
            if (!"twoCellAnchor".equals(localName) && !"oneCellAnchor".equals(localName)) {
                continue;
            }
            Integer rowNumber = firstIntegerText(element, "row");
            Integer columnNumber = firstIntegerText(element, "col");
            if (rowNumber == null) {
                continue;
            }
            int excelRowNumber = rowNumber + 1;
            int excelColumnNumber = columnNumber == null ? 1 : columnNumber + 1;
            String relId = firstEmbedRelationshipId(element);
            String mediaPath = relId == null ? null : mediaPathByRelId.get(relId);
            anchors.putIfAbsent(
                excelRowNumber,
                new ImageAnchor(
                    imageIndex,
                    "sheet1!R" + excelRowNumber + "C" + excelColumnNumber,
                    mediaPath,
                    contentType(mediaPath)
                )
            );
            imageIndex++;
        }
        return anchors;
    }

    private Map<String, String> readDrawingRelationships(ZipFile zipFile) throws IOException {
        ZipEntry entry = zipFile.getEntry("xl/drawings/_rels/drawing1.xml.rels");
        if (entry == null) {
            return Map.of();
        }
        Document document = readXml(zipFile, entry);
        NodeList relationships = document.getElementsByTagNameNS("*", "Relationship");
        Map<String, String> mediaPathByRelId = new HashMap<>();
        for (int i = 0; i < relationships.getLength(); i++) {
            Element relationship = (Element) relationships.item(i);
            String id = relationship.getAttribute("Id");
            String target = relationship.getAttribute("Target");
            if (id.isBlank() || target.isBlank() || !target.contains("media/")) {
                continue;
            }
            mediaPathByRelId.put(id, normalizeMediaPath(target));
        }
        return mediaPathByRelId;
    }

    private MaterialParsedDocument readRows(
        Document sheet,
        List<String> sharedStrings,
        Map<Integer, ImageAnchor> imageAnchorsByRow
    ) {
        NodeList rowNodes = sheet.getElementsByTagNameNS("*", "row");
        Map<String, String> headersByColumn = new LinkedHashMap<>();
        String documentType = MaterialMatchPreviewService.UNKNOWN;
        String sourceFormat = SOURCE_FORMAT_UNKNOWN;
        int headerRowNumber = 0;
        List<MaterialQuoteRow> rows = new ArrayList<>();

        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element rowElement = (Element) rowNodes.item(i);
            int rowNumber = parseInt(rowElement.getAttribute("r"), i + 1);
            Map<String, String> valuesByColumn = readCells(rowElement, sharedStrings);
            if (valuesByColumn.values().stream().allMatch(String::isBlank)) {
                continue;
            }

            if (headersByColumn.isEmpty()) {
                DetectedDocument detected = detectDocument(valuesByColumn);
                if (detected == null) {
                    continue;
                }
                headersByColumn.putAll(valuesByColumn);
                documentType = detected.documentType();
                sourceFormat = detected.sourceFormat();
                headerRowNumber = rowNumber;
                continue;
            }
            if (isEndRow(valuesByColumn)) {
                break;
            }

            Map<String, String> rawColumns = rawColumns(headersByColumn, valuesByColumn);
            if (!hasDetailValues(documentType, rawColumns)) {
                continue;
            }
            int sequence = rows.size() + 1;
            ImageAnchor imageAnchor = imageAnchorsByRow.get(rowNumber);
            if (imageAnchor != null) {
                rawColumns.put("_imageIndex", String.valueOf(imageAnchor.index()));
                rawColumns.put("_imageAnchor", imageAnchor.anchor());
                if (imageAnchor.mediaPath() != null) {
                    rawColumns.put("_imageMediaPath", imageAnchor.mediaPath());
                }
                if (imageAnchor.contentType() != null) {
                    rawColumns.put("_imageContentType", imageAnchor.contentType());
                }
            }
            String description = valueAny(rawColumns, DESCRIPTION_ALIASES);
            String packing = valueAny(rawColumns, PACKING_ALIASES);
            String sizeModel = valueAny(rawColumns, SIZE_MODEL_ALIASES);
            if (sizeModel.isBlank() && SOURCE_FORMAT_SKU_RFQ.equals(sourceFormat)) {
                sizeModel = packing;
            }
            rows.add(new MaterialQuoteRow(
                documentType,
                sourceFormat,
                headerRowNumber,
                sequence,
                rowNumber,
                rawColumns,
                valueAny(rawColumns, IMPA_ALIASES),
                description,
                sizeModel,
                valueAny(rawColumns, QUANTITY_ALIASES),
                valueAny(rawColumns, UNIT_ALIASES),
                valueAny(rawColumns, REMARK_ALIASES),
                valueAny(rawColumns, SUPPLIER_ITEM_ALIASES),
                valueAny(rawColumns, RAW_NAME_SPEC_ALIASES),
                valueAny(rawColumns, PRICE_ALIASES),
                packing,
                valueAny(rawColumns, STOCK_ALIASES),
                imageAnchor != null,
                imageAnchor == null ? null : imageAnchor.index(),
                imageAnchor == null ? null : imageAnchor.anchor(),
                imageAnchor == null ? null : imageAnchor.mediaPath(),
                imageAnchor == null ? null : imageAnchor.contentType()
            ));
        }
        return new MaterialParsedDocument(documentType, sourceFormat, headerRowNumber, rows);
    }

    private DetectedDocument detectDocument(Map<String, String> valuesByColumn) {
        Set<String> normalizedHeaders = valuesByColumn.values().stream()
            .map(this::normalizeHeader)
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.toSet());
        if (normalizedHeaders.containsAll(DEMAND_HEADERS)) {
            return new DetectedDocument(MaterialMatchPreviewService.DEMAND_INQUIRY, SOURCE_FORMAT_STANDARD_FQ);
        }
        if (normalizedHeaders.containsAll(SUPPLIER_HEADERS)) {
            return new DetectedDocument(MaterialMatchPreviewService.SUPPLIER_QUOTATION, SOURCE_FORMAT_SUPPLIER_QUOTATION);
        }
        if (hasAny(normalizedHeaders, SKU_RFQ_DESCRIPTION_HEADERS)
            && hasAny(normalizedHeaders, SKU_RFQ_QUANTITY_HEADERS)
            && hasAny(normalizedHeaders, SKU_RFQ_UNIT_HEADERS)) {
            return new DetectedDocument(MaterialMatchPreviewService.DEMAND_INQUIRY, SOURCE_FORMAT_SKU_RFQ);
        }
        return null;
    }

    private Map<String, String> rawColumns(
        Map<String, String> headersByColumn,
        Map<String, String> valuesByColumn
    ) {
        Map<String, String> rawColumns = new LinkedHashMap<>();
        for (Map.Entry<String, String> headerEntry : headersByColumn.entrySet()) {
            String header = headerEntry.getValue();
            if (header == null || header.isBlank()) {
                continue;
            }
            rawColumns.put(header, valuesByColumn.getOrDefault(headerEntry.getKey(), ""));
        }
        return rawColumns;
    }

    private boolean hasDetailValues(String documentType, Map<String, String> rawColumns) {
        if (MaterialMatchPreviewService.DEMAND_INQUIRY.equals(documentType)) {
            return !valueAny(rawColumns, IMPA_ALIASES).isBlank()
                || !valueAny(rawColumns, DESCRIPTION_ALIASES).isBlank()
                || !valueAny(rawColumns, SIZE_MODEL_ALIASES).isBlank()
                || !valueAny(rawColumns, QUANTITY_ALIASES).isBlank();
        }
        if (MaterialMatchPreviewService.SUPPLIER_QUOTATION.equals(documentType)) {
            return !valueAny(rawColumns, SUPPLIER_ITEM_ALIASES).isBlank()
                || !valueAny(rawColumns, RAW_NAME_SPEC_ALIASES).isBlank();
        }
        return rawColumns.values().stream().anyMatch(value -> !value.isBlank());
    }

    private boolean isEndRow(Map<String, String> valuesByColumn) {
        return valuesByColumn.values().stream()
            .map(value -> value == null ? "" : value)
            .anyMatch(value -> value.contains("杂费项目")
                || value.toUpperCase(Locale.ROOT).contains("MISCELLANEOUS"));
    }

    private Map<String, String> readCells(Element rowElement, List<String> sharedStrings) {
        Map<String, String> values = new LinkedHashMap<>();
        NodeList cellNodes = rowElement.getElementsByTagNameNS("*", "c");
        for (int i = 0; i < cellNodes.getLength(); i++) {
            Element cell = (Element) cellNodes.item(i);
            String reference = cell.getAttribute("r");
            String column = columnLetters(reference);
            if (column.isBlank()) {
                continue;
            }
            values.put(column, readCellValue(cell, sharedStrings));
        }
        return values;
    }

    private String readCellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return textFromDescendants(cell, "t").trim();
        }
        String value = firstText(cell, "v");
        if ("s".equals(type)) {
            int index = parseInt(value, -1);
            if (index >= 0 && index < sharedStrings.size()) {
                return sharedStrings.get(index).trim();
            }
            return "";
        }
        return value.trim();
    }

    private Document readXml(ZipFile zipFile, String entryName) throws IOException {
        ZipEntry entry = zipFile.getEntry(entryName);
        if (entry == null) {
            throw new IOException("Missing XLSX entry: " + entryName);
        }
        return readXml(zipFile, entry);
    }

    private Document readXml(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            return documentBuilder().parse(inputStream);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException("Failed to parse XLSX XML entry: " + entry.getName(), ex);
        }
    }

    private DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder();
    }

    private String value(Map<String, String> values, String key) {
        return values.getOrDefault(key, "");
    }

    private String valueAny(Map<String, String> values, List<String> aliases) {
        for (String alias : aliases) {
            String value = values.getOrDefault(alias, "");
            if (!value.isBlank()) {
                return value;
            }
        }
        Map<String, String> valuesByNormalizedHeader = new HashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            valuesByNormalizedHeader.put(normalizeHeader(entry.getKey()), entry.getValue());
        }
        for (String alias : aliases) {
            String value = valuesByNormalizedHeader.getOrDefault(normalizeHeader(alias), "");
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean hasAny(Set<String> normalizedHeaders, Set<String> candidates) {
        return candidates.stream().anyMatch(normalizedHeaders::contains);
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&amp;", "&")
            .replaceAll("[^A-Za-z0-9]", "")
            .toUpperCase(Locale.ROOT);
    }

    private String columnLetters(String reference) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < reference.length(); i++) {
            char ch = reference.charAt(i);
            if (Character.isLetter(ch)) {
                builder.append(ch);
            } else {
                break;
            }
        }
        return builder.toString();
    }

    private String firstText(Element element, String localName) {
        NodeList nodes = element.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent();
    }

    private Integer firstIntegerText(Element element, String localName) {
        String value = firstText(element, localName);
        if (value.isBlank()) {
            return null;
        }
        return parseInt(value, -1);
    }

    private String firstEmbedRelationshipId(Element element) {
        NodeList nodes = element.getElementsByTagNameNS("*", "blip");
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element blip) {
                String embed = blip.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed");
                if (!embed.isBlank()) {
                    return embed;
                }
            }
        }
        return null;
    }

    private String textFromDescendants(Element element, String localName) {
        NodeList nodes = element.getElementsByTagNameNS("*", localName);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < nodes.getLength(); i++) {
            builder.append(nodes.item(i).getTextContent());
        }
        return builder.toString();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String normalizeMediaPath(String target) {
        String normalized = target.replace("\\", "/");
        if (normalized.startsWith("../")) {
            normalized = normalized.substring(3);
        }
        if (!normalized.startsWith("xl/")) {
            normalized = "xl/" + normalized;
        }
        return normalized;
    }

    private String contentType(String mediaPath) {
        if (mediaPath == null) {
            return null;
        }
        String lower = mediaPath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private record ImageAnchor(int index, String anchor, String mediaPath, String contentType) {
    }

    private record DetectedDocument(String documentType, String sourceFormat) {
    }
}
