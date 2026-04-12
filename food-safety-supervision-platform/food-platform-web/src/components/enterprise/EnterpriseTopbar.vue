<template>
  <header class="enterprise-topbar">
    <div>
      <p class="enterprise-topbar__eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p class="enterprise-topbar__subtitle">{{ subtitle }}</p>
    </div>

    <div class="enterprise-topbar__actions">
      <label class="enterprise-search">
        <span class="material-symbols-outlined">search</span>
        <input type="text" placeholder="搜索任务、产品或记录..." />
      </label>
      <div class="enterprise-user-chip">
        <span>{{ username }}</span>
        <small>企业用户</small>
      </div>
    </div>
  </header>
</template>

<script setup>
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
