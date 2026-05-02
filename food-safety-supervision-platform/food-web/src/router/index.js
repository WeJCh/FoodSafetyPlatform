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

const SystemAdminDashboardView = () => import("../views/systemAdmin/SystemAdminDashboardView.vue");
const SystemAdminRegulatorListView = () => import("../views/systemAdmin/SystemAdminRegulatorListView.vue");
const SystemAdminRegulatorCreateView = () => import("../views/systemAdmin/SystemAdminRegulatorCreateView.vue");
const SystemAdminRegulatorCreateConfirmView = () => import("../views/systemAdmin/SystemAdminRegulatorCreateConfirmView.vue");
const SystemAdminRegulatorDetailView = () => import("../views/systemAdmin/SystemAdminRegulatorDetailView.vue");
const SystemAdminRegulatorEditView = () => import("../views/systemAdmin/SystemAdminRegulatorEditView.vue");
const SystemAdminRegulatorRegionAdjustView = () => import("../views/systemAdmin/SystemAdminRegulatorRegionAdjustView.vue");
const SystemAdminRegulatorStatusConfirmView = () => import("../views/systemAdmin/SystemAdminRegulatorStatusConfirmView.vue");
const EnterpriseRegisterView = () => import("../views/EnterpriseRegisterView.vue");
const EnterpriseDashboardView = () => import("../views/enterprise/EnterpriseDashboardView.vue");
const EnterpriseInspectionDetailView = () => import("../views/enterprise/EnterpriseInspectionDetailView.vue");
const EnterpriseInspectionsView = () => import("../views/enterprise/EnterpriseInspectionsView.vue");
const EnterpriseProfileDetailView = () => import("../views/enterprise/EnterpriseProfileDetailView.vue");
const EnterpriseProductDetailView = () => import("../views/enterprise/EnterpriseProductDetailView.vue");
const EnterpriseProductFormView = () => import("../views/enterprise/EnterpriseProductFormView.vue");
const EnterpriseProductsView = () => import("../views/enterprise/EnterpriseProductsView.vue");
const EnterpriseProfileOverviewView = () => import("../views/enterprise/EnterpriseProfileOverviewView.vue");
const EnterpriseRectificationDetailView = () => import("../views/enterprise/EnterpriseRectificationDetailView.vue");
const EnterpriseRectificationSubmitSuccessView = () => import("../views/enterprise/EnterpriseRectificationSubmitSuccessView.vue");
const EnterpriseRectificationSubmitView = () => import("../views/enterprise/EnterpriseRectificationSubmitView.vue");
const EnterpriseRectificationsView = () => import("../views/enterprise/EnterpriseRectificationsView.vue");
const LoginView = () => import("../views/LoginView.vue");
const PublicBulletinDetailView = () => import("../views/public/PublicBulletinDetailView.vue");
const PublicBulletinListView = () => import("../views/public/PublicBulletinListView.vue");
const PublicComplaintTrackView = () => import("../views/public/PublicComplaintTrackView.vue");
const PublicComplaintView = () => import("../views/public/PublicComplaintView.vue");
const PublicComplaintDetailView = () => import("../views/public/PublicComplaintDetailView.vue");
const PublicComplaintSubmitSuccessView = () => import("../views/public/PublicComplaintSubmitSuccessView.vue");
const PublicEnterpriseDetailView = () => import("../views/public/PublicEnterpriseDetailView.vue");
const PublicEnterpriseListView = () => import("../views/public/PublicEnterpriseListView.vue");
const PublicHomeView = () => import("../views/public/PublicHomeView.vue");
const PublicRegisterView = () => import("../views/PublicRegisterView.vue");
const PublicSamplingResultDetailView = () => import("../views/public/PublicSamplingResultDetailView.vue");
const PublicSamplingResultListView = () => import("../views/public/PublicSamplingResultListView.vue");
const RegulatorAdminComplaintDetailView = () =>
  import("../views/regulatorAdmin/RegulatorAdminComplaintDetailView.vue");
