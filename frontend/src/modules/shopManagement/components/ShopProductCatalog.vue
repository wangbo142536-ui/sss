<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import IconButton from "@/components/IconButton.vue";
import ShopProductCatalogEditor from "./ShopProductCatalogEditor.vue";
import { buildShopProductCatalog } from "../composables/useShopProductCatalog";
import { resolveShopCatalogCover } from "../services/shopCatalogCoverService";
import type {
  ShopCatalogCategoryOption,
  ShopCatalogSku,
  ShopQualitySelection,
  ShopQualitySelectionSavePayload
} from "../types/shopProductCatalog";
import "../styles/shop-product-catalog.css";

const props = withDefaults(
  defineProps<{
    rows: ShopCatalogSku[];
    loading?: boolean;
    readonly?: boolean;
    categoryOptions?: ShopCatalogCategoryOption[];
    saveSku?: (sku: ShopCatalogSku) => Promise<void>;
    qualityEditable?: boolean;
    saveQualitySelection?: (skuId: number, payload: ShopQualitySelectionSavePayload) => Promise<ShopQualitySelection>;
    uploadQualityReport?: (file: File) => Promise<{ fileId: string; fileName: string }>;
    downloadQualityReport?: (reportUrl: string, fileName: string) => Promise<void>;
  }>(),
  {
    loading: false,
    readonly: false,
    categoryOptions: () => [],
    saveSku: undefined,
    qualityEditable: false,
    saveQualitySelection: undefined,
    uploadQualityReport: undefined,
    downloadQualityReport: undefined
  }
);

const selectedGroupKey = ref("");
const selectedCategoryKey = ref("");
const editingSku = ref<ShopCatalogSku | null>(null);
const editorSaving = ref(false);
const editorError = ref("");
const PREVIEW_DEFAULT_SLOT_COUNT = 6;
const PREVIEW_BOOK_WIDTH = 112;
const PREVIEW_BOOK_GAP = 9;
const PREVIEW_BOOKS_HORIZONTAL_PADDING = 4;
const CATEGORY_SHELF_SIZE = 7;
const previewShelfSlots = ref<Record<string, number>>({});
const previewShelfElements = new Map<string, HTMLElement>();
const previewShelfObservers = new Map<string, ResizeObserver>();
const catalog = computed(() => buildShopProductCatalog(props.rows));
const currentGroup = computed(() => catalog.value.find((group) => group.key === selectedGroupKey.value));
const currentCategory = computed(() => currentGroup.value?.categories.find((category) => category.key === selectedCategoryKey.value));
const level = computed<"groups" | "categories" | "skus">(() => {
  if (currentCategory.value) return "skus";
  if (currentGroup.value) return "categories";
  return "groups";
});

function openGroup(groupKey: string) {
  selectedGroupKey.value = groupKey;
  selectedCategoryKey.value = "";
}

function openCategory(categoryKey: string) {
  if (!currentGroup.value?.categories.some((category) => category.key === categoryKey)) return;
  selectedCategoryKey.value = categoryKey;
}

function openCategoryFromGroup(groupKey: string, categoryKey: string) {
  selectedGroupKey.value = groupKey;
  selectedCategoryKey.value = categoryKey;
}

function previewSlotCount(groupKey: string): number {
  return Math.max(1, previewShelfSlots.value[groupKey] || PREVIEW_DEFAULT_SLOT_COUNT);
}

function visiblePreviewCategories(group: ReturnType<typeof buildShopProductCatalog>[number]) {
  const slotCount = previewSlotCount(group.key);
  const visibleCount = group.categories.length > slotCount ? Math.max(0, slotCount - 1) : group.categories.length;
  return group.categories.slice(0, visibleCount);
}

function hiddenPreviewCategoryCount(group: ReturnType<typeof buildShopProductCatalog>[number]): number {
  return Math.max(0, group.categories.length - visiblePreviewCategories(group).length);
}

function updatePreviewShelfSlots(groupKey: string, element: HTMLElement) {
  const availableWidth = Math.max(0, element.clientWidth - PREVIEW_BOOKS_HORIZONTAL_PADDING);
  if (availableWidth <= 0) return;
  const slotCount = Math.max(1, Math.floor((availableWidth + PREVIEW_BOOK_GAP) / (PREVIEW_BOOK_WIDTH + PREVIEW_BOOK_GAP)));
  if (previewShelfSlots.value[groupKey] === slotCount) return;
  previewShelfSlots.value = { ...previewShelfSlots.value, [groupKey]: slotCount };
}

