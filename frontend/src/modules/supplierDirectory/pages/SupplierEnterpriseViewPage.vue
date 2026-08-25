<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import IconButton from "@/components/IconButton.vue";
import ImagePreviewModal from "@/components/ImagePreviewModal.vue";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import { getAuthSession } from "@/services/authService";
import ShopProductCatalog from "@/modules/shopManagement/components/ShopProductCatalog.vue";
import type { ShopCatalogSku } from "@/modules/shopManagement/types/shopProductCatalog";
import type { ShopQualitySelectionSavePayload } from "@/modules/shopManagement/types/shopProductCatalog";
import {
  downloadSupplierQualityReport,
  getSupplierDirectorySupplier,
  listSupplierDirectoryQualifications,
  listSupplierDirectoryProducts,
  listSupplierQualitySelections,
  loadSupplierQualificationImage,
  loadSupplierLogo,
  saveSupplierQualitySelection,
  updateSupplierDirectoryStatus,
  uploadSupplierQualityReport
} from "../services/supplierDirectoryService";
import {
  filterSupplierProducts,
  supplierProductClassificationGroups,
  type SupplierProductFilters
} from "../services/supplierDirectoryModel";
import {
  maskSupplierContactName,
  maskSupplierEmail,
  maskSupplierName,
  maskSupplierPhone
} from "../services/supplierDirectoryPrivacy";
import type { SupplierDirectoryItem, SupplierQualification } from "../types/supplierDirectory";
import "../styles/supplier-directory.css";

const route = useRoute();
const router = useRouter();
const supplier = ref<SupplierDirectoryItem | null>(null);
const products = ref<ShopCatalogSku[]>([]);
const qualifications = ref<Array<SupplierQualification & { imageUrl: string }>>([]);
const qualificationLoading = ref(false);
const qualificationError = ref("");
const qualificationPreviewOpen = ref(false);
const qualificationPreviewTitle = ref("");
const qualificationPreviewImages = ref<Array<{ src: string; alt: string }>>([]);
const loading = ref(false);
const statusSaving = ref(false);
const errorMessage = ref("");
const logoUrl = ref("");
const objectUrls = new Set<string>();
const companyId = computed(() => Number(route.params.supplierId));
const emptyFilters = (): SupplierProductFilters => ({ keyword: "", classification: "", codeStatus: "", shelfStatus: "" });
const filterDraft = reactive<SupplierProductFilters>(emptyFilters());
const appliedFilters = reactive<SupplierProductFilters>(emptyFilters());
const classificationGroups = computed(() => supplierProductClassificationGroups(products.value));
const filteredProducts = computed(() => filterSupplierProducts(products.value, appliedFilters));
const maskedSupplierName = computed(() => maskSupplierName(supplier.value?.name || ""));
const maskedContactName = computed(() => maskSupplierContactName(supplier.value?.contactName || ""));
const maskedContactPhone = computed(() => maskSupplierPhone(supplier.value?.contactPhone || ""));
const maskedContactEmail = computed(() => maskSupplierEmail(supplier.value?.contactEmail || ""));
const canManageStatus = computed(() => {
  const session = getAuthSession();
  const roles = (session?.roles || []).map((role) => role.toUpperCase());
  const companyType = String(session?.company?.companyType || session?.company?.type || "").toUpperCase();
  const userType = String(session?.user?.userType || "").toUpperCase();
  return roles.includes("PLATFORM_ADMIN") || companyType === "PLATFORM_ADMIN" || userType === "PLATFORM_ADMIN";
});

function applyProductFilters() {
  Object.assign(appliedFilters, filterDraft);
}

function resetProductFilters() {
  Object.assign(filterDraft, emptyFilters());
  Object.assign(appliedFilters, emptyFilters());
}

async function loadWorkspace() {
  if (!Number.isFinite(companyId.value) || companyId.value <= 0) {
    errorMessage.value = "供货商编号无效";
    return;
  }
  loading.value = true;
  errorMessage.value = "";
  try {
    const [profile, skuRows, qualityRows] = await Promise.all([
      getSupplierDirectorySupplier(companyId.value),
      listSupplierDirectoryProducts(companyId.value),
      listSupplierQualitySelections(companyId.value)
    ]);
    const qualityBySkuId = new Map(qualityRows.map((item) => [item.skuId, item]));
    skuRows.forEach((row) => {
      const selection = qualityBySkuId.get(Number(row.skuId));
      if (selection) row.qualitySelection = selection;
    });
    supplier.value = profile;
    products.value = skuRows;
    const source = profile.logoUrl || (profile.logoFileId ? `/api/files/${encodeURIComponent(profile.logoFileId)}` : "");
    logoUrl.value = source ? await loadSupplierLogo(source).catch(() => "") : "";
    if (logoUrl.value.startsWith("blob:")) objectUrls.add(logoUrl.value);
  } catch (error) {
    errorMessage.value = error instanceof Error && error.message ? error.message : "供货商企业信息加载失败";
  } finally {
    loading.value = false;
  }
}

