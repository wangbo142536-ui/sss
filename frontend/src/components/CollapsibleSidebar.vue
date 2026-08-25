<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { t } from "@/i18n";
import type { UserRole, WorkbenchMenuItem } from "@/types/workbench";

const sidebarOpenGroups = ref<Record<string, boolean>>({});

const props = defineProps<{
  items: WorkbenchMenuItem[];
  role: UserRole | null;
  expanded: boolean;
}>();

const route = useRoute();
const router = useRouter();
const openGroups = sidebarOpenGroups;

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

const hasVisibleRole = (item: WorkbenchMenuItem) => Boolean(props.role && item.roles.includes(props.role));
const visibleItems = computed(() => {
  const filterItems = (items: WorkbenchMenuItem[]): WorkbenchMenuItem[] =>
    items
      .map((item) => {
        const children = filterItems(item.children ?? []);
        if (children.length) return { ...item, children, roles: item.roles.length ? item.roles : children.flatMap((child) => child.roles) };
        return hasVisibleRole(item) ? item : null;
      })
      .filter(Boolean) as WorkbenchMenuItem[];
  return filterItems(props.items);
});
const routeMatches = (item: WorkbenchMenuItem) => {
  if (!item.route) return false;
  if (route.path === item.route) return true;
  return item.route !== "/traffic-boat" && route.path.startsWith(`${item.route}/`);
};
const isActive = (item: WorkbenchMenuItem): boolean =>
  routeMatches(item) ||
  Boolean(item.children?.some(isActive));
const menuLabel = (item: WorkbenchMenuItem) => t(item.labelKey) || item.label;
const menuIcon = (item: WorkbenchMenuItem) => {
  const icon = item.icon.trim();
  return iconLabels[icon] || icon.slice(0, 3).toUpperCase();
};
const isGroupOpen = (item: WorkbenchMenuItem) => openGroups.value[item.key] ?? Boolean(item.children?.some(isActive));
const toggleGroup = (item: WorkbenchMenuItem) => {
  openGroups.value = { ...openGroups.value, [item.key]: !isGroupOpen(item) };
};
const navigateToMenu = (item: WorkbenchMenuItem) => {
  if (!item.route) return;
  router.push({ path: item.route, query: { _menuRefresh: String(Date.now()) } });
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
        <button
          v-else-if="item.route"
          type="button"
          :class="['sidebar-link', { active: isActive(item) }]"
          :title="menuLabel(item)"
          @click="navigateToMenu(item)"
        >
          <span class="sidebar-icon" aria-hidden="true">{{ menuIcon(item) }}</span>
          <span class="sidebar-label">{{ menuLabel(item) }}</span>
        </button>
        <button v-else type="button" class="sidebar-link sidebar-placeholder" :title="menuLabel(item)">
          <span class="sidebar-icon" aria-hidden="true">{{ menuIcon(item) }}</span>
          <span class="sidebar-label">{{ menuLabel(item) }}</span>
        </button>
        <div v-if="item.children?.length && isGroupOpen(item)" class="sidebar-submenu">
          <div v-for="child in item.children" :key="child.key" class="sidebar-item sidebar-child-item">
            <button
              v-if="child.children?.length"
              type="button"
              :class="['sidebar-link', 'sidebar-sublink', 'sidebar-group-toggle', { active: isActive(child), 'is-open': isGroupOpen(child) }]"
              :title="menuLabel(child)"
              :aria-expanded="isGroupOpen(child)"
              @click="toggleGroup(child)"
            >
              <span class="sidebar-icon" aria-hidden="true">{{ menuIcon(child) }}</span>
              <span class="sidebar-label">{{ menuLabel(child) }}</span>
              <span class="sidebar-chevron" aria-hidden="true">&gt;</span>
            </button>
            <button
              v-else-if="child.route"
              type="button"
              :class="['sidebar-link', 'sidebar-sublink', { active: isActive(child) }]"
              :title="menuLabel(child)"
              @click="navigateToMenu(child)"
            >
              <span class="sidebar-icon" aria-hidden="true">{{ menuIcon(child) }}</span>
              <span class="sidebar-label">{{ menuLabel(child) }}</span>
            </button>
            <button v-else type="button" class="sidebar-link sidebar-sublink sidebar-placeholder" :title="menuLabel(child)">
              <span class="sidebar-icon" aria-hidden="true">{{ menuIcon(child) }}</span>
              <span class="sidebar-label">{{ menuLabel(child) }}</span>
            </button>
            <div v-if="child.children?.length && isGroupOpen(child)" class="sidebar-submenu sidebar-submenu-nested">
              <button
                v-for="grandchild in child.children"
                :key="grandchild.key"
                type="button"
                :class="['sidebar-link', 'sidebar-sublink', 'sidebar-thirdlink', { active: isActive(grandchild) }]"
                :title="menuLabel(grandchild)"
                @click="navigateToMenu(grandchild)"
              >
                <span class="sidebar-icon" aria-hidden="true">{{ menuIcon(grandchild) }}</span>
                <span class="sidebar-label">{{ menuLabel(grandchild) }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </nav>
  </aside>
</template>

