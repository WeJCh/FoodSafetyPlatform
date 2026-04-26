<template>
  <div class="enterprise-topbar enterprise-topbar--grid">
    <div class="enterprise-topbar__search enterprise-topbar__search--center">
      <span class="material-symbols-outlined enterprise-topbar__search-icon" aria-hidden="true">search</span>
      <input v-model="localQuery" type="search" :placeholder="searchPlaceholder" autocomplete="off" @keydown.enter.prevent="onSearchSubmit" />
    </div>

    <div class="enterprise-topbar__meta">
      <slot name="actions" />
      <button class="enterprise-topbar__icon-button" type="button" title="通知" @click="onNotifications">
        <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
      </button>
      <button class="enterprise-topbar__icon-button" type="button" title="帮助" @click="onHelp">
        <span class="material-symbols-outlined" aria-hidden="true">help_outline</span>
      </button>
      <div class="enterprise-topbar__divider" />
      <div class="enterprise-topbar__user enterprise-topbar__user--row">
        <div class="enterprise-topbar__avatar" aria-hidden="true">{{ userInitials }}</div>
        <div class="enterprise-topbar__user-lines">
          <span>{{ displayName }}</span>
          <span>{{ roleLabel }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { enterpriseFeaturePendingNotice } from "../../views/enterprise/enterpriseShared";

const props = defineProps({
  searchPlaceholder: {
    type: String,
    default: "搜索监管记录、产品或任务..."
  },
  username: {
    type: String,
    default: ""
  },
  roleLabel: {
    type: String,
    default: "企业用户"
  }
});

const localQuery = ref("");

watch(
  () => props.username,
  () => {
    localQuery.value = "";
  }
);

const displayName = computed(() => props.username?.trim() || "企业账户");

const userInitials = computed(() => {
  const raw = (props.username || "").trim();
  if (!raw) return "企";
  const parts = raw.split(/[\s@._-]+/).filter(Boolean);
  const ascii = /^[A-Za-z0-9]+$/.test(raw.replace(/[\s@._-]+/g, ""));
  if (ascii && parts.length >= 2) {
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase().slice(0, 2);
  }
  if (/[\u4e00-\u9fff]/.test(raw)) return raw.slice(0, 1);
  return raw.slice(0, 2).toUpperCase();
});

function onNotifications() {
  // TODO: 接入消息通知接口与通知中心页面
  enterpriseFeaturePendingNotice("消息通知");
}

function onHelp() {
  // TODO: 接入帮助中心 / 在线客服链接
  enterpriseFeaturePendingNotice("帮助中心");
}

function onSearchSubmit() {
  // TODO: 接入全局搜索（跨产品、检查、整改）后端检索
  if (!localQuery.value.trim()) {
    enterpriseFeaturePendingNotice("搜索");
    return;
  }
  enterpriseFeaturePendingNotice(`搜索「${localQuery.value.trim()}」`);
}
</script>
