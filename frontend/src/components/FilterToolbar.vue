<script setup lang="ts">
import IconButton from "@/components/IconButton.vue";
import { t } from "@/i18n";

defineProps<{
  fields: Array<{
    key: string;
    label: string;
    type?: "text" | "select" | "date";
    placeholder?: string;
    options?: Array<{ label: string; value: string }>;
  }>;
  primaryAction?: {
    icon: "Plus" | "Send" | "Save" | "Upload" | "Check";
    label: string;
  };
  expandable?: boolean;
  expanded?: boolean;
}>();

defineEmits<{
  search: [];
  reset: [];
  refresh: [];
  import: [];
  export: [];
  primary: [];
  expand: [];
}>();
</script>

<template>
  <section class="filter-toolbar" aria-label="list filters">
    <div class="filter-fields">
      <label
        v-for="(field, index) in fields"
        :key="field.key"
        :class="['filter-field', { 'filter-field--search': index === 0 && field.type !== 'select' && field.type !== 'date' }]"
      >
        <span>{{ field.label }}</span>
        <span v-if="index === 0 && field.type !== 'select' && field.type !== 'date'" class="filter-search-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" />
          </svg>
        </span>
        <select v-if="field.type === 'select'" :aria-label="field.label">
          <option value="">{{ t("common.all") }}</option>
          <option v-for="option in field.options" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
        <input v-else :type="field.type === 'date' ? 'date' : 'text'" :placeholder="field.placeholder" :aria-label="field.label" />
      </label>
    </div>
    <div class="toolbar-icon-actions">
      <IconButton icon="Search" :label="t('common.search')" @click="$emit('search')" />
      <IconButton icon="X" :label="t('common.reset')" @click="$emit('reset')" />
      <IconButton icon="RefreshCw" :label="t('action.refresh')" @click="$emit('refresh')" />
      <IconButton icon="Download" :label="t('action.export')" @click="$emit('export')" />
      <IconButton v-if="primaryAction" :icon="primaryAction.icon" :label="primaryAction.label" variant="primary" @click="$emit('primary')" />
      <IconButton
        v-if="expandable"
        :icon="expanded ? 'Minimize2' : 'Maximize2'"
        :label="expanded ? t('common.collapse') : t('common.expand')"
        @click="$emit('expand')"
      />
    </div>
  </section>
</template>
