<template>
  <div :class="pageClass">
    <header class="public-workspace__topbar">
      <div class="public-workspace__topbar-inner">
        <div class="public-workspace__brand-nav">
          <span class="public-workspace__brand">食品安全监管平台</span>
          <nav class="public-workspace__nav" aria-label="公众导航">
            <button
              v-for="item in publicNavItems"
              :key="item.key"
              type="button"
              class="public-workspace__nav-item"
              :class="{ 'is-active': item.key === activeKey }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>

        <div class="public-workspace__toolbar">
          <label v-if="showSearch" class="public-workspace__search-box">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <input
              :value="searchValue"
              type="text"
              :placeholder="searchPlaceholder"
              :style="searchInputStyle"
              @input="handleSearchInput"
              @keyup.enter="$emit('search')"
            />
          </label>
          <button
            type="button"
            class="ghost public-workspace__account"
            :class="{ 'is-active': activeKey === 'account' }"
            title="个人信息"
            @click="goTo('public-account')"
          >
            <span class="material-symbols-outlined" aria-hidden="true">account_circle</span>
          </button>
          <button type="button" class="ghost public-workspace__logout" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <slot />
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { performLogout } from "../../session/authRuntime";
import { publicNavItems } from "../../views/public/publicShared";

const props = defineProps({
  pageClass: { type: String, default: "" },
  activeKey: { type: String, default: "" },
  showSearch: { type: Boolean, default: false },
  searchValue: { type: String, default: "" },
  searchPlaceholder: { type: String, default: "" },
  searchMinWidth: { type: [Number, String], default: "" }
});

const emit = defineEmits(["update:searchValue", "search"]);

const router = useRouter();

const searchInputStyle = computed(() => {
  if (!props.searchMinWidth) return {};
  const width = typeof props.searchMinWidth === "number" ? `${props.searchMinWidth}px` : props.searchMinWidth;
  return { minWidth: width };
});

function handleSearchInput(event) {
  emit("update:searchValue", event.target.value);
}

function goTo(name) {
  router.push({ name }).catch(() => {});
}

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}
</script>

<style scoped>
.public-workspace__topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid rgba(195, 198, 211, 0.4);
  background: rgba(248, 250, 253, 0.84);
}

.public-workspace__topbar-inner {
  max-width: 1680px;
  margin: 0 auto;
  min-height: var(--public-topbar-min-h);
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.public-workspace__brand-nav {
  display: flex;
  align-items: center;
  gap: 24px;
}

.public-workspace__brand {
  font-family: var(--font-display);
  font-size: var(--public-brand-size);
  font-weight: 800;
  color: var(--primary);
}

.public-workspace__nav {
  display: flex;
  gap: 18px;
}

.public-workspace__nav-item {
  border: none;
  background: transparent;
  min-height: var(--public-topbar-min-h);
  padding: 0;
  color: var(--on-surface-variant);
  font-size: var(--public-nav-size);
  font-weight: 700;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 120ms ease, border-color 120ms ease;
}

.public-workspace__nav-item:hover {
  color: var(--primary);
}

.public-workspace__nav-item.is-active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.public-workspace__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.public-workspace__search-box {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  border: 1px solid rgba(195, 198, 211, 0.44);
  background: rgba(255, 255, 255, 0.75);
  padding: 0 14px;
  min-height: var(--public-toolbar-min-h);
}

.public-workspace__search-box input {
  border: none;
  background: transparent;
  font-size: var(--public-toolbar-input-size);
}

.public-workspace__account {
  min-height: var(--public-toolbar-min-h);
  margin: 0;
  padding-inline: 12px;
}

.public-workspace__account.is-active {
  color: var(--primary);
  border-color: rgba(70, 89, 231, 0.24);
  background: rgba(70, 89, 231, 0.08);
}

.public-workspace__account .material-symbols-outlined {
  font-size: 22px;
}

.public-workspace__logout {
  min-height: var(--public-toolbar-min-h);
  font-size: var(--public-logout-font-size);
  margin: 0;
}

@media (max-width: 1100px) {
  .public-workspace__nav {
    display: none;
  }
}

@media (max-width: 760px) {
  .public-workspace__toolbar {
    display: none;
  }
}
</style>
