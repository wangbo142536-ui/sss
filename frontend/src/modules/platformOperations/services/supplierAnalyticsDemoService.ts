import type {
  AnalyticsBusinessType,
  LocalizedText,
  SupplierAnalyticsFilters,
  SupplierAnalyticsResult,
  SupplierCategoryPerformance,
  SupplierDiagnosis,
  SupplierSkuPerformance
} from "../types/supplierAnalytics";

const text = (zh: string, en: string): LocalizedText => ({ zh, en });

const materialCategories: SupplierCategoryPerformance[] = [
  { id: "deck", name: text("甲板物料", "Deck stores"), platformAmount: 12860000, supplierAmount: 2160000, amountShare: 16.8, platformLineCount: 18420, supplierLineCount: 2918, lineShare: 15.8, rank: 3, supplierCount: 42, signal: "strong", finding: text("份额稳定，主力规格覆盖较完整", "Stable share with solid coverage of core specifications") },
  { id: "cabin", name: text("舱室用品", "Cabin stores"), platformAmount: 7940000, supplierAmount: 624000, amountShare: 7.9, platformLineCount: 26300, supplierLineCount: 1780, lineShare: 6.8, rank: 11, supplierCount: 51, signal: "watch", finding: text("平台需求大，但供货规格集中在低频段", "Platform demand is large, but supplied specifications cluster in low-frequency ranges") },
  { id: "safety", name: text("安全防护", "Safety equipment"), platformAmount: 6860000, supplierAmount: 318000, amountShare: 4.6, platformLineCount: 11480, supplierLineCount: 498, lineShare: 4.3, rank: 16, supplierCount: 37, signal: "gap", finding: text("高频认证规格缺口直接限制成交", "Missing high-frequency certified specifications directly limits conversion") },
  { id: "tools", name: text("五金工具", "Hardware tools"), platformAmount: 5210000, supplierAmount: 486000, amountShare: 9.3, platformLineCount: 15210, supplierLineCount: 1320, lineShare: 8.7, rank: 8, supplierCount: 46, signal: "watch", finding: text("报价响应率偏低，丢失可竞争需求", "Low quote response loses otherwise competitive demand") }
];

const foodCategories: SupplierCategoryPerformance[] = [
  { id: "fresh", name: text("生鲜果蔬", "Fresh produce"), platformAmount: 9380000, supplierAmount: 1320000, amountShare: 14.1, platformLineCount: 38600, supplierLineCount: 5160, lineShare: 13.4, rank: 4, supplierCount: 31, signal: "strong", finding: text("时令品覆盖较好，份额随季节波动", "Seasonal coverage is solid, with expected share fluctuation") },
  { id: "meat", name: text("肉禽蛋品", "Meat and eggs"), platformAmount: 11260000, supplierAmount: 782000, amountShare: 6.9, platformLineCount: 22480, supplierLineCount: 1402, lineShare: 6.2, rank: 10, supplierCount: 28, signal: "watch", finding: text("主流包装规格覆盖不足", "Mainstream package sizes are under-covered") },
  { id: "dry", name: text("粮油干货", "Dry goods"), platformAmount: 7240000, supplierAmount: 298000, amountShare: 4.1, platformLineCount: 29610, supplierLineCount: 1048, lineShare: 3.5, rank: 17, supplierCount: 36, signal: "gap", finding: text("价格指数偏高且报价响应慢", "Price index is high and quote response is slow") },
  { id: "frozen", name: text("冷冻食品", "Frozen food"), platformAmount: 6480000, supplierAmount: 536000, amountShare: 8.3, platformLineCount: 17460, supplierLineCount: 1370, lineShare: 7.8, rank: 7, supplierCount: 25, signal: "watch", finding: text("履约稳定，但SKU深度不足", "Fulfilment is stable, but SKU depth is limited") }
];

