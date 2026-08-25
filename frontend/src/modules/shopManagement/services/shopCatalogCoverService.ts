import cleaningChemicalsCover from "../assets/catalog-covers/cleaning-chemicals.webp";
import crewWelfareCover from "../assets/catalog-covers/crew-welfare.webp";
import cuttingToolsCover from "../assets/catalog-covers/cutting-tools.webp";
import electricalEquipmentCover from "../assets/catalog-covers/electrical-equipment.webp";
import fastenersCover from "../assets/catalog-covers/fasteners.webp";
import foodGeneralCover from "../assets/catalog-covers/food-general.webp";
import fruitCover from "../assets/catalog-covers/fruit.webp";
import generalToolsCover from "../assets/catalog-covers/general-tools.webp";
import kitchenSuppliesCover from "../assets/catalog-covers/kitchen-supplies.webp";
import lifesavingFireCover from "../assets/catalog-covers/lifesaving-fire.webp";
import linenCover from "../assets/catalog-covers/linen.webp";
import marinePaintCover from "../assets/catalog-covers/marine-paint.webp";
import materialGeneralCover from "../assets/catalog-covers/material-general.webp";
import paintingToolsCover from "../assets/catalog-covers/painting-tools.webp";
import petroleumProductsCover from "../assets/catalog-covers/petroleum-products.webp";
import pipeFittingsCover from "../assets/catalog-covers/pipe-fittings.webp";
import powerToolsCover from "../assets/catalog-covers/power-tools.webp";
import riggingDeckCover from "../assets/catalog-covers/rigging-deck.webp";
import sealingSuppliesCover from "../assets/catalog-covers/sealing-supplies.webp";
import stationeryCover from "../assets/catalog-covers/stationery.webp";
import weldingEquipmentCover from "../assets/catalog-covers/welding-equipment.webp";
import type { ShopCatalogCategory, ShopCatalogProductType } from "../types/shopProductCatalog";

type CatalogCoverTarget = Pick<ShopCatalogCategory, "code" | "label">;

function normalize(value: unknown): string {
  return String(value ?? "")
    .normalize("NFKC")
    .trim()
    .toLocaleLowerCase("zh-CN")
    .replace(/[\s、，,·（）()\-_/]+/g, "");
}

const CATEGORY_NAME_COVERS = new Map<string, string>([
  ["厨房用品", kitchenSuppliesCover],
  ["船舶油漆", marinePaintCover],
  ["船员后勤娱乐用品", crewWelfareCover],
  ["电器设备", electricalEquipmentCover],
  ["风动电动工具", powerToolsCover],
  ["管件连接器", pipeFittingsCover],
  ["焊接设备", weldingEquipmentCover],
  ["接口密封用品", sealingSuppliesCover],
  ["救生救难用具消火器类", lifesavingFireCover],
  ["螺钉螺帽类", fastenersCover],
  ["切削工具", cuttingToolsCover],
  ["石油制品类", petroleumProductsCover],
  ["涂装用器具类", paintingToolsCover],
  ["文具类", stationeryCover],
  ["文具用品", stationeryCover],
  ["洗涤化学制品类", cleaningChemicalsCover],
  ["亚麻布类", linenCover],
  ["一般作业工具类", generalToolsCover],
  ["装配索具类甲板消耗品", riggingDeckCover],
  ["水果", fruitCover]
]);

const CATEGORY_CODE_COVERS = new Map<string, string>([
  ["17", kitchenSuppliesCover],
  ["25", marinePaintCover],
  ["11", crewWelfareCover],
  ["79", electricalEquipmentCover],
  ["59", powerToolsCover],
  ["35", pipeFittingsCover],
  ["85", weldingEquipmentCover],
  ["81", sealingSuppliesCover],
  ["33", lifesavingFireCover],
  ["69", fastenersCover],
  ["63", cuttingToolsCover],
  ["45", petroleumProductsCover],
  ["27", paintingToolsCover],
  ["47", stationeryCover],
  ["55", cleaningChemicalsCover],
  ["15", linenCover],
  ["61", generalToolsCover],
  ["23", riggingDeckCover],
  ["f01", fruitCover]
]);

export function resolveShopCatalogCover(category: CatalogCoverTarget, productType: ShopCatalogProductType): string {
  const byName = CATEGORY_NAME_COVERS.get(normalize(category.label));
  if (byName) return byName;

  const byCode = CATEGORY_CODE_COVERS.get(normalize(category.code));
  if (byCode) return byCode;

  return normalize(productType) === "food" ? foodGeneralCover : materialGeneralCover;
}
