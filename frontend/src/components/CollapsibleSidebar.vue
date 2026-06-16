<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import { t } from "@/i18n";
import type { UserRole, WorkbenchMenuItem } from "@/types/workbench";

const props = defineProps<{
  items: WorkbenchMenuItem[];
  role: UserRole;
  expanded: boolean;
}>();

const route = useRoute();
const openGroups = ref<Record<string, boolean>>({ basicManagement: true, BASIC_MANAGEMENT: true });

const iconLabels: Record<string, string> = {
  LayoutDashboard: "DB",
  PackageSearch: "PK",
  Utensils: "FD",
  Library: "IM",
  Building2: "SP",
  FileText: "QT",
  Scale: "CP",
  ShoppingCart: "PO",
  Ship: "DL",
  Archive: "FN",
  UserCheck: "RG",
  ShieldCheck: "PM",
  FileSearch: "IQ",
  ClipboardList: "MP",
  Beef: "FP",
  Store: "ST"
};

const hasVisibleRole = (item: WorkbenchMenuItem) => item.roles.includes(props.role);
const visibleItems = computed(() => {
  const filterItems = (items: WorkbenchMenuItem[]): WorkbenchMenuItem[] =>
    items
      .map((item) => {
        const children = filterItems(item.children ?? []);
        if (children.length) return { ...item, children, roles: item.roles.length ? item.roles : children.flatMap((child) => child.roles) };
        return hasVisibleRole(item) && item.route ? item : null;
      })
      .filter(Boolean) as WorkbenchMenuItem[];
  return filterItems(props.items);
});
const isActive = (item: WorkbenchMenuItem): boolean =>
  Boolean(item.route && (route.path === item.route || route.path.startsWith(`${item.route}/`))) ||
  Boolean(item.children?.some(isActive));
const menuLabel = (item: WorkbenchMenuItem) => t(item.labelKey) || item.label;
const menuIcon = (item: WorkbenchMenuItem) => {
  const icon = item.icon.trim();
  return iconLabels[icon] || icon.slice(0, 3).toUpperCase();
};
const isGroupOpen = (item: WorkbenchMenuItem) => openGroups.value[item.key] ?? true;
const toggleGroup = (item: WorkbenchMenuItem) => {
  openGroups.value = { ...openGroups.value, [item.key]: !isGroupOpen(item) };
};
</script>

<template>
  <aside :class="['collapsible-sidebar', { 'is-expanded': expanded }]">
    <nav class="sidebar-menu" :aria-label="t('app.workbench')">
      <div v-for="item in visibleItems" :key="item.key" class="sidebar-item">
        <button
          v-if="item.children?.length"
          type="button"
          :class="['sidebar-link', 'sidebar-group-toggle', { active: isActive(item), 'is-open': isGroupOpen(item) }]"
          :title="menuLabel(item)"
          :aria-expanded="isGroupOpen(item)"
          @click="toggleGroup(item)"
        >
          <span class="sidebar-icon" aria-hidden="true">{{ menuIcon(item) }}</span>
          <span class="sidebar-label">{{ menuLabel(item) }}</span>
          <span class="sidebar-chevron" aria-hidden="true">&gt;</span>
        </button>
        <RouterLink
          v-else
          :class="['sidebar-link', { active: isActive(item) }]"
          :to="item.route || '/'"
          :title="menuLabel(item)"
        >
          <span class="sidebar-icon" aria-hidden="true">{{ menuIcon(item) }}</span>
          <span class="sidebar-label">{{ menuLabel(item) }}</span>
        </RouterLink>
        <div v-if="item.children?.length && isGroupOpen(item)" class="sidebar-submenu">
          <RouterLink
            v-for="child in item.children"
            :key="child.key"
            :class="['sidebar-link', 'sidebar-sublink', { active: isActive(child) }]"
            :to="child.route || '/'"
            :title="menuLabel(child)"
          >
            <span class="sidebar-icon" aria-hidden="true">{{ menuIcon(child) }}</span>
            <span class="sidebar-label">{{ menuLabel(child) }}</span>
          </RouterLink>
        </div>
      </div>
    </nav>
  </aside>
</template>