function setPreviewShelfElement(groupKey: string, candidate: unknown) {
  const currentElement = previewShelfElements.get(groupKey);
  if (candidate === currentElement) return;

  previewShelfObservers.get(groupKey)?.disconnect();
  previewShelfObservers.delete(groupKey);
  previewShelfElements.delete(groupKey);

  if (!(candidate instanceof HTMLElement)) return;
  previewShelfElements.set(groupKey, candidate);
  updatePreviewShelfSlots(groupKey, candidate);
  if (typeof ResizeObserver === "undefined") return;

  const observer = new ResizeObserver(() => updatePreviewShelfSlots(groupKey, candidate));
  observer.observe(candidate);
  previewShelfObservers.set(groupKey, observer);
}

function categoryShelves(group: ReturnType<typeof buildShopProductCatalog>[number]) {
  const shelves = [];
  for (let index = 0; index < group.categories.length; index += CATEGORY_SHELF_SIZE) {
    shelves.push(group.categories.slice(index, index + CATEGORY_SHELF_SIZE));
  }
  return shelves;
}

function bookStyle(index: number): Record<string, string> {
  return { "--book-index": String(index) };
}

function isUncategorized(categoryKey: string): boolean {
  return categoryKey === "UNCATEGORIZED";
}

function goHome() {
  selectedGroupKey.value = "";
  selectedCategoryKey.value = "";
}

function goBack() {
  if (level.value === "skus") {
    selectedCategoryKey.value = "";
    return;
  }
  goHome();
}

function formatCurrency(value: number, currency: string): string {
  const amount = Number.isFinite(Number(value)) ? Number(value) : 0;
  const normalized = String(currency || "CNY").trim().toUpperCase();
  const symbol = normalized === "USD" ? "$" : normalized === "CNY" || normalized === "RMB" ? "¥" : `${normalized} `;
  return `${symbol}${amount.toFixed(2)}`;
}

function skuCode(row: ShopCatalogSku): string {
  return row.platformCode || row.impaCode || row.supplierSkuCode || "暂无编码";
}

function skuImage(row: ShopCatalogSku): string {
  return row.thumbnail || row.imageUrl || "";
}

function visibleProductTags(row: ShopCatalogSku): string[] {
  return (row.productTags || []).map((tag) => tag.trim()).filter(Boolean).slice(0, 2);
}

function hiddenProductTagCount(row: ShopCatalogSku): number {
  return Math.max(0, (row.productTags || []).map((tag) => tag.trim()).filter(Boolean).length - 2);
}

function productTagTone(tag: string): string {
  if (/热卖|爆款|hot/i.test(tag)) return "hot";
  if (/上新|新品|new/i.test(tag)) return "new";
  if (/特卖|特价|促销|sale/i.test(tag)) return "sale";
  if (/质量|品质|quality/i.test(tag)) return "quality";
  if (/实惠|性价比|价格|value/i.test(tag)) return "value";
  return "default";
}

function openSkuEditor(row: ShopCatalogSku) {
  editingSku.value = row;
  editorError.value = "";
}

function closeSkuEditor() {
  if (editorSaving.value) return;
  editingSku.value = null;
  editorError.value = "";
}

async function saveEditedSku(draft: ShopCatalogSku) {
  if (!props.saveSku) {
    editorError.value = "当前账户没有商品编辑权限";
    return;
  }
  editorSaving.value = true;
  editorError.value = "";
  try {
    await props.saveSku(draft);
    editorSaving.value = false;
    closeSkuEditor();
  } catch (error) {
    editorError.value = error instanceof Error && error.message ? error.message : "商品保存失败，请稍后重试";
  } finally {
    editorSaving.value = false;
  }
}

async function saveQualitySelection(skuId: number, payload: ShopQualitySelectionSavePayload): Promise<ShopQualitySelection> {
  if (!props.saveQualitySelection) throw new Error("当前账户没有海事严选维护权限");
  const saved = await props.saveQualitySelection(skuId, payload);
  const row = props.rows.find((item) => Number(item.skuId) === skuId);
  if (row) row.qualitySelection = saved;
  if (editingSku.value && Number(editingSku.value.skuId) === skuId) editingSku.value.qualitySelection = saved;
  return saved;
}

watch(
  catalog,
  (groups) => {
    const group = groups.find((item) => item.key === selectedGroupKey.value);
    if (!group) {
      goHome();
      return;
    }
    if (selectedCategoryKey.value && !group.categories.some((item) => item.key === selectedCategoryKey.value)) {
      selectedCategoryKey.value = "";
    }
  },
  { flush: "sync" }
);

