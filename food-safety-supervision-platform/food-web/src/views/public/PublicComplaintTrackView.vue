<template>
    <PublicWorkspacePage
    page-class="public-complaint-track-page"
    active-key="complaints"
    :show-search="true"
    v-model:search-value="filters.keyword"
    search-placeholder="搜索投诉编号、企业名称或投诉内容"
    :search-min-width="220"
    @search="applyFilters"
  >
    <main class="public-complaint-track-page__main">
      <section class="public-complaint-track-page__head">
        <h1>我的投诉</h1>
        <p>查看投诉处理进度、反馈结果与关键节点。</p>
      </section>

      <section class="public-complaint-track-page__stats">
        <article class="public-complaint-track-page__stat-card">
          <span>全部投诉</span>
          <strong>{{ stats.totalCount }}</strong>
        </article>
        <article class="public-complaint-track-page__stat-card is-processing">
          <span>处理中</span>
          <strong>{{ stats.processingCount }}</strong>
        </article>
        <article class="public-complaint-track-page__stat-card is-finished">
          <span>已办结</span>
          <strong>{{ stats.finishedCount }}</strong>
        </article>
        <button type="button" class="public-complaint-track-page__create-btn" @click="goTo('public-complaint-create')">
          <span class="material-symbols-outlined" aria-hidden="true">add_moderator</span>
          发起投诉
        </button>
      </section>

      <section class="public-complaint-track-page__filters">
        <label>
          <span>处理状态</span>
          <select v-model="filters.status">
            <option value="">全部</option>
            <option value="SUBMITTED">待受理</option>
            <option value="PENDING">待分派</option>
            <option value="ASSIGNED">已分派</option>
            <option value="PROCESSING">处理中</option>
            <option value="FEEDBACKED">已反馈</option>
            <option value="REJECTED">已驳回</option>
          </select>
        </label>
        <div class="public-complaint-track-page__filter-actions">
          <button type="button" @click="applyFilters" :disabled="loading">{{ loading ? "查询中..." : "查询" }}</button>
          <button type="button" class="ghost" @click="resetFilters">重置</button>
        </div>
      </section>

      <section class="public-complaint-track-page__list">
        <AppEmptyState
          v-if="!filteredRecords.length"
          :title="emptyTitle"
          :description="emptyDescription"
          class="public-complaint-track-page__empty"
        />

        <article
          v-for="item in filteredRecords"
          :key="item.id"
          class="public-complaint-track-page__item"
        >
          <div class="public-complaint-track-page__item-main">
            <div class="public-complaint-track-page__item-meta">
              <span>ID: {{ item.complaintNo || "-" }}</span>
              <small>提交时间：{{ formatTime(item.createTime) }}</small>
            </div>
            <h3>{{ item.content || "未填写投诉内容摘要" }}</h3>
            <div class="public-complaint-track-page__item-status">
              <AppStatusTag :label="formatStatus(item.status)" :tone="statusTone(item.status)" />
              <span>{{ progressPercent(item.status) }}%</span>
            </div>
            <div class="public-complaint-track-page__progress">
              <span :style="{ width: `${progressPercent(item.status)}%` }" />
            </div>
          </div>
          <div class="public-complaint-track-page__item-side">
            <div class="public-complaint-track-page__latest">
              <span class="material-symbols-outlined" aria-hidden="true">info</span>
              <div>
                <strong>最近进展</strong>
                <p>{{ latestSummary(item) }}</p>
              </div>
            </div>
            <button type="button" @click="goDetail(item)">查看详情</button>
          </div>
        </article>
      </section>

      <section class="public-complaint-track-page__pager">
        <span>第 {{ page }} / {{ pages }} 页，共 {{ total }} 条</span>
        <div class="public-complaint-track-page__pager-actions">
          <button type="button" class="ghost" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
          <button type="button" class="ghost" :disabled="page >= pages" @click="changePage(page + 1)">下一页</button>
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
import { fetchMyComplaints, fetchMyComplaintStats } from "../../api/complaint";
import AppEmptyState from "../../components/common/AppEmptyState.vue";
import AppStatusTag from "../../components/common/AppStatusTag.vue";
import AppStatusToast from "../../components/common/AppStatusToast.vue";
import { getActiveSession } from "../../session/authRuntime";
import { formatTime } from "../../utils/formatters";
import { complaintStatusMap, formatStatusLabel, getStatusTone } from "../../utils/statusMaps";
import { getEmptyStateText, resolveErrorMessage } from "../../utils/uiFeedback";