const materialSkus: SupplierSkuPerformance[] = [
  { id: "M-001", skuName: text("尼龙系泊缆", "Nylon mooring rope"), categoryName: text("甲板物料", "Deck stores"), specification: "Φ48mm × 220m", platformAmount: 1840000, supplierAmount: 426000, share: 23.2, rank: 2, specificationCovered: true },
  { id: "M-002", skuName: text("防滑工作手套", "Anti-slip work gloves"), categoryName: text("安全防护", "Safety equipment"), specification: "EN388 4X42C / XL", platformAmount: 1260000, supplierAmount: 42000, share: 3.3, rank: 19, specificationCovered: false },
  { id: "M-003", skuName: text("棉纱拖把头", "Cotton mop head"), categoryName: text("舱室用品", "Cabin stores"), specification: "450g / M12", platformAmount: 936000, supplierAmount: 118000, share: 12.6, rank: 6, specificationCovered: true },
  { id: "M-004", skuName: text("活动扳手", "Adjustable wrench"), categoryName: text("五金工具", "Hardware tools"), specification: "300mm / 12 inch", platformAmount: 814000, supplierAmount: 36000, share: 4.4, rank: 15, specificationCovered: false },
  { id: "M-005", skuName: text("舱盖防水胶带", "Hatch sealing tape"), categoryName: text("甲板物料", "Deck stores"), specification: "100mm × 20m", platformAmount: 748000, supplierAmount: 164000, share: 21.9, rank: 3, specificationCovered: true }
];

const foodSkus: SupplierSkuPerformance[] = [
  { id: "F-001", skuName: text("富士苹果", "Fuji apples"), categoryName: text("生鲜果蔬", "Fresh produce"), specification: "80# / 10kg", platformAmount: 1060000, supplierAmount: 188000, share: 17.7, rank: 3, specificationCovered: true },
  { id: "F-002", skuName: text("冷冻鸡胸", "Frozen chicken breast"), categoryName: text("肉禽蛋品", "Meat and eggs"), specification: "2kg × 6 / carton", platformAmount: 1580000, supplierAmount: 76000, share: 4.8, rank: 13, specificationCovered: false },
  { id: "F-003", skuName: text("一级大豆油", "Grade A soybean oil"), categoryName: text("粮油干货", "Dry goods"), specification: "10L × 2", platformAmount: 1280000, supplierAmount: 32000, share: 2.5, rank: 21, specificationCovered: false },
  { id: "F-004", skuName: text("速冻水饺", "Frozen dumplings"), categoryName: text("冷冻食品", "Frozen food"), specification: "1kg × 10", platformAmount: 886000, supplierAmount: 98000, share: 11.1, rank: 5, specificationCovered: true },
  { id: "F-005", skuName: text("荷兰土豆", "Dutch potatoes"), categoryName: text("生鲜果蔬", "Fresh produce"), specification: "10kg / bag", platformAmount: 692000, supplierAmount: 82000, share: 11.8, rank: 6, specificationCovered: true }
];