async function saveQualitySelection(skuId: number, payload: ShopQualitySelectionSavePayload) {
  return saveSupplierQualitySelection(companyId.value, skuId, payload);
}

function isImageQualification(item: SupplierQualification): boolean {
  if (item.contentType.toLowerCase().startsWith("image/")) return true;
  return /\.(png|jpe?g|webp|gif|bmp)$/i.test(item.fileName);
}

async function loadQualifications() {
  if (!Number.isFinite(companyId.value) || companyId.value <= 0) return;
  qualificationLoading.value = true;
  qualificationError.value = "";
  try {
    const rows = await listSupplierDirectoryQualifications(companyId.value);
    qualifications.value = await Promise.all(rows.map(async (item) => {
      const imageUrl = isImageQualification(item)
        ? await loadSupplierQualificationImage(item.fileUrl).catch(() => "")
        : "";
      if (imageUrl.startsWith("blob:")) objectUrls.add(imageUrl);
      return { ...item, imageUrl };
    }));
  } catch (error) {
    qualificationError.value = error instanceof Error && error.message ? error.message : "企业证书加载失败";
  } finally {
    qualificationLoading.value = false;
  }
}

function openQualificationPreview(item: SupplierQualification & { imageUrl: string }) {
  if (!item.imageUrl) return;
  qualificationPreviewTitle.value = item.title || item.fileName || "企业证书";
  qualificationPreviewImages.value = [{ src: item.imageUrl, alt: qualificationPreviewTitle.value }];
  qualificationPreviewOpen.value = true;
}

async function toggleSupplierStatus() {
  if (!supplier.value || !canManageStatus.value || statusSaving.value) return;
  const previousStatus = supplier.value.status;
  const nextStatus = previousStatus === "ACTIVE" ? "DISABLED" : "ACTIVE";
  statusSaving.value = true;
  errorMessage.value = "";
  supplier.value.status = nextStatus;
  try {
    await updateSupplierDirectoryStatus(supplier.value.companyId, nextStatus);
  } catch (error) {
    supplier.value.status = previousStatus;
    errorMessage.value = error instanceof Error && error.message ? error.message : "服务商状态更新失败";
  } finally {
    statusSaving.value = false;
  }
}

onMounted(() => {
  void loadWorkspace();
  void loadQualifications();
});
onBeforeUnmount(() => objectUrls.forEach((url) => URL.revokeObjectURL(url)));
</script>

