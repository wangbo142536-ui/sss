<script setup lang="ts">
defineProps<{
  title: string;
  hint: string;
}>();
</script>

<template>
  <div class="supplier-analytics-section-heading">
    <h2>{{ title }}</h2>
    <p>{{ hint }}</p>
  </div>
</template>

