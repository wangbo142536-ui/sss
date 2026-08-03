<script setup lang="ts">
import type { SupplierDiagnosis } from "../types/supplierAnalytics";
import { usePlatformAnalyticsI18n } from "../locales";

defineProps<{ rows: SupplierDiagnosis[] }>();
const { ta, localText } = usePlatformAnalyticsI18n();
const impactLabel = (impact: SupplierDiagnosis["impact"]) => ta(impact === "high" ? "impactHigh" : impact === "medium" ? "impactMedium" : "impactLow");
</script>

<template>
  <div class="supplier-diagnosis-list">
    <article v-for="row in rows" :key="row.id" class="supplier-diagnosis-row">
      <header>
        <span :class="['supplier-impact-tag', `is-${row.impact}`]">{{ impactLabel(row.impact) }}</span>
        <strong>{{ localText(row.title) }}</strong>
      </header>
      <dl>
        <div><dt>{{ ta("evidence") }}</dt><dd>{{ localText(row.evidence) }}</dd></div>
        <div><dt>{{ ta("cause") }}</dt><dd>{{ localText(row.cause) }}</dd></div>
        <div><dt>{{ ta("action") }}</dt><dd>{{ localText(row.suggestion) }}</dd></div>
        <div class="is-target"><dt>{{ ta("target") }}</dt><dd>{{ localText(row.target) }}</dd></div>
      </dl>
    </article>
  </div>
</template>

