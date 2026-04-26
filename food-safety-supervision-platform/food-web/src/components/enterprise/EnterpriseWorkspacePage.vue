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
        :role-label="userType || '企业用户'"
      >
        <template #actions>
          <slot name="actions">
            <EnterpriseStatusChip v-if="statusLabel" :label="statusLabel" :tone="statusTone" />
          </slot>
        </template>
      </EnterpriseTopbar>
    </template>

    <slot name="summary" />
    <slot />
  </EnterpriseShell>
</template>

<script setup>
import EnterpriseSidebar from "./EnterpriseSidebar.vue";
import EnterpriseStatusChip from "./EnterpriseStatusChip.vue";
import EnterpriseTopbar from "./EnterpriseTopbar.vue";
import { enterpriseNavItems } from "../../views/enterprise/enterpriseShared";
import EnterpriseShell from "../../layouts/EnterpriseShell.vue";

defineProps({
  activeKey: { type: String, default: "dashboard" },
  title: { type: String, default: "" },
  subtitle: { type: String, default: "" },
  username: { type: String, default: "" },
  userType: { type: String, default: "" },
  statusLabel: { type: String, default: "" },
  statusTone: { type: String, default: "neutral" },
  topSearchPlaceholder: { type: String, default: "搜索监管记录、产品或任务..." }
});

defineEmits(["navigate", "logout"]);
</script>
