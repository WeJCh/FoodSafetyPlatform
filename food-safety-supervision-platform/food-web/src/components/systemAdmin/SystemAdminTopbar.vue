<template>
  <header class="sys-admin-topbar">
    <div class="sys-admin-topbar__left">
      <h1>系统管理员</h1>
      <label class="sys-admin-search">
        <span class="material-symbols-outlined">search</span>
        <input :placeholder="searchPlaceholder" type="text" />
      </label>
    </div>
    <div class="sys-admin-topbar__right">
      <div class="sys-admin-user">
        <strong>{{ username || "Admin User" }}</strong>
        <small>系统管理员</small>
      </div>
    </div>
  </header>
</template>

<script setup>
defineProps({
  username: { type: String, default: "" },
  searchPlaceholder: { type: String, default: "搜索资源或记录..." }
});
</script>

<style scoped>
.sys-admin-topbar {
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
.sys-admin-topbar__left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.sys-admin-topbar__left h1 {
  margin: 0;
  color: #002660;
  font-size: 19px;
  font-weight: 700;
}
.sys-admin-search {
  width: 280px;
  background: #e6e8eb;
  border-radius: 2px;
  padding: 7px 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.sys-admin-search span {
  color: #94a3b8;
  font-size: 18px;
}
.sys-admin-search input {
  border: 0;
  background: transparent;
  width: 100%;
  font-size: 13px;
}
.sys-admin-topbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sys-admin-user {
  border-left: 1px solid #e2e8f0;
  margin-left: 8px;
  padding-left: 12px;
  line-height: 1.2;
}
.sys-admin-user strong {
  display: block;
  color: #0f172a;
  font-size: 12px;
}
.sys-admin-user small {
  color: #64748b;
  font-size: 10px;
}
</style>
