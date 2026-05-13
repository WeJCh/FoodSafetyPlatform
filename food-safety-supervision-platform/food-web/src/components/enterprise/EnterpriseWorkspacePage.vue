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
        :role-label="userType || '浼佷笟鐢ㄦ埛'"
        @account="handleAccountNavigate"
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
import { useRouter } from "vue-router";
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
  topSearchPlaceholder: { type: String, default: "鎼滅储鐩戠璁板綍銆佷骇鍝佹垨浠诲姟..." }
});

const router = useRouter();

defineEmits(["navigate", "logout"]);

function handleAccountNavigate() {
  router.push({ name: "enterprise-account" }).catch(() => {});
}
</script>
