import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";
import MerchantLayout from "../layouts/MerchantLayout.vue";
import MerchantHome from "../views/merchant/MerchantHome.vue";
import MerchantShop from "../views/merchant/Shop.vue";
import MerchantRouteList from "../views/merchant/RouteList.vue";
import MerchantRouteEdit from "../views/merchant/RouteEdit.vue";
import MerchantOrderList from "../views/merchant/OrderList.vue";
import MerchantCustomerList from "../views/merchant/CustomerList.vue";
import MerchantContractList from "../views/merchant/ContractList.vue";
import MerchantComplaintList from "../views/merchant/ComplaintList.vue";
import MerchantStatistics from "../views/merchant/Statistics.vue";

const routes = [
  { path: "/login", component: () => import("../views/auth/Login.vue"), meta: { public: true } },
  { path: "/register", component: () => import("../views/auth/Register.vue"), meta: { public: true } },
  {
    path: "/",
    component: () => import("../layouts/UserLayout.vue"),
    meta: { roleTypes: [1, 2] },
    children: [
      { path: "", component: () => import("../views/user/Home.vue") },
      { path: "route/list", component: () => import("../views/user/RouteList.vue") },
      { path: "route/detail/:id", component: () => import("../views/user/RouteDetail.vue") },
      { path: "favorite", component: () => import("../views/user/Favorite.vue") },
      { path: "order/list", component: () => import("../views/user/OrderList.vue") },
      { path: "order/detail/:id", component: () => import("../views/user/OrderDetail.vue") },
      { path: "order/pay/:id", component: () => import("../views/user/OrderPay.vue") },
      { path: "contract/list", component: () => import("../views/user/ContractList.vue") },
      { path: "contract/detail/:id", component: () => import("../views/user/ContractDetail.vue") },
      { path: "complaint/list", component: () => import("../views/user/ComplaintList.vue") },
      { path: "complaint/detail/:id", component: () => import("../views/user/ComplaintDetail.vue") },
      { path: "user/profile", component: () => import("../views/user/UserProfile.vue"), meta: { roleTypes: [1, 2] } },
      { path: "user/travelers", component: () => import("../views/user/Travelers.vue"), meta: { roleTypes: [1, 2] } }
    ]
  },
  {
    path: "/merchant",
    component: MerchantLayout,
    meta: { roleTypes: [2] },
    children: [
      { path: "", component: MerchantHome },
      { path: "shop", component: MerchantShop },
      { path: "route/list", component: MerchantRouteList },
      { path: "route/edit/:id?", component: MerchantRouteEdit },
      { path: "order/list", component: MerchantOrderList },
      { path: "customer/list", component: MerchantCustomerList },
      { path: "contract/list", component: MerchantContractList },
      { path: "complaint/list", component: MerchantComplaintList },
      { path: "statistics", component: MerchantStatistics }
    ]
  },
  {
    path: "/admin",
    component: () => import("../layouts/AdminLayout.vue"),
    meta: { roleTypes: [3] },
    children: [
      { path: "", component: () => import("../views/admin/AdminHome.vue") },
      { path: "dashboard", redirect: "/admin" },
      { path: "user/list", component: () => import("../views/admin/UserList.vue") },
      { path: "merchant/list", component: () => import("../views/admin/MerchantList.vue") },
      { path: "category/list", component: () => import("../views/admin/CategoryList.vue") },
      { path: "tag/list", component: () => import("../views/admin/TagList.vue") },
      { path: "route/audit", component: () => import("../views/admin/RouteAudit.vue") },
      { path: "banner/list", component: () => import("../views/admin/BannerList.vue") },
      { path: "contract-template/list", component: () => import("../views/admin/ContractTemplateList.vue") },
      { path: "order/list", component: () => import("../views/admin/OrderList.vue") },
      { path: "pay/list", component: () => import("../views/admin/PayList.vue") },
      { path: "complaint/list", component: () => import("../views/admin/ComplaintList.vue") },
      { path: "statistics", component: () => import("../views/admin/Statistics.vue") }
    ]
  },
  { path: "/:pathMatch(.*)*", redirect: "/" }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

const asyncRouteReloadKey = "__yutu_async_route_reload__";
const asyncRouteErrorPatterns = [
  /Failed to fetch dynamically imported module/i,
  /Importing a module script failed/i,
  /error loading dynamically imported module/i,
  /Loading chunk [\d\w-]+ failed/i,
  /Unable to preload CSS/i
];

router.beforeEach(async (to, from, next) => {
  const auth = useAuthStore();
  if (to.meta.public) {
    next();
    return;
  }
  if (!auth.token) {
    next("/login");
    return;
  }
  if (!auth.user) {
    try {
      await auth.fetchMe();
    } catch (e) {
      auth.logout();
      next("/login");
      return;
    }
  }
  const roleTypes = [...to.matched].reverse().map((m) => m.meta.roleTypes).find(Boolean);
  if (roleTypes && !roleTypes.includes(auth.user?.roleType)) {
    if (auth.user?.roleType === 1) next("/");
    else if (auth.user?.roleType === 2) next("/merchant");
    else next("/admin");
    return;
  }
  next();
});

router.afterEach(() => {
  sessionStorage.removeItem(asyncRouteReloadKey);
});

router.onError((error, to) => {
  const message = String(error?.message || error || "");
  const shouldReload = asyncRouteErrorPatterns.some((pattern) => pattern.test(message));
  if (!shouldReload) {
    return;
  }

  const target = to?.fullPath || window.location.pathname + window.location.search + window.location.hash;
  if (sessionStorage.getItem(asyncRouteReloadKey) === target) {
    sessionStorage.removeItem(asyncRouteReloadKey);
    return;
  }

  sessionStorage.setItem(asyncRouteReloadKey, target);
  window.location.replace(target);
});

export default router;
