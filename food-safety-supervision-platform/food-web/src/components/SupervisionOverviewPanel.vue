<template>
  <section class="supervision-overview">
    <div class="overview-head">
      <div>
        <div class="eyebrow">{{ mode === "enforcer" ? "执法工作台" : "区域管理员工作台" }}</div>
        <h2>把企业、检查、抽检、投诉和预警放在同一张监管视图里</h2>
        <p>{{ mode === "enforcer" ? "默认按当前执法人员个人口径汇总" : "默认按当前辖区业务口径汇总" }}</p>
      </div>
      <button class="refresh-button" type="button" :disabled="loading" @click="loadOverview">
        {{ loading ? "刷新中..." : "刷新概览" }}
      </button>
    </div>

    <div class="hero-grid">
      <article class="hero-card hero-card--enterprise">
        <span>企业全景</span>
        <strong>{{ formatNumber(overview.enterpriseTotalCount) }}</strong>
        <em>纳管企业总数</em>
        <div class="hero-ratio">
          <div>
            <label>重点监管</label>
            <b>{{ formatPercent(overview.keyEnterpriseCount, overview.enterpriseTotalCount) }}</b>
          </div>
          <div>
            <label>备案通过</label>
            <b>{{ formatPercent(overview.approvedEnterpriseCount, overview.enterpriseTotalCount) }}</b>
          </div>
        </div>
      </article>

      <article class="hero-card hero-card--warning">
        <span>待处置预警</span>
        <strong>{{ formatNumber(overview.openWarningCount) }}</strong>
        <em>当前仍在监管链路中的风险提醒</em>
        <div class="warning-caption">
          这里复用了现有预警统计口径，用来把业务执行和风险联动放在同一页里看清楚。
        </div>
      </article>
    </div>

    <div class="metric-grid">
      <article v-for="card in summaryCards" :key="card.key" class="metric-card" :class="`metric-card--${card.key}`">
        <span>{{ card.label }}</span>
        <strong>{{ formatNumber(card.value) }}</strong>
        <em>{{ card.note }}</em>
      </article>
    </div>

    <div class="cluster-grid">
      <section class="cluster-card cluster-card--inspection">
        <div class="cluster-top">
          <span>检查执行</span>
          <strong>{{ formatNumber(overview.inspectionTotalCount) }}</strong>
        </div>
        <div class="cluster-body">
          <article>
            <label>检查总数</label>
            <b>{{ formatNumber(overview.inspectionTotalCount) }}</b>
          </article>
          <article>
            <label>不合格数</label>
            <b>{{ formatNumber(overview.inspectionFailCount) }}</b>
          </article>
          <article>
            <label>不合格占比</label>
            <b>{{ formatPercent(overview.inspectionFailCount, overview.inspectionTotalCount) }}</b>
          </article>
        </div>
      </section>

      <section class="cluster-card cluster-card--sampling">
        <div class="cluster-top">
          <span>抽检闭环</span>
          <strong>{{ formatNumber(overview.samplingTotalCount) }}</strong>
        </div>
        <div class="cluster-body">
          <article>
            <label>抽检总数</label>
            <b>{{ formatNumber(overview.samplingTotalCount) }}</b>
          </article>
          <article>
            <label>不合格数</label>
            <b>{{ formatNumber(overview.samplingFailCount) }}</b>
          </article>
          <article>
            <label>不合格占比</label>
            <b>{{ formatPercent(overview.samplingFailCount, overview.samplingTotalCount) }}</b>
          </article>
        </div>
      </section>

      <section class="cluster-card cluster-card--complaint">
        <div class="cluster-top">
          <span>投诉协同</span>
          <strong>{{ formatNumber(overview.complaintTotalCount) }}</strong>
        </div>
        <div class="cluster-body">
          <article>
            <label>投诉总数</label>
            <b>{{ formatNumber(overview.complaintTotalCount) }}</b>
          </article>
          <article>
            <label>已反馈数</label>
            <b>{{ formatNumber(overview.complaintFeedbackedCount) }}</b>
          </article>
          <article>
            <label>超时数</label>
            <b>{{ formatNumber(overview.complaintOverdueCount) }}</b>
          </article>
        </div>
      </section>
    </div>

    <section v-if="mode === 'enforcer'" class="activity-panel">
      <div class="activity-panel__head">
        <div>
          <h3>最近动态</h3>
          <p>展示当前执法人员可见的投诉、检查和抽检日志。</p>
        </div>
        <button class="refresh-button" type="button" :disabled="loadingRecent" @click="loadRecentFeeds">
          {{ loadingRecent ? "刷新中..." : "刷新动态" }}
        </button>
      </div>

      <div v-if="recentErrorMessage" class="overview-error">
        {{ recentErrorMessage }}
      </div>
      <div v-else-if="!recentFeeds.length" class="activity-panel__empty">当前暂无最近动态。</div>
      <div v-else class="activity-list">
        <button
          v-for="item in recentFeeds"
          :key="item.id"
          class="activity-item"
          type="button"
          @click="openActivityFeed(item)"
        >
          <span class="activity-item__dot" :class="`is-${item.tone}`"></span>
          <div class="activity-item__main">
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
            <span>{{ item.meta }}</span>
          </div>
          <span class="activity-item__action">查看详情</span>
        </button>
      </div>
    </section>

    <div v-if="errorMessage" class="overview-error">
      {{ errorMessage }}
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchRecentComplaintLogs } from "../api/complaint";
import { fetchRecentOperationAuditLogs } from "../api/regulationOperation";
import { fetchSupervisionOverview } from "../api/regulation";
import { formatTime } from "../utils/formatters";

