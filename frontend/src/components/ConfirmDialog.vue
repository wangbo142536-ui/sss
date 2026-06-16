<script setup lang="ts">
import IconButton from "@/components/IconButton.vue";
import { t } from "@/i18n";

withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    message: string;
    confirmLabel?: string;
    danger?: boolean;
  }>(),
  {
    confirmLabel: "",
    danger: false
  }
);

defineEmits<{
  close: [];
  confirm: [];
}>();
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="modal-backdrop" role="presentation" @click="$emit('close')">
        <section class="confirm-dialog" role="dialog" aria-modal="true" @click.stop>
          <header>
            <strong>{{ title }}</strong>
            <IconButton icon="X" :label="t('common.close')" variant="plain" @click="$emit('close')" />
          </header>
          <p>{{ message }}</p>
          <footer>
            <IconButton icon="X" :label="t('common.cancel')" @click="$emit('close')" />
            <IconButton icon="Check" :label="confirmLabel || t('common.confirm')" :variant="danger ? 'danger' : 'primary'" @click="$emit('confirm')" />
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
