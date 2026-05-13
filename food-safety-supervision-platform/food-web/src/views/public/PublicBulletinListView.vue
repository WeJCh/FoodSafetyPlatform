<template>
    <PublicWorkspacePage
    page-class="public-bulletins-page"
    active-key="bulletins"
    :show-search="true"
    v-model:search-value="filters.keyword"
    search-placeholder="搜索公告标题或关键词"
    :search-min-width="220"
    @search="handleSearch"
  >
    <main class="public-bulletins-page__main">
      <section class="public-bulletins-page__hero">
        <h1>监管公告</h1>
        <p>政策通知与公开公告</p>
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

          <AppEmptyState
            v-if="!records.length"
            :title="emptyTitle"
            :description="emptyDescription"
            class="public-bulletins-page__empty-state"
          />

          <button
            v-for="item in records"
            :key="item.id"
            type="button"
            class="public-bulletins-page__row"
            @click="viewBulletin(item)"
          >
            <div class="public-bulletins-page__row-main">
              <strong>{{ item.title || "-" }}</strong>
              <p>{{ bulletinSubline(item) }}</p>
            </div>
            <div class="public-bulletins-page__row-status">
              <AppStatusTag :label="formatCategory(item.category)" tone="success" />
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

      <AppStatusToast :message="status.message" :type="status.type" />
    </main>
    </PublicWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import PublicWorkspacePage from "../../components/public/PublicWorkspacePage.vue";
import AppEmptyState from "../../components/common/AppEmptyState.vue";
import AppStatusTag from "../../components/common/AppStatusTag.vue";
import AppStatusToast from "../../components/common/AppStatusToast.vue";
import { fetchPublicBulletins } from "../../api/regulation";
import { getActiveSession } from "../../session/authRuntime";
import { formatTime } from "../../utils/formatters";
import { resolveErrorMessage } from "../../utils/uiFeedback";

const route = useRoute();
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
const hasFilters = computed(() => Boolean(filters.keyword.trim() || filters.category));
const emptyTitle = computed(() => (hasFilters.value ? "暂无符合条件的公告" : "暂无公告"));
const emptyDescription = computed(() => (hasFilters.value ? "可以调整关键字或公告类别后再试。" : "已发布的监管公告会展示在这里。"));

function setStatus(message = "", type = "info") {
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
    setStatus(resolveErrorMessage(error, "公告列表加载失败，请稍后重试"), "error");
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  page.value = 1;
  syncRouteQuery();
  loadBulletins();
}

function changePage(nextPage) {
  page.value = nextPage;
  loadBulletins();
}

function setCategory(next) {
  filters.category = next;
  page.value = 1;
  syncRouteQuery();
  loadBulletins();
}

function viewBulletin(item) {
  if (!item?.id) return;
  const nextQuery = {};
  if (filters.keyword.trim()) nextQuery.keyword = filters.keyword.trim();
  if (filters.category) nextQuery.category = filters.category;
  router.push({
    name: "public-bulletin-detail",
    params: { bulletinId: item.id },
    query: nextQuery
  }).catch(() => {});
}

function syncRouteQuery() {
  const nextQuery = {};
  if (filters.keyword.trim()) nextQuery.keyword = filters.keyword.trim();
  if (filters.category) nextQuery.category = filters.category;
  router.replace({ query: nextQuery }).catch(() => {});
}

function applyRouteQuery() {
  const nextKeyword = typeof route.query.keyword === "string" ? route.query.keyword.trim() : "";
  const nextCategory = typeof route.query.category === "string" ? route.query.category.trim().toUpperCase() : "";
  filters.keyword = nextKeyword;
  filters.category = categoryOptions.some((item) => item.key === nextCategory) ? nextCategory : "";
}

function bulletinDate(item) {
  return String(formatTime(item?.publishedTime || "")).slice(0, 10) || "-";
}

function bulletinSubline(item) {
  return `公告类别：${formatCategory(item?.category)}${item?.publishedByName ? ` · 发布人：${item.publishedByName}` : ""}`;
}

onMounted(() => {
  applyRouteQuery();
  loadBulletins();
});

watch(
  () => [route.query.keyword, route.query.category],
  () => {
    applyRouteQuery();
    page.value = 1;
    loadBulletins();
  }
);
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
  min-height: var(--public-topbar-min-h);
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
  font-size: var(--public-brand-size);
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
  min-height: var(--public-topbar-min-h);
  padding: 0;
  color: var(--on-surface-variant);
  font-size: var(--public-nav-size);
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
  padding: 0 14px;
  min-height: var(--public-toolbar-min-h);
}

.public-bulletins-page__search-box input {
  border: none;
  background: transparent;
  font-size: var(--public-toolbar-input-size);
  min-width: var(--public-toolbar-input-min-w);
}

.public-bulletins-page__logout {
  min-height: var(--public-toolbar-min-h);
  font-size: var(--public-logout-font-size);
  margin: 0;
}

.public-bulletins-page__account {
  min-height: var(--public-toolbar-min-h);
  margin: 0;
  padding-inline: 12px;
}

.public-bulletins-page__account .material-symbols-outlined {
  font-size: 22px;
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
  font-size: var(--public-hero-title);
}

.public-bulletins-page__hero p {
  margin: 0;
  letter-spacing: 0.12em;
  font-size: var(--public-hero-subtitle-en);
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
  font-size: var(--public-table-head-overline);
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
  min-height: var(--public-btn-action-min-h);
  font-size: var(--public-control);
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
  font-size: var(--public-table-head-overline);
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
  font-size: var(--public-body);
  font-weight: 800;
  letter-spacing: -0.02em;
}

.public-bulletins-page__row-main p {
  margin: 6px 0 0;
  color: var(--on-surface-variant);
  font-size: var(--public-overline);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.public-bulletins-page__row-status {
  text-align: center;
}

.public-bulletins-page__row-time {
  text-align: right;
  color: var(--on-surface-variant);
  font-size: var(--public-caption);
  font-family: var(--font-display);
}

.public-bulletins-page__empty-state {
  margin: 20px 24px;
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
  font-size: var(--public-pager);
  line-height: 1.5;
}

.public-bulletins-page__pager div {
  display: flex;
  align-items: center;
  gap: 6px;
}

.public-bulletins-page__pager button {
  width: var(--public-pager-btn);
  height: var(--public-pager-btn);
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
    font-size: var(--public-page-title-xs);
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




