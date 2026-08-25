<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import ConfirmDialog from "@/components/ConfirmDialog.vue";
import IconButton from "@/components/IconButton.vue";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import { getAuthSession } from "@/services/authService";
import { useI18n } from "@/i18n";
import SupplierPostcard from "../components/SupplierPostcard.vue";
import { supplierDirectoryMessages } from "../locales";
import { filterSupplierDirectory, paginateSupplierDirectory, supplierDirectorySummary } from "../services/supplierDirectoryModel";
import { listSupplierDirectory, loadSupplierLogo, updateSupplierDirectoryStatus } from "../services/supplierDirectoryService";
import type { SupplierDirectoryFilters, SupplierDirectoryItem } from "../types/supplierDirectory";
import "../styles/supplier-directory.css";

const router = useRouter();
const { language } = useI18n();
const labels = computed(() => supplierDirectoryMessages[language.value === "en-US" ? "en-US" : "zh-CN"]);
const suppliers = ref<SupplierDirectoryItem[]>([]);
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const noticeMessage = ref("");
const page = ref(1);
const pageSize = 9;
const pendingStatusSupplier = ref<SupplierDirectoryItem | null>(null);
const logoUrls = ref<Record<number, string>>({});
const objectLogoUrls = new Set<string>();
const filters = reactive<SupplierDirectoryFilters>({ keyword: "", port: "", category: "", status: "" });

const filteredSuppliers = computed(() => filterSupplierDirectory(suppliers.value, filters));
const visibleSuppliers = computed(() => paginateSupplierDirectory(filteredSuppliers.value, page.value, pageSize));
const summary = computed(() => supplierDirectorySummary(filteredSuppliers.value));
const totalPages = computed(() => Math.max(1, Math.ceil(filteredSuppliers.value.length / pageSize)));
const servicePorts = computed(() => [...new Set(suppliers.value.flatMap((row) => row.servicePorts))].sort((a, b) => a.localeCompare(b, "zh-CN")));
const categories = computed(() => [...new Set(suppliers.value.flatMap((row) => row.categories))].sort((a, b) => a.localeCompare(b, "zh-CN")));
const canManageStatus = computed(() => {
  const session = getAuthSession();
  const roles = (session?.roles || []).map((role) => role.toUpperCase());
  const companyType = String(session?.company?.companyType || session?.company?.type || "").toUpperCase();
  return roles.includes("PLATFORM_ADMIN") || companyType === "PLATFORM_ADMIN";
});

watch(filters, () => { page.value = 1; }, { deep: true });
watch(totalPages, (value) => { if (page.value > value) page.value = value; });

async function hydrateLogos(rows: SupplierDirectoryItem[]) {
  const next: Record<number, string> = {};
  await Promise.all(rows.map(async (row) => {
    const source = row.logoUrl || (row.logoFileId ? `/api/files/${encodeURIComponent(row.logoFileId)}` : "");
    if (!source) return;
    const resolved = await loadSupplierLogo(source).catch(() => "");
    if (!resolved) return;
    next[row.companyId] = resolved;
    if (resolved.startsWith("blob:")) objectLogoUrls.add(resolved);
  }));
  logoUrls.value = next;
}

async function loadSuppliers() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const response = await listSupplierDirectory();
    suppliers.value = response.items;
    await hydrateLogos(response.items);
  } catch (error) {
    errorMessage.value = error instanceof Error && error.message ? error.message : "服务商管理查询失败";
    suppliers.value = [];
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.keyword = "";
  filters.port = "";
  filters.category = "";
  filters.status = "";
}

function openSupplier(supplier: SupplierDirectoryItem) {
  void router.push(`/suppliers/${supplier.companyId}/products`);
}

function requestStatusChange(supplier: SupplierDirectoryItem) {
  pendingStatusSupplier.value = supplier;
}

async function confirmStatusChange() {
  const supplier = pendingStatusSupplier.value;
  if (!supplier || saving.value) return;
  saving.value = true;
  errorMessage.value = "";
  noticeMessage.value = "";
  try {
    const nextStatus = supplier.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    await updateSupplierDirectoryStatus(supplier.companyId, nextStatus);
    supplier.status = nextStatus;
    noticeMessage.value = `${supplier.name}已${nextStatus === "ACTIVE" ? "启用" : "停用"}`;
    pendingStatusSupplier.value = null;
  } catch (error) {
    errorMessage.value = error instanceof Error && error.message ? error.message : "服务商状态更新失败";
  } finally {
    saving.value = false;
  }
}

