<template>
  <div class="public-bulletins-page">
    <header class="public-bulletins-page__topbar">
      <div class="public-bulletins-page__topbar-inner">
        <div class="public-bulletins-page__brand-nav">
          <span class="public-bulletins-page__brand">食品安全监管平台</span>
          <nav class="public-bulletins-page__nav" aria-label="公众导航">
            <button
              v-for="item in topNavItems"
              :key="item.key"
              type="button"
              class="public-bulletins-page__nav-item"
              :class="{ 'is-active': item.key === 'bulletins' }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-bulletins-page__toolbar">
          <label class="public-bulletins-page__search-box">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <input v-model.trim="filters.keyword" type="text" placeholder="搜索公告标题或类别" @keyup.enter="handleSearch" />
          </label>
          <button type="button" class="public-bulletins-page__icon-btn" @click="onFeaturePending('通知中心')">
            <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
          </button>
          <button type="button" class="public-bulletins-page__icon-btn" @click="onFeaturePending('个人中心')">
            <span class="material-symbols-outlined" aria-hidden="true">account_circle</span>
          </button>
          <button class="ghost public-bulletins-page__logout" type="button" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="public-bulletins-page__main">
      <section class="public-bulletins-page__hero">
        <h1>监管公告</h1>
        <p>Regulatory Announcements & Directives</p>
      </section>

      <section class="public-bulletins-page__content">
        <aside class="public-bulletins-page__sidebar">
          <div class="public-bulletins-page__side-card">
            <h3>公告类别</h3>
            <div class="public-bulletins-page__category-list">
              <button
                v-for="item in categoryOptions"
                :key="item.key"
                type="button"
                class="public-bulletins-page__category-btn"
                :class="{ 'is-active': filters.category === item.key }"
                @click="setCategory(item.key)"
              >
                <span>{{ item.label }}</span>
              </button>
            </div>
          </div>
        </aside>

        <section class="public-bulletins-page__list-card">
          <div class="public-bulletins-page__table-head">
            <span>公告标题</span>
            <span>类别</span>
            <span>发布日期</span>
          </div>
          <div v-if="!records.length" class="public-bulletins-page__empty">暂无已发布公告</div>
          <button v-for="item in records" :key="item.id" type="button" class="public-bulletins-page__row" @click="viewBulletin(item)">
            <div class="public-bulletins-page__row-main">
              <strong>{{ item.title || '-' }}</strong>
              <p>{{ bulletinSubline(item) }}</p>
            </div>
            <div class="public-bulletins-page__row-status">
              <i class="public-bulletins-page__status-chip is-published">{{ formatCategory(item.category) }}</i>
            </div>
            <div class="public-bulletins-page__row-time">{{ bulletinDate(item) }}</div>
          </button>

          <div class="public-bulletins-page__pager">
            <p>显示 {{ pagerStart }} 到 {{ pagerEnd }} 条，共 {{ total }} 条记录</p>
            <div>
              <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">
                <span class="material-symbols-outlined" aria-hidden="true">chevron_left</span>
              </button>
              <button type="button" class="is-active">{{ page }}</button>
              <button type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">
                <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
              </button>
            </div>
          </div>
        </section>
      </section>

      <div class="status" :class="status.type" v-if="status.message">{{ status.message }}</div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchPublicBulletins } from "../../api/regulation";
import { getActiveSession, performLogout } from "../../session/authRuntime";
import { formatTime } from "../../utils/formatters";

const router = useRouter();
const publicToken = getActiveSession()?.token || "";
const filters = reactive({ keyword: "", category: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "" });

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

const categoryOptions = [
  { key: "", label: "全部公告" },
  { key: "POLICY", label: "政策法规" },
  { key: "INSPECTION", label: "监督检查" },
  { key: "NOTICE", label: "消费提示" },
  { key: "OTHER", label: "其他公告" }
];

const categoryLabelMap = Object.fromEntries(categoryOptions.filter((item) => item.key).map((item) => [item.key, item.label]));

const pagerStart = computed(() => (total.value === 0 ? 0 : (page.value - 1) * size.value + 1));
const pagerEnd = computed(() => Math.min(page.value * size.value, total.value));

