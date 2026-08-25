// @ts-nocheck -- CSS and Vue rendering contract test.
import { mount } from "@vue/test-utils";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import SupplierPostcard from "../components/SupplierPostcard.vue";
import postcardSource from "../components/SupplierPostcard.vue?raw";

const styles = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/styles/supplier-directory.css"), "utf8");
const localeSource = readFileSync(resolve(process.cwd(), "src/modules/supplierDirectory/locales/index.ts"), "utf8");
const labels = {
  enabled: "启用",
  disabled: "停用",
  products: "商品数量",
  categories: "大类数量",
  rating: "评分",
  reputation: "信誉情况",
  noRating: "暂无评价",
  evaluations: "条评价",
  positiveRate: "好评率",
  viewDetail: "查看详情",
  enable: "启用供货商",
  disable: "停用供货商"
};
const supplier = {
  companyId: 35,
  id: "SUP-35",
  name: "测试供货商",
  creditCode: "91330900TEST000001",
  logoFileId: "",
  logoUrl: "",
  introduction: "Marine supplier introduction",
  servicePorts: ["舟山"],
  categories: ["物料"],
  status: "ACTIVE",
  skuCount: 471,
  categoryCount: 18,
  contactName: "联系人",
  contactPhone: "13900000000",
  contactEmail: "supplier@example.com",
  averageRating: 4.8,
  evaluationCount: 12,
  positiveRate: 96,
  reputationLevel: "优秀"
};