onMounted(loadSuppliers);
onBeforeUnmount(() => objectLogoUrls.forEach((url) => URL.revokeObjectURL(url)));
</script>

<template>
  <WorkbenchLayout>
    <section class="supplier-directory-page">
      <section class="supplier-directory-overview" aria-label="服务商统计" aria-live="polite">
        <div class="supplier-directory-overview__stat is-total">
          <span>服务商总数</span>
          <strong>{{ summary.supplierCount }}</strong>
        </div>
        <div class="supplier-directory-overview__stat is-active">
          <span>启用服务商</span>
          <strong>{{ summary.activeCount }}</strong>
        </div>
        <div class="supplier-directory-overview__stat is-products">
          <span>商品总数</span>
          <strong>{{ summary.skuCount.toLocaleString() }}</strong>
        </div>
      </section>

      <section class="supplier-directory-filter" aria-label="服务商筛选">
        <label class="supplier-directory-filter__keyword">
          <span>{{ labels.search }}</span>
          <input v-model="filters.keyword" :placeholder="labels.keywordPlaceholder" :aria-label="labels.search" />
        </label>
        <label>
          <span>{{ labels.servicePort }}</span>
          <select v-model="filters.port"><option value="">{{ labels.all }}</option><option v-for="port in servicePorts" :key="port" :value="port">{{ port }}</option></select>
        </label>
        <label>
          <span>{{ labels.category }}</span>
          <select v-model="filters.category"><option value="">{{ labels.all }}</option><option v-for="category in categories" :key="category" :value="category">{{ category }}</option></select>
        </label>
        <label>
          <span>{{ labels.status }}</span>
          <select v-model="filters.status"><option value="">{{ labels.all }}</option><option value="ACTIVE">{{ labels.enabled }}</option><option value="DISABLED">{{ labels.disabled }}</option></select>
        </label>
        <div class="supplier-directory-filter__actions">
          <IconButton icon="Search" label="查询" :loading="loading" @click="page = 1" />
          <IconButton icon="X" label="重置" @click="resetFilters" />
          <IconButton icon="RefreshCw" label="刷新" :loading="loading" @click="loadSuppliers" />
        </div>
      </section>

      <p v-if="errorMessage" class="supplier-directory-notice is-error">{{ errorMessage }}</p>
      <p v-else-if="noticeMessage" class="supplier-directory-notice">{{ noticeMessage }}</p>

      <div v-if="loading" class="supplier-directory-state"><span class="loading-spinner" aria-hidden="true"></span><strong>{{ labels.loading }}</strong></div>
      <div v-else-if="!visibleSuppliers.length" class="supplier-directory-state"><strong>{{ labels.empty }}</strong></div>
      <section v-else class="supplier-directory-grid" aria-label="服务商明信片目录">
        <SupplierPostcard
          v-for="supplier in visibleSuppliers"
          :key="supplier.companyId"
          :supplier="supplier"
          :logo-url="logoUrls[supplier.companyId]"
          :can-manage-status="canManageStatus"
          :labels="labels"
          @view="openSupplier"
          @toggle-status="requestStatusChange"
        />
      </section>

      <footer v-if="filteredSuppliers.length > pageSize" class="supplier-directory-pagination">
        <span>显示 {{ (page - 1) * pageSize + 1 }}-{{ Math.min(page * pageSize, filteredSuppliers.length) }} 家，共 {{ filteredSuppliers.length }} 家</span>
        <div>
          <IconButton icon="ChevronLeft" label="上一页" :disabled="page <= 1" @click="page -= 1" />
          <strong>第 {{ page }} / {{ totalPages }} 页</strong>
          <IconButton icon="ChevronRight" label="下一页" :disabled="page >= totalPages" @click="page += 1" />
        </div>
      </footer>
    </section>

    <ConfirmDialog
      :open="Boolean(pendingStatusSupplier)"
      :title="pendingStatusSupplier?.status === 'ACTIVE' ? labels.disable : labels.enable"
      :message="pendingStatusSupplier?.status === 'ACTIVE' ? labels.confirmDisable : labels.confirmEnable"
      :danger="pendingStatusSupplier?.status === 'ACTIVE'"
      :confirm-label="pendingStatusSupplier?.status === 'ACTIVE' ? labels.disable : labels.enable"
      @close="pendingStatusSupplier = null"
      @confirm="confirmStatusChange"
    />
  </WorkbenchLayout>
</template>
