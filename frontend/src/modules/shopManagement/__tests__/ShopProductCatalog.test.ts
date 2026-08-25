// @ts-nocheck -- Vitest runs this CSS contract in Node; the app does not ship Node typings.
import { mount } from "@vue/test-utils";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it, vi } from "vitest";
import ShopProductCatalog from "../components/ShopProductCatalog.vue";
import type { ShopCatalogSku } from "../types/shopProductCatalog";

const catalogStyles = readFileSync(resolve(process.cwd(), "src/modules/shopManagement/styles/shop-product-catalog.css"), "utf8");

const rows: ShopCatalogSku[] = [
  {
    id: "m-1",
    productType: "MATERIAL",
    categoryCode: "1501",
    categoryName: "文具用品",
    productName: "荧光笔",
    productTags: ["热卖", "质量好", "价格实惠"],
    supplierSkuCode: "MARKER-A",
    platformCode: "150101",
    thumbnail: "/images/marker-a.png",
    imageUrl: "",
    packing: "10支/盒",
    stock: 24,
    unit: "盒",
    price: 16.8,
    currency: "CNY"
  },
  {
    id: "m-2",
    productType: "MATERIAL",
    categoryCode: "1501",
    categoryName: "文具用品",
    productName: "签字笔",
    supplierSkuCode: "PEN-B",
    platformCode: "150102",
    thumbnail: "",
    imageUrl: "",
    packing: "12支/盒",
    stock: 8,
    unit: "盒",
    price: 12,
    currency: "CNY"
  },
  {
    id: "f-1",
    productType: "FOOD",
    categoryCode: "F01",
    categoryName: "新鲜蔬菜",
    productName: "生菜",
    supplierSkuCode: "FOOD-01",
    platformCode: "",
    thumbnail: "",
    imageUrl: "",
    packing: "5公斤/箱",
    stock: 3,
    unit: "箱",
    price: 25,
    currency: "CNY"
  }
];

