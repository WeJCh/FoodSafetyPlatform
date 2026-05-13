<template>
  <header class="reg-admin-topbar">
    <div class="reg-admin-topbar__left">
      <strong>区域管理员</strong>
      <input :placeholder="searchPlaceholder" />
    </div>
    <div class="reg-admin-topbar__right">
      <button type="button" class="reg-admin-topbar__account" title="个人信息" @click="goAccount">
        <span class="material-symbols-outlined">account_circle</span>
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

const emit = defineEmits(["account"]);

function goAccount() {
  emit("account");
}
</script>

<style scoped>
.reg-admin-topbar { position: fixed; left: 256px; top: 0; right: 0; height: 64px; padding: 0 24px; background: rgba(255, 255, 255, 0.88); backdrop-filter: blur(6px); display: flex; align-items: center; justify-content: space-between; z-index: 30; border-bottom: 1px solid #e2e8f0; }
.reg-admin-topbar__left { display: flex; align-items: center; gap: 18px; }
.reg-admin-topbar__left strong { font-size: 20px; color: #002660; }
.reg-admin-topbar__left input { width: 280px; border: 0; background: #f1f5f9; border-radius: 10px; padding: 8px 12px; font-size: 13px; }
.reg-admin-topbar__right { display: flex; align-items: center; gap: 10px; }
.reg-admin-topbar__account { width: 40px; height: 40px; border: 1px solid #cbd5e1; background: #fff; color: #475569; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; }
.reg-admin-topbar__account:hover { color: #2563eb; border-color: #bfdbfe; background: #eff6ff; }
.reg-admin-topbar__user { margin-left: 8px; border-left: 1px solid #cbd5e1; padding-left: 12px; font-size: 13px; color: #002660; font-weight: 600; }
</style>
