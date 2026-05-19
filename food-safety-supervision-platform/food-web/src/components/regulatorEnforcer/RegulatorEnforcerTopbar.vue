<template>
  <header class="enforcer-topbar">
    <div class="enforcer-topbar__left">
      <h1>执法人员</h1>
      <label class="regulator-topbar-search">
        <span class="material-symbols-outlined regulator-topbar-search__icon">search</span>
        <input class="regulator-topbar-search__input" :placeholder="searchPlaceholder" type="text" />
      </label>
    </div>
    <div class="enforcer-topbar__right">
      <button type="button" class="enforcer-topbar__account" title="个人信息" @click="goAccount">
        <span class="material-symbols-outlined">account_circle</span>
      </button>
      <div class="topbar-user-badge__divider" />
      <div class="topbar-user-badge">
        <div class="topbar-user-badge__avatar" aria-hidden="true">{{ userInitials }}</div>
        <div class="topbar-user-badge__body">
          <strong class="topbar-user-badge__name">{{ displayName }}</strong>
          <small class="topbar-user-badge__role">执法人员</small>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  username: { type: String, default: "" },
  searchPlaceholder: { type: String, default: "搜索企业、任务或待办事项" }
});

const displayName = computed(() => props.username?.trim() || "执法账号");

const userInitials = computed(() => {
  const raw = (props.username || "").trim();
  if (!raw) return "EF";
  const parts = raw.split(/[\s@._-]+/).filter(Boolean);
  const ascii = /^[A-Za-z0-9]+$/.test(raw.replace(/[\s@._-]+/g, ""));
  if (ascii && parts.length >= 2) {
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase().slice(0, 2);
  }
  if (/[\u4e00-\u9fff]/.test(raw)) return raw.slice(0, 1);
  return raw.slice(0, 2).toUpperCase();
});

const emit = defineEmits(["account"]);

function goAccount() {
  emit("account");
}
</script>

<style scoped>
.enforcer-topbar {
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

.enforcer-topbar__left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.enforcer-topbar__left h1 {
  margin: 0;
  color: #002660;
  font-size: 19px;
  font-weight: 700;
}

.enforcer-topbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.enforcer-topbar__account {
  width: 40px;
  height: 40px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #475569;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.enforcer-topbar__account:hover {
  color: #2563eb;
  border-color: #bfdbfe;
  background: #eff6ff;
}
</style>
