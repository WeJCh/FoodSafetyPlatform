<template>
  <EnterpriseShell>
    <template #sidebar>
      <EnterpriseSidebar
        :active-key="activeKey"
        :nav-items="enterpriseNavItems"
        @navigate="$emit('navigate', $event)"
        @logout="$emit('logout')"
      />
    </template>

    <template #topbar>
      <EnterpriseTopbar
        :search-placeholder="topSearchPlaceholder"
        :username="username"
        role-label="企业用户"
        @account="handleAccountNavigate"
      />
    </template>

    <slot name="summary" />
    <slot />
  </EnterpriseShell>
</template>

<script setup>
import { useRouter } from "vue-router";
import EnterpriseSidebar from "./EnterpriseSidebar.vue";
import EnterpriseTopbar from "./EnterpriseTopbar.vue";
import { enterpriseNavItems } from "../../views/enterprise/enterpriseShared";
import EnterpriseShell from "../../layouts/EnterpriseShell.vue";

defineProps({
  activeKey: { type: String, default: "dashboard" },
  title: { type: String, default: "" },
  subtitle: { type: String, default: "" },
  username: { type: String, default: "" },
  userType: { type: String, default: "" },
  topSearchPlaceholder: { type: String, default: "搜索记录、产品或任务..." }
});

const router = useRouter();

defineEmits(["navigate", "logout"]);

function handleAccountNavigate() {
  router.push({ name: "enterprise-account" }).catch(() => {});
}
</script>
