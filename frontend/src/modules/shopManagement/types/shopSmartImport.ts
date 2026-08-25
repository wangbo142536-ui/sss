export type ShopSmartImportPhase = "ANALYSIS" | "EXECUTION";

export type ShopSmartImportStatus =
  | "IDLE"
  | "UPLOADING"
  | "QUEUED"
  | "RUNNING"
  | "PARTIAL"
  | "COMPLETED"
  | "FAILED";

export type ShopSmartImportAnalysisStage =
  | "UPLOAD"
  | "WORKBOOK_PARSE"
  | "MODEL_RECOGNITION"
  | "STANDARD_MATCH"
  | "PREVIEW_BUILD";

export type ShopSmartImportExecutionStage =
  | "SKU_CREATE"
  | "STANDARD_LINK"
  | "DETAIL_IMAGE_SAVE"
  | "COUNT_RECONCILIATION"
  | "VALIDATE_SELECTION"
  | "PERSIST_PRODUCTS"
  | "LINK_IMAGES"
  | "IMPORT_COMPLETE";

export type ShopSmartImportStage = ShopSmartImportAnalysisStage | ShopSmartImportExecutionStage;

export interface ShopSmartImportCounts {
  sheetTotal: number;
  sheetProcessed: number;
  itemTotal: number;
  itemProcessed: number;
  materialCount: number;
  foodCount: number;
  impaMatchedCount: number;
  categoryMatchedCount: number;
  pendingReviewCount: number;
  imageTotal: number;
  imageProcessed: number;
  failedCount: number;
}

export interface ShopSmartImportJob {
  jobId: string;
  batchId: string;
  fileName: string;
  phase: ShopSmartImportPhase;
  status: ShopSmartImportStatus;
  stage: ShopSmartImportStage;
  stageIndex: number;
  stageCount: number;
  overallPercent: number;
  stageProcessed: number;
  stageTotal: number;
  currentSheet: string;
  currentChunk: number;
  totalChunks: number;
  message: string;
  errorCode: string;
  retryable: boolean;
  counts: ShopSmartImportCounts;
  result?: unknown;
}

export const SHOP_SMART_IMPORT_ANALYSIS_STAGES: ReadonlyArray<{ code: ShopSmartImportAnalysisStage; label: string }> = [
  { code: "UPLOAD", label: "上传文件" },
  { code: "WORKBOOK_PARSE", label: "解析工作簿" },
  { code: "MODEL_RECOGNITION", label: "模型识别" },
  { code: "STANDARD_MATCH", label: "标准库匹配" },
  { code: "PREVIEW_BUILD", label: "生成预览" }
];

export const SHOP_SMART_IMPORT_EXECUTION_STAGES: ReadonlyArray<{ code: ShopSmartImportExecutionStage; label: string }> = [
  { code: "SKU_CREATE", label: "创建商品记录" },
  { code: "STANDARD_LINK", label: "写入标准库关联" },
  { code: "DETAIL_IMAGE_SAVE", label: "保存规格价格图片" },
  { code: "COUNT_RECONCILIATION", label: "数量核对完成" }
];

export const EMPTY_SHOP_SMART_IMPORT_COUNTS: ShopSmartImportCounts = {
  sheetTotal: 0,
  sheetProcessed: 0,
  itemTotal: 0,
  itemProcessed: 0,
  materialCount: 0,
  foodCount: 0,
  impaMatchedCount: 0,
  categoryMatchedCount: 0,
  pendingReviewCount: 0,
  imageTotal: 0,
  imageProcessed: 0,
  failedCount: 0
};

export const EMPTY_SHOP_SMART_IMPORT_JOB: ShopSmartImportJob = {
  jobId: "",
  batchId: "",
  fileName: "",
  phase: "ANALYSIS",
  status: "IDLE",
  stage: "UPLOAD",
  stageIndex: 0,
  stageCount: SHOP_SMART_IMPORT_ANALYSIS_STAGES.length,
  overallPercent: 0,
  stageProcessed: 0,
  stageTotal: 0,
  currentSheet: "",
  currentChunk: 0,
  totalChunks: 0,
  message: "",
  errorCode: "",
  retryable: false,
  counts: { ...EMPTY_SHOP_SMART_IMPORT_COUNTS }
};

export function isShopSmartImportTerminal(status: ShopSmartImportStatus): boolean {
  return status === "COMPLETED" || status === "PARTIAL" || status === "FAILED";
}
