<template>
  <RegulatorAdminWorkspacePage
    active-key="overview"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
  >
    <section class="overview-page">
      <header class="overview-page__header">
        <h1>区域管理员工作台</h1>
        <p>聚合辖区监管核心指标、待办事项与最近动态。</p>
      </header>

      <div class="metric-grid">
        <article class="metric-card">
          <p>辖区企业总数</p>
          <strong>{{ enterpriseTotal }}</strong>
          <span>已审核 {{ approvedEnterpriseCount }} 家</span>
        </article>
        <article class="metric-card">
          <p>检查任务总数</p>
          <strong>{{ inspectionTaskTotal }}</strong>
          <span>执行中 {{ inProgressInspectionTasks }} 项</span>
        </article>
        <article class="metric-card">
          <p>待处理投诉</p>
          <strong>{{ complaintTodoCount }}</strong>
          <span>待受理 / 待分派 / 已分派 / 处理中</span>
        </article>
        <article class="metric-card metric-card--danger">
          <p>活跃风险预警</p>
          <strong>{{ warningOpenCount }}</strong>
          <span>处理中 {{ warningProcessingCount }} 条</span>
        </article>
      </div>

      <div class="overview-content">
        <section class="panel">
          <div class="panel__head">
            <h2>待办事项中心</h2>
          </div>
          <div class="todo-grid">
            <button type="button" class="todo-item" @click="handleSidebarNavigate('approvals')">
              <span class="material-symbols-outlined">fact_check</span>
              <div>
                <strong>{{ pendingEnterpriseCount }} 家待审批企业</strong>
                <p>新注册企业待备案核验</p>
              </div>
            </button>
            <button type="button" class="todo-item" @click="handleSidebarNavigate('complaints')">
              <span class="material-symbols-outlined">assignment_return</span>
              <div>
                <strong>{{ complaintTodoCount }} 件待处理投诉</strong>
                <p>投诉流转需要受理、分派或继续跟进</p>
              </div>
            </button>
            <button type="button" class="todo-item" @click="handleSidebarNavigate('rectification')">
              <span class="material-symbols-outlined">rule</span>
              <div>
                <strong>{{ rectificationTodoCount }} 项待复核整改</strong>
                <p>整改闭环任务待确认</p>
              </div>
            </button>
            <button type="button" class="todo-item todo-item--danger" @click="handleSidebarNavigate('warnings')">
              <span class="material-symbols-outlined">notification_important</span>
              <div>
                <strong>{{ warningOpenCount }} 条待处理预警</strong>
                <p>高风险告警优先处置</p>
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
          <h3>快捷操作入口</h3>
          <div class="quick-grid">
            <button type="button" @click="handleSidebarNavigate('approvals')">完善企业备案</button>
            <button type="button" @click="handleSidebarNavigate('dispatch')">新建检查任务</button>
            <button type="button" @click="handleSidebarNavigate('bulletins')">发布官方公告</button>
            <button type="button" @click="handleSidebarNavigate('stats')">查看统计分析</button>
          </div>
        </section>

        <section class="trend-panel">
          <h3>运行概览</h3>
          <div class="trend-row">
            <span>本周预警态势</span>
            <strong>OPEN {{ warningOpenCount }} / PROCESSING {{ warningProcessingCount }}</strong>
          </div>
          <div class="trend-row">
            <span>投诉处理效率</span>
            <strong>{{ complaintEfficiency }}%</strong>
          </div>
          <div class="trend-row">
            <span>整改复核待办</span>
            <strong>{{ rectificationTodoCount }} 项</strong>
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
import { fetchComplaints, fetchRecentComplaintLogs } from "../../api/complaint";
import {
  fetchRecentBulletinAuditLogs,
  fetchEnterprises,
  fetchPendingEnterprises,
  fetchRecentEnterpriseAuditLogs,
  fetchWarningRecords
} from "../../api/regulation";
import { fetchInspectionTasks, fetchRecentOperationAuditLogs, fetchRectifications } from "../../api/regulationOperation";
import { formatTime } from "../../utils/formatters";
import { resolveErrorMessage } from "../../utils/uiFeedback";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
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