describe("ShopProductCatalog", () => {
  it("书柜使用多层书架、书册悬浮抽出反馈，且不使用大模糊阴影", () => {
    expect(catalogStyles).toContain(".shop-product-catalog__cabinet-body");
    expect(catalogStyles).toContain(".shop-product-catalog__shelf-row");
    expect(catalogStyles).toMatch(/category-book:hover[\s\S]*translateY\(-6px\)/);
    expect(catalogStyles).toContain("shop-catalog-view-enter");
    expect(catalogStyles).toContain("animation-fill-mode: backwards");
    expect(catalogStyles).toContain("@media (prefers-reduced-motion: reduce)");
    expect(catalogStyles).not.toMatch(/box-shadow:[^;]*(?:16|20|24|28|32)px/);
  });

  it("不同商品大类纵向排列，每个大类只保留一层横向书架", () => {
    expect(catalogStyles).toMatch(/shop-product-catalog__cabinet-grid[\s\S]*?grid-template-columns:\s*minmax\(0,\s*1fr\)/);
    expect(catalogStyles).toMatch(/shelf-row--preview[\s\S]*?shelf-books[\s\S]*?display:\s*flex/);
    expect(catalogStyles).toMatch(/shelf-row--preview[\s\S]*?shelf-books[\s\S]*?flex-wrap:\s*nowrap/);
    expect(catalogStyles).toMatch(/shelf-row--preview[\s\S]*?shelf-books[\s\S]*?overflow:\s*hidden/);

    const wrapper = mount(ShopProductCatalog, { props: { rows } });
    expect(wrapper.findAll('[data-catalog-shelf="preview"]')).toHaveLength(2);
    expect(wrapper.text()).not.toContain("暂无更多分类");
  });

  it("宽度不足时预留最后一本为更多分类，不换行也不横向滚动", () => {
    const manyMaterialRows: ShopCatalogSku[] = Array.from({ length: 8 }, (_, index) => ({
      ...rows[0],
      id: `material-${index + 1}`,
      categoryCode: `M${index + 1}`,
      categoryName: `物料分类${index + 1}`,
      productName: `物料商品${index + 1}`,
      supplierSkuCode: `MATERIAL-${index + 1}`
    }));
    const wrapper = mount(ShopProductCatalog, { props: { rows: [...manyMaterialRows, rows[2]] } });

    expect(wrapper.findAll('[data-catalog-group="MATERIAL"]')).toHaveLength(1);
    expect(wrapper.findAll('[data-catalog-group="FOOD"]')).toHaveLength(1);
    expect(wrapper.get('[data-catalog-group="MATERIAL"]').findAll("button")).toHaveLength(6);
    expect(wrapper.get('[aria-label="查看物料全部8个分类"]').text()).toContain("+3");
  });

  it("首页分类书卡和封面统一宽高，长名称使用省略号", () => {
    expect(catalogStyles).toMatch(/shop-product-catalog__preview-book\s*\{[\s\S]*?width:\s*112px[\s\S]*?height:\s*148px/);
    expect(catalogStyles).toMatch(/shop-product-catalog__book-cover\s*\{[\s\S]*?width:\s*72px[\s\S]*?height:\s*72px/);
    expect(catalogStyles).toMatch(/shop-product-catalog__book-label\s*\{[\s\S]*?text-overflow:\s*ellipsis[\s\S]*?white-space:\s*nowrap/);
  });

  it("未分类书册使用橙色异常语义并显示待归类文字", async () => {
    const wrapper = mount(ShopProductCatalog, {
      props: { rows: [rows[0], { ...rows[0], id: "uncategorized", categoryCode: "", categoryName: "", productName: "未分类商品" }] }
    });

    const previewBook = wrapper.get('[aria-label="异常：打开物料中的未分类书册，1个SKU"]');
    expect(previewBook.classes()).toContain("is-uncategorized");
    expect(previewBook.text()).toContain("待归类");

    await wrapper.get('[aria-label="打开物料书柜，2个SKU"]').trigger("click");
    const categoryBook = wrapper.get('[aria-label="异常：打开未分类书册，1个SKU"]');
    expect(categoryBook.classes()).toContain("is-uncategorized");
    expect(categoryBook.text()).toContain("待归类");
    expect(catalogStyles).toMatch(/is-uncategorized[\s\S]*?--book-surface:\s*#fff4e5/);
  });

  it("桌面SKU为一行四张卡片，大图固定在等高图片框内完整显示", () => {
    expect(catalogStyles).toContain("grid-template-columns: repeat(4, minmax(0, 1fr))");
    expect(catalogStyles).toMatch(/shop-product-catalog__sku-card-button\s*\{[\s\S]*?grid-template-rows:\s*168px minmax\(0,\s*1fr\)/);
    expect(catalogStyles).toMatch(/shop-product-catalog__sku-image\s*\{[\s\S]*?height:\s*168px[\s\S]*?min-height:\s*0[\s\S]*?overflow:\s*hidden/);
    expect(catalogStyles).toMatch(/shop-product-catalog__sku-image img\s*\{[\s\S]*?max-width:\s*100%[\s\S]*?max-height:\s*100%[\s\S]*?object-fit:\s*contain/);
  });

  it("大类书架使用七列固定网格且每本分类书都展示内容封面", async () => {
    expect(catalogStyles).toMatch(
      /shelf-row--categories[\s\S]*?shelf-books[\s\S]*?grid-template-columns:\s*repeat\(7,\s*minmax\(0,\s*1fr\)\)/
    );
    expect(catalogStyles).toMatch(/shelf-row--categories[\s\S]*?shelf-books[\s\S]*?overflow-x:\s*visible/);
    expect(catalogStyles).toContain(".shop-product-catalog__category-cover img");
    expect(catalogStyles).toContain("object-fit: cover");

    const wrapper = mount(ShopProductCatalog, { props: { rows } });
    await wrapper.get('[aria-label="打开物料书柜，2个SKU"]').trigger("click");
    const book = wrapper.get('[aria-label="打开文具用品书册，2个SKU"]');
    const cover = book.get("img");
    expect(cover.attributes("alt")).toBe("");
    expect(cover.attributes("src")).toContain("stationery");
  });

  it("通过可键盘操作的书柜和书册进入桌面双列SKU卡片", async () => {
    const wrapper = mount(ShopProductCatalog, { props: { rows } });

    expect(wrapper.get('[aria-label="打开物料书柜，2个SKU"]').element.tagName).toBe("BUTTON");
    expect(wrapper.text()).toContain("物料");
    expect(wrapper.text()).toContain("伙食");
    expect(wrapper.findAll('[data-catalog-shelf="preview"]')).toHaveLength(2);
    expect(wrapper.get('[aria-label="打开物料中的文具用品书册，2个SKU"]').element.tagName).toBe("BUTTON");

    await wrapper.get('[aria-label="打开物料书柜，2个SKU"]').trigger("click");
    expect(wrapper.text()).toContain("文具用品");
    expect(wrapper.get('[aria-label="打开文具用品书册，2个SKU"]').element.tagName).toBe("BUTTON");

    await wrapper.get('[aria-label="打开文具用品书册，2个SKU"]').trigger("click");
    const skuGrid = wrapper.get('[aria-label="文具用品SKU列表"]');
    expect(skuGrid.classes()).toContain("shop-product-catalog__sku-grid");
    expect(skuGrid.findAll("article")).toHaveLength(2);
    expect(wrapper.text()).toContain("荧光笔");
    expect(wrapper.text()).toContain("10支/盒");
    expect(wrapper.text()).toContain("库存 24 盒");
    expect(wrapper.text()).toContain("¥16.80");
    expect(wrapper.get("nav").attributes("aria-label")).toBe("产品名册路径");
  });

  it("支持面包屑返回并展示空数据状态", async () => {
    const wrapper = mount(ShopProductCatalog, { props: { rows } });
    await wrapper.get('[aria-label="打开物料书柜，2个SKU"]').trigger("click");
    await wrapper.get('[aria-label="打开文具用品书册，2个SKU"]').trigger("click");
    await wrapper.get('[aria-label="返回上一级"]').trigger("click");
    expect(wrapper.get('[aria-label="打开文具用品书册，2个SKU"]')).toBeTruthy();

    await wrapper.setProps({ rows: [] });
    expect(wrapper.get('[role="status"]').text()).toContain("当前筛选条件下暂无商品");
  });

  it("点击SKU卡片打开编辑弹窗，修改后调用真实保存回调并关闭", async () => {
    const saveSku = vi.fn().mockResolvedValue(undefined);
    const wrapper = mount(ShopProductCatalog, {
      props: {
        rows,
        saveSku,
        categoryOptions: [{ value: "1501", label: "文具用品" }]
      },
      attachTo: document.body
    });

    await wrapper.get('[aria-label="打开物料书柜，2个SKU"]').trigger("click");
    await wrapper.get('[aria-label="打开文具用品书册，2个SKU"]').trigger("click");
    await wrapper.get('[aria-label="编辑商品荧光笔"]').trigger("click");

    const dialog = document.body.querySelector('[role="dialog"][aria-label="编辑商品"]') as HTMLElement;
    expect(dialog).toBeTruthy();
    const productName = dialog.querySelector('input[aria-label="商品名称"]') as HTMLInputElement;
    productName.value = "新版荧光笔";
    productName.dispatchEvent(new Event("input", { bubbles: true }));
    (dialog.querySelector('button[aria-label="保存修改"]') as HTMLButtonElement).click();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(saveSku).toHaveBeenCalledTimes(1);
    expect(saveSku.mock.calls[0][0].productName).toBe("新版荧光笔");
    expect(document.body.querySelector('[role="dialog"][aria-label="编辑商品"]')).toBeNull();
    wrapper.unmount();
  });

  it("只读名册点击SKU打开商品详情，标签右上展示且全部字段不可修改", async () => {
    const wrapper = mount(ShopProductCatalog, {
      props: { rows, readonly: true },
      attachTo: document.body
    });

    await wrapper.get('[aria-label="打开物料书柜，2个SKU"]').trigger("click");
    await wrapper.get('[aria-label="打开文具用品书册，2个SKU"]').trigger("click");
    const skuCard = wrapper.get('[aria-label="查看商品荧光笔"]');
    expect(skuCard.text()).toContain("热卖");
    expect(skuCard.text()).toContain("质量好");
    expect(skuCard.text()).toContain("+1");
    expect(catalogStyles).toMatch(/shop-product-catalog__sku-tags\s*\{[\s\S]*?top:\s*8px;[\s\S]*?right:\s*8px/);

    await skuCard.trigger("click");
    const dialog = document.body.querySelector('[role="dialog"][aria-label="查看商品"]') as HTMLElement;
    expect(dialog).toBeTruthy();
    expect(dialog.textContent).toContain("商品详情");
    expect(dialog.querySelectorAll("input:not([readonly])")).toHaveLength(0);
    expect(dialog.querySelectorAll("select:not([disabled])")).toHaveLength(0);
    expect(dialog.querySelector('button[aria-label="保存修改"]')).toBeNull();
    expect(dialog.querySelector('button[aria-label="添加商品标签"]')).toBeNull();
    expect(dialog.querySelector('button[aria-label="添加规格"]')).toBeNull();
    (dialog.querySelector('button[aria-label="关闭商品详情"]') as HTMLButtonElement).click();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(document.body.querySelector('[role="dialog"][aria-label="查看商品"]')).toBeNull();
    wrapper.unmount();
  });
});
