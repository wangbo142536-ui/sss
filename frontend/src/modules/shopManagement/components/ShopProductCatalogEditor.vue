<script setup lang="ts">
import { ref, watch } from "vue";
import IconButton from "@/components/IconButton.vue";
import type {
  ShopCatalogCategoryOption,
  ShopCatalogSku,
  ShopCatalogSpec,
  ShopQualitySelection,
  ShopQualitySelectionSavePayload
} from "../types/shopProductCatalog";
import "../styles/shop-product-catalog-editor.css";

const props = withDefaults(
  defineProps<{
    open: boolean;
    sku: ShopCatalogSku | null;
    categoryOptions?: ShopCatalogCategoryOption[];
    saving?: boolean;
    error?: string;
    readonly?: boolean;
    qualityEditable?: boolean;
    saveQualitySelection?: (skuId: number, payload: ShopQualitySelectionSavePayload) => Promise<ShopQualitySelection>;
    uploadQualityReport?: (file: File) => Promise<{ fileId: string; fileName: string }>;
    downloadQualityReport?: (reportUrl: string, fileName: string) => Promise<void>;
  }>(),
  {
    categoryOptions: () => [],
    saving: false,
    error: "",
    readonly: false,
    qualityEditable: false,
    saveQualitySelection: undefined,
    uploadQualityReport: undefined,
    downloadQualityReport: undefined
  }
);

const emit = defineEmits<{
  close: [];
  save: [sku: ShopCatalogSku];
}>();

const draft = ref<ShopCatalogSku | null>(null);
const validationError = ref("");
const qualitySaving = ref(false);
const qualityUploading = ref(false);
const qualityError = ref("");
const qualityReportInput = ref<HTMLInputElement | null>(null);
const qualityDraft = ref<ShopQualitySelectionSavePayload>({
  inspectionTime: "",
  inspectionContent: "",
  inspectionProcess: "",
  reportFileId: "",
  reportFileName: "",
  inspectionConclusion: ""
});

function dateTimeLocal(value: string): string {
  return value ? value.replace(" ", "T").slice(0, 16) : "";
}

function resetQualityDraft(selection?: ShopQualitySelection) {
  qualityDraft.value = {
    inspectionTime: dateTimeLocal(selection?.inspectionTime || ""),
    inspectionContent: selection?.inspectionContent || "",
    inspectionProcess: selection?.inspectionProcess || "",
    reportFileId: selection?.reportFileId || "",
    reportFileName: selection?.reportFileName || "",
    inspectionConclusion: selection?.inspectionConclusion || ""
  };
  qualityError.value = "";
}

function cloneSku(sku: ShopCatalogSku): ShopCatalogSku {
  return {
    ...sku,
    productTags: [...(sku.productTags || [])],
    specs: [...(sku.specs || [])],
    specItems: (sku.specItems || []).map((item) => ({ ...item }))
  };
}

function addTag() {
  if (props.readonly || !draft.value || (draft.value.productTags?.length || 0) >= 6) return;
  draft.value.productTags = [...(draft.value.productTags || []), ""];
}

function removeTag(index: number) {
  if (props.readonly || !draft.value) return;
  draft.value.productTags = (draft.value.productTags || []).filter((_, itemIndex) => itemIndex !== index);
}

function close() {
  if (!props.saving) emit("close");
}

function updateCategory(categoryCode: string) {
  if (props.readonly || !draft.value) return;
  const option = props.categoryOptions.find((item) => item.value === categoryCode);
  draft.value.categoryCode = categoryCode;
  draft.value.category = option?.label || categoryCode;
  draft.value.categoryName = option?.label || categoryCode;
}

function addSpec() {
  if (props.readonly || !draft.value) return;
  const item: ShopCatalogSpec = {
    id: `catalog-spec-${Date.now()}-${draft.value.specItems?.length || 0}`,
    key: "specification",
    name: "规格",
    value: "",
    unit: ""
  };
  draft.value.specItems = [...(draft.value.specItems || []), item];
}

function removeSpec(index: number) {
  if (props.readonly || !draft.value) return;
  draft.value.specItems = (draft.value.specItems || []).filter((_, itemIndex) => itemIndex !== index);
}

