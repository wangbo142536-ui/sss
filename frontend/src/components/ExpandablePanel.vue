<script setup lang="ts">
import { ref } from "vue";
import IconButton from "@/components/IconButton.vue";
import { t } from "@/i18n";

defineProps<{
  title?: string;
  subtitle?: string;
  showHeader?: boolean;
  showExpand?: boolean;
}>();

const expanded = ref(false);
</script>

<template>
  <section :class="['expandable-panel', { 'is-expanded': expanded, 'is-bare': showHeader === false }]">
    <header v-if="showHeader !== false" class="panel-titlebar">
      <div>
        <strong>{{ title }}</strong>
        <span v-if="subtitle">{{ subtitle }}</span>
      </div>
      <IconButton
        v-if="showExpand !== false"
        :icon="expanded ? 'Minimize2' : 'Maximize2'"
        :label="expanded ? t('common.collapse') : t('common.expand')"
        @click="expanded = !expanded"
      />
    </header>
    <div v-else-if="showExpand !== false" class="floating-panel-action">
      <IconButton
        :icon="expanded ? 'Minimize2' : 'Maximize2'"
        :label="expanded ? t('common.collapse') : t('common.expand')"
        @click="expanded = !expanded"
      />
    </div>
    <div class="expandable-panel__body">
      <slot :expanded="expanded" />
    </div>
  </section>
</template>
