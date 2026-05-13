<template>
  <section class="supervision-overview">
    <div class="overview-head">
      <div>
        <div class="eyebrow">{{ mode === "enforcer" ? "执法工作台" : "监管总览" }}</div>
        <h2>把企业、检查、抽检、投诉和预警放在同一张监管视图里</h2>
        <p>
          {{ mode === "enforcer"
            ? "聚合当前执法人员可见的企业、任务、投诉和预警动态，减少来回切换。"
            : "聚合辖区监管核心指标和最近动态，帮助快速掌握整体态势。" }}
        </p>
      </div>
      <button class="refresh-button" type="button" :disabled="loading" @click="loadOverview">
        {{ loading ? "刷新中..." : "刷新数据" }}
      </button>
    </div>

    <div class="metric-grid">
      <article v-for="card in summaryCards" :key="card.key" class="metric-card">
        <span>{{ card.label }}</span>
        <strong>{{ formatNumber(card.value) }}</strong>
        <p>{{ card.note }}</p>
      </article>
    </div>

    <section class="activity-panel">
      <div class="activity-panel__head">
        <h3>最近动态</h3>
      </div>
      <div v-if="recentErrorMessage" class="overview-error">{{ recentErrorMessage }}</div>
      <div v-else-if="!recentFeeds.length" class="activity-panel__empty">当前暂无最近动态。</div>
      <div v-else class="activity-list">
        <button
          v-for="item in recentFeeds"
          :key="item.id"
          class="activity-item"
          type="button"
          @click="openFeed(item)"
        >
          <div class="activity-item__main">
            <span class="activity-tag" :class="`is-${item.tone}`">{{ item.tag }}</span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
          <div class="activity-item__meta">{{ item.meta }}</div>
        </button>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchRecentComplaintLogs } from "../api/complaint";
import { fetchRecentMyWarningLogs, fetchSupervisionOverview } from "../api/regulation";
import { fetchRecentOperationAuditLogs, fetchRecentRectificationActions } from "../api/regulationOperation";
import { formatComplaintAuditOperatorName, formatComplaintAuditSummary } from "../utils/complaintAudit";
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
const recentErrorMessage = ref("");
const overview = ref({
  enterpriseTotalCount: 0,
  keyEnterpriseCount: 0,
  approvedEnterpriseCount: 0,
  inspectionTotalCount: 0,
  samplingTotalCount: 0,
  complaintTotalCount: 0,
  complaintFeedbackedCount: 0,
  complaintOverdueCount: 0,
  openWarningCount: 0
});
const recentComplaintFeeds = ref([]);
const recentOperationFeeds = ref([]);
const recentRectificationFeeds = ref([]);
const recentWarningFeeds = ref([]);

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
    key: "warning",
    label: "活跃预警",
    value: Number(overview.value.openWarningCount) || 0,
    note: "当前仍需持续跟进的预警数量"
  }
]));

const recentFeeds = computed(() =>
  [...recentComplaintFeeds.value, ...recentOperationFeeds.value, ...recentRectificationFeeds.value, ...recentWarningFeeds.value]
    .sort((a, b) => b.sortTime - a.sortTime)
    .slice(0, 6)
);

function formatNumber(value) {
  return Number(value || 0).toLocaleString("zh-CN");
}

function formatFeedMeta(item) {
  const operatorName = formatComplaintAuditOperatorName(item?.operatorName);
  const timeText = formatTime(item?.createTime || "");
  return `${operatorName} | ${timeText && timeText !== "-" ? timeText : "-"}`;
}

function timeValueOf(value) {
  const raw = String(value || "").trim();
  if (!raw) return 0;
  const timestamp = Date.parse(raw);
  return Number.isNaN(timestamp) ? 0 : timestamp;
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

function rectificationLogTone(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "REVIEW_REWORK") return "danger";
  if (value === "ENTERPRISE_SUBMIT" || value === "REVIEW_CONFIRM") return "success";
  return "info";
}

