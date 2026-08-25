// @ts-nocheck -- Vitest runs this CSS contract in Node; the app does not ship Node typings.
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import skuThumbnailSource from "@/components/SkuThumbnail.vue?raw";
import workbenchSource from "@/views/WorkbenchPage.vue?raw";

const workbenchStyles = readFileSync(resolve(process.cwd(), "src/styles/workbench.css"), "utf8");

describe("Workbench shop product table columns", () => {
  it("图片列圆形缩略图只负责打开大图，替换操作位于预览弹窗", () => {
    const imageCell = workbenchSource.slice(
      workbenchSource.indexOf('<template #cell-image="{ row }">'),
      workbenchSource.indexOf('<template #cell-category="{ row }">')
    );

    expect(imageCell).toContain(":action-label=\"t('action.previewImage')\"");
    expect(imageCell).toContain("openShopSkuImagePreview(row)");
    expect(imageCell).not.toContain("openShopSkuImagePicker(row)");
    expect(imageCell).not.toContain("<button");
    expect(imageCell).not.toContain("imageReplaceShort");
    expect(skuThumbnailSource).toContain("actionLabel?: string");
    expect(workbenchStyles).toContain(".shop-image-editor .sku-thumb");
    expect(workbenchStyles).toContain("border-radius: 50%");
    expect(workbenchStyles).toContain(".image-preview-stage-actions");
    expect(workbenchSource).toContain(':action-label="previewShopSkuRowId ? t(\'page.supplierProducts.imageReplace\') : undefined"');
    expect(workbenchSource).toContain('@action="replacePreviewedShopSkuImage"');
  });

  it("标记列固定放在操作列之前，新增状态不再挤在商品名称单元格", () => {
    const columns = workbenchSource.slice(
      workbenchSource.indexOf("const shopSkuColumns"),
      workbenchSource.indexOf("const companyQualificationColumns")
    );
    const productNameCell = workbenchSource.slice(
      workbenchSource.indexOf('<template #cell-productName="{ row }">'),
      workbenchSource.indexOf('<template #cell-specs="{ row }">')
    );
    const markerCellStart = workbenchSource.indexOf('<template #cell-marker="{ row }">');
    const markerCell = workbenchSource.slice(
      markerCellStart,
      workbenchSource.indexOf('<template #cell-operation="{ row }">', markerCellStart)
    );
    const previewActionLabels = workbenchSource.slice(
      workbenchSource.indexOf("const getShopPreviewActionLabel"),
      workbenchSource.indexOf("const getShopPreviewActionHint")
    );

    expect(columns).toContain('{ key: "marker", label: "标记", width: "72px", align: "center" }');
    expect(columns.indexOf('key: "marker"')).toBeLessThan(columns.indexOf('key: "operation"'));
    expect(workbenchSource).toContain('<template #cell-marker="{ row }">');
    expect(workbenchSource).toContain("isShopNewSkuRow(row)");
    expect(productNameCell).not.toContain("shop-preview-action-pill");
    expect(markerCell).toContain('v-if="row.previewAction"');
    expect(markerCell).toContain("getShopPreviewActionLabel(row.previewAction)");
    expect(markerCell).toContain("row.previewAction.toLowerCase()");
    expect(previewActionLabels).toContain('UPDATE: "page.supplierProducts.previewActionUpdate"');
    expect(workbenchStyles).toContain(".shop-management-pane--products .data-table");
    expect(workbenchStyles).toContain('data-column-key="marker"');
    expect(workbenchStyles).toContain("min-width: 72px");
    expect(workbenchStyles).toContain('data-column-key="operation"');
    expect(workbenchStyles).toContain("min-width: 150px");
  });
});
