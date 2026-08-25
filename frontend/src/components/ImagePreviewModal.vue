<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import IconButton from "@/components/IconButton.vue";
import { t } from "@/i18n";
import type { SkuAttribute, SkuImage } from "@/types/workbench";

const props = defineProps<{
  open: boolean;
  title: string;
  images: SkuImage[];
  attributes?: SkuAttribute[];
  actionLabel?: string;
  actionLoading?: boolean;
}>();

const emit = defineEmits<{
  close: [];
  action: [];
}>();

const index = ref(0);
const currentImage = computed(() => props.images[index.value] ?? props.images[0]);

watch(
  () => props.open,
  (open) => {
    if (open) {
      index.value = 0;
    }
  }
);

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === "Escape" && props.open) {
    emit("close");
  }
};

onMounted(() => {
  window.addEventListener("keydown", handleKeydown);
});

onUnmounted(() => {
  window.removeEventListener("keydown", handleKeydown);
});
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="modal-backdrop image-preview-backdrop" role="presentation" @click="$emit('close')">
        <section class="image-preview-modal" role="dialog" aria-modal="true" @click.stop>
          <header>
            <div>
              <strong>{{ title }}</strong>
              <span>{{ images.length ? index + 1 : 0 }} / {{ images.length }}</span>
            </div>
            <div class="image-preview-actions">
              <IconButton icon="X" :label="t('common.close')" variant="plain" @click="$emit('close')" />
            </div>
          </header>
          <div class="image-preview-stage">
            <div v-if="actionLabel" class="image-preview-stage-actions">
              <IconButton icon="Upload" :label="actionLabel" variant="primary" :loading="actionLoading" @click="$emit('action')" />
            </div>
            <IconButton icon="Minimize2" :label="t('common.previous')" :disabled="!images.length || index === 0" @click="index--" />
            <img v-if="currentImage" :src="currentImage.src" :alt="currentImage.alt" />
            <div v-else class="image-preview-empty" role="status">{{ t('page.supplierProducts.noImage') }}</div>
            <IconButton icon="Maximize2" :label="t('common.next')" :disabled="!images.length || index >= images.length - 1" @click="index++" />
          </div>
          <aside class="image-preview-attrs">
            <span v-for="attribute in attributes" :key="attribute.key">
              {{ t(attribute.labelKey) }}: {{ attribute.value }}
            </span>
          </aside>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