async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatCategory(value) {
  return categoryLabelMap[String(value || "").toUpperCase()] || "未分类";
}

async function loadBulletins() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchPublicBulletins(publicToken, {
      keyword: filters.keyword,
      category: filters.category,
      page: page.value,
      size: size.value
    });
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    records.value = [];
    setStatus(error.message || "加载公告列表失败", "error");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  loadBulletins();
}

function changePage(nextPage) {
  page.value = nextPage;
  loadBulletins();
}

function setCategory(next) {
  filters.category = next;
  page.value = 1;
  loadBulletins();
}

function viewBulletin(item) {
  if (!item?.id) return;
  router.push({
    name: "public-bulletin-detail",
    params: { bulletinId: item.id }
  }).catch(() => {});
}

function goTo(name) {
  router.push({ name }).catch(() => {});
}

function onFeaturePending(name) {
  window.alert(`${name} 功能待后续完善`);
}

function bulletinDate(item) {
  return String(formatTime(item?.publishedTime || "")).slice(0, 10) || "-";
}

function bulletinSubline(item) {
  return `公告类别：${formatCategory(item?.category)}${item?.publishedByName ? ` · 发布人：${item.publishedByName}` : ""}`;
}

onMounted(loadBulletins);
</script>

<style scoped>
.public-bulletins-page {
  min-height: 100vh;
  background: var(--surface);
}

.public-bulletins-page__topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid rgba(195, 198, 211, 0.4);
  background: rgba(248, 250, 253, 0.84);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.public-bulletins-page__topbar-inner {
  max-width: 1680px;
  margin: 0 auto;
  min-height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.public-bulletins-page__brand-nav {
  display: flex;
  align-items: center;
  gap: 24px;
}

.public-bulletins-page__brand {
  font-family: var(--font-display);
  font-size: 25px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--primary);
}

.public-bulletins-page__nav {
  display: flex;
  align-items: center;
  gap: 18px;
}

.public-bulletins-page__nav-item {
  border: none;
  background: transparent;
  min-height: 56px;
  padding: 0;
  color: var(--on-surface-variant);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}

.public-bulletins-page__nav-item.is-active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.public-bulletins-page__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.public-bulletins-page__search-box {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  border: 1px solid rgba(195, 198, 211, 0.44);
  background: rgba(255, 255, 255, 0.75);
  padding: 0 12px;
  min-height: 34px;
}

.public-bulletins-page__search-box input {
  border: none;
  background: transparent;
  font-size: 12px;
  min-width: 180px;
}

.public-bulletins-page__icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  color: var(--on-surface-variant);
}

.public-bulletins-page__logout {
  min-height: 34px;
  margin: 0;
}

.public-bulletins-page__main {
  max-width: 1680px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.public-bulletins-page__hero {
  margin-bottom: 24px;
  border-radius: 12px;
  padding: 42px 36px;
  color: #fff;
  background: linear-gradient(135deg, #002660 0%, #003a8c 100%);
}

.public-bulletins-page__hero h1 {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-size: 40px;
}

.public-bulletins-page__hero p {
  margin: 0;
  letter-spacing: 0.12em;
  font-size: 11px;
  font-weight: 700;
  opacity: 0.88;
  text-transform: uppercase;
}

.public-bulletins-page__content {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
}

.public-bulletins-page__side-card,
.public-bulletins-page__list-card {
  border-radius: 12px;
  border: 1px solid rgba(195, 198, 211, 0.36);
  background: var(--surface-container-lowest);
}

.public-bulletins-page__side-card {
  padding: 18px 16px;
}

.public-bulletins-page__side-card h3 {
  margin: 0 0 12px;
  color: var(--primary);
  font-size: 12px;
  letter-spacing: 0.08em;
  font-weight: 800;
  text-transform: uppercase;
}

.public-bulletins-page__category-list {
  display: grid;
  gap: 6px;
}

.public-bulletins-page__category-btn {
  border: none;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 36px;
  border-radius: 8px;
  background: transparent;
  padding: 0 10px;
  cursor: pointer;
  color: var(--on-surface-variant);
}

.public-bulletins-page__category-btn.is-active {
  background: rgba(0, 38, 96, 0.08);
  color: var(--primary);
  font-weight: 700;
}

.public-bulletins-page__list-card {
  overflow: hidden;
  box-shadow: 0 18px 40px -34px rgba(0, 38, 96, 0.32);
}

.public-bulletins-page__table-head,
.public-bulletins-page__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px 140px;
  align-items: center;
  gap: 12px;
}

.public-bulletins-page__table-head {
  padding: 16px 24px;
  border-bottom: 1px solid rgba(195, 198, 211, 0.34);
  background: rgba(242, 244, 247, 0.78);
}

.public-bulletins-page__table-head span {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--on-surface-variant);
}

