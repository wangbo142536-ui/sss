<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "@/i18n";

const { language, setLanguage, t } = useI18n();

const nextLanguageLabel = computed(() => (language.value === "zh-CN" ? "EN" : "CH"));

const toggleLanguage = () => {
  setLanguage(language.value === "zh-CN" ? "en-US" : "zh-CN");
};
</script>

<template>
  <button
    type="button"
    class="language-switch-button"
    :aria-label="t('common.switchLanguage')"
    :title="t('common.switchLanguage')"
    @click="toggleLanguage"
  >
    {{ nextLanguageLabel }}
  </button>
</template>
