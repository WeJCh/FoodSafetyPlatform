<template>
  <aside class="enforcer-sidebar">
    <div class="enforcer-brand">
      <div class="enforcer-brand__logo">食安</div>
      <div>
        <p class="enforcer-brand__title">食品安全监管平台</p>
      </div>
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
  background: #0f172a;
  color: #cbd5e1;
  border-right: none;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  z-index: 40;
}
.enforcer-brand { display: flex; gap: 12px; align-items: center; margin-bottom: 20px; }
.enforcer-brand__logo { width: 40px; height: 40px; border-radius: 8px; display: grid; place-items: center; background: #2563eb; color: #fff; font-weight: 700; }
.enforcer-brand__title { margin: 0; color: #fff; font-size: 15px; font-weight: 700; letter-spacing: 0.04em; }
.enforcer-nav {
  display: grid;
  gap: 6px;
  flex: 1;
  overflow: auto;
}
.enforcer-nav__item {
  border: 0;
  background: transparent;
  color: inherit;
  padding: 10px 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  text-align: left;
  transition: 0.2s ease;
}
.enforcer-nav__item:hover {
  background: rgba(51, 65, 85, 0.55);
  color: #fff;
}
.enforcer-nav__item.is-active {
  background: rgba(37, 99, 235, 0.22);
  color: #bfdbfe;
  border-left: 3px solid #60a5fa;
}
.enforcer-logout {
  border: 1px solid #334155;
  background: #111827;
  color: #e2e8f0;
  padding: 10px 12px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  margin-top: auto;
}
.enforcer-logout:hover {
  background: #0b1220;
  color: #fecaca;
  border-color: rgba(248, 113, 113, 0.35);
}
</style>

