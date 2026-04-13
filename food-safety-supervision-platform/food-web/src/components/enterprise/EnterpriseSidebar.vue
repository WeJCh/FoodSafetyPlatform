<template>
  <div class="enterprise-sidebar">
    <div class="enterprise-sidebar__brand">
      <div class="enterprise-sidebar__brand-mark">
        <span class="material-symbols-outlined is-filled" aria-hidden="true">policy</span>
      </div>
      <div class="enterprise-sidebar__brand-copy">
        <div class="enterprise-sidebar__title">{{ title }}</div>
        <p class="enterprise-sidebar__subtitle">{{ subtitle }}</p>
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
      <button class="enterprise-sidebar__footer-item" type="button" @click="onSettings">
        <span class="material-symbols-outlined" aria-hidden="true">settings</span>
        设置
      </button>
      <button class="enterprise-sidebar__footer-item is-danger" type="button" @click="$emit('logout')">
        <span class="material-symbols-outlined" aria-hidden="true">logout</span>
        退出
      </button>
    </div>
  </div>
</template>

<script setup>
import { enterpriseFeaturePendingNotice } from "../../views/enterprise/enterpriseShared";

const navMaterialIcon = {
  dashboard: "dashboard",
  profile: "domain",
  products: "inventory_2",
  inspections: "history_edu",
  rectifications: "task_alt"
};

defineProps({
  title: {
    type: String,
    default: "企业监管中心"
  },
  subtitle: {
    type: String,
    default: "Enterprise Oversight"
  },
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

function onSettings() {
  // TODO: 企业端设置（通知偏好、密码修改等）待产品定稿与后端接口
  enterpriseFeaturePendingNotice("企业端设置");
}
</script>
