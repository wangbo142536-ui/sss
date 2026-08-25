// @ts-nocheck -- Vitest reads component source as a structural contract.
import { describe, expect, it } from "vitest";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";
import zhSource from "@/locales/zh-CN.ts?raw";
import enSource from "@/locales/en-US.ts?raw";

describe("company role management UI contract", () => {
  it("keeps one edit action and uses the recursive permission tree in the shared role drawer", () => {
    const tableStart = workbenchSource.indexOf('<DataTable v-else :columns="companyRoleColumns"');
    const tableEnd = workbenchSource.indexOf("</DataTable>", tableStart);
    const roleTable = workbenchSource.slice(tableStart, tableEnd);

    expect(roleTable.match(/openEditCompanyRole\(row\)/g)).toHaveLength(1);
    expect(roleTable).not.toContain("companyMembers.action.configureRole");
    expect(workbenchSource).toContain("<CompanyRolePermissionTree");
    expect(workbenchSource).toContain('v-model="roleForm.menuPermissionKeys"');
    expect(workbenchSource).toContain("normalizeMenuPermissionKeys(companyMenuOptions.value, roleForm.value.menuPermissionKeys)");
  });

  it("uses the exact localized search label above the existing member keyword input", () => {
    expect(workbenchSource).toContain('t("companyMembers.filter.searchLabel")');
    expect(zhSource).toContain('searchLabel: "检索"');
    expect(enSource).toContain('searchLabel: "Search"');
    expect(zhSource).not.toContain("permissionLevel:");
    expect(enSource).not.toContain("permissionLevel:");
  });

  it("hides role codes from the list and keeps the shared drawer on backend-generated codes", () => {
    const drawerStart = workbenchSource.indexOf('<form v-else-if="memberDrawerMode === \'role\'"');
    const drawerEnd = workbenchSource.indexOf('<form v-else class="member-drawer-form"', drawerStart);
    const roleDrawer = workbenchSource.slice(drawerStart, drawerEnd);
    const submitStart = workbenchSource.indexOf('if (memberDrawerMode.value === "role")');
    const submitEnd = workbenchSource.indexOf('} else if (memberDrawerMode.value === "create")', submitStart);
    const roleSubmit = workbenchSource.slice(submitStart, submitEnd);

    expect(workbenchSource).not.toContain('{ key: "code", label: t("companyMembers.field.roleCode")');
    expect(roleDrawer).not.toContain("roleForm.roleCode");
    expect(roleDrawer).not.toContain("companyMembers.field.roleCode");
    expect(roleDrawer).not.toContain("companyMembers.placeholder.roleCode");
    expect(roleSubmit).toContain("updateCompanyRole(selectedCompanyRoleCode.value");
    expect(roleSubmit).toContain("createCompanyRole({");
    expect(roleSubmit).not.toContain("roleCode:");
  });
});