const diagnoses = (businessType: AnalyticsBusinessType): SupplierDiagnosis[] => [
  { id: "coverage", impact: "high", priority: 1, title: text("高频规格覆盖不足", "Insufficient high-frequency specification coverage"), evidence: businessType === "material" ? text("安全防护类平台前20规格中仅覆盖9个，缺口SKU贡献该类采购额的41%", "Only 9 of the top 20 safety specifications are covered; missing SKUs represent 41% of category spend") : text("肉禽蛋品前15包装规格中仅覆盖7个，缺口贡献该类采购额的38%", "Only 7 of the top 15 meat package sizes are covered; gaps represent 38% of category spend"), cause: text("现有SKU建档围绕历史订单，未按平台高频规格补齐", "The catalogue follows historical orders instead of platform demand frequency"), suggestion: text("优先补齐缺口前5规格，完成资质、包装和交期信息后参与报价", "Add the top five missing specifications, then complete certification, packaging and lead-time data before quoting"), target: text("60天内规格覆盖率由58%提升至75%", "Raise specification coverage from 58% to 75% within 60 days") },
  { id: "response", impact: "high", priority: 2, title: text("报价响应不足", "Low quote response"), evidence: text("近90天询价响应率62%，行业中位为78%，预计错失约86万元可竞争采购额", "90-day quote response is 62% versus a 78% industry median, with about CNY 860k of addressable demand missed"), cause: text("低金额询价和非核心规格未进入日常报价优先队列", "Low-value inquiries and non-core specifications are excluded from the daily quote queue"), suggestion: text("设置核心类目自动提醒和24小时响应责任人，无法供货时也提交缺货状态", "Assign a 24-hour response owner and reminders for core categories; submit an unavailable response when supply is impossible"), target: text("30天内响应率达到80%以上", "Reach at least 80% response within 30 days") },
  { id: "price", impact: "medium", priority: 3, title: text("部分规格价格偏离", "Price deviation on selected specifications"), evidence: text("低份额SKU的加权价格指数为108，较行业中位高8个点", "The weighted price index for low-share SKUs is 108, eight points above the industry median"), cause: text("小批量采购成本直接摊入单价，未按年度需求形成阶梯报价", "Small-lot costs are fully loaded into unit price without annual volume tiers"), suggestion: text("对平台年度采购额前10规格提供分档价，并单列运费或最小起订条件", "Offer tiered pricing for the top ten annual specifications and state freight or minimum-order conditions separately"), target: text("重点规格价格指数控制在102以内", "Bring the key-specification price index to 102 or below") },
  { id: "category", impact: "low", priority: 4, title: text("类目需求天花板偏低", "Limited category demand ceiling"), evidence: text("当前主营类目仅覆盖平台可采购总额的34%，单纯提升现有类目份额的增长空间有限", "Current core categories cover only 34% of addressable platform spend, limiting growth from share gains alone"), cause: text("供应范围集中在少数大类，邻近类目尚未形成组合供货能力", "Supply remains concentrated in a few categories without adjacent-category bundles"), suggestion: text("先评估一个相邻高频类目，以20个高频SKU做小范围供货验证", "Pilot one adjacent high-frequency category with 20 SKUs before broader expansion"), target: text("下季度新增类目贡献供应额5%", "Generate 5% of supply value from the new category next quarter") }
];

export const supplierOptions = [
  { value: "supplier-a", label: text("供货商A", "Supplier A") }
];

export async function loadSupplierAnalyticsDemo(filters: SupplierAnalyticsFilters): Promise<SupplierAnalyticsResult> {
  const isFood = filters.businessType === "food";
  const categories = (isFood ? foodCategories : materialCategories)
    .filter((item) => !filters.categoryId || item.id === filters.categoryId);
  const allowedCategoryNames = new Set(categories.map((item) => item.name.zh));
  const skus = (isFood ? foodSkus : materialSkus)
    .filter((item) => !filters.categoryId || allowedCategoryNames.has(item.categoryName.zh));
  const selectedSupplier = supplierOptions.find((item) => item.value === filters.supplierId) ?? supplierOptions[0];

  return {
    source: "demo",
    generatedAt: new Date().toISOString(),
    filters: { ...filters },
    supplierName: selectedSupplier.label,
    overview: {
      platformAmount: isFood ? 34360000 : 32870000,
      supplierAmount: isFood ? 2936000 : 3588000,
      amountShare: isFood ? 8.5 : 10.9,
      platformLineCount: isFood ? 108150 : 71410,
      supplierLineCount: isFood ? 8980 : 6516,
      lineShare: isFood ? 8.3 : 9.1,
      categoryRank: isFood ? 7 : 6,
      categorySupplierCount: isFood ? 39 : 56,
      specificationCoverage: isFood ? 61 : 58
    },
    categories,
    skus,
    benchmarks: [
      { key: "priceIndex", supplierValue: isFood ? 106 : 108, medianValue: 100, leaderValue: 96, unit: "index", direction: "lower" },
      { key: "share", supplierValue: isFood ? 8.5 : 10.9, medianValue: isFood ? 7.8 : 9.6, leaderValue: isFood ? 19.4 : 23.8, unit: "%", direction: "higher" },
      { key: "conversion", supplierValue: isFood ? 24 : 27, medianValue: 32, leaderValue: 46, unit: "%", direction: "higher" },
      { key: "response", supplierValue: 62, medianValue: 78, leaderValue: 94, unit: "%", direction: "higher" }
    ],
    diagnoses: diagnoses(filters.businessType)
  };
}
