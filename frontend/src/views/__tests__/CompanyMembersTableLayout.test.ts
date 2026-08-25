// @ts-nocheck -- Vitest runs this source contract in Node; the app does not ship Node typings.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import dataTableSource from "@/components/DataTable.vue?raw";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";

const workbenchCss = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");

describe("company member table layout contract", () => {
  it("keeps account and operation fixed while the middle columns scroll at readable widths", () => {
    expect(workbenchSource).toContain('class="company-members-table"');
    expect(workbenchSource).toContain('{ key: "username", label: t("companyMembers.field.account"), width: "220px" }');
    expect(workbenchSource).toContain('{ key: "contact", label: t("companyMembers.field.contact"), width: "260px" }');
    expect(workbenchSource).toContain('{ key: "operation", label: t("common.operation"), width: "220px", align: "center" }');
    expect(dataTableSource).toContain(':data-column-key="column.key"');
    expect(workbenchCss).toMatch(/\.company-members-table \.data-table\s*\{[^}]*min-width:\s*1560px;/s);
    expect(workbenchCss).toMatch(/\.company-members-table \[data-column-key="username"\]\s*\{[^}]*position:\s*sticky;[^}]*left:\s*48px;/s);
    expect(workbenchCss).toMatch(/\.company-members-table \[data-column-key="operation"\]\s*\{[^}]*position:\s*sticky;[^}]*right:\s*0;/s);
    expect(workbenchCss).toMatch(/\.company-members-table \.icon-action-row\s*\{[^}]*flex-wrap:\s*nowrap;/s);
  });
});
