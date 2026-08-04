<script setup lang="ts">
import { onMounted, ref } from "vue";
import ExpandablePanel from "@/components/ExpandablePanel.vue";
import IconButton from "@/components/IconButton.vue";
import StableDateTimeInput from "@/components/StableDateTimeInput.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import { downloadCustomsAttachment, listCustomsDeclarations } from "../services/customsDeclarationService";
import type { CustomsDeclarationRecord } from "../types";
import "../styles/customs-declaration.css";

const rows = ref<CustomsDeclarationRecord[]>([]);
const keyword = ref("");
const status = ref("");
const deliveryDate = ref("");
const loading = ref(false);
const error = ref("");
const downloadingId = ref<number | null>(null);

const text = (value?: string) => value?.trim() || "-";
const money = (value?: number) => `¥ ${Number(value || 0).toFixed(2)}`;
const dateTime = (value?: string) => value ? value.replace("T", " ").slice(0, 19) : "-";

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const page = await listCustomsDeclarations({
      keyword: keyword.value.trim(),
      status: status.value,
      deliveryDate: deliveryDate.value,
      page: 1,
      size: 100
    });
    rows.value = page.items || [];
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "报关记录加载失败";
  } finally {
    loading.value = false;
  }
}

function reset() {
  keyword.value = "";
  status.value = "";
  deliveryDate.value = "";
  void load();
}

async function download(row: CustomsDeclarationRecord) {
  downloadingId.value = row.id;
  error.value = "";
  try {
    await downloadCustomsAttachment(row.id);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "附件导出失败";
  } finally {
    downloadingId.value = null;
  }
}

onMounted(load);
</script>

<template>
  <WorkbenchLayout title="报关管理">
    <div class="customs-declaration-page">
      <ExpandablePanel :show-header="false" :show-expand="false" class="inquiry-management-panel customs-declaration-panel">
        <section class="filter-toolbar" aria-label="报关记录筛选">
          <div class="filter-fields">
            <label class="filter-field filter-field--search">
              <span>检索</span>
              <span class="filter-search-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="m21 21-4.3-4.3M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z" /></svg></span>
              <input v-model="keyword" placeholder="采购单号、船名、报关单位" @keydown.enter.prevent="load" />
            </label>
            <label class="filter-field">
              <span>报关状态</span>
              <select v-model="status"><option value="">全部</option><option value="DECLARING">报关中</option><option value="DECLARED">已报关</option></select>
            </label>
            <label class="filter-field"><span>送货日期</span><StableDateTimeInput v-model="deliveryDate" mode="date" /></label>
          </div>
          <div class="icon-action-row">
            <IconButton icon="Search" label="查询报关记录" variant="primary" :loading="loading" @click="load" />
            <IconButton icon="X" label="重置查询条件" @click="reset" />
            <IconButton icon="RefreshCw" label="刷新报关记录" :disabled="loading" @click="load" />
          </div>
        </section>

        <p v-if="error" class="customs-declaration-message is-error">{{ error }}</p>
        <p v-else-if="loading && !rows.length" class="customs-declaration-message">正在加载报关记录</p>
        <p v-else-if="!rows.length" class="customs-declaration-message">暂无报关记录，业务责任方完成一键报关后将在此展示。</p>

        <section v-else class="customs-declaration-grid" aria-label="报关记录">
          <article v-for="row in rows" :key="row.id" class="purchase-order-query-card customs-declaration-card">
            <header class="customs-declaration-card__head">
              <div class="customs-declaration-progress" aria-label="报关进度">
                <span class="is-complete"><i aria-hidden="true">✓</i><strong>报关中</strong></span>
                <b aria-hidden="true"></b>
                <span :class="{ 'is-complete': row.status === 'DECLARED' }"><i aria-hidden="true">✓</i><strong>已报关</strong></span>
              </div>
              <StatusBadge label="已报关" variant="success" />
            </header>

            <div class="customs-declaration-card__identity">
              <div><span>船名</span><strong>{{ text(row.vesselName) }}</strong></div>
              <div><span>采购单号</span><strong>{{ row.purchaseOrderNo }}</strong></div>
              <div><span>船舶代理</span><strong>{{ text(row.shipAgent) }}</strong></div>
              <div><span>物料种类</span><strong>{{ text(row.goodsCategory) }}</strong></div>
            </div>

            <dl class="customs-declaration-card__details">
              <div><dt>内贸/外贸</dt><dd>{{ text(row.tradeType) }}</dd></div>
              <div><dt>配货（报关）单位</dt><dd>{{ text(row.declarantCompany) }}</dd></div>
              <div><dt>报关联系人及电话</dt><dd>{{ [row.declarantContact, row.declarantPhone].filter(Boolean).join(' / ') || '-' }}</dd></div>
              <div><dt>送货起止时间</dt><dd>{{ text(row.deliveryStart) }} 至 {{ text(row.deliveryEnd) }}</dd></div>
              <div><dt>送货地点</dt><dd>{{ text(row.deliveryLocation) }}</dd></div>
              <div><dt>供应船舶</dt><dd>{{ text(row.supplyVessel) }}</dd></div>
              <div><dt>船长及联系方式</dt><dd>{{ text(row.captainContact) }}</dd></div>
              <div><dt>申请人及电话</dt><dd>{{ [row.applicant, row.applicantPhone].filter(Boolean).join(' / ') || '-' }}</dd></div>
              <div><dt>申请日期</dt><dd>{{ text(row.applicationDate) }}</dd></div>
              <div><dt>报关费用</dt><dd>{{ money(row.customsFee) }}</dd></div>
            </dl>

            <footer class="purchase-order-query-card__footer customs-declaration-card__footer">
              <div><span>责任方</span><strong>{{ text(row.responsibleCompanyName) }}</strong></div>
              <div><span>报关时间</span><strong>{{ dateTime(row.declaredAt) }}</strong></div>
              <div class="icon-action-row"><IconButton icon="Download" label="导出附件一" :loading="downloadingId === row.id" @click="download(row)" /></div>
            </footer>
          </article>
        </section>
      </ExpandablePanel>
    </div>
  </WorkbenchLayout>
</template>