function submit() {
  if (props.readonly || !draft.value || props.saving) return;
  if (!draft.value.productName.trim()) {
    validationError.value = "请填写商品名称";
    return;
  }
  if (!draft.value.supplierSkuCode.trim()) {
    validationError.value = "请填写企业 SKU";
    return;
  }
  validationError.value = "";
  emit("save", cloneSku(draft.value));
}

async function handleQualityReport(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file || !props.uploadQualityReport) return;
  qualityUploading.value = true;
  qualityError.value = "";
  try {
    const uploaded = await props.uploadQualityReport(file);
    qualityDraft.value.reportFileId = uploaded.fileId;
    qualityDraft.value.reportFileName = uploaded.fileName;
  } catch (error) {
    qualityError.value = error instanceof Error ? error.message : "检测报告上传失败";
  } finally {
    qualityUploading.value = false;
    (event.target as HTMLInputElement).value = "";
  }
}

async function saveQuality() {
  const skuId = Number(draft.value?.skuId);
  if (!props.qualityEditable || !props.saveQualitySelection || !Number.isFinite(skuId) || skuId <= 0) return;
  if (!qualityDraft.value.inspectionTime || !qualityDraft.value.inspectionContent.trim() || !qualityDraft.value.inspectionConclusion.trim()) {
    qualityError.value = "请填写QC检查时间、检查内容和检测结论";
    return;
  }
  qualitySaving.value = true;
  qualityError.value = "";
  try {
    const saved = await props.saveQualitySelection(skuId, { ...qualityDraft.value });
    if (draft.value) draft.value.qualitySelection = saved;
    resetQualityDraft(saved);
  } catch (error) {
    qualityError.value = error instanceof Error ? error.message : "海事严选保存失败";
  } finally {
    qualitySaving.value = false;
  }
}

async function downloadQuality() {
  const selection = draft.value?.qualitySelection;
  if (!selection?.reportUrl || !props.downloadQualityReport) return;
  try {
    await props.downloadQualityReport(selection.reportUrl, selection.reportFileName);
  } catch (error) {
    qualityError.value = error instanceof Error ? error.message : "检测报告下载失败";
  }
}

watch(
  () => [props.open, props.sku] as const,
  ([open, sku]) => {
    if (!open || !sku) return;
    draft.value = cloneSku(sku);
    resetQualityDraft(sku.qualitySelection);
    validationError.value = "";
  },
  { immediate: true }
);
</script>

