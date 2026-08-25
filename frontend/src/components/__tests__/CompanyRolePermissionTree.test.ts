import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import CompanyRolePermissionTree from "@/components/CompanyRolePermissionTree.vue";
import { normalizeMenuPermissionKeys } from "@/components/companyRolePermissionSelection";

const menuTree = [
  {
    code: "CS_ROOT",
    name: "基础服务",
    children: [
      { code: "CS_MEMBER", name: "企业成员", children: [] },
      {
        code: "CS_OPERATIONS",
        name: "业务操作",
        children: [
          { code: "CS_VIEW", name: "查看业务", children: [] },
          { code: "CS_EDIT", name: "编辑业务", children: [] }
        ]
      }
    ]
  }
];

const lastUpdate = (wrapper: ReturnType<typeof mount>) => {
  const updates = wrapper.emitted("update:modelValue") || [];
  return updates[updates.length - 1]?.[0] as string[];
};

describe("company role permission tree", () => {
  it("restores existing selections, shows partial ancestors, and renders no level badges", () => {
    const wrapper = mount(CompanyRolePermissionTree, {
      props: { nodes: menuTree, modelValue: ["CS_VIEW"] }
    });
    const checkboxes = wrapper.findAll<HTMLInputElement>('input[type="checkbox"]');

    expect(checkboxes).toHaveLength(5);
    expect(checkboxes.map((item) => item.element.checked)).toEqual([false, false, false, true, false]);
    expect(checkboxes[0].element.indeterminate).toBe(true);
    expect(checkboxes[2].element.indeterminate).toBe(true);
    expect(wrapper.find(".company-role-permission-name small").exists()).toBe(false);
    expect(wrapper.emitted("update:modelValue")).toBeUndefined();
  });

  it("selects and clears a parent together with all descendants", async () => {
    const wrapper = mount(CompanyRolePermissionTree, {
      props: { nodes: menuTree, modelValue: [] }
    });

    await wrapper.findAll<HTMLInputElement>('input[type="checkbox"]')[0].setValue(true);
    expect(lastUpdate(wrapper)).toEqual(["CS_ROOT", "CS_MEMBER", "CS_OPERATIONS", "CS_VIEW", "CS_EDIT"]);

    await wrapper.setProps({ modelValue: lastUpdate(wrapper) });
    await wrapper.findAll<HTMLInputElement>('input[type="checkbox"]')[0].setValue(false);
    expect(lastUpdate(wrapper)).toEqual([]);
  });

  it("auto-selects complete ancestors and keeps siblings when one child is cleared", async () => {
    const wrapper = mount(CompanyRolePermissionTree, {
      props: {
        nodes: menuTree,
        modelValue: ["CS_ROOT", "CS_MEMBER", "CS_OPERATIONS", "CS_VIEW", "CS_EDIT"]
      }
    });

    await wrapper.findAll<HTMLInputElement>('input[type="checkbox"]')[4].setValue(false);

    expect(lastUpdate(wrapper)).toEqual(["CS_MEMBER", "CS_VIEW"]);
    await wrapper.setProps({ modelValue: lastUpdate(wrapper) });
    const checkboxes = wrapper.findAll<HTMLInputElement>('input[type="checkbox"]');
    expect(checkboxes[0].element.checked).toBe(false);
    expect(checkboxes[0].element.indeterminate).toBe(true);
    expect(checkboxes[1].element.checked).toBe(true);
    expect(checkboxes[2].element.checked).toBe(false);
    expect(checkboxes[2].element.indeterminate).toBe(true);
    expect(checkboxes[3].element.checked).toBe(true);
    expect(checkboxes[4].element.checked).toBe(false);
  });

  it("auto-selects parent codes when every child is selected", async () => {
    const wrapper = mount(CompanyRolePermissionTree, {
      props: { nodes: menuTree, modelValue: ["CS_MEMBER", "CS_VIEW"] }
    });

    await wrapper.findAll<HTMLInputElement>('input[type="checkbox"]')[4].setValue(true);

    expect(lastUpdate(wrapper)).toEqual(["CS_MEMBER", "CS_VIEW", "CS_EDIT", "CS_OPERATIONS", "CS_ROOT"]);
  });

  it("normalizes saved keys by expanding selected parents and completing ancestors", () => {
    expect(normalizeMenuPermissionKeys(menuTree, ["CS_OPERATIONS"])).toEqual([
      "CS_OPERATIONS",
      "CS_VIEW",
      "CS_EDIT"
    ]);
    expect(normalizeMenuPermissionKeys(menuTree, ["CS_MEMBER", "CS_VIEW", "CS_EDIT"])).toEqual([
      "CS_MEMBER",
      "CS_VIEW",
      "CS_EDIT",
      "CS_OPERATIONS",
      "CS_ROOT"
    ]);
  });
});
