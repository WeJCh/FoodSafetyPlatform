<template>
  <div class="reg-admin-workspace">
    <RegulatorAdminSidebar
      :active-key="activeKey"
      :nav-items="regulatorAdminNavItems"
      :username="username"
      @navigate="$emit('navigate', $event)"
      @logout="$emit('logout')"
    />
    <RegulatorAdminTopbar
      :username="username"
      :search-placeholder="searchPlaceholder"
      @pending-feature="$emit('pending-feature', $event)"
    />
    <main class="reg-admin-workspace__main">
      <slot />
    </main>
  </div>
</template>

<script setup>
import RegulatorAdminSidebar from "./RegulatorAdminSidebar.vue";
import RegulatorAdminTopbar from "./RegulatorAdminTopbar.vue";
import { regulatorAdminNavItems } from "../../views/regulatorAdmin/regulatorAdminShared";

defineProps({
  activeKey: { type: String, default: "overview" },
  username: { type: String, default: "" },
  searchPlaceholder: { type: String, default: "搜索企业、任务或预警..." }
});

defineEmits(["navigate", "logout", "pending-feature"]);
</script>

<style scoped>
.reg-admin-workspace { min-height: 100vh; background: #f7f9fc; }
.reg-admin-workspace__main { margin-left: 256px; padding: 88px 24px 24px; }
</style>
