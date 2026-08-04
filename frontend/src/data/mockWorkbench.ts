import type { SkuAttribute, SupplierSku, UserRole, WorkbenchMenuItem } from "@/types/workbench";

const productImage = (bg: string, fg: string, label: string) =>
  `data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='96' height='96' viewBox='0 0 96 96'%3E%3Crect width='96' height='96' rx='10' fill='${bg.replace("#", "%23")}'/%3E%3Crect x='18' y='24' width='60' height='48' rx='8' fill='white' opacity='.85'/%3E%3Cpath d='M28 38h40M28 49h30M28 60h22' stroke='${fg.replace("#", "%23")}' stroke-width='5' stroke-linecap='round'/%3E%3Ctext x='48' y='88' text-anchor='middle' font-family='Arial' font-size='10' fill='${fg.replace("#", "%23")}'%3E${label}%3C/text%3E%3C/svg%3E`;

const roles: UserRole[] = ["admin", "purchaser", "supplier", "operator", "finance"];

export const currentUser = {
  name: "陈航",
  role: "admin" as UserRole,
  roleKey: "role.admin"
};

export const menuItems: WorkbenchMenuItem[] = [
  {
    key: "dashboardGroup",
    labelKey: "nav.dashboard",
    icon: "DB",
    sortOrder: 0,
    roles,
    children: [
      { key: "dashboardGovernment", labelKey: "nav.dashboardGovernment", route: "/dashboard-government", icon: "DG", sortOrder: 0, roles },
      { key: "dashboard", labelKey: "nav.dashboard", route: "/dashboard", icon: "DB", sortOrder: 5, roles }
    ]
  },
  {
    key: "procurementServices",
    labelKey: "nav.procurementServices",
    icon: "PS",
    sortOrder: 10,
    roles,
    children: [
      {
        key: "materialProcurement",
        labelKey: "nav.materialProcurement",
        icon: "MP",
        sortOrder: 0,
        roles: ["admin", "purchaser", "supplier"],
        children: [
          { key: "requests", labelKey: "nav.requests", route: "/procurement/materials", icon: "RQ", sortOrder: 0, roles: ["admin", "purchaser"] },
          { key: "inquiries", labelKey: "nav.inquiries", route: "/inquiries", icon: "IQ", sortOrder: 10, roles: ["admin", "purchaser", "supplier"] },
          { key: "quotes", labelKey: "nav.quotes", route: "/quotes", icon: "QT", sortOrder: 20, roles: ["admin", "purchaser", "supplier"] },
          { key: "compare", labelKey: "nav.compare", route: "/comparison", icon: "CP", sortOrder: 30, roles: ["admin", "purchaser"] },
          { key: "orders", labelKey: "nav.orders", route: "/orders/new", icon: "PO", sortOrder: 40, roles: ["admin", "purchaser"] },
          { key: "supplierOrders", labelKey: "nav.supplierOrders", route: "/supplier/orders", icon: "SO", sortOrder: 50, roles: ["admin", "supplier"] },
          { key: "settlements", labelKey: "nav.settlements", route: "/settlements", icon: "ST", sortOrder: 60, roles: ["admin", "purchaser"] },
          { key: "supplierSettlements", labelKey: "nav.supplierSettlements", route: "/supplier/settlements", icon: "SS", sortOrder: 70, roles: ["admin", "supplier"] },
          { key: "evaluations", labelKey: "nav.evaluations", route: "/evaluations", icon: "EV", sortOrder: 80, roles: ["admin", "purchaser"] }
        ]
      },
      {
        key: "foodProcurement",
        labelKey: "nav.foodProcurement",
        icon: "FP",
        sortOrder: 10,
        roles,
        children: [
          { key: "food", labelKey: "nav.food", route: "/procurement/food", icon: "FD", sortOrder: 0, roles },
          { key: "foodInquiries", labelKey: "nav.foodInquiries", route: "/food/inquiries", icon: "IQ", sortOrder: 10, roles },
          { key: "foodQuotes", labelKey: "nav.foodQuotes", route: "/food/quotes", icon: "QT", sortOrder: 20, roles },
          { key: "foodCompare", labelKey: "nav.foodCompare", route: "/food/comparison", icon: "CP", sortOrder: 30, roles },
          { key: "foodOrders", labelKey: "nav.foodOrders", route: "/food/orders", icon: "PO", sortOrder: 40, roles },
          { key: "foodSupplierOrders", labelKey: "nav.foodSupplierOrders", route: "/supplier/food/orders", icon: "SO", sortOrder: 50, roles },
          { key: "foodSettlements", labelKey: "nav.foodSettlements", route: "/food/settlements", icon: "ST", sortOrder: 60, roles },
          { key: "foodSupplierSettlements", labelKey: "nav.foodSupplierSettlements", route: "/supplier/food/settlements", icon: "SS", sortOrder: 70, roles },
          { key: "foodEvaluations", labelKey: "nav.foodEvaluations", route: "/food/evaluations", icon: "EV", sortOrder: 80, roles }
        ]
      }
    ]
  },
  {
    key: "trafficService",
    labelKey: "nav.bargeServices",
    icon: "BS",
    sortOrder: 20,
    roles: ["admin", "operator", "supplier"],
    children: [
      { key: "trafficBoat", labelKey: "nav.trafficBoat", route: "/traffic-boat", icon: "TB", sortOrder: 0, roles: ["admin", "operator", "supplier"] },
      { key: "trafficBoatMyServices", labelKey: "nav.trafficBoatMyServices", route: "/traffic-boat/my-services", icon: "MS", sortOrder: 5, roles: ["admin", "purchaser", "operator", "supplier"] },
      { key: "bargeSettlements", labelKey: "nav.settlements", route: "/traffic-boat/settlements", icon: "ST", sortOrder: 10, roles: ["admin", "operator", "supplier"] }
    ]
  },
  {
    key: "customsServices",
    labelKey: "nav.customsServices",
    icon: "CS",
    sortOrder: 30,
    roles,
    children: [
      { key: "customsDeclarations", labelKey: "nav.customsDeclarations", route: "/customs-services/declarations", icon: "CD", sortOrder: 0, roles }
    ]
  },
  { key: "portShippingServices", labelKey: "nav.portShippingServices", route: "/port-shipping-services", icon: "PH", sortOrder: 35, roles },
  {
    key: "regulatoryServices",
    labelKey: "nav.regulatoryServices",
    icon: "RS",
    sortOrder: 40,
    roles,
    children: [
      { key: "qualitySupervision", labelKey: "nav.qualitySupervision", route: "/regulatory/quality", icon: "QS", sortOrder: 0, roles }
    ]
  },
  { key: "borderInspectionServices", labelKey: "nav.borderInspectionServices", route: "/border-inspection-services", icon: "BI", sortOrder: 45, roles },
  { key: "maritimeServices", labelKey: "nav.maritimeServices", route: "/maritime-services", icon: "MS", sortOrder: 50, roles },
  { key: "crewServices", labelKey: "nav.crewServices", route: "/crew-services", icon: "USE", sortOrder: 55, roles: ["admin", "purchaser", "operator"] },
  { key: "taxServices", labelKey: "nav.taxServices", route: "/tax-services", icon: "TX", sortOrder: 60, roles },
  { key: "financialServices", labelKey: "nav.financialServices", route: "/financial-services", icon: "FN", sortOrder: 70, roles },
  { key: "weatherServices", labelKey: "nav.weatherServices", route: "/weather-services", icon: "WT", sortOrder: 80, roles },
  { key: "vesselDynamicsServices", labelKey: "nav.vesselDynamicsServices", route: "/vessel-dynamics-services", icon: "VD", sortOrder: 90, roles },
  {
    key: "platformOperations",
    labelKey: "nav.platformOperations",
    icon: "OP",
    sortOrder: 95,
    roles: ["admin", "supplier"],
    children: [
      { key: "supplierDataAnalysis", labelKey: "nav.dataAnalysis", route: "/platform-operations/data-analysis", icon: "DA", sortOrder: 0, roles: ["admin", "supplier"] }
    ]
  },
  {
    key: "basicManagement",
    labelKey: "nav.basicManagement",
    icon: "BSV",
    sortOrder: 100,
    roles: ["admin", "purchaser", "supplier"],
    children: [
      { key: "supplierProducts", labelKey: "nav.supplierProducts", route: "/shop/products", icon: "SKU", sortOrder: 0, roles: ["admin", "purchaser", "supplier"] },
      { key: "impa", labelKey: "nav.impa", route: "/standard-library/impa", icon: "IM", sortOrder: 10, roles: ["admin", "purchaser"] },
      { key: "suppliers", labelKey: "nav.suppliers", route: "/suppliers", icon: "SP", sortOrder: 20, roles: ["admin", "purchaser"] },
      { key: "registrations", labelKey: "nav.registrations", route: "/admin/registrations", icon: "RG", sortOrder: 30, roles: ["admin"] },
      { key: "permissions", labelKey: "nav.permissions", route: "/admin/permissions", icon: "PM", sortOrder: 40, roles: ["admin"] },
      { key: "menuManagement", labelKey: "nav.menuManagement", route: "/admin/menus", icon: "MN", sortOrder: 50, roles: ["admin"] },
      { key: "dataDictionary", labelKey: "nav.dataDictionary", route: "/admin/dictionaries", icon: "DD", sortOrder: 60, roles: ["admin"] }
    ]
  }
];

