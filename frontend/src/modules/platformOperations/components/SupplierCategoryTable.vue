<script setup lang="ts">
import type { AnalyticsMetricBasis, SupplierCategoryPerformance } from "../types/supplierAnalytics";
import { usePlatformAnalyticsI18n } from "../locales";

defineProps<{
  rows: SupplierCategoryPerformance[];
  metricBasis: AnalyticsMetricBasis;
}>();

const { ta, localText, language } = usePlatformAnalyticsI18n();
const formatNumber = (value: number) => new Intl.NumberFormat(language.value, { maximumFractionDigits: 0 }).format(value);
</script>

<template>
  <div class="supplier-analytics-table-wrap">
    <table class="supplier-analytics-table supplier-category-table">
      <thead>
        <tr>
          <th>{{ ta("categoryName") }}</th>
          <th>{{ ta("platformScale") }}</th>
          <th>{{ ta("supplierScale") }}</th>
          <th>{{ ta("shareAndRank") }}</th>
          <th>{{ ta("analysisFinding") }}</th>
        </tr>
      </thead>
      <tbody v-if="rows.length">
        <tr v-for="row in rows" :key="row.id">
          <td><strong>{{ localText(row.name) }}</strong><span :class="['supplier-signal', `is-${row.signal}`]">{{ ta(row.signal === 'strong' ? 'signalStrong' : row.signal === 'watch' ? 'signalWatch' : 'signalGap') }}</span></td>
          <td>{{ formatNumber(metricBasis === 'amount' ? row.platformAmount : row.platformLineCount) }} <small>{{ metricBasis === 'amount' ? ta("yuanUnit") : ta("lineUnit") }}</small></td>
          <td>{{ formatNumber(metricBasis === 'amount' ? row.supplierAmount : row.supplierLineCount) }} <small>{{ metricBasis === 'amount' ? ta("yuanUnit") : ta("lineUnit") }}</small></td>
          <td>
            <div class="supplier-share-cell">
              <div><strong>{{ metricBasis === 'amount' ? row.amountShare : row.lineShare }}%</strong><span>{{ ta("rankFormat", { rank: row.rank, total: row.supplierCount }) }}</span></div>
              <span class="supplier-share-track"><i :style="{ width: `${Math.min(metricBasis === 'amount' ? row.amountShare : row.lineShare, 100)}%` }" /></span>
            </div>
          </td>
          <td><p>{{ localText(row.finding) }}</p></td>
        </tr>
      </tbody>
    </table>
    <div v-if="!rows.length" class="supplier-analytics-inline-empty">{{ ta("noCategoryRows") }}</div>
  </div>
</template>
