<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import DataTable from "@/components/DataTable.vue";
import ExpandablePanel from "@/components/ExpandablePanel.vue";
import IconButton from "@/components/IconButton.vue";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import type { TableColumn } from "@/types/workbench";
import { getProvisionStandardCategories } from "../services/provisionStandardLibraryService";
import type { ProvisionStandardCategory } from "../types/provisionStandardLibrary";

const categories = ref<ProvisionStandardCategory[]>([]);
const keyword = ref("");
const activeCode = ref("");
const loading = ref(false);
const errorMessage = ref("");

const columns: TableColumn[] = [
  { key: "categoryCode", label: "分类编码", width: "24%" },
  { key: "nameCn", label: "中文名称", width: "28%" },
  { key: "nameEn", label: "英文名称", width: "34%" },
  { key: "levelLabel", label: "层级", width: "14%", align: "center" }
];

const tableRows = computed(() => categories.value
  .filter((item) => !activeCode.value || item.categoryCode === activeCode.value || item.parentCode === activeCode.value)
  .map((item) => ({ ...item, levelLabel: item.level === 1 ? "一级分类" : "二级分类" })));

async function loadCategories() {
  loading.value = true;
  errorMessage.value = "";
  try {
    categories.value = await getProvisionStandardCategories(keyword.value);
    if (activeCode.value && !categories.value.some((item) => item.categoryCode === activeCode.value)) activeCode.value = "";
  } catch (error) {
    errorMessage.value = error instanceof Error && error.message ? error.message : "伙食标准分类加载失败";
  } finally {
    loading.value = false;
  }
}

function selectCategory(category: ProvisionStandardCategory) {
  activeCode.value = activeCode.value === category.categoryCode ? "" : category.categoryCode;
}

onMounted(loadCategories);
</script>

<template>
  <WorkbenchLayout>
    <ExpandablePanel :show-header="false" :show-expand="false" class="impa-library-panel provision-library-panel" aria-labelledby="provision-library-title">
      <h1 id="provision-library-title" class="visually-hidden">伙食标准库</h1>
      <div class="impa-library-workspace">
        <div class="library-toolbar list-search-toolbar">
          <label class="library-search list-search-field">
            <span class="list-search-icon" aria-hidden="true">⌕</span>
            <input v-model="keyword" type="search" placeholder="搜索分类编码、中文名称或英文名称" @keyup.enter="loadCategories" />
          </label>
          <div class="toolbar-icon-actions">
            <IconButton icon="Search" label="检索" variant="primary" :loading="loading" @click="loadCategories" />
            <IconButton icon="RefreshCw" label="刷新" :loading="loading" @click="loadCategories" />
          </div>
        </div>
        <p v-if="errorMessage" class="supplier-directory-notice is-error">{{ errorMessage }}</p>
        <div class="library-shell">
          <nav class="library-tree" aria-label="伙食标准分类">
            <button
              v-for="category in categories"
              :key="category.categoryCode"
              type="button"
              :class="{ active: category.categoryCode === activeCode }"
              :aria-pressed="category.categoryCode === activeCode"
              @click="selectCategory(category)"
            >
              <strong>{{ category.categoryCode }}</strong>
              <span>{{ category.nameCn }}</span>
              <em>{{ category.level === 1 ? "一级" : "二级" }}</em>
            </button>
            <div v-if="loading || !categories.length" class="library-empty">{{ loading ? "正在加载" : "暂无分类" }}</div>
          </nav>
          <section class="library-detail" aria-label="伙食标准分类明细">
            <DataTable
              :columns="columns"
              :rows="tableRows"
              :loading="loading"
              row-key="categoryCode"
              empty-label="暂无符合条件的伙食标准分类"
            />
          </section>
        </div>
      </div>
    </ExpandablePanel>
  </WorkbenchLayout>
</template>

<style scoped>
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
