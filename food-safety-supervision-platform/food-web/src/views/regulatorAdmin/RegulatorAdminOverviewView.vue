<template>
  <RegulatorAdminWorkspacePage
    active-key="overview"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="overview-page">
      <header class="overview-page__header">
        <h1>监管总览</h1>
        <p>聚合辖区企业、检查、投诉、整改和预警动态，便于区域管理员统一掌握处置进度。</p>
      </header>

      <div class="metric-grid">
        <article class="metric-card">
          <p>企业总数</p>
          <strong>{{ enterpriseTotal }}</strong>
          <span>已通过备案 {{ approvedEnterpriseCount }} 家</span>
        </article>
        <article class="metric-card">
          <p>检查任务</p>
          <strong>{{ inspectionTaskTotal }}</strong>
          <span>进行中 {{ inProgressInspectionTasks }} 项</span>
        </article>
        <article class="metric-card">
          <p>待处理投诉</p>
          <strong>{{ complaintTodoCount }}</strong>
          <span>已反馈 {{ complaintDoneCount }} 件</span>
        </article>
        <article class="metric-card metric-card--danger">
          <p>活跃预警</p>
          <strong>{{ warningOpenCount }}</strong>
          <span>处理中 {{ warningProcessingCount }} 条</span>
        </article>
      </div>

      <div class="overview-content">
        <section class="panel">
          <div class="panel__head">
            <h2>待办入口</h2>
          </div>
          <div class="todo-grid">
            <button type="button" class="todo-item" @click="handleSidebarNavigate('approvals')">
              <span class="material-symbols-outlined">fact_check</span>
              <div>
                <strong>{{ pendingEnterpriseCount }} 家待审核企业</strong>
                <p>进入企业审核，处理备案和资料补充。</p>
              </div>
            </button>
            <button type="button" class="todo-item" @click="handleSidebarNavigate('complaints')">
              <span class="material-symbols-outlined">assignment_return</span>
              <div>
                <strong>{{ complaintTodoCount }} 件待处理投诉</strong>
                <p>受理、分派和跟进投诉处理进度。</p>
              </div>
            </button>
            <button type="button" class="todo-item" @click="handleSidebarNavigate('rectification')">
              <span class="material-symbols-outlined">rule</span>
              <div>
                <strong>{{ rectificationTodoCount }} 个待跟进整改</strong>
                <p>查看企业整改提交、确认和退回情况。</p>
              </div>
            </button>
            <button type="button" class="todo-item todo-item--danger" @click="handleSidebarNavigate('warnings')">
              <span class="material-symbols-outlined">notification_important</span>
              <div>
                <strong>{{ warningOpenCount }} 条预警待关注</strong>
                <p>进入预警中心，查看分派和处置状态。</p>
              </div>
            </button>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head">
            <h2>最近动态</h2>
          </div>
          <div v-if="!taskFeeds.length" class="empty">当前暂无可回溯的最近动态。</div>
          <div v-else class="feed-list">
            <article v-for="item in taskFeeds" :key="item.id" class="feed-item">
              <div>
                <span class="feed-tag" :class="`is-${item.tone}`">{{ item.tag }}</span>
                <strong>{{ item.title }}</strong>
                <p>{{ item.meta }}</p>
              </div>
              <button type="button" @click="openTaskFeed(item)">{{ item.action }}</button>
            </article>
          </div>
        </section>
      </div>

      <div class="aside-grid">
        <section class="quick-panel">
          <h3>快捷操作</h3>
          <div class="quick-grid">
            <button type="button" @click="handleSidebarNavigate('approvals')">企业审核</button>
            <button type="button" @click="handleSidebarNavigate('dispatch')">检查派发</button>
            <button type="button" @click="handleSidebarNavigate('bulletins')">公告管理</button>
            <button type="button" @click="handleSidebarNavigate('stats')">统计分析</button>
          </div>
        </section>

        <section class="trend-panel">
          <h3>当前态势</h3>
          <div class="trend-row">
            <span>预警处理进度</span>
            <strong>OPEN {{ warningOpenCount }} / PROCESSING {{ warningProcessingCount }}</strong>
          </div>
          <div class="trend-row">
            <span>投诉处理效率</span>
            <strong>{{ complaintEfficiency }}%</strong>
          </div>
          <div class="trend-row">
            <span>待跟进整改</span>
            <strong>{{ rectificationTodoCount }} 个</strong>
          </div>
        </section>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchComplaintStatsOverview, fetchRecentComplaintLogs } from "../../api/complaint";
