<template>
  <aside class="enforcer-sidebar">
    <div class="enforcer-brand">
      <h2>执法政务系统</h2>
      <p>监管人员工作区</p>
    </div>

    <nav class="enforcer-nav">
      <button
        v-for="item in navItems"
        :key="item.key"
        type="button"
        class="enforcer-nav__item"
        :class="{ 'is-active': item.key === activeKey }"
        @click="$emit('navigate', item.key)"
      >
        <span class="material-symbols-outlined">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <button type="button" class="enforcer-logout" @click="$emit('logout')">
      <span class="material-symbols-outlined">logout</span>
      <span>退出登录</span>
    </button>
  </aside>
</template>

<script setup>
defineProps({
  activeKey: { type: String, default: "overview" },
  navItems: { type: Array, default: () => [] }
});

defineEmits(["navigate", "logout"]);
</script>

<style scoped>
.enforcer-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  width: 256px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 40;
}
.enforcer-brand h2 {
  margin: 0;
  color: #002660;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0.01em;
}
.enforcer-brand p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 700;
}
.enforcer-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}
.enforcer-nav__item {
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
.enforcer-nav__item:hover {
  background: #e2e8f0;
  color: #002660;
}
.enforcer-nav__item.is-active {
  background: #002660;
  color: #f8fafc;
  font-weight: 700;
  box-shadow: 0 1px 2px rgba(2, 38, 96, 0.26);
}
.enforcer-logout {
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
.enforcer-logout:hover {
  color: #ba1a1a;
  background: #fee2e2;
}
</style>