export const metrics = [
  { labelKey: "dashboard.metrics.requestList", value: "18", noteKey: "dashboard.metrics.requestListNote", status: "info" },
  { labelKey: "dashboard.metrics.waitingQuotes", value: "42", noteKey: "dashboard.metrics.waitingQuotesNote", status: "warning" },
  { labelKey: "dashboard.metrics.pendingOrders", value: "7", noteKey: "dashboard.metrics.pendingOrdersNote", status: "success" },
  { labelKey: "dashboard.metrics.orderAmount", value: "3", noteKey: "dashboard.metrics.orderAmountNote", status: "danger" }
];

const attributesA: SkuAttribute[] = [
  { key: "spec", labelKey: "attr.spec", value: "25mm x 30m" },
  { key: "material", labelKey: "attr.material", value: "Polypropylene" },
  { key: "packing", labelKey: "attr.packing", value: "Carton" },
  { key: "carton", labelKey: "attr.carton", value: "12 rolls" },
  { key: "dimensions", labelKey: "attr.dimensions", value: "52 x 38 x 30 cm" },
  { key: "grossWeight", labelKey: "attr.grossWeight", value: "18.4 kg" },
  { key: "barcode", labelKey: "attr.barcode", value: "6971002400186" },
  { key: "stock", labelKey: "attr.stock", value: "320" }
];