import {
  fetchWarningRecordDetail,
  fetchPendingEnterprises,
  fetchRecentBulletinAuditLogs,
  fetchRecentEnterpriseAuditLogs,
  fetchRecentWarningLogs,
  fetchWarningRecords,
  fetchEnterprises
} from "../../api/regulation";
import {
  fetchInspectionTasks,
  fetchRecentOperationAuditLogs,
  fetchRecentRectificationActions,
  fetchRectifications
} from "../../api/regulationOperation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import { formatComplaintAuditSummary } from "../../utils/complaintAudit";
import { formatTime } from "../../utils/formatters";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import { useRegulatorAdminShellSession } from "./regulatorAdminShared";

const router = useRouter();
const { regulatorUser, token, handleSidebarNavigate, handleLogout } = useRegulatorAdminShellSession();

const status = reactive({ message: "", type: "" });
const enterpriseTotal = ref(0);
const approvedEnterpriseCount = ref(0);
const pendingEnterpriseCount = ref(0);
const inspectionTaskTotal = ref(0);
const inProgressInspectionTasks = ref(0);
const complaintTodoCount = ref(0);
const complaintDoneCount = ref(0);
const rectificationTodoCount = ref(0);
const warningOpenCount = ref(0);
const warningProcessingCount = ref(0);
const taskFeeds = ref([]);

const complaintEfficiency = computed(() => {
  const total = complaintTodoCount.value + complaintDoneCount.value;
  if (!total) return 0;
  return Math.round((complaintDoneCount.value / total) * 100);
});

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function isWarningNotFoundError(error) {
  const message = String(error?.message || error || "").trim().toLowerCase();
  return message === "warning not found";
}

function totalOf(pageLike) {
  if (Array.isArray(pageLike)) return pageLike.length;
  return Number(pageLike?.total || 0);
}

function unwrapSettled(result, fallback) {
  return result?.status === "fulfilled" ? result.value : fallback;
}

function timeValueOf(value) {
  const raw = String(value || "").trim();
  if (!raw) return 0;
  const timestamp = Date.parse(raw);
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function formatFeedTime(value) {
  const text = String(formatTime(value || ""));
  return text && text !== "-" ? text : "-";
}

function complaintLogTone(actionType) {
  if (actionType === "COMPLAINT_REJECT") return "danger";
  if (actionType === "COMPLAINT_HANDLE") return "success";
  return "info";
}

function operationFeedTone(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value.includes("OFFLINE")) return "danger";
  if (value.includes("PUBLISH") || value.includes("SUBMIT")) return "success";
  return "info";
}

function bulletinFeedTone(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "BULLETIN_OFFLINE") return "danger";
  if (value === "BULLETIN_PUBLISH") return "success";
  return "info";
}

function rectificationFeedTone(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "REVIEW_REWORK") return "danger";
  if (value === "ENTERPRISE_SUBMIT" || value === "REVIEW_CONFIRM") return "success";
  return "info";
}

function warningFeedTone(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "RESOLVE" || value === "AUTO_ARCHIVE") return "success";
  if (value === "AUTO_LEVEL_UP") return "danger";
  return "info";
}

function formatRectificationFeedTitle(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "SYSTEM_CREATE") return "系统创建整改任务";
  if (value === "ENTERPRISE_SUBMIT") return "企业提交整改";
  if (value === "REVIEW_CONFIRM") return "监管确认闭环";
  if (value === "REVIEW_REWORK") return "监管打回重做";
  return "整改动态";
}

function formatWarningFeedTitle(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "ASSIGN") return "分派处理";
  if (value === "PROCESS") return "开始处理";
  if (value === "RESOLVE") return "标记解决";
  if (value === "AUTO_LEVEL_UP") return "自动升级";
  if (value === "AUTO_ARCHIVE") return "系统归档";
  return "预警动态";
}

