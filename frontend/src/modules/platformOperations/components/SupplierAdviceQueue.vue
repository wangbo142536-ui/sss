<script setup lang="ts">
import type { SupplierDiagnosis } from "../types/supplierAnalytics";
import { usePlatformAnalyticsI18n } from "../locales";

defineProps<{ rows: SupplierDiagnosis[] }>();
const { ta, localText } = usePlatformAnalyticsI18n();
</script>

<template>
  <ol class="supplier-advice-queue">
    <li v-for="row in rows" :key="row.id">
      <span>{{ row.priority }}</span>
      <div><strong>{{ localText(row.title) }}</strong><p>{{ localText(row.suggestion) }}</p></div>
      <small><b>{{ ta("target") }}</b>{{ localText(row.target) }}</small>
    </li>
  </ol>
</template>