const route = useRoute();
const router = useRouter();
const publicToken = getActiveSession()?.token || "";

const filters = reactive({ status: "", keyword: "" });
const loading = ref(false);
const records = ref([]);
const stats = ref({ totalCount: 0, processingCount: 0, finishedCount: 0 });
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "" });

const filteredRecords = computed(() => records.value);
const hasFilters = computed(() => Boolean(filters.status || filters.keyword.trim()));
const emptyTitle = computed(() => getEmptyStateText("投诉记录", hasFilters.value));
const emptyDescription = computed(() =>
  hasFilters.value ? "可以调整筛选条件后重新查询。" : "还没有投诉记录，提交后会在这里展示。"
);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatStatus(value) {
  return formatStatusLabel(value, complaintStatusMap);
}

function statusTone(value) {
  return getStatusTone(value, "COMPLAINT");
}

function progressPercent(value) {
  const map = {
    SUBMITTED: 15,
    PENDING: 30,
    ASSIGNED: 55,
    PROCESSING: 75,
    FEEDBACKED: 100,
    REJECTED: 100
  };
  return map[String(value || "").toUpperCase()] || 0;
}

function latestSummary(item) {
  const current = String(item?.status || "").toUpperCase();
  if (current === "FEEDBACKED") {
    return resolveFeedbackSummary(item) || "投诉已办结，处理结果已反馈。";
  }
  if (current === "REJECTED") {
    return resolveRejectReason(item) || "投诉已驳回，请补充材料后重新提交。";
  }
  if (current === "PROCESSING") return "监管人员正在核查处理中。";
  if (current === "ASSIGNED") return "案件已分派，等待执法人员办理。";
  if (current === "PENDING") return "投诉已完成受理，等待进一步分派。";
  return "投诉已提交，当前处于待受理状态。";
}

function resolveFeedbackSummary(item) {
  return item?.feedbackSummary || item?.handleResult || "";
}

function resolveRejectReason(item) {
  return item?.rejectReason || item?.handleResult || "";
}

function buildListQuery() {
  const query = {};
  if (filters.keyword.trim()) query.keyword = filters.keyword.trim();
  if (filters.status) query.status = filters.status;
  if (page.value > 1) query.page = String(page.value);
  return query;
}

function applyRouteQuery() {
  filters.keyword = typeof route.query.keyword === "string" ? route.query.keyword.trim() : "";
  filters.status = typeof route.query.status === "string" ? route.query.status : "";
  const nextPage = Number(route.query.page || 1);
  page.value = Number.isFinite(nextPage) && nextPage > 0 ? nextPage : 1;
}

function syncRouteQuery() {
  router.replace({ query: buildListQuery() }).catch(() => {});
}

function goDetail(item) {
  if (!item?.id) return;
  router.push({
    name: "public-complaint-detail",
    params: { complaintId: item.id },
    query: buildListQuery()
  }).catch(() => {});
}

async function loadComplaints() {
  loading.value = true;
  setStatus("");
  try {
    const params = {
      keyword: filters.keyword.trim() || undefined,
      status: filters.status,
      page: page.value,
      size: size.value
    };
    const [data, statsData] = await Promise.all([
      fetchMyComplaints(publicToken, params),
      fetchMyComplaintStats(publicToken, params)
    ]);
    stats.value = {
      totalCount: statsData?.totalCount || 0,
      processingCount: statsData?.processingCount || 0,
      finishedCount: statsData?.finishedCount || 0
    };
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    records.value = [];
    stats.value = { totalCount: 0, processingCount: 0, finishedCount: 0 };
    setStatus(resolveErrorMessage(error, "投诉记录加载失败，请稍后重试"), "error");
  } finally {
    loading.value = false;
  }
}

function applyFilters() {
  page.value = 1;
  syncRouteQuery();
  loadComplaints();
}

function resetFilters() {
  filters.status = "";
  filters.keyword = "";
  page.value = 1;
  syncRouteQuery();
  loadComplaints();
}

function changePage(next) {
  page.value = next;
  syncRouteQuery();
  loadComplaints();
}

onMounted(() => {
  applyRouteQuery();
  loadComplaints();
});

