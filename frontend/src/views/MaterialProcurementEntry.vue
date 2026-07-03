<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import StableDateTimeInput from "@/components/StableDateTimeInput.vue";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import { t, useI18n } from "@/i18n";
import { listPublicDictionaryItems, type DictionaryItem } from "@/services/dataDictionaryService";
import { getMaterialDemandDetail, saveMaterialDemand, uploadMaterialMatchPreview } from "@/services/procurementMaterialService";
import type { MaterialDemandSaveResponse, MaterialMatchCandidate, MaterialMatchPreviewItem, MaterialMatchPreviewResponse, MaterialMatchResult } from "@/types/procurementMaterials";

const route = useRoute();
const router = useRouter();
const { language } = useI18n();
const isMaterialSearchOpen = ref(false);
const isMatchExpanded = ref(false);
const isUploading = ref(false);
const isSavingDemand = ref(false);
const isAutoSavingCompare = ref(false);
const isLoadingDemand = ref(false);
const uploadError = ref("");
const saveError = ref("");
const saveNotice = ref("");
const selectedFileName = ref("");
const selectedSourceFileId = ref("");
const searchKeyword = ref("");
const statusFilter = ref<"ALL" | "MATCHED" | "ABNORMAL">("ALL");
const expandedRowKey = ref("");
const selectedCandidateIndexes = ref<Record<string, number>>({});
const matchConfirmed = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);
const matchPreview = ref<MaterialMatchPreviewResponse | null>(null);
const demandId = ref<number | undefined>();
const demandNo = ref("");
const demandStatus = ref("");
type DemandFormKey = "applicationNo" | "inquiryNo" | "materialType" | "currency" | "recipientCompany" | "handlerName" | "handlerEmail" | "vesselName" | "supplyPortCode" | "vesselEta";
type SupplyInfoItem = {
  key: DemandFormKey;
  label: string;
  placeholder: string;
  type: "text" | "select" | "datetime" | "handler";
  required?: boolean;
};
const demandForm = ref({
  applicationNo: "",
  inquiryNo: "",
  materialType: "",
  currency: "",
  recipientCompany: "",
  handlerName: "",
  handlerEmail: "",
  vesselName: "",
  supplyPortCode: "",
  supplyPortName: "",
  vesselEta: ""
});
const invalidDemandField = ref<DemandFormKey | "">("");
const invalidMaterialRowKey = ref("");
const invalidMaterialField = ref<"quantity" | "unit" | "">("");
const demandInputRefs = ref<Partial<Record<DemandFormKey, HTMLElement>>>({});
const portOptions = ref<DictionaryItem[]>([]);
const unitOptions = ref<DictionaryItem[]>([]);
const progressOverlayVisible = ref(false);
const progressPercent = ref(0);
const progressStageIndex = ref(0);
const progressState = ref<"running" | "success" | "failed">("running");
const progressResult = ref<MaterialMatchPreviewResponse | null>(null);
let progressTimer: number | undefined;
const DEFAULT_MATERIAL_QUANTITY = "1";
const DEFAULT_MATERIAL_UNIT = "个";

const supplyInfo = computed<SupplyInfoItem[]>(() => [
  { key: "vesselName" as const, label: t("page.materials.vesselName"), placeholder: t("page.materials.vesselNamePlaceholder"), type: "text", required: true },
  { key: "inquiryNo" as const, label: t("page.materials.inquiryNo"), placeholder: t("page.materials.inquiryNoPlaceholder"), type: "text" },
  { key: "materialType" as const, label: t("page.materials.materialType"), placeholder: t("page.materials.materialTypePlaceholder"), type: "text" },
  { key: "currency" as const, label: t("page.materials.currency"), placeholder: t("page.materials.currencyPlaceholder"), type: "text" },
  { key: "supplyPortCode" as const, label: t("page.materials.supplyPort"), placeholder: t("page.materials.supplyPortPlaceholder"), type: "select", required: true },
  { key: "vesselEta" as const, label: t("page.materials.vesselEta"), placeholder: t("page.materials.vesselEtaPlaceholder"), type: "datetime", required: true },
  { key: "recipientCompany" as const, label: t("page.materials.recipientCompany"), placeholder: t("page.materials.recipientCompanyPlaceholder"), type: "text" },
  { key: "handlerName" as const, label: t("page.materials.handlerContact"), placeholder: t("page.materials.handlerNamePlaceholder"), type: "handler" }
]);

const demandRequiredFields: Array<{ key: DemandFormKey; errorKey: string }> = [
  { key: "vesselName", errorKey: "page.materials.vesselNameRequired" },
  { key: "supplyPortCode", errorKey: "page.materials.supplyPortRequired" },
  { key: "vesselEta", errorKey: "page.materials.vesselEtaRequired" }
];

const commonCategories = [
  "缆绳索具",
  "船用油漆",
  "安全防护",
  "救生消防",
  "通用五金",
  "手动作业工具",
  "电气器材",
  "焊接耗材"
];

const previewItems = computed(() => matchPreview.value?.items ?? []);
const progressStages = computed(() => [
  t("page.materials.progressUpload"),
  t("page.materials.progressParse"),
  t("page.materials.progressMatch"),
  t("page.materials.progressPreview")
]);
const progressStyle = computed(() => ({ "--progress": `${progressPercent.value}%` }));
const progressRowsLabel = computed(() => (progressResult.value ? t("page.materials.progressCountValue", { count: progressResult.value.totalRows }) : t("page.materials.progressReading")));
const progressMatchedLabel = computed(() =>
  progressResult.value
    ? t("page.materials.progressCountValue", { count: progressResult.value.exactCount + progressResult.value.similarCount })
    : t("page.materials.progressReading")
);
const progressPendingLabel = computed(() => (progressResult.value ? t("page.materials.progressCountValue", { count: progressResult.value.unmatchedCount }) : t("page.materials.progressReading")));
const demandStatusValue = computed(() => demandStatus.value.toUpperCase());
const isDemandReadonly = computed(() => ["ORDERED", "DISCARDED", "SUPPLIED"].includes(demandStatusValue.value));
const isDemandInComparisonStage = computed(() => !["", "SAVED"].includes(demandStatusValue.value));
const isDemandActionBusy = computed(() => isUploading.value || isSavingDemand.value || isAutoSavingCompare.value || isLoadingDemand.value);
const isMaterialEditDisabled = computed(() => isDemandInComparisonStage.value || isDemandReadonly.value || isDemandActionBusy.value);

const matchFilters = computed(() => [
  { value: "ALL" as const, label: `${t("common.all")} ${previewItems.value.length}` },
  { value: "MATCHED" as const, label: `${t("page.materials.validationMatched")} ${validationCounts.value.matched}` },
  { value: "ABNORMAL" as const, label: `${t("page.materials.validationAbnormal")} ${validationCounts.value.abnormal}` }
]);

const validationCounts = computed(() => ({
  matched: previewItems.value.filter((item) => validationStatus(item) === "MATCHED").length,
  abnormal: previewItems.value.filter((item) => validationStatus(item) === "ABNORMAL").length
}));

const filteredItems = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  return previewItems.value.filter((item) => {
    if (statusFilter.value !== "ALL" && validationStatus(item) !== statusFilter.value) return false;
    if (!keyword) return true;

    return [
      item.impaCode,
      item.platformCode,
      item.sizeModel,
      item.candidateImpaCode,
      item.candidateSpec,
      (item as MaterialMatchPreviewItem & { cnCode?: string }).cnCode,
      (item as MaterialMatchPreviewItem & { candidateCnCode?: string }).candidateCnCode,
      ...(item.candidates ?? []).flatMap((candidate) => [
        candidate.impaCode,
        candidate.specification,
        (candidate as MaterialMatchCandidate & { cnCode?: string }).cnCode
      ]),
      ...(item.candidateSnapshot ?? []).flatMap((candidate) => [
        candidate.impaCode,
        candidate.specification,
        (candidate as MaterialMatchCandidate & { cnCode?: string }).cnCode
      ])
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
  });
});

function rowKey(row: MaterialMatchPreviewItem, index: number): string {
  const stableKey = [row.sourceRowNo ?? row.sourceRowNumber ?? row.sequence, row.impaCode ?? row.supplierItemNo, row.description ?? row.rawNameSpec]
    .filter(Boolean)
    .join("-");
  return stableKey || `row-${index}`;
}

function rowCandidates(row: MaterialMatchPreviewItem): MaterialMatchCandidate[] {
  return row.candidates?.length
    ? row.candidates
    : row.candidateImpaCode
      ? [
          {
            impaCode: row.candidateImpaCode,
            nameCn: row.candidateNameCn,
            nameEn: row.candidateNameEn,
            specification: row.candidateSpec,
            reason: row.reason
          }
        ]
      : [];
}

function selectedCandidateIndex(row: MaterialMatchPreviewItem, index: number): number {
  const key = rowKey(row, index);
  const candidates = rowCandidates(row);
  const selectedIndex = selectedCandidateIndexes.value[key];
  if (selectedIndex !== undefined && candidates[selectedIndex]) return selectedIndex;
  return 0;
}

function selectedCandidate(row: MaterialMatchPreviewItem, index: number): MaterialMatchCandidate | null {
  const candidates = rowCandidates(row);
  return candidates[selectedCandidateIndex(row, index)] ?? null;
}

function documentTypeLabel(type?: string): string {
  if (type === "DEMAND_INQUIRY") return t("page.materials.documentTypeDemand");
  if (type === "SUPPLIER_QUOTATION") return t("page.materials.documentTypeSupplier");
  return t("page.materials.documentTypeUnknown");
}

function statusClass(status?: MaterialMatchResult): string {
  if (status === "EXACT") return "ok";
  if (status === "SIMILAR") return "sim";
  if (status === "UNMATCHED") return "bad";
  return "wait";
}

function itemStatusClass(item: MaterialMatchPreviewItem): string {
  const status = validationStatus(item);
  if (status === "MATCHED") return "ok";
  if (status === "ABNORMAL") return "bad";
  return "wait";
}

function isExactMatchRow(item: MaterialMatchPreviewItem): boolean {
  return item.matchResult === "EXACT" || materialReasonKind(item.reason) === "code";
}

