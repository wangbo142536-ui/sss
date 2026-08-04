<script setup lang="ts">
import { ref, watch } from "vue";
import DataTable from "@/components/DataTable.vue";
import IconButton from "@/components/IconButton.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { declareCustoms, downloadCustomsAttachment, getCustomsContext } from "../services/customsDeclarationService";
import type { CustomsBusinessType, CustomsDeclarationContext } from "../types";
import "../styles/customs-declaration.css";

const props = defineProps<{ businessType: CustomsBusinessType; purchaseOrderId: number; initialContext?: CustomsDeclarationContext | null }>();
const emit = defineEmits<{ updated: [context: CustomsDeclarationContext] }>();
const context = ref<CustomsDeclarationContext | null>(props.initialContext || null);
const loading = ref(false);
const error = ref("");

const columns = [
  { key: "code", label: "商品编码", width: "128px" },
  { key: "productName", label: "品名", width: "240px" },
  { key: "specification", label: "规格", width: "180px" },
  { key: "quantity", label: "数量", width: "90px", align: "right" as const },
  { key: "unit", label: "单位", width: "80px" }
];

async function reload() {
  context.value = await getCustomsContext(props.businessType, props.purchaseOrderId);
  emit("updated", context.value);
}

async function declareOrder() {
  loading.value = true;
  error.value = "";
  try {
    await declareCustoms(props.businessType, props.purchaseOrderId);
    await reload();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "一键报关失败";
  } finally {
    loading.value = false;
  }
}

async function download() {
  if (!context.value?.declarationId) return;
  loading.value = true;
  error.value = "";
  try {
    await downloadCustomsAttachment(context.value.declarationId);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "附件下载失败";
  } finally {
    loading.value = false;
  }
}

watch(() => [props.businessType, props.purchaseOrderId], () => {
  context.value = props.initialContext || null;
  if (!context.value) void reload();
}, { immediate: true });
</script>

<template>
  <article class="purchase-order-section customs-order-detail-panel">
    <header class="customs-order-detail-panel__header">
      <div>
        <h3>报关明细</h3>
        <p>责任方：{{ context?.responsibleCompanyName || '-' }}　报关费：¥ {{ Number(context?.customsFee || 0).toFixed(2) }}</p>
      </div>
      <div class="customs-order-detail-panel__actions">
        <StatusBadge v-if="context?.status === 'DECLARED'" label="已报关" variant="success" />
        <span v-else class="customs-order-detail-panel__empty-status">--</span>
        <IconButton v-if="context?.status !== 'DECLARED'" icon="Send" label="一键报关" variant="primary" :disabled="!context?.canDeclare" :loading="loading" @click="declareOrder" />
        <IconButton v-else icon="Download" label="下载附件一" :loading="loading" @click="download" />
      </div>
    </header>
    <p v-if="error" class="inline-error">{{ error }}</p>
    <DataTable :columns="columns" :rows="context?.items || []" row-key="id" empty-label="暂无报关商品明细" />
  </article>
</template>
