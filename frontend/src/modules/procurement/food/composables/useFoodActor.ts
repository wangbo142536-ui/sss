import { computed } from "vue";
import { useRoute } from "vue-router";
import { getAuthSession } from "@/services/authService";

export type FoodActor = "BUYER" | "SUPPLIER";

const buyerRoutes = ["/food/orders", "/food/settlements", "/food/evaluations", "/food/comparison"];

export function resolveFoodActor(path: string, fallbackSupplier: boolean): FoodActor {
  if (path.startsWith("/supplier/food/")) return "SUPPLIER";
  if (buyerRoutes.some((route) => path === route || path.startsWith(`${route}/`))) return "BUYER";
  return fallbackSupplier ? "SUPPLIER" : "BUYER";
}

export function useFoodActor() {
  const route = useRoute();
  const session = getAuthSession();
  const companyType = String(session?.company?.companyType || session?.company?.type || "").toUpperCase();
  const roles = (session?.roles || []).map((role) => role.toUpperCase());
  const fallbackSupplier = companyType.includes("SUPPLIER") || roles.some((role) => role.includes("SUPPLIER"));
  const isSupplier = computed(() => resolveFoodActor(route.path, fallbackSupplier) === "SUPPLIER");
  return { isSupplier };
}
