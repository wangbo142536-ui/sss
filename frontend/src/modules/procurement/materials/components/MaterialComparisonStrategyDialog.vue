<script setup lang="ts">
import { computed, reactive, watch } from "vue";
import IconButton from "@/components/IconButton.vue";
import type {
  MaterialComparisonItem,
  MaterialComparisonStrategySettings,
  MaterialComparisonStrategySettingsPayload
} from "@/types/procurementMaterials";
import "../styles/material-comparison-strategy.css";

const props = defineProps<{
  open: boolean;
  settings?: MaterialComparisonStrategySettings;
  items: MaterialComparisonItem[];
  saving?: boolean;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  close: [];
  apply: [payload: MaterialComparisonStrategySettingsPayload];
}>();

const form = reactive<MaterialComparisonStrategySettingsPayload>({
  mixedSupplierCount: 3,
  priceEnabled: true,
  priceLevel: 5,
  qualityEnabled: true,
  qualityLevel: 3,
  coreDemandItemIds: []
});
const error = computed(() => !form.priceEnabled && !form.qualityEnabled ? "价格低、质量高至少保留一项" : "");
const coreItems = computed(() => props.items.filter((item) => Number(item.demandItemId) > 0));

function resetForm() {
  const source = props.settings ?? {
    mixedSupplierCount: 3,
    priceEnabled: true,
    priceLevel: 5,
    qualityEnabled: true,
    qualityLevel: 3,
    coreDemandItemIds: [],
    strategyVersion: 1
  };
  form.mixedSupplierCount = source.mixedSupplierCount;
  form.priceEnabled = source.priceEnabled;
  form.priceLevel = source.priceLevel;
  form.qualityEnabled = source.qualityEnabled;
  form.qualityLevel = source.qualityLevel;
  form.coreDemandItemIds = [...source.coreDemandItemIds];
}

function setSupplierCount(value: number) {
  form.mixedSupplierCount = Math.max(2, Math.min(10, Math.round(value || 3)));
}

function toggleCoreItem(itemId?: number) {
  if (!itemId) return;
  form.coreDemandItemIds = form.coreDemandItemIds.includes(itemId)
    ? form.coreDemandItemIds.filter((id) => id !== itemId)
    : [...form.coreDemandItemIds, itemId];
}

function applyStrategy() {
  if (error.value || props.saving || props.readonly) return;
  emit("apply", {
    ...form,
    coreDemandItemIds: [...form.coreDemandItemIds]
  });
}

watch(() => [props.open, props.settings] as const, ([open]) => {
  if (open) resetForm();
}, { immediate: true, deep: true });
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="material-strategy-backdrop" role="presentation" @mousedown.self="emit('close')">
      <section class="material-strategy-dialog" role="dialog" aria-modal="true" aria-labelledby="material-strategy-title">
        <header class="material-strategy-dialog__header">
          <div>
            <h2 id="material-strategy-title">比价策略引擎</h2>
            <p>设置最低混供数量、产品侧重和必须采购的核心商品</p>
          </div>
          <IconButton icon="X" label="关闭策略设置" :disabled="saving" @click="emit('close')" />
        </header>

        <div class="material-strategy-dialog__body">
          <section class="material-strategy-section">
            <div class="material-strategy-section__heading">
              <strong>最低混供供货商数量</strong>
              <span>系统尽量使用指定数量，并确保每家至少承担一个商品</span>
            </div>
            <div class="material-strategy-supplier-count">
              <button type="button" :class="{ active: form.mixedSupplierCount === 2 }" @click="setSupplierCount(2)">2 家</button>
              <button type="button" :class="{ active: form.mixedSupplierCount === 3 }" @click="setSupplierCount(3)">3 家</button>
              <label>
                <span>自定义</span>
                <input :value="form.mixedSupplierCount" type="number" min="2" max="10" @input="setSupplierCount(Number(($event.target as HTMLInputElement).value))" />
                <em>家</em>
              </label>
            </div>
          </section>

          <section class="material-strategy-section">
            <div class="material-strategy-section__heading">
              <strong>产品策略侧重</strong>
              <span>质量依据商家商品标签，价格依据真实报价并参考商家“价格低”标签</span>
            </div>
            <div class="material-strategy-objectives">
              <label :class="['material-strategy-objective', { disabled: !form.priceEnabled }]">
                <span class="material-strategy-objective__title">
                  <input v-model="form.priceEnabled" type="checkbox" />
                  <strong>价格低</strong>
                </span>
                <input v-model.number="form.priceLevel" type="range" min="1" max="5" :disabled="!form.priceEnabled" />
                <span class="material-strategy-level">{{ form.priceLevel }} 档</span>
              </label>
              <label :class="['material-strategy-objective', { disabled: !form.qualityEnabled }]">
                <span class="material-strategy-objective__title">
                  <input v-model="form.qualityEnabled" type="checkbox" />
                  <strong>质量高</strong>
                </span>
                <input v-model.number="form.qualityLevel" type="range" min="1" max="5" :disabled="!form.qualityEnabled" />
                <span class="material-strategy-level">{{ form.qualityLevel }} 档</span>
              </label>
            </div>
            <p v-if="error" class="material-strategy-error" role="alert">{{ error }}</p>
          </section>

          <section class="material-strategy-section material-strategy-section--core">
            <div class="material-strategy-section__heading">
              <strong>核心商品</strong>
              <span>已选择 {{ form.coreDemandItemIds.length }} 项，生成结果必须覆盖这些商品</span>
            </div>
            <div class="material-strategy-core-list">
              <label v-for="item in coreItems" :key="item.demandItemId" :class="{ selected: form.coreDemandItemIds.includes(Number(item.demandItemId)) }">
                <input
                  type="checkbox"
                  :checked="form.coreDemandItemIds.includes(Number(item.demandItemId))"
                  @change="toggleCoreItem(item.demandItemId)"
                />
                <span>
                  <strong>{{ item.productName || item.description || item.sourceSkuName || '未命名商品' }}</strong>
                  <small>{{ item.impaCode || item.platformCode || '暂无编码' }} · {{ item.specification || item.unit || '暂无规格' }}</small>
                </span>
              </label>
            </div>
          </section>
        </div>

        <footer class="material-strategy-dialog__footer">
          <button type="button" class="material-strategy-secondary" :disabled="saving" @click="emit('close')">取消</button>
          <button type="button" class="material-strategy-primary" :disabled="Boolean(error) || saving || readonly" @click="applyStrategy">
            {{ saving ? '正在重新生成' : '保存并重新生成' }}
          </button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
