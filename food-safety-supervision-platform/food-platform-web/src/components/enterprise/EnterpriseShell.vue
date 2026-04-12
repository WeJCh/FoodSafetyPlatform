<template>
  <div class="enterprise-app-shell">
    <EnterpriseSidebar @logout="$emit('logout')" />
    <div class="enterprise-app-shell__main">
      <EnterpriseTopbar
        :title="title"
        :subtitle="subtitle"
        :eyebrow="eyebrow"
        :username="username"
      />
      <main class="enterprise-app-shell__content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import EnterpriseSidebar from "./EnterpriseSidebar.vue";
import EnterpriseTopbar from "./EnterpriseTopbar.vue";

defineEmits(["logout"]);

defineProps({
  title: {
    type: String,
    default: "企业工作台"
  },
  subtitle: {
    type: String,
    default: "聚合备案维护、产品档案、检查记录与整改跟进。"
  },
  eyebrow: {
    type: String,
    default: "Digital Ledger"
  },
  username: {
    type: String,
    default: "企业用户"
  }
});
</script>