function buildRecentTaskFeeds({
  enterpriseAudits = [],
  complaintAudits = [],
  operationAudits = [],
  bulletinAudits = [],
  rectificationAudits = [],
  warningAudits = []
} = {}) {
  const activities = [];

  enterpriseAudits.forEach((item) => {
    if (!item?.id || !item?.targetId) return;
    activities.push({
      id: `enterprise-audit-${item.id}`,
      title: item.targetName || "企业档案",
      meta: `${item.actionName || item.actionType || "企业审核"} | ${formatFeedTime(item.createTime)}`,
      tone:
        item.actionType === "ENTERPRISE_APPROVE"
          ? "success"
          : item.actionType === "ENTERPRISE_REJECT"
            ? "danger"
            : "neutral",
      tag: "企业审核",
      action: "查看企业",
      routeName: "regulator-admin-enterprise-detail",
      params: { enterpriseId: item.targetId },
      query: { from: "overview" },
      sortTime: timeValueOf(item.createTime)
    });
  });

  complaintAudits.forEach((item) => {
    if (!item?.id || !item?.targetId) return;
    activities.push({
      id: `complaint-audit-${item.id}`,
      title: item.targetName || `投诉 #${item.targetId}`,
      meta: `${formatComplaintAuditSummary(item, "投诉流转")} | ${formatFeedTime(item.createTime)}`,
      tone: complaintLogTone(item.actionType),
      tag: "投诉动态",
      action: "查看投诉",
      routeName: "regulator-admin-complaint-detail",
      params: { complaintId: item.targetId },
      query: { from: "overview" },
      sortTime: timeValueOf(item.createTime)
    });
  });

  operationAudits.forEach((item) => {
    if (!item?.id || !item?.targetId) return;
    const targetType = String(item.targetType || "").toUpperCase();
    const isSampling = targetType === "SAMPLING_TASK" || targetType === "SAMPLING_RESULT";
    const routeName = isSampling
      ? (targetType === "SAMPLING_TASK" ? "regulator-admin-sampling-detail" : "regulator-admin-sampling")
      : "regulator-admin-dispatch";

    activities.push({
      id: `operation-audit-${item.id}`,
      title: item.targetName || `${isSampling ? "抽检任务" : "检查任务"} #${item.targetId}`,
      meta: `${item.summary || item.actionName || item.actionType || "监管任务日志"} | ${formatFeedTime(item.createTime)}`,
      tone: operationFeedTone(item.actionType),
      tag: isSampling ? "抽检动态" : "检查动态",
      action: isSampling ? "查看抽检" : "进入检查任务",
      routeName,
      params: isSampling && targetType === "SAMPLING_TASK" ? { taskId: item.targetId } : {},
      query: isSampling ? {} : { status: item.actionType === "INSPECTION_START" ? "IN_PROGRESS" : undefined },
      sortTime: timeValueOf(item.createTime)
    });
  });

  bulletinAudits
    .filter((item) => {
      const actionType = String(item?.actionType || "").toUpperCase();
      return actionType === "BULLETIN_PUBLISH" || actionType === "BULLETIN_OFFLINE";
    })
    .forEach((item) => {
      if (!item?.id || !item?.targetId) return;
      const actionType = String(item.actionType || "").toUpperCase();
      activities.push({
        id: `bulletin-audit-${item.id}`,
        title: item.targetName || `公告 #${item.targetId}`,
        meta: `${item.summary || item.actionName || item.actionType || "公告动态"} | ${formatFeedTime(item.createTime)}`,
        tone: bulletinFeedTone(actionType),
        tag: "公告动态",
        action: "查看公告",
        routeName: "regulator-admin-bulletin-edit",
        params: { bulletinId: item.targetId },
        query: { from: "overview" },
        sortTime: timeValueOf(item.createTime)
      });
    });

  rectificationAudits.forEach((item) => {
    if (!item?.id || !item?.rectificationId) return;
    activities.push({
      id: `rectification-audit-${item.id}`,
      title: item.enterpriseName || item.rectificationNo || `整改 #${item.rectificationId}`,
      meta: `${formatRectificationFeedTitle(item.actionType)} | ${formatFeedTime(item.createTime)}`,
      tone: rectificationFeedTone(item.actionType),
      tag: "整改动态",
      action: "查看整改",
      routeName: "regulator-admin-rectification-detail",
      params: { rectificationId: item.rectificationId },
      query: { from: "overview" },
      sortTime: timeValueOf(item.createTime)
    });
  });

  warningAudits.forEach((item) => {
    if (!item?.warningId) return;
    activities.push({
      id: `warning-audit-${item.id || item.warningId}`,
      title: item.warningTitle || item.warningNo || `预警 #${item.warningId}`,
      meta: `${formatWarningFeedTitle(item.actionType)} | ${formatFeedTime(item.createTime)}`,
      tone: warningFeedTone(item.actionType),
      tag: "预警动态",
      action: "查看预警",
      routeName: "regulator-admin-warning-detail",
      params: { warningId: item.warningId },
      query: { from: "overview" },
      sortTime: timeValueOf(item.createTime)
    });
  });

  taskFeeds.value = activities
    .sort((a, b) => b.sortTime - a.sortTime)
    .slice(0, 6)
    .map(({ sortTime, ...item }) => item);
}

