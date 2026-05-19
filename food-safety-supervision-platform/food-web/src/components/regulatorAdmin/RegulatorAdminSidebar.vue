<template>
  <aside class="reg-admin-sidebar">
    <div class="reg-admin-brand">
      <div class="reg-admin-brand__logo">食安</div>
      <div>
        <p class="reg-admin-brand__title">食品安全监管平台</p>
      </div>
    </div>

    <nav class="reg-admin-nav">
      <button
        v-for="item in navItems"
        :key="item.key"
        class="reg-admin-nav__item"
        :class="{ 'is-active': item.key === activeKey }"
        type="button"
        @click="$emit('navigate', item.key)"
      >
        <span class="material-symbols-outlined reg-admin-nav__icon">{{ item.icon }}</span>
        <span class="reg-admin-nav__label">{{ item.label }}</span>
      </button>
    </nav>

    <div class="reg-admin-sidebar__footer">
      <button type="button" class="reg-admin-logout" @click="$emit('logout')">
        <span class="material-symbols-outlined">logout</span>
        <span>退出登录</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  activeKey: { type: String, default: "overview" },
  navItems: { type: Array, default: () => [] },
  username: { type: String, default: "" }
});

defineEmits(["navigate", "logout"]);
</script>

<style scoped>
.reg-admin-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  width: 256px;
  min-height: 100vh;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #0f172a;
  color: #cbd5e1;
  border-right: none;
}

.reg-admin-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 0;
}

.reg-admin-brand__logo {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  background: #2563eb;
  color: #fff;
  font-weight: 700;
  border-radius: 8px;
}

.reg-admin-brand__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #fff;
}

.reg-admin-nav {
  display: grid;
  gap: 6px;
  flex: 1;
  min-height: 0;
  overflow: auto;
  align-content: start;
}

.reg-admin-nav__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 48px;
  text-align: left;
  padding: 10px 12px;
  border: none;
  border-left: 3px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  transition: background-color 120ms ease, color 120ms ease, border-color 120ms ease;
}

.reg-admin-nav__item:hover {
  background: rgba(51, 65, 85, 0.55);
  color: #fff;
}

.reg-admin-nav__item.is-active {
  background: rgba(37, 99, 235, 0.22);
  border-left-color: #60a5fa;
  color: #bfdbfe;
}

.reg-admin-nav__icon {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: currentColor;
  font-size: 22px;
  font-weight: normal;
}

.reg-admin-nav__label {
  font-size: 12px;
  font-weight: 600;
}

.reg-admin-logout {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(248, 113, 113, 0.35);
  border-radius: 8px;
  background: #111827;
  color: #fecaca;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.reg-admin-logout:hover {
  background: #0b1220;
}

.reg-admin-sidebar__footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
  display: grid;
  gap: 6px;
}
</style>
