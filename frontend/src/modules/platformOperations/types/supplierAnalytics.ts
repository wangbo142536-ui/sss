export type AnalyticsBusinessType = "material" | "food";
export type AnalyticsMetricBasis = "amount" | "lineCount";
export type AnalyticsImpact = "high" | "medium" | "low";

export type LocalizedText = {
  zh: string;
  en: string;
};

export type SupplierAnalyticsFilters = {
  year: number;
  supplierId: string;
  businessType: AnalyticsBusinessType;
  categoryId: string;
  metricBasis: AnalyticsMetricBasis;
};

export type SupplierAnalyticsOverview = {
  platformAmount: number;
  supplierAmount: number;
  amountShare: number;
  platformLineCount: number;
  supplierLineCount: number;
  lineShare: number;
  categoryRank: number;
  categorySupplierCount: number;
  specificationCoverage: number;
};

export type SupplierCategoryPerformance = {
  id: string;
  name: LocalizedText;
  platformAmount: number;
  supplierAmount: number;
  amountShare: number;
  platformLineCount: number;
  supplierLineCount: number;
  lineShare: number;
  rank: number;
  supplierCount: number;
  signal: "strong" | "watch" | "gap";
  finding: LocalizedText;
};

export type SupplierSkuPerformance = {
  id: string;
  skuName: LocalizedText;
  categoryName: LocalizedText;
  specification: string;
  platformAmount: number;
  supplierAmount: number;
  share: number;
  rank: number;
  specificationCovered: boolean;
};

export type SupplierBenchmark = {
  key: "priceIndex" | "share" | "conversion" | "response";
  supplierValue: number;
  medianValue: number;
  leaderValue: number;
  unit: "%" | "index";
  direction: "higher" | "lower";
};

export type SupplierDiagnosis = {
  id: string;
  impact: AnalyticsImpact;
  title: LocalizedText;
  evidence: LocalizedText;
  cause: LocalizedText;
  suggestion: LocalizedText;
  target: LocalizedText;
  priority: number;
};

export type SupplierAnalyticsResult = {
  source: "demo";
  generatedAt: string;
  filters: SupplierAnalyticsFilters;
  supplierName: LocalizedText;
  overview: SupplierAnalyticsOverview;
  categories: SupplierCategoryPerformance[];
  skus: SupplierSkuPerformance[];
  benchmarks: SupplierBenchmark[];
  diagnoses: SupplierDiagnosis[];
};