<template>
  <WorkbenchLayout>
    <section
      class="supplier-enterprise-page"
      tabindex="0"
      aria-label="供货商企业与商品名册"
    >
      <header class="supplier-enterprise-toolbar">
        <IconButton icon="ChevronLeft" label="返回供货商列表" @click="router.push('/suppliers')" />
        <div>
          <span>服务商管理</span>
          <strong>/</strong>
          <span>企业信息</span>
        </div>
        <IconButton icon="RefreshCw" label="刷新" :loading="loading" @click="loadWorkspace" />
      </header>

      <p v-if="errorMessage" class="supplier-directory-notice is-error">{{ errorMessage }}</p>

      <section v-if="supplier" class="supplier-enterprise-profile">
        <div class="supplier-enterprise-profile__logo">
          <img v-if="logoUrl" :src="logoUrl" :alt="`${maskedSupplierName}标志`" />
          <span v-else>{{ maskedSupplierName.slice(0, 1) }}</span>
        </div>
        <div class="supplier-enterprise-profile__main">
          <div class="supplier-enterprise-profile__title">
            <div>
              <h1>{{ maskedSupplierName }}</h1>
              <small aria-label="社会信用代码已隐藏">**********</small>
            </div>
            <div class="supplier-enterprise-profile__status">
              <button
                type="button"
                role="switch"
                class="supplier-enterprise-status-switch"
                :class="supplier.status === 'ACTIVE' ? 'is-active' : 'is-disabled'"
                :aria-checked="supplier.status === 'ACTIVE'"
                :aria-busy="statusSaving"
                :aria-label="!canManageStatus ? `当前状态：${supplier.status === 'ACTIVE' ? '启用' : '停用'}；仅平台管理员可修改` : supplier.status === 'ACTIVE' ? '停用服务商' : '启用服务商'"
                :title="!canManageStatus ? `当前状态：${supplier.status === 'ACTIVE' ? '启用' : '停用'}；仅平台管理员可修改` : supplier.status === 'ACTIVE' ? '停用服务商' : '启用服务商'"
                :disabled="statusSaving || !canManageStatus"
                @click="toggleSupplierStatus"
              ><span aria-hidden="true"></span></button>
            </div>
          </div>
          <div class="supplier-enterprise-profile__facts">
            <span><small>联系人</small><strong>{{ maskedContactName }}</strong></span>
            <span><small>联系电话</small><strong>{{ maskedContactPhone }}</strong></span>
            <span><small>联系邮箱</small><strong>{{ maskedContactEmail }}</strong></span>
            <span class="is-metric"><small>SKU 数量</small><strong>{{ supplier.skuCount.toLocaleString() }}</strong></span>
            <span class="is-metric"><small>商品大类</small><strong>{{ supplier.categoryCount }}</strong></span>
            <span class="is-metric">
              <small>评价情况</small>
              <span
                class="supplier-postcard__metric-stars supplier-enterprise-profile__rating-stars"
                :aria-label="supplier.evaluationCount > 0 ? `${Number(supplier.averageRating || 0).toFixed(1)}分` : '暂无评价'"
              >
                <i
                  v-for="point in 5"
                  :key="point"
                  :class="{ active: supplier.evaluationCount > 0 && point <= Math.round(Number(supplier.averageRating || 0)) }"
                  aria-hidden="true"
                >★</i>
              </span>
            </span>
          </div>
          <div class="supplier-enterprise-profile__introduction">
            <strong>企业介绍</strong>
            <p>{{ supplier.introduction || "暂未填写企业介绍" }}</p>
          </div>
        </div>
      </section>

      <section class="supplier-enterprise-products">
        <div class="supplier-enterprise-products__filters shop-list-toolbar" aria-label="供货商商品检索">
          <label class="shop-filter-field shop-filter-field--search">
            <span>关键词</span>
            <input v-model="filterDraft.keyword" placeholder="搜索商品名称、SKU、平台编码、条码或品牌" @keydown.enter.prevent="applyProductFilters" />
          </label>
          <label class="shop-filter-field">
            <span>商品分类</span>
            <select v-model="filterDraft.classification">
              <option value="">全部分类</option>
              <optgroup v-for="group in classificationGroups" :key="group.productType" :label="group.label">
                <option :value="`TYPE::${group.productType}`">{{ group.label }}（全部）</option>
                <option
                  v-for="category in group.categories"
                  :key="`${group.productType}-${category.name}`"
                  :value="`CATEGORY::${group.productType}::${encodeURIComponent(category.name)}`"
                >
                  {{ category.label }}（{{ category.count }}）
                </option>
              </optgroup>
            </select>
          </label>
          <label class="shop-filter-field">
            <span>编码状态</span>
            <select v-model="filterDraft.codeStatus">
              <option value="">全部</option>
              <option value="MATCHED_SUCCESS">已匹配</option>
              <option value="PENDING_EXCEPTION">待人工</option>
              <option value="EXCEPTION">异常</option>
            </select>
          </label>
          <label class="shop-filter-field">
            <span>上下架状态</span>
            <select v-model="filterDraft.shelfStatus">
              <option value="">全部</option>
              <option value="ON_SHELF">已上架</option>
              <option value="OFF_SHELF">已下架</option>
            </select>
          </label>
          <div class="supplier-enterprise-products__filter-actions toolbar-icon-actions">
            <IconButton icon="Search" label="检索" :loading="loading" @click="applyProductFilters" />
            <IconButton icon="X" label="重置" @click="resetProductFilters" />
          </div>
        </div>
        <ShopProductCatalog
          :rows="filteredProducts"
          :loading="loading"
          readonly
          :quality-editable="canManageStatus"
          :save-quality-selection="saveQualitySelection"
          :upload-quality-report="uploadSupplierQualityReport"
          :download-quality-report="downloadSupplierQualityReport"
        />
      </section>

      <section class="supplier-enterprise-qualifications" aria-labelledby="supplier-qualification-title">
        <header>
          <h2 id="supplier-qualification-title">企业证书</h2>
          <span>{{ qualifications.length }} 项</span>
        </header>
        <p v-if="qualificationError" class="supplier-directory-notice is-error">{{ qualificationError }}</p>
        <div v-if="qualificationLoading" class="supplier-enterprise-qualifications__empty" role="status">正在加载企业证书</div>
        <div v-else-if="!qualifications.length" class="supplier-enterprise-qualifications__empty">暂无企业证书</div>
        <div v-else class="supplier-enterprise-qualifications__grid">
          <article v-for="item in qualifications" :key="item.qualificationId" class="supplier-enterprise-qualification-card">
            <button
              v-if="item.imageUrl"
              type="button"
              class="supplier-enterprise-qualification-card__preview"
              :aria-label="`放大查阅${item.title || item.fileName}`"
              @click="openQualificationPreview(item)"
            >
              <img :src="item.imageUrl" :alt="item.title || item.fileName" />
              <span>点击放大</span>
            </button>
            <div v-else class="supplier-enterprise-qualification-card__file" aria-label="非图片证书文件">
              <strong>FILE</strong>
              <span>暂不支持图片预览</span>
            </div>
            <div class="supplier-enterprise-qualification-card__meta">
              <strong :title="item.title || item.fileName">{{ item.title || item.fileName }}</strong>
              <small>{{ item.updatedAt || "--" }}</small>
            </div>
          </article>
        </div>
      </section>

      <ImagePreviewModal
        :open="qualificationPreviewOpen"
        :title="qualificationPreviewTitle"
        :images="qualificationPreviewImages"
        @close="qualificationPreviewOpen = false"
      />
    </section>
  </WorkbenchLayout>
</template>
