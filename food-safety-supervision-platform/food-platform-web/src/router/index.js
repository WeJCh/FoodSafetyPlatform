import { createRouter, createWebHistory } from "vue-router";
import EnterpriseLayoutView from "../views/enterprise/EnterpriseLayoutView.vue";
import DashboardView from "../views/enterprise/DashboardView.vue";
import ProfileView from "../views/enterprise/ProfileView.vue";
import ProfileDetailView from "../views/enterprise/ProfileDetailView.vue";
import ProductsView from "../views/enterprise/ProductsView.vue";
import ProductCreateView from "../views/enterprise/ProductCreateView.vue";
import ProductDetailView from "../views/enterprise/ProductDetailView.vue";
import ProductEditView from "../views/enterprise/ProductEditView.vue";
import InspectionsView from "../views/enterprise/InspectionsView.vue";
import InspectionDetailView from "../views/enterprise/InspectionDetailView.vue";
import RectificationsView from "../views/enterprise/RectificationsView.vue";
import RectificationDetailView from "../views/enterprise/RectificationDetailView.vue";
import RectificationSubmitView from "../views/enterprise/RectificationSubmitView.vue";
import RectificationSuccessView from "../views/enterprise/RectificationSuccessView.vue";
import EnterpriseRegisterView from "../views/auth/EnterpriseRegisterView.vue";
import ForgotPasswordView from "../views/auth/ForgotPasswordView.vue";
import LoginView from "../views/auth/LoginView.vue";
import PublicRegisterView from "../views/auth/PublicRegisterView.vue";
import { hasEnterpriseSession, restoreResolvedSession } from "../session/authRuntime";

const authTitle = "食品安全监管平台";

const routes = [
  {
    path: "/",
    redirect: { name: "login" }
  },
  {
    path: "/login",
    name: "login",
    component: LoginView,
    meta: { title: "登录", guestOnly: true }
  },
  {
    path: "/register/public",
    name: "public-register",
    component: PublicRegisterView,
    meta: { title: "公众注册", guestOnly: true }
  },
  {
    path: "/register/enterprise",
    name: "enterprise-register",
    component: EnterpriseRegisterView,
    meta: { title: "企业注册", guestOnly: true }
  },
  {
    path: "/forgot-password",
    name: "forgot-password",
    component: ForgotPasswordView,
    meta: { title: "忘记密码", guestOnly: true }
  },
  {
    path: "/enterprise",
    component: EnterpriseLayoutView,
    meta: { requiresEnterprise: true },
    children: [
      { path: "", redirect: { name: "enterprise-dashboard" } },
      { path: "dashboard", name: "enterprise-dashboard", component: DashboardView, meta: { title: "企业工作台", subtitle: "概览备案状态、近期检查与整改进度。", eyebrow: "Sovereign Oversight" } },
      { path: "profile", name: "enterprise-profile", component: ProfileView, meta: { title: "企业备案", subtitle: "查看备案状态、证照信息和基础档案。", eyebrow: "Enterprise Filing" } },
      { path: "profile/detail", name: "enterprise-profile-detail", component: ProfileDetailView, meta: { title: "企业备案详情", subtitle: "统一查看备案基本信息、联系人和附件。", eyebrow: "Enterprise Filing" } },
      { path: "products", name: "enterprise-products", component: ProductsView, meta: { title: "产品档案", subtitle: "管理产品档案，并保持监管状态与基础信息一致。", eyebrow: "Product Ledger" } },
      { path: "products/new", name: "enterprise-product-create", component: ProductCreateView, meta: { title: "新增产品档案", subtitle: "录入新的产品基本信息与合规元数据。", eyebrow: "Product Ledger" } },
      { path: "products/:id", name: "enterprise-product-detail", component: ProductDetailView, meta: { title: "产品详情", subtitle: "查看单个产品的合规档案与审计信息。", eyebrow: "Product Ledger" } },
      { path: "products/:id/edit", name: "enterprise-product-edit", component: ProductEditView, meta: { title: "编辑产品信息", subtitle: "更新产品档案中的监管数据与合规状态。", eyebrow: "Product Ledger" } },
      { path: "inspections", name: "enterprise-inspections", component: InspectionsView, meta: { title: "检查记录", subtitle: "查看企业检查结果、问题项与复核状态。", eyebrow: "Inspection Archive" } },
      { path: "inspections/:id", name: "enterprise-inspection-detail", component: InspectionDetailView, meta: { title: "检查记录详情", subtitle: "查看单次检查结论、问题清单与关联整改任务。", eyebrow: "Inspection Archive" } },
      { path: "rectifications", name: "enterprise-rectifications", component: RectificationsView, meta: { title: "整改任务", subtitle: "跟踪整改时限、状态流转与提交节点。", eyebrow: "Rectification Tasks" } },
      { path: "rectifications/:id", name: "enterprise-rectification-detail", component: RectificationDetailView, meta: { title: "整改任务详情", subtitle: "查看整改要求、审计轨迹和最新复核意见。", eyebrow: "Rectification Tasks" } },
      { path: "rectifications/:id/submit", name: "enterprise-rectification-submit", component: RectificationSubmitView, meta: { title: "提交整改", subtitle: "提交整改说明和佐证材料，等待监管复核。", eyebrow: "Rectification Tasks" } },
      { path: "rectifications/:id/success", name: "enterprise-rectification-success", component: RectificationSuccessView, meta: { title: "提交成功", subtitle: "整改材料已提交，等待监管复核结果。", eyebrow: "Rectification Tasks" } }
    ]
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: { name: "login" }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  const session = await restoreResolvedSession();

  if (to.meta.requiresEnterprise) {
    if (!session || !hasEnterpriseSession(session)) {
      return { name: "login", query: { redirect: to.fullPath } };
    }
  }

  if (to.meta.guestOnly && session && hasEnterpriseSession(session)) {
    return { name: "enterprise-dashboard" };
  }

  return true;
});

router.afterEach((to) => {
  const pageTitle = to.meta?.title ? `${to.meta.title} | ${authTitle}` : authTitle;
  document.title = pageTitle;
});

export default router;