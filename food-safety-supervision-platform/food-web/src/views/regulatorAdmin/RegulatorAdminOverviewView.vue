<template>
  <RegulatorAdminWorkspacePage
    active-key="overview"
    :username="regulatorUser.username"
    @navigate="handleSidebarNavigate"
    @logout="handleLogout"
    @pending-feature="onPendingFeature"
  >
    <section class="overview-page">
      <header class="overview-page__header">
        <h1>监管概览</h1>
        <p>区域监管核心指标、待办事项与风险态势总览</p>
      </header>

      <div class="metric-grid">
        <article class="metric-card">
          <p>辖区企业总数</p>
          <strong>{{ enterpriseTotal }}</strong>
          <span>已核准 {{ approvedEnterpriseCount }} 家</span>
        </article>
        <article class="metric-card">
          <p>检查任务总数</p>
          <strong>{{ inspectionTaskTotal }}</strong>
          <span>执行中 {{ inProgressInspectionTasks }} 项</span>
        </article>
        <article class="metric-card">
          <p>待处理投诉</p>
          <strong>{{ complaintTodoCount }}</strong>
          <span>待受理 / 待分派案件</span>
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
            <button type="button" @click="onPendingFeature('待办全量列表')">查看全部</button>
          </div>
          <div class="todo-grid">
            <button type="button" class="todo-item" @click="handleSidebarNavigate('approvals')">
              <span class="material-symbols-outlined">fact_check</span>
              <div><strong>{{ pendingEnterpriseCount }} 家待审批企业</strong><p>新注册企业待备案核验</p></div>
            </button>
            <button type="button" class="todo-item" @click="handleSidebarNavigate('complaints')">
              <span class="material-symbols-outlined">assignment_return</span>
              <div><strong>{{ complaintTodoCount }} 件待派发投诉</strong><p>投诉流转需要分配执行人员</p></div>
            </button>
            <button type="button" class="todo-item" @click="handleSidebarNavigate('rectification')">
              <span class="material-symbols-outlined">rule</span>
              <div><strong>{{ rectificationTodoCount }} 项待复核整改</strong><p>整改闭环任务待确认</p></div>
            </button>
            <button type="button" class="todo-item todo-item--danger" @click="handleSidebarNavigate('warnings')">
              <span class="material-symbols-outlined">notification_important</span>
              <div><strong>{{ warningOpenCount }} 条待处理预警</strong><p>高风险告警优先处置</p></div>
            </button>
          </div>
        </section>

        <section class="panel">
          <div class="panel__head"><h2>最近任务动态</h2></div>
          <div v-if="!taskFeeds.length" class="empty">当前暂无任务动态。</div>
          <div v-else class="feed-list">
            <article v-for="item in taskFeeds" :key="item.id" class="feed-item">
              <div><span class="feed-tag" :class="`is-${item.tone}`">{{ item.tag }}</span><strong>{{ item.title }}</strong><p>{{ item.meta }}</p></div>
              <button type="button" @click="onPendingFeature(item.action)">{{ item.action }}</button>
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
            <button type="button" @click="onPendingFeature('导出监管报表')">导出监管报表</button>
          </div>
        </section>

        <section class="trend-panel">
          <h3>趋势统计</h3>
          <div class="trend-row">
            <span>本周预警趋势</span>
            <strong>OPEN {{ warningOpenCount }} / PROCESSING {{ warningProcessingCount }}</strong>
          </div>
          <div class="trend-row">
            <span>投诉处理效率</span>
            <strong>{{ complaintEfficiency }}%</strong>
          </div>
          <div class="trend-row">
            <span>系统审计</span>
            <strong>TODO: 接入审计日志 API</strong>
          </div>
        </section>
      </div>

      <div v-if="status.message" class="status" :class="status.type">{{ status.message }}</div>
    </section>
  </RegulatorAdminWorkspacePage>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { fetchComplaints } from "../../api/complaint";
import { fetchEnterprises, fetchPendingEnterprises, fetchWarningRecords } from "../../api/regulation";
import { fetchInspectionTasks, fetchRectifications } from "../../api/regulationOperation";
import RegulatorAdminWorkspacePage from "../../components/regulatorAdmin/RegulatorAdminWorkspacePage.vue";
import {
  regulatorFeaturePendingNotice,
  useRegulatorAdminShellSession
} from "./regulatorAdminShared";

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

function onPendingFeature(title) {
  regulatorFeaturePendingNotice(title);
}

function setStatus(message, type = "info") {
  status.message = message;
  status.type = type;
}

function totalOf(pageLike) {
  if (Array.isArray(pageLike)) return pageLike.length;
  return Number(pageLike?.total || 0);
}

function buildTaskFeeds(inspectionTasks = []) {
  taskFeeds.value = inspectionTasks.slice(0, 3).map((item) => {
    const statusText = String(item.status || "").toUpperCase();
    const tone = statusText === "COMPLETED" ? "success" : statusText === "IN_PROGRESS" ? "info" : "neutral";
    const tag = statusText || "CREATED";
    return {
      id: item.id || `${item.enterpriseId}-${item.taskTitle}`,
      title: item.taskTitle || "监管任务",
      meta: item.enterpriseName || "待补充企业信息",
      tone,
      tag,
      action: "查看详情"
    };
  });
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
      complaintPendingPage,
      complaintAcceptedPage,
      complaintAssignedPage,
      complaintDonePage,
      rectificationSubmittedPage,
      rectificationReworkPage,
      warningOpenPage,
      warningProcessingPage,
      inspectionTaskFeed
    ] = await Promise.all([
      fetchEnterprises(token.value, { page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchEnterprises(token.value, { approvalStatus: "APPROVED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchPendingEnterprises(token.value).catch(() => []),
      fetchInspectionTasks(token.value, { page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchInspectionTasks(token.value, { status: "IN_PROGRESS", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchComplaints(token.value, { status: "PENDING", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchComplaints(token.value, { status: "ACCEPTED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchComplaints(token.value, { status: "ASSIGNED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchComplaints(token.value, { status: "PROCESSED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchRectifications(token.value, { status: "SUBMITTED", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchRectifications(token.value, { status: "REWORK", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchWarningRecords(token.value, { status: "OPEN", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchWarningRecords(token.value, { status: "PROCESSING", page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      fetchInspectionTasks(token.value, { page: 1, size: 20 }).catch(() => ({ records: [], total: 0 }))
    ]);

    enterpriseTotal.value = totalOf(enterprisePage);
    approvedEnterpriseCount.value = totalOf(approvedEnterprisePage);
    pendingEnterpriseCount.value = Array.isArray(pending) ? pending.length : Number(pending?.length || 0);
    inspectionTaskTotal.value = totalOf(inspectionTasks);
    inProgressInspectionTasks.value = totalOf(inProgressInspectionPage);
    complaintTodoCount.value = totalOf(complaintPendingPage) + totalOf(complaintAcceptedPage) + totalOf(complaintAssignedPage);
    complaintDoneCount.value = totalOf(complaintDonePage);
    rectificationTodoCount.value = totalOf(rectificationSubmittedPage) + totalOf(rectificationReworkPage);
    warningOpenCount.value = totalOf(warningOpenPage);
    warningProcessingCount.value = totalOf(warningProcessingPage);

    buildTaskFeeds(inspectionTaskFeed.records || []);
  } catch (error) {
    setStatus(error.message || "加载监管概览失败。", "error");
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
.panel__head button { border: 0; background: transparent; color: #1d4ed8; font-size: 12px; font-weight: 700; cursor: pointer; }
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