watch(
  () => [route.query.keyword, route.query.status, route.query.page],
  () => {
    const nextKeyword = typeof route.query.keyword === "string" ? route.query.keyword.trim() : "";
    const nextStatus = typeof route.query.status === "string" ? route.query.status : "";
    const nextPage = Number(route.query.page || 1);
    const normalizedPage = Number.isFinite(nextPage) && nextPage > 0 ? nextPage : 1;

    if (nextKeyword === filters.keyword && nextStatus === filters.status && normalizedPage === page.value) {
      return;
    }

    applyRouteQuery();
    loadComplaints();
  }
);
</script>

<style scoped>
.public-complaint-track-page { min-height: 100vh; background: var(--surface); }
.public-complaint-track-page__topbar { position: sticky; top: 0; z-index: 40; border-bottom: 1px solid rgba(195,198,211,.4); background: rgba(248,250,253,.84); }
.public-complaint-track-page__topbar-inner { max-width: 1680px; margin: 0 auto; min-height: var(--public-topbar-min-h); padding: 0 16px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.public-complaint-track-page__brand-nav { display: flex; align-items: center; gap: 24px; }
.public-complaint-track-page__brand { font-family: var(--font-display); font-size: var(--public-brand-size); font-weight: 800; color: var(--primary); }
.public-complaint-track-page__nav { display: flex; gap: 18px; }
.public-complaint-track-page__nav-item { border: none; background: transparent; min-height: var(--public-topbar-min-h); color: var(--on-surface-variant); font-size: var(--public-nav-size); font-weight: 700; border-bottom: 2px solid transparent; cursor: pointer; }
.public-complaint-track-page__nav-item.is-active { color: var(--primary); border-bottom-color: var(--primary); }
.public-complaint-track-page__toolbar { display: flex; align-items: center; gap: 10px; }
.public-complaint-track-page__search-box { display: inline-flex; align-items: center; gap: 6px; border-radius: 8px; border: 1px solid rgba(195,198,211,.44); background: rgba(255,255,255,.75); padding: 0 14px; min-height: var(--public-toolbar-min-h); }
.public-complaint-track-page__search-box input { border: none; background: transparent; font-size: var(--public-toolbar-input-size); min-width: 220px; }
.public-complaint-track-page__account { min-height: var(--public-toolbar-min-h); margin: 0; padding-inline: 12px; }
.public-complaint-track-page__account .material-symbols-outlined { font-size: 22px; }
.public-complaint-track-page__logout { min-height: var(--public-toolbar-min-h); font-size: var(--public-logout-font-size); margin: 0; }
.public-complaint-track-page__main { max-width: 1680px; margin: 0 auto; padding: 24px 16px 48px; display: grid; gap: 14px; }
.public-complaint-track-page__head h1 { margin: 0 0 6px; color: var(--primary); font-family: var(--font-display); font-size: var(--public-hero-title-alt); line-height: 1; }
.public-complaint-track-page__head p { margin: 0; font-size: var(--public-body-secondary); color: var(--on-surface-variant); }
.public-complaint-track-page__stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.public-complaint-track-page__stat-card { border: 1px solid rgba(195,198,211,.32); border-radius: 10px; background: var(--surface-container-lowest); padding: 12px; display: grid; gap: 6px; border-bottom: 2px solid rgba(70,89,231,.28); }
.public-complaint-track-page__stat-card span { font-size: var(--public-overline); color: var(--on-surface-variant); letter-spacing: .06em; text-transform: uppercase; }
.public-complaint-track-page__stat-card strong { font-size: var(--public-stat-number); color: var(--primary); line-height: 1; font-family: var(--font-display); }
.public-complaint-track-page__stat-card.is-processing { border-bottom-color: rgba(210,122,0,.5); }
.public-complaint-track-page__stat-card.is-finished { border-bottom-color: rgba(33,156,84,.5); }
.public-complaint-track-page__create-btn { border: none; border-radius: 10px; background: linear-gradient(135deg,#002660 0%,#003a8c 100%); color: #fff; display: grid; place-items: center; gap: 4px; font-size: var(--public-caption); font-weight: 700; cursor: pointer; padding: 10px; }
.public-complaint-track-page__filters { border: 1px solid rgba(195,198,211,.3); border-radius: 10px; background: var(--surface-container-lowest); padding: 10px 12px; display: flex; gap: 10px; align-items: end; flex-wrap: wrap; }
.public-complaint-track-page__filters label { display: grid; gap: 6px; min-width: 200px; }
.public-complaint-track-page__filters span { font-size: var(--public-overline); color: var(--on-surface-variant); }
.public-complaint-track-page__filters select { min-height: var(--public-dense-control-min-h); border: 1px solid rgba(195,198,211,.6); border-radius: 7px; padding: 0 10px; font-size: var(--public-caption); background: #fff; }
.public-complaint-track-page__filter-actions { display: inline-flex; gap: 8px; }
.public-complaint-track-page__filter-actions button { min-height: var(--public-dense-control-min-h); border-radius: 7px; border: 1px solid transparent; background: var(--primary); color: #fff; font-size: var(--public-caption); font-weight: 700; padding: 0 12px; cursor: pointer; }
.public-complaint-track-page__filter-actions .ghost { background: #fff; color: var(--on-surface-variant); border-color: rgba(195,198,211,.6); }
.public-complaint-track-page__list { display: grid; gap: 14px; }
.public-complaint-track-page__item { border: 1px solid rgba(195,198,211,.32); border-radius: 10px; background: var(--surface-container-lowest); padding: 16px 14px; display: grid; grid-template-columns: minmax(0, 1fr) minmax(220px, .38fr); gap: 14px; }
.public-complaint-track-page__item-meta { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.public-complaint-track-page__item-meta span { font-size: var(--public-table-head-overline); font-weight: 700; letter-spacing: .06em; text-transform: uppercase; color: var(--primary); background: rgba(70,89,231,.12); border-radius: 6px; padding: 2px 7px; }
.public-complaint-track-page__item-meta small { font-size: var(--public-overline); color: var(--on-surface-variant); }
.public-complaint-track-page__item h3 { margin: 10px 0 14px; font-size: var(--public-body); color: var(--on-surface); line-height: 1.5; }
.public-complaint-track-page__item-status { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.public-complaint-track-page__item-status span { font-size: var(--public-overline); color: var(--on-surface-variant); font-weight: 700; }
.public-complaint-track-page__progress { height: 6px; border-radius: 999px; background: rgba(195,198,211,.45); overflow: hidden; }
.public-complaint-track-page__progress span { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg,#003a8c 0%,#335bae 100%); }
.public-complaint-track-page__item-side { display: grid; gap: 10px; align-content: start; }
.public-complaint-track-page__latest { background: var(--surface-container-low); border-radius: 8px; padding: 10px; display: flex; align-items: flex-start; gap: 8px; }
.public-complaint-track-page__latest .material-symbols-outlined { font-size: var(--public-code-strong); color: var(--primary); margin-top: 1px; }
.public-complaint-track-page__latest strong { display: block; font-size: var(--public-overline); margin-bottom: 3px; }
.public-complaint-track-page__latest p { margin: 0; font-size: var(--public-overline); color: var(--on-surface-variant); line-height: 1.55; }
.public-complaint-track-page__item-side button { min-height: var(--public-dense-control-min-h); border-radius: 7px; border: 1px solid rgba(195,198,211,.58); background: #fff; color: var(--primary); font-size: var(--public-caption); font-weight: 700; cursor: pointer; }
.public-complaint-track-page__pager { border-top: 1px solid rgba(195,198,211,.4); padding-top: 10px; display: flex; justify-content: space-between; align-items: center; gap: 10px; color: var(--on-surface-variant); font-size: var(--public-pager); line-height: 1.5; flex-wrap: wrap; }
.public-complaint-track-page__pager-actions { display: inline-flex; gap: 8px; }
.public-complaint-track-page__pager-actions .ghost { min-height: var(--public-chip-min-h); border-radius: 7px; border: 1px solid rgba(195,198,211,.58); background: #fff; color: var(--on-surface-variant); padding: 0 10px; font-size: var(--public-caption); cursor: pointer; }
.public-complaint-track-page__pager-actions .ghost:disabled { opacity: .55; cursor: not-allowed; }
.public-complaint-track-page__empty { padding-top: 28px; padding-bottom: 28px; }
@media (max-width: 1200px) { .public-complaint-track-page__item { grid-template-columns: 1fr; } .public-complaint-track-page__stats { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 1100px) { .public-complaint-track-page__nav { display: none; } }
@media (max-width: 760px) { .public-complaint-track-page__toolbar { display: none; } .public-complaint-track-page__head h1 { font-size: var(--public-page-title-xs); } .public-complaint-track-page__stats { grid-template-columns: 1fr; } }
</style>




