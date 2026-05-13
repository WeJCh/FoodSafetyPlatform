<template>
  <RegulatorAdminWorkspacePage
    active-key="complaints"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="complaint-page">
      <header class="complaint-page__head">
        <div>
          <h1>{{ uiText.title }}</h1>
          <p>{{ uiText.subtitle }}</p>
        </div>
      </header>

      <section class="stats-grid">
        <article class="stat-card stat-card--primary">
          <span>{{ uiText.submittedTitle }}</span>
          <strong>{{ summary.submittedCount }}</strong>
          <p>{{ uiText.submittedHint }}</p>
        </article>
        <article class="stat-card">
          <span>{{ uiText.pendingTitle }}</span>
          <strong>{{ summary.pendingCount }}</strong>
          <p>{{ uiText.pendingHint }}</p>
        </article>
        <article class="stat-card">
          <span>{{ uiText.processingTitle }}</span>
          <strong>{{ activeProcessingCount }}</strong>
          <p>{{ uiText.processingHint }}</p>
        </article>
        <article class="stat-card">
          <span>{{ uiText.doneTitle }}</span>
          <strong>{{ summary.doneCount }}</strong>
          <p>{{ uiText.doneHint }}</p>
        </article>
      </section>

      <section class="filter-card">
        <div class="filter-toolbar">
          <div class="filter-grid">
            <label>
              {{ uiText.typeFilterLabel }}
              <select v-model="filters.complaintType">
                <option value="">{{ uiText.allTypeOption }}</option>
                <option v-for="option in complaintTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label>
              {{ uiText.statusFilterLabel }}
              <select v-model="filters.status">
                <option value="">{{ uiText.allStatusOption }}</option>
                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>
          <div class="filter-actions">
            <button class="primary" type="button" :disabled="loading" @click="handleSearch">
              {{ loading ? uiText.searching : uiText.search }}
            </button>
            <button class="ghost" type="button" :disabled="loading" @click="resetFilters">{{ uiText.reset }}</button>
          </div>
        </div>
      </section>

      <section class="table-card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ uiText.columns.time }}</th>
                <th>{{ uiText.columns.no }}</th>
                <th>{{ uiText.columns.enterprise }}</th>
                <th>{{ uiText.columns.summary }}</th>
                <th>{{ uiText.columns.assignee }}</th>
                <th>{{ uiText.columns.status }}</th>
                <th class="th-right">{{ uiText.columns.action }}</th>
              </tr>
            </thead>
            <tbody v-if="records.length">
              <tr v-for="item in records" :key="item.id">
                <td>{{ formatTime(item.createTime || item.updateTime) }}</td>
                <td class="mono">{{ item.complaintNo || "-" }}</td>
                <td class="strong">{{ item.enterpriseName || "-" }}</td>
                <td class="summary" :title="item.content || '-'">{{ summaryText(item.content) }}</td>
                <td>
                  <span v-if="item.assignedToName">{{ item.assignedToName }}</span>
                  <span v-else class="muted">{{ uiText.unassigned }}</span>
                </td>
                <td>
                  <span class="status-pill" :class="statusClass(item.status)">
                    {{ formatComplaintStatus(item.status) }}
                  </span>
                </td>
                <td class="td-right">
                  <div class="action-row">
                    <button class="ghost" type="button" @click="handleViewDetail(item)">{{ uiText.detail }}</button>
                    <button
                      v-if="item.status === 'SUBMITTED'"
                      class="primary"
                      type="button"
                      :disabled="loading"
                      @click="handleAccept(item)"
                    >
                      {{ uiText.accept }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!records.length" class="empty">{{ uiText.empty }}</div>
        </div>
        <div class="pager">
          <span>{{ uiText.totalLabel(total, page, pages) }}</span>
          <div class="pager-actions">
            <button class="ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">
              {{ uiText.prev }}
            </button>
            <button class="ghost" type="button" :disabled="page >= pages || loading" @click="changePage(page + 1)">
              {{ uiText.next }}
            </button>
          </div>
        </div>
      </section>

      <section class="stats-bento">
        <article class="progress-card">
          <h3>{{ uiText.progressTitle }}</h3>
          <div class="flow-track">
            <div v-for="item in progressStats" :key="item.key" class="flow-node">
              <span class="dot">{{ item.count }}</span>
              <em>{{ item.label }}</em>
            </div>
          </div>
        </article>
      </section>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { acceptComplaint, fetchComplaintStatsOverview, fetchComplaints } from "../../api/complaint";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatTime } from "../../utils/formatters";
import { complaintStatusMap } from "../../utils/statusMaps";
import { complaintTypeOptions } from "../../utils/complaintTypes";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const status = reactive({ message: "", type: "" });
const loading = ref(false);
const records = ref([]);
const page = ref(1);
const size = ref(8);
const total = ref(0);
const pages = ref(1);
const summary = reactive({
  submittedCount: 0,
  pendingCount: 0,
  assignedCount: 0,
  processingCount: 0,
  feedbackedCount: 0,
  rejectedCount: 0,
  todoCount: 0,
  doneCount: 0,
  overdueCount: 0
});
const filters = reactive({
  complaintType: "",
  status: ""
});