const RegulatorAdminApprovalsView = () => import("../views/regulatorAdmin/RegulatorAdminApprovalsView.vue");
const RegulatorAdminDispatchCreateView = () => import("../views/regulatorAdmin/RegulatorAdminDispatchCreateView.vue");
const RegulatorAdminDispatchRecordsView = () => import("../views/regulatorAdmin/RegulatorAdminDispatchRecordsView.vue");
const RegulatorAdminEnterpriseDetailView = () => import("../views/regulatorAdmin/RegulatorAdminEnterpriseDetailView.vue");
const RegulatorAdminDispatchTasksView = () => import("../views/regulatorAdmin/RegulatorAdminDispatchTasksView.vue");
const RegulatorAdminInspectionTaskDetailView = () =>
  import("../views/regulatorAdmin/RegulatorAdminInspectionTaskDetailView.vue");
const RegulatorAdminComplaintFlowView = () =>
  import("../views/regulatorAdmin/RegulatorAdminComplaintFlowView.vue");
const RegulatorAdminRectificationReviewView = () =>
  import("../views/regulatorAdmin/RegulatorAdminRectificationReviewView.vue");
const RegulatorAdminRectificationDetailView = () =>
  import("../views/regulatorAdmin/RegulatorAdminRectificationDetailView.vue");
const RegulatorAdminBulletinManagementView = () =>
  import("../views/regulatorAdmin/RegulatorAdminBulletinManagementView.vue");
const RegulatorAdminBulletinEditView = () =>
  import("../views/regulatorAdmin/RegulatorAdminBulletinEditView.vue");
const RegulatorAdminEnterpriseListView = () => import("../views/regulatorAdmin/RegulatorAdminEnterpriseListView.vue");
const RegulatorAdminOverviewView = () => import("../views/regulatorAdmin/RegulatorAdminOverviewView.vue");
const RegulatorAdminSamplingCreateView = () => import("../views/regulatorAdmin/RegulatorAdminSamplingCreateView.vue");
const RegulatorAdminSamplingTaskDetailView = () =>
  import("../views/regulatorAdmin/RegulatorAdminSamplingTaskDetailView.vue");
