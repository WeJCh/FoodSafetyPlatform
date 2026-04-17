import { computed } from "vue";
import { useRouter } from "vue-router";
import { getActiveSession, performLogout } from "../../session/authRuntime";

export const regulatorEnforcerNavItems = [
  { key: "overview", label: "工作台", icon: "dashboard", routeName: "regulator-enforcer-enterprises" },
  { key: "tasks", label: "我的检查任务", icon: "assignment_turned_in", routeName: "regulator-enforcer-tasks" },
  { key: "sampling", label: "我的抽检任务", icon: "biotech", routeName: "regulator-enforcer-sampling" },
  { key: "complaints", label: "投诉处理", icon: "forum", routeName: "regulator-enforcer-complaints" },
  { key: "rectifications", label: "整改跟进", icon: "rebase_edit", routeName: "regulator-enforcer-rectifications" },
  { key: "warnings", label: "我的风险预警", icon: "warning", routeName: "regulator-enforcer-warnings" },
  { key: "inspections", label: "检查记录", icon: "analytics", routeName: "regulator-enforcer-inspections" },
  { key: "stats", label: "数据统计", icon: "monitoring", routeName: "regulator-enforcer-stats" }
];

export function regulatorEnforcerFeaturePendingNotice(featureTitle) {
  window.alert(`${featureTitle}\n\nTODO: 当前后端未提供该能力，先保留 UI 占位。`);
}

export function useRegulatorEnforcerShellSession() {
  const router = useRouter();
  const session = computed(() => getActiveSession() || {});
  const enforcerUser = computed(() => session.value);
  const token = computed(() => session.value.token || "");

  async function handleSidebarNavigate(nextKey) {
    const target = regulatorEnforcerNavItems.find((item) => item.key === nextKey);
    if (!target?.routeName) return;
    if (router.currentRoute.value.name !== target.routeName) {
      await router.push({ name: target.routeName }).catch(() => {});
    }
  }

  async function handleLogout() {
    await performLogout();
    router.replace({ name: "login" }).catch(() => {});
  }

  return { enforcerUser, token, handleSidebarNavigate, handleLogout };
}

