<template>
  <div class="sys-admin-workspace">
    <SystemAdminSidebar
      :active-key="activeKey"
      :nav-items="systemAdminNavItems"
      @navigate="$emit('navigate', $event)"
      @logout="$emit('logout')"
    />
    <SystemAdminTopbar
      :username="username"
      :search-placeholder="searchPlaceholder"
      @pending-feature="$emit('pending-feature', $event)"
    />
    <main class="sys-admin-workspace__main">
      <slot />
    </main>
  </div>
</template>

<script setup>
import SystemAdminSidebar from "./SystemAdminSidebar.vue";
import SystemAdminTopbar from "./SystemAdminTopbar.vue";
import { systemAdminNavItems } from "../../views/systemAdmin/systemAdminShared";

defineProps({
  activeKey: { type: String, default: "dashboard" },
  username: { type: String, default: "" },
  searchPlaceholder: { type: String, default: "搜索资源或记录..." }
});

defineEmits(["navigate", "logout", "pending-feature"]);
</script>

<style scoped>
.sys-admin-workspace {
  min-height: 100vh;
  background: #f7f9fc;
}
.sys-admin-workspace__main {
  margin-left: 256px;
  padding: 88px 12px 20px;
}
</style>