const RegulatorAdminSamplingTasksView = () => import("../views/regulatorAdmin/RegulatorAdminSamplingTasksView.vue");
const RegulatorAdminWarningCenterView = () => import("../views/regulatorAdmin/RegulatorAdminWarningCenterView.vue");
const RegulatorAdminStatisticsView = () => import("../views/regulatorAdmin/RegulatorAdminStatisticsView.vue");
const RegulatorEnforcerEnterpriseListView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerEnterpriseListView.vue");
const RegulatorEnforcerTaskListView = () => import("../views/regulatorEnforcer/RegulatorEnforcerTaskListView.vue");
const RegulatorEnforcerTaskDetailView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerTaskDetailView.vue");
const RegulatorEnforcerInspectionSubmitView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerInspectionSubmitView.vue");
const RegulatorEnforcerSamplingTaskView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerSamplingTaskView.vue");
const RegulatorEnforcerSamplingTaskDetailView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerSamplingTaskDetailView.vue");
const RegulatorEnforcerSamplingResultSubmitView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerSamplingResultSubmitView.vue");
const RegulatorEnforcerInspectionRecordView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerInspectionRecordView.vue");
const RegulatorEnforcerInspectionRecordDetailView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerInspectionRecordDetailView.vue");
const RegulatorEnforcerComplaintListView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerComplaintListView.vue");
const RegulatorEnforcerComplaintDetailView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerComplaintDetailView.vue");
const RegulatorEnforcerRectificationView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerRectificationView.vue");
const RegulatorEnforcerRectificationDetailView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerRectificationDetailView.vue");
const RegulatorEnforcerWarningView = () => import("../views/regulatorEnforcer/RegulatorEnforcerWarningView.vue");
const RegulatorEnforcerWarningDetailView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerWarningDetailView.vue");
const RegulatorEnforcerStatsView = () => import("../views/regulatorEnforcer/RegulatorEnforcerStatsView.vue");
const RegulatorEnforcerEnterpriseDetailView = () =>
  import("../views/regulatorEnforcer/RegulatorEnforcerEnterpriseDetailView.vue");

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
    return { name: "admin-dashboard" };
  }

  if (hasEnterpriseSession(session)) {
    return { name: "enterprise-dashboard" };
  }

  if (hasPublicSession(session)) {
    return { name: "public-home" };
  }

  if (hasRegulatorAdminSession(session)) {
    return { name: "regulator-admin-overview" };
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
    path: "/public/complaints/success",
    name: "public-complaint-submit-success",
    component: PublicComplaintSubmitSuccessView,
    meta: { title: "投诉提交成功", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/complaints",
    name: "public-complaints",
    component: PublicComplaintTrackView,
    meta: { title: "我的投诉", requiresRole: "PUBLIC" }
  },
  {
    path: "/public/complaints/:complaintId",
    name: "public-complaint-detail",
    component: PublicComplaintDetailView,
    meta: { title: "投诉详情", requiresRole: "PUBLIC" }
  },
  {
    path: "/admin/dashboard",
    name: "admin-dashboard",
    component: SystemAdminDashboardView,
    meta: { title: "系统管理员工作台", requiresRole: "ADMIN" }
  },
  {
    path: "/admin/regulators/create",
    name: "admin-regulator-create",
    component: SystemAdminRegulatorCreateView,
    meta: { title: "添加监管人员", requiresRole: "ADMIN" }
  },
  {
    path: "/admin/regulators/create/confirm",
    name: "admin-regulator-create-confirm",
    component: SystemAdminRegulatorCreateConfirmView,
    meta: { title: "核对人员信息", requiresRole: "ADMIN" }
  },
  {
    path: "/admin/regulators/list",
    name: "admin-regulator-list",
    component: SystemAdminRegulatorListView,
    meta: { title: "监管人员列表", requiresRole: "ADMIN" }
  },
  {
    path: "/admin/regulators/:userId",
    name: "admin-regulator-detail",
    component: SystemAdminRegulatorDetailView,
    meta: { title: "监管人员详情", requiresRole: "ADMIN" }
  },
  {
    path: "/admin/regulators/:userId/edit",
    name: "admin-regulator-edit",
    component: SystemAdminRegulatorEditView,
    meta: { title: "编辑监管人员", requiresRole: "ADMIN" }
  },
  {
    path: "/admin/regulators/:userId/region-adjust",
    name: "admin-regulator-region-adjust",
    component: SystemAdminRegulatorRegionAdjustView,
    meta: { title: "调整监管辖区", requiresRole: "ADMIN" }
  },
  {
    path: "/admin/regulators/:userId/status-confirm",
    name: "admin-regulator-status-confirm",
    component: SystemAdminRegulatorStatusConfirmView,
    meta: { title: "状态切换确认", requiresRole: "ADMIN" }
  },
  {
    path: "/enterprise/dashboard",
    name: "enterprise-dashboard",
    component: EnterpriseDashboardView,
    meta: { title: "企业工作台", requiresRole: "ENTERPRISE" }
  },
  {
    path: "/enterprise/profile",
    name: "enterprise-profile",
    component: EnterpriseProfileOverviewView,
    meta: { title: "企业备案", requiresRole: "ENTERPRISE", initialSection: "profile" }
  },
  {
    path: "/enterprise/profile/detail",
    name: "enterprise-profile-detail",
    component: EnterpriseProfileDetailView,
    meta: { title: "备案详情", requiresRole: "ENTERPRISE" }
  },
  {
    path: "/enterprise/products",
    name: "enterprise-products",
    component: EnterpriseProductsView,
    meta: { title: "产品档案", requiresRole: "ENTERPRISE", initialSection: "products" }
  },
  {
    path: "/enterprise/products/new",
    name: "enterprise-product-create",
    component: EnterpriseProductFormView,
    meta: { title: "新增产品", requiresRole: "ENTERPRISE" }
  },
  {
    path: "/enterprise/products/:productId/edit",
    name: "enterprise-product-edit",
    component: EnterpriseProductFormView,
    meta: { title: "编辑产品", requiresRole: "ENTERPRISE" }
  },
  {
    path: "/enterprise/products/:productId",
    name: "enterprise-product-detail",
    component: EnterpriseProductDetailView,
    meta: { title: "产品详情", requiresRole: "ENTERPRISE" }
  },
  {
    path: "/enterprise/inspections",
    name: "enterprise-inspections",
    component: EnterpriseInspectionsView,
    meta: { title: "检查记录", requiresRole: "ENTERPRISE", initialSection: "inspections" }
  },
  {
    path: "/enterprise/inspections/:inspectionId",
    name: "enterprise-inspection-detail",
    component: EnterpriseInspectionDetailView,
    meta: { title: "检查记录详情", requiresRole: "ENTERPRISE" }
  },
  {
    path: "/enterprise/rectifications",
    name: "enterprise-rectifications",
    component: EnterpriseRectificationsView,
    meta: { title: "整改任务", requiresRole: "ENTERPRISE", initialSection: "rectification" }
  },
  {
    path: "/enterprise/rectifications/:rectificationId",
    name: "enterprise-rectification-detail",
    component: EnterpriseRectificationDetailView,
    meta: { title: "整改任务详情", requiresRole: "ENTERPRISE" }
  },
  {
    path: "/enterprise/rectifications/:rectificationId/submit",
    name: "enterprise-rectification-submit",
    component: EnterpriseRectificationSubmitView,
    meta: { title: "提交整改", requiresRole: "ENTERPRISE" }
  },
  {
    path: "/enterprise/rectifications/:rectificationId/success",
    name: "enterprise-rectification-submit-success",
    component: EnterpriseRectificationSubmitSuccessView,
    meta: { title: "提交成功", requiresRole: "ENTERPRISE" }
  },
  {
    path: "/regulator/admin/overview",
    name: "regulator-admin-overview",
    component: RegulatorAdminOverviewView,
    meta: { title: "工作台", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/enterprises",
    name: "regulator-admin-enterprises",
    component: RegulatorAdminEnterpriseListView,
    meta: { title: "企业管理", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/approvals",
    name: "regulator-admin-approvals",
    component: RegulatorAdminApprovalsView,
    meta: { title: "备案审核", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/dispatch",
    name: "regulator-admin-dispatch",
    component: RegulatorAdminDispatchTasksView,
    meta: { title: "任务派发", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/dispatch/tasks/:taskId",
    name: "regulator-admin-inspection-task-detail",
    component: RegulatorAdminInspectionTaskDetailView,
    meta: { title: "检查任务详情", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/dispatch/create",
    name: "regulator-admin-dispatch-create",
    component: RegulatorAdminDispatchCreateView,
    meta: { title: "发起检查任务", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/sampling",
    name: "regulator-admin-sampling",
    component: RegulatorAdminSamplingTasksView,
    meta: { title: "抽检任务", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/sampling/create",
    name: "regulator-admin-sampling-create",
    component: RegulatorAdminSamplingCreateView,
    meta: { title: "新建抽检任务", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/sampling/tasks/:taskId",
    name: "regulator-admin-sampling-detail",
    component: RegulatorAdminSamplingTaskDetailView,
    meta: { title: "抽检结果详情", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/inspections",
    name: "regulator-admin-inspections",
    redirect: { name: "regulator-admin-dispatch-records" },
    meta: { title: "检查记录", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/dispatch/records",
    name: "regulator-admin-dispatch-records",
    component: RegulatorAdminDispatchRecordsView,
    meta: { title: "检查记录", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/complaints",
    name: "regulator-admin-complaints",
    component: RegulatorAdminComplaintFlowView,
    meta: { title: "投诉流转", requiresRole: "REGULATOR_ADMIN" }
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
    component: RegulatorAdminRectificationReviewView,
    meta: { title: "整改复核", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/rectifications/:rectificationId",
    name: "regulator-admin-rectification-detail",
    component: RegulatorAdminRectificationDetailView,
    meta: { title: "整改复核详情", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/warnings",
    name: "regulator-admin-warnings",
    component: RegulatorAdminWarningCenterView,
    meta: { title: "风险预警", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/bulletins",
    name: "regulator-admin-bulletins",
    component: RegulatorAdminBulletinManagementView,
    meta: { title: "公告管理", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/bulletins/create",
    name: "regulator-admin-bulletin-create",
    component: RegulatorAdminBulletinEditView,
    meta: { title: "新建公告", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/bulletins/:bulletinId/edit",
    name: "regulator-admin-bulletin-edit",
    component: RegulatorAdminBulletinEditView,
    meta: { title: "编辑公告", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/stats",
    name: "regulator-admin-stats",
    component: RegulatorAdminStatisticsView,
    meta: { title: "数据统计", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/admin/enterprises/:enterpriseId",
    name: "regulator-admin-enterprise-detail",
    component: RegulatorAdminEnterpriseDetailView,
    meta: { title: "企业详情", requiresRole: "REGULATOR_ADMIN" }
  },
  {
    path: "/regulator/enforcer/enterprises",
    name: "regulator-enforcer-enterprises",
    component: RegulatorEnforcerEnterpriseListView,
    meta: { title: "企业监管", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/tasks",
    name: "regulator-enforcer-tasks",
    component: RegulatorEnforcerTaskListView,
    meta: { title: "我的任务", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/tasks/:taskId",
    name: "regulator-enforcer-task-detail",
    component: RegulatorEnforcerTaskDetailView,
    meta: { title: "检查任务详情", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/tasks/:taskId/submit",
    name: "regulator-enforcer-task-submit",
    component: RegulatorEnforcerInspectionSubmitView,
    meta: { title: "检查结果提交", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/sampling",
    name: "regulator-enforcer-sampling",
    component: RegulatorEnforcerSamplingTaskView,
    meta: { title: "抽检任务", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/sampling/:taskId",
    name: "regulator-enforcer-sampling-detail",
    component: RegulatorEnforcerSamplingTaskDetailView,
    meta: { title: "抽检任务详情", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/sampling/:taskId/submit",
    name: "regulator-enforcer-sampling-submit",
    component: RegulatorEnforcerSamplingResultSubmitView,
    meta: { title: "抽检结果提交", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/inspections",
    name: "regulator-enforcer-inspections",
    component: RegulatorEnforcerInspectionRecordView,
    meta: { title: "检查记录", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/inspections/:inspectionId",
    name: "regulator-enforcer-inspection-detail",
    component: RegulatorEnforcerInspectionRecordDetailView,
    meta: { title: "检查记录详情", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/complaints",
    name: "regulator-enforcer-complaints",
    component: RegulatorEnforcerComplaintListView,
    meta: { title: "投诉处理", requiresRole: "REGULATOR_ENFORCER" }
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
    component: RegulatorEnforcerRectificationView,
    meta: { title: "整改跟进", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/rectifications/:rectificationId",
    name: "regulator-enforcer-rectification-detail",
    component: RegulatorEnforcerRectificationDetailView,
    meta: { title: "整改详情", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/warnings",
    name: "regulator-enforcer-warnings",
    component: RegulatorEnforcerWarningView,
    meta: { title: "风险预警", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/warnings/:warningId",
    name: "regulator-enforcer-warning-detail",
    component: RegulatorEnforcerWarningDetailView,
    meta: { title: "预警详情", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/stats",
    name: "regulator-enforcer-stats",
    component: RegulatorEnforcerStatsView,
    meta: { title: "数据统计", requiresRole: "REGULATOR_ENFORCER" }
  },
  {
    path: "/regulator/enforcer/enterprises/:enterpriseId",
    name: "regulator-enforcer-enterprise-detail",
    component: RegulatorEnforcerEnterpriseDetailView,
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
