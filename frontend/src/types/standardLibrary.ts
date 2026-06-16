export interface StandardCategoryNode {
  code: string;
  nameCn: string;
  nameEn?: string;
  level: 1 | 2;
  itemCount: number;
  parentCode?: string | null;
  children?: StandardCategoryNode[];
}

export interface ImpaStandardItem {
  impaCode: string;
  categoryCode: string;
  segmentCode: string;
  nameCn: string;
  specification?: string | null;
  unit?: string | null;
}
