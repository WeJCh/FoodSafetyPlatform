<template>
    <PublicWorkspacePage
    page-class="public-sampling-results-page"
    active-key="sampling"
    :show-search="true"
    v-model:search-value="filters.enterpriseName"
    search-placeholder="搜索抽检企业"
    :search-min-width="220"
    @search="handleSearch"
  >
    <main class="public-sampling-results-page__main">
      <section class="public-sampling-results-page__head">
        <div>
          <div class="public-sampling-results-page__crumb">政务公开 / 抽检结果</div>
          <h1>抽检结果</h1>
          <p>实时公开已对外发布的食品安全监督抽检结果，保障公众知情权与监督权。</p>
        </div>
        <div class="public-sampling-results-page__filters">
          <label>
            <span>企业名称</span>
            <input v-model.trim="filters.enterpriseName" type="text" placeholder="请输入企业名称" @keyup.enter="handleSearch" />
          </label>
          <label>
            <span>抽检结果</span>
            <select v-model="filters.result">
              <option value="">全部结果</option>
              <option value="PASS">合格</option>
              <option value="FAIL">不合格</option>
            </select>
          </label>
          <label>
            <span>产品名称</span>
            <input v-model.trim="filters.productName" type="text" placeholder="支持模糊匹配" @keyup.enter="handleSearch" />
          </label>
          <div class="public-sampling-results-page__filters-actions">
            <button type="button" :disabled="loading" @click="handleSearch">{{ loading ? "查询中..." : "查询" }}</button>
            <button type="button" class="ghost" @click="resetFilters">重置</button>
          </div>
        </div>
      </section>

      <section class="public-sampling-results-page__table-card">
        <div class="public-sampling-results-page__table-head">
          <span>抽检企业 / 任务编号</span>
          <span>产品名称 / 规格型号</span>
          <span>抽检与公示时间</span>
          <span>抽检结果</span>
          <span>操作</span>
        </div>

        <AppEmptyState
          v-if="!records.length"
          :title="emptyTitle"
          :description="emptyDescription"
          class="public-sampling-results-page__empty-state"
        />

        <div v-for="item in records" :key="item.id" class="public-sampling-results-page__row">
          <div class="public-sampling-results-page__col-main">
            <strong>{{ item.enterpriseName || "-" }}</strong>
            <small>{{ item.taskNo || "-" }}</small>
          </div>
          <div class="public-sampling-results-page__col-sub">
            <p>{{ item.productName || "-" }}</p>
            <small>{{ item.productSpecification || "暂无规格" }}</small>
          </div>
          <div class="public-sampling-results-page__col-time">
            <p><span>抽检</span>{{ formatDate(item.sampledTime) }}</p>
            <p><span>公示</span>{{ formatDateShort(item.publishedTime || item.updateTime) }}</p>
          </div>
          <div class="public-sampling-results-page__col-status">
            <AppStatusTag :label="formatResult(item.result)" :tone="item.result === 'FAIL' ? 'danger' : 'success'" />
          </div>
          <div class="public-sampling-results-page__col-action">
            <button type="button" @click="viewResult(item)">查看详情</button>
          </div>
        </div>

        <div class="public-sampling-results-page__pager">
          <p>{{ pagerSummary }}</p>
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
import { fetchPublicSamplingResults } from "../../api/regulationOperation";
import { getActiveSession } from "../../session/authRuntime";
import { resolveErrorMessage } from "../../utils/uiFeedback";

const route = useRoute();
const router = useRouter();
const publicToken = getActiveSession()?.token || "";
const filters = reactive({ enterpriseName: "", productName: "", result: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "" });

const pagerStart = computed(() => {
  if (!total.value || !records.value.length) {
    return 0;
  }
  return (page.value - 1) * size.value + 1;
});

const pagerEnd = computed(() => {
  if (!total.value || !records.value.length) {
    return 0;
  }
  return (page.value - 1) * size.value + records.value.length;
});

