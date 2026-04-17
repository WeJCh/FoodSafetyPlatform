import { computed } from "vue";
import { useRouter } from "vue-router";
import { getActiveSession, performLogout } from "../../session/authRuntime";

export const systemAdminNavItems = [
  { key: "dashboard", label: "工作台", icon: "dashboard", routeName: "admin-dashboard", subtitle: "系统总览与动态" },
  { key: "list", label: "监管人员列表", icon: "group", routeName: "admin-regulator-list", subtitle: "人员账号与状态管理" },
  { key: "create", label: "新建监管人员", icon: "person_add", routeName: "admin-regulator-create", subtitle: "创建新监管人员账号" }
];

export function systemAdminFeaturePendingNotice(featureTitle) {
  window.alert(`${featureTitle}\n\nTODO: 当前后端未提供该能力，先保留 UI 占位。`);
}

export function useSystemAdminShellSession() {
  const router = useRouter();
  const session = computed(() => getActiveSession() || {});
  const adminUser = computed(() => session.value);
  const token = computed(() => session.value.token || "");

  async function handleSidebarNavigate(nextKey) {
    const target = systemAdminNavItems.find((item) => item.key === nextKey);
    if (!target?.routeName) return;
    if (router.currentRoute.value.name !== target.routeName) {
      await router.push({ name: target.routeName }).catch(() => {});
    }
  }

  async function handleLogout() {
    await performLogout();
    router.replace({ name: "login" }).catch(() => {});
  }

  return { adminUser, token, handleSidebarNavigate, handleLogout };
}