function warningLogTone(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "RESOLVE" || value === "AUTO_ARCHIVE") return "success";
  if (value === "AUTO_LEVEL_UP") return "danger";
  return "info";
}

function formatOperationFeedTitle(item) {
  const actionType = String(item?.actionType || "").toUpperCase();
  if (actionType === "INSPECTION_ASSIGN") return "检查任务已分派";
  if (actionType === "INSPECTION_START") return "检查任务开始处理";
  if (actionType === "INSPECTION_SUBMIT") return "检查结果已提交";
  if (actionType === "SAMPLING_ASSIGN") return "抽检任务已分派";
  if (actionType === "SAMPLING_RESULT_SUBMIT") return "抽检结果已提交";
  if (actionType === "SAMPLING_RESULT_PUBLISH") return "抽检结果已公示";
  return item?.actionName || item?.actionType || "监管动态";
}

function operationLogTag(targetType) {
  const value = String(targetType || "").toUpperCase();
  if (value === "SAMPLING_TASK" || value === "SAMPLING_RESULT") return "抽检";
  if (value === "INSPECTION_TASK") return "检查";
  return "监管";
}

function formatRectificationFeedTitle(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "SYSTEM_CREATE") return "系统创建整改任务";
  if (value === "ENTERPRISE_SUBMIT") return "企业提交整改";
  if (value === "REVIEW_CONFIRM") return "监管确认闭环";
  if (value === "REVIEW_REWORK") return "监管打回重做";
  return "整改动态";
}

function formatWarningAction(actionType) {
  const value = String(actionType || "").toUpperCase();
  if (value === "ASSIGN") return "分派处理";
  if (value === "PROCESS") return "开始处理";
  if (value === "RESOLVE") return "标记解决";
  if (value === "AUTO_LEVEL_UP") return "自动升级";
  if (value === "AUTO_ARCHIVE") return "系统归档";
  return "预警动态";
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
  return null;
}

function resolveWarningFeedRoute(item) {
  if (!item?.warningId) return null;
  return {
    name: "regulator-enforcer-warning-detail",
    params: { warningId: item.warningId },
    query: { from: "stats" }
  };
}

function resolveRectificationFeedRoute(item) {
  if (!item?.rectificationId) return null;
  return {
    name: "regulator-enforcer-rectification-detail",
    params: { rectificationId: item.rectificationId },
    query: { from: "overview" }
  };
}

function openFeed(item) {
  if (!item?.route) return;
  router.push(item.route).catch(() => {});
}

async function loadOverview() {
  loading.value = true;
  try {
    overview.value = {
      ...overview.value,
      ...((await fetchSupervisionOverview(props.token)) || {})
    };
  } finally {
    loading.value = false;
  }
}

