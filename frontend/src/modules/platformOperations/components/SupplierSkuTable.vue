<script setup lang="ts">
import type { SupplierSkuPerformance } from "../types/supplierAnalytics";
import { usePlatformAnalyticsI18n } from "../locales";

defineProps<{ rows: SupplierSkuPerformance[] }>();
const { ta, localText, language } = usePlatformAnalyticsI18n();
const formatNumber = (value: number) => new Intl.NumberFormat(language.value, { maximumFractionDigits: 0 }).format(value);
</script>

<template>
  <div class="supplier-analytics-table-wrap">
    <table class="supplier-analytics-table supplier-sku-table">
      <thead><tr><th>{{ ta("rank") }}</th><th>{{ ta("skuName") }}</th><th>{{ ta("specification") }}</th><th>{{ ta("categoryShort") }}</th><th>{{ ta("platformAmount") }}</th><th>{{ ta("supplierAmount") }}</th><th>{{ ta("share") }}</th><th>{{ ta("coverage") }}</th></tr></thead>
      <tbody v-if="rows.length">
        <tr v-for="row in rows" :key="row.id" :class="{ 'is-gap': !row.specificationCovered }">
          <td><strong class="supplier-sku-rank">{{ row.rank }}</strong></td>
          <td><strong>{{ localText(row.skuName) }}</strong><small>{{ row.id }}</small></td>
          <td>{{ row.specification }}</td>
          <td>{{ localText(row.categoryName) }}</td>
          <td>{{ formatNumber(row.platformAmount) }}</td>
          <td>{{ formatNumber(row.supplierAmount) }}</td>
          <td><strong>{{ row.share }}%</strong></td>
          <td><span :class="['supplier-coverage-tag', { 'is-missing': !row.specificationCovered }]">{{ row.specificationCovered ? ta("covered") : ta("missing") }}</span></td>
        </tr>
      </tbody>
    </table>
    <div v-if="!rows.length" class="supplier-analytics-inline-empty">{{ ta("noSkuRows") }}</div>
  </div>
</template>

