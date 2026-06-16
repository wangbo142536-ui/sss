<script setup lang="ts">
defineProps<{
  tabs: Array<{ key: string; label: string; disabled?: boolean; reason?: string }>;
  modelValue: string;
}>();

defineEmits<{
  "update:modelValue": [value: string];
}>();
</script>

<template>
  <div class="animated-tabs" role="tablist">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      role="tab"
      :class="{ active: modelValue === tab.key }"
      :aria-selected="modelValue === tab.key"
      :disabled="tab.disabled"
      :title="tab.reason"
      @click="$emit('update:modelValue', tab.key)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>
