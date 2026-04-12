import { createRouter, createWebHistory } from "vue-router";
import {
  getResolvedSession,
  hasAdminSession,
  hasEnterpriseSession,
  hasPublicSession,
  hasRegulatorAdminSession,
  hasRegulatorEnforcerSession,
  hasRegulatorSession,
  restoreResolvedSession
} from "../session/authRuntime";
import { getStoredSession } from "../session/authSession";

const APP_TITLE = "Food Safety Platform";

const AdminView = () => import("../views/AdminView.vue");
const EnterpriseDetailView = () => import("../views/EnterpriseDetailView.vue");
const EnterpriseRegisterView = () => import("../views/EnterpriseRegisterView.vue");
const EnterpriseProfileView = () => import("../views/EnterpriseProfileView.vue");
const LoginView = () => import("../views/LoginView.vue");
const PublicBulletinDetailView = () => import("../views/PublicBulletinDetailView.vue");
const PublicBulletinListView = () => import("../views/PublicBulletinListView.vue");
const PublicComplaintTrackView = () => import("../views/PublicComplaintTrackView.vue");
const PublicComplaintView = () => import("../views/PublicComplaintView.vue");
const PublicEnterpriseDetailView = () => import("../views/PublicEnterpriseDetailView.vue");
const PublicEnterpriseListView = () => import("../views/PublicEnterpriseListView.vue");
const PublicHomeView = () => import("../views/PublicHomeView.vue");
const PublicRegisterView = () => import("../views/PublicRegisterView.vue");
const PublicSamplingResultDetailView = () => import("../views/PublicSamplingResultDetailView.vue");
const PublicSamplingResultListView = () => import("../views/PublicSamplingResultListView.vue");
const RegulatorAdminComplaintDetailView = () => import("../views/RegulatorAdminComplaintDetailView.vue");
const RegulatorAdminView = () => import("../views/RegulatorAdminView.vue");
const RegulatorEnforcerComplaintDetailView = () => import("../views/RegulatorEnforcerComplaintDetailView.vue");
const RegulatorEnforcerView = () => import("../views/RegulatorEnforcerView.vue");

function getSessionSnapshot(sessionOverride = null) {
  return sessionOverride || getResolvedSession() || getStoredSession() || {
    token: "",
    userId: null,
    username: "",
    userType: "",
    roleType: "",
    roles: []
  };
}

export function getDefaultRouteLocation(sessionOverride = null) {
  const session = getSessionSnapshot(sessionOverride);

  if (hasAdminSession(session)) {
    return { name: "admin-regulator-create" };
  }

  if (hasEnterpriseSession(session)) {
    return { name: "enterprise-profile" };
  }

  if (hasPublicSession(session)) {
    return { name: "public-home" };
  }

  if (hasRegulatorAdminSession(session)) {
    return { name: "regulator-admin-enterprises" };
  }

  if (hasRegulatorEnforcerSession(session)) {
    return { name: "regulator-enforcer-enterprises" };
  }

  if (hasRegulatorSession(session)) {
    return { name: "regulator-enforcer-enterprises" };
  }

  return { name: "login" };
}

