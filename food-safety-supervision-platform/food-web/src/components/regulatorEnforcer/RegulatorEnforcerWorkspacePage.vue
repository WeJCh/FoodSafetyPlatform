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
      @pending-feature="$emit('pending-feature', $event)"
    />
    <main class="enforcer-workspace__main">
      <slot />
    </main>
  </div>
</template>

<script setup>
import RegulatorEnforcerSidebar from "./RegulatorEnforcerSidebar.vue";
import RegulatorEnforcerTopbar from "./RegulatorEnforcerTopbar.vue";
import { regulatorEnforcerNavItems } from "../../views/regulatorEnforcer/regulatorEnforcerShared";

defineProps({
  activeKey: { type: String, default: "overview" },
  username: { type: String, default: "" },
  searchPlaceholder: { type: String, default: "搜索功能、任务或企业..." }
});

defineEmits(["navigate", "logout", "pending-feature"]);
</script>

<style scoped>
.enforcer-workspace {
  min-height: 100vh;
  background: #f7f9fc;
}
.enforcer-workspace__main {
  margin-left: 256px;
  padding: 88px 16px 20px;
}
</style>

