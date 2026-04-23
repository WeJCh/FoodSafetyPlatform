<template>
  <div class="public-complaint-track-page">
    <header class="public-complaint-track-page__topbar">
      <div class="public-complaint-track-page__topbar-inner">
        <div class="public-complaint-track-page__brand-nav">
          <span class="public-complaint-track-page__brand">食品安全监管平台</span>
          <nav class="public-complaint-track-page__nav" aria-label="公众导航">
            <button
              v-for="item in topNavItems"
              :key="item.key"
              type="button"
              class="public-complaint-track-page__nav-item"
              :class="{ 'is-active': item.key === 'complaints' }"
              @click="goTo(item.routeName)"
            >
              {{ item.label }}
            </button>
          </nav>
        </div>
        <div class="public-complaint-track-page__toolbar">
          <label class="public-complaint-track-page__search-box">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <input
              v-model.trim="filters.keyword"
              type="text"
              placeholder="搜索编号、企业、关键词"
              @keyup.enter="applyFilters"
            />
          </label>
          <button type="button" class="public-complaint-track-page__icon-btn" @click="onFeaturePending('通知中心')">
            <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
          </button>
          <button type="button" class="public-complaint-track-page__icon-btn" @click="onFeaturePending('个人中心')">
            <span class="material-symbols-outlined" aria-hidden="true">account_circle</span>
          </button>
          <button type="button" class="ghost public-complaint-track-page__logout" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="public-complaint-track-page__main">
      <section class="public-complaint-track-page__head">
        <h1>我的投诉追踪</h1>
        <p>在这里查看您提交的投诉记录及实时处理进展。</p>
      </section>

      <section class="public-complaint-track-page__stats">
        <article class="public-complaint-track-page__stat-card">
          <span>全部投诉</span>
          <strong>{{ statsRecords.length }}</strong>
        </article>
        <article class="public-complaint-track-page__stat-card is-processing">
          <span>处理中</span>
          <strong>{{ processingCount }}</strong>
        </article>
        <article class="public-complaint-track-page__stat-card is-finished">
          <span>已完结</span>
          <strong>{{ finishedCount }}</strong>
        </article>
        <button type="button" class="public-complaint-track-page__create-btn" @click="goTo('public-complaint-create')">
          <span class="material-symbols-outlined" aria-hidden="true">add_moderator</span>
          发起新投诉
        </button>
      </section>

      <section class="public-complaint-track-page__filters">
        <label>
          <span>状态筛选</span>
          <select v-model="filters.status">
            <option value="">全部</option>
            <option value="SUBMITTED">已提交</option>
            <option value="PENDING">已受理</option>
            <option value="ASSIGNED">已派发</option>
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
        <article v-if="!filteredRecords.length" class="public-complaint-track-page__empty">
          <span class="material-symbols-outlined" aria-hidden="true">search_off</span>
          <h3>暂无投诉记录</h3>
          <p>可先发起一条投诉，后续在这里持续追踪办理进度。</p>
          <button type="button" @click="goTo('public-complaint-create')">发起投诉</button>
        </article>

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
            <h3>{{ item.content || "投诉内容待补充" }}</h3>
            <div class="public-complaint-track-page__item-status">
              <i :class="`is-${statusClass(item.status)}`">{{ formatStatus(item.status) }}</i>
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
                <strong>最新动态</strong>
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

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMyComplaints } from "../../api/complaint";
import { getActiveSession, performLogout } from "../../session/authRuntime";
import { formatTime } from "../../utils/formatters";

const router = useRouter();
const publicToken = getActiveSession()?.token || "";

const topNavItems = [
  { key: "home", label: "首页", routeName: "public-home" },
  { key: "bulletins", label: "监管公告", routeName: "public-bulletins" },
  { key: "enterprises", label: "企业公示", routeName: "public-enterprises" },
  { key: "sampling", label: "抽检结果", routeName: "public-sampling-results" },
  { key: "complaint-create", label: "我要投诉", routeName: "public-complaint-create" },
  { key: "complaints", label: "我的投诉", routeName: "public-complaints" }
];

const filters = reactive({ status: "", keyword: "" });
const loading = ref(false);
const records = ref([]);
const statsRecords = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const status = reactive({ message: "", type: "" });

