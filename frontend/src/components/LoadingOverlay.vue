<script setup lang="ts">
import { t } from "@/i18n";

withDefaults(
  defineProps<{
    active?: boolean;
    label?: string;
  }>(),
  {
    active: false,
    label: ""
  }
);
</script>

<template>
  <div class="loading-host">
    <slot />
    <Transition name="fade">
      <div v-if="active" class="loading-overlay" aria-live="polite">
        <span class="loading-spinner" aria-hidden="true"></span>
        <strong>{{ label || t("common.loading") }}</strong>
      </div>
    </Transition>
  </div>
</template>
