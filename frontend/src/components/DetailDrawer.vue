<script setup lang="ts">
import IconButton from "@/components/IconButton.vue";
import { t } from "@/i18n";

withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    subtitle?: string;
    width?: "default" | "wide";
  }>(),
  {
    subtitle: "",
    width: "default"
  }
);

defineEmits<{
  close: [];
}>();
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="open" class="drawer-backdrop" role="presentation" @click="$emit('close')">
        <aside :class="['detail-drawer', `detail-drawer--${width}`]" role="dialog" aria-modal="true" @click.stop>
          <header class="drawer-head">
            <div>
              <strong>{{ title }}</strong>
              <span v-if="subtitle">{{ subtitle }}</span>
            </div>
            <IconButton icon="X" :label="t('common.close')" variant="plain" @click="$emit('close')" />
          </header>
          <div class="drawer-body">
            <slot />
          </div>
          <footer class="drawer-footer">
            <slot name="footer">
              <IconButton icon="X" :label="t('common.close')" @click="$emit('close')" />
            </slot>
          </footer>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>