const attributesB: SkuAttribute[] = [
  { key: "spec", labelKey: "attr.spec", value: "5L / bucket" },
  { key: "material", labelKey: "attr.material", value: "Synthetic oil" },
  { key: "packing", labelKey: "attr.packing", value: "4 buckets / carton" },
  { key: "dimensions", labelKey: "attr.dimensions", value: "42 x 34 x 29 cm" },
  { key: "volume", labelKey: "attr.volume", value: "0.041 m3" },
  { key: "grossWeight", labelKey: "attr.grossWeight", value: "22 kg" },
  { key: "netWeight", labelKey: "attr.netWeight", value: "20 kg" },
  { key: "stock", labelKey: "attr.stock", value: "96" }
];

const attributesC: SkuAttribute[] = [
  { key: "spec", labelKey: "attr.spec", value: "Large / SOLAS" },
  { key: "material", labelKey: "attr.material", value: "PVC coated fabric" },
  { key: "packing", labelKey: "attr.packing", value: "Single bag" },
  { key: "color", labelKey: "attr.color", value: "Orange" },
  { key: "dimensions", labelKey: "attr.dimensions", value: "62 x 48 x 8 cm" },
  { key: "barcode", labelKey: "attr.barcode", value: "SOLAS-LJ-2406" },
  { key: "stock", labelKey: "attr.stock", value: "54" }
];

export const supplierSkus: SupplierSku[] = [
  {
    id: "SKU-110101",
    thumbnail: productImage("#e6f1fb", "#0d63a8", "ROPE"),
    images: [{ src: productImage("#e6f1fb", "#0d63a8", "ROPE"), alt: "Mooring rope" }],
    name: "Mooring Rope",
    itemNo: "ZS-MR-25",
    impaCode: "110101",
    supplier: "供货商A",
    price: 128,
    currency: "CNY",
    stock: 320,
    status: "success",
    attributes: attributesA
  },
  {
    id: "SKU-150203",
    thumbnail: productImage("#eef7ff", "#1174c7", "OIL"),
    images: [{ src: productImage("#eef7ff", "#1174c7", "OIL"), alt: "Lubricating oil" }],
    name: "Lubricating Oil",
    itemNo: "ZS-LO-5L",
    impaCode: "150203",
    supplier: "供货商B",
    price: 268,
    currency: "CNY",
    stock: 96,
    status: "warning",
    attributes: attributesB
  },
  {
    id: "SKU-330501",
    thumbnail: productImage("#f0f8ff", "#0a4c82", "LIFE"),
    images: [{ src: productImage("#f0f8ff", "#0a4c82", "LIFE"), alt: "Life jacket" }],
    name: "Life Jacket",
    itemNo: "ZS-LJ-SOLAS",
    impaCode: "330501",
    supplier: "供货商C",
    price: 186,
    currency: "CNY",
    stock: 54,
    status: "success",
    attributes: attributesC
  }
];

