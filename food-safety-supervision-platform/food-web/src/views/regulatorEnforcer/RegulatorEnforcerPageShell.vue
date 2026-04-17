<template>
  <RegulatorEnforcerWorkspacePage
    :active-key="activeKey"
    :username="enforcerUser.username || enforcerUser.realName || ''"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="regulatorEnforcerFeaturePendingNotice"
  >
    <section class="shell-panel">
      <header class="shell-panel__header">
        <h2>{{ title }}</h2>
        <p>{{ subtitle }}</p>
      </header>
      <div class="shell-panel__body">
        <slot>
          <div class="placeholder-note">阶段 1 已完成：页面骨架已就绪，后续阶段将逐页迁入真实业务逻辑与接口。</div>
        </slot>
      </div>
    </section>
  </RegulatorEnforcerWorkspacePage>
</template>

<script setup>
import RegulatorEnforcerWorkspacePage from "../../components/regulatorEnforcer/RegulatorEnforcerWorkspacePage.vue";
import {
  regulatorEnforcerFeaturePendingNotice,
  useRegulatorEnforcerShellSession
} from "./regulatorEnforcerShared";

defineProps({
  activeKey: { type: String, default: "overview" },
  title: { type: String, required: true },
  subtitle: { type: String, default: "" }
});

const { enforcerUser, handleSidebarNavigate, handleLogout } = useRegulatorEnforcerShellSession();
</script>

<style scoped>
.shell-panel {
  border: 1px solid #d9e2ec;
  background: #fff;
  min-height: calc(100vh - 108px);
  border-radius: 2px;
  overflow: hidden;
}
.shell-panel__header {
  padding: 18px 20px 14px;
  border-bottom: 1px solid #e2e8f0;
}
.shell-panel__header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
}
.shell-panel__header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}
.shell-panel__body {
  padding: 20px;
}
.placeholder-note {
  border: 1px dashed #cbd5e1;
  border-radius: 2px;
  background: #f8fafc;
  color: #475569;
  font-size: 14px;
  padding: 14px;
}
</style>

