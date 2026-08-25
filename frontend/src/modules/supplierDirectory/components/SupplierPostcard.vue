<script setup lang="ts">
import { computed } from "vue";
import { maskSupplierContactName, maskSupplierEmail, maskSupplierName, maskSupplierPhone } from "../services/supplierDirectoryPrivacy";
import type { SupplierDirectoryItem } from "../types/supplierDirectory";

const props = defineProps<{
  supplier: SupplierDirectoryItem;
  logoUrl?: string;
  canManageStatus?: boolean;
  labels: {
    enabled: string;
    disabled: string;
    products: string;
    categories: string;
    rating: string;
    noRating: string;
    enable: string;
    disable: string;
  };
}>();

const maskedName = computed(() => maskSupplierName(props.supplier.name));
const maskedContactName = computed(() => maskSupplierContactName(props.supplier.contactName));
const maskedPhone = computed(() => maskSupplierPhone(props.supplier.contactPhone));
const maskedEmail = computed(() => maskSupplierEmail(props.supplier.contactEmail));
const maskedContactLine = computed(() => `${maskedContactName.value} / ${maskedPhone.value} / ${maskedEmail.value}`);

defineEmits<{
  view: [supplier: SupplierDirectoryItem];
  toggleStatus: [supplier: SupplierDirectoryItem];
}>();

function roundedRating(supplier: SupplierDirectoryItem): number {
  return Math.max(0, Math.min(5, Math.round(supplier.averageRating ?? 0)));
}
</script>

<template>
  <article
    :class="['supplier-postcard', { 'is-disabled': supplier.status !== 'ACTIVE' }]"
    tabindex="0"
    :aria-label="`${maskedName}，${supplier.status === 'ACTIVE' ? labels.enabled : labels.disabled}`"
    @click="$emit('view', supplier)"
    @keydown.enter.prevent="$emit('view', supplier)"
  >
    <div class="supplier-postcard__actions">
      <button
        v-if="canManageStatus"
        type="button"
        :class="['supplier-postcard__status-dot', supplier.status === 'ACTIVE' ? 'is-active' : 'is-disabled']"
        :aria-label="`当前状态：${supplier.status === 'ACTIVE' ? labels.enabled : labels.disabled}；${supplier.status === 'ACTIVE' ? labels.disable : labels.enable}`"
        :title="`当前状态：${supplier.status === 'ACTIVE' ? labels.enabled : labels.disabled}；${supplier.status === 'ACTIVE' ? labels.disable : labels.enable}`"
        @click.stop="$emit('toggleStatus', supplier)"
      ></button>
      <span
        v-else
        role="status"
        :class="['supplier-postcard__status-dot', supplier.status === 'ACTIVE' ? 'is-active' : 'is-disabled']"
        :aria-label="`当前状态：${supplier.status === 'ACTIVE' ? labels.enabled : labels.disabled}`"
        :title="`当前状态：${supplier.status === 'ACTIVE' ? labels.enabled : labels.disabled}`"
      >
      </span>
    </div>

    <header class="supplier-postcard__identity">
      <div class="supplier-postcard__visual">
        <div class="supplier-postcard__logo">
          <img v-if="logoUrl" :src="logoUrl" :alt="`${maskedName}标志`" />
          <span v-else aria-hidden="true">{{ supplier.name.slice(0, 1) }}</span>
        </div>
      </div>
      <div class="supplier-postcard__company">
        <div>
          <h2>{{ maskedName }}</h2>
        </div>
        <p class="supplier-postcard__credit-code" aria-label="社会信用代码已隐藏">**********</p>
      </div>
    </header>

    <p
      class="supplier-postcard__contact-line"
      :title="maskedContactLine"
      :aria-label="maskedContactLine"
    ><span class="supplier-postcard__contact-value">{{ maskedContactName }}</span><span class="supplier-postcard__contact-separator" aria-hidden="true"> / </span><span class="supplier-postcard__contact-value">{{ maskedPhone }}</span><span class="supplier-postcard__contact-separator" aria-hidden="true"> / </span><span class="supplier-postcard__contact-value">{{ maskedEmail }}</span></p>

    <section class="supplier-postcard__metrics" aria-label="供货能力摘要">
      <div class="supplier-postcard__metric is-products"><span>{{ labels.products }}</span><strong>{{ supplier.skuCount.toLocaleString() }}</strong></div>
      <div class="supplier-postcard__metric is-categories"><span>{{ labels.categories }}</span><strong>{{ supplier.categoryCount }}</strong></div>
      <div class="supplier-postcard__metric is-rating">
        <span>{{ labels.rating }}</span>
        <div
          class="supplier-postcard__metric-stars"
          :aria-label="supplier.averageRating == null || supplier.evaluationCount <= 0 ? labels.noRating : `${supplier.averageRating}分`"
        >
          <i v-for="point in 5" :key="point" :class="{ active: point <= roundedRating(supplier) }" aria-hidden="true">★</i>
        </div>
      </div>
    </section>

    <div class="supplier-postcard__scope">
      <strong>服务范围</strong>
      <div class="supplier-postcard__ports">
        <span v-for="port in supplier.servicePorts.slice(0, 4)" :key="port">{{ port }}</span>
        <em v-if="supplier.servicePorts.length > 4">+{{ supplier.servicePorts.length - 4 }}</em>
        <i v-if="!supplier.servicePorts.length">--</i>
      </div>
    </div>
  </article>
</template>