const props = defineProps({
  token: {
    type: String,
    required: true
  },
  mode: {
    type: String,
    default: "admin"
  }
});

const router = useRouter();
const loading = ref(false);
const loadingRecent = ref(false);
const errorMessage = ref("");
const recentErrorMessage = ref("");
const overview = ref({
  enterpriseTotalCount: 0,
  keyEnterpriseCount: 0,
  approvedEnterpriseCount: 0,
  inspectionTotalCount: 0,
  inspectionFailCount: 0,
  samplingTotalCount: 0,
  samplingFailCount: 0,
  complaintTotalCount: 0,
  complaintFeedbackedCount: 0,
  complaintOverdueCount: 0,
  openWarningCount: 0
});
const recentComplaintFeeds = ref([]);
const recentOperationFeeds = ref([]);

const summaryCards = computed(() => ([
  {
    key: "enterprise",
    label: "重点监管企业",
    value: Number(overview.value.keyEnterpriseCount) || 0,
    note: "当前需要重点跟进的企业数量"
  },
  {
    key: "approved",
    label: "备案通过企业",
    value: Number(overview.value.approvedEnterpriseCount) || 0,
    note: "已经完成备案审核的企业数量"
  },
  {
    key: "feedback",
    label: "已反馈投诉",
    value: Number(overview.value.complaintFeedbackedCount) || 0,
    note: "已经形成反馈闭环的投诉数量"
  },
  {
    key: "overdue",
    label: "投诉超时",
    value: Number(overview.value.complaintOverdueCount) || 0,
    note: "已分派但超过截止时间的投诉数量"
  }
]));

const recentFeeds = computed(() =>
  [...recentComplaintFeeds.value, ...recentOperationFeeds.value]
    .sort((a, b) => b.sortTime - a.sortTime)
    .slice(0, 6)
);

async function loadOverview() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await fetchSupervisionOverview(props.token);
    overview.value = {
      ...overview.value,
      ...(data || {})
    };
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "监管概览加载失败";
  } finally {
    loading.value = false;
  }
}

function complaintLogTone(actionType) {
  if (actionType === "COMPLAINT_REJECT") return "danger";
  if (actionType === "COMPLAINT_HANDLE") return "success";
  if (actionType === "COMPLAINT_PROCESS_START") return "info";
  return "neutral";
}

function operationLogTone(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value.includes("OFFLINE")) return "danger";
  if (value.includes("PUBLISH") || value.includes("SUBMIT")) return "success";
  return "info";
}

function formatFeedMeta(item) {
  const operatorName = String(item?.operatorName || "").trim() || "监管人员";
  const timeText = formatTime(item?.createTime || "");
  return `${operatorName} | ${timeText && timeText !== "-" ? timeText : "-"}`;
}