async function openTaskFeed(item) {
  if (!item?.routeName) return;
  if (item.routeName === "regulator-admin-warning-detail") {
    const warningId = Number(item?.params?.warningId || 0);
    if (!warningId) {
      taskFeeds.value = taskFeeds.value.filter((feed) => feed.id !== item.id);
      setStatus("该预警动态缺少有效预警编号，已从最近动态中移除。", "error");
      return;
    }
    try {
      await fetchWarningRecordDetail(token.value, warningId);
    } catch (error) {
      if (isWarningNotFoundError(error)) {
        taskFeeds.value = taskFeeds.value.filter((feed) => feed.id !== item.id);
        setStatus("该预警已不存在或当前账号无权查看，已从最近动态中移除。", "error");
        return;
      }
      setStatus(resolveErrorMessage(error, "预警详情加载失败"), "error");
      return;
    }
  }
  router.push({ name: item.routeName, params: item.params || {}, query: item.query || {} }).catch(() => {});
}

async function loadPage() {
  setStatus("");
  const results = await Promise.allSettled([
    fetchEnterprises(token.value, { page: 1, size: 1 }),
    fetchPendingEnterprises(token.value),
    fetchInspectionTasks(token.value, { page: 1, size: 1 }),
    fetchComplaintStatsOverview(token.value),
    fetchRecentComplaintLogs(token.value, 8),
    fetchRectifications(token.value, { page: 1, size: 1 }),
    fetchWarningRecords(token.value, { page: 1, size: 1 }),
    fetchRecentEnterpriseAuditLogs(token.value, 8),
    fetchRecentOperationAuditLogs(token.value, { limit: 8 }),
    fetchRecentBulletinAuditLogs(token.value, 8),
    fetchRecentRectificationActions(token.value, 8),
    fetchRecentWarningLogs(token.value, 8)
  ]);

  const [
    enterprisesResult,
    pendingEnterprisesResult,
    inspectionTasksResult,
    complaintStatsResult,
    complaintAuditsResult,
    rectificationsResult,
    warningsResult,
    enterpriseAuditsResult,
    operationAuditsResult,
    bulletinAuditsResult,
    rectificationAuditsResult,
    warningAuditsResult
  ] = results;

  const enterprises = unwrapSettled(enterprisesResult, { total: 0, records: [] });
  const pendingEnterprises = unwrapSettled(pendingEnterprisesResult, []);
  const inspectionTasks = unwrapSettled(inspectionTasksResult, { total: 0, records: [] });
  const complaintStats = unwrapSettled(complaintStatsResult, { todoCount: 0, doneCount: 0 });
  const complaintAudits = unwrapSettled(complaintAuditsResult, []);
  const rectifications = unwrapSettled(rectificationsResult, { total: 0, records: [] });
  const warnings = unwrapSettled(warningsResult, { total: 0, records: [] });
  const enterpriseAudits = unwrapSettled(enterpriseAuditsResult, []);
  const operationAudits = unwrapSettled(operationAuditsResult, []);
  const bulletinAudits = unwrapSettled(bulletinAuditsResult, []);
  const rectificationAudits = unwrapSettled(rectificationAuditsResult, []);
  const warningAudits = unwrapSettled(warningAuditsResult, []);

  enterpriseTotal.value = totalOf(enterprises);
  approvedEnterpriseCount.value = Math.max(totalOf(enterprises) - totalOf(pendingEnterprises), 0);
  pendingEnterpriseCount.value = totalOf(pendingEnterprises);
  inspectionTaskTotal.value = totalOf(inspectionTasks);
  inProgressInspectionTasks.value = Array.isArray(inspectionTasks?.records)
    ? inspectionTasks.records.filter((item) => item?.status === "IN_PROGRESS").length
    : 0;

  complaintTodoCount.value = Number(complaintStats?.todoCount || 0);
  complaintDoneCount.value = Number(complaintStats?.doneCount || 0);

  rectificationTodoCount.value = Array.isArray(rectifications?.records)
    ? rectifications.records.filter((item) =>
      ["PENDING_SUBMIT", "PENDING_REVIEW", "REWORK"].includes(String(item?.status || "").toUpperCase())
    ).length
    : 0;
  warningOpenCount.value = Array.isArray(warnings?.records)
    ? warnings.records.filter((item) => String(item?.status || "").toUpperCase() === "OPEN").length
    : 0;
  warningProcessingCount.value = Array.isArray(warnings?.records)
    ? warnings.records.filter((item) => String(item?.status || "").toUpperCase() === "PROCESSING").length
    : 0;

  buildRecentTaskFeeds({
    enterpriseAudits: Array.isArray(enterpriseAudits) ? enterpriseAudits : [],
    complaintAudits: Array.isArray(complaintAudits) ? complaintAudits : [],
    operationAudits: Array.isArray(operationAudits) ? operationAudits : [],
    bulletinAudits: Array.isArray(bulletinAudits) ? bulletinAudits : [],
    rectificationAudits: Array.isArray(rectificationAudits) ? rectificationAudits : [],
    warningAudits: Array.isArray(warningAudits) ? warningAudits : []
  });

  const firstRejected = results.find((item) => item.status === "rejected");
  if (firstRejected) {
    setStatus(resolveErrorMessage(firstRejected.reason, "部分工作台数据加载失败"), "error");
  }
}