const statusOptions = [
  { value: "SUBMITTED", label: "待受理" },
  { value: "PENDING", label: "待分派" },
  { value: "ASSIGNED", label: "已分派" },
  { value: "PROCESSING", label: "处理中" },
  { value: "FEEDBACKED", label: "已反馈" },
  { value: "REJECTED", label: "已驳回" }
];

const uiText = {
  title: "投诉流转中心",
  subtitle: "统一查看投诉受理、分派、处理和反馈进度。",
  submittedTitle: "待受理",
  submittedHint: "公众提交后尚未受理的投诉",
  pendingTitle: "待分派",
  pendingHint: "已受理但尚未分派的投诉",
  processingTitle: "处理中",
  processingHint: "已分派或正在办理的投诉",
  doneTitle: "已办结",
  doneHint: "已反馈或已驳回的投诉",
  typeFilterLabel: "投诉类别",
  allTypeOption: "全部类别",
  statusFilterLabel: "当前状态",
  allStatusOption: "全部状态",
  search: "执行筛选",
  searching: "查询中...",
  reset: "重置",
  columns: {
    time: "投诉时间",
    no: "编号",
    enterprise: "涉及企业",
    summary: "投诉摘要",
    assignee: "责任人",
    status: "状态",
    action: "操作"
  },
  unassigned: "未分配",
  detail: "详情",
  accept: "受理",
  empty: "暂无投诉记录",
  prev: "上一页",
  next: "下一页",
  progressTitle: "流转进度分布",
  totalLabel(totalCount, currentPage, totalPages) {
    return `共 ${totalCount} 条，${currentPage}/${totalPages} 页`;
  }
};

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function formatComplaintStatus(value) {
  return complaintStatusMap[value] || value || "-";
}

function summaryText(content) {
  const text = String(content || "").trim();
  if (!text) return "-";
  if (text.length <= 26) return text;
  return `${text.slice(0, 26)}...`;
}

function statusClass(value) {
  if (value === "SUBMITTED") return "is-submitted";
  if (value === "PENDING") return "is-pending";
  if (value === "ASSIGNED") return "is-assigned";
  if (value === "PROCESSING") return "is-processing";
  if (value === "FEEDBACKED") return "is-feedbacked";
  if (value === "REJECTED") return "is-rejected";
  return "";
}

const activeProcessingCount = computed(() => Number(summary.assignedCount || 0) + Number(summary.processingCount || 0));

const progressStats = computed(() => ([
  { key: "SUBMITTED", label: "待受理", count: Number(summary.submittedCount || 0) },
  { key: "PENDING", label: "待分派", count: Number(summary.pendingCount || 0) },
  { key: "ASSIGNED", label: "分派", count: Number(summary.assignedCount || 0) },
  { key: "PROCESSING", label: "处理", count: Number(summary.processingCount || 0) },
  { key: "FEEDBACKED", label: "反馈", count: Number(summary.feedbackedCount || 0) }
]));

async function loadSummary() {
  const data = await fetchComplaintStatsOverview(token.value);
  summary.submittedCount = Number(data?.submittedCount || 0);
  summary.pendingCount = Number(data?.pendingCount || 0);
  summary.assignedCount = Number(data?.assignedCount || 0);
  summary.processingCount = Number(data?.processingCount || 0);
  summary.feedbackedCount = Number(data?.feedbackedCount || 0);
  summary.rejectedCount = Number(data?.rejectedCount || 0);
  summary.todoCount = Number(data?.todoCount || 0);
  summary.doneCount = Number(data?.doneCount || 0);
  summary.overdueCount = Number(data?.overdueCount || 0);
}

