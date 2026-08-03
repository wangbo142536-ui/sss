<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import IconButton from "@/components/IconButton.vue";
import { loadSupplierAnalyticsDemo } from "../services/supplierAnalyticsDemoService";
import type { SupplierAnalyticsFilters, SupplierAnalyticsResult, SupplierSkuPerformance } from "../types/supplierAnalytics";
import { usePlatformAnalyticsI18n } from "../locales";
import "../styles/supplier-analytics.css";
import "../styles/supplier-analytics-benchmark.css";
import "../styles/supplier-analytics-responsive.css";

const { localText } = usePlatformAnalyticsI18n();
const trendBars = [40, 55, 45, 70, 85, 60, 75, 90, 65, 80, 95, 85];
const filters = ref<SupplierAnalyticsFilters>({
  year: 2026,
  supplierId: "supplier-a",
  businessType: "material",
  categoryId: "",
  metricBasis: "amount"
});
const result = ref<SupplierAnalyticsResult | null>(null);
const selectedSku = ref<SupplierSkuPerformance | null>(null);

const sortedDiagnoses = computed(() => [...(result.value?.diagnoses ?? [])].sort((first, second) => first.priority - second.priority));
const amountBasis = computed(() => filters.value.metricBasis === "amount");
const numberFormatter = new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 0 });
const compactFormatter = new Intl.NumberFormat("zh-CN", { notation: "compact", maximumFractionDigits: 1 });
const formatNumber = (value: number) => numberFormatter.format(value);
const compact = (value: number) => compactFormatter.format(value);

const dashboardMetrics = computed(() => {
  if (!result.value) return [];
  const overview = result.value.overview;
  const basisValue = amountBasis.value ? overview.supplierAmount : overview.supplierLineCount;
  const share = amountBasis.value ? overview.amountShare : overview.lineShare;
  const growth = Math.max(8, 100 - overview.specificationCoverage - 23.6);
  return [
    { label: "年度供应量", en: "Total Supply", value: compact(basisValue), unit: amountBasis.value ? "元" : "行", trend: "+12.5% 较上季度", icon: "box", tone: "blue" },
    { label: "平台占有率", en: "Market Share", value: share.toFixed(1), unit: "%", trend: "+3.2% 行业领先", icon: "pie", tone: "indigo" },
    { label: "增长潜力", en: "Growth Rate", value: growth.toFixed(1), unit: "%", trend: "处于高增长机会期", icon: "bolt", tone: "green" }
  ];
});

const categoryBars = computed(() => {
  if (!result.value) return [];
  return result.value.categories.slice(0, 3).map((item) => {
    const share = amountBasis.value ? item.amountShare : item.lineShare;
    return {
      id: item.id,
      name: localText(item.name),
      share,
      gap: Math.max(0, 100 - share),
      tone: item.signal
    };
  });
});

const topSkus = computed(() => {
  if (!result.value) return [];
  return [...result.value.skus].sort((first, second) => second.supplierAmount - first.supplierAmount).slice(0, 5);
});

const selectedSkuDetail = computed(() => {
  const sku = selectedSku.value;
  if (!sku || !result.value) return null;
  const priceDelta = sku.specificationCovered ? 5.5 : 8.2;
  const leadTimeDelta = sku.specificationCovered ? 9 : 17;
  const matchScore = sku.specificationCovered ? 89.4 : 72.8;
  return {
    sku,
    matchScore,
    missingCount: sku.specificationCovered ? 1 : 2,
    packStatus: sku.specificationCovered ? "匹配" : "待补齐",
    supplierPack: sku.specification,
    platformPack: sku.specificationCovered ? sku.specification : "平台高频规格待补齐",
    supplierPrice: sku.supplierAmount,
    platformPrice: Math.round(sku.supplierAmount / (1 + priceDelta / 100)),
    priceDelta,
    leadTimeDelta,
    rows: [
      { name: "规格完整度", platform: "标准字段齐全", supplier: sku.specificationCovered ? "字段齐全" : "缺少认证字段", status: sku.specificationCovered ? "匹配良好" : "存在差距", deviation: sku.specificationCovered ? "+0.12%" : "-18.0%", risk: sku.specificationCovered ? "L1 低" : "L3 中" },
      { name: "供应覆盖量", platform: formatNumber(sku.platformAmount), supplier: formatNumber(sku.supplierAmount), status: sku.share >= 10 ? "覆盖稳定" : "存在差距", deviation: `${sku.share.toFixed(1)}%`, risk: sku.share >= 10 ? "L2 中" : "L4 高" },
      { name: "质保与交付", platform: "36 MONTHS / 28 天", supplier: sku.specificationCovered ? "36 MONTHS / 37 天" : "12 MONTHS / 45 天", status: sku.specificationCovered ? "需关注" : "严重缺失", deviation: `+${leadTimeDelta} 天`, risk: sku.specificationCovered ? "L3 中" : "L5 极高" }
    ]
  };
});

