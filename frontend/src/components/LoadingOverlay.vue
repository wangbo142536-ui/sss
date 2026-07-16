<script setup lang="ts">
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
        <div class="loading-pill">
          <span class="loading-spinner" aria-hidden="true"></span>
          <strong>{{ label || "加载中" }}</strong>
        </div>
      </div>
    </Transition>
  </div>
</template>