async function loadRecentFeeds() {
  if (props.mode !== "enforcer") {
    recentComplaintFeeds.value = [];
    recentOperationFeeds.value = [];
    recentRectificationFeeds.value = [];
    recentWarningFeeds.value = [];
    recentErrorMessage.value = "";
    return;
  }

  recentErrorMessage.value = "";
  try {
    const [complaintLogs, operationLogs, rectificationLogs, warningLogs] = await Promise.all([
      fetchRecentComplaintLogs(props.token, 6),
      fetchRecentOperationAuditLogs(props.token, { limit: 6 }),
      fetchRecentRectificationActions(props.token, 6),
      fetchRecentMyWarningLogs(props.token, 8)
    ]);

    recentComplaintFeeds.value = (Array.isArray(complaintLogs) ? complaintLogs : [])
      .filter((item) => item?.targetId)
      .map((item, index) => ({
        id: item.id || `complaint-feed-${index}`,
        title: item.targetName || `投诉 #${item.targetId}`,
        desc: `[投诉] ${formatComplaintAuditSummary(item, "投诉流转日志")}`,
        meta: formatFeedMeta(item),
        tag: "投诉动态",
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
        tag: `${operationLogTag(item.targetType)}动态`,
        tone: operationLogTone(item.actionType),
        sortTime: timeValueOf(item.createTime),
        route: resolveOperationFeedRoute(item)
      }))
      .filter((item) => item.route);

    recentRectificationFeeds.value = (Array.isArray(rectificationLogs) ? rectificationLogs : [])
      .filter((item) => item?.rectificationId)
      .map((item, index) => ({
        id: item.id || `rectification-feed-${index}`,
        title: item.enterpriseName || item.rectificationNo || `整改 #${item.rectificationId}`,
        desc: `[整改] ${formatRectificationFeedTitle(item.actionType)}`,
        meta: formatFeedMeta(item),
        tag: "整改动态",
        tone: rectificationLogTone(item.actionType),
        sortTime: timeValueOf(item.createTime),
        route: resolveRectificationFeedRoute(item)
      }))
      .filter((item) => item.route);

    recentWarningFeeds.value = (Array.isArray(warningLogs) ? warningLogs : [])
      .filter((item) => item?.warningId)
      .map((item, index) => ({
        id: item.id || `warning-feed-${index}`,
        title: item.warningTitle || item.warningNo || `预警 #${item.warningId}`,
        desc: `[预警] ${formatWarningAction(item.actionType)}`,
        meta: formatFeedMeta(item),
        tag: "预警动态",
        tone: warningLogTone(item.actionType),
        sortTime: timeValueOf(item.createTime),
        route: resolveWarningFeedRoute(item)
      }))
      .filter((item) => item.route);
  } catch (error) {
    recentErrorMessage.value = error instanceof Error ? error.message : "最近动态加载失败";
  }
}

onMounted(async () => {
  await Promise.all([loadOverview(), loadRecentFeeds()]);
});
</script>

<style scoped>
.supervision-overview { display: grid; gap: 16px; }
.overview-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.eyebrow { color: #64748b; font-size: 11px; font-weight: 800; text-transform: uppercase; }
.overview-head h2 { margin: 6px 0 0; color: #0f172a; font-size: 24px; }
.overview-head p { margin: 8px 0 0; color: #64748b; line-height: 1.6; }
.refresh-button { min-height: 38px; border: 0; background: #002660; color: #fff; border-radius: 8px; padding: 0 14px; font-size: 12px; font-weight: 800; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.metric-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; display: grid; gap: 6px; }
.metric-card span { color: #64748b; font-size: 12px; font-weight: 700; }
.metric-card strong { color: #0f172a; font-size: 28px; line-height: 1; }
.metric-card p { margin: 0; color: #94a3b8; font-size: 11px; line-height: 1.5; }
.activity-panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; }
.activity-panel__head h3 { margin: 0 0 12px; color: #0f172a; font-size: 16px; }
.activity-panel__empty, .overview-error { color: #94a3b8; font-size: 13px; }
.activity-list { display: grid; gap: 10px; }
.activity-item { width: 100%; border: 1px solid #eef2f7; background: #fff; border-radius: 10px; padding: 12px; display: flex; align-items: center; justify-content: space-between; gap: 12px; text-align: left; cursor: pointer; }
.activity-item__main strong { display: block; margin-top: 6px; color: #0f172a; font-size: 13px; }
.activity-item__main p { margin: 4px 0 0; color: #64748b; font-size: 12px; line-height: 1.5; }
.activity-item__meta { color: #94a3b8; font-size: 11px; white-space: nowrap; }
.activity-tag { display: inline-flex; min-height: 22px; align-items: center; padding: 0 10px; border-radius: 999px; font-size: 10px; font-weight: 900; }
.activity-tag.is-success { background: #dcfce7; color: #166534; }
.activity-tag.is-danger { background: #fee2e2; color: #991b1b; }
.activity-tag.is-info { background: #dbeafe; color: #1d4ed8; }
.activity-tag.is-neutral { background: #e2e8f0; color: #475569; }
@media (max-width: 960px) {
  .overview-head { flex-direction: column; }
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .metric-grid { grid-template-columns: 1fr; }
  .activity-item { flex-direction: column; align-items: flex-start; }
  .activity-item__meta { white-space: normal; }
}
</style>
