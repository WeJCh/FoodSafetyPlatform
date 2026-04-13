import { computed } from "vue";
import { useRouter } from "vue-router";
import { getActiveSession, performLogout } from "../../session/authRuntime";

export function enterpriseFeaturePendingNotice(featureTitle) {
  window.alert(`${featureTitle}\n\n该功能待后续完善，敬请期待。`);
}

export const enterpriseNavItems = [
  { key: "dashboard", label: "工作台", caption: "总览状态与待办入口" },
  { key: "profile", label: "企业档案", caption: "主体资料与审核状态" },
  { key: "products", label: "产品档案", caption: "产品目录与状态维护" },
  { key: "inspections", label: "检查记录", caption: "历史检查与明细回看" },
  { key: "rectifications", label: "整改任务", caption: "整改提报与复核跟进" }
];

export const ENTERPRISE_PRODUCT_CATEGORY_PRESETS = [
  "酒类及饮料",
  "肉制品与调味品",
  "乳制品",
  "预包装食品",
  "生鲜蔬果",
  "冷冻食品"
];

export function getEnterpriseRouteName(key) {
  if (key === "dashboard") return "enterprise-dashboard";
  if (key === "products") return "enterprise-products";
  if (key === "inspections") return "enterprise-inspections";
  if (key === "rectifications") return "enterprise-rectifications";
  return "enterprise-profile";
}

export function useEnterpriseShellSession() {
  const router = useRouter();
  const session = computed(() => getActiveSession() || {});
  const enterpriseUser = computed(() => session.value);
  const token = computed(() => session.value.token || "");

  async function handleSidebarNavigate(nextKey) {
    const targetName = getEnterpriseRouteName(nextKey);
    if (router.currentRoute.value.name !== targetName) {
      await router.push({ name: targetName }).catch(() => {});
    }
  }

  async function handleLogout() {
    await performLogout();
    router.replace({ name: "login" }).catch(() => {});
  }

  return { enterpriseUser, token, handleSidebarNavigate, handleLogout };
}

export function getApprovalStatusLabel(loaded, approvalStatus) {
  if (!loaded) return "未提交";
  if (approvalStatus === "APPROVED") return "已通过";
  if (approvalStatus === "REJECTED") return "已驳回";
  if (approvalStatus === "PENDING") return "待审核";
  return "未提交";
}

export function getApprovalStatusTone(loaded, approvalStatus) {
  if (!loaded) return "neutral";
  if (approvalStatus === "APPROVED") return "success";
  if (approvalStatus === "REJECTED") return "danger";
  if (approvalStatus === "PENDING") return "warning";
  return "neutral";
}

export function formatProductStatus(value) {
  return { ACTIVE: "启用", INACTIVE: "停用" }[value] || value || "-";
}

export function formatInspectionResult(value) {
  return { PASS: "合格", FAIL: "不合格" }[value] || value || "-";
}

export function formatRectificationStatus(value) {
  return {
    ONGOING: "整改中",
    SUBMITTED: "待复核",
    REWORK: "打回重做",
    CONFIRMED: "已确认"
  }[value] || value || "-";
}

const RECTIFICATION_ACTION_LABELS = {
  SYSTEM_CREATE: "系统创建整改任务",
  ENTERPRISE_SUBMIT: "企业提交整改",
  REVIEW_CONFIRM: "监管复核通过",
  REVIEW_REWORK: "监管打回重做"
};

export function formatRectificationActionLabel(actionType, actionName) {
  if (actionName) return actionName;
  const key = String(actionType || "").toUpperCase();
  return RECTIFICATION_ACTION_LABELS[key] || key || "流程记录";
}

export function rectificationLogTime(log) {
  return log?.createTime || log?.actionTime || "";
}
