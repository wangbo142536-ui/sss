<script setup lang="ts">
import { computed, ref, watch } from "vue";
import DetailDrawer from "@/components/DetailDrawer.vue";
import IconButton from "@/components/IconButton.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { getAuthSession, uploadQualificationFile } from "@/services/authService";
import type { BusinessAttachmentPayload } from "@/services/fulfillmentService";
import type { FoodSettlement } from "../types";

type SettlementWithInvoice = FoodSettlement & {
  actualAmount?: number;
  invoiceAttachments?: BusinessAttachmentPayload[];
};

const props = defineProps<{
  open: boolean;
  settlement?: SettlementWithInvoice;
  editable?: boolean;
  saving?: boolean;
}>();

const emit = defineEmits<{
  close: [];
  submit: [payload: { actualAmount: number; invoiceNo: string; invoiceAttachments: BusinessAttachmentPayload[] }];
}>();

const actualAmount = ref("");
const invoiceNo = ref("");
const attachments = ref<BusinessAttachmentPayload[]>([]);
const uploading = ref(false);
const previewing = ref("");
const error = ref("");

const title = computed(() => props.editable ? "编辑结算单" : "查看结算单");
const canSubmit = computed(() => Number(actualAmount.value) > 0 && invoiceNo.value.trim().length > 0 && attachments.value.length > 0);

function statusLabel(status = "") {
  return ({ PENDING_INVOICE: "待供发票", INVOICED: "待结算", SETTLED: "待付款", PAID: "已付款" } as Record<string, string>)[status] || status;
}

function statusVariant(status = ""): "neutral" | "info" | "warning" | "success" {
  if (status === "PAID") return "success";
  if (["INVOICED", "SETTLED"].includes(status)) return "info";
  if (status === "PENDING_INVOICE") return "warning";
  return "neutral";
}

watch(() => [props.open, props.settlement] as const, () => {
  if (!props.open || !props.settlement) return;
  actualAmount.value = props.settlement.actualAmount == null ? "" : String(props.settlement.actualAmount);
  invoiceNo.value = props.settlement.invoiceNo || "";
  attachments.value = [...(props.settlement.invoiceAttachments || [])];
  error.value = "";
}, { immediate: true });

async function uploadFiles(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files || []);
  if (!files.length) return;
  uploading.value = true;
  error.value = "";
  try {
    for (const file of files) {
      const uploaded = await uploadQualificationFile(file);
      const fileId = String(uploaded.fileId || uploaded.id || "");
      attachments.value.push({
        fileId,
        fileName: uploaded.name || file.name,
        fileUrl: uploaded.url || (fileId ? `/api/files/${encodeURIComponent(fileId)}` : "")
      });
    }
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "发票附件上传失败";
  } finally {
    uploading.value = false;
    input.value = "";
  }
}

function removeAttachment(index: number) {
  attachments.value.splice(index, 1);
}

async function previewAttachment(file: BusinessAttachmentPayload) {
  const source = file.fileUrl || (file.fileId ? `/api/files/${encodeURIComponent(file.fileId)}` : "");
  if (!source) return;
  previewing.value = file.fileId || file.fileName;
  error.value = "";
  try {
    const session = getAuthSession();
    const response = await fetch(source, {
      headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
    });
    if (!response.ok) throw new Error("发票附件读取失败");
    const objectUrl = URL.createObjectURL(await response.blob());
    window.open(objectUrl, "_blank", "noopener,noreferrer");
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : "发票附件读取失败";
  } finally {
    previewing.value = "";
  }
}

function submit() {
  if (!canSubmit.value) {
    error.value = "请填写实际金额、发票号码并上传发票附件。";
    return;
  }
  emit("submit", {
    actualAmount: Number(actualAmount.value),
    invoiceNo: invoiceNo.value.trim(),
    invoiceAttachments: [...attachments.value]
  });
}
</script>

<template>
  <DetailDrawer :open="open" :title="title" width="wide" @close="$emit('close')">
    <section v-if="settlement" class="settlement-editor food-settlement-editor">
      <header>
        <div><span>结算单号</span><strong>FS-{{ settlement.settlementId }}</strong></div>
        <StatusBadge :label="statusLabel(settlement.status)" :variant="statusVariant(settlement.status)" />
      </header>
      <dl>
        <div><dt>采购单号</dt><dd>{{ settlement.orderNo || '-' }}</dd></div>
        <div><dt>船代公司</dt><dd>{{ settlement.buyerCompanyName || '-' }}</dd></div>
        <div><dt>船舶名称</dt><dd>{{ settlement.vesselName || '-' }}</dd></div>
        <div><dt>供货商</dt><dd>{{ settlement.supplierName || '-' }}</dd></div>
        <div><dt>报价金额</dt><dd>{{ Number(settlement.amount || 0).toFixed(2) }} {{ settlement.currency || '' }}</dd></div>
        <div><dt>实际金额</dt><dd>{{ settlement.actualAmount == null ? '--' : `${Number(settlement.actualAmount).toFixed(2)} ${settlement.currency || ''}` }}</dd></div>
      </dl>
      <label class="settlement-editor__amount">
        <span>实际金额</span>
        <input v-model="actualAmount" type="number" min="0" step="0.01" placeholder="填写实际发生金额" :readonly="!editable" />
      </label>
      <label class="settlement-editor__amount">
        <span>发票号码</span>
        <input v-model="invoiceNo" type="text" placeholder="填写发票号码" :readonly="!editable" />
      </label>
      <section class="settlement-editor__invoice">
        <header><strong>发票附件</strong><span>{{ attachments.length }} 个文件</span></header>
        <div class="supplier-customs-file-list food-settlement-file-list">
          <div v-for="(file, index) in attachments" :key="file.fileId || file.fileName">
            <button type="button" :disabled="previewing === (file.fileId || file.fileName)" @click="previewAttachment(file)">{{ file.fileName }}</button>
            <IconButton v-if="editable" icon="Trash2" label="移除附件" variant="danger" @click="removeAttachment(index)" />
          </div>
          <span v-if="!attachments.length" class="food-settlement-empty-file">暂无发票附件</span>
        </div>
        <label v-if="editable" class="supplier-customs-upload">
          <span>{{ uploading ? '上传中' : '上传发票图片或文件' }}</span>
          <input type="file" multiple accept="image/*,.pdf,.doc,.docx,.xls,.xlsx" :disabled="uploading" @change="uploadFiles" />
        </label>
      </section>
      <div v-if="error" class="inline-error">{{ error }}</div>
    </section>
    <template #footer>
      <IconButton icon="X" label="关闭" @click="$emit('close')" />
      <IconButton v-if="editable" icon="Send" label="提交发票" variant="primary" :disabled="!canSubmit" :loading="saving" @click="submit" />
    </template>
  </DetailDrawer>
</template>

<style scoped>
.food-settlement-editor {
  display: grid;
  gap: 14px;
}

.food-settlement-file-list {
  display: grid;
  gap: 8px;
}

.food-settlement-file-list > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid rgba(102, 174, 232, 0.38);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.62);
}

.food-settlement-file-list > div > button:first-child {
  min-width: 0;
  overflow: hidden;
  color: #075f9f;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.food-settlement-empty-file {
  padding: 18px;
  color: #6f8396;
  text-align: center;
}
</style>
