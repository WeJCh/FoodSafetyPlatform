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
        <span class="material-symbols-outlined">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <div class="reg-admin-sidebar__footer">
      <p>{{ username || "监管管理员" }}</p>
      <button type="button" @click="$emit('logout')">退出登录</button>
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
.reg-admin-sidebar { width: 256px; min-height: 100vh; background: #0f172a; color: #cbd5e1; padding: 24px 16px; display: flex; flex-direction: column; position: fixed; left: 0; top: 0; }
.reg-admin-brand { display: flex; gap: 12px; align-items: center; margin-bottom: 20px; }
.reg-admin-brand__logo { width: 40px; height: 40px; border-radius: 8px; display: grid; place-items: center; background: #2563eb; color: #fff; font-weight: 700; }
.reg-admin-brand__title { margin: 0; color: #fff; font-size: 15px; font-weight: 700; letter-spacing: 0.04em; }
.reg-admin-nav { display: grid; gap: 6px; flex: 1; overflow: auto; }
.reg-admin-nav__item { border: 0; background: transparent; color: inherit; border-radius: 8px; padding: 10px 12px; display: flex; gap: 10px; align-items: center; cursor: pointer; text-align: left; }
.reg-admin-nav__item:hover { background: rgba(51, 65, 85, 0.55); color: #fff; }
.reg-admin-nav__item.is-active { background: rgba(37, 99, 235, 0.22); color: #bfdbfe; border-left: 3px solid #60a5fa; }
.reg-admin-sidebar__footer { border-top: 1px solid rgba(148, 163, 184, 0.2); padding-top: 16px; display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.reg-admin-sidebar__footer p { margin: 0; font-size: 12px; color: #e2e8f0; }
.reg-admin-sidebar__footer button { border: 1px solid #334155; background: #111827; color: #e2e8f0; padding: 6px 10px; border-radius: 8px; cursor: pointer; }
</style>
