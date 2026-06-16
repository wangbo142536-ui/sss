import { createRouter, createWebHistory } from "vue-router";
import CompanyOnboardingView from "@/views/CompanyOnboardingView.vue";
import FoodProcurementEntry from "@/views/FoodProcurementEntry.vue";
import HomeView from "@/views/HomeView.vue";
import LoginView from "@/views/LoginView.vue";
import MaterialProcurementEntry from "@/views/MaterialProcurementEntry.vue";
import RegisterView from "@/views/RegisterView.vue";
import ServiceEntry from "@/views/ServiceEntry.vue";
import WorkbenchPage from "@/views/WorkbenchPage.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "home", component: HomeView, meta: { title: "保障最后一公里" } },
    { path: "/主页", redirect: "/" },
    { path: "/login", name: "login", component: LoginView, meta: { title: "登录" } },
    { path: "/register", name: "register", component: RegisterView, meta: { title: "注册" } },
    {
      path: "/onboarding/company-profile",
      name: "company-onboarding",
      component: CompanyOnboardingView,
      meta: { title: "企业入驻资料" }
    },
    { path: "/dashboard", name: "dashboard", component: WorkbenchPage, meta: { title: "Dashboard", pageKey: "dashboard" } },
    {
      path: "/standard-library/impa",
      name: "impa-library",
      component: WorkbenchPage,
      meta: { title: "IMPA 标准库", pageKey: "impa" }
    },
    { path: "/suppliers", name: "suppliers", component: WorkbenchPage, meta: { title: "供货商信息", pageKey: "suppliers" } },
    {
      path: "/suppliers/:supplierId/products",
      name: "supplier-products",
      component: WorkbenchPage,
      meta: { title: "企业管理", pageKey: "supplierProducts" }
    },
    {
      path: "/shop/products",
      name: "shop-products",
      component: WorkbenchPage,
      meta: { title: "企业管理", pageKey: "supplierProducts" }
    },
    {
      path: "/procurement/requests",
      name: "procurement-requests",
      component: WorkbenchPage,
      meta: { title: "物料采购入口", pageKey: "requests" }
    },
    { path: "/inquiries", name: "inquiries", component: WorkbenchPage, meta: { title: "询价管理", pageKey: "inquiries" } },
    { path: "/quotes", name: "quotes", component: WorkbenchPage, meta: { title: "报价管理", pageKey: "quotes" } },
    { path: "/comparison", name: "comparison-list", component: WorkbenchPage, meta: { title: "比价管理", pageKey: "comparisonList" } },
    {
      path: "/procurement/requests/:requestId/compare",
      name: "compare",
      component: WorkbenchPage,
      meta: { title: "比价明细", pageKey: "compare" }
    },
    { path: "/orders", name: "orders", component: WorkbenchPage, meta: { title: "采购管理", pageKey: "orders" } },
    { path: "/orders/new", name: "new-order", component: WorkbenchPage, meta: { title: "采购管理", pageKey: "orders" } },
    { path: "/orders/:orderId", name: "order-detail", component: WorkbenchPage, meta: { title: "采购管理", pageKey: "orders" } },
    { path: "/food/inquiries", name: "food-inquiries", component: WorkbenchPage, meta: { title: "伙食询价管理", pageKey: "foodInquiries" } },
    { path: "/food/quotes", name: "food-quotes", component: WorkbenchPage, meta: { title: "伙食报价管理", pageKey: "foodQuotes" } },
    { path: "/food/comparison", name: "food-comparison", component: WorkbenchPage, meta: { title: "伙食比价管理", pageKey: "foodComparisonList" } },
    { path: "/food/orders", name: "food-orders", component: WorkbenchPage, meta: { title: "伙食采购管理", pageKey: "foodOrders" } },
    {
      path: "/delivery-tasks",
      name: "delivery-tasks",
      component: WorkbenchPage,
      meta: { title: "驳船管理", pageKey: "delivery" }
    },
    {
      path: "/settlements",
      name: "settlements",
      component: WorkbenchPage,
      meta: { title: "结算管理", pageKey: "settlements" }
    },
    { path: "/crew-services", name: "crew-services", component: WorkbenchPage, meta: { title: "船员服务", pageKey: "crewServices" } },
    { path: "/admin/permissions", name: "admin-permissions", component: WorkbenchPage, meta: { title: "权限管理", pageKey: "permissions" } },
    { path: "/admin/registrations", name: "admin-registrations", component: WorkbenchPage, meta: { title: "注册审核", pageKey: "registrations" } },
    { path: "/admin/menus", name: "admin-menus", component: WorkbenchPage, meta: { title: "菜单管理", pageKey: "menuManagement" } },
    { path: "/admin/users", name: "admin-users", component: WorkbenchPage, meta: { title: "权限管理", pageKey: "permissions" } },
    { path: "/admin/roles", name: "admin-roles", component: WorkbenchPage, meta: { title: "权限管理", pageKey: "permissions" } },
    { path: "/company/members", name: "company-members", component: WorkbenchPage, meta: { title: "Company Members", pageKey: "companyMembers" } },
    {
      path: "/procurement/food",
      name: "food-procurement-entry",
      component: FoodProcurementEntry,
      meta: { title: "伙食采购入口" }
    },
    {
      path: "/procurement/materials",
      name: "material-procurement-entry",
      component: MaterialProcurementEntry,
      meta: { title: "物料采购入口" }
    },
    {
      path: "/procurement/materials/:demandId",
      name: "material-procurement-detail",
      component: MaterialProcurementEntry,
      meta: { title: "物料采购入口" }
    },
    { path: "/services", name: "service-entry", component: ServiceEntry, meta: { title: "服务入口" } }
  ]
});
