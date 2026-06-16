import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_impa_items import build_import_plan


class ImpaImportPlanTest(unittest.TestCase):
    def test_builds_standard_item_and_i18n_rows_without_touching_mapping_tables(self):
        workbook_path = Path("tmp/provision_analysis/IMPA.xlsx")

        plan = build_import_plan(workbook_path)

        self.assertEqual(29231, len(plan.items))
        self.assertEqual(58459, len(plan.i18n_rows))
        self.assertEqual(3, len(plan.errors))
        self.assertEqual({"impa_item", "impa_item_i18n", "data_import_batch", "data_import_error"}, plan.target_tables)
        self.assertTrue(all(row.category_code for row in plan.i18n_rows))
        self.assertTrue(all(row.impa_code.startswith(row.category_code) for row in plan.i18n_rows))
        self.assertNotIn("impa_category", plan.target_tables)
        self.assertNotIn("supplier_sku", plan.target_tables)
        self.assertNotIn("demand_item", plan.target_tables)

    def test_keeps_known_english_rows_with_blank_description_as_errors(self):
        workbook_path = Path("tmp/provision_analysis/IMPA.xlsx")

        plan = build_import_plan(workbook_path)

        self.assertEqual(
            {"2322A4", "233506", "651088"},
            {error.business_key for error in plan.errors},
        )
        self.assertTrue(all(error.error_code == "MISSING_DESCRIPTION" for error in plan.errors))


if __name__ == "__main__":
    unittest.main()
