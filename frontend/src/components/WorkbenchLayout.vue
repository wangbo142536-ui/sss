<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import CollapsibleSidebar from "@/components/CollapsibleSidebar.vue";
import IconButton from "@/components/IconButton.vue";
import LanguageSwitch from "@/components/LanguageSwitch.vue";
import { currentUser, menuItems } from "@/data/mockWorkbench";
import { t } from "@/i18n";
import { getAuthMenus } from "@/services/permissionService";
import type { WorkbenchMenuItem } from "@/types/workbench";

defineProps<{
  title?: string;
  subtitle?: string;
}>();

const storageKey = "ship-supply-sidebar-expanded";
const route = useRoute();
const router = useRouter();
const sidebarExpanded = ref(typeof window === "undefined" || window.localStorage.getItem(storageKey) !== "false");
const navigationItems = ref<WorkbenchMenuItem[]>(menuItems);

watch(sidebarExpanded, (expanded) => {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(storageKey, String(expanded));
  }
});

const findActiveMenu = (items: WorkbenchMenuItem[]): WorkbenchMenuItem | undefined => {
  for (const item of items) {
    if (item.route && (route.path === item.route || route.path.startsWith(`${item.route}/`))) return item;
    const child = findActiveMenu(item.children ?? []);
    if (child) return child;
  }
  return undefined;
};
const activeMenu = computed(() => findActiveMenu(navigationItems.value));
const activeMenuLabel = computed(() => (activeMenu.value ? activeMenu.value.label || t(activeMenu.value.labelKey) : ""));

onMounted(async () => {
  navigationItems.value = await getAuthMenus();
});
</script>

<template>
  <div :class="['workbench-shell', { 'sidebar-expanded': sidebarExpanded }]">
    <div class="workbench-frame">
      <header class="workbench-topbar">
        <div class="workbench-brand-group">
          <button
            type="button"
            class="workbench-menu-toggle"
            :aria-expanded="sidebarExpanded"
            :aria-label="sidebarExpanded ? t('common.collapse') : t('common.expand')"
            :title="sidebarExpanded ? t('common.collapse') : t('common.expand')"
            @click="sidebarExpanded = !sidebarExpanded"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path v-if="sidebarExpanded" d="m15 6-6 6 6 6" />
              <path v-else d="m9 6 6 6-6 6" />
            </svg>
          </button>
          <RouterLink class="workbench-brand" to="/">
            <strong>{{ t("app.name") }}</strong>
            <span>{{ activeMenuLabel || t("app.workbench") }}</span>
          </RouterLink>
        </div>
        <div class="workbench-tools">
          <LanguageSwitch />
          <IconButton class="workbench-home-button" icon="Home" :label="t('common.backHome')" @click="router.push('/')" />
        </div>
      </header>

      <CollapsibleSidebar
        :items="navigationItems"
        :role="currentUser.role"
        :expanded="sidebarExpanded"
      />

      <main class="workbench-main">
        <slot />
      </main>
    </div>
  </div>
</template>
