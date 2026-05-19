<template>
  <EnterpriseWorkspacePage
    active-key="rectifications"
    title="提交成功"
    subtitle="整改进展已提交，等待监管复核。"
    top-search-placeholder="搜索任务或编号..."
    :username="enterpriseUser.username"
    :user-type="enterpriseUser.userType"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <div class="enterprise-workspace-card enterprise-success-screen">
      <div class="enterprise-success-screen__icon">
        <span class="material-symbols-outlined is-filled" aria-hidden="true">check_circle</span>
      </div>
      <h1>整改材料已提交</h1>
      <p class="secondary-text" style="max-width: 420px; margin: 0 auto 22px; line-height: 1.6">
        系统已接收本次整改进展，状态将更新为“待复核”。您可随时在任务详情中查看监管意见。
      </p>
      <div class="enterprise-panel" style="max-width: 520px; margin: 0 auto 24px; text-align: left">
        <div class="enterprise-readonly-field">
          <span>本次提交说明</span>
          <div>{{ progress || "（未附带文字说明）" }}</div>
        </div>
      </div>
      <div class="action-buttons enterprise-success-actions" style="justify-content: center">
        <RouterLink class="ghost enterprise-toolbar-button" :to="{ name: 'enterprise-rectifications' }">返回任务列表</RouterLink>
        <RouterLink class="primary enterprise-link-button" :to="{ name: 'enterprise-rectification-detail', params: { rectificationId } }">查看任务详情</RouterLink>
      </div>
    </div>
  </EnterpriseWorkspacePage>
</template>

<script setup>
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";
import EnterpriseWorkspacePage from "../../components/enterprise/EnterpriseWorkspacePage.vue";
import { useEnterpriseShellSession } from "./enterpriseShared";

const route = useRoute();
const { enterpriseUser, handleSidebarNavigate, handleLogout } = useEnterpriseShellSession();
const rectificationId = computed(() => String(route.params.rectificationId || ""));
const progress = computed(() => String(route.query.progress || ""));
</script>