function statusLabel(item: MaterialMatchPreviewItem): string {
  const status = validationStatus(item);
  if (status === "MATCHED") return t("page.materials.validationMatched");
  if (status === "ABNORMAL") return t("page.materials.validationAbnormal");
  return t("page.materials.statusUnknown");
}

function validationStatus(item: MaterialMatchPreviewItem): "MATCHED" | "ABNORMAL" | "UNKNOWN" {
  const explicit = String(item.validationStatus || "").toUpperCase();
  if (explicit === "MATCHED" || explicit === "ABNORMAL") return explicit;
  return isExactMatchRow(item) ? "MATCHED" : "ABNORMAL";
}

function listMatchStatus(item: MaterialMatchPreviewItem): "EXACT" | "SIMILAR" | "UNMATCHED" | "UNKNOWN" {
  if (item.matchResult === "EXACT" || item.matchResult === "SIMILAR" || item.matchResult === "UNMATCHED") return item.matchResult;

  const kind = materialReasonKind(item.reason);
  if (kind === "code" || kind === "nameSpec") return "EXACT";
  if (kind === "nameSpecCheck" || kind === "lowConfidence") return "SIMILAR";
  if (kind === "nameSpecMismatch") return rowCandidates(item).length ? "SIMILAR" : "UNMATCHED";
  if (kind === "nameMismatchSpecMismatch") return "UNMATCHED";
  return "UNKNOWN";
}

function materialReasonKind(reason?: string): "code" | "nameSpec" | "nameSpecCheck" | "lowConfidence" | "nameSpecMismatch" | "nameMismatchSpecMismatch" | "unknown" {
  const code = reason?.toUpperCase();
  if (!code) return "unknown";
  if (code === "CODE_MATCH" || code.startsWith("CODE_HIT_")) return "code";
  if (code === "NAME_SPEC_MATCH") return "nameSpec";
  if (code === "NAME_MATCH_SPEC_CHECK") return "nameSpecCheck";
  if (code === "NAME_MATCH_LOW_CONFIDENCE") return "lowConfidence";
  if (code === "NAME_MATCH_SPEC_MISMATCH" || code === "NAME_SPEC_CANDIDATE" || code === "NAME_SPEC_MISMATCH") return "nameSpecMismatch";
  if (code === "NO_CANDIDATE") return "nameMismatchSpecMismatch";
  return "unknown";
}

function reasonLabel(reason?: string): string {
  if (!reason) return "-";
  const kind = materialReasonKind(reason);
  if (kind !== "unknown") return t(`page.materials.reasonLabels.${kind}`);
  const key = `page.materials.reasonCodes.${reason}`;
  const translated = t(key);
  if (translated !== key) return translated;
  const readable = reason
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
  return t("page.materials.reasonFallback", { reason: readable || reason });
}

function firstText(...values: Array<string | number | undefined | null>): string {
  const found = values.find((value) => value !== undefined && value !== null && String(value).trim());
  return found === undefined || found === null ? "-" : String(found);
}

function isMostlyEnglishName(value?: string): boolean {
  const text = value?.trim();
  if (!text) return false;
  const chineseCount = text.match(/[\u3400-\u9fff]/g)?.length ?? 0;
  const latinCount = text.match(/[A-Za-z]/g)?.length ?? 0;
  if (latinCount === 0) return false;
  if (chineseCount === 0) return true;
  return latinCount > chineseCount;
}

function sourceName(row: MaterialMatchPreviewItem): string {
  return itemDisplayName(row);
}

function itemDisplayName(row: MaterialMatchPreviewItem): string {
  return firstText(row.cleanName, row.coreName, row.description, row.rawNameSpec);
}

function preferredCandidateName(row: MaterialMatchPreviewItem, candidate?: MaterialMatchCandidate | null): string {
  if (!candidate) return "-";
  return isMostlyEnglishName(sourceName(row))
    ? firstText(candidate.nameEn, candidate.nameCn, candidate.impaCode)
    : firstText(candidate.nameCn, candidate.nameEn, candidate.impaCode);
}

function displayedCandidateLabel(row: MaterialMatchPreviewItem, index: number): string {
  const candidate = selectedCandidate(row, index);
  const code = firstText(candidate?.impaCode, row.candidateImpaCode, row.candidates?.[0]?.impaCode);
  const name = preferredCandidateName(row, candidate);
  if (name === "-" || name === code) return code;
  return `${code} / ${name}`;
}

function candidateMatchType(candidate: MaterialMatchCandidate, row?: MaterialMatchPreviewItem): "complete" | "code" | "spec" {
  const rawType = candidate.candidateMatchType?.toUpperCase();
  if (rawType === "COMPLETE" || rawType === "EXACT" || rawType === "FULL" || rawType === "FULL_MATCH") return "complete";
  if (rawType === "CODE" || rawType === "CODE_MATCH") return "code";
  if (rawType === "SPEC" || rawType === "SPEC_MATCH" || rawType === "NAME_SPEC" || rawType === "NAME_SPEC_MATCH") return "spec";

  const reason = (candidate.reason || row?.reason || "").toUpperCase();
  if (reason.includes("NAME_SPEC_MATCH") || reason.includes("NAME_SPEC_CANDIDATE")) return "spec";
  if (reason.includes("CODE_MATCH") || reason.includes("CODE_HIT") || reason.includes("SUPPLIER_ITEM_NO")) {
    return reason.includes("NAME_SPEC_MATCH") ? "complete" : "code";
  }
  return "spec";
}

function candidateMatchTypeLabel(candidate: MaterialMatchCandidate, row?: MaterialMatchPreviewItem): string {
  return t(`page.materials.candidateMatch.${candidateMatchType(candidate, row)}`);
}

function visibleCandidateEntries(row: MaterialMatchPreviewItem, index: number) {
  const candidates = rowCandidates(row);
  if (!candidates.length) return [];
  if (isExactMatchRow(row)) {
    const candidateIndex = selectedCandidateIndex(row, index);
    return [{ candidate: candidates[candidateIndex] ?? candidates[0], candidateIndex: candidates[candidateIndex] ? candidateIndex : 0 }];
  }
  return candidates.map((candidate, candidateIndex) => ({ candidate, candidateIndex }));
}

function demandComparisonRows(row: MaterialMatchPreviewItem, index: number) {
  const candidate = selectedCandidate(row, index);
  return [
    { label: t("page.materials.impaOrPlatformCode"), source: firstText(row.impaCode, row.platformCode), candidate: firstText(candidate?.impaCode) },
    { label: t("page.materials.description"), source: itemDisplayName(row), candidate: preferredCandidateName(row, candidate) },
    { label: t("page.materials.sizeModel"), source: firstText(row.sizeModel), candidate: firstText(candidate?.specification) },
    { label: t("page.materials.quantity"), source: firstText(row.quantity), candidate: "-" },
    { label: t("page.materials.unit"), source: firstText(row.unit), candidate: firstText(candidate?.unit) },
    { label: t("page.materials.reason"), source: reasonLabel(row.reason), candidate: reasonLabel(candidate?.reason || row.reason) }
  ];
}

function isPriceLikeRawField(key: string): boolean {
  const normalized = key.toLowerCase().replace(/[\s._/-]/g, "");
  return ["price", "unitprice", "amount", "totalamount", "fob", "fobwarehouse", "quotation", "quote", "cost", "currency"].some((word) => normalized.includes(word));
}

function rawSourceRows(row: MaterialMatchPreviewItem): Array<{ label: string; value: string }> {
  const displayName = itemDisplayName(row);
  const originalName = firstText(row.description, row.rawNameSpec);
  const rawRows = Object.entries(row.rawColumns ?? {})
    .filter(([key, value]) => key.trim() && String(value ?? "").trim() && !isPriceLikeRawField(key))
    .map(([key, value]) => ({ label: key, value: String(value) }));
  if (originalName !== "-" && originalName !== displayName && !rawRows.some((rowItem) => rowItem.value === originalName)) {
    rawRows.unshift({ label: t("page.materials.originalDescription"), value: originalName });
  }
  return rawRows;
}

function selectCandidate(row: MaterialMatchPreviewItem, rowIndex: number, candidateIndex: number): void {
  selectedCandidateIndexes.value = { ...selectedCandidateIndexes.value, [rowKey(row, rowIndex)]: candidateIndex };
  matchConfirmed.value = false;
}

function confirmMatchPreview(): void {
  if (!matchPreview.value || !previewItems.value.length) return;
  const nextItems = matchPreview.value.items.map((row, index) => {
    const candidate = selectedCandidate(row, index);
    if (!candidate) return row;
    return {
      ...row,
      candidateImpaCode: candidate.impaCode,
      candidateNameCn: candidate.nameCn,
      candidateNameEn: candidate.nameEn,
      candidateSpec: candidate.specification,
      reason: candidate.reason || row.reason,
      matchResult: candidateMatchType(candidate, row) === "complete" || candidateMatchType(candidate, row) === "code" ? "EXACT" : "SIMILAR"
    };
  });
  matchPreview.value = { ...matchPreview.value, items: nextItems };
  matchConfirmed.value = true;
}

function applySelectedCandidates(): MaterialMatchPreviewItem[] {
  return previewItems.value.map((row, index) => {
    const candidate = selectedCandidate(row, index);
    const candidates = rowCandidates(row);
    if (!candidate) return { ...row, candidateSnapshot: candidates };
    return {
      ...row,
      selectedImpaCode: candidate.impaCode,
      candidateImpaCode: candidate.impaCode,
      candidateNameCn: candidate.nameCn,
      candidateNameEn: candidate.nameEn,
      candidateSpec: candidate.specification,
      reason: candidate.reason || row.reason,
      matchResult: candidateMatchType(candidate, row) === "complete" || candidateMatchType(candidate, row) === "code" ? "EXACT" : "SIMILAR",
      candidateSnapshot: candidates
    } as MaterialMatchPreviewItem;
  });
}