const radarAxes = computed(() => {
  if (!result.value) return [];
  const overview = result.value.overview;
  const response = result.value.benchmarks.find((item) => item.key === "response")?.supplierValue ?? 62;
  const conversion = result.value.benchmarks.find((item) => item.key === "conversion")?.supplierValue ?? 27;
  const priceIndex = result.value.benchmarks.find((item) => item.key === "priceIndex")?.supplierValue ?? 108;
  return [
    { label: "价格", value: Math.max(24, 100 - Math.max(0, priceIndex - 96) * 3), x: 50, y: 18 },
    { label: "规格", value: overview.specificationCoverage, x: 84, y: 50 },
    { label: "响应", value: response, x: 50, y: 82 },
    { label: "质量", value: Math.min(92, conversion * 2.4), x: 16, y: 50 }
  ];
});

const radarPolygon = computed(() => {
  const center = { x: 50, y: 50 };
  return radarAxes.value
    .map((axis) => {
      const ratio = axis.value / 100;
      const x = center.x + (axis.x - center.x) * ratio;
      const y = center.y + (axis.y - center.y) * ratio;
      return `${x},${y}`;
    })
    .join(" ");
});

async function loadDemo() {
  result.value = await loadSupplierAnalyticsDemo(filters.value);
  selectedSku.value = null;
}

function openSkuDetail(sku: SupplierSkuPerformance) {
  selectedSku.value = sku;
  window.requestAnimationFrame(() => {
    document.querySelector(".supplier-analytics-page")?.scrollTo({ top: 0, behavior: "smooth" });
  });
}

function closeSkuDetail() {
  selectedSku.value = null;
}

onMounted(() => {
  void loadDemo();
});
</script>

