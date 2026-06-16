<script setup lang="ts">
import { computed } from "vue";
import { t } from "@/i18n";
import type { SkuAttribute } from "@/types/workbench";

const props = defineProps<{
  attributes: SkuAttribute[];
}>();

defineEmits<{
  detail: [];
}>();

const visibleAttributes = computed(() => props.attributes.slice(0, 3));
const moreCount = computed(() => Math.max(props.attributes.length - visibleAttributes.value.length, 0));
</script>

<template>
  <div class="attribute-summary">
    <span v-for="attribute in visibleAttributes" :key="attribute.key" class="attribute-chip">
      {{ t(attribute.labelKey) }}: {{ attribute.value }}
    </span>
    <button
      v-if="moreCount"
      type="button"
      class="attribute-more-button"
      :aria-label="t('common.viewAttributes')"
      :title="t('common.viewAttributes')"
      @click.stop="$emit('detail')"
    >
      {{ t("common.moreAttributes", { count: moreCount }) }}
    </button>
  </div>
</template>