function parsePositiveQuantity(value: unknown): number | null {
  const text = String(value ?? "").trim();
  if (!text) return null;
  const parsed = Number(text.replace(/,/g, ""));
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

function normalizeQuantityValue(value: unknown): string {
  const text = String(value ?? "").trim();
  return parsePositiveQuantity(text) === null ? DEFAULT_MATERIAL_QUANTITY : text;
}

function normalizeUnitValue(value: unknown): string {
  const text = String(value ?? "").trim();
  if (!text) return DEFAULT_MATERIAL_UNIT;
  const matched = unitOptions.value.find((item) => [item.itemCode, item.itemValue, item.itemName, item.itemNameEn].some((candidate) => String(candidate ?? "").trim().toLowerCase() === text.toLowerCase()));
  return matched?.itemName || text;
}

function dictionaryOptionLabel(item: DictionaryItem): string {
  const code = item.itemValue || item.itemCode;
  return code && code !== item.itemName ? `${item.itemName} / ${code}` : item.itemName;
}

function unitDictionaryOptionLabel(item: DictionaryItem): string {
  if (language.value === "en-US") {
    return item.itemNameEn || item.itemValue || item.itemName || item.itemCode;
  }
  return item.itemName || item.itemValue || item.itemCode;
}

function unitOptionValue(item: DictionaryItem): string {
  return item.itemName || item.itemValue || item.itemCode;
}

function selectedPortName(): string {
  const selected = portOptions.value.find((item) => item.itemCode === demandForm.value.supplyPortCode || item.itemValue === demandForm.value.supplyPortCode);
  return selected?.itemName || demandForm.value.supplyPortName || demandForm.value.supplyPortCode || "";
}

const hasCustomSupplyPort = computed(() => {
  const code = demandForm.value.supplyPortCode.trim();
  if (!code) return false;
  return !portOptions.value.some((item) => item.itemCode === code || item.itemValue === code);
});

function normalizePortText(value?: string | null): string {
  return String(value ?? "").replace(/[^A-Za-z0-9\u4e00-\u9fa5]/g, "").toUpperCase();
}

function matchedPortOption(rawPort?: string): DictionaryItem | undefined {
  const normalized = normalizePortText(rawPort);
  if (!normalized) return undefined;
  return portOptions.value.find((item) =>
    [item.itemCode, item.itemName, item.itemValue, item.itemNameEn]
      .map(normalizePortText)
      .some((value) => value === normalized)
  );
}

function applySuggestedPort(rawPort?: string): void {
  const text = String(rawPort ?? "").trim();
  if (!text) return;
  const matched = matchedPortOption(text);
  demandForm.value.supplyPortCode = matched?.itemCode || text;
  demandForm.value.supplyPortName = matched?.itemName || text;
}

function handleSupplyPortChange(): void {
  demandForm.value.supplyPortName = selectedPortName();
  handleDemandInput("supplyPortCode");
}

function applyHeaderContextToDemandForm(preview: MaterialMatchPreviewResponse): void {
  demandForm.value.applicationNo = preview.requestNo || demandForm.value.applicationNo;
  demandForm.value.inquiryNo = preview.inquiryNo || demandForm.value.inquiryNo;
  demandForm.value.materialType = preview.materialType || demandForm.value.materialType;
  demandForm.value.currency = preview.currency || demandForm.value.currency;
  demandForm.value.recipientCompany = preview.recipientCompany || demandForm.value.recipientCompany;
  demandForm.value.handlerName = preview.handlerName || demandForm.value.handlerName;
  demandForm.value.handlerEmail = preview.handlerEmail || demandForm.value.handlerEmail;
  demandForm.value.vesselName = preview.vesselName || demandForm.value.vesselName;
  demandForm.value.vesselEta = preview.eta || demandForm.value.vesselEta;
  applySuggestedPort(preview.suggestedPort);
}

async function loadProcurementDictionaries(): Promise<void> {
  try {
    const [ports, units] = await Promise.all([
      listPublicDictionaryItems("PORT"),
      listPublicDictionaryItems("UNIT")
    ]);
    portOptions.value = ports;
    unitOptions.value = units;
    if (demandForm.value.supplyPortCode) {
      demandForm.value.supplyPortName = selectedPortName();
    }
    if (demandForm.value.supplyPortName && hasCustomSupplyPort.value) {
      applySuggestedPort(demandForm.value.supplyPortName);
    }
    if (matchPreview.value) {
      matchPreview.value = {
        ...matchPreview.value,
        items: matchPreview.value.items.map(normalizeMaterialItemDefaults)
      };
    }
  } catch {
    portOptions.value = [];
    unitOptions.value = [];
  }
}

function normalizeMaterialItemDefaults(item: MaterialMatchPreviewItem): MaterialMatchPreviewItem {
  return {
    ...item,
    quantity: normalizeQuantityValue(item.quantity),
    unit: normalizeUnitValue(item.unit)
  };
}

function normalizeMaterialPreviewDefaults(preview: MaterialMatchPreviewResponse): MaterialMatchPreviewResponse {
  return {
    ...preview,
    items: preview.items.map(normalizeMaterialItemDefaults)
  };
}

function refreshPreviewStats(items: MaterialMatchPreviewItem[]): MaterialMatchPreviewResponse | null {
  if (!matchPreview.value) return null;
  return {
    ...matchPreview.value,
    totalRows: items.length,
    exactCount: items.filter((item) => item.matchResult === "EXACT").length,
    similarCount: items.filter((item) => item.matchResult === "SIMILAR").length,
    unmatchedCount: items.filter((item) => item.matchResult === "UNMATCHED").length,
    items
  };
}

function setDemandInputRef(key: DemandFormKey, element: unknown): void {
  if (element instanceof HTMLInputElement) {
    demandInputRefs.value[key] = element;
  } else if (element instanceof HTMLSelectElement) {
    demandInputRefs.value[key] = element;
  } else if (element && typeof (element as { focus?: unknown }).focus === "function") {
    demandInputRefs.value[key] = element as HTMLElement;
  } else {
    delete demandInputRefs.value[key];
  }
}

function focusDemandField(key: DemandFormKey): void {
  invalidDemandField.value = key;
  window.requestAnimationFrame(() => {
    demandInputRefs.value[key]?.focus();
  });
}

function handleDemandInput(key: DemandFormKey): void {
  if (invalidDemandField.value !== key) return;
  if (String(demandForm.value[key] ?? "").trim()) {
    invalidDemandField.value = "";
    saveError.value = "";
  }
}

function materialRowKey(row: MaterialMatchPreviewItem): string {
  const index = previewItems.value.findIndex((item) => item === row);
  return rowKey(row, index >= 0 ? index : 0);
}

type EditableMaterialRowField = "impaCode" | "platformCode" | "description" | "sizeModel" | "quantity" | "unit" | "remarks";

function updateMaterialRowField(row: MaterialMatchPreviewItem, field: EditableMaterialRowField, value: string): void {
  const preview = matchPreview.value;
  if (!preview || isMaterialEditDisabled.value) return;
  const index = preview.items.findIndex((item) => item === row);
  if (index < 0) return;
  const items = preview.items.slice();
  items[index] = {
    ...items[index],
    [field]: value,
    validationStatus: ["impaCode", "platformCode", "description", "sizeModel"].includes(field) ? "ABNORMAL" : items[index].validationStatus,
    validationReason: ["impaCode", "platformCode", "description", "sizeModel"].includes(field) ? "MANUAL_EDIT_RECHECK_REQUIRED" : items[index].validationReason
  };
  matchPreview.value = { ...preview, items };
  matchConfirmed.value = false;

  const key = rowKey(items[index], index);
  if (invalidMaterialRowKey.value === key && (field !== "quantity" || parsePositiveQuantity(value) !== null)) {
    invalidMaterialRowKey.value = "";
    invalidMaterialField.value = "";
    saveError.value = "";
  }
}

function normalizeMaterialRowField(row: MaterialMatchPreviewItem, field: "quantity" | "unit"): void {
  const preview = matchPreview.value;
  if (!preview || isMaterialEditDisabled.value) return;
  const index = preview.items.findIndex((item) => item === row);
  if (index < 0) return;
  const current = preview.items[index];
  const normalized = field === "quantity" ? String(current.quantity ?? "").trim() : normalizeUnitValue(current.unit);
  updateMaterialRowField(current, field, normalized);
}

function canEditAbnormalRow(row: MaterialMatchPreviewItem): boolean {
  return validationStatus(row) === "ABNORMAL" && !isMaterialEditDisabled.value;
}

function editableSourceCode(row: MaterialMatchPreviewItem): string {
  return firstText(row.impaCode, row.platformCode) === "-" ? "" : firstText(row.impaCode, row.platformCode);
}

function updateMaterialCode(row: MaterialMatchPreviewItem, value: string): void {
  updateMaterialRowField(row, "impaCode", value.trim());
  if (row.platformCode && row.platformCode === row.impaCode) {
    updateMaterialRowField(row, "platformCode", value.trim());
  }
}

function validateMaterialRows(): boolean {
  const preview = matchPreview.value;
  if (!preview) return false;

  const invalidQuantityIndex = preview.items.findIndex((item) => parsePositiveQuantity(item.quantity) === null);
  if (invalidQuantityIndex >= 0) {
    const row = preview.items[invalidQuantityIndex];
    const key = rowKey(row, invalidQuantityIndex);
    invalidMaterialRowKey.value = key;
    invalidMaterialField.value = "quantity";
    expandedRowKey.value = key;
    saveNotice.value = "";
    saveError.value = t("page.materials.quantityPositiveRequired");
    return false;
  }

  matchPreview.value = {
    ...preview,
    items: preview.items.map((item) => ({ ...item, unit: normalizeUnitValue(item.unit) }))
  };
  invalidMaterialRowKey.value = "";
  invalidMaterialField.value = "";
  return true;
}

function validateDemandForm(): boolean {
  const missingField = demandRequiredFields.find((field) => !String(demandForm.value[field.key] ?? "").trim());
  if (!missingField) {
    invalidDemandField.value = "";
    return true;
  }
  saveNotice.value = "";
  saveError.value = t(missingField.errorKey);
  focusDemandField(missingField.key);
  return false;
}

function friendlyDemandSaveError(error: unknown): string {
  if (!(error instanceof Error) || !error.message) return t("page.materials.saveFailed");
  const message = error.message;
  const normalized = message.toLowerCase();
  if (normalized.includes("vesselname") || normalized.includes("vessel name")) return t("page.materials.vesselNameRequired");
  if (normalized.includes("supplyport") || normalized.includes("supply port")) return t("page.materials.supplyPortRequired");
  if (normalized.includes("vesseleta") || normalized.includes("vessel eta") || normalized.includes("inquirydate") || normalized.includes("inquiry date")) return t("page.materials.vesselEtaRequired");
  if (normalized.includes("quantity")) return t("page.materials.quantityPositiveRequired");
  return message;
}

async function persistDemand(showSuccess: boolean): Promise<MaterialDemandSaveResponse | null> {
  if (!matchPreview.value || !previewItems.value.length || isDemandReadonly.value) return null;
  saveError.value = "";
  saveNotice.value = "";
  if (!validateDemandForm()) return null;
  if (!validateMaterialRows()) return null;

  const items = applySelectedCandidates().map(normalizeMaterialItemDefaults);
  const nextPreview = refreshPreviewStats(items);
  if (nextPreview) matchPreview.value = nextPreview;
  matchConfirmed.value = true;

  try {
    const response = await saveMaterialDemand({
      demandId: demandId.value,
      demandNo: demandNo.value,
      applicationNo: demandForm.value.applicationNo.trim() || undefined,
      inquiryNo: demandForm.value.inquiryNo.trim() || undefined,
      materialType: demandForm.value.materialType.trim() || undefined,
      currency: demandForm.value.currency.trim() || undefined,
      recipientCompany: demandForm.value.recipientCompany.trim() || undefined,
      handlerName: demandForm.value.handlerName.trim() || undefined,
      handlerEmail: demandForm.value.handlerEmail.trim() || undefined,
      vesselName: demandForm.value.vesselName.trim(),
      supplyPortCode: demandForm.value.supplyPortCode.trim(),
      supplyPortName: selectedPortName(),
      vesselEta: demandForm.value.vesselEta.trim(),
      sourceFileName: selectedFileName.value,
      sourceFileId: selectedSourceFileId.value || undefined,
      documentType: matchPreview.value.documentType,
      headerRowIndex: matchPreview.value.headerRowIndex,
      items
    });
    demandId.value = response.demandId;
    demandNo.value = response.demandNo;
    demandStatus.value = response.status || demandStatus.value || "SAVED";
    if (showSuccess) saveNotice.value = t("page.materials.saveSuccess");
    return response;
  } catch (error) {
    saveError.value = friendlyDemandSaveError(error);
    return null;
  }
}

async function saveDemand(): Promise<void> {
  if (!matchPreview.value || !previewItems.value.length || isDemandActionBusy.value || isDemandReadonly.value) return;
  isSavingDemand.value = true;
  try {
    await persistDemand(true);
  } finally {
    isSavingDemand.value = false;
  }
}

function quoteMaterialPreview(): void {
  if (!matchPreview.value || !previewItems.value.length || isDemandActionBusy.value || isDemandReadonly.value) return;
  saveNotice.value = t("page.materials.quoteSuccess");
  window.setTimeout(() => {
    router.push("/inquiries");
  }, 360);
}

async function openComparisonDetail(): Promise<void> {
  if (isDemandReadonly.value) {
    saveNotice.value = "";
    saveError.value = t("page.materials.readonlyAfterCompare");
    return;
  }
  if (!matchPreview.value || !previewItems.value.length || isDemandActionBusy.value) return;

  isAutoSavingCompare.value = true;
  try {
    const response = await persistDemand(false);
    if (!response?.demandId) return;
    await router.push(`/procurement/requests/${encodeURIComponent(String(response.demandId))}/compare`);
  } finally {
    isAutoSavingCompare.value = false;
  }
}

function routeDemandId(): string {
  return String(route.params.demandId || route.query.demandId || "");
}

async function loadDemandDetail(id: string): Promise<void> {
  if (!id) return;
  isLoadingDemand.value = true;
  uploadError.value = "";
  saveError.value = "";
  saveNotice.value = "";
  try {
    const detail = await getMaterialDemandDetail(id);
    demandId.value = detail.demand.demandId;
    demandNo.value = detail.demand.demandNo;
    demandStatus.value = detail.demand.status || "";
    demandForm.value = {
      applicationNo: detail.demand.applicationNo || "",
      inquiryNo: detail.demand.inquiryNo || "",
      materialType: detail.demand.materialType || "",
      currency: detail.demand.currency || "",
      recipientCompany: detail.demand.recipientCompany || "",
      handlerName: detail.demand.handlerName || "",
      handlerEmail: detail.demand.handlerEmail || "",
      vesselName: detail.demand.vesselName || "",
      supplyPortCode: detail.demand.supplyPortCode || "",
      supplyPortName: detail.demand.supplyPortName || "",
      vesselEta: detail.demand.vesselEta || ""
    };
    if (demandForm.value.supplyPortCode) demandForm.value.supplyPortName = selectedPortName();
    selectedFileName.value = detail.demand.sourceFileName || "";
    selectedSourceFileId.value = detail.demand.sourceFileId || "";
    matchPreview.value = normalizeMaterialPreviewDefaults({
      documentType: detail.demand.documentType || "UNKNOWN",
      inquiryNo: detail.demand.inquiryNo,
      requestNo: detail.demand.applicationNo,
      vesselName: detail.demand.vesselName,
      materialType: detail.demand.materialType,
      currency: detail.demand.currency,
      suggestedPort: detail.demand.supplyPortName,
      eta: detail.demand.vesselEta,
      recipientCompany: detail.demand.recipientCompany,
      handlerName: detail.demand.handlerName,
      handlerEmail: detail.demand.handlerEmail,
      headerRowIndex: detail.demand.headerRowIndex || 0,
      totalRows: detail.demand.skuCount || detail.items.length,
      exactCount: detail.demand.exactCount,
      similarCount: detail.demand.similarCount,
      unmatchedCount: detail.demand.unmatchedCount,
      items: detail.items
    });
    statusFilter.value = "ALL";
    expandedRowKey.value = "";
    selectedCandidateIndexes.value = {};
  } catch (error) {
    uploadError.value = error instanceof Error && error.message ? error.message : t("page.materials.loadDemandFailed");
  } finally {
    isLoadingDemand.value = false;
  }
}

function openFilePicker(): void {
  if (!isDemandActionBusy.value && !isDemandReadonly.value) fileInputRef.value?.click();
}

function clearProgressTimer(): void {
  if (progressTimer !== undefined) {
    window.clearTimeout(progressTimer);
    progressTimer = undefined;
  }
}

function startProgressOverlay(): void {
  clearProgressTimer();
  progressOverlayVisible.value = true;
  progressPercent.value = 8;
  progressStageIndex.value = 0;
  progressState.value = "running";
  progressResult.value = null;

  const steps = [
    { delay: 450, percent: 24, stage: 0 },
    { delay: 900, percent: 48, stage: 1 },
    { delay: 1350, percent: 72, stage: 2 },
    { delay: 1800, percent: 88, stage: 3 }
  ];
  let index = 0;

  const runStep = () => {
    const step = steps[index];
    if (!step || progressState.value !== "running") return;
    progressPercent.value = step.percent;
    progressStageIndex.value = step.stage;
    index += 1;
    progressTimer = window.setTimeout(runStep, 520);
  };

  progressTimer = window.setTimeout(runStep, steps[0].delay);
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

async function handleFile(file?: File): Promise<void> {
  if (!file || isDemandActionBusy.value || isDemandReadonly.value) return;
  selectedFileName.value = file.name;
  selectedSourceFileId.value = "";
  uploadError.value = "";
  saveNotice.value = "";
  isUploading.value = true;
  expandedRowKey.value = "";
  selectedCandidateIndexes.value = {};
  matchConfirmed.value = false;
  startProgressOverlay();

  try {
    const preview = normalizeMaterialPreviewDefaults(await uploadMaterialMatchPreview(file));
    selectedSourceFileId.value = preview.sourceFileId || "";
    if (preview.sourceFileName) selectedFileName.value = preview.sourceFileName;
    applyHeaderContextToDemandForm(preview);
    matchPreview.value = preview;
    progressResult.value = preview;
    clearProgressTimer();
    progressStageIndex.value = 3;
    progressPercent.value = 100;
    progressState.value = "success";
    statusFilter.value = "ALL";
    await delay(340);
  } catch (error) {
    matchPreview.value = null;
    clearProgressTimer();
    progressState.value = "failed";
    uploadError.value = error instanceof Error && error.message ? error.message : t("page.materials.uploadFailed");
    await delay(600);
  } finally {
    progressOverlayVisible.value = false;
    isUploading.value = false;
    if (fileInputRef.value) fileInputRef.value.value = "";
  }
}

function handleFileChange(event: Event): void {
  const input = event.target as HTMLInputElement;
  void handleFile(input.files?.[0]);
}

function handleDrop(event: DragEvent): void {
  void handleFile(event.dataTransfer?.files?.[0]);
}

function toggleRow(row: MaterialMatchPreviewItem, index: number): void {
  const key = rowKey(row, index);
  expandedRowKey.value = expandedRowKey.value === key ? "" : key;
}

onMounted(() => {
  void loadProcurementDictionaries();
  void loadDemandDetail(routeDemandId());
});

watch(
  () => [route.params.demandId, route.query.demandId],
  () => {
    void loadDemandDetail(routeDemandId());
  }
);

onBeforeUnmount(() => {
  clearProgressTimer();
});
</script>

<template>
  <WorkbenchLayout>
  <div class="material-workbench">
    <section class="material-stage" :aria-label="t('nav.materials')">
      <section class="command-head">
        <div class="supply-strip" :aria-label="t('compare.mainInfo')">
          <div v-for="item in supplyInfo" :key="item.key" class="supply-item">
            <label>
              <span :class="{ 'required-field-label': item.required }"><i v-if="item.required" aria-hidden="true">*</i>{{ item.label }}</span>
              <select
                v-if="item.type === 'select'"
                :ref="(element) => setDemandInputRef(item.key, element)"
                v-model="demandForm[item.key]"
                :class="{ 'is-invalid': invalidDemandField === item.key }"
                :disabled="isDemandActionBusy || isDemandReadonly"
                :aria-invalid="invalidDemandField === item.key"
                @change="handleSupplyPortChange"
              >
                <option value="">{{ item.placeholder }}</option>
                <option v-if="hasCustomSupplyPort" :value="demandForm.supplyPortCode">
                  {{ demandForm.supplyPortName || demandForm.supplyPortCode }}
                </option>
                <option v-for="port in portOptions" :key="port.id || port.itemCode" :value="port.itemCode">
                  {{ dictionaryOptionLabel(port) }}
                </option>
              </select>
              <StableDateTimeInput
                v-else-if="item.type === 'datetime'"
                :ref="(element) => setDemandInputRef(item.key, element)"
                v-model="demandForm[item.key]"
                mode="datetime"
                :invalid="invalidDemandField === item.key"
                :placeholder="item.placeholder"
                :disabled="isDemandActionBusy || isDemandReadonly"
                @input="handleDemandInput(item.key)"
              />
              <div v-else-if="item.type === 'handler'" class="handler-contact-fields">
                <input
                  :ref="(element) => setDemandInputRef('handlerName', element)"
                  v-model="demandForm.handlerName"
                  type="text"
                  :placeholder="item.placeholder"
                  :disabled="isDemandActionBusy || isDemandReadonly"
                  @input="handleDemandInput('handlerName')"
                />
                <input
                  :ref="(element) => setDemandInputRef('handlerEmail', element)"
                  v-model="demandForm.handlerEmail"
                  type="text"
                  :placeholder="t('page.materials.handlerEmailPlaceholder')"
                  :disabled="isDemandActionBusy || isDemandReadonly"
                  @input="handleDemandInput('handlerEmail')"
                />
              </div>
              <input
                v-else
                :ref="(element) => setDemandInputRef(item.key, element)"
                v-model="demandForm[item.key]"
                :class="{ 'is-invalid': invalidDemandField === item.key }"
                type="text"
                :placeholder="item.placeholder"
                :disabled="isDemandActionBusy || isDemandReadonly"
                :aria-invalid="invalidDemandField === item.key"
                @input="handleDemandInput(item.key)"
              />
            </label>
          </div>
          <div class="supply-upload-item">
            <div
              :class="['drop-zone', 'supply-drop-zone', { 'is-loading': isUploading, 'has-error': uploadError, 'is-disabled': isDemandActionBusy || isDemandReadonly }]"
              role="button"
              tabindex="0"
              @click="openFilePicker"
              @keydown.enter.prevent="openFilePicker"
              @keydown.space.prevent="openFilePicker"
              @dragover.prevent
              @drop.prevent="handleDrop"
            >
              <input ref="fileInputRef" class="file-input" type="file" accept=".xlsx,.xls" :disabled="isDemandActionBusy || isDemandReadonly" @change="handleFileChange" />
              <i aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="M12 15V4m0 0 4 4m-4-4-4 4" />
                  <path d="M4 15v4h16v-4" />
                </svg>
              </i>
              <div>
                <strong>{{ isUploading ? t("page.materials.matching") : t("page.materials.uploadTitle") }}</strong>
                <p>{{ selectedFileName || t("page.materials.uploadHint") }}</p>
              </div>
            </div>
            <p v-if="uploadError" class="upload-error supply-upload-error">{{ t("page.materials.uploadFailed") }}: {{ uploadError }}</p>
          </div>
        </div>
      </section>

      <section class="workspace-grid" :aria-label="t('nav.materials')">
        <aside :class="['insight-panel', 'match-panel', { 'is-expanded': isMatchExpanded }]">
          <div class="panel-head">
            <div class="match-title-line">
              <h2>{{ t("page.materials.matchDetail") }}</h2>
              <p v-if="matchPreview" class="match-summary">
                {{ t("page.materials.detectedAs") }}：{{ documentTypeLabel(matchPreview.documentType) }}
                <span>{{ t("page.materials.totalRows") }} {{ matchPreview.totalRows }}</span>
                <span>{{ t("page.materials.headerRow") }} {{ matchPreview.headerRowIndex || "-" }}</span>
                <span v-if="matchConfirmed">{{ t("page.materials.matchConfirmed") }}</span>
                <span v-if="saveNotice" class="match-save-notice">{{ saveNotice }}</span>
              </p>
            </div>
            <div class="match-card-actions">
              <button
                type="button"
                class="icon-button compare-shortcut-button"
                :disabled="!matchPreview || !previewItems.length || isDemandActionBusy || isDemandReadonly"
                :aria-label="t('page.materials.compareShortcut')"
                :title="t('page.materials.compareShortcut')"
                @click="openComparisonDetail"
              >
                <span v-if="isAutoSavingCompare" class="button-spinner" aria-hidden="true"></span>
                <template v-else>{{ t("page.materials.compareShortcut") }}</template>
              </button>
              <button
                type="button"
                class="icon-button confirm-match-button"
                :disabled="!matchPreview || !previewItems.length || isDemandActionBusy || isDemandReadonly"
                :aria-label="t('page.materials.saveDemand')"
                :title="t('page.materials.saveDemand')"
                @click="saveDemand"
              >
                <span v-if="isSavingDemand" class="button-spinner" aria-hidden="true"></span>
                <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M5 4h12l2 2v14H5V4Zm3 0v6h8V4M8 20v-6h8v6" />
                </svg>
              </button>
              <button
                type="button"
                class="icon-button add-button"
                :disabled="isDemandActionBusy || isDemandReadonly"
                aria-label="鏂板鐗╂枡"
                @click="isMaterialSearchOpen = true"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 5v14M5 12h14" />
                </svg>
              </button>
              <button
                type="button"
                class="icon-button float-button"
                :aria-label="isMatchExpanded ? '鏀惰捣鐗╂枡鍖归厤鏄庣粏' : '鏀惧ぇ鐗╂枡鍖归厤鏄庣粏'"
                @click="isMatchExpanded = !isMatchExpanded"
              >
                <svg v-if="!isMatchExpanded" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M8 3H3v5M3 3l7 7M16 3h5v5M21 3l-7 7M8 21H3v-5M3 21l7-7M16 21h5v-5M21 21l-7-7" />
                </svg>
                <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M10 3v7H3M3 10l7-7M14 3v7h7M21 10l-7-7M10 21v-7H3M3 14l7 7M14 21v-7h7M21 14l-7 7" />
                </svg>
              </button>
            </div>
          </div>
          <p v-if="saveError" class="upload-error">{{ t("page.materials.saveFailed") }}: {{ saveError }}</p>

          <div class="match-tools">
            <label class="search-mini">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
              </svg>
              <input v-model="searchKeyword" :placeholder="t('page.materials.searchPlaceholder')" />
            </label>
            <button
              v-for="filter in matchFilters"
              :key="filter.value"
              type="button"
              :class="{ 'is-active': statusFilter === filter.value }"
              @click="statusFilter = filter.value"
            >
              {{ filter.label }}
            </button>
          </div>

          <div class="match-list" role="table" aria-label="鐗╂枡鍖归厤鏄庣粏">
            <div class="match-list-scroll">
              <div class="match-list-head" role="row">
                <span>{{ t("page.materials.sequence") }}</span>
                <span>{{ t("page.materials.impaOrPlatformCode") }}</span>
                <span>{{ t("page.materials.description") }}</span>
                <span>{{ t("page.materials.sizeModel") }}</span>
                <span>{{ t("page.materials.quantity") }}</span>
                <span>{{ t("page.materials.unit") }}</span>
                <span>{{ t("page.materials.matchResult") }}</span>
              </div>

              <template v-if="filteredItems.length">
                <template v-for="(row, index) in filteredItems" :key="rowKey(row, index)">
                  <div
                    :class="['match-row', { 'is-open': expandedRowKey === rowKey(row, index) }]"
                    role="row"
                    tabindex="0"
                    @click="toggleRow(row, index)"
                    @keydown.enter.prevent="toggleRow(row, index)"
                    @keydown.space.prevent="toggleRow(row, index)"
                  >
                    <span class="row-index"><b>{{ row.sequence ?? index + 1 }}</b></span>
                    <label v-if="canEditAbnormalRow(row)" class="match-inline-field" @click.stop>
                      <input
                        :value="editableSourceCode(row)"
                        :placeholder="t('page.materials.impaOrPlatformCode')"
                        @input="updateMaterialCode(row, ($event.target as HTMLInputElement).value)"
                      />
                    </label>
                    <span v-else class="item-code">{{ firstText(row.impaCode, row.platformCode) }}</span>
                    <label v-if="canEditAbnormalRow(row)" class="match-inline-field" @click.stop>
                      <input
                        :value="itemDisplayName(row) === '-' ? '' : itemDisplayName(row)"
                        :placeholder="t('page.materials.description')"
                        @input="updateMaterialRowField(row, 'description', ($event.target as HTMLInputElement).value)"
                      />
                    </label>
                    <strong v-else>{{ itemDisplayName(row) }}</strong>
                    <label v-if="canEditAbnormalRow(row)" class="match-inline-field" @click.stop>
                      <input
                        :value="row.sizeModel ?? ''"
                        :placeholder="t('page.materials.sizeModel')"
                        @input="updateMaterialRowField(row, 'sizeModel', ($event.target as HTMLInputElement).value)"
                      />
                    </label>
                    <span v-else>{{ firstText(row.sizeModel) }}</span>
                    <label
                      :class="['match-inline-field', { 'is-invalid': invalidMaterialRowKey === materialRowKey(row) && invalidMaterialField === 'quantity' }]"
                      @click.stop
                    >
                      <input
                        :value="row.quantity ?? ''"
                        type="number"
                        min="0.0001"
                        step="0.0001"
                        inputmode="decimal"
                        :placeholder="t('page.materials.quantityPlaceholder')"
                        :disabled="isMaterialEditDisabled"
                        :aria-invalid="invalidMaterialRowKey === materialRowKey(row) && invalidMaterialField === 'quantity'"
                        @input="updateMaterialRowField(row, 'quantity', ($event.target as HTMLInputElement).value)"
                        @blur="normalizeMaterialRowField(row, 'quantity')"
                      />
                      <small v-if="invalidMaterialRowKey === materialRowKey(row) && invalidMaterialField === 'quantity'">{{ t("page.materials.quantityPositiveRequired") }}</small>
                    </label>
                    <label class="match-inline-field" @click.stop>
                      <select
                        :value="row.unit ?? ''"
                        :disabled="isMaterialEditDisabled"
                        @change="updateMaterialRowField(row, 'unit', ($event.target as HTMLSelectElement).value)"
                        @blur="normalizeMaterialRowField(row, 'unit')"
                        >
                          <option value="">{{ t("page.materials.unitPlaceholder") }}</option>
                        <option v-for="unit in unitOptions" :key="unit.id || unit.itemCode" :value="unitOptionValue(unit)">
                          {{ unitDictionaryOptionLabel(unit) }}
                        </option>
                      </select>
                    </label>
                    <span>
                      <b :class="['status-pill', itemStatusClass(row)]">{{ statusLabel(row) }}</b>
                    </span>
                  </div>
                  <div v-if="expandedRowKey === rowKey(row, index)" :class="['match-row-detail', { 'is-readonly-detail': isDemandInComparisonStage }]" @click.stop>
                    <div class="match-detail-summary">
                      <section>
                        <h3>{{ t("page.materials.demandSku") }}</h3>
                        <div class="sku-comparison-table">
                          <div class="sku-comparison-head">
                            <span>{{ t("page.materials.field") }}</span>
                            <span>{{ t("page.materials.sourceValue") }}</span>
                          </div>
                          <div v-for="line in demandComparisonRows(row, index)" :key="line.label" class="sku-comparison-row">
                            <span>{{ line.label }}</span>
                            <strong>{{ line.source }}</strong>
                          </div>
                        </div>
                      </section>
                      <label v-if="canEditAbnormalRow(row)" class="match-remark-field match-remark-field--side">
                        <span>备注</span>
                        <textarea
                          :value="row.remarks ?? ''"
                          placeholder="填写备注"
                          rows="8"
                          @input="updateMaterialRowField(row, 'remarks', ($event.target as HTMLTextAreaElement).value)"
                        ></textarea>
                      </label>
                    </div>
                    <section class="raw-source-section">
                      <h3>{{ t("page.materials.rawSourceFields") }}</h3>
                      <div class="raw-source-grid">
                        <div v-for="line in rawSourceRows(row)" :key="line.label" class="raw-source-row">
                          <span>{{ line.label }}</span>
                          <strong>{{ line.value }}</strong>
                        </div>
                      </div>
                    </section>
                  </div>
                </template>
              </template>
              <div v-else class="match-empty">
                {{ isUploading ? t("page.materials.matching") : matchPreview ? t("page.materials.noFilteredData") : t("page.materials.uploadToPreview") }}
              </div>
            </div>
          </div>
        </aside>
      </section>
    </section>

    <Teleport to="body">
      <div v-if="isMaterialSearchOpen" class="modal-backdrop" @click.self="isMaterialSearchOpen = false">
        <section class="material-search-dialog" role="dialog" aria-modal="true" aria-labelledby="material-search-title">
          <div class="modal-head">
            <div>
              <span>MATERIAL SEARCH</span>
              <h2 id="material-search-title">{{ t("page.materials.materialSearch") }}</h2>
            </div>
            <button type="button" class="icon-button close-button" :aria-label="t('common.close')" @click="isMaterialSearchOpen = false">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m6 6 12 12M18 6 6 18" />
              </svg>
            </button>
          </div>

          <label class="search-box">
            <span>{{ t("page.materials.materialSearch") }}</span>
            <input :placeholder="t('page.materials.searchPlaceholder')" autofocus />
          </label>

          <div class="category-panel">
            <div class="category-head">
              <h3>{{ t("page.materials.commonCategories") }}</h3>
              <span>{{ t("page.materials.commonCategoriesHint") }}</span>
            </div>
            <div class="category-grid">
              <button v-for="category in commonCategories" :key="category" type="button">{{ category }}</button>
            </div>
          </div>
        </section>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="progressOverlayVisible" class="match-progress-backdrop" role="status" aria-live="polite">
        <section class="match-progress-panel" :class="`is-${progressState}`">
          <div class="match-progress-head">
            <span>{{ t("page.materials.progressKicker") }}</span>
            <h2>{{ progressState === "failed" ? t("page.materials.progressFailedTitle") : t("page.materials.progressTitle") }}</h2>
            <p>{{ selectedFileName }}</p>
          </div>

          <ol class="match-progress-stages">
            <li
              v-for="(stage, index) in progressStages"
              :key="stage"
              :class="{ active: index === progressStageIndex, done: index < progressStageIndex || progressPercent === 100 }"
            >
              <i>{{ index + 1 }}</i>
              <span>{{ stage }}</span>
            </li>
          </ol>

          <div class="ship-progress" :style="progressStyle">
            <div class="ship-progress-track">
              <div class="ship-progress-fill"></div>
              <div class="ship-runner" aria-hidden="true">
                <svg viewBox="0 0 64 40">
                  <path class="ship-flag" d="M35 5v13M36 7h15l-4 5 4 5H36" />
                  <path class="ship-body" d="M8 21h45l-6 10H16L8 21Z" />
                  <path class="ship-cabin" d="M23 13h18l5 8H18l5-8Z" />
                  <path class="ship-wave" d="M6 34c5-3 9-3 14 0s9 3 14 0 9-3 14 0 8 3 12 0" />
                </svg>
              </div>
            </div>
          </div>

          <div class="match-progress-counters">
            <div>
              <span>{{ t("page.materials.progressRowsLabel") }}</span>
              <strong>{{ progressRowsLabel }}</strong>
            </div>
            <div>
              <span>{{ t("page.materials.progressMatchedLabel") }}</span>
              <strong>{{ progressMatchedLabel }}</strong>
            </div>
            <div>
              <span>{{ t("page.materials.progressPendingLabel") }}</span>
              <strong>{{ progressPendingLabel }}</strong>
            </div>
          </div>
        </section>
      </div>
    </Teleport>
  </div>
  </WorkbenchLayout>
</template>

<style scoped>
.material-workbench {
  min-height: 0;
  min-width: 0;
  max-width: 100%;
  height: 100%;
  overflow: hidden;
  color: #12243a;
  background: transparent;
}

.material-stage {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  height: 100%;
  margin: 0 auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #cfe4f7;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 54px rgba(30, 86, 140, 0.12);
}

.command-head {
  margin-top: 0;
}

.panel-head span {
  margin: 0 0 10px;
  color: #1d72d2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.supply-strip,
.upload-panel,
.insight-panel,
.signal-card {
  border: 1px solid #d7e7f5;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(38, 96, 148, 0.08);
}

.supply-strip {
  padding: 10px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  border-radius: 16px;
}

.supply-item {
  min-width: 0;
  padding: 10px 12px;
  border-radius: 12px;
  background: linear-gradient(180deg, #f2f8ff 0%, #ffffff 100%);
}

.supply-upload-item {
  min-width: 0;
}

.supply-item label,
.supply-item span,
.signal-card span {
  display: block;
}

.supply-item label {
  min-width: 0;
}

.supply-item span,
.signal-card span {
  color: #1d72d2;
  font-size: 12px;
  font-weight: 900;
}

.supply-item .required-field-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.supply-item .required-field-label i {
  color: #e5484d;
  font-style: normal;
  font-weight: 950;
}

.supply-item input,
.supply-item select {
  width: 100%;
  height: 32px;
  margin-top: 7px;
  border: 1px solid #cfe4f7;
  border-radius: 9px;
  padding: 0 10px;
  color: #153655;
  font-size: 15px;
  font-weight: 800;
  background: #ffffff;
  outline: none;
}

.handler-contact-fields {
  display: grid;
  grid-template-columns: minmax(0, 0.72fr) minmax(0, 1fr);
  gap: 8px;
}

.supply-item input.is-invalid,
.supply-item select.is-invalid {
  border-color: #e5484d;
  box-shadow: 0 0 0 3px rgba(229, 72, 77, 0.12);
  background: #fffafa;
}

.supply-item input::placeholder {
  color: #7f98ae;
}

.supply-item input:focus,
.supply-item select:focus {
  border-color: #1d72d2;
  box-shadow: 0 0 0 3px rgba(29, 114, 210, 0.12);
}

.workspace-grid {
  flex: 1;
  min-height: 0;
  min-width: 0;
  max-width: 100%;
  margin-top: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}

.upload-panel,
.insight-panel {
  min-width: 0;
  min-height: 0;
  padding: 14px;
  border-radius: 18px;
  overflow: hidden;
}

.upload-panel {
  display: flex;
  flex-direction: column;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.panel-head h2 {
  margin: 0;
  color: #102f4f;
  font-size: 22px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-button {
  width: 42px;
  height: 42px;
  border: 1px solid #cfe4f7;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #1d72d2;
  background: #f2f8ff;
  cursor: pointer;
}

.icon-button:hover,
.icon-button:focus-visible {
  color: #ffffff;
  border-color: #1d72d2;
  background: #1d72d2;
  outline: none;
}

.icon-button svg,
.search-mini svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.match-card-actions .icon-button {
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  min-width: 34px;
  min-height: 34px;
  border-radius: 11px;
  padding: 0;
}

.match-card-actions .icon-button svg {
  width: 18px;
  height: 18px;
}

.confirm-match-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.quote-shortcut-button,
.compare-shortcut-button {
  width: 34px;
  min-width: 34px;
  color: #0a68c7;
  font-weight: 800;
}

.upload-success {
  margin: 8px 0 0;
  color: #087f5b;
  font-size: 13px;
  font-weight: 700;
}

.add-button {
  color: #ffffff;
  background: linear-gradient(135deg, #1484e6, #1d72d2);
  box-shadow: 0 10px 22px rgba(29, 114, 210, 0.2);
}

.compare-button {
  color: #0e7490;
  background: #ecfeff;
  border-color: #b7eef6;
}

.panel-head p,
.drop-zone p,
.signal-card p,
.category-head span {
  margin: 0;
  color: #5d7288;
  line-height: 1.6;
}

.panel-head > b {
  padding: 6px 10px;
  border-radius: 999px;
  color: #1d72d2;
  background: #e8f2ff;
  font-size: 12px;
  white-space: nowrap;
}

.drop-zone {
  position: relative;
  margin-top: 12px;
  flex: 0 1 clamp(190px, 31vh, 285px);
  min-height: clamp(180px, 27vh, 245px);
  padding: 18px;
  display: grid;
  align-content: center;
  justify-items: center;
  text-align: center;
  border: 1px dashed #8dbfed;
  border-radius: 16px;
  background:
    radial-gradient(circle at center top, rgba(29, 114, 210, 0.12), transparent 42%),
    #f6fbff;
  cursor: pointer;
}

.supply-drop-zone {
  min-height: 78px;
  height: 100%;
  margin-top: 0;
  padding: 12px 14px;
  grid-template-columns: 42px minmax(0, 1fr);
  align-content: center;
  justify-items: stretch;
  align-items: center;
  gap: 12px;
  text-align: left;
  border-radius: 12px;
}

.drop-zone.is-loading {
  cursor: wait;
  opacity: 0.78;
}

.drop-zone.has-error {
  border-color: #f1b9b9;
  background: #fff7f7;
}

.file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.drop-zone i {
  width: 54px;
  height: 54px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  color: #1d72d2;
  background: #e8f2ff;
}

.supply-drop-zone i {
  width: 42px;
  height: 42px;
  border-radius: 12px;
}

.drop-zone svg {
  width: 24px;
  height: 24px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.drop-zone strong {
  margin-top: 14px;
  color: #0f2c4c;
  font-size: 20px;
}

.supply-drop-zone strong {
  display: block;
  min-width: 0;
  margin-top: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.drop-zone p {
  max-width: 500px;
  margin-top: 8px;
}

.supply-drop-zone p {
  max-width: none;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.upload-error {
  margin: 10px 0 0;
  padding: 9px 11px;
  border: 1px solid #f2c7c7;
  border-radius: 12px;
  color: #a83434;
  background: #fff6f6;
  font-size: 12px;
  font-weight: 800;
}

.text-button,
.category-grid button {
  min-height: 42px;
  cursor: pointer;
  font-weight: 900;
  text-decoration: none;
}

.text-button {
  padding: 0 15px;
  border: 1px solid #cfe4f7;
  border-radius: 999px;
  color: #1d72d2;
  background: #f2f8ff;
}

.match-panel {
  display: flex;
  flex-direction: column;
  transform-origin: right top;
  transition:
    border-color 180ms ease,
    box-shadow 220ms ease,
    transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1);
  will-change: transform;
}

.match-panel.is-expanded {
  position: fixed;
  inset: 24px;
  z-index: 900;
  padding: 20px;
  border-radius: 22px;
  border-color: #b8d8f3;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(247, 251, 255, 0.98)),
    #ffffff;
  box-shadow: 0 30px 100px rgba(20, 58, 94, 0.26);
  animation: match-card-rise 260ms cubic-bezier(0.18, 0.9, 0.22, 1) both;
}

.match-panel.is-expanded .panel-head,
.match-panel.is-expanded .match-tools,
.match-panel.is-expanded .match-list {
  animation: match-content-in 280ms cubic-bezier(0.22, 0.8, 0.22, 1) both;
}

.match-panel.is-expanded .match-tools {
  animation-delay: 35ms;
}

.match-panel.is-expanded .match-list {
  animation-delay: 70ms;
}

.match-title-line {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.match-title-line > span {
  flex-basis: 100%;
  margin-bottom: 0;
}

.match-title-line h2 {
  margin: 0;
  color: #102f4f;
  font-size: 20px;
  white-space: nowrap;
}

.match-summary {
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #456581;
  font-size: 12px;
  font-weight: 800;
}

.match-summary span {
  margin: 0;
  color: #1d72d2;
  font-size: 12px;
  letter-spacing: 0;
  text-transform: none;
}

.match-summary .match-save-notice {
  color: #047857;
  margin-left: auto;
}

.match-card-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.export-button {
  color: #1d72d2;
}

.match-toolbar {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.match-toolbar button,
.match-tools button,
.match-row button {
  border: 1px solid #cfe4f7;
  border-radius: 999px;
  color: #244a6d;
  background: #f7fbff;
  cursor: pointer;
  font-weight: 900;
}

.match-toolbar button {
  min-height: 32px;
  padding: 0 11px;
  font-size: 12px;
}

.match-toolbar button:first-child {
  color: #ffffff;
  border-color: #1d72d2;
  background: #1d72d2;
}

.match-tools {
  margin-top: 10px;
  display: grid;
  grid-template-columns: minmax(220px, 1fr) repeat(4, max-content);
  gap: 10px;
  align-items: center;
  padding-bottom: 2px;
}

.search-mini {
  height: 40px;
  min-width: 0;
  padding: 0 14px;
  border: 1px solid #a8cfee;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4f7fa8;
  background: #ffffff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.search-mini input {
  width: 100%;
  min-width: 0;
  border: 0;
  color: #12243a;
  background: transparent;
  outline: none;
}

.search-mini input::placeholder {
  color: #7893aa;
}

.match-tools button {
  min-height: 40px;
  padding: 0 16px;
  font-size: 12px;
  white-space: nowrap;
}

.match-tools button.is-active {
  color: #ffffff;
  border-color: #1d72d2;
  background: #1d72d2;
  box-shadow: 0 8px 18px rgba(29, 114, 210, 0.18);
}

.match-list {
  min-width: 0;
  max-width: 100%;
  min-height: 0;
  margin-top: 12px;
  border: 1px solid #b9d8f2;
  border-radius: 16px;
  overflow-x: auto;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: #ffffff;
  box-shadow: 0 10px 22px rgba(58, 123, 177, 0.06);
}

.match-list-scroll {
  width: 100%;
  min-width: 1080px;
  max-width: none;
  max-height: 100%;
  overflow: visible;
}

.supply-upload-error {
  margin-top: 6px;
  padding: 6px 8px;
}

@keyframes match-card-rise {
  from {
    opacity: 0;
    transform: translate3d(18px, -10px, 0) scale(0.965);
    filter: saturate(0.92);
  }

  to {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
    filter: saturate(1);
  }
}

@keyframes match-content-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .match-panel,
  .match-panel.is-expanded,
  .match-panel.is-expanded .panel-head,
  .match-panel.is-expanded .match-tools,
  .match-panel.is-expanded .match-list {
    animation: none;
    transition: none;
  }
}

.match-list-head,
.match-row {
  display: grid;
  grid-template-columns:
    46px
    116px
    minmax(180px, 1.25fr)
    minmax(110px, 0.85fr)
    108px
    86px
    92px;
  column-gap: 6px;
  align-items: center;
}

.match-list-head {
  min-height: 40px;
  padding: 0 12px;
  border-bottom: 1px solid #a8cfee;
  color: #1d72d2;
  background: linear-gradient(180deg, #edf8ff 0%, #dff1ff 100%);
  font-size: 12px;
  font-weight: 900;
  box-shadow: inset 0 -1px 0 #a8cfee;
}

.match-row {
  min-height: 56px;
  padding: 8px 12px;
  color: #24415f;
  border-top: 1px solid #d9eaf7;
  cursor: pointer;
  font-size: 12px;
}

.match-row:hover,
.match-row:focus-visible,
.match-row.is-open {
  background: #eef7ff;
  outline: none;
}

.match-row:nth-child(odd) {
  background: #ffffff;
}

.match-row:nth-child(even) {
  background: #f7fbff;
}

.match-list-head > *,
.match-row > * {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-row > .match-inline-field {
  overflow: visible;
  white-space: normal;
}

.match-inline-field {
  position: relative;
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.match-inline-field input,
.match-inline-field select {
  width: 100%;
  height: 32px;
  box-sizing: border-box;
  border: 1px solid #b9d4e8;
  border-radius: 6px;
  background: #ffffff;
  color: #102f4f;
  font: inherit;
  font-weight: 800;
  outline: none;
  padding: 0 8px;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    background 0.16s ease;
}

.match-inline-field input:focus,
.match-inline-field select:focus {
  border-color: #1d72d2;
  box-shadow: 0 0 0 3px rgba(29, 114, 210, 0.14);
}

.match-inline-field input:disabled,
.match-inline-field select:disabled {
  background: #f1f6fb;
  color: #7b8fa4;
  cursor: not-allowed;
}

.match-inline-field.is-invalid input,
.match-inline-field.is-invalid select {
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
}

.match-inline-field small {
  color: #dc2626;
  font-size: 10px;
  font-weight: 800;
  line-height: 1.1;
  white-space: normal;
}

.match-remark-field {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.match-remark-field span {
  color: #5d7896;
  font-size: 12px;
  font-weight: 800;
}

.match-remark-field input,
.match-remark-field textarea {
  min-width: 0;
  min-height: 34px;
  border: 1px solid #b9d4e8;
  border-radius: 6px;
  padding: 0 10px;
  color: #102f4f;
  font-weight: 800;
  outline: none;
}

.match-remark-field textarea {
  width: 100%;
  height: 100%;
  min-height: 132px;
  padding: 10px 12px;
  resize: vertical;
  line-height: 1.5;
}

.match-remark-field--side {
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: stretch;
  min-width: 0;
  margin-bottom: 0;
  padding: 12px;
  border: 1px solid #d2e7f8;
  border-radius: 14px;
  background: #ffffff;
}

.match-remark-field--side span {
  display: flex;
  align-items: center;
}

.match-remark-field input:focus,
.match-remark-field textarea:focus {
  border-color: #1d72d2;
  box-shadow: 0 0 0 3px rgba(29, 114, 210, 0.14);
}

.match-row strong {
  min-width: 0;
  overflow: hidden;
  color: #102f4f;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-index {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #24415f;
}

.row-index input {
  width: 16px;
  height: 16px;
  margin: 0;
  border: 1px solid #98c7ea;
  border-radius: 5px;
  accent-color: #1d72d2;
}

.row-index b {
  font-weight: 900;
}

.item-code {
  color: #0b73cf;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-weight: 900;
}

.match-row .score {
  color: #0b73cf;
  font-weight: 900;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
}

.status-pill.ok {
  color: #05705f;
  background: #defaf4;
}

.status-pill.sim {
  color: #9a610b;
  background: #fff0cf;
}

.status-pill.wait {
  color: #946116;
  background: #fff1d6;
}

.status-pill.bad {
  color: #b43232;
  background: #ffe6e6;
}

.match-row-detail {
  display: grid;
  gap: 12px;
  padding: 14px 16px;
  border-top: 1px solid #d9eaf7;
  background: #f7fbff;
  animation: match-content-in 160ms ease-out;
}

.match-row-detail section {
  min-width: 0;
  padding: 12px;
  border: 1px solid #d2e7f8;
  border-radius: 14px;
  background: #ffffff;
}

.match-detail-summary {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(360px, 1fr);
  gap: 12px;
  align-items: stretch;
}

.match-row-detail.is-readonly-detail .match-detail-summary {
  grid-template-columns: minmax(0, 1fr);
}

.match-row-detail.is-readonly-detail .match-remark-field--side {
  display: none;
}

.match-row-detail h3 {
  margin: 0 0 10px;
  color: #1d72d2;
  font-size: 13px;
}

.raw-source-section {
  grid-column: 1 / -1;
}

.raw-source-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.raw-source-row {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #cfe7fb;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f5fbff 100%);
}

.raw-source-row span,
.raw-source-row strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.raw-source-row span {
  color: #6b8aa8;
  font-size: 11px;
  font-weight: 800;
}

.raw-source-row strong {
  color: #173b61;
  font-size: 12px;
  font-weight: 900;
}

.sku-comparison-table,
.candidate-table {
  display: grid;
  overflow: hidden;
  border: 1px solid #d2e7f8;
  border-radius: 12px;
  font-size: 12px;
}

.sku-comparison-head,
.sku-comparison-row {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
}

.sku-comparison-head,
.candidate-table-head {
  color: #1d72d2;
  background: #edf8ff;
  font-weight: 900;
}

.sku-comparison-row {
  min-height: 38px;
  color: #23415e;
  border-top: 1px solid #d9eaf7;
}

.sku-comparison-row:nth-child(odd) {
  background: #f8fcff;
}

.sku-comparison-row span,
.sku-comparison-row strong,
.sku-comparison-row em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sku-comparison-row strong {
  color: #102f4f;
  font-weight: 900;
}

.sku-comparison-row em {
  color: #05705f;
  font-style: normal;
  font-weight: 900;
}

.candidate-table-head,
.candidate-row {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) minmax(0, 0.85fr) 74px;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
}

.candidate-row {
  min-height: 42px;
  color: #23415e;
  border-top: 1px solid #d9eaf7;
  cursor: pointer;
}

.candidate-row:nth-child(odd) {
  background: #ffffff;
}

.candidate-row:nth-child(even) {
  background: #f8fcff;
}

.candidate-row:hover,
.candidate-row:focus-visible,
.candidate-row.is-selected {
  background: #eaf6ff;
  outline: none;
}

.candidate-row strong,
.candidate-row b {
  color: #0b73cf;
  font-weight: 900;
}

.candidate-row span,
.candidate-row small,
.candidate-row strong,
.candidate-row b {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.candidate-row small {
  color: #4f6f89;
}

.match-empty {
  min-height: 240px;
  display: grid;
  place-items: center;
  color: #62809a;
  font-size: 13px;
  font-weight: 800;
}

.match-toolbar button:hover,
.match-toolbar button:focus-visible,
.match-tools button:hover,
.match-tools button:focus-visible,
.match-row button:hover,
.match-row button:focus-visible,
.text-button:hover,
.text-button:focus-visible {
  color: #ffffff;
  border-color: #1d72d2;
  background: #1d72d2;
  outline: none;
}

.signal-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.upload-signals {
  padding-top: 0;
}

.signal-card {
  min-height: 112px;
  padding: 16px;
  border-radius: 16px;
  background:
    linear-gradient(180deg, #ffffff 0%, #f6fbff 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.signal-card strong {
  display: block;
  margin-top: 9px;
  color: #0f2c4c;
  font-size: 30px;
  line-height: 1;
}

.signal-card p {
  margin-top: 8px;
  font-size: 13px;
}

.search-box {
  margin-top: 12px;
  display: grid;
  gap: 8px;
  color: #233f5c;
  font-size: 13px;
  font-weight: 900;
}

.search-box input {
  width: 100%;
  height: 46px;
  border: 1px solid #cfe4f7;
  border-radius: 13px;
  padding: 0 14px;
  color: #12243a;
  background: #ffffff;
  outline: none;
}

.search-box input::placeholder {
  color: #6f879a;
}

.search-box input:focus {
  border-color: #1d72d2;
  box-shadow: 0 0 0 3px rgba(29, 114, 210, 0.14);
}

.category-panel {
  margin-top: 12px;
}

.category-head {
  margin-bottom: 10px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.category-head h3 {
  margin: 0;
  color: #102f4f;
  font-size: 17px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.category-grid button {
  border: 1px solid #cfe4f7;
  border-radius: 12px;
  color: #244a6d;
  background: #f7fbff;
}

.category-grid button:hover,
.category-grid button:focus-visible {
  border-color: #1d72d2;
  color: #ffffff;
  background: #1d72d2;
  outline: none;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  padding: 24px;
  display: grid;
  place-items: center;
  background: rgba(15, 44, 76, 0.28);
  backdrop-filter: blur(10px);
}

.material-search-dialog {
  width: min(720px, 100%);
  padding: 20px;
  border: 1px solid #cfe4f7;
  border-radius: 20px;
  background:
    radial-gradient(circle at 12% 8%, rgba(29, 114, 210, 0.1), transparent 34%),
    #ffffff;
  box-shadow: 0 24px 70px rgba(22, 62, 101, 0.24);
}

.modal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.modal-head span {
  color: #1d72d2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.modal-head h2 {
  margin: 8px 0 0;
  color: #102f4f;
  font-size: 24px;
}

.match-progress-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(238, 248, 255, 0.68);
  backdrop-filter: blur(12px) saturate(1.08);
}

.match-progress-panel {
  width: min(680px, 100%);
  padding: 24px;
  border: 1px solid rgba(168, 207, 238, 0.9);
  border-radius: 24px;
  background:
    radial-gradient(circle at 14% 0%, rgba(29, 114, 210, 0.12), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 251, 255, 0.98));
  box-shadow: 0 30px 90px rgba(39, 94, 143, 0.2);
}

.match-progress-panel.is-failed {
  border-color: rgba(242, 178, 178, 0.9);
  background:
    radial-gradient(circle at 14% 0%, rgba(220, 74, 74, 0.1), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 247, 247, 0.98));
}

.match-progress-head {
  display: grid;
  gap: 6px;
  text-align: center;
}

.match-progress-head span {
  color: #1d72d2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.match-progress-head h2 {
  margin: 0;
  color: #0f2c4c;
  font-size: 24px;
}

.match-progress-head p {
  min-height: 20px;
  margin: 0;
  overflow: hidden;
  color: #5d7288;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-progress-stages {
  margin: 22px 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  list-style: none;
}

.match-progress-stages li {
  min-width: 0;
  display: grid;
  justify-items: center;
  gap: 8px;
  color: #6d879d;
  font-size: 12px;
  font-weight: 900;
  text-align: center;
}

.match-progress-stages i {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 1px solid #c6dff3;
  border-radius: 999px;
  background: #ffffff;
  color: #5790bd;
  font-style: normal;
}

.match-progress-stages li.active i,
.match-progress-stages li.done i {
  border-color: #1d72d2;
  color: #ffffff;
  background: #1d72d2;
}

.match-progress-stages li.active span,
.match-progress-stages li.done span {
  color: #0f2c4c;
}

.ship-progress {
  margin-top: 28px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 52px;
  gap: 14px;
  align-items: center;
}

.ship-progress-track {
  position: relative;
  height: 16px;
  border: 1px solid #b9d8f2;
  border-radius: 999px;
  background: #eaf6ff;
  box-shadow: inset 0 1px 2px rgba(56, 107, 152, 0.08);
}

.ship-progress-fill {
  width: var(--progress);
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #83d7ff 0%, #1d72d2 100%);
  transition: width 420ms cubic-bezier(0.22, 0.8, 0.22, 1);
}

.ship-runner {
  position: absolute;
  left: var(--progress);
  bottom: 5px;
  width: 58px;
  color: #1d72d2;
  transform: translateX(-50%);
  transition: left 420ms cubic-bezier(0.22, 0.8, 0.22, 1);
  animation: ship-bob 1.2s ease-in-out infinite;
}

.ship-runner svg {
  width: 100%;
  height: auto;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

.ship-body,
.ship-cabin {
  fill: #e7f4ff;
}

.ship-flag {
  stroke: #0f80df;
}

.ship-wave {
  stroke: #7fc8f5;
  stroke-width: 2.4;
}

.match-progress-counters {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.match-progress-counters div {
  min-width: 0;
  padding: 12px;
  border: 1px solid #d6e9f8;
  border-radius: 14px;
  background: #ffffff;
}

.match-progress-counters span {
  display: block;
  color: #648198;
  font-size: 12px;
  font-weight: 800;
}

.match-progress-counters strong {
  display: block;
  margin-top: 6px;
  color: #0f2c4c;
  font-size: 18px;
  font-weight: 900;
}

@keyframes ship-bob {
  0%,
  100% {
    transform: translate(-50%, 0);
  }

  50% {
    transform: translate(-50%, -4px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .ship-runner,
  .ship-progress-fill {
    animation: none;
    transition-duration: 0ms;
  }
}

@media (max-width: 1180px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .match-tools {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .material-workbench {
    padding: 10px;
  }

  .material-stage {
    height: calc(100dvh - 20px);
    padding: 12px;
  }

  .panel-head,
  .category-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .supply-strip,
  .signal-grid,
  .category-grid {
    grid-template-columns: 1fr;
  }

  .match-toolbar,
  .match-tools {
    grid-template-columns: 1fr;
  }

  .match-list-scroll {
    min-width: 920px;
  }

  .match-row-detail {
    grid-template-columns: 1fr;
  }
}
</style>
