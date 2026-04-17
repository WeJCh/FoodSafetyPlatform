<template>
  <header class="enforcer-topbar">
    <div class="enforcer-topbar__left">
      <h1>执法人员</h1>
      <label class="enforcer-search">
        <span class="material-symbols-outlined">search</span>
        <input :placeholder="searchPlaceholder" type="text" />
      </label>
    </div>
    <div class="enforcer-topbar__right">
      <button type="button" @click="$emit('pending-feature', '通知中心')">
        <span class="material-symbols-outlined">notifications</span>
      </button>
      <button type="button" @click="$emit('pending-feature', '帮助中心')">
        <span class="material-symbols-outlined">help</span>
      </button>
      <div class="enforcer-user">
        <strong>{{ username || "Enforcer User" }}</strong>
        <small>执法人员</small>
      </div>
    </div>
  </header>
</template>

<script setup>
defineProps({
  username: { type: String, default: "" },
  searchPlaceholder: { type: String, default: "搜索功能、任务或企业..." }
});

defineEmits(["pending-feature"]);
</script>

<style scoped>
.enforcer-topbar {
  position: fixed;
  left: 256px;
  right: 0;
  top: 0;
  height: 64px;
  padding: 0 24px;
  background: rgba(248, 250, 252, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.55);
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 35;
}
.enforcer-topbar__left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.enforcer-topbar__left h1 {
  margin: 0;
  color: #002660;
  font-size: 19px;
  font-weight: 700;
}
.enforcer-search {
  width: 300px;
  background: #e6e8eb;
  border-radius: 2px;
  padding: 7px 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.enforcer-search span {
  color: #94a3b8;
  font-size: 18px;
}
.enforcer-search input {
  border: 0;
  background: transparent;
  width: 100%;
  font-size: 13px;
}
.enforcer-topbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.enforcer-topbar__right button {
  border: 0;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: 99px;
}
.enforcer-topbar__right button:hover {
  background: #e2e8f0;
  color: #002660;
}
.enforcer-user {
  border-left: 1px solid #e2e8f0;
  margin-left: 8px;
  padding-left: 12px;
  line-height: 1.2;
}
.enforcer-user strong {
  display: block;
  color: #0f172a;
  font-size: 12px;
}
.enforcer-user small {
  color: #64748b;
  font-size: 10px;
}
</style>

