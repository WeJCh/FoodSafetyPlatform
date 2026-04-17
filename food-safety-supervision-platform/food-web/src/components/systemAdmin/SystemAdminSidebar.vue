<template>
  <aside class="sys-admin-sidebar">
    <div class="sys-admin-brand">
      <h2>食品安全监管平台</h2>
    </div>

    <nav class="sys-admin-nav">
      <button
        v-for="item in navItems"
        :key="item.key"
        type="button"
        class="sys-admin-nav__item"
        :class="{ 'is-active': item.key === activeKey }"
        @click="$emit('navigate', item.key)"
      >
        <span class="material-symbols-outlined">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <button type="button" class="sys-admin-logout" @click="$emit('logout')">
      <span class="material-symbols-outlined">logout</span>
      <span>退出登录</span>
    </button>
  </aside>
</template>

<script setup>
defineProps({
  activeKey: { type: String, default: "dashboard" },
  navItems: { type: Array, default: () => [] }
});

defineEmits(["navigate", "logout"]);
</script>

<style scoped>
.sys-admin-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  width: 256px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 40;
}
.sys-admin-brand h2 {
  margin: 0;
  color: #002660;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.04em;
}
.sys-admin-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}
.sys-admin-nav__item {
  border: 0;
  background: transparent;
  color: #475569;
  padding: 10px 12px;
  border-radius: 2px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  text-align: left;
  transition: 0.2s ease;
}
.sys-admin-nav__item:hover {
  background: #e2e8f0;
  color: #002660;
}
.sys-admin-nav__item.is-active {
  background: #002660;
  color: #f8fafc;
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(2, 38, 96, 0.26);
}
.sys-admin-logout {
  border: 0;
  background: transparent;
  color: #64748b;
  padding: 10px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  margin-top: auto;
}
.sys-admin-logout:hover {
  color: #ba1a1a;
  background: #fee2e2;
}
</style>