function canAccessRole(session, requiredRole) {
  switch (requiredRole) {
    case "ADMIN":
      return hasAdminSession(session);
    case "PUBLIC":
      return hasPublicSession(session);
    case "ENTERPRISE":
      return hasEnterpriseSession(session);
    case "REGULATOR_ADMIN":
      return hasRegulatorAdminSession(session);
    case "REGULATOR_ENFORCER":
      return hasRegulatorEnforcerSession(session);
    default:
      return true;
  }
}

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
    path: "/public",
    name: "public-home",
    component: PublicHomeView,
    meta: { title: "公众首页", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/bulletins",
    name: "public-bulletins",
    component: PublicBulletinListView,
    meta: { title: "监管公告", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/bulletins/:bulletinId",
    name: "public-bulletin-detail",
    component: PublicBulletinDetailView,
    meta: { title: "公告详情", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/enterprises",
    name: "public-enterprises",
    component: PublicEnterpriseListView,
    meta: { title: "企业公示", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/enterprises/:enterpriseId",
    name: "public-enterprise-detail",
    component: PublicEnterpriseDetailView,
    meta: { title: "企业详情", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/sampling-results",
    name: "public-sampling-results",
    component: PublicSamplingResultListView,
    meta: { title: "抽检结果", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/sampling-results/:samplingResultId",
    name: "public-sampling-result-detail",
    component: PublicSamplingResultDetailView,
    meta: { title: "抽检结果详情", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/complaints/new",
    name: "public-complaint-create",
    component: PublicComplaintView,
    meta: { title: "我要投诉", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/complaints",
    name: "public-complaints",
    component: PublicComplaintTrackView,
    meta: { title: "我的投诉", requiresRole: "PUBLIC" }
  },
  {
    path: "/admin/regulators/create",
    name: "admin-regulator-create",
    component: AdminView,
    meta: { title: "添加监管人员", requiresRole: "ADMIN", initialSection: "create" }
  },
  {
    path: "/admin/regulators/list",
    name: "admin-regulator-list",
    component: AdminView,
    meta: { title: "监管人员列表", requiresRole: "ADMIN", initialSection: "list" }
  },
  {
    path: "/enterprise/profile",
    name: "enterprise-profile",
    component: EnterpriseProfileView,
    meta: { title: "企业备案", requiresRole: "ENTERPRISE", initialSection: "profile" }
  },
  {
    path: "/enterprise/products",
    name: "enterprise-products",
    component: EnterpriseProfileView,
    meta: { title: "产品档案", requiresRole: "ENTERPRISE", initialSection: "products" }
  },
  {
    path: "/enterprise/inspections",
    name: "enterprise-inspections",
    component: EnterpriseProfileView,
    meta: { title: "检查记录", requiresRole: "ENTERPRISE", initialSection: "inspections" }
  },
  {
    path: "/enterprise/rectifications",
    name: "enterprise-rectifications",
    component: EnterpriseProfileView,
    meta: { title: "整改任务", requiresRole: "ENTERPRISE", initialSection: "rectification" }
  },
  {
    path: "/regulator/admin/enterprises",
    name: "regulator-admin-enterprises",
    component: RegulatorAdminView,
    meta: { title: "企业管理", requiresRole: "REGULATOR_ADMIN", initialSection: "enterprises" }
  },
  {
    path: "/regulator/admin/approvals",
    name: "regulator-admin-approvals",
    component: RegulatorAdminView,
    meta: { title: "备案审核", requiresRole: "REGULATOR_ADMIN", initialSection: "approvals" }
  },
  {
    path: "/regulator/admin/dispatch",
    name: "regulator-admin-dispatch",
    component: RegulatorAdminView,
    meta: { title: "任务派发", requiresRole: "REGULATOR_ADMIN", initialSection: "dispatch" }
  },
  {
    path: "/regulator/admin/sampling",
    name: "regulator-admin-sampling",
    component: RegulatorAdminView,
    meta: { title: "抽检任务", requiresRole: "REGULATOR_ADMIN", initialSection: "sampling" }
  },
  {
    path: "/regulator/admin/inspections",
    name: "regulator-admin-inspections",
    component: RegulatorAdminView,
    meta: { title: "检查记录", requiresRole: "REGULATOR_ADMIN", initialSection: "inspections" }
  },
  {
    path: "/regulator/admin/complaints",
    name: "regulator-admin-complaints",
    component: RegulatorAdminView,
    meta: { title: "投诉流转", requiresRole: "REGULATOR_ADMIN", initialSection: "complaints" }
  },
  {
    path: "/regulator/admin/complaints/:complaintId",
    name: "regulator-admin-complaint-detail",
    component: RegulatorAdminComplaintDetailView,
    meta: { title: "投诉详情", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/rectifications",
    name: "regulator-admin-rectifications",
    component: RegulatorAdminView,
    meta: { title: "整改复核", requiresRole: "REGULATOR_ADMIN", initialSection: "rectification" }
  },
  {
    path: "/regulator/admin/warnings",
    name: "regulator-admin-warnings",
    component: RegulatorAdminView,
    meta: { title: "风险预警", requiresRole: "REGULATOR_ADMIN", initialSection: "warnings" }
  },
  {
    path: "/regulator/admin/bulletins",
    name: "regulator-admin-bulletins",
    component: RegulatorAdminView,
    meta: { title: "公告发布", requiresRole: "REGULATOR_ADMIN", initialSection: "bulletins" }
  },
  {
    path: "/regulator/admin/stats",
    name: "regulator-admin-stats",
    component: RegulatorAdminView,
    meta: { title: "数据统计", requiresRole: "REGULATOR_ADMIN", initialSection: "stats" }
  },
  {
    path: "/regulator/admin/enterprises/:enterpriseId",
    name: "regulator-admin-enterprise-detail",
    component: EnterpriseDetailView,
    meta: { title: "企业详情", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/enforcer/enterprises",
    name: "regulator-enforcer-enterprises",
    component: RegulatorEnforcerView,
    meta: { title: "企业监管", requiresRole: "REGULATOR_ENFORCER", initialSection: "enterprises" }
  },
  {
    path: "/regulator/enforcer/tasks",
    name: "regulator-enforcer-tasks",
    component: RegulatorEnforcerView,
    meta: { title: "我的任务", requiresRole: "REGULATOR_ENFORCER", initialSection: "tasks" }
  },
  {
    path: "/regulator/enforcer/sampling",
    name: "regulator-enforcer-sampling",
    component: RegulatorEnforcerView,
    meta: { title: "抽检任务", requiresRole: "REGULATOR_ENFORCER", initialSection: "sampling" }
  },
  {
    path: "/regulator/enforcer/inspections",
    name: "regulator-enforcer-inspections",
    component: RegulatorEnforcerView,
    meta: { title: "检查记录", requiresRole: "REGULATOR_ENFORCER", initialSection: "inspections" }
  },
  {
    path: "/regulator/enforcer/complaints",
    name: "regulator-enforcer-complaints",
    component: RegulatorEnforcerView,
    meta: { title: "投诉处理", requiresRole: "REGULATOR_ENFORCER", initialSection: "complaints" }
  },
  {
    path: "/regulator/enforcer/complaints/:complaintId",
    name: "regulator-enforcer-complaint-detail",
    component: RegulatorEnforcerComplaintDetailView,
    meta: { title: "投诉详情", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/rectifications",
    name: "regulator-enforcer-rectifications",
    component: RegulatorEnforcerView,
    meta: { title: "整改跟进", requiresRole: "REGULATOR_ENFORCER", initialSection: "rectification" }
  },
  {
    path: "/regulator/enforcer/warnings",
    name: "regulator-enforcer-warnings",
    component: RegulatorEnforcerView,
    meta: { title: "风险预警", requiresRole: "REGULATOR_ENFORCER", initialSection: "warnings" }
  },
  {
    path: "/regulator/enforcer/stats",
    name: "regulator-enforcer-stats",
    component: RegulatorEnforcerView,
    meta: { title: "数据统计", requiresRole: "REGULATOR_ENFORCER", initialSection: "stats" }
  },
  {
    path: "/regulator/enforcer/enterprises/:enterpriseId",
    name: "regulator-enforcer-enterprise-detail",
    component: EnterpriseDetailView,
    meta: { title: "企业详情", requiresRole: "REGULATOR_ENFORCER" }
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

  if (to.meta?.guestOnly && session?.token) {
    return getDefaultRouteLocation(session);
  }

  if (to.meta?.requiresRole) {
    if (!session?.token) {
      return {
        name: "login",
        query: { redirect: to.fullPath }
      };
    }

    if (!canAccessRole(session, to.meta.requiresRole)) {
      return getDefaultRouteLocation(session);
    }
  }

  return true;
});

router.afterEach((to) => {
  const pageTitle = to.meta?.title ? `${to.meta.title} | ${APP_TITLE}` : APP_TITLE;
  document.title = pageTitle;
});

export { routes };
export default router;