<template>
  <WorkbenchLayout>
    <div class="supplier-analytics-page">
      <template v-if="result">
        <section v-if="!selectedSku" id="supplier-current" class="supplier-insight-cockpit" aria-label="供应商供应分析驾驶舱">
          <div class="supplier-insight-kpis">
            <article v-for="metric in dashboardMetrics" :key="metric.label" :class="['supplier-insight-card', `is-${metric.tone}`]">
              <div class="supplier-insight-scan" aria-hidden="true" />
              <header>
                <p>{{ metric.label }} <span>({{ metric.en }})</span></p>
                <i :class="`is-${metric.icon}`" aria-hidden="true" />
              </header>
              <div class="supplier-insight-value">
                <strong>{{ metric.value }}</strong>
                <span>{{ metric.unit }}</span>
              </div>
              <footer>{{ metric.trend }}</footer>
            </article>
          </div>

          <div class="supplier-insight-grid">
            <div class="supplier-insight-main">
              <article class="supplier-insight-card supplier-insight-panel" id="supplier-benchmark">
                <header class="supplier-insight-panel-head">
                  <h2>品类占有率分析</h2>
                  <span>供应规模 vs 平台缺口</span>
                </header>
                <div class="supplier-category-bars">
                  <div v-for="item in categoryBars" :key="item.id" class="supplier-category-bar">
                    <div>
                      <strong>{{ item.name }}</strong>
                      <span>{{ item.share.toFixed(1) }}% / {{ item.gap.toFixed(1) }}% 缺口</span>
                    </div>
                    <div class="supplier-category-track">
                      <i :class="`is-${item.tone}`" :style="{ width: `${Math.min(item.share, 100)}%` }" />
                    </div>
                  </div>
                </div>
              </article>

              <article class="supplier-insight-card supplier-insight-panel supplier-trend-panel">
                <header class="supplier-insight-panel-head">
                  <h2>年度供应趋势对比</h2>
                  <div class="supplier-trend-legend"><span>实际供应</span><span>机会预测</span></div>
                </header>
                <div class="supplier-trend-chart" aria-label="年度供应趋势柱状图">
                  <span v-for="(height, index) in trendBars" :key="index" :class="{ 'is-current': index === 7 }" :style="{ height: `${height}%` }">
                    <b>{{ index + 1 }}月</b>
                  </span>
                </div>
              </article>

              <section id="supplier-advice" class="supplier-insight-actions">
                <article v-for="row in sortedDiagnoses.slice(0, 4)" :key="row.id" class="supplier-insight-action">
                  <span :class="`is-${row.impact}`" />
                  <h3>{{ localText(row.title) }}</h3>
                  <p>{{ localText(row.suggestion) }}</p>
                </article>
              </section>
            </div>

            <aside class="supplier-insight-side">
              <article class="supplier-insight-card supplier-insight-panel">
                <header class="supplier-insight-panel-head">
                  <h2>SKU 供应排行 TOP 5</h2>
                </header>
                <div class="supplier-sku-rank-list">
                  <button v-for="(sku, index) in topSkus" :key="sku.id" type="button" class="supplier-sku-rank-item" @click="openSkuDetail(sku)">
                    <span>{{ index + 1 }}</span>
                    <div>
                      <strong>{{ localText(sku.skuName) }}</strong>
                      <small>{{ localText(sku.categoryName) }}</small>
                    </div>
                    <b>{{ formatNumber(sku.supplierAmount) }}</b>
                  </button>
                </div>
              </article>

              <article class="supplier-insight-card supplier-insight-panel supplier-radar-panel" id="supplier-diagnosis">
                <header class="supplier-insight-panel-head">
                  <h2>规格失配诊断</h2>
                  <span>覆盖、响应、价格、质量</span>
                </header>
                <div class="supplier-radar">
                  <svg viewBox="0 0 100 100" role="img" aria-label="供应能力雷达图">
                    <circle cx="50" cy="50" r="36" />
                    <circle cx="50" cy="50" r="26" />
                    <circle cx="50" cy="50" r="16" />
                    <path d="M50 14 L86 50 L50 86 L14 50 Z" />
                    <polygon :points="radarPolygon" />
                    <g v-for="axis in radarAxes" :key="axis.label">
                      <circle :cx="axis.x" :cy="axis.y" r="1.8" />
                    </g>
                  </svg>
                  <span class="is-top">价格</span>
                  <span class="is-right">规格</span>
                  <span class="is-bottom">响应</span>
                  <span class="is-left">质量</span>
                </div>
              </article>
            </aside>
          </div>

        </section>

        <section v-else-if="selectedSkuDetail" class="supplier-sku-detail-view" aria-label="SKU 规格对比分析详情">
          <header class="supplier-sku-detail-hero">
            <div>
              <h2>规格比对分析详情</h2>
              <p>{{ localText(selectedSkuDetail.sku.skuName) }}，实时解析供应商物料规格与平台标准契合度。</p>
            </div>
            <div class="supplier-sku-detail-scores">
              <article>
                <span>综合匹配得分</span>
                <strong>{{ selectedSkuDetail.matchScore.toFixed(1) }} <small>%</small></strong>
              </article>
              <article class="is-alert">
                <span>关键缺失项</span>
                <strong>{{ String(selectedSkuDetail.missingCount).padStart(2, "0") }} <small>项</small></strong>
              </article>
              <IconButton class="supplier-sku-detail-back" icon="ChevronLeft" label="返回供应分析" @click="closeSkuDetail" />
            </div>
          </header>

          <div class="supplier-sku-detail-grid">
            <article class="supplier-insight-card supplier-sku-detail-radar">
              <header class="supplier-insight-panel-head">
                <h2>综合能力表现</h2>
                <span>供应商表现 · 核心指标</span>
              </header>
              <svg viewBox="0 0 200 200" role="img" aria-label="SKU 综合能力表现">
                <polygon points="100,20 170,60 170,140 100,180 30,140 30,60" />
                <polygon points="100,60 135,80 135,120 100,140 65,120 65,80" />
                <line x1="100" x2="100" y1="20" y2="180" />
                <line x1="30" x2="170" y1="60" y2="140" />
                <line x1="170" x2="30" y1="60" y2="140" />
                <path d="M100 40 L160 70 L145 130 L100 160 L50 135 L45 75 Z" />
                <text x="100" y="14">价格</text>
                <text x="176" y="62">规格</text>
                <text x="176" y="145">交付</text>
                <text x="100" y="197">合规</text>
                <text x="24" y="145">质量</text>
                <text x="24" y="62">服务</text>
              </svg>
              <div class="supplier-sku-radar-note">
                <div><span>供应商表现</span><b>高于平均 12%</b></div>
                <i />
              </div>
            </article>

            <div class="supplier-sku-detail-cards">
              <article class="supplier-insight-card supplier-sku-compare-card is-good">
                <header>
                  <h3>包装规格 (Pack Size)</h3>
                  <span>{{ selectedSkuDetail.packStatus }}</span>
                </header>
                <div class="supplier-sku-compare-values">
                  <div><span>供应商规格</span><strong>{{ selectedSkuDetail.supplierPack }}</strong></div>
                  <div><span>平台标准</span><strong>{{ selectedSkuDetail.platformPack }}</strong></div>
                </div>
                <p>规格与平台高频采购标准进行逐项比对，用于判断是否可进入自动推荐和报价优先队列。</p>
              </article>

              <article class="supplier-insight-card supplier-sku-compare-card is-watch">
                <header>
                  <h3>价格竞争力 (Price)</h3>
                  <span>存在差距</span>
                </header>
                <div class="supplier-sku-compare-values">
                  <div><span>供应商报价</span><strong>￥{{ formatNumber(selectedSkuDetail.supplierPrice) }}</strong></div>
                  <div><span>平台基准</span><strong>￥{{ formatNumber(selectedSkuDetail.platformPrice) }}</strong></div>
                </div>
                <p>当前报价较平台同规格基准高 {{ selectedSkuDetail.priceDelta.toFixed(1) }}%，需要结合批量阶梯价重新校准。</p>
              </article>

              <article class="supplier-insight-card supplier-sku-compare-card supplier-sku-lead-card is-risk">
                <header>
                  <div>
                    <h3>交付周期 (Lead Time)</h3>
                    <p>关键供应链环节时效性监测</p>
                  </div>
                  <span>严重缺失</span>
                </header>
                <div class="supplier-sku-lead-body">
                  <div class="supplier-sku-lead-bars">
                    <div><span>供应商反馈周期</span><b>{{ 28 + selectedSkuDetail.leadTimeDelta }} 天</b><i style="width: 90%" /></div>
                    <div><span>行业平均水平</span><b>28 天</b><i style="width: 56%" /></div>
                  </div>
                  <div class="supplier-sku-lead-delta">
                    <strong>+{{ selectedSkuDetail.leadTimeDelta }}</strong>
                    <span>天数超期</span>
                    <em>影响库存周转</em>
                  </div>
                </div>
              </article>
            </div>
          </div>

          <article class="supplier-insight-card supplier-sku-detail-table">
            <header class="supplier-insight-panel-head">
              <h2>参数逐项比对列表</h2>
              <button type="button">导出分析结果</button>
            </header>
            <div class="supplier-analytics-table-wrap">
              <table class="supplier-analytics-table">
                <thead><tr><th>参数名称</th><th>平台标准</th><th>供应商提供</th><th>状态指标</th><th>偏离度</th><th>风险权重</th></tr></thead>
                <tbody>
                  <tr v-for="row in selectedSkuDetail.rows" :key="row.name">
                    <td><strong>{{ row.name }}</strong></td>
                    <td>{{ row.platform }}</td>
                    <td>{{ row.supplier }}</td>
                    <td><span class="supplier-coverage-tag" :class="{ 'is-missing': row.status.includes('缺失') || row.status.includes('差距') }">{{ row.status }}</span></td>
                    <td>{{ row.deviation }}</td>
                    <td>{{ row.risk }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </article>
        </section>
      </template>
    </div>
  </WorkbenchLayout>
</template>
