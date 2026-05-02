import { computed } from "vue";
import { useRouter } from "vue-router";
import { getActiveSession, performLogout } from "../../session/authRuntime";

export const regulatorAdminNavItems = [
  { key: "overview", label: "工作台", icon: "dashboard", routeName: "regulator-admin-overview", subtitle: "监管总览与待办中心" },
  { key: "enterprises", label: "企业管理", icon: "business", routeName: "regulator-admin-enterprises", subtitle: "企业档案与状态管理" },
  { key: "approvals", label: "企业备案审核", icon: "fact_check", routeName: "regulator-admin-approvals", subtitle: "企业备案审批流转" },
  { key: "dispatch", label: "检查任务", icon: "assignment", routeName: "regulator-admin-dispatch", subtitle: "检查任务创建与派发" },
  { key: "sampling", label: "抽检任务", icon: "biotech", routeName: "regulator-admin-sampling", subtitle: "抽检计划与结果管理" },
  { key: "complaints", label: "投诉流转", icon: "assignment_return", routeName: "regulator-admin-complaints", subtitle: "投诉分派与处理跟踪" },
  { key: "rectification", label: "整改复核", icon: "task_alt", routeName: "regulator-admin-rectifications", subtitle: "整改闭环复核" },
  { key: "warnings", label: "风险预警", icon: "warning", routeName: "regulator-admin-warnings", subtitle: "风险预警与处置" },
  { key: "bulletins", label: "公告管理", icon: "campaign", routeName: "regulator-admin-bulletins", subtitle: "监管公告发布" },
  { key: "stats", label: "统计分析", icon: "analytics", routeName: "regulator-admin-stats", subtitle: "监管统计看板" }
];

export function useRegulatorAdminShellSession() {
  const router = useRouter();
  const session = computed(() => getActiveSession() || {});
  const regulatorUser = computed(() => session.value);
  const token = computed(() => session.value.token || "");

  async function handleSidebarNavigate(nextKey) {
    const target = regulatorAdminNavItems.find((item) => item.key === nextKey);
    if (!target?.routeName) return;
    if (router.currentRoute.value.name !== target.routeName) {
      await router.push({ name: target.routeName }).catch(() => {});
    }
  }

  async function handleLogout() {
    await performLogout();
    router.replace({ name: "login" }).catch(() => {});
  }

  return { regulatorUser, token, handleSidebarNavigate, handleLogout };
}
