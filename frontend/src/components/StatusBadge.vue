<script setup lang="ts">
import type { StatusVariant } from "@/types/workbench";

withDefaults(
  defineProps<{
    label: string;
    variant?: StatusVariant;
  }>(),
  {
    variant: "neutral"
  }
);
</script>

<template>
  <span :class="['status-badge', `status-badge--${variant}`]">{{ label }}</span>
</template>
