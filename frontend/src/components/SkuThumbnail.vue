<script setup lang="ts">
import { ref, watch } from "vue";
import { t } from "@/i18n";

const props = defineProps<{
  src: string;
  alt: string;
  actionLabel?: string;
  disabled?: boolean;
}>();

defineEmits<{
  preview: [];
}>();

const imageFailed = ref(false);

watch(
  () => props.src,
  () => {
    imageFailed.value = false;
  }
);
</script>

<template>
  <button
    type="button"
    class="sku-thumb"
    :aria-label="actionLabel || t('action.previewImage')"
    :title="actionLabel || t('action.previewImage')"
    :disabled="disabled"
    @click.stop="$emit('preview')"
  >
    <img v-if="src && !imageFailed" :src="src" :alt="alt" @error="imageFailed = true" />
    <span v-else aria-hidden="true">IMG</span>
  </button>
</template>
