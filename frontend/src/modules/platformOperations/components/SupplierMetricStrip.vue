<script setup lang="ts">
import { computed } from "vue";
import type { AnalyticsMetricBasis, SupplierAnalyticsOverview } from "../types/supplierAnalytics";
import { usePlatformAnalyticsI18n } from "../locales";

const props = defineProps<{
  overview: SupplierAnalyticsOverview;
  metricBasis: AnalyticsMetricBasis;
}>();

const { ta, language } = usePlatformAnalyticsI18n();
const compact = (value: number) => new Intl.NumberFormat(language.value, { notation: "compact", maximumFractionDigits: 1 }).format(value);
const metrics = computed(() => {
  const amountBasis = props.metricBasis === "amount";
  return [
    { label: ta("platformPurchase"), value: compact(amountBasis ? props.overview.platformAmount : props.overview.platformLineCount), suffix: amountBasis ? ta("yuanUnit") : ta("lineUnit"), tone: "neutral" },
    { label: ta("supplierSupply"), value: compact(amountBasis ? props.overview.supplierAmount : props.overview.supplierLineCount), suffix: amountBasis ? ta("yuanUnit") : ta("lineUnit"), tone: "primary" },
    { label: ta("platformShare"), value: `${amountBasis ? props.overview.amountShare : props.overview.lineShare}%`, suffix: "", tone: "primary" },
    { label: ta("categoryRank"), value: ta("rankFormat", { rank: props.overview.categoryRank, total: props.overview.categorySupplierCount }), suffix: "", tone: "neutral" },
    { label: ta("specCoverage"), value: `${props.overview.specificationCoverage}%`, suffix: "", tone: props.overview.specificationCoverage < 65 ? "warning" : "primary" }
  ];
});
</script>

<template>
  <div class="supplier-metric-strip">
    <div v-for="metric in metrics" :key="metric.label" :class="['supplier-metric-strip__item', `is-${metric.tone}`]">
      <span>{{ metric.label }}</span>
      <strong>{{ metric.value }} <small v-if="metric.suffix">{{ metric.suffix }}</small></strong>
    </div>
  </div>
</template>
