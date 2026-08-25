<script setup lang="ts">
import type { CompanyMenuOption } from "@/services/companyMemberService";
import {
  getMenuPermissionState,
  reconcileMenuPermissionKeys,
  toggleMenuPermissionKeys
} from "@/components/companyRolePermissionSelection";

defineOptions({ name: "CompanyRolePermissionTree" });

const props = withDefaults(defineProps<{
  nodes: CompanyMenuOption[];
  modelValue: string[];
  depth?: number;
}>(), {
  depth: 0
});

const emit = defineEmits<{
  (event: "update:modelValue", value: string[]): void;
}>();

const onCheckboxChange = (node: CompanyMenuOption, event: Event) => {
  emit(
    "update:modelValue",
    toggleMenuPermissionKeys(props.nodes, props.modelValue, node, (event.target as HTMLInputElement).checked)
  );
};

const onChildSelection = (values: string[]) => {
  emit("update:modelValue", reconcileMenuPermissionKeys(props.nodes, values));
};

const permissionState = (node: CompanyMenuOption) => getMenuPermissionState(node, props.modelValue);
</script>

<template>
  <div :class="['company-role-permission-tree', `is-depth-${depth}`]">
    <section
      v-for="node in nodes"
      :key="node.code"
      :class="['company-role-permission-node', { 'has-children': node.children.length }]"
    >
      <label class="member-check-row company-role-permission-row">
        <input
          type="checkbox"
          :value="node.code"
          :checked="permissionState(node).checked"
          :indeterminate="permissionState(node).indeterminate"
          :aria-checked="permissionState(node).indeterminate ? 'mixed' : permissionState(node).checked"
          @change="onCheckboxChange(node, $event)"
        />
        <span>{{ node.name }}</span>
        <em>{{ node.code }}</em>
      </label>
      <CompanyRolePermissionTree
        v-if="node.children.length"
        :nodes="node.children"
        :model-value="modelValue"
        :depth="depth + 1"
        @update:model-value="onChildSelection"
      />
    </section>
  </div>
</template>

<style scoped>
.company-role-permission-tree {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.company-role-permission-node {
  min-width: 0;
}

.company-role-permission-tree.is-depth-0 {
  gap: 8px;
}

.company-role-permission-tree.is-depth-0 > .company-role-permission-node {
  border: 1px solid rgba(172, 207, 234, 0.78);
  border-radius: 8px;
  padding: 8px;
  background: #f8fbff;
}

.company-role-permission-tree.is-depth-0 > .company-role-permission-node > .company-role-permission-row {
  color: var(--color-primary-800);
  background: #eaf5ff;
  font-weight: 800;
}

.company-role-permission-tree:not(.is-depth-0) {
  margin-top: 6px;
  margin-left: 16px;
}

.company-role-permission-tree.is-depth-1 > .company-role-permission-node > .company-role-permission-row {
  border-color: rgba(185, 216, 242, 0.72);
  background: #ffffff;
}

.company-role-permission-tree.is-depth-2,
.company-role-permission-tree.is-depth-3 {
  margin-left: 14px;
}

.company-role-permission-row:hover {
  border-color: rgba(24, 105, 202, 0.32);
  background: #f1f8ff;
}

.company-role-permission-row:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(24, 105, 202, 0.12);
}

.company-role-permission-row input {
  accent-color: var(--color-primary);
}

</style>