describe("SupplierPostcard", () => {
  it("无管理权限时右上角仍展示非交互状态圆点且不保留旧LOGO徽标", () => {
    const wrapper = mount(SupplierPostcard, { props: { supplier, labels, canManageStatus: false } });
    const indicator = wrapper.get(".supplier-postcard__status-dot");
    expect(indicator.element.tagName).toBe("SPAN");
    expect(indicator.classes()).toContain("is-active");
    expect(indicator.attributes("role")).toBe("status");
    expect(indicator.attributes("aria-label")).toBe("当前状态：启用");
    expect(indicator.attributes("title")).toBe("当前状态：启用");
    expect(wrapper.find(".status-badge").exists()).toBe(false);
    expect(wrapper.find(".supplier-postcard__actions button").exists()).toBe(false);
    expect(postcardSource).not.toContain("StatusBadge");
    expect(postcardSource).not.toContain("has-actions");
  });

  it("有管理权限时状态圆点保持小尺寸并作为启停确认入口", async () => {
    const wrapper = mount(SupplierPostcard, { props: { supplier, labels, canManageStatus: true } });
    const button = wrapper.get("button.supplier-postcard__status-dot");
    expect(button.attributes("aria-label")).toBe("当前状态：启用；停用供货商");
    expect(button.attributes("title")).toBe("当前状态：启用；停用供货商");
    await button.trigger("click");
    expect(wrapper.emitted("toggleStatus")?.[0]).toEqual([supplier]);
    expect(styles).toMatch(/\.supplier-postcard__status-dot\s*\{[\s\S]*?width:\s*16px;[\s\S]*?height:\s*16px/);
  });

  it("停用状态圆点使用红色状态类并保留非颜色语义", () => {
    const wrapper = mount(SupplierPostcard, { props: { supplier: { ...supplier, status: "DISABLED" }, labels, canManageStatus: false } });
    const indicator = wrapper.get(".supplier-postcard__status-dot");
    expect(indicator.classes()).toContain("is-disabled");
    expect(indicator.attributes("aria-label")).toBe("当前状态：停用");
  });

  it("卡片使用玻璃层次且LOGO缩小后仍充满图片框", () => {
    expect(styles).toMatch(/\.supplier-postcard\s*\{[\s\S]*?background:\s*var\(--glass-card-bg\);[\s\S]*?backdrop-filter:\s*blur\(18px\)/);
    expect(styles).toMatch(/\.supplier-postcard__actions\s*\{[\s\S]*?position:\s*absolute/);
    expect(styles).toMatch(/\.supplier-postcard__logo img\s*\{[\s\S]*?object-fit:\s*cover/);
    expect(styles).toMatch(/\.supplier-postcard__identity\s*\{[^}]*grid-template-columns:\s*54px\s+minmax\(0,\s*1fr\)/);
    expect(styles).toMatch(/\.supplier-postcard__logo\s*\{[^}]*width:\s*54px;[^}]*height:\s*54px/);
    expect(styles).not.toMatch(/\.supplier-postcard[^{]*\{[^}]*box-shadow:[^;]*(?:16|18|20|24|28|32)px/);
  });

  it("联系人信息在LOGO与企业信息下方独立成行，不保留旧标题和负间距补偿", () => {
    const wrapper = mount(SupplierPostcard, { props: { supplier, labels } });
    const contact = wrapper.get(".supplier-postcard__contact-line");
    expect(contact.text()).toBe("联****人 / 139****0000 / su****@example.com");
    expect(contact.attributes("title")).toBe(contact.text());
    expect(contact.attributes("aria-label")).toBe(contact.text());
    expect(contact.findAll(".supplier-postcard__contact-value")).toHaveLength(3);
    expect(contact.findAll(".supplier-postcard__contact-separator")).toHaveLength(2);
    expect(contact.findAll(".supplier-postcard__contact-separator").map((item) => item.text())).toEqual(["/", "/"]);
    expect(wrapper.find(".supplier-postcard__contact dt").exists()).toBe(false);
    expect(postcardSource.indexOf('class="supplier-postcard__contact-line"')).toBeGreaterThan(postcardSource.indexOf('</header>'));
    expect(styles).toMatch(/\.supplier-postcard__contact-line\s*\{[^}]*text-align:\s*center;/);
    expect(styles).toMatch(/\.supplier-postcard__contact-value\s*\{[^}]*color:\s*#0877c8;[^}]*font-size:\s*14px;[^}]*font-weight:\s*(?:7|8)00/);
    expect(styles).toMatch(/\.supplier-postcard__contact-separator\s*\{[^}]*color:\s*#[0-9a-f]{6};[^}]*margin:\s*0\s+4px/);
    expect(styles).toMatch(/\.supplier-postcard__credit-code,\s*\.supplier-postcard__contact-line\s*\{[^}]*text-overflow:\s*ellipsis;[^}]*white-space:\s*nowrap/);
    expect(styles).not.toContain("margin: -42px 0 0");
    expect(styles).not.toContain("padding: 0 16px 13px 116px");
  });

  it("服务范围独立换行展示港口标签，用户可见文案不再使用服务港口", () => {
    const wrapper = mount(SupplierPostcard, { props: { supplier: { ...supplier, servicePorts: ["舟山港"] }, labels } });
    const scope = wrapper.get(".supplier-postcard__scope");
    expect(scope.text()).toContain("服务范围");
    expect(scope.text()).toContain("舟山港");
    expect(postcardSource).not.toContain("服务港口");
    expect(localeSource).toContain('servicePort: "服务范围"');
    expect(localeSource).not.toContain('servicePort: "服务港口"');
  });

  it("服务范围移到三格统计之后，不再展示独立信誉评价行", () => {
    const wrapper = mount(SupplierPostcard, { props: { supplier, labels } });
    expect(wrapper.find(".supplier-postcard__reputation").exists()).toBe(false);
    expect(wrapper.text()).not.toContain("信誉情况");
    expect(wrapper.text()).not.toContain("好评率");
    expect(postcardSource.indexOf('class="supplier-postcard__scope"')).toBeGreaterThan(postcardSource.indexOf('class="supplier-postcard__metrics"'));
  });

  it("评价格始终展示5颗星，不显示分值、分母或占位符", () => {
    const rated = mount(SupplierPostcard, { props: { supplier, labels } });
    const ratedStars = rated.get(".supplier-postcard__metric-stars");
    expect(ratedStars.findAll("i")).toHaveLength(5);
    expect(ratedStars.findAll("i.active")).toHaveLength(5);
    expect(ratedStars.attributes("aria-label")).toBe("4.8分");
    expect(rated.text()).not.toContain("/5");

    const unrated = mount(SupplierPostcard, { props: { supplier: { ...supplier, averageRating: null, evaluationCount: 0 }, labels } });
    const emptyStars = unrated.get(".supplier-postcard__metric-stars");
    expect(emptyStars.findAll("i")).toHaveLength(5);
    expect(emptyStars.findAll("i.active")).toHaveLength(0);
    expect(emptyStars.attributes("aria-label")).toBe("暂无评价");
    expect(unrated.text()).not.toContain("--");
    expect(postcardSource).not.toContain("ratingText");
  });

  it("商品数量使用稳定语义类呈现绿色，大类数量保持蓝色", () => {
    const wrapper = mount(SupplierPostcard, { props: { supplier, labels } });
    expect(wrapper.get(".supplier-postcard__metric.is-products").text()).toContain("471");
    expect(wrapper.get(".supplier-postcard__metric.is-categories").text()).toContain("18");
    expect(styles).toMatch(/\.supplier-postcard__metric\.is-products strong\s*\{[\s\S]*?color:\s*#[0-9a-f]{6}/i);
    expect(styles).toMatch(/\.supplier-postcard__metric\.is-categories strong\s*\{[\s\S]*?color:\s*#0877c8/i);
  });

  it("企业名、电话、邮箱在列表卡片和无障碍名称中统一脱敏", () => {
    const wrapper = mount(SupplierPostcard, { props: { supplier: { ...supplier, name: "宁波一忱工贸有限公司" }, labels } });
    expect(wrapper.get(".supplier-postcard__company h2").text()).toBe("宁波一********有限公司");
    expect(wrapper.get("article").attributes("aria-label")).toContain("宁波一********有限公司");
    expect(wrapper.text()).toContain("139****0000");
    expect(wrapper.text()).toContain("su****@example.com");
    expect(wrapper.text()).not.toContain("supplier@example.com");
    expect(wrapper.html()).not.toContain("supplier@example.com");
    expect(wrapper.text()).toContain("联****人");
  });

  it("社会信用代码不展示标题或真实值，统一显示十位星号", () => {
    const withCode = mount(SupplierPostcard, { props: { supplier, labels } });
    const withoutCode = mount(SupplierPostcard, { props: { supplier: { ...supplier, creditCode: "" }, labels } });
    expect(withCode.get(".supplier-postcard__credit-code").text()).toBe("**********");
    expect(withoutCode.get(".supplier-postcard__credit-code").text()).toBe("**********");
    expect(withCode.text()).not.toContain("统一社会信用代码：");
    expect(withCode.html()).not.toContain(supplier.creditCode);
  });
});
