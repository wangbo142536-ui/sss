<script setup lang="ts">
import { computed } from "vue";
import { supplierOptions } from "../services/supplierAnalyticsDemoService";
import type { AnalyticsBusinessType, AnalyticsMetricBasis, SupplierAnalyticsFilters } from "../types/supplierAnalytics";
import { usePlatformAnalyticsI18n } from "../locales";

const props = defineProps<{
  modelValue: SupplierAnalyticsFilters;
  dataLoaded: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: SupplierAnalyticsFilters];
  load: [];
}>();

const { ta, localText, language } = usePlatformAnalyticsI18n();

const categoryOptions = computed(() => props.modelValue.businessType === "food"
  ? [
      { value: "fresh", label: "生鲜果蔬", labelEn: "Fresh produce" },
      { value: "meat", label: "肉禽蛋品", labelEn: "Meat and eggs" },
      { value: "dry", label: "粮油干货", labelEn: "Dry goods" },
      { value: "frozen", label: "冷冻食品", labelEn: "Frozen food" }
    ]
  : [
      { value: "deck", label: "甲板物料", labelEn: "Deck stores" },
      { value: "cabin", label: "舱室用品", labelEn: "Cabin stores" },
      { value: "safety", label: "安全防护", labelEn: "Safety equipment" },
      { value: "tools", label: "五金工具", labelEn: "Hardware tools" }
    ]);

const update = <K extends keyof SupplierAnalyticsFilters>(key: K, value: SupplierAnalyticsFilters[K]) => {
  const next = { ...props.modelValue, [key]: value };
  if (key === "businessType") next.categoryId = "";
  emit("update:modelValue", next);
};
</script>

<template>
  <section class="supplier-analytics-filter" aria-label="Analysis filters">
    <label>
      <span>{{ ta("year") }}</span>
      <select :value="modelValue.year" @change="update('year', Number(($event.target as HTMLSelectElement).value))">
        <option :value="2026">2026</option>
        <option :value="2025">2025</option>
      </select>
    </label>
    <label class="supplier-analytics-filter__supplier">
      <span>{{ ta("supplier") }}</span>
      <select :value="modelValue.supplierId" @change="update('supplierId', ($event.target as HTMLSelectElement).value)">
        <option v-for="supplier in supplierOptions" :key="supplier.value" :value="supplier.value">
          {{ localText(supplier.label) }}
        </option>
      </select>
    </label>
    <label>
      <span>{{ ta("businessType") }}</span>
      <select :value="modelValue.businessType" @change="update('businessType', ($event.target as HTMLSelectElement).value as AnalyticsBusinessType)">
        <option value="material">{{ ta("material") }}</option>
        <option value="food">{{ ta("food") }}</option>
      </select>
    </label>
    <label>
      <span>{{ ta("category") }}</span>
      <select :value="modelValue.categoryId" @change="update('categoryId', ($event.target as HTMLSelectElement).value)">
        <option value="">{{ ta("allCategories") }}</option>
        <option v-for="category in categoryOptions" :key="category.value" :value="category.value">
          {{ language === 'en-US' ? category.labelEn : category.label }}
        </option>
      </select>
    </label>
    <label>
      <span>{{ ta("metricBasis") }}</span>
      <select :value="modelValue.metricBasis" @change="update('metricBasis', ($event.target as HTMLSelectElement).value as AnalyticsMetricBasis)">
        <option value="amount">{{ ta("amount") }}</option>
        <option value="lineCount">{{ ta("lineCount") }}</option>
      </select>
    </label>
    <button type="button" class="supplier-analytics-primary-button" @click="emit('load')">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 19V5m0 14h16M8 16v-5m4 5V7m4 9v-3" /></svg>
      {{ dataLoaded ? ta("refreshDemo") : ta("loadDemo") }}
    </button>
  </section>
</template>