onMounted(loadPage);
</script>

<style scoped>
.overview-page { display: grid; gap: 18px; }
.overview-page__header h1 { margin: 0; font-size: 30px; color: #0f172a; }
.overview-page__header p { margin: 8px 0 0; color: #64748b; line-height: 1.6; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.metric-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; display: grid; gap: 6px; }
.metric-card p { margin: 0; color: #64748b; font-size: 12px; font-weight: 700; }
.metric-card strong { color: #0f172a; font-size: 30px; line-height: 1; }
.metric-card span { color: #94a3b8; font-size: 11px; }
.metric-card--danger { border-color: #fecaca; background: #fff7f7; }
.overview-content { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 14px; }
.panel, .quick-panel, .trend-panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; }
.panel__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.panel__head h2, .quick-panel h3, .trend-panel h3 { margin: 0; font-size: 16px; color: #0f172a; }
.todo-grid, .quick-grid { display: grid; gap: 10px; }
.todo-item, .quick-grid button { width: 100%; border: 1px solid #e2e8f0; background: #f8fafc; border-radius: 10px; padding: 12px; text-align: left; cursor: pointer; }
.todo-item { display: grid; grid-template-columns: 28px minmax(0, 1fr); gap: 10px; align-items: start; }
.todo-item .material-symbols-outlined { color: #1d4ed8; font-size: 22px; }
.todo-item strong { display: block; color: #0f172a; font-size: 13px; }
.todo-item p { margin: 4px 0 0; color: #64748b; font-size: 12px; line-height: 1.5; }
.todo-item--danger { border-color: #fecaca; background: #fff7f7; }
.feed-list { display: grid; gap: 10px; }
.feed-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; border: 1px solid #eef2f7; border-radius: 10px; padding: 12px; }
.feed-item strong { display: block; color: #0f172a; font-size: 13px; margin-top: 6px; }
.feed-item p { margin: 4px 0 0; color: #64748b; font-size: 12px; line-height: 1.5; }
.feed-item button, .quick-grid button { min-height: 36px; border: 0; background: #002660; color: #fff; border-radius: 8px; font-size: 12px; font-weight: 800; }
.feed-tag { display: inline-flex; align-items: center; min-height: 22px; border-radius: 999px; padding: 0 10px; font-size: 10px; font-weight: 900; }
.feed-tag.is-success { background: #dcfce7; color: #166534; }
.feed-tag.is-danger { background: #fee2e2; color: #991b1b; }
.feed-tag.is-info { background: #dbeafe; color: #1d4ed8; }
.feed-tag.is-neutral { background: #e2e8f0; color: #475569; }
.aside-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 14px; }
.quick-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.trend-panel { display: grid; gap: 12px; }
.trend-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 0; border-bottom: 1px solid #f1f5f9; }
.trend-row:last-child { border-bottom: 0; padding-bottom: 0; }
.trend-row span { color: #64748b; font-size: 12px; }
.trend-row strong { color: #0f172a; font-size: 14px; }
.empty { color: #94a3b8; font-size: 13px; }
.status { position: fixed; right: 18px; bottom: 18px; z-index: 1000; border-radius: 8px; padding: 10px 12px; color: #fff; background: #0f172a; }
.status.error { background: #b91c1c; }
@media (max-width: 1100px) {
  .metric-grid,
  .overview-content,
  .aside-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .metric-grid,
  .overview-content,
  .aside-grid,
  .quick-grid { grid-template-columns: 1fr; }
  .feed-item { flex-direction: column; align-items: stretch; }
}
</style>