onBeforeUnmount(() => {
  previewShelfObservers.forEach((observer) => observer.disconnect());
  previewShelfObservers.clear();
  previewShelfElements.clear();
});
</script>

<template>
  <section class="shop-product-catalog" :aria-busy="loading">
    <header v-if="level !== 'groups'" class="shop-product-catalog__navigation">
      <IconButton icon="ChevronLeft" label="返回上一级" @click="goBack" />
      <nav aria-label="产品名册路径">
        <button type="button" @click="goHome">产品名册</button>
        <span aria-hidden="true">/</span>
        <button type="button" @click="openGroup(currentGroup?.key || '')">{{ currentGroup?.label }}</button>
        <template v-if="currentCategory">
          <span aria-hidden="true">/</span>
          <span aria-current="page">{{ currentCategory.label }}</span>
        </template>
      </nav>
      <span class="shop-product-catalog__total">{{ currentCategory?.skuCount ?? currentGroup?.skuCount ?? 0 }} 个 SKU</span>
    </header>

    <div v-if="loading" class="shop-product-catalog__loading" role="status" aria-live="polite">
      <span class="loading-spinner" aria-hidden="true"></span>
      <span>正在整理产品名册</span>
    </div>

    <div v-else-if="!catalog.length" class="shop-product-catalog__empty" role="status">
      <strong>当前筛选条件下暂无商品</strong>
      <span>调整检索条件后，名册会按真实商品分类自动整理。</span>
    </div>

    <div v-else-if="level === 'groups'" class="shop-product-catalog__cabinet-grid shop-product-catalog__view" aria-label="商品大类书柜">
      <article
        v-for="group in catalog"
        :key="group.key"
        class="shop-product-catalog__cabinet"
        :aria-label="`${group.label}书柜，${group.skuCount}个SKU`"
      >
        <header class="shop-product-catalog__cabinet-heading">
          <span class="shop-product-catalog__cabinet-identity">
            <span class="shop-product-catalog__cabinet-mark" aria-hidden="true">{{ group.key === "FOOD" ? "食" : "物" }}</span>
            <span>
              <strong class="shop-product-catalog__cabinet-title">{{ group.label }}</strong>
              <small>{{ group.categories.length }} 个分类</small>
            </span>
          </span>
          <span class="shop-product-catalog__cabinet-actions">
            <span>{{ group.skuCount }} 个 SKU</span>
            <IconButton icon="BookOpen" :label="`打开${group.label}书柜，${group.skuCount}个SKU`" @click="openGroup(group.key)" />
          </span>
        </header>

        <div class="shop-product-catalog__cabinet-body">
          <div
            class="shop-product-catalog__shelf-row shop-product-catalog__shelf-row--preview"
            data-catalog-shelf="preview"
            :data-catalog-group="group.key"
          >
            <div :ref="(element) => setPreviewShelfElement(group.key, element)" class="shop-product-catalog__shelf-books">
              <button
                v-for="(category, bookIndex) in visiblePreviewCategories(group)"
                :key="category.key"
                type="button"
                :class="[
                  'shop-product-catalog__preview-book',
                  `shop-product-catalog__book-tone--${(bookIndex + 1) % 4}`,
                  { 'is-uncategorized': isUncategorized(category.key) }
                ]"
                :style="bookStyle(bookIndex)"
                :aria-label="`${isUncategorized(category.key) ? '异常：' : ''}打开${group.label}中的${category.label}书册，${category.skuCount}个SKU`"
                @click="openCategoryFromGroup(group.key, category.key)"
              >
                <span v-if="isUncategorized(category.key)" class="shop-product-catalog__book-alert">待归类</span>
                <span class="shop-product-catalog__book-cover" aria-hidden="true">
                  <img :src="resolveShopCatalogCover(category, group.key)" alt="" loading="lazy" />
                </span>
                <span class="shop-product-catalog__book-label">{{ category.label }}</span>
                <span class="shop-product-catalog__book-count"><strong>{{ category.skuCount }}</strong> SKU</span>
              </button>

              <button
                v-if="hiddenPreviewCategoryCount(group) > 0"
                type="button"
                class="shop-product-catalog__preview-book shop-product-catalog__book-more"
                :style="bookStyle(visiblePreviewCategories(group).length)"
                :aria-label="`查看${group.label}全部${group.categories.length}个分类`"
                @click="openGroup(group.key)"
              >
                <span class="shop-product-catalog__book-cover shop-product-catalog__book-cover--more" aria-hidden="true">
                  <img :src="resolveShopCatalogCover({ code: '', label: '' }, group.key)" alt="" loading="lazy" />
                </span>
                <span class="shop-product-catalog__book-label">更多分类</span>
                <span class="shop-product-catalog__book-count">+{{ hiddenPreviewCategoryCount(group) }}</span>
              </button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <div
      v-else-if="level === 'categories' && currentGroup"
      class="shop-product-catalog__shelf-section shop-product-catalog__view"
      :aria-label="`${currentGroup.label}分类书架`"
    >
      <header>
        <div>
          <strong>{{ currentGroup.label }}</strong>
          <span>选择一本分类书册，查看其中的全部商品</span>
        </div>
        <span>{{ currentGroup.categories.length }} 个分类</span>
      </header>
      <div class="shop-product-catalog__category-cabinet-body">
        <div
          v-for="(shelf, shelfIndex) in categoryShelves(currentGroup)"
          :key="shelfIndex"
          class="shop-product-catalog__shelf-row shop-product-catalog__shelf-row--categories"
        >
          <div class="shop-product-catalog__shelf-books">
            <button
              v-for="(category, bookIndex) in shelf"
              :key="category.key"
              type="button"
              :class="[
                'shop-product-catalog__category-book',
                `shop-product-catalog__book-tone--${(bookIndex + shelfIndex) % 4}`,
                { 'is-uncategorized': isUncategorized(category.key) }
              ]"
              :style="bookStyle(bookIndex + shelfIndex * CATEGORY_SHELF_SIZE)"
              :aria-label="`${isUncategorized(category.key) ? '异常：' : ''}打开${category.label}书册，${category.skuCount}个SKU`"
              @click="openCategory(category.key)"
            >
              <span class="shop-product-catalog__category-cover" aria-hidden="true">
                <img :src="resolveShopCatalogCover(category, currentGroup.key)" alt="" loading="lazy" />
                <span class="shop-product-catalog__category-code">{{ isUncategorized(category.key) ? "待归类" : category.code || "未编码" }}</span>
              </span>
              <strong>{{ category.label }}</strong>
              <span class="shop-product-catalog__category-meta">{{ category.skuCount }} 个 SKU</span>
              <span class="shop-product-catalog__category-action" aria-hidden="true">查看 SKU <span>→</span></span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="currentCategory" class="shop-product-catalog__sku-grid shop-product-catalog__view" :aria-label="`${currentCategory.label}SKU列表`">
      <article v-for="row in currentCategory.skus" :key="row.id" class="shop-product-catalog__sku-card">
        <button type="button" class="shop-product-catalog__sku-card-button" :aria-label="`${readonly ? '查看' : '编辑'}商品${row.productName || '未命名商品'}`" @click="openSkuEditor(row)">
          <div class="shop-product-catalog__sku-image">
            <span v-if="row.qualitySelection?.status === 'ACTIVE'" class="shop-product-catalog__quality-badge">海事严选</span>
            <span v-if="visibleProductTags(row).length" class="shop-product-catalog__sku-tags" aria-label="商品标签">
              <span v-for="tag in visibleProductTags(row)" :key="tag" :class="`is-${productTagTone(tag)}`">{{ tag }}</span>
              <span v-if="hiddenProductTagCount(row)" class="is-more">+{{ hiddenProductTagCount(row) }}</span>
            </span>
            <img v-if="skuImage(row)" :src="skuImage(row)" :alt="row.productName" loading="lazy" />
            <span v-else aria-label="暂无商品图片">暂无图片</span>
          </div>
          <div class="shop-product-catalog__sku-content">
            <div class="shop-product-catalog__sku-heading">
              <strong>{{ row.productName || "未命名商品" }}</strong>
              <span>{{ skuCode(row) }}</span>
            </div>
            <footer>
              <div>
                <span>{{ row.packing || "包装未填写" }}</span>
                <strong>库存 {{ row.stock ?? 0 }} {{ row.unit || "件" }}</strong>
              </div>
              <strong class="shop-product-catalog__sku-price">{{ formatCurrency(row.price, row.currency) }}</strong>
            </footer>
          </div>
        </button>
      </article>
    </div>

    <ShopProductCatalogEditor
      :open="Boolean(editingSku)"
      :sku="editingSku"
      :category-options="categoryOptions"
      :saving="editorSaving"
      :error="editorError"
      :readonly="readonly"
      :quality-editable="qualityEditable"
      :save-quality-selection="saveQualitySelection"
      :upload-quality-report="uploadQualityReport"
      :download-quality-report="downloadQualityReport"
      @close="closeSkuEditor"
      @save="saveEditedSku"
    />
  </section>
</template>