export const suppliers = [
  { id: "SUP-001", name: "供货商A", port: "Zhoushan / Ningbo", score: "96", status: "active" },
  { id: "SUP-002", name: "供货商B", port: "Zhoushan / Shanghai", score: "92", status: "warning" },
  { id: "SUP-003", name: "供货商C", port: "Zhoushan", score: "89", status: "active" }
];

export const requestRows = [
  { requestNo: "REQ-240604-01", type: "物料", vessel: "PACIFIC TRADER", supplyTime: "2026-06-08", status: "matching", amount: "18,620", quoteReady: true },
  { requestNo: "REQ-240604-02", type: "伙食", vessel: "BLUE PORT", supplyTime: "2026-06-10", status: "inquiry", amount: "9,840", quoteReady: false },
  { requestNo: "REQ-240604-03", type: "物料", vessel: "ZHONG WAI YUN 6", supplyTime: "2026-06-12", status: "quoted", amount: "6,280", quoteReady: true },
  { requestNo: "REQ-240604-04", type: "物料", vessel: "SEA SOAR", supplyTime: "2026-06-13", status: "inquiry", amount: "12,360", quoteReady: false },
  { requestNo: "REQ-240604-05", type: "伙食", vessel: "OCEAN LINK", supplyTime: "2026-06-15", status: "ordered", amount: "7,520", quoteReady: true }
];

export const inquiries = [
  { inquiryNo: "INQ-240604-18", supplier: "供货商A", status: "quoted", validUntil: "2026-06-05 18:00" },
  { inquiryNo: "INQ-240604-19", supplier: "供货商B", status: "inquiry", validUntil: "2026-06-05 12:00" },
  { inquiryNo: "INQ-240604-20", supplier: "远洋伙食补给供应商", status: "inquiry", validUntil: "2026-06-04 20:00" }
];

export const quoteRows = [
  { code: "QTE-240604-01", subject: "PACIFIC TRADER 甲板物料报价", supplier: "供货商A", status: "quoted", validUntil: "2026-06-06 12:00" },
  { code: "QTE-240604-02", subject: "BLUE PORT 润滑油报价", supplier: "供货商B", status: "quoted", validUntil: "2026-06-06 18:00" },
  { code: "QTE-240604-03", subject: "ZHONG WAI YUN 6 电气仪表报价", supplier: "供货商C", status: "inquiry", validUntil: "2026-06-07 10:00" }
];

export const comparisonRows = [
  { code: "RFQ-240604", subject: "PACIFIC TRADER 物料比价", supplier: "3 家供货商", status: "compared", validUntil: "2026-06-07 18:00" },
  { code: "RFQ-240605", subject: "BLUE PORT 轮机备件比价", supplier: "2 家供货商", status: "quoted", validUntil: "2026-06-08 12:00" },
  { code: "RFQ-240606", subject: "ZHONG WAI YUN 6 物料比价", supplier: "4 家供货商", status: "inquiry", validUntil: "2026-06-08 18:00" }
];

export const orderRows = [
  { code: "PO-240604-01", sourceNo: "INQ-240604-18", vesselName: "PACIFIC TRADER", supplier: "供货商A", amount: "18,420", status: "active", orderDate: "2026-06-06", deliveryDate: "2026-06-09" },
  { code: "PO-240604-02", sourceNo: "INQ-240604-19", vesselName: "BLUE PORT", supplier: "供货商B", amount: "9,080", status: "warning", orderDate: "2026-06-07", deliveryDate: "2026-06-10" },
  { code: "PO-240604-03", sourceNo: "INQ-240604-20", vesselName: "ZHONG WAI YUN 6", supplier: "供货商C", amount: "19,910", status: "matching", orderDate: "2026-06-08", deliveryDate: "2026-06-11" }
];

