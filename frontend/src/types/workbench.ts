export type UserRole = "admin" | "purchaser" | "supplier" | "operator" | "finance";

export type WorkbenchMenuItem = {
  key: string;
  label?: string;
  labelKey: string;
  route?: string;
  icon: string;
  sortOrder?: number;
  roles: UserRole[];
  children?: WorkbenchMenuItem[];
};

export type StatusVariant = "neutral" | "info" | "success" | "warning" | "danger";

export type SkuAttribute = {
  key: string;
  labelKey: string;
  value: string;
};

export type SkuImage = {
  src: string;
  alt: string;
};

export type SupplierSku = {
  id: string;
  thumbnail: string;
  images: SkuImage[];
  name: string;
  itemNo: string;
  impaCode: string;
  supplier: string;
  price: number;
  currency: string;
  stock: number;
  status: StatusVariant;
  attributes: SkuAttribute[];
};

export type TableColumn = {
  key: string;
  label: string;
  width?: string;
  align?: "left" | "center" | "right";
};