<template>
  <Teleport to="body">
    <Transition name="shop-catalog-editor">
      <div v-if="open && draft" class="shop-catalog-editor-backdrop" role="presentation" @click.self="close">
        <section
          :class="['shop-catalog-editor', { 'is-readonly': readonly }]"
          role="dialog"
          aria-modal="true"
          :aria-label="readonly ? '查看商品' : '编辑商品'"
          tabindex="-1"
          @keydown.esc="close"
          @click.stop
        >
          <header class="shop-catalog-editor__header">
            <div class="shop-catalog-editor__identity">
              <span class="shop-catalog-editor__image" aria-hidden="true">
                <img v-if="draft.thumbnail || draft.imageUrl" :src="draft.thumbnail || draft.imageUrl" alt="" />
                <span v-else>IMG</span>
              </span>
              <span>
                <strong>{{ readonly ? "商品详情" : "编辑商品" }}</strong>
                <small>{{ draft.supplierSkuCode || draft.platformCode || "未设置编码" }}</small>
              </span>
            </div>
            <IconButton icon="X" :label="readonly ? '关闭商品详情' : '关闭编辑弹窗'" :disabled="saving" @click="close" />
          </header>

          <div class="shop-catalog-editor__body">
            <div class="shop-catalog-editor__grid">
              <label>
                <span>商品名称</span>
                <input v-model="draft.productName" :readonly="readonly" aria-label="商品名称" />
              </label>
              <label>
                <span>企业 SKU</span>
                <input v-model="draft.supplierSkuCode" :readonly="readonly" aria-label="企业 SKU" />
              </label>
              <label>
                <span>商品类型</span>
                <select v-model="draft.productType" :disabled="readonly" aria-label="商品类型">
                  <option value="MATERIAL">物料</option>
                  <option value="FOOD">伙食</option>
                </select>
              </label>
              <label>
                <span>商品分类</span>
                <select :value="draft.categoryCode || draft.category" :disabled="readonly" aria-label="商品分类" @change="updateCategory(($event.target as HTMLSelectElement).value)">
                  <option value="">未填写</option>
                  <option v-for="option in categoryOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <label>
                <span>平台 / IMPA 编码</span>
                <input v-model="draft.platformCode" :readonly="readonly" aria-label="平台 / IMPA 编码" />
              </label>
              <label>
                <span>条码</span>
                <input v-model="draft.barcode" :readonly="readonly" aria-label="条码" />
              </label>
              <label>
                <span>库存</span>
                <input v-model.number="draft.stock" :readonly="readonly" type="number" min="0" aria-label="库存" />
              </label>
              <label>
                <span>单位</span>
                <input v-model="draft.unit" :readonly="readonly" aria-label="单位" />
              </label>
              <label>
                <span>包装方式</span>
                <input v-model="draft.packing" :readonly="readonly" aria-label="包装方式" />
              </label>
              <label>
                <span>品牌</span>
                <input v-model="draft.brand" :readonly="readonly" aria-label="品牌" />
              </label>
              <label>
                <span>备货时长（天）</span>
                <input v-model.number="draft.leadTimeDays" :readonly="readonly" type="number" min="0" aria-label="备货时长（天）" />
              </label>
              <label>
                <span>配送区域</span>
                <input v-model="draft.deliveryArea" :readonly="readonly" aria-label="配送区域" />
              </label>
              <label>
                <span>币种</span>
                <select v-model="draft.currency" :disabled="readonly" aria-label="币种">
                  <option value="CNY">人民币 CNY</option>
                  <option value="RMB">人民币 RMB</option>
                  <option value="USD">美元 USD</option>
                </select>
              </label>
              <label>
                <span>单价</span>
                <input v-model.number="draft.price" :readonly="readonly" type="number" min="0" step="0.01" aria-label="单价" />
              </label>
            </div>

            <section class="shop-catalog-editor__description">
              <header>
                <strong>产品介绍</strong>
                <span>{{ (draft.productDescription || "").length }}/4000</span>
              </header>
              <textarea
                v-model="draft.productDescription"
                :readonly="readonly"
                maxlength="4000"
                rows="3"
                aria-label="产品介绍"
                placeholder="介绍商品用途、性能特点与适用场景"
              ></textarea>
            </section>

            <section class="shop-catalog-editor__tags">
              <header>
                <div>
                  <strong>商品标签</strong>
                  <span>最多 6 个，每个 12 字</span>
                </div>
                <IconButton v-if="!readonly" icon="Plus" label="添加商品标签" :disabled="(draft.productTags?.length || 0) >= 6" @click="addTag" />
              </header>
              <div v-if="draft.productTags?.length" class="shop-catalog-editor__tag-list">
                <div v-for="(_, index) in draft.productTags" :key="index" class="shop-catalog-editor__tag-row">
                  <input v-model="draft.productTags[index]" :readonly="readonly" maxlength="12" :aria-label="`商品标签${index + 1}`" placeholder="如：热卖、上新、质量好" />
                  <IconButton v-if="!readonly" icon="Trash2" :label="`删除第${index + 1}个商品标签`" variant="danger" @click="removeTag(index)" />
                </div>
              </div>
              <span v-else class="shop-catalog-editor__tag-empty">暂无商品标签，可点击加号添加。</span>
            </section>

            <section class="shop-catalog-editor__specs">
              <header>
                <div>
                  <strong>规格明细</strong>
                  <span>{{ draft.specItems?.length || 0 }} 项</span>
                </div>
                <IconButton v-if="!readonly" icon="Plus" label="添加规格" @click="addSpec" />
              </header>
              <div v-if="draft.specItems?.length" class="shop-catalog-editor__spec-list">
                <div v-for="(spec, index) in draft.specItems" :key="spec.id" class="shop-catalog-editor__spec-row">
                  <input v-model="spec.name" :readonly="readonly" :aria-label="`规格名称${index + 1}`" placeholder="规格名称" />
                  <input v-model="spec.value" :readonly="readonly" :aria-label="`规格值${index + 1}`" placeholder="规格值" />
                  <IconButton v-if="!readonly" icon="Trash2" :label="`删除第${index + 1}项规格`" variant="danger" @click="removeSpec(index)" />
                </div>
              </div>
              <span v-else class="shop-catalog-editor__spec-empty">暂无规格明细，可点击加号补充。</span>
            </section>

            <section class="shop-catalog-editor__quality-selection" aria-labelledby="quality-selection-title">
              <header>
                <div>
                  <strong id="quality-selection-title">平台严选</strong>
                  <span>海事严选 QC</span>
                </div>
                <span :class="['shop-catalog-editor__quality-status', { 'is-selected': draft.qualitySelection }]">
                  {{ draft.qualitySelection ? "已严选" : "待检查" }}
                </span>
              </header>
              <div class="shop-catalog-editor__quality-grid">
                <label>
                  <span>QC 检查时间</span>
                  <input v-model="qualityDraft.inspectionTime" type="datetime-local" :readonly="!qualityEditable" aria-label="QC检查时间" />
                </label>
                <label class="is-wide">
                  <span>检查内容</span>
                  <textarea v-model="qualityDraft.inspectionContent" :readonly="!qualityEditable" maxlength="4000" rows="3" aria-label="QC检查内容" placeholder="记录检查项目与范围"></textarea>
                </label>
                <label class="is-wide">
                  <span>检查过程留痕</span>
                  <textarea v-model="qualityDraft.inspectionProcess" :readonly="!qualityEditable" maxlength="8000" rows="3" aria-label="QC检查过程留痕" placeholder="记录检查步骤、依据与过程"></textarea>
                </label>
                <label class="is-wide">
                  <span>检测结论</span>
                  <textarea v-model="qualityDraft.inspectionConclusion" :readonly="!qualityEditable" maxlength="1000" rows="2" aria-label="QC检测结论" placeholder="填写最终检测结论"></textarea>
                </label>
              </div>
              <div class="shop-catalog-editor__quality-report">
                <span>
                  <small>检测报告</small>
                  <strong>{{ qualityDraft.reportFileName || "暂无检测报告" }}</strong>
                </span>
                <div>
                  <input ref="qualityReportInput" type="file" hidden @change="handleQualityReport" />
                  <IconButton v-if="qualityEditable" icon="Upload" label="上传检测报告" :loading="qualityUploading" @click="qualityReportInput?.click()" />
                  <IconButton v-if="draft.qualitySelection?.reportUrl" icon="Download" label="下载检测报告" @click="downloadQuality" />
                  <IconButton v-if="qualityEditable" icon="Save" label="保存海事严选" variant="primary" :loading="qualitySaving" @click="saveQuality" />
                </div>
              </div>
              <div v-if="draft.qualitySelection?.auditTrail?.length" class="shop-catalog-editor__quality-audit">
                <strong>检查过程记录</strong>
                <ol>
                  <li v-for="(entry, index) in draft.qualitySelection.auditTrail" :key="`${entry.occurredAt}-${index}`">
                    <time>{{ entry.occurredAt || "--" }}</time>
                    <span>{{ entry.action === "CREATED" ? "首次严选" : "更新严选" }}</span>
                    <p>{{ entry.inspectionConclusion }}</p>
                  </li>
                </ol>
              </div>
              <span v-if="qualityError" class="shop-catalog-editor__quality-error" role="alert">{{ qualityError }}</span>
            </section>
          </div>

          <footer class="shop-catalog-editor__footer">
            <span v-if="validationError || error" class="shop-catalog-editor__error" role="alert">{{ validationError || error }}</span>
            <div>
              <IconButton icon="X" :label="readonly ? '关闭商品详情' : '取消修改'" :disabled="saving" @click="close" />
              <IconButton v-if="!readonly" icon="Save" label="保存修改" variant="primary" :loading="saving" @click="submit" />
            </div>
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
