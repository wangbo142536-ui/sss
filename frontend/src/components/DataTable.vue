<script setup lang="ts" generic="T extends Record<string, unknown>">
import { t } from "@/i18n";
import type { TableColumn } from "@/types/workbench";

const props = withDefaults(
  defineProps<{
    columns: TableColumn[];
    rows: T[];
    loading?: boolean;
    rowKey?: string;
    expandedRowKey?: string;
    rowInteractive?: boolean;
    rowClass?: (row: T) => string | string[] | Record<string, boolean>;
    showIndex?: boolean;
    emptyLabel?: string;
  }>(),
  {
    loading: false,
    rowKey: "",
    expandedRowKey: "",
    rowInteractive: false,
    rowClass: undefined,
    showIndex: true,
    emptyLabel: ""
  }
);

defineEmits<{
  rowClick: [row: T];
  rowMouseenter: [row: T, event: MouseEvent];
  rowMouseleave: [row: T, event: MouseEvent];
}>();

const rowIdentifier = (row: T) => {
  if (props.rowKey) {
    return String(row[props.rowKey] ?? "");
  }
  return String(row.id || row.requestNo || row.inquiryNo || row.impaCode);
};
</script>

<template>
  <div class="data-table-wrap">
    <table class="data-table">
      <colgroup>
        <col v-if="props.showIndex" class="data-table-index-col" />
        <col v-for="column in props.columns" :key="column.key" :style="{ width: column.width }" />
      </colgroup>
      <thead>
        <tr>
          <th v-if="props.showIndex" class="data-table-index-cell">
            {{ t("table.index") }}
          </th>
          <th
            v-for="column in props.columns"
            :key="column.key"
            :style="{ width: column.width, textAlign: column.align || 'left' }"
          >
            <slot :name="`head-${column.key}`" :column="column">
              {{ column.label }}
            </slot>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="props.loading" class="data-table-state-row">
          <td :colspan="props.columns.length + (props.showIndex ? 1 : 0)">{{ t("common.loading") }}</td>
        </tr>
        <tr v-else-if="props.rows.length === 0" class="data-table-state-row">
          <td :colspan="props.columns.length + (props.showIndex ? 1 : 0)">{{ props.emptyLabel || t("common.empty") }}</td>
        </tr>
        <template v-for="(row, rowIndex) in props.rows" v-else :key="rowIdentifier(row)">
          <tr
            :class="[
              'data-table-row',
              rowIndex % 2 === 0 ? 'is-odd' : 'is-even',
              {
                'is-expanded': props.expandedRowKey === rowIdentifier(row),
                'is-interactive': props.rowInteractive
              },
              props.rowClass?.(row)
            ]"
            :tabindex="props.rowInteractive ? 0 : undefined"
            @click="$emit('rowClick', row)"
            @mouseenter="$emit('rowMouseenter', row, $event)"
            @mouseleave="$emit('rowMouseleave', row, $event)"
            @keydown.enter="$emit('rowClick', row)"
            @keydown.space.prevent="$emit('rowClick', row)"
          >
            <td v-if="props.showIndex" class="data-table-index-cell">
              {{ rowIndex + 1 }}
            </td>
            <td
              v-for="column in props.columns"
              :key="column.key"
              :style="{ textAlign: column.align || 'left' }"
            >
              <slot :name="`cell-${column.key}`" :row="row" :value="row[column.key]">
                {{ row[column.key] }}
              </slot>
            </td>
          </tr>
          <tr
            v-if="$slots['expanded-row'] && props.expandedRowKey === rowIdentifier(row)"
            class="data-table-expanded-row is-open"
          >
            <td :colspan="props.columns.length + (props.showIndex ? 1 : 0)">
              <div class="data-table-expand-content">
                <slot name="expanded-row" :row="row" :expanded="true" />
              </div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </div>
</template>