function totalOf(pageLike) {
  if (Array.isArray(pageLike)) return pageLike.length;
  return Number(pageLike?.total || 0);
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

function buildTaskFeeds({
  enterpriseAudits = [],
  complaintAudits = [],
  operationAudits = [],
  bulletinAudits = [],
  rectifications = [],
  warnings = []
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
      meta: `${item.summary || item.actionName || item.actionType || "投诉流转"} | ${formatFeedTime(item.createTime)}`,
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
    activities.push({
      id: `operation-audit-${item.id}`,
      title: item.targetName || `${isSampling ? "抽检任务" : "检查任务"} #${item.targetId}`,
      meta: `${item.summary || item.actionName || item.actionType || "监管任务日志"} | ${formatFeedTime(item.createTime)}`,
      tone: operationFeedTone(item.actionType),
      tag: isSampling ? "抽检动态" : "检查动态",
      action: isSampling ? "查看抽检" : "进入检查任务",
      routeName: isSampling
        ? (targetType === "SAMPLING_TASK" ? "regulator-admin-sampling-detail" : "regulator-admin-sampling")
        : "regulator-admin-dispatch",
      params: isSampling && targetType === "SAMPLING_TASK" ? { taskId: item.targetId } : {},
      query: isSampling
        ? {}
        : { status: item.actionType === "INSPECTION_START" ? "IN_PROGRESS" : undefined },
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

  rectifications.forEach((item) => {
    if (!item?.id) return;
    const statusText = String(item.status || "").toUpperCase();
    activities.push({
      id: `rectification-${item.id}`,
      title: item.enterpriseName || `整改任务 #${item.id}`,
      meta: `${statusText || "PENDING"} | ${formatFeedTime(item.updateTime || item.createTime || item.currentDeadline)}`,
      tone: statusText === "CONFIRMED" ? "success" : statusText === "REWORK" ? "danger" : "neutral",
      tag: "整改",
      action: "查看整改",
      routeName: "regulator-admin-rectification-detail",
      params: { rectificationId: item.id },
      sortTime: timeValueOf(item.updateTime || item.createTime || item.currentDeadline)
    });
  });

  warnings.forEach((item) => {
    if (!item?.id) return;
    const statusText = String(item.status || "").toUpperCase();
    activities.push({
      id: `warning-${item.id}`,
      title: item.title || item.warningNo || `预警 #${item.id}`,
      meta: `${statusText || "OPEN"} | ${formatFeedTime(item.updateTime || item.createTime)}`,
      tone: statusText === "PROCESSING" ? "info" : statusText === "CLOSED" ? "success" : "danger",
      tag: "预警",
      action: "进入预警中心",
      routeName: "regulator-admin-warnings",
      query: {
        keyword: item.title || item.warningNo || "",
        status: statusText || undefined
      },
      sortTime: timeValueOf(item.updateTime || item.createTime)
    });
  });

  taskFeeds.value = activities
    .sort((a, b) => b.sortTime - a.sortTime)
    .slice(0, 6)
    .map(({ sortTime, ...item }) => item);
}

function openTaskFeed(item) {
  if (!item?.routeName) return;
  router.push({ name: item.routeName, params: item.params || {}, query: item.query || {} }).catch(() => {});
}

async function loadOverview() {
  setStatus("");
  try {
    const [
      enterprisePage,
      approvedEnterprisePage,
      pending,
      inspectionTasks,
      inProgressInspectionPage,
      complaintSubmittedPage,
      complaintPendingPage,
      complaintAssignedPage,
      complaintProcessingPage,
      complaintDonePage,
      rectificationSubmittedPage,
      rectificationReworkPage,
      warningOpenPage,
      warningProcessingPage,
      enterpriseAuditLogs,
      complaintAuditLogs,
      operationAuditLogs,
      bulletinAuditLogs,
      rectificationFeedPage,
      warningFeedPage
    ] = await Promise.all([
      fetchEnterprises(token.value, { page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchEnterprises(token.value, { approvalStatus: "APPROVED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchPendingEnterprises(token.value).catch(() => []),
      fetchInspectionTasks(token.value, { page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchInspectionTasks(token.value, { status: "IN_PROGRESS", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchComplaints(token.value, { status: "SUBMITTED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchComplaints(token.value, { status: "PENDING", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchComplaints(token.value, { status: "ASSIGNED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchComplaints(token.value, { status: "PROCESSING", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchComplaints(token.value, { status: "FEEDBACKED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchRectifications(token.value, { status: "SUBMITTED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchRectifications(token.value, { status: "REWORK", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchWarningRecords(token.value, { status: "OPEN", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchWarningRecords(token.value, { status: "PROCESSING", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchRecentEnterpriseAuditLogs(token.value, 6).catch(() => []),
      fetchRecentComplaintLogs(token.value, 6).catch(() => []),
      fetchRecentOperationAuditLogs(token.value, { limit: 8 }).catch(() => []),
      fetchRecentBulletinAuditLogs(token.value, 6).catch(() => []),
      fetchRectifications(token.value, { page: 1, size: 4 }).catch(() => ({ records: [], total: 0 })),
      fetchWarningRecords(token.value, { page: 1, size: 4 }).catch(() => ({ records: [], total: 0 }))
    ]);

    enterpriseTotal.value = totalOf(enterprisePage);
    approvedEnterpriseCount.value = totalOf(approvedEnterprisePage);
    pendingEnterpriseCount.value = Array.isArray(pending) ? pending.length : Number(pending?.length || 0);
    inspectionTaskTotal.value = totalOf(inspectionTasks);
    inProgressInspectionTasks.value = totalOf(inProgressInspectionPage);
    complaintTodoCount.value =
      totalOf(complaintSubmittedPage) +
      totalOf(complaintPendingPage) +
      totalOf(complaintAssignedPage) +
      totalOf(complaintProcessingPage);
    complaintDoneCount.value = totalOf(complaintDonePage);
    rectificationTodoCount.value = totalOf(rectificationSubmittedPage) + totalOf(rectificationReworkPage);
    warningOpenCount.value = totalOf(warningOpenPage);
    warningProcessingCount.value = totalOf(warningProcessingPage);

    buildTaskFeeds({
      enterpriseAudits: Array.isArray(enterpriseAuditLogs) ? enterpriseAuditLogs : [],
      complaintAudits: Array.isArray(complaintAuditLogs) ? complaintAuditLogs : [],
      operationAudits: Array.isArray(operationAuditLogs) ? operationAuditLogs : [],
      bulletinAudits: Array.isArray(bulletinAuditLogs) ? bulletinAuditLogs : [],
      rectifications: rectificationFeedPage.records || [],
      warnings: warningFeedPage.records || []
    });
  } catch (error) {
    setStatus(resolveErrorMessage(error, "加载区域管理员工作台失败"), "error");
  }
}

onMounted(() => {
  loadOverview();
});
</script>

<style scoped>
.overview-page { display: grid; gap: 18px; }
.overview-page__header h1 { margin: 0; color: #002660; font-size: 30px; font-weight: 800; }
.overview-page__header p { margin: 6px 0 0; color: #475569; font-size: 14px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.metric-card { background: #fff; border-radius: 12px; padding: 16px; border-left: 4px solid #1d4ed8; box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05); }
.metric-card p { margin: 0; color: #64748b; font-size: 12px; }
.metric-card strong { display: block; margin-top: 8px; font-size: 28px; color: #1e293b; }
.metric-card span { font-size: 12px; color: #475569; }
.metric-card--danger { border-left-color: #dc2626; }
.overview-content { display: grid; grid-template-columns: 2fr 2fr; gap: 16px; }
.panel { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04); }
.panel__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.panel__head h2 { margin: 0; font-size: 18px; color: #0f172a; }
.todo-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.todo-item { border: 0; background: #f8fafc; border-radius: 10px; padding: 12px; text-align: left; display: flex; gap: 10px; cursor: pointer; }
.todo-item span { color: #1d4ed8; }
.todo-item strong { display: block; font-size: 14px; color: #0f172a; }
.todo-item p { margin: 3px 0 0; color: #64748b; font-size: 12px; }
.todo-item--danger span { color: #dc2626; }
.feed-list { display: grid; gap: 10px; }
.feed-item { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 10px; border-radius: 10px; background: #f8fafc; }
.feed-item strong { display: block; margin-top: 6px; font-size: 14px; color: #0f172a; }
.feed-item p { margin: 3px 0 0; color: #64748b; font-size: 12px; }
.feed-item button { border: 1px solid #cbd5e1; background: #fff; border-radius: 8px; padding: 6px 10px; cursor: pointer; font-size: 12px; }
.feed-tag { display: inline-block; border-radius: 6px; padding: 2px 8px; font-size: 10px; font-weight: 700; background: #e2e8f0; color: #334155; }
.feed-tag.is-success { background: #dcfce7; color: #166534; }
.feed-tag.is-info { background: #dbeafe; color: #1d4ed8; }
.feed-tag.is-danger { background: #fee2e2; color: #991b1b; }
.aside-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.quick-panel { background: #003a8c; color: #fff; border-radius: 12px; padding: 16px; }
.quick-panel h3, .trend-panel h3 { margin: 0 0 12px; font-size: 16px; }
.quick-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.quick-grid button { border: 1px solid rgba(255, 255, 255, 0.2); background: rgba(255, 255, 255, 0.08); color: #fff; border-radius: 10px; padding: 10px; text-align: left; cursor: pointer; }
.trend-panel { background: #fff; border-radius: 12px; padding: 16px; }
.trend-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #e2e8f0; font-size: 13px; }
.trend-row:last-child { border-bottom: 0; }
.empty { color: #64748b; font-size: 13px; padding: 12px 0; }
.status { position: fixed; right: 18px; bottom: 18px; background: #0f172a; color: #fff; border-radius: 8px; padding: 10px 12px; font-size: 13px; }
.status.error { background: #b91c1c; }
@media (max-width: 1200px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .overview-content, .aside-grid { grid-template-columns: 1fr; }
}
</style>
