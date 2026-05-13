<template>
  <div class="enforcer-workspace">
    <RegulatorEnforcerSidebar
      :active-key="activeKey"
      :nav-items="regulatorEnforcerNavItems"
      @navigate="$emit('navigate', $event)"
      @logout="$emit('logout')"
    />
    <RegulatorEnforcerTopbar
      :username="username"
      :search-placeholder="searchPlaceholder"
      @account="handleAccountNavigate"
    />
    <main class="enforcer-workspace__main">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import RegulatorEnforcerSidebar from "./RegulatorEnforcerSidebar.vue";
import RegulatorEnforcerTopbar from "./RegulatorEnforcerTopbar.vue";
import { regulatorEnforcerNavItems } from "../../views/regulatorEnforcer/regulatorEnforcerShared";

defineProps({
  activeKey: { type: String, default: "overview" },
  username: { type: String, default: "" },
  searchPlaceholder: { type: String, default: "搜索企业、任务或待办事项" }
});

const router = useRouter();

defineEmits(["navigate", "logout"]);

function handleAccountNavigate() {
  router.push({ name: "regulator-enforcer-account" }).catch(() => {});
}
</script>

<style scoped>
.enforcer-workspace {
  min-height: 100vh;
  background: #f7f9fc;
}

.enforcer-workspace__main {
  margin-left: 256px;
  padding: 88px 24px 24px;
}
</style>