function timeValueOf(value) {
  const raw = String(value || "").trim();
  if (!raw) return 0;
  const timestamp = Date.parse(raw);
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function operationLogTag(targetType) {
  const value = String(targetType || "").toUpperCase();
  if (value === "SAMPLING_TASK" || value === "SAMPLING_RESULT") return "抽检";
  if (value === "INSPECTION_TASK") return "检查";
  if (value === "RECTIFICATION_TASK") return "整改";
  return "监管";
}

function formatOperationFeedTitle(item) {
  const actionType = String(item?.actionType || "").toUpperCase();
  if (actionType === "INSPECTION_ASSIGN") return "检查任务已分派";
  if (actionType === "INSPECTION_START") return "检查任务开始处理";
  if (actionType === "INSPECTION_SUBMIT") return "检查结果已提交";
  if (actionType === "INSPECTION_RECTIFICATION_CREATE") return "已触发整改任务";
  if (actionType === "SAMPLING_ASSIGN") return "抽检任务已分派";
  if (actionType === "SAMPLING_RESULT_SUBMIT") return "抽检结果已提交";
  if (actionType === "SAMPLING_RESULT_PUBLISH") return "抽检结果已公示";
  if (actionType === "SAMPLING_RESULT_OFFLINE") return "抽检结果已下线";
  return item?.actionName || item?.actionType || "监管动态";
}

function resolveOperationFeedRoute(item) {
  const targetType = String(item?.targetType || "").toUpperCase();
  if (targetType === "INSPECTION_TASK" && item?.targetId) {
    return {
      name: "regulator-enforcer-task-detail",
      params: { taskId: item.targetId },
      query: { from: "stats" }
    };
  }
  if (targetType === "SAMPLING_TASK" && item?.targetId) {
    return {
      name: "regulator-enforcer-sampling-detail",
      params: { taskId: item.targetId },
      query: { from: "stats" }
    };
  }
  if (targetType === "SAMPLING_RESULT") {
    return {
      name: "regulator-enforcer-sampling",
      params: {},
      query: {}
    };
  }
  return null;
}

async function loadRecentFeeds() {
  if (props.mode !== "enforcer") {
    recentComplaintFeeds.value = [];
    recentOperationFeeds.value = [];
    recentErrorMessage.value = "";
    return;
  }
  loadingRecent.value = true;
  recentErrorMessage.value = "";
  try {
    const [complaintLogs, operationLogs] = await Promise.all([
      fetchRecentComplaintLogs(props.token, 6),
      fetchRecentOperationAuditLogs(props.token, { limit: 6 })
    ]);

    recentComplaintFeeds.value = (Array.isArray(complaintLogs) ? complaintLogs : [])
      .filter((item) => item?.targetId)
      .map((item, index) => ({
        id: item.id || `complaint-feed-${index}`,
        title: item.targetName || `投诉 #${item.targetId}`,
        desc: `[投诉] ${item.summary || item.actionName || item.actionType || "投诉流转日志"}`,
        meta: formatFeedMeta(item),
        tone: complaintLogTone(item.actionType),
        sortTime: timeValueOf(item.createTime),
        route: {
          name: "regulator-enforcer-complaint-detail",
          params: { complaintId: item.targetId },
          query: { from: "stats" }
        }
      }));

    recentOperationFeeds.value = (Array.isArray(operationLogs) ? operationLogs : [])
      .filter((item) => item?.targetId)
      .map((item, index) => ({
        id: item.id || `operation-feed-${index}`,
        title: item.targetName || `${operationLogTag(item.targetType)} #${item.targetId}`,
        desc: `[${operationLogTag(item.targetType)}] ${item.summary || formatOperationFeedTitle(item)}`,
        meta: formatFeedMeta(item),
        tone: operationLogTone(item.actionType),
        sortTime: timeValueOf(item.createTime),
        route: resolveOperationFeedRoute(item)
      }))
      .filter((item) => item.route);
  } catch (error) {
    recentComplaintFeeds.value = [];
    recentOperationFeeds.value = [];
    recentErrorMessage.value = error instanceof Error ? error.message : "最近动态加载失败";
  } finally {
    loadingRecent.value = false;
  }
}

function openActivityFeed(item) {
  if (!item?.route?.name) return;
  router.push({
    name: item.route.name,
    params: item.route.params || {},
    query: item.route.query || {}
  }).catch(() => {});
}

function formatNumber(value) {
  return new Intl.NumberFormat("zh-CN").format(Number(value) || 0);
}

function formatPercent(value, total) {
  const safeTotal = Number(total) || 0;
  if (!safeTotal) return "0%";
  return `${Math.round(((Number(value) || 0) / safeTotal) * 100)}%`;
}

onMounted(() => {
  loadOverview();
  loadRecentFeeds();
});
</script>

<style scoped>
.supervision-overview {
  --overview-paper: linear-gradient(180deg, #f8fbff 0%, #f3f8fe 100%);
  --overview-line: #d7e1ec;
  --overview-text: #0f172a;
  --overview-muted: #64748b;
  --overview-accent: #1d4f91;
  --overview-forest: #0f766e;
  --overview-rose: #c2410c;
  padding: 16px;
  border-radius: 4px;
  border: 1px solid var(--overview-line);
  background: var(--overview-paper);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
  color: var(--overview-text);
  overflow: hidden;
}

.overview-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 2px;
  background: #eaf2fd;
  color: var(--overview-accent);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.overview-head h2 {
  margin: 10px 0 6px;
  font-size: 24px;
  line-height: 1.2;
}

.overview-head p {
  margin: 0;
  color: var(--overview-muted);
}

.refresh-button {
  border: 1px solid #cfdceb;
  border-radius: 4px;
  padding: 8px 14px;
  background: #ffffff;
  color: #1d4f91;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease, background-color 0.2s ease;
}

.refresh-button:hover:not(:disabled) {
  background: #f1f6fd;
  transform: translateY(-1px);
}

.refresh-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.95fr);
  gap: 12px;
  margin-bottom: 12px;
}

