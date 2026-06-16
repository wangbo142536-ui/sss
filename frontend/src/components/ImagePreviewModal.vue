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
}>();

const emit = defineEmits<{
  close: [];
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
      <div v-if="open" class="modal-backdrop" role="presentation" @click="$emit('close')">
        <section class="image-preview-modal" role="dialog" aria-modal="true" @click.stop>
          <header>
            <div>
              <strong>{{ title }}</strong>
              <span>{{ index + 1 }} / {{ images.length }}</span>
            </div>
            <IconButton icon="X" :label="t('common.close')" variant="plain" @click="$emit('close')" />
          </header>
          <div class="image-preview-stage">
            <IconButton icon="Minimize2" :label="t('common.previous')" :disabled="index === 0" @click="index--" />
            <img v-if="currentImage" :src="currentImage.src" :alt="currentImage.alt" />
            <IconButton icon="Maximize2" :label="t('common.next')" :disabled="index >= images.length - 1" @click="index++" />
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