.public-bulletins-page__table-head span:nth-child(2) {
  text-align: center;
}

.public-bulletins-page__table-head span:nth-child(3) {
  text-align: right;
}

.public-bulletins-page__row {
  width: 100%;
  border: none;
  border-bottom: 1px solid rgba(195, 198, 211, 0.22);
  background: transparent;
  padding: 18px 24px;
  cursor: pointer;
  transition: background-color 140ms ease;
}

.public-bulletins-page__row:hover {
  background: rgba(242, 244, 247, 0.6);
}

.public-bulletins-page__row:nth-of-type(even) {
  background: rgba(248, 250, 253, 0.55);
}

.public-bulletins-page__row-main {
  text-align: left;
}

.public-bulletins-page__row-main strong {
  display: block;
  color: var(--primary);
  font-size: 15px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.public-bulletins-page__row-main p {
  margin: 6px 0 0;
  color: var(--on-surface-variant);
  font-size: 11px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.public-bulletins-page__row-status {
  text-align: center;
}

.public-bulletins-page__status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  padding: 3px 10px;
  font-size: 10px;
  font-weight: 800;
  border: 1px solid transparent;
}

.public-bulletins-page__status-chip.is-published {
  background: rgba(26, 127, 90, 0.1);
  color: #1a7f5a;
  border-color: rgba(26, 127, 90, 0.18);
}

.public-bulletins-page__row-time {
  text-align: right;
  color: var(--on-surface-variant);
  font-size: 12px;
  font-family: var(--font-display);
}

.public-bulletins-page__empty {
  padding: 30px 20px;
  text-align: center;
  color: var(--on-surface-variant);
}

.public-bulletins-page__pager {
  padding: 18px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: rgba(242, 244, 247, 0.55);
  border-top: 1px solid rgba(195, 198, 211, 0.22);
}

.public-bulletins-page__pager p {
  margin: 0;
  color: var(--on-surface-variant);
  font-size: 12px;
}

.public-bulletins-page__pager div {
  display: flex;
  align-items: center;
  gap: 6px;
}

.public-bulletins-page__pager button {
  width: 32px;
  height: 32px;
  border: 1px solid rgba(195, 198, 211, 0.35);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.7);
  color: var(--on-surface-variant);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background-color 120ms ease, border-color 120ms ease, color 120ms ease;
}

.public-bulletins-page__pager button:hover:not(:disabled):not(.is-active) {
  border-color: rgba(0, 38, 96, 0.22);
  color: var(--primary);
  background: #fff;
}

.public-bulletins-page__pager button.is-active {
  background: var(--primary);
  border-color: rgba(0, 38, 96, 0.5);
  color: #fff;
}

.public-bulletins-page__pager button:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

@media (max-width: 1100px) {
  .public-bulletins-page__nav {
    display: none;
  }

  .public-bulletins-page__content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .public-bulletins-page__topbar-inner,
  .public-bulletins-page__main {
    padding-left: 12px;
    padding-right: 12px;
  }

  .public-bulletins-page__toolbar {
    display: none;
  }

  .public-bulletins-page__hero {
    padding: 28px 20px;
  }

  .public-bulletins-page__hero h1 {
    font-size: 32px;
  }

  .public-bulletins-page__table-head,
  .public-bulletins-page__row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .public-bulletins-page__row-status,
  .public-bulletins-page__row-time {
    text-align: left !important;
  }
}
</style>
