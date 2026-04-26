<template>
  <header class="reg-admin-topbar">
    <div class="reg-admin-topbar__left">
      <strong>区域管理员</strong>
      <input :placeholder="searchPlaceholder" />
    </div>
    <div class="reg-admin-topbar__right">
      <button type="button" @click="$emit('pending-feature', '通知中心')">
        <span class="material-symbols-outlined">notifications</span>
      </button>
      <button type="button" @click="$emit('pending-feature', '帮助中心')">
        <span class="material-symbols-outlined">help_outline</span>
      </button>
      <button type="button" @click="$emit('pending-feature', '系统设置')">
        <span class="material-symbols-outlined">settings</span>
      </button>
      <div class="reg-admin-topbar__user">{{ username || "监管管理员" }}</div>
    </div>
  </header>
</template>

<script setup>
defineProps({
  username: { type: String, default: "" },
  searchPlaceholder: { type: String, default: "搜索企业、任务或预警..." }
});

defineEmits(["pending-feature"]);
</script>

<style scoped>
.reg-admin-topbar { position: fixed; left: 256px; top: 0; right: 0; height: 64px; padding: 0 24px; background: rgba(255, 255, 255, 0.88); backdrop-filter: blur(6px); display: flex; align-items: center; justify-content: space-between; z-index: 30; border-bottom: 1px solid #e2e8f0; }
.reg-admin-topbar__left { display: flex; align-items: center; gap: 18px; }
.reg-admin-topbar__left strong { font-size: 20px; color: #002660; }
.reg-admin-topbar__left input { width: 280px; border: 0; background: #f1f5f9; border-radius: 10px; padding: 8px 12px; font-size: 13px; }
.reg-admin-topbar__right { display: flex; align-items: center; gap: 10px; }
.reg-admin-topbar__right button { border: 0; background: transparent; color: #64748b; cursor: pointer; width: 32px; height: 32px; border-radius: 999px; }
.reg-admin-topbar__right button:hover { background: #e2e8f0; color: #1d4ed8; }
.reg-admin-topbar__user { margin-left: 8px; border-left: 1px solid #cbd5e1; padding-left: 12px; font-size: 13px; color: #002660; font-weight: 600; }
</style>
