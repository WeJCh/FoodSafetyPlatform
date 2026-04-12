<template>
  <aside class="enterprise-sidebar">
    <div class="enterprise-sidebar__brand">
      <div class="enterprise-sidebar__seal">FS</div>
      <div>
        <strong>食品安全监管</strong>
        <span>Enterprise Ledger</span>
      </div>
    </div>

    <nav class="enterprise-sidebar__nav">
      <RouterLink v-for="item in navItems" :key="item.name" class="enterprise-nav-link" :to="item.to">
        <span class="material-symbols-outlined">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="enterprise-sidebar__footer">
      <button class="enterprise-ghost-button enterprise-ghost-button--full" type="button" @click="$emit('logout')">
        <span class="material-symbols-outlined">logout</span>
        <span>退出登录</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
defineEmits(["logout"]);

const navItems = [
  { name: "enterprise-dashboard", to: { name: "enterprise-dashboard" }, label: "工作台", icon: "dashboard" },
  { name: "enterprise-profile", to: { name: "enterprise-profile" }, label: "企业备案", icon: "business" },
  { name: "enterprise-products", to: { name: "enterprise-products" }, label: "产品档案", icon: "inventory_2" },
  { name: "enterprise-inspections", to: { name: "enterprise-inspections" }, label: "检查记录", icon: "fact_check" },
  { name: "enterprise-rectifications", to: { name: "enterprise-rectifications" }, label: "整改任务", icon: "assignment_late" }
];
</script>