const filteredRecords = computed(() => records.value);
const processingCount = computed(() =>
  statsRecords.value.filter((item) => ["PENDING", "ASSIGNED", "PROCESSING"].includes(item.status)).length
);
const finishedCount = computed(() =>
  statsRecords.value.filter((item) => ["FEEDBACKED", "REJECTED"].includes(item.status)).length
);

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}
function goTo(name) {
  router.push({ name }).catch(() => {});
}
function onFeaturePending(name) {
  window.alert(`${name} 功能待后续完善`);
}
async function handleLogout() {
  await performLogout();
  router.replace({ name: "login" }).catch(() => {});
}
function formatStatus(value) {
  const map = {
    SUBMITTED: "已提交",
    PENDING: "已受理",
    ASSIGNED: "已派发",
    PROCESSING: "处理中",
    FEEDBACKED: "已反馈",
    REJECTED: "已驳回"
  };
  return map[value] || value || "-";
}
function statusClass(value) {
  if (value === "FEEDBACKED") return "success";
  if (value === "REJECTED") return "danger";
  if (value === "PROCESSING" || value === "ASSIGNED" || value === "PENDING") return "processing";
  return "default";
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
  return map[value] || 0;
}
function latestSummary(item) {
  if (item?.status === "FEEDBACKED") return resolveFeedbackSummary(item) || "案件已办结并完成结果反馈。";
  if (item?.status === "REJECTED") return resolveRejectReason(item) || "投诉已驳回，请补充材料后重提。";
  if (item?.status === "PROCESSING") return "监管人员正在核查处理中。";
  if (item?.status === "ASSIGNED") return "案件已派发，等待执法人员办理。";
  if (item?.status === "PENDING") return "投诉已受理，等待分派。";
  return "投诉已提交，等待平台受理。";
}
function resolveFeedbackSummary(item) {
  return item?.feedbackSummary || item?.handleResult || "";
}
function resolveRejectReason(item) {
  return item?.rejectReason || item?.handleResult || "";
}
function goDetail(item) {
  if (!item?.id) return;
  router.push({ name: "public-complaint-detail", params: { complaintId: item.id } }).catch(() => {});
}
async function loadComplaints() {
  loading.value = true;
  setStatus("");
  try {
    const keyword = filters.keyword.trim().toLowerCase();
    if (keyword) {
      const allRecords = await fetchAllComplaintRecords();
      const matchedRecords = allRecords.filter((item) => {
        const source = [item.complaintNo, item.enterpriseName, item.content, item.handleResult]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();
        return source.includes(keyword);
      });
      statsRecords.value = matchedRecords;
      total.value = matchedRecords.length;
      pages.value = Math.max(1, Math.ceil(total.value / size.value));
      if (page.value > pages.value) page.value = pages.value;
      const start = (page.value - 1) * size.value;
      records.value = matchedRecords.slice(start, start + size.value);
      return;
    }

    const data = await fetchMyComplaints(publicToken, {
      status: filters.status,
      page: page.value,
      size: size.value
    });
    statsRecords.value = await fetchAllComplaintRecords();
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    records.value = [];
    statsRecords.value = [];
    setStatus(error.message || "加载投诉列表失败", "error");
  } finally {
    loading.value = false;
  }
}

async function fetchAllComplaintRecords() {
  const merged = [];
  let currentPage = 1;
  const batchSize = 50;
  let totalPages = 1;

  do {
    const data = await fetchMyComplaints(publicToken, {
      status: filters.status,
      page: currentPage,
      size: batchSize
    });
    merged.push(...(data.records || []));
    totalPages = Math.max(1, data.pages || 1);
    currentPage += 1;
  } while (currentPage <= totalPages);

  return merged;
}
function applyFilters() {
  page.value = 1;
  loadComplaints();
}
function resetFilters() {
  filters.status = "";
  filters.keyword = "";
  page.value = 1;
  loadComplaints();
}
function changePage(next) {
  page.value = next;
  loadComplaints();
}

onMounted(loadComplaints);
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
.public-complaint-track-page__icon-btn { width: var(--public-btn-compact-min-h); height: var(--public-btn-compact-min-h); border-radius: 8px; border: 1px solid transparent; background: transparent; color: var(--on-surface-variant); cursor: pointer; }
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
.public-complaint-track-page__item-status i { font-style: normal; font-size: var(--public-overline); font-weight: 700; min-height: 24px; padding: 0 8px; border-radius: 999px; border: 1px solid transparent; display: inline-flex; align-items: center; }
.public-complaint-track-page__item-status i.is-default { background: rgba(80,95,128,.12); color: #3f4c66; border-color: rgba(80,95,128,.22); }
.public-complaint-track-page__item-status i.is-processing { background: rgba(210,122,0,.12); color: #9b5b00; border-color: rgba(210,122,0,.22); }
.public-complaint-track-page__item-status i.is-success { background: rgba(33,156,84,.12); color: #1f6e45; border-color: rgba(33,156,84,.22); }
.public-complaint-track-page__item-status i.is-danger { background: rgba(186,26,26,.12); color: #93000a; border-color: rgba(186,26,26,.22); }
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
.public-complaint-track-page__empty { border: 1px dashed rgba(195,198,211,.7); border-radius: 10px; background: rgba(248,250,253,.75); padding: 28px 16px; display: grid; place-items: center; text-align: center; gap: 6px; }
.public-complaint-track-page__empty .material-symbols-outlined { font-size: var(--public-icon-xl); color: #64749a; }
.public-complaint-track-page__empty h3 { margin: 0; font-size: var(--public-lead); color: var(--primary); }
.public-complaint-track-page__empty p { margin: 0; font-size: var(--public-caption); color: var(--on-surface-variant); }
.public-complaint-track-page__empty button { margin-top: 4px; min-height: var(--public-btn-compact-min-h); border-radius: 7px; border: none; background: var(--primary); color: #fff; font-size: var(--public-btn); font-weight: 700; padding: 0 12px; cursor: pointer; }
@media (max-width: 1200px) { .public-complaint-track-page__item { grid-template-columns: 1fr; } .public-complaint-track-page__stats { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 1100px) { .public-complaint-track-page__nav { display: none; } }
@media (max-width: 760px) {
  .public-complaint-track-page__toolbar { display: none; }
  .public-complaint-track-page__head h1 { font-size: var(--public-page-title-xs); }
  .public-complaint-track-page__stats { grid-template-columns: 1fr; }
}
</style>