const pagerSummary = computed(() => {
  if (!total.value) {
    return "暂无记录";
  }
  return `显示 ${pagerStart.value} 到 ${pagerEnd.value} 条，共 ${total.value} 条记录`;
});

const hasFilters = computed(() => Boolean(filters.enterpriseName.trim() || filters.productName.trim() || filters.result));
const emptyTitle = computed(() => (hasFilters.value ? "暂无符合条件的抽检结果" : "暂无抽检结果"));
const emptyDescription = computed(() => (hasFilters.value ? "可以调整企业名称、产品名称或结果筛选后再试。" : "已公示的抽检结果会展示在这里。"));

function setStatus(message = "", type = "info") {
  status.message = message;
  status.type = type;
}

function formatResult(value) {
  if (value === "FAIL") {
    return "不合格";
  }
  if (value === "PASS") {
    return "合格";
  }
  return "-";
}

function formatDate(value) {
  if (!value) {
    return "-";
  }
  const s = String(value).replace("T", " ");
  return s.slice(0, 16);
}

function formatDateShort(value) {
  if (!value) {
    return "-";
  }
  const s = String(value).replace("T", " ");
  return s.slice(0, 10);
}

async function loadResults() {
  loading.value = true;
  setStatus("");
  try {
    const data = await fetchPublicSamplingResults(publicToken, {
      enterpriseName: filters.enterpriseName,
      productName: filters.productName,
      result: filters.result,
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
    setStatus(resolveErrorMessage(error, "抽检结果加载失败，请稍后重试"), "error");
  } finally {
    loading.value = false;
  }
}

function syncRouteQuery() {
  const nextQuery = {};
  if (filters.enterpriseName.trim()) nextQuery.enterpriseName = filters.enterpriseName.trim();
  if (filters.productName.trim()) nextQuery.productName = filters.productName.trim();
  if (filters.result) nextQuery.result = filters.result;
  router.replace({ query: nextQuery }).catch(() => {});
}

function applyRouteQuery() {
  const nextEnterpriseName = typeof route.query.enterpriseName === "string" ? route.query.enterpriseName.trim() : "";
  const nextProductName = typeof route.query.productName === "string" ? route.query.productName.trim() : "";
  const nextResult = typeof route.query.result === "string" ? route.query.result.trim().toUpperCase() : "";
  filters.enterpriseName = nextEnterpriseName;
  filters.productName = nextProductName;
  filters.result = nextResult === "PASS" || nextResult === "FAIL" ? nextResult : "";
}

function handleSearch() {
  page.value = 1;
  syncRouteQuery();
  loadResults();
}

function resetFilters() {
  filters.enterpriseName = "";
  filters.productName = "";
  filters.result = "";
  page.value = 1;
  syncRouteQuery();
  loadResults();
}

function changePage(nextPage) {
  page.value = nextPage;
  loadResults();
}

function viewResult(item) {
  if (!item?.id) {
    return;
  }
  const nextQuery = {};
  if (filters.enterpriseName.trim()) nextQuery.enterpriseName = filters.enterpriseName.trim();
  if (filters.productName.trim()) nextQuery.productName = filters.productName.trim();
  if (filters.result) nextQuery.result = filters.result;
  router
    .push({
      name: "public-sampling-result-detail",
      params: { samplingResultId: item.id },
      query: nextQuery
    })
    .catch(() => {});
}

onMounted(() => {
  applyRouteQuery();
  loadResults();
});

watch(
  () => [route.query.enterpriseName, route.query.productName, route.query.result],
  () => {
    applyRouteQuery();
    page.value = 1;
    loadResults();
  }
);
</script>

<style scoped>
.public-sampling-results-page {
  min-height: 100vh;
  background: var(--surface);
}

.public-sampling-results-page__topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid rgba(195, 198, 211, 0.4);
  background: rgba(248, 250, 253, 0.84);
}

.public-sampling-results-page__topbar-inner {
  max-width: 1680px;
  margin: 0 auto;
  min-height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.public-sampling-results-page__brand-nav {
  display: flex;
  align-items: center;
  gap: 24px;
}

.public-sampling-results-page__brand {
  font-family: var(--font-display);
  font-size: var(--public-brand-size);
  font-weight: 800;
  color: var(--primary);
}

.public-sampling-results-page__nav {
  display: flex;
  gap: 18px;
}

.public-sampling-results-page__nav-item {
  border: none;
  background: transparent;
  min-height: 56px;
  color: var(--on-surface-variant);
  font-size: var(--public-text-md);
  font-weight: 700;
  border-bottom: 2px solid transparent;
  cursor: pointer;
}

.public-sampling-results-page__nav-item.is-active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.public-sampling-results-page__toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.public-sampling-results-page__search-box {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  border: 1px solid rgba(195, 198, 211, 0.44);
  background: rgba(255, 255, 255, 0.75);
  padding: 0 14px;
  min-height: var(--public-toolbar-min-h);
}

.public-sampling-results-page__search-box input {
  border: none;
  background: transparent;
  font-size: var(--public-text-md);
  min-width: var(--public-toolbar-input-min-w);
}

.public-sampling-results-page__account {
  min-height: var(--public-toolbar-min-h);
  margin: 0;
  padding-inline: 12px;
}

.public-sampling-results-page__account .material-symbols-outlined {
  font-size: 22px;
}

.public-sampling-results-page__logout {
  min-height: var(--public-toolbar-min-h);
  font-size: var(--public-text-md);
  margin: 0;
}

.public-sampling-results-page__main {
  max-width: 1680px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.public-sampling-results-page__crumb {
  font-size: var(--public-text-md);
  color: var(--on-surface-variant);
}

.public-sampling-results-page__head {
  background: var(--surface-container-lowest);
  border: 1px solid rgba(195, 198, 211, 0.32);
  border-bottom: none;
  border-radius: 12px 12px 0 0;
  padding: 20px 22px 16px;
}

.public-sampling-results-page__head h1 {
  margin: 4px 0 10px;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: var(--public-page-title);
  line-height: 1.05;
}

.public-sampling-results-page__head p {
  margin: 0;
  color: var(--on-surface-variant);
  font-size: var(--public-body);
  line-height: 1.65;
  max-width: 48rem;
}

.public-sampling-results-page__filters {
  margin-top: 10px;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  display: grid;
  grid-template-columns: minmax(200px, 1fr) minmax(150px, 0.9fr) minmax(200px, 1fr) auto;
  gap: 12px 14px;
  align-items: end;
}

.public-sampling-results-page__filters label {
  display: grid;
  gap: 6px;
}

.public-sampling-results-page__filters label span {
  font-size: var(--public-caption);
  color: #5e6880;
  font-weight: 600;
}

.public-sampling-results-page__filters input,
.public-sampling-results-page__filters select {
  min-height: var(--public-control-min-h);
  border-radius: 6px;
  border: 1px solid #d5dbea;
  background: #fff;
  padding: 0 12px;
  font-size: var(--public-text-md);
  color: #243047;
}

.public-sampling-results-page__filters-actions {
  display: inline-flex;
  gap: 8px;
}

.public-sampling-results-page__filters-actions button {
  min-height: var(--public-control-min-h);
  padding: 0 16px;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: var(--public-text-md);
  cursor: pointer;
  font-weight: 600;
}

.public-sampling-results-page__filters-actions button:first-child {
  background: #0a3d86;
  color: #fff;
}

.public-sampling-results-page__filters-actions button.ghost {
  border-color: #d5dbea;
  background: #f7f8fb;
  color: #56607a;
}

.public-sampling-results-page__table-card {
  border-radius: 0 0 12px 12px;
  overflow: hidden;
  border: 1px solid rgba(195, 198, 211, 0.3);
  background: var(--surface-container-lowest);
}

.public-sampling-results-page__table-head {
  display: grid;
  grid-template-columns: 2.2fr 2fr 1.5fr 1fr 1.1fr;
  gap: 10px;
  padding: 15px 20px;
  background: linear-gradient(135deg, #003a8c 0%, #0b4f9f 100%);
  color: #fff;
  font-size: var(--public-body-secondary);
  font-weight: 700;
  letter-spacing: 0.03em;
}

.public-sampling-results-page__row {
  display: grid;
  grid-template-columns: 2.2fr 2fr 1.5fr 1fr 1.1fr;
  gap: 10px;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid rgba(195, 198, 211, 0.24);
  transition: background-color 0.2s ease;
}

.public-sampling-results-page__row:hover {
  background: rgba(70, 89, 231, 0.04);
}

.public-sampling-results-page__col-main strong {
  display: block;
  color: var(--primary);
  font-size: var(--public-body-em);
  line-height: 1.25;
}

.public-sampling-results-page__col-main small,
.public-sampling-results-page__col-sub small {
  color: var(--on-surface-variant);
  font-size: var(--public-body-secondary);
  line-height: 1.4;
}

.public-sampling-results-page__col-sub p {
  margin: 0;
  font-size: var(--public-body);
  line-height: 1.5;
  color: var(--on-surface);
}

.public-sampling-results-page__col-time p {
  margin: 0 0 6px;
  font-size: var(--public-text-md);
  color: var(--on-surface);
  line-height: 1.45;
}

.public-sampling-results-page__col-time span {
  display: inline-block;
  min-width: 2.5em;
  margin-right: 6px;
  color: var(--on-surface-variant);
  font-size: var(--public-caption);
  font-weight: 600;
}

.public-sampling-results-page__col-action button {
  border: 1px solid rgba(70, 89, 231, 0.24);
  background: rgba(70, 89, 231, 0.06);
  color: var(--primary);
  font-size: var(--public-text-md);
  font-weight: 700;
  cursor: pointer;
  min-height: var(--public-btn-action-min-h);
  border-radius: 8px;
  padding: 0 14px;
}

.public-sampling-results-page__empty-state {
  margin: 20px;
}

.public-sampling-results-page__pager {
  padding: 18px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: rgba(242, 244, 247, 0.55);
  border-top: 1px solid rgba(195, 198, 211, 0.22);
}

.public-sampling-results-page__pager p {
  margin: 0;
  color: var(--on-surface-variant);
  font-size: var(--public-text-md);
  line-height: 1.5;
}

.public-sampling-results-page__pager div {
  display: flex;
  align-items: center;
  gap: 6px;
}

.public-sampling-results-page__pager button {
  width: var(--public-pager-btn);
  height: var(--public-pager-btn);
  border: 1px solid rgba(195, 198, 211, 0.35);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.7);
  color: var(--on-surface-variant);
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: var(--public-text-md);
  font-weight: 700;
}

.public-sampling-results-page__pager button.is-active {
  background: var(--primary);
  border-color: rgba(0, 38, 96, 0.5);
  color: #fff;
}

.public-sampling-results-page__pager button:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

@media (max-width: 1100px) {
  .public-sampling-results-page__nav {
    display: none;
  }
}

@media (max-width: 900px) {
  .public-sampling-results-page__head h1 {
    font-size: var(--public-page-title-sm);
  }

  .public-sampling-results-page__filters {
    grid-template-columns: 1fr 1fr;
  }

  .public-sampling-results-page__filters-actions {
    grid-column: 1 / -1;
  }

  .public-sampling-results-page__table-head,
  .public-sampling-results-page__row {
    grid-template-columns: 1.8fr 1.6fr 1.2fr 0.9fr 1fr;
  }
}

@media (max-width: 760px) {
  .public-sampling-results-page__toolbar {
    display: none;
  }

  .public-sampling-results-page__head {
    padding: 14px;
  }

  .public-sampling-results-page__filters {
    grid-template-columns: 1fr;
  }

  .public-sampling-results-page__table-head {
    display: none;
  }

  .public-sampling-results-page__row {
    grid-template-columns: 1fr;
    gap: 6px;
    padding: 12px;
    border: 1px solid rgba(195, 198, 211, 0.28);
    border-radius: 10px;
    margin: 10px 12px 0;
  }
}
</style>