.hero-card,
.metric-card,
.cluster-card {
  position: relative;
  overflow: hidden;
  animation: overview-rise 0.45s ease both;
}

.hero-card {
  min-height: 152px;
  padding: 16px;
  border-radius: 4px;
  border: 1px solid #dbe5f1;
  box-shadow: none;
}

.hero-card--enterprise {
  background: linear-gradient(135deg, #f7fbff 0%, #ecf4ff 100%);
}

.hero-card--warning {
  background: linear-gradient(135deg, #f4f9ff 0%, #ebf3fe 100%);
}

.hero-card span,
.cluster-top span,
.metric-card span {
  display: block;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.hero-card strong,
.cluster-top strong,
.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 38px;
  font-weight: 800;
  line-height: 1;
}

.hero-card em,
.metric-card em {
  display: block;
  margin-top: 8px;
  font-style: normal;
  color: var(--overview-muted);
}

.hero-ratio {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.hero-ratio div,
.warning-caption {
  padding: 10px 12px;
  border-radius: 4px;
  background: #ffffff;
  border: 1px solid #dfe8f3;
}

.hero-ratio label {
  display: block;
  font-size: 12px;
  color: var(--overview-muted);
}

.hero-ratio b {
  display: block;
  margin-top: 4px;
  font-size: 20px;
}

.warning-caption {
  margin-top: 14px;
  line-height: 1.6;
  color: #48617b;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.metric-card {
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #dbe5f1;
  background: #ffffff;
  box-shadow: none;
}

.metric-card--enterprise strong {
  color: var(--overview-accent);
}

.metric-card--approved strong {
  color: var(--overview-forest);
}

.metric-card--feedback strong {
  color: #4979a5;
}

.metric-card--overdue strong {
  color: var(--overview-rose);
}

.cluster-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.cluster-card {
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #dbe5f1;
  background: #ffffff;
}

.cluster-card--inspection {
  background: linear-gradient(180deg, #ffffff 0%, #f4f9ff 100%);
}

.cluster-card--sampling {
  background: linear-gradient(180deg, #ffffff 0%, #f2faf8 100%);
}

.cluster-card--complaint {
  background: linear-gradient(180deg, #ffffff 0%, #f6f8fc 100%);
}

.cluster-body {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.cluster-body article {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 4px;
  background: #f8fbff;
  border: 1px solid #e3ebf5;
}

.cluster-body label {
  color: var(--overview-muted);
}

.cluster-body b {
  font-size: 20px;
}

.activity-panel {
  margin-top: 12px;
  border: 1px solid #dbe5f1;
  border-radius: 4px;
  background: #ffffff;
  overflow: hidden;
}

.activity-panel__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 16px 12px;
  border-bottom: 1px solid #e6edf5;
}

.activity-panel__head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

.activity-panel__head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
}

.activity-panel__empty {
  padding: 20px 16px;
  color: #64748b;
  font-size: 13px;
}

.activity-list {
  display: grid;
}

.activity-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-bottom: 1px solid #eef2f7;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.activity-item:last-child {
  border-bottom: 0;
}

.activity-item:hover {
  background: #f8fbff;
}

.activity-item__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #94a3b8;
}

.activity-item__dot.is-success {
  background: #16a34a;
}

.activity-item__dot.is-danger {
  background: #dc2626;
}

.activity-item__dot.is-info {
  background: #2563eb;
}

.activity-item__main {
  min-width: 0;
}

.activity-item__main strong {
  display: block;
  color: #0f172a;
  font-size: 14px;
}

.activity-item__main p {
  margin: 6px 0 0;
  color: #334155;
  font-size: 13px;
  line-height: 1.5;
}

.activity-item__main span {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.activity-item__action {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.overview-error {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 4px;
  border: 1px solid #f4cccc;
  background: #fff5f5;
  color: #a53d3d;
}

@keyframes overview-rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1024px) {
  .hero-grid,
  .metric-grid,
  .cluster-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .supervision-overview {
    padding: 12px;
  }

  .overview-head {
    flex-direction: column;
  }

  .overview-head h2 {
    font-size: 23px;
  }

  .hero-card strong,
  .cluster-top strong,
  .metric-card strong {
    font-size: 38px;
  }

  .hero-ratio {
    grid-template-columns: 1fr;
  }

  .activity-panel__head {
    flex-direction: column;
  }

  .activity-item {
    grid-template-columns: 10px minmax(0, 1fr);
  }

  .activity-item__action {
    display: none;
  }
}
</style>
