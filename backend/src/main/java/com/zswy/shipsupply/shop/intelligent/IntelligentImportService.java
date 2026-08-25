package com.zswy.shipsupply.shop.intelligent;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.zswy.shipsupply.auth.AuthRepository;
import com.zswy.shipsupply.auth.CurrentUserContext;
import com.zswy.shipsupply.auth.CurrentUserService;
import com.zswy.shipsupply.procurement.materials.XlsxMaterialQuoteParser;
import com.zswy.shipsupply.procurement.materials.XlsxMaterialQuoteParser.GenericProductRow;
import com.zswy.shipsupply.procurement.materials.XlsxMaterialQuoteParser.GenericSheetDocument;
import com.zswy.shipsupply.shop.ShopImportRecognitionService;
import com.zswy.shipsupply.shop.ShopIntelligentImportAnalysis;
import com.zswy.shipsupply.shop.ShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IntelligentImportService {

    private static final Logger log = LoggerFactory.getLogger(IntelligentImportService.class);
    private static final int CHUNK_SIZE = 25;
    private static final Set<String> FOOD_SHEET_WORDS = Set.of(
        "伙食", "食品", "调味", "食用油", "米", "面", "粉", "杂粮", "饮料", "乳制品", "生鲜", "冷冻", "provision", "food", "beverage"
    );
    private static final Set<String> MATERIAL_SHEET_WORDS = Set.of(
        "物料", "备件", "日用百货", "文具", "工具", "material", "spare", "stores"
    );
    private static final Set<String> FOOD_ITEM_WORDS = Set.of(
        "调味酱", "食用油", "大米", "面粉", "牛奶", "饮料", "咖啡", "蔬菜", "水果", "罐头食品", "食品"
    );

    private final CurrentUserService currentUserService;
    private final ShopRepository shopRepository;
    private final IntelligentImportRepository repository;
    private final XlsxMaterialQuoteParser parser;
    private final AuthRepository authRepository;
    private final ShopImportModelAnalyzer modelAnalyzer;
    private final ShopImportRecognitionService recognitionService;

    public IntelligentImportService(
        CurrentUserService currentUserService,
        ShopRepository shopRepository,
        IntelligentImportRepository repository,
        XlsxMaterialQuoteParser parser,
        AuthRepository authRepository,
        ShopImportModelAnalyzer modelAnalyzer,
        ShopImportRecognitionService recognitionService
    ) {
        this.currentUserService = currentUserService;
        this.shopRepository = shopRepository;
        this.repository = repository;
        this.parser = parser;
        this.authRepository = authRepository;
        this.modelAnalyzer = modelAnalyzer;
        this.recognitionService = recognitionService;
    }

    @Transactional
    public IntelligentImportStartResponse start(String authorization, MultipartFile file) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorization);
        validate(file);
        String jobId = UUID.randomUUID().toString();
        Path storedFile = storeSource(jobId, file);
        long shopId = shopRepository.ensureShop(user.companyId(), user.userId());
        String hash = sha256(storedFile);
        long batchId = repository.createBatch(
            user.companyId(), shopId, user.userId(), jobId,
            safeFileName(file.getOriginalFilename()), hash
        );
        ImportCounters empty = ImportCounters.empty();
        repository.progress(batchId, "QUEUED", "UPLOAD", 8, null, 0, 0, empty, null);
        CompletableFuture.runAsync(() -> process(batchId, jobId, user, shopId, storedFile));
        return new IntelligentImportStartResponse(jobId, batchId, "QUEUED");
    }

    public IntelligentImportJobResponse status(String authorization, String jobId) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorization);
        ImportBatchRow batch = repository.findBatch(user.companyId(), jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INTELLIGENT_IMPORT_JOB_NOT_FOUND"));
        IntelligentImportPreview preview = null;
        if (Set.of("PREVIEW_READY", "COMPLETED", "PARTIAL").contains(batch.status())) {
            List<IntelligentImportPreviewRow> rows = repository.previewRows(user.companyId(), batch.batchId());
            int exceptions = (int) rows.stream().filter(row -> "PENDING_EXCEPTION".equals(row.codeStatus())).count();
            preview = new IntelligentImportPreview(
                batch.batchId(), "PREVIEW_READY", rows.size(), rows.size() - exceptions, exceptions, rows
            );
        }
        return response(batch, preview);
    }

    public IntelligentImportStartResponse execute(
        String authorization,
        String jobId,
        IntelligentImportExecutionRequest request
    ) {
        CurrentUserContext user = currentUserService.requireActiveCompanyUser(authorization);
        ImportBatchRow batch = repository.findBatch(user.companyId(), jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INTELLIGENT_IMPORT_JOB_NOT_FOUND"));
        if (!Set.of("PREVIEW_READY", "PARTIAL", "COMPLETED").contains(batch.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INTELLIGENT_IMPORT_PREVIEW_NOT_READY");
        }
        long shopId = shopRepository.ensureShop(user.companyId(), user.userId());
        CompletableFuture.runAsync(() -> executeRows(batch, user, shopId, request == null ? List.of() : request.previewRowIds()));
        return new IntelligentImportStartResponse(jobId, batch.batchId(), "RUNNING");
    }

    private void process(long batchId, String jobId, CurrentUserContext user, long shopId, Path source) {
        Instant started = Instant.now();
        ImportCounters counters = ImportCounters.empty();
        try {
            List<String> sheets = parser.sheetNames(source);
            boolean officialTemplate = parser.isOfficialShopTemplate(source)
                && sheets.equals(List.of("物料", "伙食"));
            if (!officialTemplate) modelAnalyzer.requireConfiguredForNonOfficial();
            counters = replace(counters, sheets.size(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            repository.progress(batchId, "RUNNING", "WORKBOOK_PARSE", 12, null, 0, 0, counters, null);
            List<GenericSheetDocument> documents = new ArrayList<>();
            int totalRows = 0;
            int imageTotal = 0;
            for (int index = 0; index < sheets.size(); index++) {
                String sheetName = sheets.get(index);
                GenericSheetDocument document;
                if (officialTemplate) {
                    document = parser.parseOfficialShopTemplate(source, sheetName);
                } else {
                    SheetStructureMapping mapping = modelAnalyzer.analyzeStructure(
                        sheetName,
                        parser.sampleGenericSheet(source, sheetName, 16)
                    );
                    document = parser.parseMappedGeneric(
                        source, sheetName, mapping.headerRowNumber(), mapping.dataStartRowNumber(), mapping.columns()
                    );
                }
                documents.add(document);
                totalRows += document.rows().size();
                imageTotal += document.imageCount();
                counters = replace(counters, sheets.size(), index + 1, totalRows, 0, 0, 0, 0, 0, 0, imageTotal, 0, 0);
                repository.progress(
                    batchId, "RUNNING", "WORKBOOK_PARSE", 12 + percent(index + 1, sheets.size(), 18),
                    sheetName, index + 1, sheets.size(), counters, null
                );
            }
            int totalChunks = Math.max(1, (totalRows + CHUNK_SIZE - 1) / CHUNK_SIZE);
            counters = replace(counters, sheets.size(), sheets.size(), totalRows, 0, 0, 0, 0, 0, 0, imageTotal, 0, 0);
            repository.progress(batchId, "RUNNING", "MODEL_RECOGNITION", 32, null, 0, totalChunks, counters, null);

            int processed = 0;
            int materialCount = 0;
            int foodCount = 0;
            int impaMatched = 0;
            int categoryMatched = 0;
            int pending = 0;
            int imagesProcessed = 0;
            int failed = 0;
            for (GenericSheetDocument document : documents) {
                Map<String, ShopIntelligentImportAnalysis> deterministicAnalyses = new LinkedHashMap<>();
                for (GenericProductRow row : document.rows()) {
                    deterministicAnalyses.put(
                        row.sourceItemId(),
                        recognitionService.analyzeForIntelligentImport(
                            row.productName(), row.specification(), row.packing(), row.rawStandardCode()
                        )
                    );
                }
                List<GenericProductRow> rowsNeedingModel = document.rows().stream()
                    .filter(row -> repository.findImpa(row.rawStandardCode()).isEmpty())
                    .filter(row -> {
                        ShopIntelligentImportAnalysis analysis = deterministicAnalyses.get(row.sourceItemId());
                        return analysis == null || analysis.reviewRequired();
                    })
                    .toList();
                Map<String, ModelAnalysis> modelResults = officialTemplate
                    ? Map.of()
                    : modelAnalyzer.analyze(rowsNeedingModel, deterministicAnalyses);
                for (GenericProductRow row : document.rows()) {
                    try {
                        ModelAnalysis modelResult = modelResults.get(row.sourceItemId());
                        ShopIntelligentImportAnalysis deterministic = deterministicAnalyses.get(row.sourceItemId());
                        String productType = validProductType(modelResult == null ? null : modelResult.productType())
                            ? modelResult.productType().toUpperCase(Locale.ROOT)
                            : classifyProductType(document.sheetName(), row.productName());
                        if ("FOOD".equals(productType)) foodCount++; else materialCount++;
                        ResolvedImportRow resolved = resolve(source, batchId, user.userId(), productType, row, modelResult, deterministic);
                        if (resolved.impaItemId() != null) impaMatched++;
                        if ("FOOD".equals(productType) && resolved.standardCategoryCode() != null) categoryMatched++;
                        if (resolved.reviewRequired()) pending++;
                        if (row.imageMediaPath() != null) imagesProcessed++;
                        repository.savePreview(batchId, user.companyId(), shopId, resolved);
                    } catch (Exception rowError) {
                        log.warn("INTELLIGENT_IMPORT_ROW_FAILED jobId={} sourceItemId={}", jobId, row.sourceItemId(), rowError);
                        throw new IllegalStateException("INTELLIGENT_IMPORT_ROW_FAILED:" + row.sourceItemId(), rowError);
                    }
                    processed++;
                    int currentChunk = Math.min(totalChunks, (processed + CHUNK_SIZE - 1) / CHUNK_SIZE);
                    counters = replace(
                        counters, sheets.size(), sheets.size(), totalRows, processed, materialCount, foodCount,
                        impaMatched, categoryMatched, pending, imageTotal, imagesProcessed, failed
                    );
                    if (processed % CHUNK_SIZE == 0 || processed == totalRows) {
                        repository.progress(
                            batchId, "RUNNING", processed < totalRows ? "MODEL_RECOGNITION" : "STANDARD_MATCH",
                            32 + percent(processed, totalRows, 55), document.sheetName(), currentChunk, totalChunks,
                            counters, null
                        );
                    }
                }
            }
            repository.progress(batchId, "RUNNING", "PREVIEW_BUILD", 94, null, totalChunks, totalChunks, counters, null);
            if (processed != totalRows || repository.previewRowCount(batchId) != totalRows) {
                throw new IllegalStateException("INTELLIGENT_IMPORT_COUNT_MISMATCH");
            }
            String finalStatus = failed > 0 ? "PARTIAL" : "PREVIEW_READY";
            repository.progress(batchId, finalStatus, "PREVIEW_BUILD", 100, null, totalChunks, totalChunks, counters, null);
            log.info(
                "INTELLIGENT_IMPORT_COMPLETED jobId={} batchId={} rows={} pending={} failed={} durationMs={}",
                jobId, batchId, totalRows, pending, failed, Duration.between(started, Instant.now()).toMillis()
            );
        } catch (Exception error) {
            log.error("INTELLIGENT_IMPORT_FAILED jobId={} batchId={}", jobId, batchId, error);
            repository.progress(batchId, "FAILED", currentStage(error), 100, null, 0, 0, counters, safeError(error));
        } finally {
            try {
                Files.deleteIfExists(source);
            } catch (IOException ignored) {
            }
        }
    }

    private void executeRows(ImportBatchRow batch, CurrentUserContext user, long shopId, List<Long> selectedIds) {
        ImportCounters counters = batch.counters();
        try {
            repository.progress(batch.batchId(), "RUNNING", "SKU_CREATE", 12, null, 0, 4, counters, null);
            ExecutionResult result = repository.execute(user.companyId(), user.userId(), shopId, batch.batchId(), selectedIds);
            repository.progress(batch.batchId(), "RUNNING", "STANDARD_LINK", 48, null, 2, 4, counters, null);
            repository.progress(batch.batchId(), "RUNNING", "DETAIL_IMAGE_SAVE", 78, null, 3, 4, counters, null);
            String finalStatus = result.pending() > 0 ? "PARTIAL" : "COMPLETED";
            repository.progress(batch.batchId(), finalStatus, "COUNT_RECONCILIATION", 100, null, 4, 4, counters, null);
        } catch (Exception error) {
            repository.progress(batch.batchId(), "FAILED", "COUNT_RECONCILIATION", 100, null, 4, 4, counters, safeError(error));
        }
    }

    private ResolvedImportRow resolve(
        Path source,
        long batchId,
        long userId,
        String productType,
        GenericProductRow row,
        ModelAnalysis model,
        ShopIntelligentImportAnalysis deterministic
    ) {
        ResolvedStandardItem standard = null;
        ProvisionCategory provision = null;
        String explicitCode = emptyToNull(row.rawStandardCode());
        String deterministicCode = deterministic == null ? null : emptyToNull(deterministic.recommendedImpaCode());
        String modelCode = validatedModelCode(model, deterministic);
        String productName = first(
            validModelText(model) ? model.productName() : null,
            deterministic == null ? null : deterministic.cleanName(),
            row.productName()
        );
        String specification = first(
            validModelText(model) ? model.specification() : null,
            deterministic == null ? null : deterministic.specification(),
            row.specification()
        );
        if ("MATERIAL".equals(productType)) {
            standard = repository.findImpa(explicitCode)
                .or(() -> repository.findImpa(deterministicCode))
                .or(() -> repository.findImpa(modelCode))
                .orElse(null);
        } else {
            String modelCategory = model == null ? null : emptyToNull(model.provisionCategoryCode());
            provision = repository.provisionCategory(modelCategory == null ? provisionCode(row.sourceSheet(), productName) : modelCategory);
        }
        ImageResult image = importImage(source, row, userId, batchId);
        boolean review = "MATERIAL".equals(productType) ? standard == null : provision == null;
        String modelCategoryCode = validatedModelCategory(model, deterministic);
        String categoryCode = standard != null
            ? standard.categoryCode()
            : provision != null
                ? provision.code()
                : first(modelCategoryCode, deterministic == null ? null : deterministic.categoryCode());
        String modelCategoryName = modelCategoryCode == null ? null : repository.impaCategoryName(modelCategoryCode).orElse(null);
        String categoryName = standard != null
            ? standard.categoryName()
            : provision != null
                ? provision.name()
                : first(modelCategoryName, deterministic == null ? null : deterministic.categoryName());
        Map<String, String> raw = new LinkedHashMap<>(row.rawColumns());
        raw.put("_sourceItemId", row.sourceItemId());
        raw.put("_nameExtraction", validModelText(model) && emptyToNull(model.productName()) != null
            ? "MODEL" : deterministic == null ? "SOURCE" : "DETERMINISTIC");
        raw.put("_specificationExtraction", validModelText(model) && emptyToNull(model.specification()) != null
            ? "MODEL" : deterministic == null ? "SOURCE" : "DETERMINISTIC");
        raw.put("_materialKind", "MATERIAL".equals(productType)
            ? model != null && emptyToNull(model.materialKind()) != null ? model.materialKind() : materialKind(productName)
            : "NOT_APPLICABLE");
        if (model != null) raw.put("_modelConfidence", String.valueOf(model.confidence()));
        if ("MATERIAL".equals(productType)) {
            raw.put("_impaCategoryDecision", modelCategoryCode != null ? "MODEL_CONSTRAINED" : categoryCode == null ? "UNMATCHED" : "DETERMINISTIC");
            if (categoryCode != null) raw.put("_impaCategoryCode", categoryCode);
            raw.put("_exactImpaDecision", standard == null ? "PENDING_EXACT_MATCH" : "STANDARD_LIBRARY_MATCHED");
        }
        Map<String, Object> standardSnapshot = standard == null
            ? provision == null ? Map.of() : Map.of("libraryType", "PROVISION", "categoryCode", provision.code(), "categoryName", provision.name())
            : Map.of("libraryType", "IMPA", "impaItemId", standard.id(), "impaCode", standard.impaCode(), "categoryCode", standard.categoryCode());
        return new ResolvedImportRow(
            row.sourceItemId(), row.sourceSheet(), row.sourceRowNo(), productType,
            "MATERIAL".equals(productType) ? "IMPA" : "PROVISION", categoryCode,
            standard == null ? null : standard.id(), standard == null ? null : standard.impaCode(),
            categoryCode, categoryName, emptyToNull(row.supplierSkuCode()), truncate(productName, 500),
            truncate(specification, 1000), emptyToNull(row.unit()), decimal(row.price()), decimal(row.stock()),
            detectCurrency(row.rawColumns()), truncate(row.packing(), 300), truncate(row.barcode(), 120),
            image == null ? null : image.fileId(), image == null ? null : image.url(),
            review ? "PENDING_EXCEPTION" : "CODE_MATCHED",
            review ? ("MATERIAL".equals(productType) ? "IMPA_STANDARD_MATCH_REQUIRED" : "PROVISION_CATEGORY_REQUIRED") : null,
            review, standard != null ? "STANDARD_LIBRARY_MATCHED" : provision != null ? "PROVISION_CATEGORY_MATCHED" : "PENDING_REVIEW",
            Map.copyOf(raw), standardSnapshot, sha256(row.sourceItemId() + raw.toString())
        );
    }

    private ImageResult importImage(Path source, GenericProductRow row, long userId, long batchId) {
        if (row.imageMediaPath() == null || row.imageMediaPath().isBlank()) return null;
        try (ZipFile zip = new ZipFile(source.toFile())) {
            ZipEntry entry = zip.getEntry(row.imageMediaPath());
            if (entry == null) return null;
            Path targetDir = Path.of("uploads", "shop-sku-images").toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            String extension = extension(row.imageMediaPath());
            Path target = targetDir.resolve("batch-" + batchId + "-" + UUID.randomUUID() + extension);
            try (InputStream input = zip.getInputStream(entry)) {
                Files.copy(input, target);
            }
            String fileId = authRepository.insertFile(
                userId, "batch-" + batchId + "-" + row.sourceSheet() + "-" + row.sourceRowNo() + extension,
                target.toString(), emptyToNull(row.imageContentType()), Files.size(target)
            );
            return new ImageResult(fileId, "/api/files/" + fileId);
        } catch (Exception error) {
            log.warn("INTELLIGENT_IMPORT_IMAGE_FAILED sourceItemId={}", row.sourceItemId(), error);
            return null;
        }
    }

    private IntelligentImportJobResponse response(ImportBatchRow batch, IntelligentImportPreview preview) {
        int stageIndex = switch (batch.stage() == null ? "" : batch.stage()) {
            case "UPLOAD", "SKU_CREATE" -> 1;
            case "WORKBOOK_PARSE", "STANDARD_LINK" -> 2;
            case "MODEL_RECOGNITION", "DETAIL_IMAGE_SAVE" -> 3;
            case "STANDARD_MATCH", "COUNT_RECONCILIATION" -> 4;
            case "PREVIEW_BUILD" -> 5;
            default -> 1;
        };
        boolean execution = Set.of("SKU_CREATE", "STANDARD_LINK", "DETAIL_IMAGE_SAVE", "COUNT_RECONCILIATION").contains(batch.stage());
        int stageCount = execution ? 4 : 5;
        ImportCounters counts = batch.counters();
        int stageTotal = "WORKBOOK_PARSE".equals(batch.stage()) ? counts.sheetTotal() : counts.itemTotal();
        int stageProcessed = "WORKBOOK_PARSE".equals(batch.stage()) ? counts.sheetProcessed() : counts.itemProcessed();
        return new IntelligentImportJobResponse(
            batch.jobId(), batch.batchId(), batch.status(), batch.stage(), stageIndex, stageCount,
            batch.percent(), stageProcessed, stageTotal, batch.currentSheet(), batch.currentChunk(), batch.totalChunks(),
            new IntelligentImportCounts(
                counts.sheetTotal(), counts.sheetProcessed(), counts.itemTotal(), counts.itemProcessed(),
                counts.materialCount(), counts.foodCount(), counts.impaMatchedCount(), counts.categoryMatchedCount(),
                counts.pendingReviewCount(), counts.imageTotal(), counts.imageProcessed(), counts.failedCount()
            ),
            message(batch), errorCode(batch), "FAILED".equals(batch.status()),
            batch.startedAt(), batch.finishedAt(), preview
        );
    }

    private String errorCode(ImportBatchRow batch) {
        if (batch.error() == null) return null;
        if (batch.error().contains("MODEL_CONFIGURATION_REQUIRED")) return "MODEL_CONFIGURATION_REQUIRED";
        if (batch.error().contains("MODEL_RESPONSE_ROWS_MISSING")) return "MODEL_RESPONSE_ROWS_MISSING";
        if (batch.error().contains("INTELLIGENT_IMPORT_COUNT_MISMATCH")) return "INTELLIGENT_IMPORT_COUNT_MISMATCH";
        return "INTELLIGENT_IMPORT_FAILED";
    }

    private String classifyProductType(String sheetName, String productName) {
        String sheet = lower(sheetName);
        if (FOOD_SHEET_WORDS.stream().anyMatch(sheet::contains)) return "FOOD";
        if (MATERIAL_SHEET_WORDS.stream().anyMatch(sheet::contains)) return "MATERIAL";
        String name = lower(productName);
        return FOOD_ITEM_WORDS.stream().anyMatch(name::contains) ? "FOOD" : "MATERIAL";
    }

    private boolean validProductType(String value) {
        return value != null && ("MATERIAL".equalsIgnoreCase(value) || "FOOD".equalsIgnoreCase(value));
    }

    private boolean validModelText(ModelAnalysis model) {
        return model != null && model.confidence() != null && model.confidence() >= 0.80;
    }

    private String validatedModelCode(ModelAnalysis model, ShopIntelligentImportAnalysis deterministic) {
        String code = model == null ? null : emptyToNull(model.standardCode());
        if (code == null || model.confidence() == null || model.confidence() < 0.90
            || deterministic == null || deterministic.candidateCodes() == null) return null;
        String normalized = code.replaceAll("[^0-9]", "");
        return deterministic.candidateCodes().stream()
            .filter(candidate -> normalized.equals(candidate == null ? "" : candidate.replaceAll("[^0-9]", "")))
            .findFirst()
            .orElse(null);
    }

    private String validatedModelCategory(ModelAnalysis model, ShopIntelligentImportAnalysis deterministic) {
        String category = model == null ? null : emptyToNull(model.impaCategoryCode());
        if (category == null || model.confidence() == null || model.confidence() < 0.80
            || deterministic == null || deterministic.candidateCategoryCodes() == null) return null;
        String normalized = category.replaceAll("[^0-9]", "");
        if (normalized.length() > 2) normalized = normalized.substring(0, 2);
        String selected = normalized;
        return deterministic.candidateCategoryCodes().stream()
            .filter(candidate -> selected.equals(candidate == null ? "" : candidate.replaceAll("[^0-9]", "")))
            .findFirst()
            .orElse(null);
    }

    private String provisionCode(String sheetName, String productName) {
        String text = lower(sheetName + " " + productName);
        if (containsAny(text, "调味", "酱", "盐", "糖", "醋")) return "PROVISION_SEASONING";
        if (containsAny(text, "油", "米", "面", "粉", "杂粮")) return "PROVISION_GRAIN_OIL";
        if (containsAny(text, "饮料", "牛奶", "乳", "茶", "咖啡")) return "PROVISION_BEVERAGE";
        if (containsAny(text, "冻", "冰")) return "PROVISION_FROZEN";
        if (containsAny(text, "菜", "肉", "鱼", "水果", "生鲜")) return "PROVISION_FRESH";
        if (containsAny(text, "罐头", "干货")) return "PROVISION_DRY_FOOD";
        return "PROVISION_OTHER";
    }

    private String materialKind(String productName) {
        String text = lower(productName);
        return containsAny(text, "备件", "零件", "配件", "spare", "valve", "pump", "bearing") ? "SPARE_PART" : "STORE";
    }

    private void validate(MultipartFile file) {
        String name = file == null ? null : file.getOriginalFilename();
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMPORT_FILE_REQUIRED");
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMPORT_XLSX_REQUIRED");
        }
    }

    private Path storeSource(String jobId, MultipartFile file) {
        try {
            Path dir = Path.of("uploads", "shop-import-jobs").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(jobId + ".xlsx");
            file.transferTo(target);
            return target;
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMPORT_UPLOAD_STORE_FAILED", error);
        }
    }

    private ImportCounters replace(ImportCounters ignored, int sheetTotal, int sheetProcessed, int itemTotal, int itemProcessed,
                                   int material, int food, int impa, int category, int pending, int imageTotal,
                                   int imageProcessed, int failed) {
        return new ImportCounters(sheetTotal, sheetProcessed, itemTotal, itemProcessed, material, food, impa, category,
            pending, imageTotal, imageProcessed, failed);
    }

    private int percent(int current, int total, int span) {
        return total <= 0 ? span : Math.min(span, (int) Math.round((double) current / total * span));
    }

    private BigDecimal decimal(String value) {
        String text = emptyToNull(value);
        if (text == null) return null;
        try {
            String normalized = text.replaceAll("[^0-9.\\-]", "");
            return normalized.isBlank() ? null : new BigDecimal(normalized);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private String detectCurrency(Map<String, String> values) {
        String text = lower(values == null ? "" : values.toString());
        return text.contains("usd") || text.contains("美元") || text.contains("$") ? "USD" : "CNY";
    }

    private String message(ImportBatchRow batch) {
        if (batch.error() != null) return batch.error();
        return switch (batch.stage() == null ? "" : batch.stage()) {
            case "UPLOAD" -> "文件已上传，等待分析";
            case "WORKBOOK_PARSE" -> "正在解析工作簿结构、逻辑商品和图片";
            case "MODEL_RECOGNITION" -> "正在识别字段、商品类型、规格和型号";
            case "STANDARD_MATCH" -> "正在核对 IMPA 与伙食标准分类";
            case "PREVIEW_BUILD" -> "正在生成可追溯预览";
            case "SKU_CREATE" -> "正在创建独立商品记录";
            case "STANDARD_LINK" -> "正在写入标准库关联";
            case "DETAIL_IMAGE_SAVE" -> "正在保存规格、价格和图片";
            case "COUNT_RECONCILIATION" -> "正在核对数量并完成导入";
            default -> "正在处理";
        };
    }

    private String currentStage(Exception error) {
        if (error instanceof ModelConfigurationRequiredException
            || (error.getMessage() != null && error.getMessage().startsWith("MODEL_"))) {
            return "MODEL_RECOGNITION";
        }
        return error instanceof IOException ? "WORKBOOK_PARSE" : "PREVIEW_BUILD";
    }

    private String safeError(Throwable error) {
        String message = error.getMessage();
        return message == null ? error.getClass().getSimpleName() : truncate(message, 900);
    }

    private String sha256(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception error) {
            throw new IllegalArgumentException("IMPORT_HASH_FAILED", error);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalArgumentException("IMPORT_HASH_FAILED", error);
        }
    }

    private String safeFileName(String value) { return truncate(value == null ? "products.xlsx" : value, 255); }
    private String extension(String path) { int index = path.lastIndexOf('.'); return index < 0 ? ".bin" : path.substring(index); }
    private String lower(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    private boolean containsAny(String value, String... words) { for (String word : words) if (value.contains(word)) return true; return false; }
    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String first(String... values) { for (String value : values) { String result = emptyToNull(value); if (result != null) return result; } return null; }
    private String truncate(String value, int max) { if (value == null) return null; return value.length() <= max ? value : value.substring(0, max); }

    private record ImageResult(String fileId, String url) { }
}
