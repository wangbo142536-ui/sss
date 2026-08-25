import { describe, expect, it } from "vitest";
import { maskSupplierContactName, maskSupplierEmail, maskSupplierName, maskSupplierPhone } from "../services/supplierDirectoryPrivacy";

describe("supplier directory privacy", () => {
  it("中文企业名保留前3后4并隐藏中间内容", () => {
    expect(maskSupplierName("宁波一忱工贸有限公司")).toBe("宁波一********有限公司");
  });

  it("英文企业名保留前4后8并隐藏中间内容", () => {
    expect(maskSupplierName("DINGHENG INTELLIGENT SUPPLY CHAIN (SG) PTE LTD")).toBe("DING******** PTE LTD");
  });

  it("短名称也必须隐藏且不能重复显示首尾字符", () => {
    expect(maskSupplierName("供应商A")).toBe("供********商A");
    expect(maskSupplierName("YILONG")).toBe("YI********ONG");
  });

  it("电话保留前3后4，邮箱仅保留前2位和域名", () => {
    expect(maskSupplierPhone("13900008171")).toBe("139****8171");
    expect(maskSupplierEmail("dh.supplier.000032@init.local")).toBe("dh****@init.local");
  });

  it("联系人保留首尾各一字，短名也至少隐藏一字", () => {
    expect(maskSupplierContactName("李海燕")).toBe("李****燕");
    expect(maskSupplierContactName("王磊")).toBe("王****");
    expect(maskSupplierContactName("王")).toBe("****");
    expect(maskSupplierContactName("")).toBe("--");
  });
});
