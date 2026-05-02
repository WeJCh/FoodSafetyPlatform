<template>
  <div class="enterprise-sidebar">
    <div class="enterprise-sidebar__brand">
      <div class="enterprise-sidebar__brand-mark">
        食安
      </div>
      <div class="enterprise-sidebar__brand-copy">
        <div class="enterprise-sidebar__title">食品安全监管平台</div>
      </div>
    </div>

    <nav class="enterprise-sidebar__nav" aria-label="企业端导航">
      <button
        v-for="item in navItems"
        :key="item.key"
        class="enterprise-sidebar__nav-item"
        :class="{ 'is-active': item.key === activeKey }"
        type="button"
        @click="$emit('navigate', item.key)"
      >
        <span
          class="material-symbols-outlined enterprise-sidebar__nav-icon"
          :class="{ 'is-filled': item.key === activeKey }"
          aria-hidden="true"
        >{{ navMaterialIcon[item.key] || "circle" }}</span>
        <span class="enterprise-sidebar__nav-label">{{ item.label }}</span>
      </button>
    </nav>

    <div class="enterprise-sidebar__footer">
      <button class="enterprise-sidebar__footer-item is-danger" type="button" @click="$emit('logout')">
        <span class="material-symbols-outlined" aria-hidden="true">logout</span>
        退出
      </button>
    </div>
  </div>
</template>

<script setup>
const navMaterialIcon = {
  dashboard: "dashboard",
  profile: "domain",
  products: "inventory_2",
  inspections: "history_edu",
  rectifications: "task_alt"
};

defineProps({
  activeKey: {
    type: String,
    default: "profile"
  },
  navItems: {
    type: Array,
    default: () => []
  }
});

defineEmits(["navigate", "logout"]);
</script>
