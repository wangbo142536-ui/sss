<script setup lang="ts">
import "../styles/material-comparison-strategy.css";

type MaterialStrategyIconType = "core" | "price" | "quality" | "unmatched";

const props = withDefaults(defineProps<{
  type: MaterialStrategyIconType;
  label: string;
  compact?: boolean;
}>(), {
  compact: false
});

</script>

<template>
  <span
    :class="['material-strategy-icon', `is-${type}`, { 'is-compact': compact }]"
    role="img"
    :aria-label="label"
  >
    <svg v-if="type === 'core'" viewBox="0 0 24 24" aria-hidden="true">
      <path
        class="material-strategy-icon__star"
        d="m12 2.7 2.82 5.72 6.31.92-4.57 4.45 1.08 6.29L12 17.11l-5.64 2.97 1.08-6.29-4.57-4.45 6.31-.92L12 2.7Z"
      />
    </svg>
    <svg v-else-if="type === 'price'" class="material-strategy-icon__price" viewBox="0 0 24 24" aria-hidden="true">
      <text x="3.1" y="16.4">¥</text>
      <path class="material-strategy-icon__down" d="M17 5v12m0 0-3.4-3.4M17 17l3.4-3.4" />
    </svg>
    <svg v-else-if="type === 'quality'" viewBox="0 0 24 24" aria-hidden="true">
      <path class="material-strategy-icon__thumb" d="M9 10.1 12 4a1.8 1.8 0 0 1 3.4 1.1l-.8 3.6h4.1a2 2 0 0 1 1.9 2.5l-1.5 6.1a2 2 0 0 1-1.9 1.5H9v-8.7ZM3.5 10.1H7v8.7H3.5v-8.7Z" />
    </svg>
    <svg v-else class="material-strategy-icon__unmatched-face" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="8.5" />
      <path d="M8.2 9.3h.01M15.8 9.3h.01" />
      <path d="M8.4 17c.9-1.7 2.1-2.5 3.6-2.5s2.7.8 3.6 2.5" />
      <path class="material-strategy-icon__tear" d="M17.3 11.2c1.2 1.5 1.7 2.4 1.7 3.1a1.7 1.7 0 0 1-3.4 0c0-.7.5-1.6 1.7-3.1Z" />
    </svg>
  </span>
</template>
