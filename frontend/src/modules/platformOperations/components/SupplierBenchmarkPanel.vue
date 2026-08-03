<script setup lang="ts">
import type { SupplierBenchmark } from "../types/supplierAnalytics";
import { usePlatformAnalyticsI18n } from "../locales";

defineProps<{ rows: SupplierBenchmark[] }>();
const { ta } = usePlatformAnalyticsI18n();
const metricLabel = (key: SupplierBenchmark["key"]) => ta(key === "priceIndex" ? "benchmarkPriceIndex" : key === "share" ? "benchmarkShare" : key === "conversion" ? "benchmarkConversion" : "benchmarkResponse");
const maxValue = (row: SupplierBenchmark) => Math.max(row.supplierValue, row.medianValue, row.leaderValue) * 1.12;
const width = (value: number, row: SupplierBenchmark) => `${Math.max(5, value / maxValue(row) * 100)}%`;
const formatValue = (value: number, row: SupplierBenchmark) => row.unit === "%" ? `${value}%` : String(value);
</script>

<template>
  <div class="supplier-benchmark-panel">
    <div class="supplier-benchmark-legend" aria-hidden="true"><span class="is-supplier">{{ ta("benchmarkSupplier") }}</span><span class="is-median">{{ ta("benchmarkMedian") }}</span><span class="is-leader">{{ ta("benchmarkLeader") }}</span></div>
    <div v-for="row in rows" :key="row.key" class="supplier-benchmark-row">
      <strong>{{ metricLabel(row.key) }}</strong>
      <div class="supplier-benchmark-bars">
        <div><i class="is-supplier" :style="{ width: width(row.supplierValue, row) }" /><span>{{ formatValue(row.supplierValue, row) }}</span></div>
        <div><i class="is-median" :style="{ width: width(row.medianValue, row) }" /><span>{{ formatValue(row.medianValue, row) }}</span></div>
        <div><i class="is-leader" :style="{ width: width(row.leaderValue, row) }" /><span>{{ formatValue(row.leaderValue, row) }}</span></div>
      </div>
    </div>
  </div>
</template>