export const foodInquiries = [
  { code: "FINQ-240604-01", subject: "PACIFIC TRADER 伙食询价", supplier: "远洋伙食补给供应商", status: "inquiry", validUntil: "2026-06-05 18:00" },
  { code: "FINQ-240604-02", subject: "BLUE PORT 干货蔬果询价", supplier: "蓝港伙食配送商", status: "quoted", validUntil: "2026-06-06 12:00" },
  { code: "FINQ-240604-03", subject: "ZHONG WAI YUN 6 冷冻品询价", supplier: "舟山冷链补给商", status: "inquiry", validUntil: "2026-06-06 18:00" }
];

export const foodQuotes = [
  { code: "FQTE-240604-01", subject: "PACIFIC TRADER 伙食报价", supplier: "远洋伙食补给供应商", status: "quoted", validUntil: "2026-06-06 12:00" },
  { code: "FQTE-240604-02", subject: "BLUE PORT 蔬果报价", supplier: "蓝港伙食配送商", status: "quoted", validUntil: "2026-06-06 18:00" },
  { code: "FQTE-240604-03", subject: "ZHONG WAI YUN 6 冷冻品报价", supplier: "舟山冷链补给商", status: "inquiry", validUntil: "2026-06-07 10:00" }
];

export const foodComparisonRows = [
  { code: "FRFQ-240604", subject: "PACIFIC TRADER 伙食比价", supplier: "3 家供货商", status: "compared", validUntil: "2026-06-07 18:00" },
  { code: "FRFQ-240605", subject: "BLUE PORT 伙食比价", supplier: "2 家供货商", status: "quoted", validUntil: "2026-06-08 12:00" },
  { code: "FRFQ-240606", subject: "ZHONG WAI YUN 6 伙食比价", supplier: "4 家供货商", status: "inquiry", validUntil: "2026-06-08 18:00" }
];

export const foodOrderRows = [
  { code: "FPO-240604-01", sourceNo: "FINQ-240604-01", vesselName: "PACIFIC TRADER", supplier: "远洋伙食补给供应商", amount: "6,840", status: "active", orderDate: "2026-06-06", deliveryDate: "2026-06-09" },
  { code: "FPO-240604-02", sourceNo: "FINQ-240604-02", vesselName: "BLUE PORT", supplier: "蓝港伙食配送商", amount: "8,260", status: "warning", orderDate: "2026-06-07", deliveryDate: "2026-06-10" },
  { code: "FPO-240604-03", sourceNo: "FINQ-240604-03", vesselName: "ZHONG WAI YUN 6", supplier: "舟山冷链补给商", amount: "5,730", status: "matching", orderDate: "2026-06-08", deliveryDate: "2026-06-11" }
];

export const settlementRows = [
  { code: "SET-240604-01", type: "material", sourceNo: "PO-240604-01", vesselName: "PACIFIC TRADER", supplier: "供货商A", amount: "18,420", status: "pending", applyDate: "2026-06-09", settlementDate: "2026-06-12" },
  { code: "SET-240604-02", type: "food", sourceNo: "FPO-240604-01", vesselName: "PACIFIC TRADER", supplier: "远洋伙食补给供应商", amount: "6,840", status: "active", applyDate: "2026-06-10", settlementDate: "2026-06-13" },
  { code: "SET-240604-03", type: "material", sourceNo: "PO-240604-02", vesselName: "BLUE PORT", supplier: "供货商B", amount: "9,080", status: "archived", applyDate: "2026-06-11", settlementDate: "2026-06-14" }
];

export const crewServiceRows = [
  { code: "CREW-240604-01", subject: "PACIFIC TRADER 换班接驳", supplier: "舟山港 crew agency", status: "active", validUntil: "2026-06-09 10:00" },
  { code: "CREW-240604-02", subject: "BLUE PORT 船员体检协助", supplier: "蓝港船员服务", status: "pending", validUntil: "2026-06-10 14:00" },
  { code: "CREW-240604-03", subject: "ZHONG WAI YUN 6 登轮手续", supplier: "海事代理服务", status: "warning", validUntil: "2026-06-11 16:00" }
];

export const ranking = [
  { supplier: "供货商A", score: 96, amount: 18420, source: "有效日价 + 正式报价" },
  { supplier: "供货商B", score: 92, amount: 19080, source: "正式报价" },
  { supplier: "供货商C", score: 89, amount: 19360, source: "有效日价" },
  { supplier: "供货商C", score: 86, amount: 19910, source: "正式报价" },
  { supplier: "东海航海用品供应商", score: 82, amount: 20420, source: "有效日价" }
];