async function loadComplaints() {
  loading.value = true;
  setStatus("");
  try {
    const [data] = await Promise.all([
      fetchComplaints(token.value, {
        complaintType: filters.complaintType,
        status: filters.status,
        page: page.value,
        size: size.value
      }),
      loadSummary()
    ]);
    records.value = data.records || [];
    total.value = data.total || 0;
    page.value = data.page || 1;
    size.value = data.size || size.value;
    pages.value = data.pages || 1;
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载投诉列表失败"), "error");
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  page.value = 1;
  await loadComplaints();
}

function resetFilters() {
  filters.complaintType = "";
  filters.status = "";
  handleSearch();
}

async function changePage(nextPage) {
  page.value = nextPage;
  await loadComplaints();
}

function handleViewDetail(item) {
  if (!item?.id) return;
  router.push({ name: "regulator-admin-complaint-detail", params: { complaintId: item.id } }).catch(() => {});
}

async function handleAccept(item) {
  if (!item?.id) return;
  loading.value = true;
  setStatus("");
  try {
    await acceptComplaint(token.value, item.id);
    setStatus("投诉已受理", "success");
    await loadComplaints();
  } catch (error) {
    setStatus(resolveErrorMessage(error, "投诉受理失败"), "error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadComplaints);
</script>

<style scoped>
.complaint-page { display: grid; gap: 16px; }
.complaint-page__head h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 900; letter-spacing: -0.02em; }
.complaint-page__head p { margin: 6px 0 0; color: #64748b; font-size: 14px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.stat-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 14px; }
.stat-card span { color: #64748b; font-size: 11px; font-weight: 700; text-transform: uppercase; }
.stat-card strong { display: block; margin-top: 8px; font-size: 30px; color: #0f172a; }
.stat-card p { margin: 6px 0 0; color: #64748b; font-size: 12px; line-height: 1.5; }
.stat-card--primary { background: linear-gradient(120deg, #002660, #003a8c); border-color: #002660; }
.stat-card--primary span, .stat-card--primary strong, .stat-card--primary p { color: #fff; }
.filter-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 14px; }
.filter-toolbar { display: flex; align-items: stretch; gap: 14px; flex-wrap: wrap; }
.filter-grid { flex: 1 1 540px; min-width: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 14px; padding: 6px 2px; }
.filter-grid label { display: grid; gap: 7px; font-size: 12px; font-weight: 800; color: #475569; }
.filter-grid input, .filter-grid select { border: 1px solid #d6dee8; background: #fff; border-radius: 8px; min-height: 38px; padding: 0 12px; font-size: 13px; color: #0f172a; }
.filter-actions { flex: 0 0 auto; display: flex; align-items: center; gap: 8px; padding: 6px 2px; }
.primary { border: 0; background: #002660; color: #fff; border-radius: 8px; padding: 10px 18px; font-size: 13px; font-weight: 700; cursor: pointer; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #334155; border-radius: 8px; padding: 10px 16px; font-size: 13px; cursor: pointer; }
.table-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; }
.table-wrap { overflow: auto; }
table { width: 100%; min-width: 1080px; border-collapse: collapse; }
thead tr { background: #e6e8eb; }
th { text-align: left; padding: 12px 14px; font-size: 11px; font-weight: 900; letter-spacing: 0.05em; text-transform: uppercase; color: #64748b; }
td { padding: 14px; border-top: 1px solid #f1f5f9; font-size: 13px; color: #1e293b; }
tbody tr:nth-child(even) { background: #f8fafc; }
tbody tr:hover { background: #f1f5f9; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; color: #1e3a8a; font-weight: 700; }
.strong { font-weight: 700; color: #0f172a; }
.summary { max-width: 280px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.muted { color: #94a3b8; font-style: italic; }
.th-right, .td-right { text-align: right; }
.action-row { display: inline-flex; align-items: center; gap: 8px; }
.status-pill { display: inline-flex; align-items: center; justify-content: center; min-height: 24px; padding: 0 10px; border-radius: 999px; font-size: 10px; font-weight: 800; border: 1px solid transparent; }
.status-pill.is-submitted { color: #1e3a8a; background: #dbeafe; border-color: #bfdbfe; }
.status-pill.is-pending { color: #9a3412; background: #ffedd5; border-color: #fed7aa; }
.status-pill.is-assigned { color: #0f766e; background: #ccfbf1; border-color: #99f6e4; }
.status-pill.is-processing { color: #7c2d12; background: #ffedd5; border-color: #fed7aa; }
.status-pill.is-feedbacked { color: #166534; background: #dcfce7; border-color: #bbf7d0; }
.status-pill.is-rejected { color: #991b1b; background: #fee2e2; border-color: #fecaca; }
.empty { padding: 18px; text-align: center; color: #64748b; font-size: 13px; }
.pager { display: flex; justify-content: space-between; align-items: center; gap: 10px; padding: 12px 14px; border-top: 1px solid #e2e8f0; color: #64748b; font-size: 12px; font-weight: 700; }
.pager-actions { display: flex; gap: 8px; }
.stats-bento { display: grid; grid-template-columns: minmax(0, 1fr); gap: 12px; }
.progress-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; }
.progress-card h3 { margin: 0 0 14px; font-size: 15px; color: #002660; }
.flow-track { display: flex; justify-content: space-between; gap: 8px; position: relative; padding: 12px 0 0; }
.flow-track::before { content: ""; position: absolute; top: 27px; left: 0; right: 0; height: 2px; background: #e2e8f0; }
.flow-node { position: relative; z-index: 1; display: grid; justify-items: center; gap: 6px; flex: 1; }
.dot { width: 34px; height: 34px; border-radius: 50%; background: #002660; color: #fff; font-size: 12px; font-weight: 800; display: grid; place-items: center; }
.flow-node em { font-style: normal; font-size: 11px; color: #334155; font-weight: 700; }
.status { position: fixed; right: 18px; bottom: 18px; border-radius: 8px; padding: 10px 12px; color: #fff; background: #0f172a; font-size: 13px; z-index: 1200; }
.status.error { background: #b91c1c; }
.status.success { background: #166534; }
@media (max-width: 760px) {
  .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .filter-grid { grid-template-columns: 1fr; }
  .filter-actions { justify-content: stretch; width: 100%; }
  .filter-actions button { flex: 1; }
}
</style>
